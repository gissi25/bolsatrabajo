# FICHA TÉCNICA — Bolsa de Trabajo (BT)

## 1. Resumen Ejecutivo

| Propiedad | Valor |
|-----------|-------|
| **Nombre del Proyecto** | BT — Bolsa de Trabajo en Línea |
| **Plataforma** | Android (Kotlin) |
| **Arquitectura** | MVVM + Repository Pattern |
| **Base de Datos** | SQLite local (`bolsadetabajo.db`, versión 14) |
| **Tablas** | 23 |
| **Índices** | 23 |
| **Triggers** | 27 (6 semánticos + 16 bloqueantes de borrado + 5 de integridad referencial) |
| **Roles** | Administrador, Postulante, Gerente de Empresa |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 (Android 16) |
| **Lenguaje** | Kotlin 100% |
| **Gradle** | 9.3.1 / AGP 9.1.1 |
| **Paquete** | `sv.ues.fia.eisi.bt` |

**Propósito:** Sistema de intermediación laboral que permite a postulantes registrar su perfil profesional completo (experiencia, formación académica, certificaciones, habilidades técnicas) y postularse a ofertas de trabajo publicadas por empresas. Los gerentes de empresa publican ofertas laborales con requisitos parametrizables. El administrador gestiona todos los catálogos y datos del sistema.

---

## 2. Arquitectura del Proyecto

### 2.1 Estructura MVVM

```
[Fragment] ←→ [ViewModel] ←→ [Repository] ←→ [ConnectionHelper/SQLiteDB]
     ↓              ↓
[Adapter]      [LiveData]
     ↓
[RecyclerView]
```

**Flujo de datos:**
1. El Fragment observa `LiveData` del ViewModel
2. El ViewModel usa corrutinas (`Dispatchers.IO`) para operaciones asíncronas
3. El Repository contiene la lógica de negocio (CRUD, autenticación, validación de duplicados)
4. ConnectionHelper es el `SQLiteOpenHelper` que crea la BD con tablas, índices y triggers

### 2.2 Árbol Completo de Archivos

```
app/src/main/java/sv/ues/fia/eisi/bt/
│
├── BTApplication.kt                  # Application class (tema claro por defecto)
├── MainActivity.kt                    # Single Activity con NavHostFragment
│
├── data/
│   ├── local/
│   │   └── ConnectionHelper.kt        # SQLiteOpenHelper: 23 tablas, 23 índices, 27 triggers
│   └── repository/
│       ├── MainRepository.kt          # Repositorio central (1069 líneas): CRUD genérico, auth, seed, búsquedas JOIN
│       └── SeedData.kt                # Datos de prueba para 12 tablas (262 distritos, 44 municipios, etc.)
│
├── ui/
│   ├── auth/
│   │   ├── LoginFragment.kt           # Pantalla de inicio de sesión
│   │   └── RegisterFragment.kt        # Pantalla de registro con selección de rol
│   ├── crud/
│   │   ├── TableDetailFragment.kt     # Vista de lista con búsqueda por tabla
│   │   ├── TableAdapter.kt            # Adaptador genérico con renderizado específico por tabla
│   │   ├── EditorDialogFragment.kt    # Diálogo dinámico para crear/editar/ver (1716 líneas)
│   │   └── DeleteConfirmDialog.kt     # Diálogo de confirmación con árbol de dependencias
│   └── dashboard/
│       ├── DashboardFragment.kt       # Dashboard con grid de tarjetas por tabla
│       └── DashboardAdapter.kt        # Adaptador con DiffUtil para el grid
│
├── utils/
│   ├── Constants.kt                   # Constantes: tablas, columnas, PKs, roles, niveles de acceso
│   ├── InputMaskUtils.kt              # Máscaras: DUI, NIT, teléfono, período
│   ├── PasswordHasher.kt              # Hashing PBKDF2 con fallback SHA-256
│   ├── StyledToast.kt                 # Toast personalizado con colores del tema
│   ├── ThemeToggleHelper.kt           # Alternar tema claro/oscuro
│   ├── TriggerErrorTranslator.kt      # Traducción de errores SQL → mensajes amigables
│   └── ValidationRules.kt             # Reglas de validación por tabla/campo con validación de fechas futuras
│
└── viewmodel/
    ├── AuthViewModel.kt               # Login + Register con corrutinas
    ├── CrudViewModel.kt               # CRUD + dependencias con Resource sealed class
    └── DashboardViewModel.kt          # Carga de tablas + seed data + filtro por rol
```

### 2.3 Librerías y Dependencias

| Librería | Versión | Propósito |
|----------|---------|-----------|
| `androidx.core:core-ktx` | 1.15.0 | Extensiones Kotlin para Android |
| `androidx.appcompat:appcompat` | 1.7.0 | Compatibilidad hacia atrás |
| `com.google.android.material:material` | 1.12.0 | Material Design 3 (CardView, DatePicker, AutoCompleteTextView,Dialog) |
| `androidx.constraintlayout:constraintlayout` | 2.2.1 | Layouts flexibles |
| `androidx.navigation:navigation-fragment-ktx` | 2.8.9 | Navegación entre fragmentos |
| `androidx.navigation:navigation-ui-ktx` | 2.8.9 | UI de navegación |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.8.7 | ViewModel + LiveData |
| `androidx.lifecycle:lifecycle-livedata-ktx` | 2.8.7 | LiveData reactivo |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Operaciones asíncronas en background |
| `androidx.recyclerview:recyclerview` | 1.4.0 | Listas virtualizadas con DiffUtil |
| `androidx.activity:activity-ktx` | 1.10.1 | Activity Result API |
| `androidx.fragment:fragment-ktx` | 1.8.6 | Fragment KTX extensions |

---

## 3. Base de Datos — 23 Tablas

### 3.1 Listado Completo de las 23 Tablas con sus PK

#### Grupo A — PK AUTOINCREMENTAL (8 tablas)
El ID se genera automáticamente al insertar.

| # | Tabla | Primary Key | Tipo PK |
|---|-------|-------------|---------|
| 1 | `CATEGORIA_HABILIDAD` | ID_CATEGORIA_HABILIDAD | INTEGER AUTOINCREMENT |
| 2 | `GENERO` | ID_GENERO | INTEGER AUTOINCREMENT |
| 3 | `TIPO_DOCUMENTO` | ID_TIPO_DOCUMENTO | INTEGER AUTOINCREMENT |
| 4 | `DEPARTAMENTO` | ID_DEPARTAMENTO | INTEGER AUTOINCREMENT |
| 5 | `GRADO_ACADEMICO` | ID_GRADO_ACADEMICO | INTEGER AUTOINCREMENT |
| 6 | `RED_SOCIAL` | ID_RED_SOCIAL | INTEGER AUTOINCREMENT |
| 7 | `TIPO_CERTIFICACION` | ID_TIPO_CERTIFICACION | INTEGER AUTOINCREMENT |
| 8 | `USUARIO` | ID_USUARIO | INTEGER AUTOINCREMENT |

#### Grupo B — PK Compuesta (9 tablas)
La clave primaria está formada por 2 o 3 columnas.

| # | Tabla | Primary Key | Columnas |
|---|-------|-------------|----------|
| 9 | `MUNICIPIO` | (ID_DEPARTAMENTO, ID_MUNICIPIO) | 2 columnas |
| 10 | `DISTRITO` | (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO) | 3 columnas |
| 11 | `OFERTA_TRABAJO` | (NIT, ID_OFERTA) | 2 columnas |
| 12 | `DETALLE_REQUISITO` | (NIT, ID_OFERTA, ID_DETALLE) | 3 columnas |
| 13 | `EXPERIENCIA_LABORAL` | (ID_POSTULANTE, NIT, ID_EXPERIENCIA) | 3 columnas |
| 14 | `CERTIFICACION` | (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE) | 3 columnas |
| 15 | `FORMACION_ACADEMICA` | (ID_FORMACION, ID_POSTULANTE) | 2 columnas |
| 16 | `HABILIDAD_POSTULANTE` | (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE) | 3 columnas |
| 17 | `RED_SOCIAL_POSTULANTE` | (ID_POSTULANTE, ID_RED_SOCIAL) | 2 columnas |

#### Grupo C — PK Simple VARCHAR (4 tablas)
La clave primaria es un VARCHAR de una sola columna, ingresado manualmente.

| # | Tabla | Primary Key |
|---|-------|-------------|
| 18 | `INSTITUCION` | ID_INSTITUCION VARCHAR(20) |
| 19 | `EMPRESA` | NIT VARCHAR(20) |
| 20 | `POSTULANTE` | ID_POSTULANTE VARCHAR(20) |
| 21 | `OFERTA_ACADEMICA` | ID_OFERTA_ACADEMICA VARCHAR(10) |

#### Grupo D — PK Compuesta Manual (1 tabla)
PK compuesta pero con valores VARCHAR ingresados manualmente.

| # | Tabla | Primary Key |
|---|-------|-------------|
| 22 | `HABILIDAD` | (ID_CATEGORIA_HABILIDAD INTEGER, ID_HABILIDAD VARCHAR(10)) |

#### Grupo E — PK Simple con FK Compuestas (1 tabla)

| # | Tabla | Primary Key | Nota |
|---|-------|-------------|------|
| 23 | `POSTULACION` | ID_POSTULACION VARCHAR(10) | PK simple, pero sus FK apuntan a PKs compuestas (NIT, ID_OFERTA) |

### 3.2 DDL Completo de Cada Tabla

```sql
-- ========================================
-- TABLAS AUTOINCREMENTALES
-- ========================================

CREATE TABLE CATEGORIA_HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_CATEGORIA VARCHAR(50)
);

CREATE TABLE GENERO (
    ID_GENERO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_GENERO VARCHAR(20)
);

CREATE TABLE TIPO_DOCUMENTO (
    ID_TIPO_DOCUMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_TIPO VARCHAR(25)
);

CREATE TABLE DEPARTAMENTO (
    ID_DEPARTAMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_DEPARTAMENTO VARCHAR(50)
);

CREATE TABLE GRADO_ACADEMICO (
    ID_GRADO_ACADEMICO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_GRADO VARCHAR(50)
);

CREATE TABLE RED_SOCIAL (
    ID_RED_SOCIAL INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_RED VARCHAR(50)
);

CREATE TABLE TIPO_CERTIFICACION (
    ID_TIPO_CERTIFICACION INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_TIPO VARCHAR(100)
);

CREATE TABLE USUARIO (
    ID_USUARIO INTEGER PRIMARY KEY AUTOINCREMENT,
    USERNAME VARCHAR(30),
    PASSWORD VARCHAR(128),
    ROL VARCHAR(20)
);

-- ========================================
-- TABLAS CON PK COMPUESTA
-- ========================================

CREATE TABLE MUNICIPIO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    NOMBRE_MUNICIPIO VARCHAR(50),
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO),
    FOREIGN KEY (ID_DEPARTAMENTO) REFERENCES DEPARTAMENTO (ID_DEPARTAMENTO)
);

CREATE TABLE DISTRITO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO INTEGER NOT NULL,
    NOMBRE_DISTRITO VARCHAR(50),
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO),
    FOREIGN KEY (ID_DEPARTAMENTO, ID_MUNICIPIO) REFERENCES MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO)
);

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
);

CREATE TABLE DETALLE_REQUISITO (
    NIT VARCHAR(20) NOT NULL,
    ID_OFERTA VARCHAR(10) NOT NULL,
    ID_DETALLE VARCHAR(10) NOT NULL,
    DESCRIPCION_REQUISITO VARCHAR(100),
    PRIMARY KEY (NIT, ID_OFERTA, ID_DETALLE),
    FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA)
);

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
);

CREATE TABLE CERTIFICACION (
    ID_CERTIFICACION VARCHAR(10) NOT NULL,
    ID_INSTITUCION VARCHAR(20) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_TIPO_CERTIFICACION INTEGER,
    NOMBRE_CERTIFICACION VARCHAR(150),
    FECHA_CERTIFICACION DATE,
    PERIODO VARCHAR(50),
    PRIMARY KEY (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_TIPO_CERTIFICACION) REFERENCES TIPO_CERTIFICACION (ID_TIPO_CERTIFICACION)
);

CREATE TABLE FORMACION_ACADEMICA (
    ID_FORMACION VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_OFERTA_ACADEMICA VARCHAR(10),
    TITULO_OBTENIDO VARCHAR(150),
    PERIODO VARCHAR(50),
    FECHA_OBTENCION DATE,
    PRIMARY KEY (ID_FORMACION, ID_POSTULANTE),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_OFERTA_ACADEMICA) REFERENCES OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA)
);

CREATE TABLE HABILIDAD_POSTULANTE (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    NIVEL_DESTREZA VARCHAR(12),
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD) REFERENCES HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
);

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
);

CREATE TABLE RED_SOCIAL_POSTULANTE (
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_RED_SOCIAL INTEGER NOT NULL,
    URL_PERFIL VARCHAR(100),
    PRIMARY KEY (ID_POSTULANTE, ID_RED_SOCIAL),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_RED_SOCIAL) REFERENCES RED_SOCIAL (ID_RED_SOCIAL)
);

-- ========================================
-- TABLAS CON PK MANUAL VARCHAR
-- ========================================

CREATE TABLE INSTITUCION (
    ID_INSTITUCION VARCHAR(20) PRIMARY KEY,
    NOMBRE_INSTITUCION VARCHAR(150)
);

CREATE TABLE HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    NOMBRE_HABILIDAD VARCHAR(100),
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD) REFERENCES CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD)
);

CREATE TABLE EMPRESA (
    NIT VARCHAR(20) PRIMARY KEY,
    ID_DISTRITO_DEPTO INTEGER NOT NULL,
    ID_DISTRITO_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO_ID INTEGER NOT NULL,
    NOMBRE_EMPRESA VARCHAR(150),
    CONTACTO_DIRECTO VARCHAR(100),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)
        REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
);

CREATE TABLE POSTULANTE (
    ID_POSTULANTE VARCHAR(20) PRIMARY KEY,
    ID_GENERO INTEGER NOT NULL,
    ID_TIPO_DOCUMENTO INTEGER NOT NULL,
    NUM_DOCUMENTO VARCHAR(20),
    ID_GRADO_ACADEMICO INTEGER NOT NULL,
    ID_DISTRITO_DEPTO INTEGER,
    ID_DISTRITO_MUNICIPIO INTEGER,
    ID_DISTRITO_ID INTEGER,
    NOMBRE VARCHAR(100),
    APELLIDO VARCHAR(100),
    FECHA_NACIMIENTO DATE,
    NUP VARCHAR(20),
    DIRECCION_DETALLE VARCHAR(250),
    TELEFONO_CASA VARCHAR(15),
    TELEFONO_CELULAR VARCHAR(15),
    EMAIL VARCHAR(100),
    FOREIGN KEY (ID_GENERO) REFERENCES GENERO (ID_GENERO),
    FOREIGN KEY (ID_TIPO_DOCUMENTO) REFERENCES TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)
        REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
);

CREATE TABLE OFERTA_ACADEMICA (
    ID_OFERTA_ACADEMICA VARCHAR(10) PRIMARY KEY,
    ID_GRADO_ACADEMICO INTEGER,
    ID_INSTITUCION VARCHAR(20),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO)
);
```

### 3.3 Mapa de Relaciones (Diagrama de FK)

```
CATEGORIA_HABILIDAD ──< HABILIDAD ──< HABILIDAD_POSTULANTE >── POSTULANTE
                                                                  │
GENERO ──────────────────────────────────────────────────────────< │
TIPO_DOCUMENTO ─────────────────────────────────────────────────< │
GRADO_ACADEMICO ──< POSTULANTE ──< FORMACION_ACADEMICA >── OFERTA_ACADEMICA
                ──< OFERTA_TRABAJO                              │
                ──< OFERTA_ACADEMICA ──< INSTITUCION            │
                                                               │
EMPRESA ──< OFERTA_TRABAJO ──< DETALLE_REQUISITO               │
       ──< EXPERIENCIA_LABORAL                                  │
                                                               │
DISTRITO ──< POSTULANTE   ──< EXPERIENCIA_LABORAL              │
        ──< EMPRESA        ──< CERTIFICACION >── TIPO_CERTIFICACION
                           ──< RED_SOCIAL_POSTULANTE >── RED_SOCIAL
                           ──< POSTULACION >── OFERTA_TRABAJO
```

---

## 4. Índices (23)

### 4.1 Definición

```sql
-- Ubicación geográfica
CREATE INDEX IF NOT EXISTS IDX_MUNICIPIO_DEPTO ON MUNICIPIO (ID_DEPARTAMENTO);
CREATE INDEX IF NOT EXISTS IDX_DISTRITO_MUNICIPIO ON DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO);

-- Habilidades
CREATE INDEX IF NOT EXISTS IDX_HABILIDAD_CATEGORIA ON HABILIDAD (ID_CATEGORIA_HABILIDAD);

-- Empresa por distrito
CREATE INDEX IF NOT EXISTS IDX_EMPRESA_DISTRITO ON EMPRESA (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID);

-- Postulante (búsquedas frecuentes)
CREATE INDEX IF NOT EXISTS IDX_POSTULANTE_GENERO ON POSTULANTE (ID_GENERO);
CREATE INDEX IF NOT EXISTS IDX_POSTULANTE_TIPO_DOC ON POSTULANTE (ID_TIPO_DOCUMENTO);
CREATE INDEX IF NOT EXISTS IDX_POSTULANTE_DISTRITO ON POSTULANTE (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID);

-- Ofertas
CREATE INDEX IF NOT EXISTS IDX_OFERTA_GRADO ON OFERTA_TRABAJO (ID_GRADO_ACADEMICO);
CREATE INDEX IF NOT EXISTS IDX_DETALLE_OFERTA ON DETALLE_REQUISITO (NIT, ID_OFERTA);

-- Experiencia Laboral
CREATE INDEX IF NOT EXISTS IDX_EXP_POSTULANTE ON EXPERIENCIA_LABORAL (ID_POSTULANTE);
CREATE INDEX IF NOT EXISTS IDX_EXP_EMPRESA ON EXPERIENCIA_LABORAL (NIT);

-- Certificaciones
CREATE INDEX IF NOT EXISTS IDX_CERT_POSTULANTE ON CERTIFICACION (ID_POSTULANTE);
CREATE INDEX IF NOT EXISTS IDX_CERT_INSTITUCION ON CERTIFICACION (ID_INSTITUCION);

-- Formación Académica
CREATE INDEX IF NOT EXISTS IDX_FORM_POSTULANTE ON FORMACION_ACADEMICA (ID_POSTULANTE);

-- Habilidad Postulante
CREATE INDEX IF NOT EXISTS IDX_HAB_POST_POSTULANTE ON HABILIDAD_POSTULANTE (ID_POSTULANTE);
CREATE INDEX IF NOT EXISTS IDX_HAB_POST_HABILIDAD ON HABILIDAD_POSTULANTE (ID_HABILIDAD);

-- Postulaciones
CREATE INDEX IF NOT EXISTS IDX_POSTULACION_POSTULANTE ON POSTULACION (ID_POSTULANTE);
CREATE INDEX IF NOT EXISTS IDX_POSTULACION_OFERTA ON POSTULACION (NIT, ID_OFERTA);

-- Red Social Postulante
CREATE INDEX IF NOT EXISTS IDX_RED_POST_POSTULANTE ON RED_SOCIAL_POSTULANTE (ID_POSTULANTE);

-- Oferta Académica
CREATE INDEX IF NOT EXISTS IDX_OA_INSTITUCION ON OFERTA_ACADEMICA (ID_INSTITUCION);
CREATE INDEX IF NOT EXISTS IDX_OA_GRADO ON OFERTA_ACADEMICA (ID_GRADO_ACADEMICO);
```

### 4.2 Propósito

Todos los índices están creados sobre columnas FK para optimizar JOINs y búsquedas. Las tablas más consultadas (POSTULANTE, OFERTA_TRABAJO, EXPERIENCIA_LABORAL, CERTIFICACION) tienen múltiples índices.

---

## 5. Triggers (27)

### 5.1 Triggers Semánticos (6 pares → 12 triggers)

Validan reglas de negocio antes de INSERT y UPDATE. Cada regla tiene un trigger para INSERT y otro para UPDATE.

#### TR_POSTULANTE_EDAD / TR_POSTULANTE_EDAD_UPD

```sql
CREATE TRIGGER TR_POSTULANTE_EDAD BEFORE INSERT ON POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_NACIMIENTO > date('now')
    THEN RAISE(ABORT, 'La fecha de nacimiento no puede ser futura') END;
    SELECT CASE WHEN (strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)) < 18
    THEN RAISE(ABORT, 'El postulante debe ser mayor de edad') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | POSTULANTE |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación 1** | FECHA_NACIMIENTO no puede ser posterior a la fecha actual |
| **Validación 2** | Edad calculada debe ser ≥ 18 años |

#### TR_POSTULANTE_GRADO / TR_POSTULANTE_GRADO_UPD

```sql
CREATE TRIGGER TR_POSTULANTE_GRADO BEFORE INSERT ON POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN (
        SELECT LOWER(NOMBRE_GRADO) FROM GRADO_ACADEMICO
        WHERE ID_GRADO_ACADEMICO = NEW.ID_GRADO_ACADEMICO
    ) IN ('bachiller')
    THEN RAISE(ABORT, 'El postulante debe tener un grado academico superior a Bachiller') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | POSTULANTE |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación** | El grado académico no puede ser "Bachiller" (el sistema no permite postulantes con grado bachiller o inferior) |

#### TR_OFERTA_RANGO_EDAD / TR_OFERTA_RANGO_EDAD_UPD

```sql
CREATE TRIGGER TR_OFERTA_RANGO_EDAD BEFORE INSERT ON OFERTA_TRABAJO
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.EDAD_MINIMA < 18
    THEN RAISE(ABORT, 'Edad minima debe ser mayor o igual a 18') END;
    SELECT CASE WHEN NEW.EDAD_MINIMA > NEW.EDAD_MAXIMA
    THEN RAISE(ABORT, 'Edad minima no puede ser mayor a la maxima') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | OFERTA_TRABAJO |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación 1** | EDAD_MINIMA ≥ 18 |
| **Validación 2** | EDAD_MINIMA ≤ EDAD_MAXIMA |

#### TR_OFERTA_VIGENCIA / TR_OFERTA_VIGENCIA_UPD

```sql
CREATE TRIGGER TR_OFERTA_VIGENCIA BEFORE INSERT ON OFERTA_TRABAJO
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_CADUCIDAD <= NEW.FECHA_PUBLICACION
    THEN RAISE(ABORT, 'La oferta ya caduco o fecha invalida') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | OFERTA_TRABAJO |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación** | FECHA_CADUCIDAD debe ser posterior a FECHA_PUBLICACION |

#### TR_POSTULACION_VIGENCIA

```sql
CREATE TRIGGER TR_POSTULACION_VIGENCIA BEFORE INSERT ON POSTULACION
FOR EACH ROW BEGIN
    SELECT CASE WHEN (
        SELECT FECHA_CADUCIDAD FROM OFERTA_TRABAJO
        WHERE NIT = NEW.NIT AND ID_OFERTA = NEW.ID_OFERTA
    ) < date('now')
    THEN RAISE(ABORT, 'La oferta de trabajo ha vencido') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | POSTULACION |
| **Evento** | BEFORE INSERT (solo creación; no UPDATE para permitir cambios de estado) |
| **Validación** | La oferta referenciada no debe haber caducado |

#### TR_EXP_LABORAL_FECHAS / TR_EXP_LABORAL_FECHAS_UPD

```sql
CREATE TRIGGER TR_EXP_LABORAL_FECHAS BEFORE INSERT ON EXPERIENCIA_LABORAL
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_INICIO >= NEW.FECHA_FIN
    THEN RAISE(ABORT, 'Fecha inicio debe ser menor a fecha fin') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | EXPERIENCIA_LABORAL |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación** | FECHA_INICIO debe ser anterior a FECHA_FIN |

#### TR_HABILIDAD_NIVEL / TR_HABILIDAD_NIVEL_UPD

```sql
CREATE TRIGGER TR_HABILIDAD_NIVEL BEFORE INSERT ON HABILIDAD_POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.NIVEL_DESTREZA NOT IN ('Básico', 'Intermedio', 'Avanzado')
    THEN RAISE(ABORT, 'Nivel de destreza debe ser Basico, Intermedio o Avanzado') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | HABILIDAD_POSTULANTE |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación** | NIVEL_DESTREZA solo puede ser "Básico", "Intermedio" o "Avanzado" |

#### TR_USUARIO_FORMATO / TR_USUARIO_FORMATO_UPD

```sql
CREATE TRIGGER TR_USUARIO_FORMATO BEFORE INSERT ON USUARIO
FOR EACH ROW BEGIN
    SELECT CASE WHEN LENGTH(NEW.PASSWORD) < 8
    THEN RAISE(ABORT, 'Password minimo 8 caracteres') END;
END;
```

| Propiedad | Valor |
|-----------|-------|
| **Tabla** | USUARIO |
| **Evento** | BEFORE INSERT / BEFORE UPDATE |
| **Validación** | Longitud del hash ≥ 8 (limitación: verifica el hash, no el texto plano) |

### 5.2 Triggers Bloqueantes de Borrado (16)

Estos triggers **previenen** la eliminación de un registro padre si existen registros hijos asociados. En lugar de eliminar en cascada, BLOQUEAN la operación y muestran un mensaje de error descriptivo.

| Trigger | Tabla Padre | Tabla(s) Hija(s) | Condición |
|---------|-------------|-------------------|-----------|
| `TR_DEL_DEPARTAMENTO` | DEPARTAMENTO | MUNICIPIO | COUNT(*) > 0 |
| `TR_DEL_MUNICIPIO` | MUNICIPIO | DISTRITO | COUNT(*) > 0 |
| `TR_DEL_DISTRITO` | DISTRITO | POSTULANTE, EMPRESA | COUNT(*) > 0 |
| `TR_DEL_CATEGORIA` | CATEGORIA_HABILIDAD | HABILIDAD | COUNT(*) > 0 |
| `TR_DEL_HABILIDAD` | HABILIDAD | HABILIDAD_POSTULANTE | COUNT(*) > 0 |
| `TR_DEL_POSTULANTE` | POSTULANTE | POSTULACION, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, CERTIFICACION, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE | COUNT(*) > 0 (6 checks) |
| `TR_DEL_TIPO_DOCUMENTO` | TIPO_DOCUMENTO | POSTULANTE | COUNT(*) > 0 |
| `TR_DEL_GENERO` | GENERO | POSTULANTE | COUNT(*) > 0 |
| `TR_DEL_RED_SOCIAL` | RED_SOCIAL | RED_SOCIAL_POSTULANTE | COUNT(*) > 0 |
| `TR_DEL_INSTITUCION` | INSTITUCION | CERTIFICACION, OFERTA_ACADEMICA | COUNT(*) > 0 |
| `TR_DEL_GRADO` | GRADO_ACADEMICO | OFERTA_TRABAJO, OFERTA_ACADEMICA, POSTULANTE | COUNT(*) > 0 |
| `TR_DEL_EMPRESA` | EMPRESA | OFERTA_TRABAJO, EXPERIENCIA_LABORAL | COUNT(*) > 0 |
| `TR_DEL_OFERTA_TRABAJO` | OFERTA_TRABAJO | DETALLE_REQUISITO, POSTULACION | COUNT(*) > 0 |
| `TR_DEL_OFERTA_ACADEMICA` | OFERTA_ACADEMICA | FORMACION_ACADEMICA | COUNT(*) > 0 |
| `TR_DEL_TIPO_CERTIFICACION` | TIPO_CERTIFICACION | CERTIFICACION | COUNT(*) > 0 |

Ejemplo (`TR_DEL_POSTULANTE` — el más complejo):
```sql
CREATE TRIGGER TR_DEL_POSTULANTE BEFORE DELETE ON POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN (SELECT COUNT(*) FROM POSTULACION WHERE ID_POSTULANTE = OLD.ID_POSTULANTE) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: el postulante tiene postulaciones') END;
    SELECT CASE WHEN (SELECT COUNT(*) FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = OLD.ID_POSTULANTE) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: el postulante tiene experiencias laborales') END;
    SELECT CASE WHEN (SELECT COUNT(*) FROM FORMACION_ACADEMICA WHERE ID_POSTULANTE = OLD.ID_POSTULANTE) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: el postulante tiene formaciones academicas') END;
    SELECT CASE WHEN (SELECT COUNT(*) FROM CERTIFICACION WHERE ID_POSTULANTE = OLD.ID_POSTULANTE) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: el postulante tiene certificaciones') END;
    SELECT CASE WHEN (SELECT COUNT(*) FROM HABILIDAD_POSTULANTE WHERE ID_POSTULANTE = OLD.ID_POSTULANTE) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: el postulante tiene habilidades asignadas') END;
    SELECT CASE WHEN (SELECT COUNT(*) FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = OLD.ID_POSTULANTE) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: el postulante tiene redes sociales') END;
END;
```

### 5.3 Triggers de Integridad Referencial (5 pares → 10 triggers)

SQLite soporta FK mediante `PRAGMA foreign_keys = ON`, pero estos triggers son una **capa adicional de seguridad** con mensajes de error más descriptivos. Cada uno tiene versión INSERT y UPDATE.

| Trigger | Tabla | Columna FK | Referencia | Mensaje |
|---------|-------|-----------|------------|---------|
| `TR_MUNICIPIO_DEPTO` / `_UPD` | MUNICIPIO | ID_DEPARTAMENTO | DEPARTAMENTO | "El departamento asociado no existe" |
| `TR_DISTRITO_MUNICIPIO` / `_UPD` | DISTRITO | (ID_DEPARTAMENTO, ID_MUNICIPIO) | MUNICIPIO | "El municipio asociado no existe" |
| `TR_HABILIDAD_CATEGORIA` / `_UPD` | HABILIDAD | ID_CATEGORIA_HABILIDAD | CATEGORIA_HABILIDAD | "La categoria asociada no existe" |
| `TR_EMPRESA_DISTRITO` / `_UPD` | EMPRESA | (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) | DISTRITO | "El distrito asociado no existe" |
| `TR_POSTULANTE_FK` / `_UPD` | POSTULANTE | ID_GENERO, ID_TIPO_DOCUMENTO, ID_GRADO_ACADEMICO | GENERO, TIPO_DOCUMENTO, GRADO_ACADEMICO | Mensajes específicos por FK |

---

## 6. Validaciones a Nivel de Código

### 6.1 Estructura de Validación

Todas las validaciones se centralizan en `ValidationRules.kt` mediante el objeto `ValidationRules`. La función principal es:

```kotlin
fun validate(tableName: String, column: String, value: String): String?
```

Retorna `null` si es válido, o un mensaje de error si no cumple. Se usa en `EditorDialogFragment` mediante `TextWatcher` para validación inline.

### 6.2 Reglas por Tabla y Campo

#### POSTULANTE

| Campo | Requerido | Patrón | Longitud | Notas |
|-------|-----------|--------|----------|-------|
| ID_POSTULANTE | ✅ | `^[A-Z]{2}\d{5}$` | máx 7 | Código: 2 letras + 5 dígitos |
| NOMBRE | ✅ | — | — | — |
| APELLIDO | ✅ | — | — | — |
| NUM_DOCUMENTO | ✅ | — | — | Se aplica máscara DUI/NIT/Pasaporte según tipo |
| EMAIL | ✅ | `^[^@]+@[^@]+\.[^@]+$` | — | Formato email básico |
| FECHA_NACIMIENTO | ✅ | `^\d{4}-\d{2}-\d{2}$` | — | Formato YYYY-MM-DD |
| ID_GENERO | ✅ | — | — | FK |
| ID_TIPO_DOCUMENTO | ✅ | — | — | FK |
| ID_GRADO_ACADEMICO | ✅ | — | — | FK |
| NUP | ✅ | — | — | — |
| DIRECCION_DETALLE | ✅ | — | — | — |
| TELEFONO_CASA | ✅ | — | — | Máscara XXXX-XXXX |
| TELEFONO_CELULAR | ✅ | — | — | Máscara XXXX-XXXX |

#### EMPRESA

| Campo | Requerido | Patrón | Longitud | Notas |
|-------|-----------|--------|----------|-------|
| NIT | ✅ | `^\d{14}$` | 14 exactos | Solo dígitos |
| NOMBRE_EMPRESA | ✅ | — | — | — |
| CONTACTO_DIRECTO | ✅ | `^\d{4}-\d{4}$` | 9 | Formato teléfono |

#### OFERTA_TRABAJO

| Campo | Requerido | Patrón | Rango | Notas |
|-------|-----------|--------|-------|-------|
| ID_OFERTA | ✅ | `^OF\d{2,}$` | — | Prefijo OF + dígitos |
| TITULO_PUESTO | ✅ | — | — | — |
| FECHA_PUBLICACION | ✅ | `^\d{4}-\d{2}-\d{2}$` | No futura | — |
| FECHA_CADUCIDAD | ✅ | `^\d{4}-\d{2}-\d{2}$` | — | — |
| EXPERIENCIA_ANIOS | ✅ | — | — | — |
| EDAD_MINIMA | — | — | 16–100 | Trigger exige ≥18 |
| EDAD_MAXIMA | — | — | 16–100 | — |
| DESCRIPCION_OFERTA_TRABAJO | ✅ | — | — | VARCHAR(5000) |

#### CERTIFICACION

| Campo | Requerido | Patrón | Notas |
|-------|-----------|--------|-------|
| ID_CERTIFICACION | ✅ | `^C\d{3,}$` | Prefijo C + dígitos |
| NOMBRE_CERTIFICACION | ✅ | — | — |
| FECHA_CERTIFICACION | ✅ | `^\d{4}-\d{2}-\d{2}$` | No futura |
| PERIODO | ✅ | `^\d{2}/\d{2}/\d{2}--\d{2}/\d{2}/\d{2}$` | Formato `DD/MM/AA--DD/MM/AA` |

#### EXPERIENCIA_LABORAL

| Campo | Requerido | Patrón | Notas |
|-------|-----------|--------|-------|
| ID_EXPERIENCIA | ✅ | `^EL\d{2,}$` | Prefijo EL + dígitos |
| PUESTO_TRABAJO | ✅ | — | — |
| FECHA_INICIO | ✅ | `^\d{4}-\d{2}-\d{2}$` | — |
| FECHA_FIN | ✅ | `^\d{4}-\d{2}-\d{2}$` | No futura |
| DESCP_EXPERIENCIA_LABORAL | ✅ | — | — |
| CONTACTO_REFERENCIA | ✅ | — | Máscara XXXX-XXXX |

#### FORMACION_ACADEMICA

| Campo | Requerido | Patrón | Notas |
|-------|-----------|--------|-------|
| ID_FORMACION | ✅ | `^FOA\d{3,}$` | Prefijo FOA + dígitos |
| TITULO_OBTENIDO | ✅ | — | — |
| FECHA_OBTENCION | ✅ | `^\d{4}-\d{2}-\d{2}$` | No futura |
| PERIODO | ✅ | `^\d{2}/\d{2}/\d{2}--\d{2}/\d{2}/\d{2}$` | Formato rango de fechas |

#### POSTULACION

| Campo | Requerido | Patrón | Notas |
|-------|-----------|--------|-------|
| ID_POSTULACION | ✅ | `^POS\d{3,}$` | Prefijo POS + dígitos |
| FECHA_APLICACION | ✅ | `^\d{4}-\d{2}-\d{2}$` | No futura |
| ESTADO_PROCESO | ✅ | — | Dropdown: Activo, En Proceso, Contratado, Rechazado |

#### Otras Tablas

| Tabla | Campo | Regla |
|-------|-------|-------|
| INSTITUCION | ID_INSTITUCION | `^[A-Za-z]{2,}\d{2,}$`, máx 20 |
| INSTITUCION | NOMBRE_INSTITUCION | Requerido |
| HABILIDAD | ID_HABILIDAD | `^H\d{2,}$`, máx 10 |
| HABILIDAD | NOMBRE_HABILIDAD | Requerido |
| DETALLE_REQUISITO | ID_DETALLE | `^D\d{1,}$`, máx 10 |
| DETALLE_REQUISITO | DESCRIPCION_REQUISITO | Requerido |
| OFERTA_ACADEMICA | ID_OFERTA_ACADEMICA | `^OFA\d{2,}$`, máx 10 |
| RED_SOCIAL_POSTULANTE | URL_PERFIL | `^https?://.*` |
| HABILIDAD_POSTULANTE | NIVEL_DESTREZA | Requerido (Dropdown: Básico, Intermedio, Avanzado) |
| MUNICIPIO | ID_MUNICIPIO | Requerido |
| MUNICIPIO | NOMBRE_MUNICIPIO | Requerido |
| DISTRITO | ID_DISTRITO | Requerido |
| DISTRITO | NOMBRE_DISTRITO | Requerido |

### 6.3 Validación de Fechas Futuras (Código)

En `ValidationRules.kt`, función `validate()`:

```kotlin
val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
val today = utcFormat.format(Date())

val futureDateTables = mapOf(
    "CERTIFICACION" to "FECHA_CERTIFICACION",
    "EXPERIENCIA_LABORAL" to "FECHA_FIN",
    "POSTULACION" to "FECHA_APLICACION",
    "OFERTA_TRABAJO" to "FECHA_PUBLICACION",
    "FORMACION_ACADEMICA" to "FECHA_OBTENCION"
)
if (futureDateTables[tableName] == column && trimmed > today) {
    return "${rules.friendlyName} no puede ser una fecha futura"
}
```

Usa **UTC** para ser consistente con el fix del DatePicker de Material Design.

### 6.4 Validación de Duplicados (Capa Repositorio)

En `MainRepository.kt`, las funciones `checkDuplicateInsert()` y `checkDuplicateUpdate()` verifican duplicados antes de cada inserción/actualización mediante consultas SQL personalizadas por tabla:

| Tabla | Campos de Unicidad |
|-------|-------------------|
| GENERO | NOMBRE_GENERO (case insensitive) |
| CATEGORIA_HABILIDAD | NOMBRE_CATEGORIA |
| TIPO_DOCUMENTO | NOMBRE_TIPO |
| DEPARTAMENTO | NOMBRE_DEPARTAMENTO |
| GRADO_ACADEMICO | NOMBRE_GRADO |
| RED_SOCIAL | NOMBRE_RED |
| TIPO_CERTIFICACION | NOMBRE_TIPO |
| INSTITUCION | NOMBRE_INSTITUCION |
| MUNICIPIO | (ID_DEPARTAMENTO, NOMBRE_MUNICIPIO) |
| DISTRITO | (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_DISTRITO) |
| HABILIDAD | NOMBRE_HABILIDAD |
| EMPRESA | NIT, NOMBRE_EMPRESA |
| POSTULANTE | NUM_DOCUMENTO, EMAIL |
| USUARIO | USERNAME |
| OFERTA_TRABAJO | (NIT, TITULO_PUESTO) |
| DETALLE_REQUISITO | (NIT, ID_OFERTA, DESCRIPCION_REQUISITO) |
| EXPERIENCIA_LABORAL | (ID_POSTULANTE, NIT, PUESTO_TRABAJO) |
| CERTIFICACION | (ID_POSTULANTE, NOMBRE_CERTIFICACION) |
| POSTULACION | (ID_POSTULANTE, NIT, ID_OFERTA) |
| RED_SOCIAL_POSTULANTE | (ID_POSTULANTE, ID_RED_SOCIAL) |
| HABILIDAD_POSTULANTE | (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE) |
| OFERTA_ACADEMICA | (ID_INSTITUCION, ID_GRADO_ACADEMICO) |

### 6.5 Transformación de Strings (Lowercase)

En `MainRepository.kt`, todos los campos de tipo nombre/descripción se convierten automáticamente a minúsculas antes de insertar/actualizar:

```kotlin
val lowerFields = setOf(
    "NOMBRE_CATEGORIA", "NOMBRE_GENERO", "NOMBRE_TIPO", "NOMBRE_DEPARTAMENTO",
    "NOMBRE_MUNICIPIO", "NOMBRE_DISTRITO", "NOMBRE_INSTITUCION", "NOMBRE_GRADO",
    "NOMBRE_RED", "NOMBRE_HABILIDAD", "NOMBRE_EMPRESA", "CONTACTO_DIRECTO",
    "NOMBRE", "APELLIDO", "DIRECCION_DETALLE", "EMAIL",
    "TITULO_PUESTO", "DESCRIPCION_OFERTA_TRABAJO", "DESCRIPCION_REQUISITO",
    "NOMBRE_CERTIFICACION", "PUESTO_TRABAJO", "DESCP_EXPERIENCIA_LABORAL",
    "CONTACTO_REFERENCIA", "TITULO_OBTENIDO", "ESTADO_PROCESO", "URL_PERFIL",
    "USERNAME", "ROL"
)
```

---

## 7. Máscaras de Entrada

### 7.1 DUI — `XXXXXXXX-X`

```kotlin
fun formatDUI(text: String): String {
    val digits = text.filter { it.isDigit() }.take(9)
    return when {
        digits.length > 8 -> "${digits.substring(0, 8)}-${digits.substring(8)}"
        else -> digits
    }
}
```

**Longitud máxima:** 10 caracteres

### 7.2 NIT — `XXXX-XXXXXX-XXX-X`

```kotlin
fun formatNIT(text: String): String {
    val digits = text.filter { it.isDigit() }.take(14)
    return when {
        digits.length > 13 -> "${digits.substring(0, 4)}-${digits.substring(4, 10)}-${digits.substring(10, 13)}-${digits.substring(13)}"
        digits.length > 10 -> "${digits.substring(0, 4)}-${digits.substring(4, 10)}-${digits.substring(10)}"
        digits.length > 4 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
        else -> digits
    }
}
```

**Longitud máxima:** 17 caracteres

### 7.3 NIT Simple (Empresa) — `XXXXXXXXXXXXXX`

```kotlin
fun formatNitSimple(text: String): String {
    return text.filter { it.isDigit() }.take(14)
}
```

### 7.4 Teléfono — `XXXX-XXXX`

```kotlin
fun formatTelefono(text: String): String {
    val digits = text.filter { it.isDigit() }.take(8)
    return if (digits.length > 4) {
        "${digits.substring(0, 4)}-${digits.substring(4)}"
    } else digits
}
```

**Longitud máxima:** 9 caracteres

### 7.5 Período — `DD/MM/AA--DD/MM/AA`

```kotlin
fun formatPeriodo(text: String): String {
    val digits = text.filter { it.isDigit() }.take(12)
    return when {
        digits.length > 10 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4, 6)}--${digits.substring(6, 8)}/${digits.substring(8, 10)}/${digits.substring(10, 12)}"
        digits.length > 8 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4, 6)}--${digits.substring(6, 8)}/${digits.substring(8, 10)}"
        digits.length > 6 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4, 6)}--${digits.substring(6, 8)}"
        digits.length > 4 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4, 6)}"
        digits.length > 2 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}"
        else -> digits
    }
}
```

### 7.6 Aplicación por Tabla

| Tabla | Campo | Máscara |
|-------|-------|---------|
| POSTULANTE | NUM_DOCUMENTO | DUI o NIT según tipo de documento |
| EMPRESA | NIT | NIT Simple (14 dígitos) |
| EMPRESA | CONTACTO_DIRECTO | Teléfono XXXX-XXXX |
| EXPERIENCIA_LABORAL | CONTACTO_REFERENCIA | Teléfono XXXX-XXXX |
| CERTIFICACION | PERIODO | DD/MM/AA--DD/MM/AA |
| FORMACION_ACADEMICA | PERIODO | DD/MM/AA--DD/MM/AA |

### 7.7 Validadores Adicionales

```kotlin
fun isValidPassword(password: String): Boolean = password.length >= 8
fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
fun isValidDate(date: String): Boolean = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.isLenient = false
    sdf.parse(date) != null
} catch (e: Exception) { false }
```

---

## 8. Seguridad

### 8.1 Hashing de Contraseñas

En `PasswordHasher.kt`:

```kotlin
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hash(password: String): String {
        val salt = generateSalt()
        val hash = pbkdf2(password.toCharArray(), salt)
        return bytesToHex(salt) + ":" + bytesToHex(hash)
    }

    fun verify(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        val salt = hexToBytes(parts[0])
        val expectedHash = hexToBytes(parts[1])
        val actualHash = pbkdf2(password.toCharArray(), salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        return try {
            val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } catch (e: Exception) {
            // Fallback SHA-256 si PBKDF2 no está disponible
            MessageDigest.getInstance("SHA-256").apply {
                update(salt)
                digest(password.joinToString("").toByteArray())
            }
        }
    }
}
```

**Formato de almacenamiento:** `hex(salt):hex(hash)` → ej: `"a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6:8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0"`

### 8.2 Sesión

- Almacenada en `SharedPreferences` (privado a la app, modo `MODE_PRIVATE`)
- Claves: `is_logged_in`, `user_id`, `username`, `user_role`
- Al cerrar sesión, se limpian todas las preferencias
- No hay tokens JWT (app local sin backend)

### 8.3 Protección de Rutas

- `DashboardFragment.onViewCreated()` verifica `KEY_IS_LOGGED_IN` en SharedPreferences
- Si no hay sesión, redirige al login
- Los botones de seed data y logout solo aparecen para admin
- Los permisos CRUD se validan por rol al cargar cada tabla y al mostrar botones de editar/eliminar

### 8.4 Prevención de Auto-Eliminación

En `DeleteConfirmDialog.kt`:

```kotlin
if (tableName == Constants.TABLE_USUARIO && idToDelete != null) {
    val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val activeUserId = prefs.getInt(Constants.KEY_USER_ID, -1)
    if (idToDelete == activeUserId.toString()) {
        StyledToast.show(requireContext(), "No puedes eliminar tu propio usuario")
        dismiss()
        return
    }
}
```

---

## 9. Roles y Permisos

### 9.1 Definición de Roles

```kotlin
const val ROLE_ADMIN = "administrador"
const val ROLE_POSTULANTE = "postulante"
const val ROLE_EMPRESA = "gerente de empresa"
```

### 9.2 Niveles de Acceso

```kotlin
enum class AccessLevel { NONE, READ_ONLY, FULL }
```

### 9.3 Matriz de Permisos

| Tabla | Administrador | Postulante | Gerente de Empresa |
|-------|--------------|------------|-------------------|
| USUARIO | **FULL** | NONE | NONE |
| CATEGORIA_HABILIDAD | FULL | READ_ONLY | READ_ONLY |
| GENERO | FULL | READ_ONLY | READ_ONLY |
| TIPO_DOCUMENTO | FULL | READ_ONLY | READ_ONLY |
| DEPARTAMENTO | FULL | READ_ONLY | READ_ONLY |
| MUNICIPIO | FULL | READ_ONLY | READ_ONLY |
| DISTRITO | FULL | READ_ONLY | READ_ONLY |
| INSTITUCION | FULL | READ_ONLY | READ_ONLY |
| GRADO_ACADEMICO | FULL | READ_ONLY | READ_ONLY |
| RED_SOCIAL | FULL | READ_ONLY | NONE |
| OFERTA_ACADEMICA | FULL | READ_ONLY | READ_ONLY |
| HABILIDAD | FULL | READ_ONLY | READ_ONLY |
| EMPRESA | FULL | READ_ONLY | **FULL** |
| OFERTA_TRABAJO | FULL | READ_ONLY | **FULL** |
| DETALLE_REQUISITO | FULL | READ_ONLY | **FULL** |
| POSTULANTE | FULL | **FULL** | READ_ONLY |
| EXPERIENCIA_LABORAL | FULL | **FULL** | READ_ONLY |
| FORMACION_ACADEMICA | FULL | **FULL** | READ_ONLY |
| CERTIFICACION | FULL | **FULL** | READ_ONLY |
| HABILIDAD_POSTULANTE | FULL | **FULL** | READ_ONLY |
| POSTULACION | FULL | **FULL** (solo crear/ver) | **FULL** (solo cambiar estado) |
| RED_SOCIAL_POSTULANTE | FULL | **FULL** | READ_ONLY |

**Nota sobre POSTULACION:** El postulante solo puede crear y ver sus postulaciones (no puede eliminar ni cambiar estado). El gerente de empresa solo puede cambiar el estado del proceso (Activo → En Proceso → Contratado/Rechazado), no puede crear ni eliminar.

### 9.4 Funcionalidad por Rol

#### Administrador
- Acceso completo a todas las tablas (FULL en 23 tablas)
- CRUD completo en catálogos (departamentos, distritos, categorías, géneros, etc.)
- CRUD completo en usuarios (crear, editar, eliminar)
- CRUD completo en postulantes y empresas
- Botón de **seed data** visible (inserción de datos de prueba)
- Gestión de todos los perfiles y ofertas

#### Postulante
- **Solo lectura** en tablas catálogo (departamentos, municipios, habilidades, etc.)
- **CRUD completo** en sus propios datos:
  - POSTULANTE (su perfil)
  - EXPERIENCIA_LABORAL (sus experiencias)
  - FORMACION_ACADEMICA (sus estudios)
  - CERTIFICACION (sus certificaciones)
  - HABILIDAD_POSTULANTE (sus habilidades)
  - RED_SOCIAL_POSTULANTE (sus redes sociales)
- **Solo crear y ver** POSTULACION (postularse a ofertas vigentes)
- **Sin acceso** a USUARIO ni a datos de otras empresas/postulantes

#### Gerente de Empresa
- **Solo lectura** en tablas catálogo
- **CRUD completo** en:
  - EMPRESA (su empresa)
  - OFERTA_TRABAJO (ofertas de su empresa)
  - DETALLE_REQUISITO (requisitos de sus ofertas)
- **Solo lectura** en datos de postulantes (para evaluar candidatos)
- **Solo cambiar estado** en POSTULACION (gestionar proceso de selección)
- **Sin acceso** a RED_SOCIAL ni a USUARIO

---

## 10. Flujo de Autenticación

### 10.1 Registro

```
[RegisterFragment] → AuthViewModel.register() → MainRepository.register()
                                                       ↓
                                              ¿Username existe?
                                             /                  \
                                           Sí                   No
                                          ↓                     ↓
                                     return -2           Hashear password
                                                         (PBKDF2)
                                                              ↓
                                                     INSERT en USUARIO
                                                              ↓
                                                     return ID_USUARIO
```

**Validaciones en registro:**
1. **UI:** Campos requeridos, password ≥ 8 caracteres, confirmación de password
2. **ViewModel:** `AuthViewModel.register()` valida campos no vacíos y longitud de password
3. **Repository:** `register()` verifica username duplicado antes de insertar
4. **BD:** Trigger `TR_USUARIO_FORMATO` verifica longitud del hash

### 10.2 Login

```
[LoginFragment] → AuthViewModel.login() → MainRepository.login()
                                                 ↓
                                          SELECT * FROM USUARIO
                                          WHERE USERNAME = ?
                                                 ↓
                                        ¿Usuario encontrado?
                                       /                    \
                                     Sí                     No
                                      ↓                      ↓
                               PasswordHasher.verify()    return null
                               (PBKDF2 comparison)
                                      ↓
                               ¿Coincide?
                              /           \
                            Sí             No
                             ↓              ↓
                     Guardar sesión      return null
                     en SharedPrefs
                             ↓
                   Redirigir a Dashboard
```

### 10.3 Persistencia de Sesión

```kotlin
// Guardar sesión (LoginFragment)
prefs.edit()
    .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
    .putInt(Constants.KEY_USER_ID, usuario.id_usuario)
    .putString(Constants.KEY_USERNAME, usuario.username)
    .putString(Constants.KEY_USER_ROLE, usuario.rol)
    .apply()

// Cerrar sesión
prefs.edit().clear().apply()
```

---

## 11. Flujo de CRUD

### 11.1 Arquitectura del CRUD Genérico

El sistema implementa un CRUD **genérico** que funciona para las 23 tablas mediante metadatos definidos en `Constants.kt`:

```kotlin
// Constants.kt define para cada tabla:
fun getColumnsForTable(tableName: String): List<String>         // Columnas en orden
fun getPrimaryKeyColumns(tableName: String): List<String>       // Columnas PK
fun getAutoGenColumn(tableName: String): String?                // Columna auto-incremental (o null)
```

### 11.2 Método Insertar (`insertRecord`)

```kotlin
fun insertRecord(tableName: String, values: List<Any>): Long
```

**Flujo:**
1. Determina si la tabla tiene columna auto-incremental (`getAutoGenColumn`)
2. Si es USUARIO, hashea la contraseña con PBKDF2
3. Aplica transformación lowercase a campos de texto
4. Verifica duplicados con `checkDuplicateInsert()` (unicidad por tabla)
5. Construye INSERT SQL dinámico
6. Ejecuta en transacción
7. Captura errores de triggers y los traduce con `TriggerErrorTranslator`

### 11.3 Método Actualizar (`updateRecord`)

```kotlin
fun updateRecord(tableName: String, id: Any, values: List<Any>): Int
```

**Flujo:**
1. Filtra columna auto-incremental de la lista de columnas
2. Aplica transformación lowercase a campos de texto
3. Construye SET clause dinámico
4. Determina WHERE clause según tipo de PK (simple, compuesta, auto-incremental)
5. Verifica duplicados excluyendo el registro actual (`checkDuplicateUpdate`)
6. Ejecuta UPDATE SQL

### 11.4 Método Eliminar (`deleteRecord` / `deleteRecordByRow`)

```kotlin
fun deleteRecord(tableName: String, id: String): Boolean
fun deleteRecordByRow(tableName: String, rowData: List<String>): Boolean
```

**Flujo:**
1. Determina columnas PK
2. Si PK simple: `DELETE FROM table WHERE pkCol = 'id'`
3. Si PK compuesta: `DELETE FROM table WHERE pk1 = 'v1' AND pk2 = 'v2'`
4. Captura errores de triggers bloqueantes y los traduce

### 11.5 Búsqueda con JOINs (`searchTable`)

Cada tabla principal tiene una consulta JOIN optimizada:

| Tabla | JOINs | Columnas extra |
|-------|-------|---------------|
| HABILIDAD | LEFT JOIN CATEGORIA_HABILIDAD | NOMBRE_CATEGORIA |
| MUNICIPIO | LEFT JOIN DEPARTAMENTO | NOMBRE_DEPARTAMENTO |
| DISTRITO | LEFT JOIN MUNICIPIO | NOMBRE_MUNICIPIO |
| EXPERIENCIA_LABORAL | LEFT JOIN POSTULANTE | NOMBRE, APELLIDO |
| HABILIDAD_POSTULANTE | LEFT JOIN POSTULANTE, HABILIDAD | NOMBRE, APELLIDO, NOMBRE_HABILIDAD |
| POSTULACION | LEFT JOIN POSTULANTE, OFERTA_TRABAJO | NOMBRE, APELLIDO, TITULO_PUESTO |
| RED_SOCIAL_POSTULANTE | LEFT JOIN POSTULANTE, RED_SOCIAL | NOMBRE, APELLIDO, NOMBRE_RED |
| OFERTA_TRABAJO | LEFT JOIN EMPRESA, GRADO_ACADEMICO | NOMBRE_EMPRESA, NOMBRE_GRADO |
| DETALLE_REQUISITO | LEFT JOIN OFERTA_TRABAJO, EMPRESA | TITULO_PUESTO, NOMBRE_EMPRESA, FECHA_CADUCIDAD |
| OFERTA_ACADEMICA | LEFT JOIN INSTITUCION, GRADO_ACADEMICO | NOMBRE_INSTITUCION, NOMBRE_GRADO |
| CERTIFICACION | LEFT JOIN POSTULANTE, INSTITUCION, TIPO_CERTIFICACION | NOMBRE, APELLIDO, NOMBRE_INSTITUCION, NOMBRE_TIPO |
| FORMACION_ACADEMICA | LEFT JOIN POSTULANTE, OFERTA_ACADEMICA | NOMBRE, APELLIDO |

---

## 12. Flujo de Borrado — Política Actual: Solo Borra Si No Tiene Hijos

### ⚠️ Regla Fundamental

El sistema **NO elimina en cascada**. La política actual es:

> **Un registro SOLO se puede eliminar si NO tiene ningún registro hijo que dependa de él.**
> Si tiene hijos, el sistema BLOQUEA la eliminación y muestra un mensaje de error.

No hay triggers de DELETE en cascada. Los 16 triggers bloqueantes (sección 5.2) impiden el borrado si existe al menos 1 registro hijo.

---

### 12.1 ¿Qué registros se dejan borrar y cuáles no?

#### PUEDEN eliminarse (sin hijos):
| Tabla | Se puede eliminar si... |
|-------|------------------------|
| `DEPARTAMENTO` | No tiene MUNICIPIOs asociados |
| `MUNICIPIO` | No tiene DISTRITOs asociados |
| `DISTRITO` | No tiene POSTULANTEs ni EMPRESAs asociados |
| `CATEGORIA_HABILIDAD` | No tiene HABILIDADes asociadas |
| `HABILIDAD` | No tiene HABILIDAD_POSTULANTEs asociadas |
| `GENERO` | No tiene POSTULANTEs asociados |
| `TIPO_DOCUMENTO` | No tiene POSTULANTEs asociados |
| `RED_SOCIAL` | No tiene RED_SOCIAL_POSTULANTEs asociadas |
| `INSTITUCION` | No tiene CERTIFICACIONes ni OFERTA_ACADEMICAs asociadas |
| `GRADO_ACADEMICO` | No tiene OFERTA_TRABAJOs, OFERTA_ACADEMICAs ni POSTULANTEs asociados |
| `TIPO_CERTIFICACION` | No tiene CERTIFICACIONes asociadas |
| `EMPRESA` | No tiene OFERTA_TRABAJOs ni EXPERIENCIA_LABORALs asociadas |
| `OFERTA_TRABAJO` | No tiene DETALLE_REQUISITOs ni POSTULACIONes asociadas |
| `OFERTA_ACADEMICA` | No tiene FORMACION_ACADEMICAs asociadas |
| `POSTULANTE` | No tiene POSTULACIONes, EXPERIENCIA_LABORALs, FORMACION_ACADEMICAs, CERTIFICACIONes, HABILIDAD_POSTULANTEs ni RED_SOCIAL_POSTULANTEs asociadas |
| `USUARIO` | No es el propio usuario activo (validación adicional en UI) |

#### NO PUEDEN eliminarse (tienen hijos):
| Tabla | ¿Por qué? |
|-------|-----------|
| `POSTULANTE` con experiencia laboral | Trigger `TR_DEL_POSTULANTE` bloquea (6 checks) |
| `POSTULANTE` con postulaciones | ídem |
| `POSTULANTE` con certificaciones | ídem |
| `EMPRESA` con ofertas de trabajo | Trigger `TR_DEL_EMPRESA` bloquea |
| `DEPARTAMENTO` con municipios | Trigger `TR_DEL_DEPARTAMENTO` bloquea |
| `OFERTA_TRABAJO` con postulaciones | Trigger `TR_DEL_OFERTA_TRABAJO` bloquea |
| Cualquier tabla cuyas hijas tengan registros | El trigger correspondiente bloquea |

#### Tablas que NO tienen triggers bloqueantes (siempre se pueden eliminar):
| Tabla | Razón |
|-------|-------|
| `USUARIO` | No tiene trigger bloqueante (solo validación UI de auto-eliminación) |
| `POSTULACION` | Es tabla hoja (no tiene hijos) |
| `EXPERIENCIA_LABORAL` | Es tabla hoja |
| `FORMACION_ACADEMICA` | Es tabla hoja |
| `CERTIFICACION` | Es tabla hoja |
| `HABILIDAD_POSTULANTE` | Es tabla hoja |
| `RED_SOCIAL_POSTULANTE` | Es tabla hoja |
| `DETALLE_REQUISITO` | Es tabla hoja |

---

### 12.2 Flujo Detallado de Borrado (3 Capas de Seguridad)

#### Capa UI — DeleteConfirmDialog

```
Usuario presiona Eliminar en un registro
                    ↓
   DeleteConfirmDialog.onCreateView()
                    ↓
   Construye PK (simple o compuesta)
                    ↓
   viewModel.checkDeleteDependencies(pkString)
                    ↓
   MainRepository.getDeleteDependencies()  ← árbol recursivo
                    ↓
   ┌──────────────────────────────────────────┐
   │ ¿Hay dependencias (hijos)?              │
   │  Sí → showCannotDeleteDialog()          │
   │       Solo muestra botón "OK"           │
   │       NO se puede eliminar              │
   │  No  → showSimpleConfirm()              │
   │       Muestra botón "Eliminar"          │
   └──────────────────────────────────────────┘
                    ↓
   (solo si no hay dependencias)
   performDelete()
                    ↓
   Verifica permisos (rol)
                    ↓
   Verifica auto-eliminación (USUARIO)
                    ↓
   viewModel.deleteItem() o deleteItemByRow()
                    ↓
   MainRepository.deleteRecord() → SQL DELETE
                    ↓
   ¿Trigger bloquea? → TriggerErrorTranslator traduce error
```

#### Capa ViewModel — CrudViewModel

```kotlin
fun deleteItem(id: String) {
    viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.deleteRecord(currentTable, id)
        }
        loadItems()
        _operationResult.value = Resource.Success("Eliminado correctamente")
    }
}
```

#### Capa Base de Datos — Triggers Bloqueantes

Los 16 triggers bloqueantes de borrado (sección 5.2) funcionan así:

```sql
CREATE TRIGGER TR_DEL_<TABLA> BEFORE DELETE ON <TABLA_PADRE>
FOR EACH ROW BEGIN
    SELECT CASE WHEN (SELECT COUNT(*) FROM <TABLA_HIJA> WHERE <FK> = OLD.<PK>) > 0
    THEN RAISE(ABORT, 'No se puede eliminar: <mensaje descriptivo>') END;
END;
```

Si el trigger se dispara, `RAISE(ABORT)` lanza una excepción SQL que es capturada por `MainRepository.deleteRecord()` y traducida por `TriggerErrorTranslator`.

---

### 12.3 Ejemplos Concretos

#### Ejemplo 1: Postulante sin ningún registro asociado → SÍ se elimina
```
POSTULANTE "P001"
  ├── POSTULACION: 0 registros → OK
  ├── EXPERIENCIA_LABORAL: 0 registros → OK
  ├── FORMACION_ACADEMICA: 0 registros → OK
  ├── CERTIFICACION: 0 registros → OK
  ├── HABILIDAD_POSTULANTE: 0 registros → OK
  └── RED_SOCIAL_POSTULANTE: 0 registros → OK
  RESULTADO: Se elimina sin problema
```

#### Ejemplo 2: Postulante con postulaciones → NO se elimina
```
POSTULANTE "P001"
  ├── POSTULACION: 3 registros → BLOQUEA (TR_DEL_POSTULANTE)
  ├── EXPERIENCIA_LABORAL: 0 registros
  ├── ...
  RESULTADO: "No se puede eliminar: el postulante tiene postulaciones"
  SOLUCIÓN: Eliminar manualmente las 3 postulaciones primero
```

#### Ejemplo 3: Departamento sin municipios → SÍ se elimina
```
DEPARTAMENTO "Ahuachapán"
  └── MUNICIPIO: 0 registros → OK
  RESULTADO: Se elimina sin problema
```

#### Ejemplo 4: Departamento con municipios → NO se elimina
```
DEPARTAMENTO "San Salvador"
  └── MUNICIPIO: 5 registros → BLOQUEA (TR_DEL_DEPARTAMENTO)
  RESULTADO: "No se puede eliminar: el departamento tiene municipios asociados"
  SOLUCIÓN: Eliminar manualmente los 5 municipios primero
```

#### Ejemplo 5: Empresa sin ofertas de trabajo → SÍ se elimina
```
EMPRESA "Nequi El Salvador"
  ├── OFERTA_TRABAJO: 0 registros → OK
  └── EXPERIENCIA_LABORAL: 0 registros → OK
  RESULTADO: Se elimina sin problema
```

#### Ejemplo 6: Empresa con ofertas de trabajo → NO se elimina
```
EMPRESA "Banco Agrícola"
  ├── OFERTA_TRABAJO: 2 registros → BLOQUEA (TR_DEL_EMPRESA)
  └── EXPERIENCIA_LABORAL: 0 registros
  RESULTADO: "No se puede eliminar: la empresa tiene ofertas de trabajo"
  SOLUCIÓN: Eliminar manualmente las 2 ofertas primero
```

---

### 12.4 Árbol de Dependencias (Solo Consulta)

En `MainRepository.getDeleteDependencies()`, se usa una función recursiva `traverse()` que explora el árbol de dependencias completo para MOSTRARLO en la UI (pero igual bloquea):

```
Ejemplo: Consultar dependencias de DEPARTAMENTO
  Nivel 1: MUNICIPIO (3)
  Nivel 2: DISTRITO (12) (por cada municipio)
  Nivel 3: POSTULANTE (5), EMPRESA (2) (por cada distrito)
```

---

## 13. Flujo de Inserción y Doble Validación

Cada inserción pasa por **3 capas de validación**:

```
                CAPA UI (EditorDialogFragment)
                ┌─────────────────────────────────┐
                │ TextWatcher en cada campo        │
                │ ValidationRules.validate()       │
                │ Errores en TextInputLayout       │
                │ Máscaras de entrada              │
                │ Dropdowns FK con carga dinámica  │
                └──────────────┬──────────────────┘
                               │
                CAPA REPOSITORIO (MainRepository)
                ┌──────────────┴──────────────────┐
                │ Transformación lowercase         │
                │ checkDuplicateInsert()           │
                │ Hashing de password (USUARIO)    │
                └──────────────┬──────────────────┘
                               │
                CAPA BASE DE DATOS (Triggers)
                ┌──────────────┴──────────────────┐
                │ TR_POSTULANTE_EDAD               │
                │ TR_POSTULANTE_GRADO              │
                │ TR_OFERTA_RANGO_EDAD             │
                │ TR_OFERTA_VIGENCIA               │
                │ TR_POSTULACION_VIGENCIA          │
                │ TR_EXP_LABORAL_FECHAS            │
                │ TR_HABILIDAD_NIVEL               │
                │ TR_USUARIO_FORMATO               │
                │ Triggers de integridad referencial│
                └──────────────────────────────────┘
```

---

## 14. Seed Data

### 14.1 Tablas y Registros

| # | Tabla | Registros |
|---|-------|-----------|
| 1 | DEPARTAMENTO | 14 departamentos de El Salvador |
| 2 | GENERO | 5 géneros |
| 3 | CATEGORIA_HABILIDAD | 5 categorías |
| 4 | GRADO_ACADEMICO | 7 grados (Bachiller a Doctorado) |
| 5 | RED_SOCIAL | 5 redes (GitHub, Steam, LinkedIn, Discord, X) |
| 6 | TIPO_CERTIFICACION | 5 tipos |
| 7 | INSTITUCION | 6 instituciones |
| 8 | TIPO_DOCUMENTO | 3 tipos (DUI, NIT, Pasaporte) |
| 9 | MUNICIPIO | 44 municipios |
| 10 | DISTRITO | 262 distritos |
| 11 | HABILIDAD | 15 habilidades (3 por categoría) |
| 12 | EMPRESA | 10 empresas |
| 13 | OFERTA_ACADEMICA | 5 ofertas académicas |

### 14.2 Verificación de Seguridad

Antes de insertar, verifica que **ninguna** de las 13 tablas tenga registros. Si alguna tiene datos, retorna error "Ya existen datos en la base de datos" sin modificar nada. Todo se ejecuta en una sola transacción.

### 14.3 Datos de Habilidades

```
[Categoría]               Habilidades
Desarrollo de Software   Python, C++, APIs
Infraestructura/Cloud    Google Cloud, Linux, Vercel/Netlify
Redes/Telecomunicaciones  Cisco Packet Tracer, Subnetting, Tráfico de red
Bases de Datos           SQL relacional, Supabase, NoSQL
Inteligencia Artificial   Gemini API, Prompt engineering, Automatización con IA
```

### 14.4 Datos de Empresas (Ejemplo)

| NIT | Empresa | Departamento | Municipio | Distrito |
|-----|---------|-------------|-----------|----------|
| 06141234560101 | Banco Agrícola | San Salvador | San Salvador Centro | San Salvador |
| 06141234560104 | Holcim El Salvador | La Libertad | La Libertad Sur | Santa Tecla |
| 06141234560105 | AES CLESA | Santa Ana | Santa Ana Centro | Santa Ana |

---

## 15. Chip VIGENTE / VENCIDA (OFERTA_TRABAJO)

### 15.1 Visualización en Tabla

En `TableAdapter.kt`, para la tabla `OFERTA_TRABAJO`:

```kotlin
val fechaCad = getStringSafely(item, 5)
val df = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
val hoy = df.format(Date())
val vencida = fechaCad.isNotBlank() && fechaCad < hoy
chipEstado.visibility = View.VISIBLE
chipEstado.text = if (vencida) "VENCIDA" else "VIGENTE"
chipEstado.background = GradientDrawable().apply {
    cornerRadius = 48f
    setColor(if (vencida) 0xFFE53935.toInt() else 0xFF43A047.toInt())
}
```

- **VIGENTE:** Fondo verde `#43A047`
- **VENCIDA:** Fondo rojo `#E53935`

### 15.2 Filtro en Búsqueda

En `TableDetailFragment.kt`, cuando la tabla es `OFERTA_TRABAJO` y el query contiene "vigente" o "vencida", se filtra por comparación de `FECHA_CADUCIDAD` con la fecha actual.

### 15.3 Filtro en Dropdowns

En `MainRepository.kt`, `getFilteredOptions()` para `OFERTA_TRABAJO`:

```kotlin
val expirationFilter = if (!includeExpired) " AND o.FECHA_CADUCIDAD >= date('now')" else ""
```

Solo muestra ofertas vigentes en los dropdowns de POSTULACION. Para DETALLE_REQUISITO, `includeExpired = true` (los requisitos pueden verse aunque la oferta haya vencido).

### 15.4 Trigger de Seguridad

El trigger `TR_POSTULACION_VIGENCIA` (BEFORE INSERT en POSTULACION) rechaza la inserción si la oferta ha caducado:

```sql
SELECT CASE WHEN (
    SELECT FECHA_CADUCIDAD FROM OFERTA_TRABAJO
    WHERE NIT = NEW.NIT AND ID_OFERTA = NEW.ID_OFERTA
) < date('now')
THEN RAISE(ABORT, 'La oferta de trabajo ha vencido') END;
```

---

## 16. DatePicker (Corrección UTC)

### 16.1 Problema Original

El `MaterialDatePicker` de Google Material Design devuelve timestamps en **UTC**. El código original los interpretaba en la **zona horaria local** del dispositivo, causando un desfase de -1 día en husos horarios negativos (ej: El Salvador UTC-6).

### 16.2 Solución Aplicada

En `EditorDialogFragment.kt`, `showDatePicker()`:

```kotlin
val utc = TimeZone.getTimeZone("UTC")
val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = utc }
val calendar = Calendar.getInstance(utc)
```

Se usa `Calendar.getInstance(utc)` para el `startMillis` y `endMillis` del picker, y el callback también usa UTC para formatear la fecha seleccionada. Las validaciones de fecha futura en `ValidationRules.kt` también usan UTC.

---

## 17. TriggerErrorTranslator

### 17.1 Funcionamiento

El `TriggerErrorTranslator` traduce los errores lanzados por los triggers SQLite (mensajes en inglés o técnicos) a mensajes amigables en español. Usa búsqueda por substring con `contains(ignoreCase = true)`.

### 17.2 Mapeo de Errores (Ejemplos)

| Error SQL / Trigger | Mensaje Traducido |
|---------------------|-------------------|
| `Fecha inicio debe ser menor a fecha fin` | La fecha de inicio debe ser anterior a la fecha de finalización |
| `Edad minima no puede ser mayor a la maxima` | La edad mínima no puede ser mayor a la edad máxima |
| `UNIQUE constraint failed` | Ya existe un registro con esos datos |
| `FOREIGN KEY constraint failed` | NO ES POSIBLE ELIMINAR — el registro tiene dependencias |
| `NOT NULL constraint failed` | Un campo obligatorio está vacío |
| `El postulante debe ser mayor de edad` | El postulante debe ser mayor de 18 años |
| `No se puede eliminar: el departamento tiene municipios asociados` | Elimine primero los municipios asociados a este departamento |
| (cualquier error de delete trigger) | "Elimine primero los ... asociados a este ..." |

### 17.3 Cobertura

El mapa contiene **~50 entradas** cubriendo:
- Errores de duplicados (23 tablas)
- Errores semánticos (8 reglas de negocio)
- Errores de integridad referencial (5 FK checks)
- Errores de borrado bloqueado (16 triggers)
- Errores genéricos de SQLite (UNIQUE, FOREIGN KEY, NOT NULL, constraint)

---

## 18. Componentes UI Relevantes

### 18.1 EditorDialogFragment (1716 líneas)

Diálogo genérico que genera dinámicamente el formulario para cualquier tabla. Soporta:

**Tipos de campos:**
- **TextInputEditText** con `TextWatcher` para validación inline
- **MaterialAutoCompleteTextView** para FK dropdowns (carga dependiente)
- **MaterialDatePicker** para campos de fecha (con fix UTC)
- **Dropdowns específicos:** NIVEL_DESTREZA (Básico/Intermedio/Avanzado), ESTADO_PROCESO (Activo/En Proceso/Contratado/Rechazado), ROL (postulante/gerente de empresa/administrador)
- **Máscaras** DUI, NIT, teléfono, período
- **Cascada geográfica:** Departamento → Municipio → Distrito (3 niveles para POSTULANTE, EMPRESA y DISTRITO)

**Modos:**
- Crear: Campos vacíos, título "Nuevo [tabla]"
- Editar: Campos precargados, título "Editar [tabla]"
- Ver: Campos deshabilitados, título "Ver [tabla]"

**Comportamiento especial:**
- POSTULACION en modo edición: Si el rol es Postulante, deshabilita todos los campos. Si es Gerente de Empresa, solo permite cambiar ESTADO_PROCESO.
- NUM_DOCUMENTO: Cambia hint y validación según el TIPO_DOCUMENTO seleccionado.
- AUTO_INCREMENTAL: La columna auto-generada se omite del formulario.

### 18.2 TableAdapter

Adaptador genérico que renderiza cualquier tabla en un RecyclerView. Usa `DiffUtil.ItemCallback` para actualizaciones eficientes.

**Renderizado específico por tabla:**

| Tabla | Texto Principal | Texto Secundario | Chip/Extra |
|-------|----------------|------------------|------------|
| POSTULANTE | NOMBRE | APELLIDO | — |
| OFERTA_TRABAJO | TITULO_PUESTO | NOMBRE_EMPRESA | Chip VIGENTE/VENCIDA |
| EMPRESA | NOMBRE_EMPRESA | CONTACTO_DIRECTO | — |
| EXPERIENCIA_LABORAL | NOMBRE + APELLIDO | PUESTO_TRABAJO | — |
| HABILIDAD_POSTULANTE | NOMBRE + APELLIDO | NOMBRE_HABILIDAD | Chip nivel |
| POSTULACION | NOMBRE + APELLIDO | TITULO_PUESTO | Chip estado |
| CERTIFICACION | NOMBRE + APELLIDO | NOMBRE_CERTIFICACION | — |
| FORMACION_ACADEMICA | NOMBRE + APELLIDO | TITULO_OBTENIDO | — |
| OFERTA_ACADEMICA | ID_OFERTA_ACADEMICA | "Institución - Grado" | — |

### 18.3 DashboardFragment

Pantalla principal con:
- Toolbar con título "Bolsa de Trabajo"
- SearchView para filtrar tablas por nombre
- RecyclerView en grid de 2 columnas con `DashboardAdapter`
- Tarjetas que muestran nombre de tabla + conteo de registros
- Filtro automático por rol del usuario
- Botón de seed data (solo visible para admin)
- Botón de alternar tema claro/oscuro
- Botón de cerrar sesión

---

## 19. Flujo de Navegación

```
[LoginFragment] ←→ [RegisterFragment]
      ↓
[DashboardFragment]
      ↓ (click en tarjeta)
[TableDetailFragment]
      ↓ (click en registro)
[EditorDialogFragment] (crear/editar/ver)
      ↓ (click en eliminar)
[DeleteConfirmDialog]
```

Transiciones con animaciones slide (300ms):
- `slide_in_left.xml` / `slide_out_right.xml` (hacia adelante)
- `slide_in_right.xml` / `slide_out_left.xml` (hacia atrás)

---

## 20. Cumplimiento del Enunciado

### 20.1 Análisis Frase por Frase

| Frase del Enunciado | Estado | Implementación |
|--------------------|--------|----------------|
| "gestionar las empresas que desean ofertar puestos de trabajo" | ✅ | Tabla EMPRESA con CRUD completo para Gerente de Empresa |
| "colocar los perfiles de manera que sean parametrizables y descriptibles" | ✅ | OFERTA_TRABAJO con campos parametrizables (edad, experiencia, grado académico) y descripción libre VARCHAR(5000) |
| "comparar los criterios de selección de la empresa con el currículo" | ⚠️ Parcial | Los datos están disponibles para comparación visual. No hay motor de matching automático |
| "permitir que la persona que desea colocar su Curriculum Vitae tenga todos los elementos" | ✅ | 6 tablas vinculadas al postulante: EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, CERTIFICACION, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE, POSTULACION |
| "datos personales como sus nombres, apellidos, genero, fecha de nacimientos" | ✅ | POSTULANTE: NOMBRE, APELLIDO, ID_GENERO, FECHA_NACIMIENTO |
| "documento de identidad personal si es nacional o extranjero, pasaporte" | ✅ | POSTULANTE: ID_TIPO_DOCUMENTO + NUM_DOCUMENTO. TIPO_DOCUMENTO incluye DUI, NIT, Pasaporte |
| "número único provisional (NUP)" | ✅ | POSTULANTE: NUP VARCHAR(20) |
| "dirección, teléfono de casa, personal u otro, datos de contacto, correo electrónico" | ✅ | POSTULANTE: DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, EMAIL |
| "redes sociales" | ✅ | RED_SOCIAL_POSTULANTE vinculado a RED_SOCIAL (con URL de perfil) |
| "experiencia laboral: puesto, periodo, funciones, organización, contacto" | ✅ | EXPERIENCIA_LABORAL: PUESTO_TRABAJO, FECHA_INICIO, FECHA_FIN, DESCP_EXPERIENCIA_LABORAL, NIT (organización), CONTACTO_REFERENCIA |
| "conocimientos académicos: títulos, diplomas, cursos, institución, fecha" | ✅ | FORMACION_ACADEMICA: TITULO_OBTENIDO, PERIODO, FECHA_OBTENCION, ID_OFERTA_ACADEMICA (que referencia INSTITUCION y GRADO_ACADEMICO) |
| "certificaciones: código, nombre, institución, periodo" | ✅ | CERTIFICACION: ID_CERTIFICACION, NOMBRE_CERTIFICACION, ID_INSTITUCION, FECHA_CERTIFICACION, PERIODO, ID_TIPO_CERTIFICACION |
| "habilidades técnicas categorizadas" | ✅ | CATEGORIA_HABILIDAD → HABILIDAD → HABILIDAD_POSTULANTE con nivel de destreza |
| "el sistema no debe permitir ingresar postulantes que tengan grado de bachiller o inferior" | ✅ | Trigger `TR_POSTULANTE_GRADO` que verifica que el grado académico no sea "Bachiller" |

### 20.2 ¿Qué Falta o se Puede Mejorar?

| Aspecto | Estado Actual | Mejora Propuesta |
|---------|--------------|------------------|
| **Motor de Matching** | ❌ No implementado | Algoritmo que compare requisitos de oferta (DETALLE_REQUISITO) con perfil del postulante (habilidades, formación, experiencia) y genere puntaje de compatibilidad |
| **Notificaciones** | ❌ No implementado | Notificar a postulantes cuando una nueva oferta coincida con su perfil |
| **Adjuntar CV** | ❌ No implementado | Permitir subir archivos PDF (currículum vitae) asociados al postulante |
| **Flujo de Estados en Postulación** | ⚠️ Parcial | El campo ESTADO_PROCESO existe (Activo, En Proceso, Contratado, Rechazado) pero no hay un workflow automatizado con transiciones válidas |
| **Búsqueda Avanzada de Ofertas** | ⚠️ Parcial | Solo búsqueda por texto. No hay filtros por rango de edad, experiencia, grado académico |
| **Dashboard de Postulante** | ❌ No implementado | Mostrar estadísticas de postulaciones (activas, rechazadas, en proceso) |
| **Paginación** | ❌ No implementado | Las listas cargan todos los registros en memoria |
| **Backend Remoto** | ❌ No implementado | Base de datos completamente local, sin sincronización con servidor |
| **Cálculo Exacto de Edad** | ⚠️ Parcial | `strftime('%Y', 'now') - strftime('%Y', FECHA_NACIMIENTO)` solo resta años, no considera día/mes exacto |
| **Trigger de Password** | ⚠️ Limitado | Verifica longitud del hash (128 chars), no del texto plano |

---

## 21. Limitaciones Conocidas

1. **Trigger de password verifica hash, no texto plano:** `TR_USUARIO_FORMATO` mide la longitud del hash PBKDF2 (128 caracteres hex), que siempre supera 8. La validación real de longitud se hace en código (`InputMaskUtils.isValidPassword()`).
2. **Cálculo de edad aproximado:** `strftime('%Y', 'now') - strftime('%Y', FECHA_NACIMIENTO)` solo resta años, no considera el día exacto del cumpleaños.
3. **Sin ORM:** Todo el SQL es manual (1069 líneas en MainRepository), lo que hace el código verboso pero da control total.
4. **Sin paginación:** Las listas cargan todos los registros en memoria sin paginación.
5. **Sin backend remoto:** Base de datos completamente local SQLite, no hay API, servidor ni sincronización.
6. **Sin motor de matching automático:** La comparación entre requisitos de oferta y perfil del postulante es visual/manual.

---

## 22. Resumen Técnico

| Componente | Detalle |
|-----------|---------|
| **Lenguaje** | Kotlin 100% |
| **Arquitectura** | MVVM + Repository |
| **Base de Datos** | SQLite (23 tablas, 23 índices, 27 triggers) |
| **Hashing** | PBKDF2WithHmacSHA256 (65536 iteraciones, salt 16 bytes) |
| **Validaciones** | 3 capas: UI (inline), Repositorio (duplicados), BD (triggers) |
| **Roles** | 3: Administrador (FULL), Postulante (autogestión), Gerente Empresa (ofertas) |
| **CRUD** | Genérico para 23 tablas con metadatos en Constants.kt |
| **Navegación** | Navigation Component con animaciones slide |
| **Tema** | Claro/Oscuro con persistencia en SharedPreferences |
| **Máscaras** | DUI, NIT, Teléfono, Período con validación dinámica |
| **Seed Data** | 262 distritos, 44 municipios, 15 habilidades, 10 empresas |
| **Traducción Errores** | TriggerErrorTranslator con ~50 entradas |
| **Chip Estado** | VIGENTE (verde) / VENCIDA (rojo) para ofertas de trabajo |
| **SDK** | Min 24 (Android 7.0), Target 36 (Android 16) |
