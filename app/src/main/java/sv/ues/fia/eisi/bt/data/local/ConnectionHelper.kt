package sv.ues.fia.eisi.bt.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class ConnectionHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bolsadetabajo.db"
        private const val DATABASE_VERSION = 11
        private const val TAG = "ConnectionHelper"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON;")

        // ================================================================
        // CREATE TABLES (22 tablas con PKs naturales/compuestas/manuales)
        // ================================================================

        // --- Tablas AUTOINCREMENTALES (7) ---

        db.execSQL("""
            CREATE TABLE CATEGORIA_HABILIDAD (
                ID_CATEGORIA_HABILIDAD INTEGER PRIMARY KEY AUTOINCREMENT,
                NOMBRE_CATEGORIA VARCHAR(50)
            )
        """)

        db.execSQL("""
            CREATE TABLE GENERO (
                ID_GENERO INTEGER PRIMARY KEY AUTOINCREMENT,
                NOMBRE_GENERO VARCHAR(20)
            )
        """)

        db.execSQL("""
            CREATE TABLE TIPO_DOCUMENTO (
                ID_TIPO_DOCUMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
                NOMBRE_TIPO VARCHAR(25)
            )
        """)

        db.execSQL("""
            CREATE TABLE DEPARTAMENTO (
                ID_DEPARTAMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
                NOMBRE_DEPARTAMENTO VARCHAR(50)
            )
        """)

        db.execSQL("""
            CREATE TABLE GRADO_ACADEMICO (
                ID_GRADO_ACADEMICO INTEGER PRIMARY KEY AUTOINCREMENT,
                NOMBRE_GRADO VARCHAR(50)
            )
        """)

        db.execSQL("""
            CREATE TABLE RED_SOCIAL (
                ID_RED_SOCIAL INTEGER PRIMARY KEY AUTOINCREMENT,
                NOMBRE_RED VARCHAR(50)
            )
        """)

        db.execSQL("""
            CREATE TABLE USUARIO (
                ID_USUARIO INTEGER PRIMARY KEY AUTOINCREMENT,
                USERNAME VARCHAR(30),
                PASSWORD VARCHAR(128),
                ROL VARCHAR(20)
            )
        """)

        // --- Tablas con PK Compuesta ---

        db.execSQL("""
            CREATE TABLE MUNICIPIO (
                ID_DEPARTAMENTO INTEGER NOT NULL,
                ID_MUNICIPIO INTEGER NOT NULL,
                NOMBRE_MUNICIPIO VARCHAR(50),
                PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO),
                FOREIGN KEY (ID_DEPARTAMENTO) REFERENCES DEPARTAMENTO (ID_DEPARTAMENTO)
            )
        """)

        db.execSQL("""
            CREATE TABLE DISTRITO (
                ID_DEPARTAMENTO INTEGER NOT NULL,
                ID_MUNICIPIO INTEGER NOT NULL,
                ID_DISTRITO INTEGER NOT NULL,
                NOMBRE_DISTRITO VARCHAR(50),
                PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO),
                FOREIGN KEY (ID_DEPARTAMENTO, ID_MUNICIPIO) REFERENCES MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO)
            )
        """)

        db.execSQL("""
            CREATE TABLE OFERTA_TRABAJO (
                NIT VARCHAR(20) NOT NULL,
                ID_OFERTA VARCHAR(10) NOT NULL,
                ID_GRADO_ACADEMICO INTEGER,
                TITULO_PUESTO VARCHAR(150),
                FECHA_PUBLICACION DATE,
                FECHA_CADUCIDAD DATE,
                EXPERIENCIA_ANIOS INTEGER,
                EDAD_MINIMA INTEGER,
                EDAD_MAXIMA INTEGER,
                DESCRIPCION_OFERTA_TRABAJO VARCHAR(5000),
                PRIMARY KEY (NIT, ID_OFERTA),
                FOREIGN KEY (NIT) REFERENCES EMPRESA (NIT),
                FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO)
            )
        """)

        db.execSQL("""
            CREATE TABLE DETALLE_REQUISITO (
                NIT VARCHAR(20) NOT NULL,
                ID_OFERTA VARCHAR(10) NOT NULL,
                ID_DETALLE VARCHAR(10) NOT NULL,
                DESCRIPCION_REQUISITO VARCHAR(100),
                PRIMARY KEY (NIT, ID_OFERTA, ID_DETALLE),
                FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA)
            )
        """)

        db.execSQL("""
            CREATE TABLE EXPERIENCIA_LABORAL (
                ID_POSTULANTE VARCHAR(20) NOT NULL,
                NIT VARCHAR(20) NOT NULL,
                ID_EXPERIENCIA VARCHAR(10) NOT NULL,
                PUESTO_TRABAJO VARCHAR(100),
                FECHA_INICIO DATE,
                FECHA_FIN DATE,
                DESCP_EXPERIENCIA_LABORAL VARCHAR(500),
                CONTACTO_REFERENCIA VARCHAR(100),
                PRIMARY KEY (ID_POSTULANTE, NIT, ID_EXPERIENCIA),
                FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
                FOREIGN KEY (NIT) REFERENCES EMPRESA (NIT)
            )
        """)

        db.execSQL("""
            CREATE TABLE CERTIFICACION (
                ID_CERTIFICACION VARCHAR(10) NOT NULL,
                ID_INSTITUCION VARCHAR(20) NOT NULL,
                ID_POSTULANTE VARCHAR(20) NOT NULL,
                NOMBRE_CERTIFICACION VARCHAR(150),
                FECHA_CERTIFICACION DATE,
                PRIMARY KEY (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE),
                FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
                FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
            )
        """)

        db.execSQL("""
            CREATE TABLE FORMACION_ACADEMICA (
                ID_FORMACION VARCHAR(10) NOT NULL,
                ID_POSTULANTE VARCHAR(20) NOT NULL,
                ID_OFERTA_ACADEMICA VARCHAR(10),
                TITULO_OBTENIDO VARCHAR(150),
                FECHA_OBTENCION DATE,
                PRIMARY KEY (ID_FORMACION, ID_POSTULANTE),
                FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
                FOREIGN KEY (ID_OFERTA_ACADEMICA) REFERENCES OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA)
            )
        """)

        db.execSQL("""
            CREATE TABLE HABILIDAD_POSTULANTE (
                ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
                ID_HABILIDAD VARCHAR(10) NOT NULL,
                ID_POSTULANTE VARCHAR(20) NOT NULL,
                NIVEL_DESTREZA VARCHAR(12),
                PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE),
                FOREIGN KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD) REFERENCES HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
                FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
            )
        """)

        db.execSQL("""
            CREATE TABLE POSTULACION (
                ID_POSTULACION VARCHAR(10) NOT NULL,
                NIT VARCHAR(20) NOT NULL,
                ID_OFERTA VARCHAR(10) NOT NULL,
                ID_POSTULANTE VARCHAR(20) NOT NULL,
                FECHA_APLICACION DATE,
                ESTADO_PROCESO VARCHAR(50),
                PRIMARY KEY (ID_POSTULACION),
                FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA),
                FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
            )
        """)

        db.execSQL("""
            CREATE TABLE RED_SOCIAL_POSTULANTE (
                ID_POSTULANTE VARCHAR(20) NOT NULL,
                ID_RED_SOCIAL INTEGER NOT NULL,
                URL_PERFIL VARCHAR(100),
                PRIMARY KEY (ID_POSTULANTE, ID_RED_SOCIAL),
                FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
                FOREIGN KEY (ID_RED_SOCIAL) REFERENCES RED_SOCIAL (ID_RED_SOCIAL)
            )
        """)

        // --- Tablas con PK Manual (VARCHAR) ---

        db.execSQL("""
            CREATE TABLE INSTITUCION (
                ID_INSTITUCION VARCHAR(20) PRIMARY KEY,
                NOMBRE_INSTITUCION VARCHAR(150)
            )
        """)

        db.execSQL("""
            CREATE TABLE HABILIDAD (
                ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
                ID_HABILIDAD VARCHAR(10) NOT NULL,
                NOMBRE_HABILIDAD VARCHAR(100),
                PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
                FOREIGN KEY (ID_CATEGORIA_HABILIDAD) REFERENCES CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD)
            )
        """)

        db.execSQL("""
            CREATE TABLE EMPRESA (
                NIT VARCHAR(20) PRIMARY KEY,
                ID_DISTRITO_DEPTO INTEGER NOT NULL,
                ID_DISTRITO_MUNICIPIO INTEGER NOT NULL,
                ID_DISTRITO_ID INTEGER NOT NULL,
                NOMBRE_EMPRESA VARCHAR(150),
                CONTACTO_DIRECTO VARCHAR(100),
                FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
            )
        """)

        db.execSQL("""
            CREATE TABLE POSTULANTE (
                ID_POSTULANTE VARCHAR(20) PRIMARY KEY,
                ID_GENERO INTEGER NOT NULL,
                ID_TIPO_DOCUMENTO INTEGER NOT NULL,
                ID_DISTRITO_DEPTO INTEGER,
                ID_DISTRITO_MUNICIPIO INTEGER,
                ID_DISTRITO_ID INTEGER,
                NOMBRE VARCHAR(100),
                APELLIDO VARCHAR(100),
                FECHA_NACIMIENTO DATE,
                NUM_DOCUMENTO VARCHAR(20),
                NUP VARCHAR(20),
                DIRECCION_DETALLE VARCHAR(250),
                TELEFONO_CASA VARCHAR(15),
                TELEFONO_CELULAR VARCHAR(15),
                EMAIL VARCHAR(100),
                FOREIGN KEY (ID_GENERO) REFERENCES GENERO (ID_GENERO),
                FOREIGN KEY (ID_TIPO_DOCUMENTO) REFERENCES TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO),
                FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
            )
        """)

        db.execSQL("""
            CREATE TABLE OFERTA_ACADEMICA (
                ID_OFERTA_ACADEMICA VARCHAR(10) PRIMARY KEY,
                ID_GRADO_ACADEMICO INTEGER,
                ID_INSTITUCION VARCHAR(20),
                FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
                FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO)
            )
        """)

        // ================================================================
        // INDICES
        // ================================================================

        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_MUNICIPIO_DEPTO ON MUNICIPIO (ID_DEPARTAMENTO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_DISTRITO_MUNICIPIO ON DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_HABILIDAD_CATEGORIA ON HABILIDAD (ID_CATEGORIA_HABILIDAD)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_EMPRESA_DISTRITO ON EMPRESA (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_POSTULANTE_GENERO ON POSTULANTE (ID_GENERO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_POSTULANTE_TIPO_DOC ON POSTULANTE (ID_TIPO_DOCUMENTO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_POSTULANTE_DISTRITO ON POSTULANTE (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_OFERTA_GRADO ON OFERTA_TRABAJO (ID_GRADO_ACADEMICO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_DETALLE_OFERTA ON DETALLE_REQUISITO (NIT, ID_OFERTA)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_EXP_POSTULANTE ON EXPERIENCIA_LABORAL (ID_POSTULANTE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_EXP_EMPRESA ON EXPERIENCIA_LABORAL (NIT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_CERT_POSTULANTE ON CERTIFICACION (ID_POSTULANTE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_CERT_INSTITUCION ON CERTIFICACION (ID_INSTITUCION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_FORM_POSTULANTE ON FORMACION_ACADEMICA (ID_POSTULANTE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_HAB_POST_POSTULANTE ON HABILIDAD_POSTULANTE (ID_POSTULANTE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_HAB_POST_HABILIDAD ON HABILIDAD_POSTULANTE (ID_HABILIDAD)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_POSTULACION_POSTULANTE ON POSTULACION (ID_POSTULANTE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_POSTULACION_OFERTA ON POSTULACION (NIT, ID_OFERTA)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_RED_POST_POSTULANTE ON RED_SOCIAL_POSTULANTE (ID_POSTULANTE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_OA_INSTITUCION ON OFERTA_ACADEMICA (ID_INSTITUCION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS IDX_OA_GRADO ON OFERTA_ACADEMICA (ID_GRADO_ACADEMICO)")

        // ================================================================
        // TRIGGERS SEMANTICOS (6)
        // ================================================================

        db.execSQL("DROP TRIGGER IF EXISTS TR_POSTULANTE_EDAD")
        db.execSQL("""
            CREATE TRIGGER TR_POSTULANTE_EDAD BEFORE INSERT ON POSTULANTE
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.FECHA_NACIMIENTO > date('now')
                THEN RAISE(ABORT, 'La fecha de nacimiento no puede ser futura') END;
                SELECT CASE WHEN (strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)) < 18
                THEN RAISE(ABORT, 'El postulante debe ser mayor de edad') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_POSTULANTE_EDAD_UPD BEFORE UPDATE ON POSTULANTE
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.FECHA_NACIMIENTO > date('now')
                THEN RAISE(ABORT, 'La fecha de nacimiento no puede ser futura') END;
                SELECT CASE WHEN (strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)) < 18
                THEN RAISE(ABORT, 'El postulante debe ser mayor de edad') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_OFERTA_RANGO_EDAD")
        db.execSQL("""
            CREATE TRIGGER TR_OFERTA_RANGO_EDAD BEFORE INSERT ON OFERTA_TRABAJO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.EDAD_MINIMA < 18
                THEN RAISE(ABORT, 'Edad minima debe ser mayor o igual a 18') END;
                SELECT CASE WHEN NEW.EDAD_MINIMA > NEW.EDAD_MAXIMA
                THEN RAISE(ABORT, 'Edad minima no puede ser mayor a la maxima') END;
            END
        """)
        db.execSQL("DROP TRIGGER IF EXISTS TR_OFERTA_RANGO_EDAD_UPD")
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_OFERTA_RANGO_EDAD_UPD BEFORE UPDATE ON OFERTA_TRABAJO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.EDAD_MINIMA < 18
                THEN RAISE(ABORT, 'Edad minima debe ser mayor o igual a 18') END;
                SELECT CASE WHEN NEW.EDAD_MINIMA > NEW.EDAD_MAXIMA
                THEN RAISE(ABORT, 'Edad minima no puede ser mayor a la maxima') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_OFERTA_VIGENCIA")
        db.execSQL("""
            CREATE TRIGGER TR_OFERTA_VIGENCIA BEFORE INSERT ON OFERTA_TRABAJO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.FECHA_CADUCIDAD <= NEW.FECHA_PUBLICACION
                THEN RAISE(ABORT, 'La oferta ya caduco o fecha invalida') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_OFERTA_VIGENCIA_UPD BEFORE UPDATE ON OFERTA_TRABAJO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.FECHA_CADUCIDAD <= NEW.FECHA_PUBLICACION
                THEN RAISE(ABORT, 'La oferta ya caduco o fecha invalida') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_POSTULACION_VIGENCIA")
        db.execSQL("""
            CREATE TRIGGER TR_POSTULACION_VIGENCIA BEFORE INSERT ON POSTULACION
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (
                    SELECT FECHA_CADUCIDAD FROM OFERTA_TRABAJO
                    WHERE NIT = NEW.NIT AND ID_OFERTA = NEW.ID_OFERTA
                ) < date('now')
                THEN RAISE(ABORT, 'La oferta de trabajo ha vencido') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_EXP_LABORAL_FECHAS")
        db.execSQL("""
            CREATE TRIGGER TR_EXP_LABORAL_FECHAS BEFORE INSERT ON EXPERIENCIA_LABORAL
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.FECHA_INICIO >= NEW.FECHA_FIN
                THEN RAISE(ABORT, 'Fecha inicio debe ser menor a fecha fin') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_EXP_LABORAL_FECHAS_UPD BEFORE UPDATE ON EXPERIENCIA_LABORAL
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.FECHA_INICIO >= NEW.FECHA_FIN
                THEN RAISE(ABORT, 'Fecha inicio debe ser menor a fecha fin') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_HABILIDAD_NIVEL")
        db.execSQL("""
            CREATE TRIGGER TR_HABILIDAD_NIVEL BEFORE INSERT ON HABILIDAD_POSTULANTE
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.NIVEL_DESTREZA NOT IN ('Básico', 'Intermedio', 'Avanzado')
                THEN RAISE(ABORT, 'Nivel de destreza debe ser Basico, Intermedio o Avanzado') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_HABILIDAD_NIVEL_UPD BEFORE UPDATE ON HABILIDAD_POSTULANTE
            FOR EACH ROW BEGIN
                SELECT CASE WHEN NEW.NIVEL_DESTREZA NOT IN ('Básico', 'Intermedio', 'Avanzado')
                THEN RAISE(ABORT, 'Nivel de destreza debe ser Basico, Intermedio o Avanzado') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_USUARIO_FORMATO")
        db.execSQL("""
            CREATE TRIGGER TR_USUARIO_FORMATO BEFORE INSERT ON USUARIO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN LENGTH(NEW.PASSWORD) < 8
                THEN RAISE(ABORT, 'Password minimo 8 caracteres') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_USUARIO_FORMATO_UPD BEFORE UPDATE ON USUARIO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN LENGTH(NEW.PASSWORD) < 8
                THEN RAISE(ABORT, 'Password minimo 8 caracteres') END;
            END
        """)

        // ================================================================
        // TRIGGERS DE CASCADA (3)
        // ================================================================

        db.execSQL("DROP TRIGGER IF EXISTS TR_DEL_DEPARTAMENTO")
        db.execSQL("""
            CREATE TRIGGER TR_DEL_DEPARTAMENTO BEFORE DELETE ON DEPARTAMENTO
            FOR EACH ROW BEGIN
                DELETE FROM MUNICIPIO WHERE ID_DEPARTAMENTO = OLD.ID_DEPARTAMENTO;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_DEL_CATEGORIA")
        db.execSQL("""
            CREATE TRIGGER TR_DEL_CATEGORIA BEFORE DELETE ON CATEGORIA_HABILIDAD
            FOR EACH ROW BEGIN
                DELETE FROM HABILIDAD_POSTULANTE WHERE ID_CATEGORIA_HABILIDAD = OLD.ID_CATEGORIA_HABILIDAD;
                DELETE FROM HABILIDAD WHERE ID_CATEGORIA_HABILIDAD = OLD.ID_CATEGORIA_HABILIDAD;
            END
        """)


        db.execSQL("DROP TRIGGER IF EXISTS TR_DEL_POSTULANTE")
        db.execSQL("""
            CREATE TRIGGER TR_DEL_POSTULANTE BEFORE DELETE ON POSTULANTE
            FOR EACH ROW BEGIN
                DELETE FROM POSTULACION WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
                DELETE FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
                DELETE FROM FORMACION_ACADEMICA WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
                DELETE FROM CERTIFICACION WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
                DELETE FROM HABILIDAD_POSTULANTE WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
                DELETE FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
            END
        """)

        // ================================================================
        // TRIGGERS DE INTEGRIDAD REFERENCIAL (5)
        // ================================================================

        db.execSQL("DROP TRIGGER IF EXISTS TR_MUNICIPIO_DEPTO")
        db.execSQL("""
            CREATE TRIGGER TR_MUNICIPIO_DEPTO BEFORE INSERT ON MUNICIPIO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM DEPARTAMENTO WHERE ID_DEPARTAMENTO = NEW.ID_DEPARTAMENTO) IS NULL
                THEN RAISE(ABORT, 'El departamento asociado no existe') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_MUNICIPIO_DEPTO_UPD BEFORE UPDATE ON MUNICIPIO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM DEPARTAMENTO WHERE ID_DEPARTAMENTO = NEW.ID_DEPARTAMENTO) IS NULL
                THEN RAISE(ABORT, 'El departamento asociado no existe') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_DISTRITO_MUNICIPIO")
        db.execSQL("""
            CREATE TRIGGER TR_DISTRITO_MUNICIPIO BEFORE INSERT ON DISTRITO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM MUNICIPIO WHERE ID_DEPARTAMENTO = NEW.ID_DEPARTAMENTO AND ID_MUNICIPIO = NEW.ID_MUNICIPIO) IS NULL
                THEN RAISE(ABORT, 'El municipio asociado no existe') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_DISTRITO_MUNICIPIO_UPD BEFORE UPDATE ON DISTRITO
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM MUNICIPIO WHERE ID_DEPARTAMENTO = NEW.ID_DEPARTAMENTO AND ID_MUNICIPIO = NEW.ID_MUNICIPIO) IS NULL
                THEN RAISE(ABORT, 'El municipio asociado no existe') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_HABILIDAD_CATEGORIA")
        db.execSQL("""
            CREATE TRIGGER TR_HABILIDAD_CATEGORIA BEFORE INSERT ON HABILIDAD
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM CATEGORIA_HABILIDAD WHERE ID_CATEGORIA_HABILIDAD = NEW.ID_CATEGORIA_HABILIDAD) IS NULL
                THEN RAISE(ABORT, 'La categoria asociada no existe') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_HABILIDAD_CATEGORIA_UPD BEFORE UPDATE ON HABILIDAD
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM CATEGORIA_HABILIDAD WHERE ID_CATEGORIA_HABILIDAD = NEW.ID_CATEGORIA_HABILIDAD) IS NULL
                THEN RAISE(ABORT, 'La categoria asociada no existe') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_EMPRESA_DISTRITO")
        db.execSQL("""
            CREATE TRIGGER TR_EMPRESA_DISTRITO BEFORE INSERT ON EMPRESA
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM DISTRITO WHERE ID_DEPARTAMENTO = NEW.ID_DISTRITO_DEPTO AND ID_MUNICIPIO = NEW.ID_DISTRITO_MUNICIPIO AND ID_DISTRITO = NEW.ID_DISTRITO_ID) IS NULL
                THEN RAISE(ABORT, 'El distrito asociado no existe') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_EMPRESA_DISTRITO_UPD BEFORE UPDATE ON EMPRESA
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM DISTRITO WHERE ID_DEPARTAMENTO = NEW.ID_DISTRITO_DEPTO AND ID_MUNICIPIO = NEW.ID_DISTRITO_MUNICIPIO AND ID_DISTRITO = NEW.ID_DISTRITO_ID) IS NULL
                THEN RAISE(ABORT, 'El distrito asociado no existe') END;
            END
        """)

        db.execSQL("DROP TRIGGER IF EXISTS TR_POSTULANTE_FK")
        db.execSQL("""
            CREATE TRIGGER TR_POSTULANTE_FK BEFORE INSERT ON POSTULANTE
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM GENERO WHERE ID_GENERO = NEW.ID_GENERO) IS NULL
                THEN RAISE(ABORT, 'El genero asociado no existe') END;
                SELECT CASE WHEN (SELECT 1 FROM TIPO_DOCUMENTO WHERE ID_TIPO_DOCUMENTO = NEW.ID_TIPO_DOCUMENTO) IS NULL
                THEN RAISE(ABORT, 'El tipo de documento asociado no existe') END;
            END
        """)
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS TR_POSTULANTE_FK_UPD BEFORE UPDATE ON POSTULANTE
            FOR EACH ROW BEGIN
                SELECT CASE WHEN (SELECT 1 FROM GENERO WHERE ID_GENERO = NEW.ID_GENERO) IS NULL
                THEN RAISE(ABORT, 'El genero asociado no existe') END;
                SELECT CASE WHEN (SELECT 1 FROM TIPO_DOCUMENTO WHERE ID_TIPO_DOCUMENTO = NEW.ID_TIPO_DOCUMENTO) IS NULL
                THEN RAISE(ABORT, 'El tipo de documento asociado no existe') END;
            END
        """)

        Log.d(TAG, "Base de datos creada: 22 tablas, 22 indices, 14 triggers")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Actualizando BD de version $oldVersion a $newVersion")
        db.execSQL("PRAGMA foreign_keys = OFF;")
        dropAllTables(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
        onCreate(db)
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        val tables = listOf(
            "RED_SOCIAL_POSTULANTE", "POSTULACION", "HABILIDAD_POSTULANTE",
            "FORMACION_ACADEMICA", "EXPERIENCIA_LABORAL", "CERTIFICACION",
            "OFERTA_ACADEMICA", "DETALLE_REQUISITO", "OFERTA_TRABAJO",
            "HABILIDAD", "USUARIO", "POSTULANTE", "EMPRESA",
            "RED_SOCIAL", "GRADO_ACADEMICO", "INSTITUCION",
            "DISTRITO", "MUNICIPIO", "DEPARTAMENTO",
            "TIPO_DOCUMENTO", "GENERO", "CATEGORIA_HABILIDAD"
        )
        for (table in tables) {
            db.execSQL("DROP TABLE IF EXISTS $table")
        }
    }

    val writableDb: SQLiteDatabase
        get() = writableDatabase

    val readableDb: SQLiteDatabase
        get() = readableDatabase

    fun getCount(tableName: String): Int {
        var count = 0
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun executeQuery(query: String): List<List<Any>> {
        val results = mutableListOf<List<Any>>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(query, null)
            while (cursor.moveToNext()) {
                val row = mutableListOf<Any>()
                for (i in 0 until cursor.columnCount) {
                    when {
                        cursor.isNull(i) -> row.add("")
                        else -> row.add(cursor.getString(i) ?: "")
                    }
                }
                results.add(row)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun executeInsert(query: String): Long {
        var id = -1L
        try {
            val db = writableDatabase
            db.execSQL(query)
            val cursor = db.rawQuery("SELECT last_insert_rowid()", null)
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    fun executeUpdate(query: String): Int {
        var rows = 0
        try {
            val db = writableDatabase
            db.execSQL(query)
            rows = 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    fun executeDelete(query: String): Int {
        var rows = 0
        try {
            val db = writableDatabase
            db.execSQL(query)
            rows = 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    fun search(tableName: String, column: String, value: String): List<List<Any>> {
        val query = "SELECT * FROM $tableName WHERE $column LIKE '%$value%'"
        return executeQuery(query)
    }

    fun getAll(tableName: String): List<List<Any>> {
        val query = "SELECT * FROM $tableName"
        return executeQuery(query)
    }

    fun getById(tableName: String, idName: String, idValue: String): List<List<Any>> {
        val query = "SELECT * FROM $tableName WHERE $idName = '$idValue'"
        return executeQuery(query)
    }

    fun deleteById(tableName: String, idName: String, idValue: String): Int {
        val query = "DELETE FROM $tableName WHERE $idName = '$idValue'"
        return executeDelete(query)
    }
}
