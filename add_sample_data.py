import sqlite3
import os

db_path = os.path.join(os.path.dirname(__file__), 'app', 'src', 'main', 'assets', 'si.db')

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

genero_data = [
    (1, 'Masculino'),
    (2, 'Femenino'),
    (3, 'Otro')
]

tipo_documento_data = [
    (1, 'DUI'),
    (2, 'Pasaporte'),
    (3, 'Carnet de Menor')
]

for data in genero_data:
    try:
        cursor.execute("INSERT OR IGNORE INTO GENERO (ID_GENERO, NOMBRE_GENERO) VALUES (?, ?)", data)
    except:
        pass

for data in tipo_documento_data:
    try:
        cursor.execute("INSERT OR IGNORE INTO TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO, NOMBRE_TIPO) VALUES (?, ?)", data)
    except:
        pass

conn.commit()

print("Sample data added successfully!")

cursor.execute("SELECT COUNT(*) FROM GENERO")
print(f"GENERO records: {cursor.fetchone()[0]}")

cursor.execute("SELECT COUNT(*) FROM TIPO_DOCUMENTO")
print(f"TIPO_DOCUMENTO records: {cursor.fetchone()[0]}")

conn.close()