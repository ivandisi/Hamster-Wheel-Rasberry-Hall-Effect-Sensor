import time
import threading
import json
from queue import LifoQueue
from queue import Empty
from tinydb import TinyDB, Query
from datetime import datetime
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from gpiozero import Button
from signal import pause

# =======================
# CONFIG
# =======================
DB_FILE = "pistacchiodbnew2.json"
tripLength = 91
Digital_PIN = 22
MIN_TRIP_DT = 0.01 
MAX_ONE_IN = 0.2
WATCHDOG_TIMEOUT = 30  # 60 secondi

# =======================
# GLOBALS
# =======================
db = TinyDB(DB_FILE)
db_lock = threading.Lock()
db_queue = LifoQueue()
stop_event = threading.Event()

last_gpio_time = 0
last_db_write = 0

# =======================
# GPIO
# =======================
sensor = Button(Digital_PIN, pull_up=True)

def myCounter():
    """Callback GPIO con debounce software e gestione queue non bloccante"""
    global last_gpio_time
    try:
        ts = time.time()
        
        if ts - last_gpio_time < MIN_TRIP_DT:
            return
        last_gpio_time = ts

        data = datetime.fromtimestamp(ts)

        try:
            test = db_queue.get_nowait()
            #print(ts - test['time'])
            if ts - test['time'] < MAX_ONE_IN:
                print("Removed old, Pistacchio", ts)
                db_queue.put({
                    'type': 'trip',
                    'time': ts,
                    'data': data.strftime("%Y%m%d"),
                    'hour': data.strftime("%H:%M")
                })
            else:
                db_queue.put(test)
                print("Pistacchio", ts)
                db_queue.put({
                    'type': 'trip',
                    'time': ts,
                    'data': data.strftime("%Y%m%d"),
                    'hour': data.strftime("%H:%M")
                })
        except Empty:
            print("Empty, Pistacchio", ts)
            db_queue.put({
                'type': 'trip',
                'time': ts,
                'data': data.strftime("%Y%m%d"),
                'hour': data.strftime("%H:%M")
            })
        
    except Exception as e:
        print("ERRORE GPIO:", e)

sensor.when_pressed = myCounter

# =======================
# DB WRITER THREAD
# =======================
def dbWriterThread():
    global last_db_write
    print("DB writer avviato")
    while not stop_event.is_set():
        try:
            item = db_queue.get(timeout=1)
            with db_lock:
                db.insert(item)
            last_db_write = time.time()
            db_queue.task_done()
        except Exception:
            pass
        
        time.sleep(60)

# =======================
# WATCHDOG THREADS
# =======================
def gpioWatchdogActive():
    global sensor, last_gpio_time
    while not stop_event.is_set():
        delta = time.time() - last_gpio_time
        if delta > WATCHDOG_TIMEOUT:
            #print(f"⚠️ WATCHDOG GPIO: inattività {int(delta)}s, reset sensore...")
            try:
                # rimuove il sensore attuale
                sensor.close()
                time.sleep(0.1)
                # ricrea il sensore
                sensor = Button(Digital_PIN, pull_up=True)
                sensor.when_pressed = myCounter
                last_gpio_time = time.time()
                #print("✅ Sensore GPIO resettato correttamente")
            except Exception as e:
                print("❌ Errore nel reset GPIO:", e)
        time.sleep(1)  # controllo frequente

def dbWatchdog():
    while not stop_event.is_set():
        qsize = db_queue.qsize()
        if qsize > 100:  # se la coda DB cresce troppo
            print(f"⚠️ WATCHDOG DB: queue lunga = {qsize}")
        time.sleep(5)

# =======================
# DB READ FUNCTIONS
# =======================
def getTripsByHour(day, hour):
    Qry = Query()
    with db_lock:
        data = db.search(
            (Qry.data == day) & Qry.hour.test(lambda h: h.startswith(hour))
        )
    return {
        'trips': len(data),
        'length': len(data) * tripLength,
        'hour': hour
    }

def getTripsByDay(day):
    return [getTripsByHour(day, f"{h:02d}") for h in range(24)]

def getTripsByMonth(req, month):
    Qry = Query()
    with db_lock:
        t = db.search(Qry.data.test(lambda d: d.startswith(req)))
    return {
        'trips': len(t),
        'length': len(t) * tripLength,
        'month': month
    }

def getTripsByYear(year):
    return [
        getTripsByMonth(year + f"{m:02d}", f"{m:02d}")
        for m in range(1, 13)
    ]

def getMaxSpeed(day):
    Qry = Query()
    with db_lock:
        events = db.search(Qry.data == day)

    CIRCUMFERENCE_M = tripLength / 100
    N_LAPS = 50
    MAX_SPEED_M_S = 4.0
    MIN_DT = (CIRCUMFERENCE_M * N_LAPS) / MAX_SPEED_M_S

    events.sort(key=lambda e: e["time"])
    times = [e["time"] for e in events]

    results = []
    for i in range(len(times) - N_LAPS):
        dt = times[i + N_LAPS] - times[i]
        if dt < MIN_DT:
            continue

        speed = (CIRCUMFERENCE_M * N_LAPS) / dt
        results.append({
            "speed": speed,
            "speedKM": speed * 3.6,
            "deltaT": dt
        })

    if not results:
        return {'speed': "", 'speedKM': "", 'deltaT': ""}

    best = max(results, key=lambda r: r["speed"])
    return {
        'speed': f"{best['speed']:.2f} m/s",
        'speedKM': f"({best['speedKM']:.2f} km/h)",
        'deltaT': f"Deltat: {best['deltaT']:.2f} s"
    }

# =======================
# HTTP API
# =======================
class ApiHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)

        try:
            if parsed.path == "/getByDay":
                day = params.get("day", [None])[0]
                if not day:
                    raise ValueError("Parametro 'day' mancante")
                result = getTripsByDay(day)

            elif parsed.path == "/getByYear":
                year = params.get("year", [None])[0]
                if not year:
                    raise ValueError("Parametro 'year' mancante")
                result = getTripsByYear(year)

            elif parsed.path == "/getMaxSpeed":
                day = params.get("day", [None])[0]
                if not day:
                    raise ValueError("Parametro 'day' mancante")
                result = getMaxSpeed(day)

            else:
                self.send_response(404)
                self.end_headers()
                return

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(result).encode())

        except Exception as e:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(str(e).encode())

    def log_message(self, *args):
        return  # silenzia log HTTP

# =======================
# THREADS
# =======================
def serverThread():
    server = ThreadingHTTPServer(("0.0.0.0", 8000), ApiHandler)
    print("Server HTTP avviato su :8000")
    server.serve_forever()

def commandThread():
    try:
        while not stop_event.is_set():
            cmd = input("Command: ").strip()
            if cmd == "q":
                stop_event.set()
            elif cmd == "a":
                with db_lock:
                    print(db.all())
            elif cmd == "t":
                myCounter()
            time.sleep(0.3)
    except KeyboardInterrupt:
        stop_event.set()

# =======================
# START
# =======================
print("[CTRL + C per terminare]")

#threading.Thread(target=dbWriterThread, daemon=True).start()
threading.Thread(target=serverThread, daemon=True).start()
threading.Thread(target=commandThread).start()
threading.Thread(target=gpioWatchdogActive, daemon=True).start()
threading.Thread(target=dbWatchdog, daemon=True).start()

try:
    pause()
except KeyboardInterrupt:
    stop_event.set()
finally:
    print("Script terminato")
