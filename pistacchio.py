import RPi.GPIO as GPIO
import time
import threading
import json
from time import sleep
from tinydb import TinyDB, Query
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

db = TinyDB('pistacchiodb.json')
stop_event = threading.Event()
tripLength = 29

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
            
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(result).encode())

        elif path == "/getByYear":
            year = params.get("year", [None])[0]
            if not year:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"Parametro 'year' mancante")
                return

            result = getTripsByYear(year)

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(result).encode())
            
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Endpoint non trovato")

def getTripsByYear(year):
  arr = []
  for number in range(1, 13):
    n = f"{number:02d}"
    data = getTripsByMonth(year + n)
    arr.append(data)
  return arr
  
def getTripsByMonth(month):
  Qry = Query()
  t = db.search(Qry.data.test(lambda d: d.startswith(month)))
  return {'trips': len(t), 'length': (len(t) * tripLength)}

def getTripsByMinute(day, hour):
  Qry = Query()
  data = db.search((Qry.data == day) &  Qry.hour.test(lambda h: h.startswith(hour)))
  return data

def getTripsByDay(day):
  arr = []
  for number in range(24):
    n = f"{number:02d}"
    data = getTripsByHour(day, n)
    arr.append(data)
  return arr
  
def getTripsByHour(day, hour):
  Qry = Query()
  data = db.search((Qry.data == day) &  Qry.hour.test(lambda h: h.startswith(hour)))
  return {'trips': len(data), 'length': (len(data) * tripLength)}

def myCounter():
  print('Pistacchio ' + str(time.time()))
  data = datetime.fromtimestamp(time.time())
  db.insert({'type': 'trip', 'time': time.time(), 'data': data.strftime("%Y%m%d"), 'hour': data.strftime("%H:%M")})

def gpioThread():
  try:
    while not stop_event.is_set():
      if GPIO.input(Digital_PIN):
        myCounter()
      sleep(0.025)
  finally:
    GPIO.cleanup()

def serverThread():
  server = HTTPServer(("0.0.0.0", 8000), ApiHandler)
  print("Server avviato su http://localhost:8000")
  server.serve_forever()
  
def commandThread():
  try:
    while not stop_event.is_set():
      cmd = input("Command: ")
      cmd = cmd.strip()
      
      if cmd == "q":
        stop_event.set()
      if cmd == "r":
        cmdTime = input("hour: ")
        Qry = Query()
        print(db.search(Qry.hour == cmdTime))
      if cmd == "trips":
        cmdTime = input("date for Trips (yyyymmdd): ")
        print(getTripsByDay(cmdTime))
      if cmd == "mtrips":
        cmdTime = input("year for Trips (yyyy): ")
        print(getTripsByYear(cmdTime))
      if cmd == "t":
        myCounter(trips)
      if cmd == "a":
        print(db.all())
        
      sleep(0.5)
      
  except KeyboardInterrupt:
    print('\nScript end! total trips ' + str(trips))
  finally:
    stop_event.set()   
    GPIO.cleanup()
    
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

Digital_PIN = 22
trips = 0;

GPIO.setup(Digital_PIN, GPIO.IN)

print('[CTRL + C, to stop the Script!]')

t0 = threading.Thread(target=serverThread, daemon=True)
t1 = threading.Thread(target=gpioThread)
t2 = threading.Thread(target=commandThread)

t0.start()
t1.start()
t2.start()

t0.join()
t1.join()
t2.join()


