import sqlite3
from tinydb import TinyDB
import threading

# --- Configurazioni ---
tinydb_path = "pistacchiodbnew2.json"      # percorso TinyDB
sqlite_path = "pistacchio.db"    # percorso SQLite
tripLength = 500  # esempio, usa il valore reale
db_lock = threading.Lock()

# --- Connetti a SQLite ---
con = sqlite3.connect(sqlite_path)
cur = con.cursor()

# --- Crea tabella Trip se non esiste ---
cur.execute("""
CREATE TABLE IF NOT EXISTS Trip (
    type TEXT,
    time REAL,
    data TEXT,
    hour TEXT
)
""")
con.commit()

# --- Leggi dati da TinyDB ---
tiny_db = TinyDB(tinydb_path)
all_records = tiny_db.all()  # lista di dizionari

# --- Inserisci dati in SQLite ---
with db_lock:
    # Prepara i dati come tuple
    sqlite_data = [
        (r["type"], r["time"], r["data"], r["hour"])
        for r in all_records
        if r.get("type") == "trip"  # filtra solo i record "trip"
    ]

    cur.executemany(
        "INSERT INTO Trip (type, time, data, hour) VALUES (?, ?, ?, ?)",
        sqlite_data
    )
    con.commit()

print(f"Inseriti {len(sqlite_data)} record da TinyDB a SQLite.")

con.close()
