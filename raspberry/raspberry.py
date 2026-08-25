import time
import threading
import base64
import json
from queue import Queue
from queue import Empty
from datetime import datetime
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from gpiozero import DigitalInputDevice
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
# Debouncing time 10ms (delegato al pin factory via bounce_time)
MIN_TRIP_DT = 0.01
# High level filter, max 1 trip in 200ms on a 28cm diameter wheel (realistic max hamster speed)
MAX_ONE_IN = 0.2
# Ricostruzione periodica del sensore
WATCHDOG_TIMEOUT = 60
# Pausa fra detach della callback e close(): lascia esaurire i callback in volo
GPIO_DETACH_SETTLE = 0.02
# Intervallo di flush del writer SQLite
DB_FLUSH_INTERVAL = 60

# =======================
# GLOBALS
# =======================

db_lock = threading.Lock()
db_queue = Queue()
stop_event = threading.Event()

gpio_lock = threading.RLock()

last_gpio_time = time.time()
last_accepted_ts = 0.0
last_db_write = 0

sensor = None

# =======================
# DB INIT
# =======================
def init_db():
    con = sqlite3.connect(DB_FILE)
    cur = con.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS Trip (
            type TEXT,
            time REAL,
            data TEXT,
            hour TEXT
        )
    """)
    # Indici per velocizzare le query più comuni
    cur.execute("CREATE INDEX IF NOT EXISTS idx_trip_data ON Trip(data)")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_trip_time ON Trip(time)")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_trip_data_hour ON Trip(data, hour)")
    con.commit()
    con.close()

init_db()

# Connessione condivisa per le letture (WAL mode = letture e scritture non si bloccano)
con_read = sqlite3.connect(DB_FILE, check_same_thread=False)
con_read.execute("PRAGMA journal_mode=WAL")
con_read.execute("PRAGMA cache_size=-32000")   # 32MB cache in RAM
con_read.execute("PRAGMA temp_store=MEMORY")
con_read.execute("PRAGMA synchronous=NORMAL")

# =======================
# GPIO
# =======================
def myCounter():
    global last_gpio_time, last_accepted_ts
    try:
        ts = time.time()
        last_gpio_time = ts

        # Filtro di alto livello: scarta i fronti troppo ravvicinati per
        # essere giri reali della ruota. Sostituisce la vecchia logica
        # basata sulla LifoQueue, che poteva perdere il campione di
        # confronto se il writer svuotava la coda nel frattempo.
        if ts - last_accepted_ts < MAX_ONE_IN:
            return
        last_accepted_ts = ts

        data = datetime.fromtimestamp(ts)
        db_queue.put({
            'type': 'trip',
            'time': ts,
            'data': data.strftime("%Y%m%d"),
            'hour': data.strftime("%H:%M")
        })
        print("Pistacchio", ts)

    except Exception as e:
        print("ERRORE GPIO:", e)


def _make_sensor():
    dev = DigitalInputDevice(Digital_PIN, pull_up=True, bounce_time=MIN_TRIP_DT)
    dev.when_activated = myCounter
    return dev


def rebuildSensor(reason=""):
    global sensor, last_gpio_time
    with gpio_lock:
        old = sensor
        if old is not None:
            try:
                old.pin.when_changed = None
            except Exception:
                pass
            time.sleep(GPIO_DETACH_SETTLE)
            try:
                old.close()
            except Exception as e:
                print("⚠️ close() sensore:", e)

        try:
            sensor = _make_sensor()
            last_gpio_time = time.time()
            print(f"✅ GPIO ricreato ({reason})", time.time())
        except Exception as e:
            sensor = None
            print("❌ Impossibile ricreare il GPIO:", e)


with gpio_lock:
    sensor = _make_sensor()


def _thread_excepthook(args):
    print(f"❌ Eccezione in {args.thread.name}: "
          f"{args.exc_type.__name__}: {args.exc_value}")

    files = []
    tb = args.exc_traceback
    while tb:
        files.append(tb.tb_frame.f_code.co_filename)
        tb = tb.tb_next

    if any(("lgpio" in f or "gpiozero" in f) for f in files):
        threading.Thread(
            target=rebuildSensor,
            args=("crash thread GPIO",),
            daemon=True
        ).start()

threading.excepthook = _thread_excepthook

# =======================
# DB WRITER THREAD
# =======================
def _drain_queue():
    batch = []
    while True:
        try:
            item = db_queue.get_nowait()
            batch.append(item)
            db_queue.task_done()
        except Empty:
            break
    return batch


def sqliteWriterThread():
    print("SQLite DB writer started")
    global last_db_write

    con_thread = sqlite3.connect(DB_FILE)
    con_thread.execute("PRAGMA journal_mode=WAL")
    con_thread.execute("PRAGMA synchronous=NORMAL")

    def flush():
        global last_db_write
        batch = _drain_queue()
        if not batch:
            return
        sqlite_data = [
            (row["type"], row["time"], row["data"], row["hour"])
            for row in batch
        ]
        with db_lock:
            con_thread.executemany(
                "INSERT INTO Trip (type, time, data, hour) VALUES (?, ?, ?, ?)",
                sqlite_data
            )
            con_thread.commit()
        last_db_write = time.time()
        print(f"Wrote {len(batch)} events into SQLite")

    while not stop_event.wait(timeout=DB_FLUSH_INTERVAL):
        try:
            flush()
        except Exception as e:
            print("❌ Errore scrittura SQLite:", e)

    # Flush finale: non perdere l'ultimo minuto di eventi allo shutdown
    try:
        flush()
    except Exception as e:
        print("❌ Errore flush finale:", e)

    con_thread.close()
    print("SQLite DB writer stopped")

# =======================
# WATCHDOG THREADS
# =======================
def gpioWatchdogActive():
    while not stop_event.is_set():
        remaining = WATCHDOG_TIMEOUT - (time.time() - last_gpio_time)
        if remaining > 0:
            # ricontrolla al massimo ogni 5s: last_gpio_time può
            # essere aggiornato dalla callback nel frattempo
            if stop_event.wait(timeout=min(remaining, 5)):
                break
            continue
        rebuildSensor("idle 60s")


def dbWatchdog():
    while not stop_event.wait(timeout=5):
        qsize = db_queue.qsize()
        if qsize > 100:
            print(f"⚠️ WATCHDOG DB: queue length = {qsize}")

# =======================
# DB READ FUNCTIONS
# =======================
def _reconnect_read():
    global con_read
    print("⚠️ Riconnessione DB read...")
    try:
        con_read.close()
    except Exception:
        pass
    con_read = sqlite3.connect(DB_FILE, check_same_thread=False)
    con_read.execute("PRAGMA journal_mode=WAL")
    con_read.execute("PRAGMA cache_size=-32000")
    con_read.execute("PRAGMA temp_store=MEMORY")
    con_read.execute("PRAGMA synchronous=NORMAL")

def get_read_cursor():
    """Restituisce un cursore valido, riconnettendo se necessario."""
    global con_read
    try:
        con_read.execute("SELECT 1")
    except (sqlite3.DatabaseError, sqlite3.OperationalError):
        _reconnect_read()
    return con_read.cursor()

def table_exists(name):
    try:
        con_read.execute(f"SELECT 1 FROM {name} LIMIT 1")
        return True
    except sqlite3.OperationalError:
        return False

def getTripsByHour(day, hour):
    # Usa >= e <= invece di LIKE per sfruttare l'indice su (data, hour)
    hour_start = f"{hour}:00"
    hour_end   = f"{hour}:59"
    with db_lock:
        cur = get_read_cursor()
        cur.execute("""
            SELECT COUNT(*)
            FROM Trip
            WHERE data = ?
              AND hour >= ?
              AND hour <= ?
        """, (day, hour_start, hour_end))
        trips = cur.fetchone()[0]
    return {
        'trips': trips,
        'length': trips * tripLength,
        'hour': hour
    }

def getTripsByMonth(req, month):
    # req è tipo "202501" — filtra con >= e <= invece di LIKE
    year  = req[:4]
    mon   = req[4:6]
    start = f"{year}{mon}01"
    end   = f"{year}{mon}99"
    with db_lock:
        cur = get_read_cursor()
        cur.execute("""
            SELECT COUNT(*)
            FROM Trip
            WHERE data >= ? AND data <= ?
        """, (start, end))
        trips = cur.fetchone()[0]
    return {
        'trips': trips,
        'length': trips * tripLength,
        'month': month
    }

def getTripsByDays(days: str) -> list[dict]:
    if not days:
        return []

    json_str = base64.b64decode(days).decode('utf-8')
    dates_array = json.loads(json_str)

    if not dates_array:
        return []

    placeholders = ",".join(["?"] * len(dates_array))

    with db_lock:
        cur = get_read_cursor()
        cur.execute(f"""
            SELECT data, COUNT(*) AS trips
            FROM Trip
            WHERE data IN ({placeholders})
            GROUP BY data
        """, dates_array)
        rows = cur.fetchall()

    trips_dict = {row[0]: row[1] for row in rows}

    return [
        {
            'data': d,
            'trips': trips_dict.get(d, 0),
            'length': trips_dict.get(d, 0) * tripLength
        }
        for d in dates_array
    ]

def getMaxSpeed(day: str) -> dict:
    CIRCUMFERENCE_M = tripLength / 100
    N_LAPS = 50
    MAX_SPEED_M_S = 4.0
    MIN_DT = (CIRCUMFERENCE_M * N_LAPS) / MAX_SPEED_M_S

    with db_lock:
        cur = get_read_cursor()
        cur.execute("""
            SELECT time
            FROM Trip
            WHERE data = ?
            ORDER BY time ASC
        """, (day,))
        rows = cur.fetchall()

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
    return {
        'speed': f"{best['speed']:.2f} m/s",
        'speedKM': f"({best['speedKM']:.2f} km/h)",
        'deltaT': f"deltaT: {best['deltaT']:.2f} s"
    }

def getTripsByDay(day):
    # Una sola query invece di 24 separate
    with db_lock:
        cur = get_read_cursor()
        cur.execute("""
            SELECT SUBSTR(hour, 1, 2) as h, COUNT(*)
            FROM Trip
            WHERE data = ?
            GROUP BY h
        """, (day,))
        rows = dict(cur.fetchall())

    return [
        {
            'trips': rows.get(f"{h:02d}", 0),
            'length': rows.get(f"{h:02d}", 0) * tripLength,
            'hour': f"{h:02d}"
        }
        for h in range(24)
    ]

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

            elif parsed.path == "/health":
                with gpio_lock:
                    sensor_ok = sensor is not None
                    try:
                        pin_value = sensor.value if sensor_ok else None
                    except Exception:
                        pin_value = None
                        sensor_ok = False
                result = {
                    'sensor': sensor_ok,
                    'pin_value': pin_value,
                    'queue': db_queue.qsize(),
                    'last_gpio_time': last_gpio_time,
                    'last_db_write': last_db_write
                }

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
http_server = None

def serverThread():
    global http_server
    http_server = ThreadingHTTPServer(("0.0.0.0", 8000), ApiHandler)
    print("Server HTTP started on :8000")
    http_server.serve_forever()

def commandThread():
    try:
        while not stop_event.is_set():
            cmd = input("Command: ").strip()
            if cmd == "q":
                stop_event.set()
            elif cmd == "t":
                myCounter()
            elif cmd == "r":
                rebuildSensor("manuale")
            time.sleep(0.3)
    except (KeyboardInterrupt, EOFError):
        stop_event.set()

# =======================
# START
# =======================
print("[CTRL + C to end]")

writer = threading.Thread(target=sqliteWriterThread)
writer.start()
threading.Thread(target=serverThread, daemon=True).start()
threading.Thread(target=commandThread, daemon=True).start()
threading.Thread(target=gpioWatchdogActive, daemon=True).start()
threading.Thread(target=dbWatchdog, daemon=True).start()

try:
    while not stop_event.wait(timeout=1):
        pass
except KeyboardInterrupt:
    stop_event.set()
finally:
    stop_event.set()

    # Attende il flush finale del writer prima di chiudere tutto
    writer.join(timeout=10)

    with gpio_lock:
        if sensor is not None:
            try:
                sensor.pin.when_changed = None
            except Exception:
                pass
            time.sleep(GPIO_DETACH_SETTLE)
            try:
                sensor.close()
            except Exception:
                pass

    if http_server is not None:
        try:
            http_server.shutdown()
        except Exception:
            pass

    try:
        con_read.close()
    except Exception:
        pass

    print("Script ended")
