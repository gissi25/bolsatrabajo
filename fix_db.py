import sqlite3

db_path = 'app/src/main/assets/si.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

missing_tables = [
    """
    CREATE TABLE IF NOT EXISTS CATEGORIA_HABILIDAD (
        ID_CATEGORIA_HABILIDAD INTEGER PRIMARY KEY NOT NULL,
        NOMBRE_CATEGORIA       VARCHAR(50)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS MUNICIPIO (
        ID_MUNICIPIO     INTEGER PRIMARY KEY NOT NULL,
        ID_DEPARTAMENTO  INTEGER NOT NULL,
        NOMBRE_MUNICIPIO VARCHAR(50),
        FOREIGN KEY (ID_DEPARTAMENTO) REFERENCES DEPARTAMENTO (ID_DEPARTAMENTO)
    )
    """,
    """
    CREATE TABLE IF NOT EXISTS POSTULANTE (
        ID_POSTULANTE      INTEGER PRIMARY KEY NOT NULL,
        ID_USUARIO         INTEGER,
        ID_GENERO          INTEGER NOT NULL,
        ID_DISTRITO        INTEGER,
        ID_TIPO_DOCUMENTO  INTEGER NOT NULL,
        NOMBRE             VARCHAR(100),
        APELLIDO           VARCHAR(100),
        FECHA_NACIMIENTO   DATE,
        NUM_DOCUMENTO      VARCHAR(20),
        NUP                VARCHAR(20),
        DIRECCION_DETALLE  VARCHAR(250),
        TELEFONO_CASA      VARCHAR(15),
        TELEFONO_CELULAR   VARCHAR(15),
        EMAIL              VARCHAR(100),
        FOREIGN KEY (ID_GENERO) REFERENCES GENERO (ID_GENERO),
        FOREIGN KEY (ID_TIPO_DOCUMENTO) REFERENCES TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO),
        FOREIGN KEY (ID_DISTRITO) REFERENCES DISTRITO (ID_DISTRITO)
    )
    """
]

for sql in missing_tables:
    try:
        cursor.execute(sql)
        table_name = sql.split('(')[0].replace('CREATE TABLE IF NOT EXISTS', '').strip()
        print(f'Tabla creada: {table_name}')
    except Exception as e:
        print(f'Error: {e}')

conn.commit()

tables = cursor.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").fetchall()
print(f'\nTotal de tablas: {len(tables)}')
for t in tables:
    print(f'  - {t[0]}')

conn.close()
print('\nBase de datos corregida exitosamente!')