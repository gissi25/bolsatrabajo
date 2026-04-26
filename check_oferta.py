import sqlite3
conn = sqlite3.connect('C:/Users/Gissi Giron/AndroidStudioProjects/BT/app/src/main/assets/si.db')
cur = conn.cursor()
cur.execute('PRAGMA table_info(OFERTA_TRABAJO)')
for c in cur.fetchall():
    print(c)
conn.close()