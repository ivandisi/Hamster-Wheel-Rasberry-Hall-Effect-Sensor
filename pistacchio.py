import time
import threading
import json
from tinydb import TinyDB, Query
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from gpiozero import Button
from signal import pause

db = TinyDB('pistacchiodbnew.json')
stop_event = threading.Event()
tripLength = 91
Digital_PIN = 22

sensor = Button(Digital_PIN, pull_up=True, bounce_time=0.0009)
sensor.when_pressed = lambda: myCounter()  

def getTripsByYear(year):
    arr = []
    for number in range(1, 13):
        n = f"{number:02d}"
        data = getTripsByMonth(year + n, n)
        arr.append(data)
    return arr

def getTripsByMonth(req, month):
    Qry = Query()
    t = db.search(Qry.data.test(lambda d: d.startswith(req)))
    return {'trips': len(t), 'length': (len(t) * tripLength), 'month': month}

def getTripsByDay(day):
    arr = []
    for number in range(24):
        n = f"{number:02d}"
        data = getTripsByHour(day, n)
        arr.append(data)
    return arr

def getTripsByHour(day, hour):
    Qry = Query()
    data = db.search((Qry.data == day) & Qry.hour.test(lambda h: h.startswith(hour)))
    return {'trips': len(data), 'length': (len(data) * tripLength), 'hour': hour}

def myCounter():
    print('Pistacchio ' + str(time.time()))
    data = datetime.fromtimestamp(time.time())
    db.insert({'type': 'trip', 'time': time.time(), 'data': data.strftime("%Y%m%d"), 'hour': data.strftime("%H:%M")})

class ApiHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        params = parse_qs(parsed_path.query)

        if path == "/getByDay":
            day = params.get("day", [None])[0]
            if not day:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"Parametro 'day' mancante")
                return
            result = getTripsByDay(day)
        elif path == "/getByYear":
            year = params.get("year", [None])[0]
            if not year:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"Parametro 'year' mancante")
                return
            result = getTripsByYear(year)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Endpoint non trovato")
            return

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(result).encode())

def serverThread():
    server = HTTPServer(("0.0.0.0", 8000), ApiHandler)
    print("Server avviato su http://localhost:8000")
    server.serve_forever()

def commandThread():
    try:
        while not stop_event.is_set():
            cmd = input("Command: ").strip()
            if cmd == "q":
                stop_event.set()
            elif cmd == "r":
                cmdTime = input("hour: ")
                Qry = Query()
                print(db.search(Qry.hour == cmdTime))
            elif cmd == "trips":
                cmdTime = input("date for Trips (yyyymmdd): ")
                print(getTripsByDay(cmdTime))
            elif cmd == "mtrips":
                cmdTime = input("year for Trips (yyyy): ")
                print(getTripsByYear(cmdTime))
            elif cmd == "t":
                myCounter()
            elif cmd == "a":
                print(db.all())
            time.sleep(0.5)
    except KeyboardInterrupt:
        stop_event.set()

print('[CTRL + C, to stop the Script!]')

t0 = threading.Thread(target=serverThread, daemon=True)
t2 = threading.Thread(target=commandThread)

t0.start()
t2.start()

try:
    pause() 
except KeyboardInterrupt:
    stop_event.set()
finally:
    print("Script terminato")
