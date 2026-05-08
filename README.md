# FICHA TÉCNICA — Bolsa de Trabajo (BT)

## 1. Resumen Ejecutivo

**Proyecto:** Bolsa de Trabajo en Línea  
**Plataforma:** Android (Kotlin)  
**Base de Datos:** SQLite (local)  
**Arquitectura:** MVVM + Repository Pattern  
**Base de Datos:** 22 tablas, 23 índices, 14 triggers semánticos  
**Roles:** Administrador, Postulante, Gerente de Empresa  
**Versión BD:** 11  

**Propósito:** Sistema de intermediación laboral que permite a postulantes registrar su perfil profesional (experiencia, formación, certificaciones, habilidades) y postularse a ofertas de trabajo publicadas por empresas. Los gerentes de empresa publican ofertas laborales con requisitos parametrizables.

---

## 2. Arquitectura del Proyecto

### 2.1 Árbol de Archivos

```
app/src/main/java/sv/ues/fia/eisi/bt/
│
├── BTApplication.kt              # Application class - inicialización global
├── MainActivity.kt                # Activity principal - host de navegación
│
├── data/
│   ├── local/
│   │   ├── ConnectionHelper.kt    # SQLiteOpenHelper - creación de BD, tablas, índices y triggers
│   │   │
│   │   ├── dao/                   # Data Access Objects - operaciones CRUD por tabla
│   │   │   ├── CategoriaHabilidadDao.kt
│   │   │   ├── CertificacionDao.kt
│   │   │   ├── DepartamentoDao.kt
│   │   │   ├── DetalleRequisitoDao.kt
│   │   │   ├── DistritoDao.kt
│   │   │   ├── EmpresaDao.kt
│   │   │   ├── ExperienciaLaboralDao.kt
│   │   │   ├── FormacionAcademicaDao.kt
│   │   │   ├── GeneroDao.kt
│   │   │   ├── GradoAcademicoDao.kt
│   │   │   ├── HabilidadDao.kt
│   │   │   ├── HabilidadPostulanteDao.kt
│   │   │   ├── InstitucionDao.kt
│   │   │   ├── MunicipioDao.kt
│   │   │   ├── OfertaAcademicaDao.kt
│   │   │   ├── OfertaTrabajoDao.kt
│   │   │   ├── PostulacionDao.kt
│   │   │   ├── PostulanteDao.kt
│   │   │   ├── RedSocialDao.kt
│   │   │   ├── RedSocialPostulanteDao.kt
│   │   │   ├── TipoDocumentoDao.kt
│   │   │   └── UsuarioDao.kt
│   │   │
│   │   └── entities/              # Data Classes - modelos por tabla
│   │       ├── CategoriaHabilidad.kt
│   │       ├── Certificacion.kt
│   │       ├── Departamento.kt
│   │       ├── DetalleRequisito.kt
│   │       ├── Distrito.kt
│   │       ├── Empresa.kt
│   │       ├── ExperienciaLaboral.kt
│   │       ├── FormacionAcademica.kt
│   │       ├── Genero.kt
│   │       ├── GradoAcademico.kt
│   │       ├── Habilidad.kt
│   │       ├── HabilidadPostulante.kt
│   │       ├── Institucion.kt
│   │       ├── Municipio.kt
│   │       ├── OfertaAcademica.kt
│   │       ├── OfertaTrabajo.kt
│   │       ├── Postulacion.kt
│   │       ├── Postulante.kt
│   │       ├── RedSocial.kt
│   │       ├── RedSocialPostulante.kt
│   │       ├── TipoDocumento.kt
│   │       └── Usuario.kt
│   │
│   └── repository/
│       ├── MainRepository.kt      # Repositorio central - CRUD genérico, consultas, seed
│       └── SeedData.kt            # Datos de prueba para 12 tablas
│
├── ui/
│   ├── auth/
│   │   ├── LoginFragment.kt       # Pantalla de inicio de sesión
│   │   └── RegisterFragment.kt    # Pantalla de registro
│   │
│   ├── crud/
│   │   ├── TableDetailFragment.kt # Vista de detalle de tabla (lista + búsqueda)
│   │   ├── TableAdapter.kt        # Adaptador RecyclerView para listar registros
│   │   ├── EditorDialogFragment.kt# Diálogo para crear/editar/ver registros
│   │   └── DeleteConfirmDialog.kt # Diálogo de confirmación de eliminación
│   │
│   └── dashboard/
│       ├── DashboardFragment.kt   # Pantalla principal con grid de tablas
│       └── DashboardAdapter.kt    # Adaptador para grid del dashboard
│
├── utils/
│   ├── Constants.kt               # Constantes globales (roles, tablas, columnas, accesos)
│   ├── InputMaskUtils.kt          # Máscaras de entrada (DUI, NIT, teléfono)
│   ├── PasswordHasher.kt          # Hashing de contraseñas (SHA-256)
│   ├── StyledToast.kt             # Toast personalizado
│   ├── ThemeToggleHelper.kt       # Alternar tema claro/oscuro
│   ├── TriggerErrorTranslator.kt  # Traducción de errores de triggers SQLite
│   └── ValidationRules.kt         # Reglas de validación por tabla/campo
│
└── viewmodel/
    ├── AuthViewModel.kt           # Lógica de autenticación (login, registro)
    ├── CrudViewModel.kt           # Lógica CRUD (carga, inserción, actualización, eliminación)
    └── DashboardViewModel.kt      # Lógica del dashboard (tablas, seed)
```

### 2.2 Librerías Relevantes

| Librería | Propósito |
|----------|-----------|
| `androidx.navigation` | Navegación entre fragmentos (Login → Dashboard → TableDetail → Editor) |
| `androidx.recyclerview` | Listas virtualizadas con DiffUtil (TableAdapter, DashboardAdapter) |
| `com.google.android.material` | Material Design 3 (CardView, Button, Dialog, DatePicker, SearchView, Snackbar/Toast) |
| `androidx.lifecycle` | ViewModel + LiveData (separación de datos y UI) |
| `kotlinx.coroutines` | Operaciones asíncronas en segundo plano (Dispatchers.IO) |
| `SQLite (nativo)` | Base de datos local sin ORM (SQL puro) |
| `SHA-256 (java.security)` | Hashing de contraseñas |

### 2.3 Arquitectura MVVM

```
[Fragment] ←→ [ViewModel] ←→ [Repository] ←→ [DAO] ←→ [ConnectionHelper/SQLiteDB]
     ↓              ↓
[Adapter]      [LiveData]
     ↓
[RecyclerView]
```

El flujo de datos es unidireccional:
1. El Fragment observa LiveData del ViewModel
2. El ViewModel usa corrutinas para operaciones en segundo plano
3. El Repository contiene la lógica de negocio
4. Los DAOs realizan consultas SQL directas

---

## 3. Base de Datos — 22 Tablas

### 3.1 Tablas AUTOINCREMENTALES (7)

#### CATEGORIA_HABILIDAD
```sql
CREATE TABLE CATEGORIA_HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_CATEGORIA VARCHAR(50)
);
```

#### GENERO
```sql
CREATE TABLE GENERO (
    ID_GENERO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_GENERO VARCHAR(20)
);
```

#### TIPO_DOCUMENTO
```sql
CREATE TABLE TIPO_DOCUMENTO (
    ID_TIPO_DOCUMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_TIPO VARCHAR(25)
);
```

#### DEPARTAMENTO
```sql
CREATE TABLE DEPARTAMENTO (
    ID_DEPARTAMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_DEPARTAMENTO VARCHAR(50)
);
```

#### GRADO_ACADEMICO
```sql
CREATE TABLE GRADO_ACADEMICO (
    ID_GRADO_ACADEMICO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_GRADO VARCHAR(50)
);
```

#### RED_SOCIAL
```sql
CREATE TABLE RED_SOCIAL (
    ID_RED_SOCIAL INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_RED VARCHAR(50)
);
```

#### USUARIO
```sql
CREATE TABLE USUARIO (
    ID_USUARIO INTEGER PRIMARY KEY AUTOINCREMENT,
    USERNAME VARCHAR(30),
    PASSWORD VARCHAR(128),
    ROL VARCHAR(20)
);
```

### 3.2 Tablas con PK Compuesta (10)

#### MUNICIPIO
```sql
CREATE TABLE MUNICIPIO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    NOMBRE_MUNICIPIO VARCHAR(50),
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO),
    FOREIGN KEY (ID_DEPARTAMENTO) REFERENCES DEPARTAMENTO (ID_DEPARTAMENTO)
);
```

#### DISTRITO
```sql
CREATE TABLE DISTRITO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO INTEGER NOT NULL,
    NOMBRE_DISTRITO VARCHAR(50),
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO),
    FOREIGN KEY (ID_DEPARTAMENTO, ID_MUNICIPIO) REFERENCES MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO)
);
```

#### OFERTA_TRABAJO
```sql
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
```

#### DETALLE_REQUISITO
```sql
CREATE TABLE DETALLE_REQUISITO (
    NIT VARCHAR(20) NOT NULL,
    ID_OFERTA VARCHAR(10) NOT NULL,
    ID_DETALLE VARCHAR(10) NOT NULL,
    DESCRIPCION_REQUISITO VARCHAR(100),
    PRIMARY KEY (NIT, ID_OFERTA, ID_DETALLE),
    FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA)
);
```

#### EXPERIENCIA_LABORAL
```sql
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
```

#### CERTIFICACION
```sql
CREATE TABLE CERTIFICACION (
    ID_CERTIFICACION VARCHAR(10) NOT NULL,
    ID_INSTITUCION VARCHAR(20) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    NOMBRE_CERTIFICACION VARCHAR(150),
    FECHA_CERTIFICACION DATE,
    PRIMARY KEY (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
);
```

#### FORMACION_ACADEMICA
```sql
CREATE TABLE FORMACION_ACADEMICA (
    ID_FORMACION VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_OFERTA_ACADEMICA VARCHAR(10),
    TITULO_OBTENIDO VARCHAR(150),
    FECHA_OBTENCION DATE,
    PRIMARY KEY (ID_FORMACION, ID_POSTULANTE),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_OFERTA_ACADEMICA) REFERENCES OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA)
);
```

#### HABILIDAD_POSTULANTE
```sql
CREATE TABLE HABILIDAD_POSTULANTE (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    NIVEL_DESTREZA VARCHAR(12),
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD) REFERENCES HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
);
```

#### RED_SOCIAL_POSTULANTE
```sql
CREATE TABLE RED_SOCIAL_POSTULANTE (
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_RED_SOCIAL INTEGER NOT NULL,
    URL_PERFIL VARCHAR(100),
    PRIMARY KEY (ID_POSTULANTE, ID_RED_SOCIAL),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_RED_SOCIAL) REFERENCES RED_SOCIAL (ID_RED_SOCIAL)
);
```

#### POSTULACION
```sql
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
```

### 3.3 Tablas con PK Manual VARCHAR (5)

#### INSTITUCION
```sql
CREATE TABLE INSTITUCION (
    ID_INSTITUCION VARCHAR(20) PRIMARY KEY,
    NOMBRE_INSTITUCION VARCHAR(150)
);
```

#### HABILIDAD
```sql
CREATE TABLE HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    NOMBRE_HABILIDAD VARCHAR(100),
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD) REFERENCES CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD)
);
```

#### EMPRESA
```sql
CREATE TABLE EMPRESA (
    NIT VARCHAR(20) PRIMARY KEY,
    ID_DISTRITO_DEPTO INTEGER NOT NULL,
    ID_DISTRITO_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO_ID INTEGER NOT NULL,
    NOMBRE_EMPRESA VARCHAR(150),
    CONTACTO_DIRECTO VARCHAR(100),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
);
```

#### POSTULANTE
```sql
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
);
```

#### OFERTA_ACADEMICA
```sql
CREATE TABLE OFERTA_ACADEMICA (
    ID_OFERTA_ACADEMICA VARCHAR(10) PRIMARY KEY,
    ID_GRADO_ACADEMICO INTEGER,
    ID_INSTITUCION VARCHAR(20),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO)
);
```

---

## 4. Índices (23)

```sql
-- Ubicación geográfica
CREATE INDEX IF NOT EXISTS IDX_MUNICIPIO_DEPTO ON MUNICIPIO (ID_DEPARTAMENTO);
CREATE INDEX IF NOT EXISTS IDX_DISTRITO_MUNICIPIO ON DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO);

-- Habilidades
CREATE INDEX IF NOT EXISTS IDX_HABILIDAD_CATEGORIA ON HABILIDAD (ID_CATEGORIA_HABILIDAD);

-- Empresa
CREATE INDEX IF NOT EXISTS IDX_EMPRESA_DISTRITO ON EMPRESA (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID);

-- Postulante
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

-- Formacion Academica
CREATE INDEX IF NOT EXISTS IDX_FORM_POSTULANTE ON FORMACION_ACADEMICA (ID_POSTULANTE);

-- Habilidad Postulante
CREATE INDEX IF NOT EXISTS IDX_HAB_POST_POSTULANTE ON HABILIDAD_POSTULANTE (ID_POSTULANTE);
CREATE INDEX IF NOT EXISTS IDX_HAB_POST_HABILIDAD ON HABILIDAD_POSTULANTE (ID_HABILIDAD);

-- Postulacion
CREATE INDEX IF NOT EXISTS IDX_POSTULACION_POSTULANTE ON POSTULACION (ID_POSTULANTE);
CREATE INDEX IF NOT EXISTS IDX_POSTULACION_OFERTA ON POSTULACION (NIT, ID_OFERTA);

-- Red Social Postulante
CREATE INDEX IF NOT EXISTS IDX_RED_POST_POSTULANTE ON RED_SOCIAL_POSTULANTE (ID_POSTULANTE);

-- Oferta Academica
CREATE INDEX IF NOT EXISTS IDX_OA_INSTITUCION ON OFERTA_ACADEMICA (ID_INSTITUCION);
CREATE INDEX IF NOT EXISTS IDX_OA_GRADO ON OFERTA_ACADEMICA (ID_GRADO_ACADEMICO);
```

---

## 5. Triggers (14)

### 5.1 Triggers Semánticos (6) — Validan reglas de negocio

#### TR_POSTULANTE_EDAD / TR_POSTULANTE_EDAD_UPD

```sql
CREATE TRIGGER TR_POSTULANTE_EDAD BEFORE INSERT ON POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_NACIMIENTO > date('now')
    THEN RAISE(ABORT, 'La fecha de nacimiento no puede ser futura') END;
    SELECT CASE WHEN (strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)) < 18
    THEN RAISE(ABORT, 'El postulante debe ser mayor de edad') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | POSTULANTE |
| **Evento** | INSERT / UPDATE |
| **Propósito** | Validar que la fecha de nacimiento no sea futura y que el postulante sea mayor de 18 años |
| **Cálculo de edad** | `strftime('%Y', 'now') - strftime('%Y', FECHA_NACIMIENTO)` — resta los años. No es exacto al día pero es una validación general |

#### TR_OFERTA_RANGO_EDAD / TR_OFERTA_RANGO_EDAD_UPD

```sql
CREATE TRIGGER TR_OFERTA_RANGO_EDAD BEFORE INSERT ON OFERTA_TRABAJO
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.EDAD_MINIMA < 18
    THEN RAISE(ABORT, 'Edad minima debe ser mayor o igual a 18') END;
    SELECT CASE WHEN NEW.EDAD_MINIMA > NEW.EDAD_MAXIMA
    THEN RAISE(ABORT, 'Edad minima no puede ser mayor a la maxima') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | OFERTA_TRABAJO |
| **Evento** | INSERT / UPDATE |
| **Propósito** | La edad mínima de una oferta no puede ser menor a 18 (no se permiten ofertas para menores de edad). La edad mínima no puede superar la edad máxima |

#### TR_OFERTA_VIGENCIA / TR_OFERTA_VIGENCIA_UPD

```sql
CREATE TRIGGER TR_OFERTA_VIGENCIA BEFORE INSERT ON OFERTA_TRABAJO
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_CADUCIDAD <= NEW.FECHA_PUBLICACION
    THEN RAISE(ABORT, 'La oferta ya caduco o fecha invalida') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | OFERTA_TRABAJO |
| **Evento** | INSERT / UPDATE |
| **Propósito** | La fecha de caducidad debe ser posterior a la fecha de publicación. Una oferta no puede caducar antes o el mismo día que se publica |

#### TR_POSTULACION_VIGENCIA

```sql
CREATE TRIGGER TR_POSTULACION_VIGENCIA BEFORE INSERT ON POSTULACION
FOR EACH ROW BEGIN
    SELECT CASE WHEN (
        SELECT FECHA_CADUCIDAD FROM OFERTA_TRABAJO
        WHERE NIT = NEW.NIT AND ID_OFERTA = NEW.ID_OFERTA
    ) < date('now')
    THEN RAISE(ABORT, 'La oferta de trabajo ha vencido') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | POSTULACION |
| **Evento** | INSERT (solo creación, no UPDATE para no bloquear cambios de estado) |
| **Propósito** | No permitir postularse a una oferta cuya fecha de caducidad ya pasó. Consulta la oferta referenciada y compara su FECHA_CADUCIDAD con la fecha actual |

#### TR_EXP_LABORAL_FECHAS / TR_EXP_LABORAL_FECHAS_UPD

```sql
CREATE TRIGGER TR_EXP_LABORAL_FECHAS BEFORE INSERT ON EXPERIENCIA_LABORAL
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_INICIO >= NEW.FECHA_FIN
    THEN RAISE(ABORT, 'Fecha inicio debe ser menor a fecha fin') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | EXPERIENCIA_LABORAL |
| **Evento** | INSERT / UPDATE |
| **Propósito** | La fecha de inicio de una experiencia laboral debe ser anterior a la fecha de finalización |

#### TR_HABILIDAD_NIVEL / TR_HABILIDAD_NIVEL_UPD

```sql
CREATE TRIGGER TR_HABILIDAD_NIVEL BEFORE INSERT ON HABILIDAD_POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.NIVEL_DESTREZA NOT IN ('Básico', 'Intermedio', 'Avanzado')
    THEN RAISE(ABORT, 'Nivel de destreza debe ser Basico, Intermedio o Avanzado') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | HABILIDAD_POSTULANTE |
| **Evento** | INSERT / UPDATE |
| **Propósito** | El nivel de destreza solo puede ser "Básico", "Intermedio" o "Avanzado" |

#### TR_USUARIO_FORMATO / TR_USUARIO_FORMATO_UPD

```sql
CREATE TRIGGER TR_USUARIO_FORMATO BEFORE INSERT ON USUARIO
FOR EACH ROW BEGIN
    SELECT CASE WHEN LENGTH(NEW.PASSWORD) < 8
    THEN RAISE(ABORT, 'Password minimo 8 caracteres') END;
END
```

| Aspecto | Descripción |
|---------|-------------|
| **Tabla** | USUARIO |
| **Evento** | INSERT / UPDATE |
| **Propósito** | La contraseña debe tener al menos 8 caracteres. NOTA: Esta validación verifica el hash, no la contraseña en texto plano |

### 5.2 Triggers de Borrado en Cascada (3)

#### TR_DEL_DEPARTAMENTO

```sql
CREATE TRIGGER TR_DEL_DEPARTAMENTO BEFORE DELETE ON DEPARTAMENTO
FOR EACH ROW BEGIN
    DELETE FROM MUNICIPIO WHERE ID_DEPARTAMENTO = OLD.ID_DEPARTAMENTO;
END
```

| Propósito | Al eliminar un departamento, elimina todos sus municipios |
|-----------|----------------------------------------------------------|

#### TR_DEL_CATEGORIA

```sql
CREATE TRIGGER TR_DEL_CATEGORIA BEFORE DELETE ON CATEGORIA_HABILIDAD
FOR EACH ROW BEGIN
    DELETE FROM HABILIDAD_POSTULANTE WHERE ID_CATEGORIA_HABILIDAD = OLD.ID_CATEGORIA_HABILIDAD;
    DELETE FROM HABILIDAD WHERE ID_CATEGORIA_HABILIDAD = OLD.ID_CATEGORIA_HABILIDAD;
END
```

| Propósito | Al eliminar una categoría, elimina todas sus habilidades y las asignaciones de postulantes |
|-----------|------------------------------------------------------------------------------------------|

#### TR_DEL_POSTULANTE

```sql
CREATE TRIGGER TR_DEL_POSTULANTE BEFORE DELETE ON POSTULANTE
FOR EACH ROW BEGIN
    DELETE FROM POSTULACION WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
    DELETE FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
    DELETE FROM FORMACION_ACADEMICA WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
    DELETE FROM CERTIFICACION WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
    DELETE FROM HABILIDAD_POSTULANTE WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
    DELETE FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = OLD.ID_POSTULANTE;
END
```

| Propósito | Al eliminar un postulante, elimina todos sus registros asociados (postulaciones, experiencias, formaciones, certificaciones, habilidades, redes sociales) |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|

### 5.3 Triggers de Integridad Referencial (5)

Estos triggers verifican que las FK apunten a registros existentes antes de INSERT/UPDATE. SQLite por defecto soporta FK con `PRAGMA foreign_keys = ON`, pero estos triggers son una capa adicional de seguridad con mensajes de error más descriptivos.

| Trigger | Tabla | Columna FK | Referencia | Mensaje de Error |
|---------|-------|-----------|------------|-------------------|
| TR_MUNICIPIO_DEPTO | MUNICIPIO | ID_DEPARTAMENTO | DEPARTAMENTO | "El departamento asociado no existe" |
| TR_DISTRITO_MUNICIPIO | DISTRITO | (ID_DEPARTAMENTO, ID_MUNICIPIO) | MUNICIPIO | "El municipio asociado no existe" |
| TR_HABILIDAD_CATEGORIA | HABILIDAD | ID_CATEGORIA_HABILIDAD | CATEGORIA_HABILIDAD | "La categoria asociada no existe" |
| TR_EMPRESA_DISTRITO | EMPRESA | (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) | DISTRITO | "El distrito asociado no existe" |
| TR_POSTULANTE_FK | POSTULANTE | ID_GENERO, ID_TIPO_DOCUMENTO | GENERO, TIPO_DOCUMENTO | "El genero/documento asociado no existe" |

---

## 6. Validaciones a Nivel de Código

### 6.1 Estructura de Validación

Todas las validaciones se centralizan en `ValidationRules.kt` mediante el objeto `ValidationRules`. La función principal es:

```kotlin
fun validate(tableName: String, column: String, value: String): String?
```

Retorna `null` si es válido, o un mensaje de error si no cumple.

### 6.2 Tabla: Reglas por Campo

#### USUARIO

| Campo | Regla | Descripción |
|-------|-------|-------------|
| USERNAME | required, minLength=3 | Obligatorio, mínimo 3 caracteres |
| PASSWORD | required, minLength=8 | Obligatorio, mínimo 8 caracteres |
| ROL | required | Obligatorio |

#### POSTULANTE

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_POSTULANTE | required, pattern=`^[A-Z]{2}\d{5}$`, maxLength=7 | 2 letras mayúsculas + 5 dígitos |
| NOMBRE | required | Obligatorio |
| APELLIDO | required | Obligatorio |
| NUM_DOCUMENTO | required | Obligatorio |
| EMAIL | required, pattern=`^[^@]+@[^@]+\.[^@]+$` | Formato email básico |
| FECHA_NACIMIENTO | required, pattern=`^\d{4}-\d{2}-\d{2}$` | Formato YYYY-MM-DD |
| ID_GENERO | required | Obligatorio |
| ID_TIPO_DOCUMENTO | required | Obligatorio |
| NUP | required | Obligatorio |
| DIRECCION_DETALLE | required | Obligatorio |
| TELEFONO_CASA | required | Obligatorio |
| TELEFONO_CELULAR | required | Obligatorio |

#### EMPRESA

| Campo | Regla | Descripción |
|-------|-------|-------------|
| NIT | required, minLength=14, maxLength=14, pattern=`^\d{14}$` | Exactamente 14 dígitos |
| NOMBRE_EMPRESA | required | Obligatorio |
| CONTACTO_DIRECTO | required, minLength=9, maxLength=9, pattern=`^\d{4}-\d{4}$` | Formato XXXX-XXXX |

#### OFERTA_TRABAJO

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_OFERTA | required, pattern=`^OF\d{2,}$`, maxLength=10 | Prefijo OF + mínimo 2 dígitos |
| TITULO_PUESTO | required | Obligatorio |
| FECHA_PUBLICACION | required, pattern, **no futura** | YYYY-MM-DD, no puede ser después de hoy |
| FECHA_CADUCIDAD | required, pattern | YYYY-MM-DD |
| EXPERIENCIA_ANIOS | required | Obligatorio |
| EDAD_MINIMA | min=16, max=100 | Entre 16 y 100 (la BD con trigger exige >=18) |
| EDAD_MAXIMA | min=16, max=100 | Entre 16 y 100 |
| DESCRIPCION_OFERTA_TRABAJO | required | Obligatorio |

#### CERTIFICACION

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_CERTIFICACION | required, pattern=`^C\d{3,}$`, maxLength=10 | Prefijo C + mínimo 3 dígitos |
| NOMBRE_CERTIFICACION | required | Obligatorio |
| FECHA_CERTIFICACION | required, pattern, **no futura** | YYYY-MM-DD, no puede ser después de hoy |

#### EXPERIENCIA_LABORAL

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_EXPERIENCIA | required, pattern=`^EL\d{2,}$`, maxLength=10 | Prefijo EL + mínimo 2 dígitos |
| PUESTO_TRABAJO | required | Obligatorio |
| FECHA_INICIO | required, pattern | YYYY-MM-DD |
| FECHA_FIN | required, pattern, **no futura** | YYYY-MM-DD, no puede ser después de hoy |
| DESCP_EXPERIENCIA_LABORAL | required | Obligatorio |
| CONTACTO_REFERENCIA | required | Obligatorio |

#### FORMACION_ACADEMICA

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_FORMACION | required, pattern=`^FOA\d{3,}$`, maxLength=10 | Prefijo FOA + mínimo 3 dígitos |
| TITULO_OBTENIDO | required | Obligatorio |
| FECHA_OBTENCION | required, pattern, **no futura** | YYYY-MM-DD, no puede ser después de hoy |

#### POSTULACION

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_POSTULACION | required, pattern=`^POS\d{3,}$`, maxLength=10 | Prefijo POS + mínimo 3 dígitos |
| FECHA_APLICACION | required, pattern, **no futura** | YYYY-MM-DD, no puede ser después de hoy |
| ESTADO_PROCESO | required | Obligatorio |

#### HABILIDAD

| Campo | Regla | Descripción |
|-------|-------|-------------|
| ID_CATEGORIA_HABILIDAD | required | Obligatorio |
| ID_HABILIDAD | required, pattern=`^H\d{2,}$`, maxLength=10 | Prefijo H + mínimo 2 dígitos |
| NOMBRE_HABILIDAD | required | Obligatorio |

#### Otras Tablas

| Tabla | Campo | Regla |
|-------|-------|-------|
| INSTITUCION | ID_INSTITUCION | required, pattern=`^[A-Za-z]{2,}\d{2,}$`, maxLength=20 |
| INSTITUCION | NOMBRE_INSTITUCION | required |
| DETALLE_REQUISITO | ID_DETALLE | required, pattern=`^D\d{1,}$`, maxLength=10 |
| DETALLE_REQUISITO | DESCRIPCION_REQUISITO | required |
| OFERTA_ACADEMICA | ID_OFERTA_ACADEMICA | required, pattern=`^OFA\d{2,}$`, maxLength=10 |
| RED_SOCIAL_POSTULANTE | URL_PERFIL | required, pattern=`^https?://.*` |
| MUNICIPIO | ID_MUNICIPIO | required |
| MUNICIPIO | NOMBRE_MUNICIPIO | required |
| DISTRITO | ID_DISTRITO | required |
| DISTRITO | NOMBRE_DISTRITO | required |
| CATEGORIA_HABILIDAD | NOMBRE_CATEGORIA | required |
| GENERO | NOMBRE_GENERO | required |
| TIPO_DOCUMENTO | NOMBRE_TIPO | required |
| DEPARTAMENTO | NOMBRE_DEPARTAMENTO | required |
| GRADO_ACADEMICO | NOMBRE_GRADO | required |
| RED_SOCIAL | NOMBRE_RED | required |
| HABILIDAD_POSTULANTE | NIVEL_DESTREZA | required |

### 6.3 Validaciones de Fecha Futura (Código)

En `ValidationRules.kt`, función `validate()`, líneas 154-168:

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

Usa UTC para ser consistente con el fix del DatePicker.

---

## 7. Máscaras de Entrada (InputMaskUtils)

### 7.1 Formato DUI

**Formato esperado:** `XXXXXXXX-X` (9 dígitos + guión)

```kotlin
fun formatDUI(text: String): String {
    val digits = text.filter { it.isDigit() }.take(9)
    return when {
        digits.length > 8 -> "${digits.substring(0, 8)}-${digits.substring(8)}"
        else -> digits
    }
}
```

**Longitud máxima:** 10 caracteres (8 dígitos + guión + 1 dígito verificador)

### 7.2 Formato NIT

**Formato esperado:** `XXXX-XXXXXX-XXX-X` (14 dígitos + 3 guiones)

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

### 7.3 Formato NIT Simple (Empresa)

**Formato esperado:** `XXXXXXXXXXXXXX` (14 dígitos sin guiones)

```kotlin
fun formatNitSimple(text: String): String {
    return text.filter { it.isDigit() }.take(14)
}
```

### 7.4 Formato Teléfono

**Formato esperado:** `XXXX-XXXX` (8 dígitos + guión)

```kotlin
fun formatTelefono(text: String): String {
    val digits = text.filter { it.isDigit() }.take(8)
    return if (digits.length > 4) {
        "${digits.substring(0, 4)}-${digits.substring(4)}"
    } else {
        digits
    }
}
```

**Longitud máxima:** 9 caracteres (4 dígitos + guión + 4 dígitos)

### 7.5 Aplicación por Tabla

| Tabla | Campo | Máscara | Longitud |
|-------|-------|---------|----------|
| POSTULANTE | NUM_DOCUMENTO | DUI o NIT según tipo de documento | 17 |
| EMPRESA | NIT | NIT Simple (14 dígitos) | 14 |
| EMPRESA | CONTACTO_DIRECTO | Teléfono XXXX-XXXX | 9 |
| EXPERIENCIA_LABORAL | CONTACTO_REFERENCIA | Teléfono XXXX-XXXX | 9 |
| Cualquier TELEFONO | TELEFONO_* | Teléfono XXXX-XXXX | 9 |

---

## 8. Roles y Permisos

### 8.1 Definición de Roles

```kotlin
const val ROLE_ADMIN = "administrador"
const val ROLE_POSTULANTE = "postulante"
const val ROLE_EMPRESA = "gerente de empresa"
```

### 8.2 Niveles de Acceso por Tabla

```kotlin
enum class AccessLevel { NONE, READ_ONLY, FULL }

fun getRoleTables(role: String): Map<String, AccessLevel> {
    return when (role) {
        "administrador" -> mapOf(
            "CATEGORIA_HABILIDAD" to FULL, "GENERO" to FULL,
            "TIPO_DOCUMENTO" to FULL, "DEPARTAMENTO" to FULL,
            "MUNICIPIO" to FULL, "DISTRITO" to FULL,
            "INSTITUCION" to FULL, "GRADO_ACADEMICO" to FULL,
            "RED_SOCIAL" to FULL, "OFERTA_ACADEMICA" to FULL,
            "HABILIDAD" to FULL, "EMPRESA" to FULL,
            "OFERTA_TRABAJO" to FULL, "DETALLE_REQUISITO" to FULL,
            "POSTULANTE" to FULL, "EXPERIENCIA_LABORAL" to FULL,
            "FORMACION_ACADEMICA" to FULL, "CERTIFICACION" to FULL,
            "HABILIDAD_POSTULANTE" to FULL, "POSTULACION" to FULL,
            "RED_SOCIAL_POSTULANTE" to FULL, "USUARIO" to FULL
        )
        "postulante" -> mapOf(
            "DEPARTAMENTO" to READ_ONLY, "MUNICIPIO" to READ_ONLY,
            "DISTRITO" to READ_ONLY, "INSTITUCION" to READ_ONLY,
            "GRADO_ACADEMICO" to READ_ONLY, "RED_SOCIAL" to READ_ONLY,
            "OFERTA_ACADEMICA" to READ_ONLY, "CATEGORIA_HABILIDAD" to READ_ONLY,
            "HABILIDAD" to READ_ONLY, "GENERO" to READ_ONLY,
            "TIPO_DOCUMENTO" to READ_ONLY, "EMPRESA" to READ_ONLY,
            "OFERTA_TRABAJO" to READ_ONLY, "DETALLE_REQUISITO" to READ_ONLY,
            "POSTULANTE" to FULL, "EXPERIENCIA_LABORAL" to FULL,
            "FORMACION_ACADEMICA" to FULL, "CERTIFICACION" to FULL,
            "HABILIDAD_POSTULANTE" to FULL, "POSTULACION" to FULL,
            "RED_SOCIAL_POSTULANTE" to FULL
        )
        "gerente de empresa" -> mapOf(
            "DEPARTAMENTO" to READ_ONLY, "MUNICIPIO" to READ_ONLY,
            "DISTRITO" to READ_ONLY, "INSTITUCION" to READ_ONLY,
            "GRADO_ACADEMICO" to READ_ONLY, "OFERTA_ACADEMICA" to READ_ONLY,
            "CATEGORIA_HABILIDAD" to READ_ONLY, "HABILIDAD" to READ_ONLY,
            "GENERO" to READ_ONLY, "TIPO_DOCUMENTO" to READ_ONLY,
            "EMPRESA" to FULL, "OFERTA_TRABAJO" to FULL,
            "DETALLE_REQUISITO" to FULL
        )
        else -> emptyMap()
    }
}
```

### 8.3 Resumen de Accesos por Rol

| Tabla | Admin | Postulante | Empresa |
|-------|-------|-----------|---------|
| USUARIO | FULL | - | - |
| CATEGORIA_HABILIDAD | FULL | READ_ONLY | READ_ONLY |
| GENERO | FULL | READ_ONLY | READ_ONLY |
| TIPO_DOCUMENTO | FULL | READ_ONLY | READ_ONLY |
| DEPARTAMENTO | FULL | READ_ONLY | READ_ONLY |
| MUNICIPIO | FULL | READ_ONLY | READ_ONLY |
| DISTRITO | FULL | READ_ONLY | READ_ONLY |
| INSTITUCION | FULL | READ_ONLY | READ_ONLY |
| GRADO_ACADEMICO | FULL | READ_ONLY | READ_ONLY |
| RED_SOCIAL | FULL | READ_ONLY | - |
| OFERTA_ACADEMICA | FULL | READ_ONLY | READ_ONLY |
| HABILIDAD | FULL | READ_ONLY | READ_ONLY |
| EMPRESA | FULL | READ_ONLY | **FULL** |
| OFERTA_TRABAJO | FULL | READ_ONLY | **FULL** |
| DETALLE_REQUISITO | FULL | READ_ONLY | **FULL** |
| POSTULANTE | FULL | **FULL** | - |
| EXPERIENCIA_LABORAL | FULL | **FULL** | - |
| FORMACION_ACADEMICA | FULL | **FULL** | - |
| CERTIFICACION | FULL | **FULL** | - |
| HABILIDAD_POSTULANTE | FULL | **FULL** | - |
| POSTULACION | FULL | **FULL** | - |
| RED_SOCIAL_POSTULANTE | FULL | **FULL** | - |

### 8.4 Funcionalidad por Rol

- **Administrador:** Acceso completo a todas las tablas. Puede ver el botón de seed data. Gestiona catálogos (departamentos, distritos, categorías, etc.). CRUD completo en todas las tablas.
- **Postulante:** Solo lectura en tablas catálogo. CRUD completo en sus propios datos (su perfil, experiencias, formaciones, certificaciones, habilidades, postulaciones, redes sociales). Puede postularse a ofertas vigentes.
- **Gerente de Empresa:** Solo lectura en tablas catálogo. CRUD completo en su empresa, las ofertas de trabajo de su empresa y los requisitos de esas ofertas.

---

## 9. Flujo de Autenticación

### 9.1 Registro

1. El usuario completa el formulario de registro (username, password, rol)
2. `AuthViewModel.register()` verifica que el username no exista
3. Si no existe, hashea la contraseña con `PasswordHasher.hash()` (SHA-256 + salt implícito)
4. Inserta en `USUARIO` y crea registro en `POSTULANTE` o `EMPRESA` según el rol
5. Redirige al login

### 9.2 Login

1. El usuario ingresa username y password
2. `AuthViewModel.login()` busca el usuario en BD
3. Compara el hash de la contraseña ingresada con el almacenado
4. Guarda sesión en `SharedPreferences` (username, rol, user_id)
5. Redirige al Dashboard

### 9.3 Protección de Rutas

- `DashboardFragment.onViewCreated()` verifica `KEY_IS_LOGGED_IN` en SharedPreferences
- Si no hay sesión, redirige al login
- Los botones de seed y logout solo aparecen para admin
- Los permisos CRUD se validan por rol al cargar cada tabla

---

## 10. Flujo de Borrado

### 10.1 Arquitectura del Borrado

El borrado tiene **3 capas de seguridad**:

1. **UI:** Diálogo de confirmación (`DeleteConfirmDialog.kt`) que muestra las dependencias que serán eliminadas en cascada
2. **ViewModel:** `CrudViewModel.deleteItem()` obtiene dependencias y ejecuta el borrado
3. **BD:** Triggers de cascada eliminan registros dependientes automáticamente

### 10.2 Diálogo de Confirmación (`DeleteConfirmDialog.kt`)

```kotlin
class DeleteConfirmDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar eliminacion")
            .setMessage("¿Estás seguro que deseas eliminar este registro? Esta accion no es revocable y los registros asociados tambien seran eliminados.")
            .setPositiveButton("Eliminar") { _, _ ->
                // Llama al ViewModel para ejecutar el borrado
                val tableName = arguments?.getString(Constants.BUNDLE_TABLE_NAME) ?: return@setPositiveButton
                viewModel.deleteItem(tableName, itemId)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}
```

### 10.3 Dependencias por Tabla

En `MainRepository.kt`, la función `getDeleteDependencies()` retorna las tablas que serán afectadas al eliminar un registro:

| Tabla Padre | Tablas Hijas (dependencias) |
|-------------|----------------------------|
| DEPARTAMENTO | MUNICIPIO |
| MUNICIPIO | DISTRITO |
| CATEGORIA_HABILIDAD | HABILIDAD |
| INSTITUCION | OFERTA_ACADEMICA, CERTIFICACION |
| GRADO_ACADEMICO | OFERTA_TRABAJO, OFERTA_ACADEMICA |
| EMPRESA | OFERTA_TRABAJO, CERTIFICACION, EXPERIENCIA_LABORAL |
| OFERTA_TRABAJO | DETALLE_REQUISITO, POSTULACION |
| OFERTA_ACADEMICA | FORMACION_ACADEMICA |
| POSTULANTE | CERTIFICACION, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, HABILIDAD_POSTULANTE, POSTULACION, RED_SOCIAL_POSTULANTE |
| HABILIDAD | HABILIDAD_POSTULANTE |
| RED_SOCIAL | RED_SOCIAL_POSTULANTE |
| DISTRITO | EMPRESA, POSTULANTE |
| GENERO | POSTULANTE |
| TIPO_DOCUMENTO | POSTULANTE |

### 10.4 Escenarios de Borrado

#### Escenario 1: Eliminar Postulante

1. Admin selecciona un postulante → presiona eliminar
2. `CrudViewModel` consulta dependencias: certificaciones, experiencias, formaciones, habilidades, postulaciones, redes sociales
3. `DeleteConfirmDialog` muestra advertencia
4. Usuario confirma → se ejecuta DELETE
5. **Trigger `TR_DEL_POSTULANTE`** elimina en cascada: POSTULACION, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, CERTIFICACION, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE
6. Toast de éxito

#### Escenario 2: Eliminar Departamento

1. Admin elimina un departamento
2. **Trigger `TR_DEL_DEPARTAMENTO`** elimina municipios y distritos
3. Si hay empresas o postulantes referenciando esos distritos, la FK lo impide
4. Si hay FK bloqueando, el trigger falla y `TriggerErrorTranslator` traduce el error

#### Escenario 3: Eliminar Empresa

1. Admin intenta eliminar una empresa
2. Si tiene ofertas de trabajo activas, la FK lo bloquea
3. Si no tiene ofertas, se elimina
4. **Trigger `TR_DEL_CATEGORIA`** style: no hay trigger de cascada para EMPRESA, la FK de OFERTA_TRABAJO lo protege

### 10.5 TriggerErrorTranslator

El `TriggerErrorTranslator` traduce los errores lanzados por los triggers SQLite a mensajes amigables para el usuario. El mapeo es por substring:

```kotlin
private val ERROR_MAP = mapOf(
    "Duplicado: Ya existe un registro con esa clave" to "...",
    "Fecha inicio debe ser menor a fecha fin" to "La fecha de inicio debe ser anterior...",
    "Edad minima no puede ser mayor a la maxima" to "...",
    "UNIQUE constraint failed" to "Ya existe un registro con esos datos",
    "FOREIGN KEY constraint failed" to "NO ES POSIBLE ELIMINAR ",
    "NOT NULL constraint failed" to "Un campo obligatorio esta vacio",
    // ... 30+ entradas
)
```

---

## 11. Flujo de Inserción y Doble Validación

Cada inserción pasa por **3 capas de validación**:

1. **Capa UI (EditorDialogFragment):** Validación inline con `ValidationRules.validate()` al escribir cada campo. Errores mostrados en el `TextInputLayout`.
2. **Capa Repositorio (MainRepository):** Verificación de duplicados con `checkDuplicateInsert()`. Transformación de strings (lowercase para nombres, etc.).
3. **Capa Base de Datos (Triggers):** Validaciones semánticas finales (edades, fechas, niveles de destreza, formato de password).

---

## 12. Seed Data

### 12.1 Tablas que Llena

| # | Tabla | Registros |
|---|-------|-----------|
| 1 | DEPARTAMENTO | 14 departamentos |
| 2 | GENERO | 5 géneros |
| 3 | CATEGORIA_HABILIDAD | 5 categorías |
| 4 | GRADO_ACADEMICO | 6 grados |
| 5 | RED_SOCIAL | 5 redes |
| 6 | INSTITUCION | 6 instituciones |
| 7 | MUNICIPIO | 44 municipios |
| 8 | DISTRITO | 262 distritos |
| 9 | HABILIDAD | 15 habilidades |
| 10 | EMPRESA | 10 empresas |
| 11 | TIPO_DOCUMENTO | 3 tipos (DUI, NIT, Pasaporte) |
| 12 | OFERTA_ACADEMICA | 5 ofertas académicas |

### 12.2 Verificación de Datos Existentes

Antes de insertar, verifica si **alguna** de las 12 tablas tiene registros. Si alguna tiene datos, retorna error "Ya existen datos en la base de datos" sin modificar nada.

### 12.3 Diálogo de Confirmación

El botón de seed solo es visible para **admin**. Al presionarlo, muestra un `MaterialAlertDialogBuilder` con:
- Título: "Insertar datos de prueba"
- Mensaje: "¿Estás seguro que deseas insertar los datos de prueba en la base de datos? Esta acción no se puede deshacer."
- Botón "Sí": Ejecuta `insertSeedData()`
- Botón "Cancelar": Cierra el diálogo

---

## 13. Chip VIGENTE/VENCIDA (OFERTA_TRABAJO)

### 13.1 Visualización en Tabla

En `TableAdapter.kt`, bloque `OFERTA_TRABAJO`, se compara `FECHA_CADUCIDAD` (columna index 5) con la fecha actual en UTC:

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

- **VIGENTE:** Fondo verde `#43A047`, texto blanco
- **VENCIDA:** Fondo rojo `#E53935`, texto blanco

### 13.2 Filtro en Búsqueda

En `TableDetailFragment.kt`, `filterItems()`, cuando la tabla es `OFERTA_TRABAJO` y el query contiene "vigente" o "vencida", se filtra por comparación de `FECHA_CADUCIDAD` con la fecha actual.

### 13.3 Filtro en Dropdowns

En `MainRepository.kt`, `getFilteredOptions()` para `OFERTA_TRABAJO`, la consulta SQL incluye `AND o.FECHA_CADUCIDAD >= date('now')` para solo mostrar ofertas vigentes en los dropdowns de POSTULACION y DETALLE_REQUISITO.

### 13.4 Trigger de Seguridad

El trigger `TR_POSTULACION_VIGENCIA` (BEFORE INSERT en POSTULACION) consulta la FECHA_CADUCIDAD de la OFERTA_TRABAJO referenciada y la compara con `date('now')`. Si está vencida, rechaza la inserción.

---

## 14. Calendario (DatePicker)

### 14.1 Problema Original

El `MaterialDatePicker` de Google Material Design devuelve timestamps en **UTC**. El código original los interpretaba en la **zona horaria local** del dispositivo, causando un desfase de -1 día en husos horarios negativos (ej: El Salvador UTC-6).

### 14.2 Solución Aplicada

En `EditorDialogFragment.kt`, `showDatePicker()`:

```kotlin
val utc = TimeZone.getTimeZone("UTC")
val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = utc }
// Calendar.getInstance() → Calendar.getInstance(utc)
// startMillis / endMillis usan UTC
// El callback del picker también usa Calendar.getInstance(utc)
```

---

## 15. Cumplimiento del Enunciado

### 15.1 Análisis Frase por Frase

| Frase del Enunciado | ¿Se Cumple? | Implementación |
|--------------------|-------------|----------------|
| "gestionar las empresas que desean ofertar puestos de trabajo" | ✅ | Tabla EMPRESA con CRUD. Gerente de empresa puede gestionar su empresa y ofertas |
| "colocar los perfiles de manera que sean parametrizables y descriptibles" | ✅ | OFERTA_TRABAJO tiene campos parametrizables (edad, experiencia, grado académico) y descripción libre |
| "comparar los criterios de selección de la empresa con el currículo" | ⚠️ Parcial | Los datos están disponibles para comparación manual. No hay un motor de matching automático |
| "permitir que la persona que desea colocar su Curriculum Vitae tenga todos los elementos" | ✅ | POSTULANTE + EXPERIENCIA_LABORAL + FORMACION_ACADEMICA + CERTIFICACION + HABILIDAD_POSTULANTE + RED_SOCIAL_POSTULANTE |
| "datos personales como sus nombres, apellidos, genero, fecha de nacimientos" | ✅ | POSTULANTE: NOMBRE, APELLIDO, ID_GENERO, FECHA_NACIMIENTO |
| "documento de identidad personal si es nacional o extranjero, pasaporte" | ✅ | POSTULANTE: ID_TIPO_DOCUMENTO + NUM_DOCUMENTO. TIPO_DOCUMENTO incluye DUI, NIT, Pasaporte |
| "número único provisional (NUP)" | ✅ | POSTULANTE: NUP |
| "dirección, teléfono de casa, personal u otro, datos de contacto, correo electrónico" | ✅ | POSTULANTE: DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, EMAIL |
| "redes sociales" | ✅ | RED_SOCIAL_POSTULANTE vinculado a RED_SOCIAL |
| "experiencia laboral: puesto, periodo, funciones, organización, contacto" | ✅ | EXPERIENCIA_LABORAL: PUESTO_TRABAJO, FECHA_INICIO, FECHA_FIN, DESCP_EXPERIENCIA_LABORAL, NIT (empresa), CONTACTO_REFERENCIA |
| "conocimientos académicos: títulos, diplomas, cursos, institución, fecha" | ✅ | FORMACION_ACADEMICA: TITULO_OBTENIDO, FECHA_OBTENCION, ID_OFERTA_ACADEMICA (que referencia INSTITUCION y GRADO_ACADEMICO) |
| "certificaciones: código, nombre, institución, periodo" | ✅ | CERTIFICACION: ID_CERTIFICACION, NOMBRE_CERTIFICACION, ID_INSTITUCION, FECHA_CERTIFICACION |
| "habilidades técnicas categorizadas" | ✅ | HABILIDAD categorizada por CATEGORIA_HABILIDAD. HABILIDAD_POSTULANTE asigna nivel de destreza |

### 15.2 Sugerencias para Mejorar el Cumplimiento

1. **Motor de Matching:** Agregar un algoritmo que compare los requisitos de una oferta (DETALLE_REQUISITO) con el perfil del postulante (habilidades, formación, experiencia) y genere un puntaje de compatibilidad.

2. **Notificaciones:** Notificar a postulantes cuando una nueva oferta coincida con su perfil.

3. **Adjuntar CV:** Permitir subir archivos PDF (currículum vitae) asociados al postulante.

4. **Estados de Postulación:** El campo ESTADO_PROCESO en POSTULACION ya existe, pero no hay un flujo automatizado (ej: "Revisado", "Entrevista", "Rechazado", "Contratado").

5. **Búsqueda Avanzada:** Filtrar ofertas por rango de edad, experiencia, grado académico, no solo por texto.

6. **Dashboard de Postulante:** Mostrar estadísticas de postulaciones (activas, rechazadas, en proceso).

---

## 16. Componentes UI Relevantes

### 16.1 TableAdapter

Adaptador genérico que renderiza cualquier tabla en un `RecyclerView`. Usa `DiffUtil.ItemCallback` para actualizaciones eficientes. Renderizado específico por tabla:

| Tabla | PK | Texto Principal | Texto Secundario |
|-------|-----|----------------|------------------|
| POSTULANTE | ID_POSTULANTE | Nombre | Apellido |
| OFERTA_TRABAJO | (NIT, ID_OFERTA) + chip | Título del puesto | Empresa |
| EMPRESA | NIT | Nombre empresa | Contacto directo |
| EXPERIENCIA_LABORAL | (ID_POSTULANTE, NIT, ID_EXPERIENCIA) | Postulante | Puesto |
| ... | ... | ... | ... |

### 16.2 EditorDialogFragment

Diálogo genérico que genera dinámicamente el formulario para cualquier tabla. Soporta:
- **Campos de texto** con `TextWatcher` para validación inline
- **Campos de fecha** con `MaterialDatePicker`
- **Dropdowns FK** con `MaterialAutoCompleteTextView` (carga dependiente)
- **Dropdowns de distrito** con lógica de 3 niveles (departamento → municipio → distrito)
- **Máscaras** DUI, NIT, teléfono
- **Modos:** Crear, Editar, Ver

### 16.3 Theme Toggle

Botón en Dashboard y TableDetail que alterna entre tema claro y oscuro usando `ThemeToggleHelper`. Persiste la preferencia en `SharedPreferences`.

---

## 17. Seguridad

### 17.1 Contraseñas

- Hash SHA-256 de 128 caracteres hexadecimal
- La contraseña se hashea ANTES de almacenar en BD
- El trigger `TR_USUARIO_FORMATO` verifica longitud del hash (no del texto plano, lo cual es una limitación)

### 17.2 Sesión

- Almacenada en `SharedPreferences` (privado a la app)
- Al cerrar sesión, se limpian todas las preferencias
- No hay tokens JWT ni refresh tokens (app local, sin backend)

### 17.3 Validaciones

- Doble capa: código + triggers BD
- Traducción de errores de BD a mensajes amigables
- FK con `PRAGMA foreign_keys = ON` + triggers adicionales

---

## 18. Limitaciones Conocidas

1. **Trigger de password verifica hash, no texto plano:** `TR_USUARIO_FORMATO` mide la longitud del hash, no del password original. Si el hash mide 128 caracteres, siempre pasa la validación.
2. **Cálculo de edad aproximado:** `strftime('%Y', 'now') - strftime('%Y', FECHA_NACIMIENTO)` solo resta años, no considera el día exacto.
3. **Sin ORM:** Todo el SQL es manual, lo que hace el código más verboso pero da control total sobre las consultas.
4. **Sin paginación:** Las listas cargan todos los registros en memoria.
5. **Sin backend remoto:** Base de datos completamente local, no hay sincronización con servidor.
