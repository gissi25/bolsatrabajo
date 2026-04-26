import sqlite3

conn = sqlite3.connect('C:/Users/Gissi Giron/AndroidStudioProjects/BT/app/src/main/assets/si.db')
cur = conn.cursor()

# Add missing triggers that reference FECHA_NACIMIENTO and EMAIL
missing_triggers = [
('TR_POSTULANTE_EDAD', '''CREATE TRIGGER TR_POSTULANTE_EDAD
BEFORE INSERT ON POSTULANTE
FOR EACH ROW
BEGIN
    SELECT RAISE(ABORT, 'El postulante debe ser mayor de 18 años para registrarse.')
    WHERE (STRFTIME('%Y', 'now') - STRFTIME('%Y', NEW.FECHA_NACIMIENTO)) < 18;
END'''),

('TR_POSTULANTE_EMAIL', '''CREATE TRIGGER TR_POSTULANTE_EMAIL
BEFORE INSERT ON POSTULANTE
FOR EACH ROW
BEGIN
    SELECT RAISE(ABORT, 'El formato del correo electronico no es valido.')
    WHERE NEW.EMAIL NOT LIKE '%_@__%.__%';
END'''),

('TR_CASCADA_USUARIO', '''CREATE TRIGGER TR_CASCADA_USUARIO
AFTER DELETE ON POSTULANTE
FOR EACH ROW
BEGIN
    DELETE FROM USUARIO WHERE ID_USUARIO = OLD.ID_USUARIO;
END'''),
]

for name, sql in missing_triggers:
    try:
        cur.execute(f'DROP TRIGGER IF EXISTS {name}')
        cur.execute(sql)
        print(f'OK: {name}')
    except Exception as e:
        print(f'Error {name}: {e}')

conn.commit()
cur.execute('SELECT name FROM sqlite_master WHERE type=?', ('trigger',))
triggers = [r[0] for r in cur.fetchall()]
print(f'Total triggers: {len(triggers)}')
conn.close()