import time
import threading
import base64
import json
from queue import LifoQueue
from queue import Empty
from tinydb import TinyDB, Query
from datetime import datetime
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from gpiozero import Button
from signal import pause
import sqlite3

# =======================
# CONFIG
# =======================
DB_FILE = "pistacchio.db"
# Length of wheel, 28cm diameter (88 circumference)
tripLength = 88
# Sensor PIN
Digital_PIN = 22
# Debouncing time 10ms
MIN_TRIP_DT = 0.01 
# High level filter, max 1 trip in 200ms on a 28cm diameter wheel (realistic max hamster speed)
MAX_ONE_IN = 0.2
# Watchdog
WATCHDOG_TIMEOUT = 30 

# =======================
# GLOBALS
# =======================

con = sqlite3.connect(DB_FILE)
cur = con.cursor()

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
def sqliteWriterThread():
    print("SQLite DB writer started")
    global last_db_write

    # Connessione locale al thread
    con_thread = sqlite3.connect(DB_FILE)
    cur_thread = con_thread.cursor()

    while not stop_event.is_set():
        time.sleep(60)  # scrive ogni 60 secondi
        batch = []

        while True:
            try:
                item = db_queue.get_nowait()
                batch.append(item)
                db_queue.task_done()
            except Empty:
                break

        if batch:
            # Converte batch di dict in tuple per SQLite
            sqlite_data = [
                (row["type"], row["time"], row["data"], row["hour"])
                for row in batch
            ]

            with db_lock:
                cur_thread.executemany(
                    "INSERT INTO Trip (type, time, data, hour) VALUES (?, ?, ?, ?)",
                    sqlite_data
                )
                con_thread.commit()

            last_db_write = time.time()
            print(f"Wrote {len(batch)} events into SQLite")

    con_thread.close()
            
# =======================
# WATCHDOG THREADS
# =======================
def gpioWatchdogActive():
    global sensor, last_gpio_time
    while not stop_event.is_set():
        delta = time.time() - last_gpio_time
        if delta > WATCHDOG_TIMEOUT:
            try:
                sensor.close()
                time.sleep(0.1)
                sensor = Button(Digital_PIN, pull_up=True)
                sensor.when_pressed = myCounter
                last_gpio_time = time.time()
            except Exception as e:
                print("❌ Error reset GPIO:", e)
        time.sleep(1)  # controllo frequente

def dbWatchdog():
    while not stop_event.is_set():
        qsize = db_queue.qsize()
        if qsize > 100:
            print(f"⚠️ WATCHDOG DB: queue length = {qsize}")
        time.sleep(5)

# =======================
# DB READ FUNCTIONS
# =======================
def table_exists(conn, name):
    try:
        conn.execute(f"SELECT 1 FROM {name} LIMIT 1")
        return True
    except sqlite3.OperationalError:
        return False
    
def getTripsByHour(day, hour):
    with db_lock:
        con_thread = sqlite3.connect(DB_FILE)
        cur_thread = con_thread.cursor()
    
        cur_thread.execute("""
            SELECT COUNT(*) 
            FROM Trip
            WHERE data = ?
              AND hour LIKE ?
        """, (day, f"{hour}%"))

        trips = cur_thread.fetchone()[0]
    
    con_thread.close()
    return {
        'trips': trips,
        'length': trips * tripLength,
        'hour': hour
    }
    
def getTripsByMonth(req, month):
    with db_lock:
        con_thread = sqlite3.connect(DB_FILE)
        cur_thread = con_thread.cursor()
        cur_thread.execute("""
            SELECT COUNT(*)
            FROM Trip
            WHERE data LIKE ?
        """, (f"{req}%",))

        trips = cur_thread.fetchone()[0]

    con_thread.close()
    return {
        'trips': trips,
        'length': trips * tripLength,
        'month': month
    }
    
def getTripsByDays(days: list[str]) -> list[dict]:
    if not days:
        return []
        
    json_str = base64.b64decode(days).decode('utf-8')
    dates_array = json.loads(json_str)
    
    print(dates_array)
    
    placeholders = ",".join(["?"] * len(dates_array))
    
    print(placeholders)

    with db_lock:
        con_thread = sqlite3.connect(DB_FILE)
        cur_thread = con_thread.cursor()
        cur_thread.execute(f"""
            SELECT data, COUNT(*) AS trips
            FROM Trip
            WHERE data IN ({placeholders})
            GROUP BY data
        """, dates_array)
        rows = cur_thread.fetchall()

    trips_dict = {row[0]: row[1] for row in rows}

    result = []
    for d in dates_array:
        trips = trips_dict.get(d, 0)
        result.append({
            'data': d,
            'trips': trips,
            'length': trips * tripLength
        })

    con_thread.close()
    return result
    
    
def getMaxSpeed(day: str) -> dict:

    CIRCUMFERENCE_M = tripLength / 100
    N_LAPS = 50
    MAX_SPEED_M_S = 4.0
    MIN_DT = (CIRCUMFERENCE_M * N_LAPS) / MAX_SPEED_M_S

    with db_lock:
        con_thread = sqlite3.connect(DB_FILE)
        cur_thread = con_thread.cursor()
        cur_thread.execute("""
            SELECT time
            FROM Trip
            WHERE data = ?
            ORDER BY time ASC
        """, (day,))
        rows = cur_thread.fetchall()

    # Estrai i tempi come lista di float/int
    times = [row[0] for row in rows]

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
    
    con_thread.close()
    return {
        'speed': f"{best['speed']:.2f} m/s",
        'speedKM': f"({best['speedKM']:.2f} km/h)",
        'deltaT': f"deltaT: {best['deltaT']:.2f} s"
    }

if not table_exists(con, 'Trip'):
    cur.execute("""
        CREATE TABLE IF NOT EXISTS Trip (
            type TEXT,
            time REAL,
            data TEXT,
            hour TEXT
        )
        """)
    con.commit()
    
def getTripsByDay(day):
    return [getTripsByHour(day, f"{h:02d}") for h in range(24)]


def getTripsByYear(year):
    return [
        getTripsByMonth(year + f"{m:02d}", f"{m:02d}")
        for m in range(1, 13)
    ]

# =======================
# HTTP API
# =======================
class ApiHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)

        try:
            if parsed.path == "/getByDays":
                days = params.get("days", [None])[0]
                if not days:
                    raise ValueError("Missing 'days' parameter")
                print(days)
                result = getTripsByDays(days)

            elif parsed.path == "/getByDay":
                day = params.get("day", [None])[0]
                if not day:
                    raise ValueError("Missing 'day' parameter")
                result = getTripsByDay(day)

            elif parsed.path == "/getByYear":
                year = params.get("year", [None])[0]
                if not year:
                    raise ValueError("Missing 'year' parameter")
                result = getTripsByYear(year)

            elif parsed.path == "/getMaxSpeed":
                day = params.get("day", [None])[0]
                if not day:
                    raise ValueError("Missing 'day' parameter")
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
    print("Server HTTP started on :8000")
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
print("[CTRL + C to end]")

threading.Thread(target=sqliteWriterThread, daemon=True).start()
threading.Thread(target=serverThread, daemon=True).start()
threading.Thread(target=commandThread).start()
threading.Thread(target=gpioWatchdogActive, daemon=True).start()
threading.Thread(target=dbWatchdog, daemon=True).start()

try:
    pause()
except KeyboardInterrupt:
    stop_event.set()
finally:
    print("Script ended")
