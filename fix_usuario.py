import sqlite3

conn = sqlite3.connect('C:/Users/Gissi Giron/AndroidStudioProjects/BT/app/src/main/assets/si.db')
cur = conn.cursor()

# 1. Drop ALL triggers
cur.execute("SELECT name FROM sqlite_master WHERE type=?", ('trigger',))
for row in cur.fetchall():
    cur.execute(f"DROP TRIGGER IF EXISTS {row[0]}")
print("Triggers eliminados")

# 2. Rename old USUARIO
cur.execute("ALTER TABLE USUARIO RENAME TO USUARIO_OLD")

# 3. Create new USUARIO without ID_POSTULANTE
cur.execute("""CREATE TABLE USUARIO (
    ID_USUARIO INTEGER PRIMARY KEY,
    USERNAME VARCHAR(30) NOT NULL UNIQUE,
    PASSWORD VARCHAR(10) NOT NULL,
    ROL VARCHAR(20) NOT NULL DEFAULT 'postulante'
)""")

# 4. Copy data (ID_POSTULANTE was at index 1)
cur.execute("""INSERT INTO USUARIO (ID_USUARIO, USERNAME, PASSWORD, ROL) 
SELECT ID_USUARIO, USERNAME, PASSWORD, COALESCE(ROL, 'postulante') FROM USUARIO_OLD""")

cur.execute("DROP TABLE USUARIO_OLD")

conn.commit()

# Verify
cur.execute("PRAGMA table_info(USUARIO)")
print("Nueva estructura USUARIO:")
for c in cur.fetchall():
    print(f"  {c[1]} {c[2]}")

cur.execute("SELECT * FROM USUARIO")
print("Users:", cur.fetchall())

conn.close()
print("\nListo!")