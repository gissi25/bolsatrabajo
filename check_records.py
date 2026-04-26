import sqlite3

conn = sqlite3.connect('C:/Users/Gissi Giron/AndroidStudioProjects/BT/app/src/main/assets/si.db')
cur = conn.cursor()

cur.execute('SELECT name FROM sqlite_master WHERE type=?', ('table',))
tables = [r[0] for r in cur.fetchall()]

for table in tables:
    try:
        cur.execute(f'SELECT COUNT(*) FROM {table}')
        count = cur.fetchone()[0]
        if count > 0:
            print(f'{table}: {count} registros')
    except:
        pass

conn.close()