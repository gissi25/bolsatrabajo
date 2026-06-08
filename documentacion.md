# Documentación Técnica — Bolsa de Trabajo (BT)

---

## 1. Introducción

### 1.1 Descripción General del Proyecto

**Bolsa de Trabajo (BT)** es una aplicación móvil nativa para Android que funciona como un sistema de intermediación laboral. Permite la gestión completa de ofertas de trabajo, postulantes, empresas, postulaciones y todos los elementos asociados a un proceso de reclutamiento y selección de personal.

La aplicación opera con una base de datos **SQLite** local embebida en el dispositivo (`bolsadetabajo.db`), lo que la hace completamente autónoma sin necesidad de conexión a internet ni servidor remoto. Está diseñada para ser utilizada por tres tipos de usuarios con distintos niveles de acceso: **administradores**, **postulantes** (buscadores de empleo) y **gerentes de empresa** (reclutadores).

El sistema fue desarrollado en **Kotlin** siguiendo la arquitectura **MVVM (Model-View-ViewModel)** con el **patrón Repository**, utilizando componentes de Jetpack como **Navigation Component**, **LiveData**, **ViewModel**, **Coroutines** y **Material Design 3** para la interfaz de usuario.

- **Nombre del paquete:** `sv.ues.fia.eisi.bt`
- **Nombre de la aplicación:** "Bolsa de Trabajo"
- **ID de aplicación:** `sv.ues.fia.eisi.bt`
- **Versión:** 1.0
- **SDK mínimo:** API 24 (Android 7.0 Nougat)
- **SDK objetivo:** API 36 (Android 16)
- **Base de datos:** SQLite 3, nombre interno `bolsadetabajo.db`, versión 16

---

### 1.2 Objetivos del Sistema

| Objetivo | Descripción |
|----------|-------------|
| **Gestión de Postulantes** | Registrar, consultar, actualizar y eliminar información personal, académica, laboral y de habilidades de los buscadores de empleo |
| **Gestión de Empresas** | Administrar el catálogo de empresas registradas con su ubicación geográfica y datos de contacto |
| **Gestión de Ofertas** | Publicar ofertas de trabajo con requisitos, rango salarial indirecto (edad, experiencia, grado académico) y período de vigencia |
| **Gestión de Postulaciones** | Permitir que los postulantes apliquen a ofertas vigentes y que las empresas gestionen el estado del proceso (activo, en proceso, contratado, rechazado) |
| **Catálogos** | Mantener tablas de referencia (géneros, tipos de documento, departamentos, municipios, distritos, grados académicos, categorías de habilidad, habilidades, instituciones, redes sociales, tipos de certificación) |
| **Exportación de CV** | Generar un PDF con el currículum vitae completo del postulante incluyendo todos sus datos agrupados por secciones |
| **Exportación de Vacante** | Generar un PDF con el detalle completo de una oferta de trabajo incluyendo requisitos, descripción y datos de la empresa |
| **Control de Acceso** | Autenticar usuarios mediante PBKDF2 y restringir operaciones según el rol asignado |
| **Integridad de Datos** | Validar datos a nivel de base de datos (triggers semánticos y de integridad referencial), a nivel de repositorio (duplicados) y a nivel de UI (formatos, longitudes, patrones) |

---

### 1.3 Tecnologías Utilizadas

#### 1.3.1 Lenguaje y Entorno

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Kotlin** | 2.2.10 | Lenguaje de programación principal |
| **Java** | 11 (source/target) | Compatibilidad de compilación |
| **Android Gradle Plugin** | 8.x | Herramienta de compilación |
| **Gradle** | 9.3.1 | Sistema de construcción |
| **Android SDK** | API 36 (compileSdk) | SDK de desarrollo Android |

#### 1.3.2 Librerías y Frameworks (Android/Jetpack)

| Librería | Propósito |
|----------|-----------|
| **androidx.core.ktx** | Extensiones Kotlin para Android Core |
| **androidx.appcompat** | Compatibilidad hacia atrás de Material Design |
| **com.google.android.material** | Material Design 3 (MDC) para componentes UI |
| **androidx.constraintlayout** | Layout flexible basado en restricciones |
| **androidx.navigation-fragment** | Navegación entre fragmentos (NavController + NavHostFragment) |
| **androidx.navigation-ui** | Integración de navegación con ActionBar y menús |
| **androidx.lifecycle-viewmodel** | ViewModel para arquitectura MVVM |
| **androidx.lifecycle-runtime** | Lifecycle-aware components |
| **androidx.lifecycle-viewmodel-ktx** | Extensiones Kotlin para ViewModel |
| **androidx.fragment** | Fragmentos para la UI |
| **kotlinx-coroutines** | Programación asíncrona (Dispatchers.IO para BD) |
| **androidx.recyclerview** | Listas virtualizadas con RecyclerView |
| **androidx.cardview** | Tarjetas para el dashboard |

#### 1.3.3 Base de Datos

| Componente | Detalle |
|------------|---------|
| **Motor** | SQLite 3 (embebido en Android) |
| **Nombre BD** | `bolsadetabajo.db` |
| **Versión BD** | 16 |
| **Tablas** | 23 |
| **Índices** | 22 |
| **Triggers** | 27 (17 semánticos + 10 de integridad referencial) |
| **Hash de contraseñas** | PBKDF2WithHmacSHA256, 65536 iteraciones, salt de 16 bytes, key de 256 bits |
| **Capa de acceso** | `SQLiteOpenHelper` (ConnectionHelper.kt) |
| **FK habilitadas** | `PRAGMA foreign_keys = ON` |

#### 1.3.4 Otras Herramientas

| Herramienta | Propósito |
|-------------|-----------|
| **MaterialDatePicker** | Selector de fechas para campos del tipo DATE |
| **PdfDocument** (Android Canvas) | Generación de PDFs nativos (CV y Vacante) |
| **FileProvider** | Proveer URIs seguras para archivos PDF exportados |
| **SharedPreferences** | Almacenamiento de sesión (login persistente, preferencias de tema) |
| **StyledToast** | Toast personalizados con fondo redondeado |

---

### 1.4 Arquitectura del Sistema

La aplicación sigue la arquitectura **MVVM (Model-View-ViewModel)** con una capa de repositorio intermedia. El flujo de datos es unidireccional y se organiza en 4 capas:

```
┌─────────────────────────────────────────────────────────────────┐
│                        CAPA DE VISTA (UI)                       │
│                                                                 │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  Login   │  │  Dashboard   │  │ TableDetail  │              │
│  │ Fragment │  │  Fragment    │  │  Fragment    │              │
│  └────┬─────┘  └──────┬───────┘  └──────┬───────┘              │
│       │               │                 │                       │
│  ┌────▼─────┐  ┌──────▼───────┐  ┌──────▼───────┐              │
│  │ Register │  │ EditorDialog │  │ DeleteConfirm│              │
│  │ Fragment │  │  Fragment    │  │   Dialog     │              │
│  └──────────┘  └──────────────┘  └──────────────┘              │
│                                                                 │
│  Layouts XML: activity_main, fragment_login, fragment_dashboard,│
│  fragment_table_detail, dialog_editor, item_table_card, etc.    │
└──────────────────────────┬──────────────────────────────────────┘
                           │ LiveData (observación)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE VIEWMODEL                            │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  AuthViewModel   │  │  CrudViewModel   │  │DashboardVM   │  │
│  │ (login/register) │  │ (CRUD genérico)  │  │(tablas/seed) │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘  │
│           │                     │                     │         │
│      Resource<T>           Resource<T>           Resource<T>   │
│      Usuario               List<List<Any>>      DashboardItem  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Llamadas con coroutines (viewModelScope)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE REPOSITORIO (DAO)                     │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              MainRepository.kt (1312 líneas)            │    │
│  │                                                         │    │
│  │  • insertRecord(tableName, values) → INSERT genérico    │    │
│  │  • updateRecord(tableName, id, values) → UPDATE         │    │
│  │  • deleteRecord(tableName, id) → DELETE                 │    │
│  │  • searchTable(tableName, query) → SELECT con JOINs     │    │
│  │  • checkDuplicateInsert/Update → validación unicidad    │    │
│  │  • getFkReferences(tableName) → metadatos FK            │    │
│  │  • getDeleteDependencies → árbol de dependencias        │    │
│  │  • login(username, password) → auth con PBKDF2          │    │
│  │  • insertSeedData() → datos de prueba                   │    │
│  │  • getPostulantFullData / getOfertaFullData → PDF export│    │
│  └─────────────────────────┬───────────────────────────────┘    │
│                            │                                    │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │              SeedData.kt (171 líneas)                   │    │
│  │  Datos de prueba: 14 deptos, 44 municipios, 262        │    │
│  │  distritos, 5 categorías habilidad, 15 habilidades,     │    │
│  │  7 grados, 5 redes, 5 tipos certificación, 3 tipos     │    │
│  │  doc, 6 instituciones, 10 empresas, 5 ofertas académicas│    │
│  └─────────────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────────────┘
                           │ SQLiteDatabase (getDb())
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE DATOS (LOCAL)                         │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │         ConnectionHelper.kt (655 líneas)                │    │
│  │         extends SQLiteOpenHelper                        │    │
│  │                                                         │    │
│  │  • onCreate(): 23 CREATE TABLE + 22 CREATE INDEX        │    │
│  │               + 27 CREATE TRIGGER                       │    │
│  │  • onUpgrade(): DROP ALL TABLES + recreate              │    │
│  │  • onConfigure(): PRAGMA foreign_keys = ON              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              bolsadetabajo.db (SQLite)                   │    │
│   │  23 tablas, 22 índices, 27 triggers                     │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

#### 1.4.1 Capa de Presentación (UI)

La UI está compuesta por **fragmentos Android** que se comunican con los ViewModels a través de **LiveData**. Cada fragmento infla un layout XML de Material Design 3. Los fragments principales son:

| Fragmento | Archivo | Función |
|-----------|---------|---------|
| `LoginFragment` | `ui/auth/LoginFragment.kt` | Autenticación de usuarios |
| `RegisterFragment` | `ui/auth/RegisterFragment.kt` | Registro de nuevos usuarios |
| `DashboardFragment` | `ui/dashboard/DashboardFragment.kt` | Panel principal con tarjetas por tabla |
| `TableDetailFragment` | `ui/crud/TableDetailFragment.kt` | Listado, búsqueda y exportación por tabla |
| `EditorDialogFragment` | `ui/crud/EditorDialogFragment.kt` | Diálogo modal para crear/editar registros |
| `DeleteConfirmDialog` | `ui/crud/DeleteConfirmDialog.kt` | Confirmación de eliminación con dependencias |

#### 1.4.2 Capa de ViewModel

Los ViewModels exponen **LiveData** que los fragments observan. Contienen la lógica de negocio ligera y orquestan las llamadas al repositorio usando **coroutines** con `Dispatchers.IO` para operaciones de base de datos.

| ViewModel | Archivo | Responsabilidad |
|-----------|---------|-----------------|
| `AuthViewModel` | `viewmodel/AuthViewModel.kt` | Login, registro, estado de sesión |
| `CrudViewModel` | `viewmodel/CrudViewModel.kt` | CRUD genérico para las 23 tablas, carga de items, FK references, dependencias |
| `DashboardViewModel` | `viewmodel/DashboardViewModel.kt` | Carga de todas las tablas con conteos, filtrado, inserción de seed data |

#### 1.4.3 Capa de Repositorio

`MainRepository.kt` actúa como **DAO (Data Access Object)** único. Contiene toda la lógica de acceso a datos:

- **CRUD genérico**: Los métodos `insertRecord`, `updateRecord`, `deleteRecord`, `searchTable` aceptan el nombre de la tabla como parámetro, lo que permite manejar las 23 tablas con un solo conjunto de métodos.
- **Auto-incremento**: Detecta columnas auto-incrementales y calcula el siguiente ID automáticamente.
- **Normalización**: Convierte a minúsculas los campos de texto (nombres, emails, descripciones) antes de insertar/actualizar.
- **Detección de duplicados**: Verifica unicidad antes de insertar/actualizar usando reglas específicas por tabla.
- **Encriptación**: Aplica PBKDF2 a las contraseñas en el momento de insertar/actualizar la tabla USUARIO.
- **Integridad referencial programática**: El método `getDeleteDependencies` construye un árbol de dependencias para advertir al usuario antes de eliminar registros con hijos.

#### 1.4.4 Capa de Datos

`ConnectionHelper.kt` extiende `SQLiteOpenHelper` y maneja:

- **Creación del esquema**: 23 tablas con sus constraints (PK, FK, UNIQUE, NOT NULL) y 22 índices.
- **Triggers**: 27 triggers que se crean en `onCreate()`.
- **Migraciones**: `onUpgrade()` elimina todas las tablas y las recrea (estrategia de desarrollo, no recomendada para producción).
- **Foreign Keys**: Habilitadas vía `PRAGMA foreign_keys = ON` en `onConfigure()`.

---

### 1.5 Estructura de Navegación

La navegación entre pantallas se define en `res/navigation/nav_graph.xml` y se implementa con **Jetpack Navigation Component** (NavController + NavHostFragment).

```
                        ┌──────────────────┐
                        │   LoginFragment  │ ◄── Inicio (startDestination)
                        └────────┬─────────┘
                           ┌─────┴──────┐
                           ▼            ▼
                  ┌────────────┐  ┌──────────────┐
                  │ Register   │  │  Dashboard   │ (requiere auth)
                  │ Fragment   │  │  Fragment    │
                  └────────────┘  └──────┬───────┘
                                         │
                                    ┌────▼───────┐
                                    │  TableDetail│
                                    │  Fragment   │ (genérico, recibe tableName)
                                    └────┬───────┘
                                         │
                              ┌──────────┴──────────┐
                              ▼                     ▼
                     ┌──────────────┐     ┌──────────────────┐
                     │ EditorDialog │     │ DeleteConfirm    │
                     │  Fragment    │     │    Dialog        │
                     └──────────────┘     └──────────────────┘
```

- **Transiciones**: Animaciones slide_in_right / slide_out_left para navegación hacia adelante y slide_in_left / slide_out_right para retroceso.
- **Arguments**: `TableDetailFragment` recibe `tableName` (String) y `tableDisplayName` (String) como argumentos de navegación.
- **DialogFragment**: `EditorDialogFragment` se abre como diálogo modal sobre `TableDetailFragment` usando `show()`.
- **PopUpTo**: Al hacer login o logout, se limpia la pila de retroceso con `popUpToInclusive=true` para evitar que el usuario regrese a la pantalla anterior con el botón Back.

---

### 1.6 Roles de Usuario y Control de Acceso

La aplicación implementa un sistema de **3 roles** con niveles de acceso diferenciados. La definición de permisos se encuentra en `Constants.kt` (`getRoleTables()`) y se valida en `EditorDialogFragment.saveData()` antes de cada operación de escritura.

| Rol | Descripción | Tablas con FULL ACCESS | Tablas con READ ONLY |
|-----|-------------|----------------------|---------------------|
| **administrador** | Acceso total al sistema | **Todas las 23 tablas** | *(ninguna)* |
| **postulante** | Buscador de empleo | POSTULANTE, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, HABILIDAD_POSTULANTE, CERTIFICACION, RED_SOCIAL_POSTULANTE, POSTULACION | EMPRESA, OFERTA_TRABAJO, DETALLE_REQUISITO |
| **gerente de empresa** | Reclutador | EMPRESA, OFERTA_TRABAJO, DETALLE_REQUISITO, POSTULACION | POSTULANTE, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, HABILIDAD_POSTULANTE, CERTIFICACION, RED_SOCIAL_POSTULANTE |

Además del control por rol, existen reglas específicas de negocio:

- **POSTULACION modo edición:** Un postulante no puede modificar ninguna postulación (todos los campos se bloquean). Una empresa solo puede modificar el campo `ESTADO_PROCESO` (los demás se bloquean).
- **Auto-eliminación:** Un usuario no puede eliminar su propio registro de la tabla USUARIO mientras tenga la sesión activa.
- **Registro de usuarios:** Solo se permite el registro de nuevos usuarios con rol `postulante` (el rol `gerente de empresa` y `administrador` deben ser asignados por un administrador existente).

---

### 1.7 Convenciones del Proyecto

#### 1.7.1 Base de Datos

- **Nombres de tablas**: En mayúsculas con guiones bajos (`POSTULANTE`, `OFERTA_TRABAJO`).
- **Nombres de columnas**: En mayúsculas con guiones bajos (`ID_POSTULANTE`, `NOMBRE_EMPRESA`).
- **Prefijos de ID**: 
  - `ID_` para todas las claves primarias/foráneas.
  - Códigos manuales con prefijos descriptivos: `OF` (Oferta), `H` (Habilidad), `EL` (Experiencia Laboral), `FOA` (Formación Académica), `C` (Certificación), `POS` (Postulación), `D` (Detalle), `INS` (Institución), `OFA` (Oferta Académica).
- **Triggers**: Prefijo `TR_` seguido del nombre descriptivo. Sufijo `_UPD` para variante UPDATE.
- **Índices**: Prefijo `IDX_` seguido de la tabla y columna indexada.

#### 1.7.2 Código Kotlin

- **Paquete base**: `sv.ues.fia.eisi.bt`
- **Estructura de paquetes**: `data/` (local, repository), `ui/` (auth, crud, dashboard), `utils/`, `viewmodel/`.
- **Nombres de clases**: PascalCase (`MainRepository`, `EditorDialogFragment`).
- **Nombres de funciones**: camelCase (`insertRecord`, `getFkReferences`).
- **Constantes**: SCREAMING_SNAKE_CASE en companion object (`TABLE_POSTULANTE`, `ROLE_ADMIN`).

#### 1.7.3 UI/UX

- **Idioma**: Español (strings.xml completamente en español).
- **Tema**: Material Design 3, con soporte para tema oscuro (values-night/themes.xml).
- **Toast**: Personalizados con fondo redondeado (custom_toast.xml).
- **Animaciones**: Slide horizontal para transiciones entre fragments.

---

### 1.8 Resumen General de la Base de Datos

| Componente | Cantidad |
|------------|----------|
| **Tablas** | 23 |
| **Tablas catálogo** | 7 (CATEGORIA_HABILIDAD, GENERO, TIPO_DOCUMENTO, DEPARTAMENTO, GRADO_ACADEMICO, RED_SOCIAL, TIPO_CERTIFICACION) |
| **Tablas geográficas** | 2 (MUNICIPIO, DISTRITO) |
| **Tablas de sistema** | 1 (USUARIO) |
| **Tablas de habilidades/inst.** | 2 (INSTITUCION, HABILIDAD) |
| **Tablas principales** | 2 (EMPRESA, POSTULANTE) |
| **Tablas de ofertas** | 3 (OFERTA_ACADEMICA, OFERTA_TRABAJO, DETALLE_REQUISITO) |
| **Tablas child de postulante** | 5 (EXPERIENCIA_LABORAL, CERTIFICACION, FORMACION_ACADEMICA, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE) |
| **Tabla de postulación** | 1 (POSTULACION) |
| **Claves primarias simples (autoincrementales)** | 8 |
| **Claves primarias simples (manuales VARCHAR)** | 5 |
| **Claves primarias compuestas (2 columnas)** | 4 (MUNICIPIO, HABILIDAD, FORMACION_ACADEMICA, RED_SOCIAL_POSTULANTE) |
| **Claves primarias compuestas (3 columnas)** | 5 (DISTRITO, OFERTA_TRABAJO, DETALLE_REQUISITO, EXPERIENCIA_LABORAL, CERTIFICACION, HABILIDAD_POSTULANTE) |
| **Índices** | 22 |
| **Triggers de restricción CHECK** | 8 (4 pares INSERT/UPDATE) |
| **Triggers de validación temporal** | 9 (4 pares INSERT/UPDATE, 1 solo INSERT) |
| **Triggers de integridad referencial** | 10 (5 pares INSERT/UPDATE) |
| **Total triggers** | 27 |
| **Restricciones UNIQUE** | 23 |
| **Claves foráneas declarativas (SQLite)** | 22 |
| **Archivo DDL** | `esquema_bolsa_trabajo.sql` (302 líneas) |

---

## 2. Estructura del Proyecto

### 2.1 Árbol de Directorios Completo

A continuación se presenta la estructura completa del proyecto con todos los archivos fuente, recursos y configuración. Se excluyen los directorios generados por la compilación (`build/`, `.gradle/`, `.idea/caches/`, `.idea/libraries/`), el repositorio Git (`.git/`) y los archivos temporales de Kotlin (`.kotlin/`).

```
bolsatrabajo/
│
│   ═══════════════════════════════════════════════════════════════
│   ARCHIVOS RAÍZ (Configuración del proyecto)
│   ═══════════════════════════════════════════════════════════════
│
├── build.gradle.kts                    (4 líneas,   0.2 KB)
│   Gradle script raíz. Declara los plugins del proyecto
│   (solo android application). Delega al módulo app/.
│
├── settings.gradle.kts                 (25 líneas,  0.6 KB)
│   Configuración de Gradle. Define el nombre del proyecto
│   ("BT") e incluye el módulo :app. Repositorios: google(),
│   mavenCentral(), gradlePluginPortal().
│
├── gradle.properties                   (16 líneas,  0.9 KB)
│   Propiedades globales de Gradle: JVM args, GRADLE_USER_HOME,
│   caché de dependencias, parallel execution habilitado.
│
├── local.properties                    (8 líneas,   0.4 KB)
│   Ruta local del SDK de Android (generado automáticamente
│   por Android Studio, no se versiona en Git).
│
├── gradlew                             (223 líneas, 8.8 KB)
│   Gradle Wrapper para Unix/Linux/Mac. Script shell que
│   descarga y ejecuta la versión correcta de Gradle.
│
├── gradlew.bat                         (73 líneas,  2.9 KB)
│   Gradle Wrapper para Windows. Equivalente a gradlew
│   pero en Batch para cmd.exe.
│
│   ═══════════════════════════════════════════════════════════════
│   CARPETA GRADLE (Wrapper y catálogo de versiones)
│   ═══════════════════════════════════════════════════════════════
│
├── gradle/
│   ├── libs.versions.toml              (archivo TOML)
│   │   Catálogo de versiones centralizado. Define las versiones
│   │   de todas las dependencias (AndroidX, Material, Coroutines,
│   │   Navigation, Lifecycle) y sus librerías asociadas.
│   │
│   └── wrapper/
│       ├── gradle-wrapper.jar          (binario)
│       │   JAR del Gradle Wrapper. Contiene la lógica para
│       │   descargar y ejecutar Gradle 9.3.1.
│       │
│       └── gradle-wrapper.properties   (archivo propiedades)
│           Configuración del wrapper: distribución URL de
│           Gradle 9.3.1, tipo de distribución (bin).
│
│   ═══════════════════════════════════════════════════════════════
│   ARCHIVOS DE DOCUMENTACIÓN Y BASE DE DATOS (RAÍZ)
│   ═══════════════════════════════════════════════════════════════
│
├── documentacion.md                    (290+ líneas, 24.2 KB)
│   ← ESTE ARCHIVO. Documentación técnica completa del sistema.
│
├── README.md                           (1439 líneas, 73.9 KB)
│   README original del proyecto. Contiene documentación
│   exhaustiva de la base de datos: DDL completo, listado de
│   triggers, índices, instrucciones de instalación y uso.
│
├── esquema_bolsa_trabajo.sql           (271 líneas, 11.1 KB)
│   Script DDL para Power Designer. Contiene las sentencias
│   CREATE TABLE de las 23 tablas con sus PKs, FKs, UNIQUEs
│   e índices. Es la versión de referencia del esquema.
│   NO incluye triggers (los triggers están solo en
│   ConnectionHelper.kt como SQL embebido).
│
├── Triggers_BaseDatos_BolsaTrabajo.docx (180 párrafos, 22.8 KB)
│   Documento Word con el listado y descripción de los triggers
│   de la base de datos. Incluye código SQL de cada trigger,
│   su tipo (semántico/integridad), tabla afectada y momento
│   de disparo.
│
│   ═══════════════════════════════════════════════════════════════
│   MÓDULO APP (código fuente y recursos)
│   ═══════════════════════════════════════════════════════════════
│
└── app/
    ├── build.gradle.kts                 (65 líneas,  1.8 KB)
    │   Script de compilación del módulo app. Define:
    │   • namespace: sv.ues.fia.eisi.bt
    │   • compileSdk: 36, minSdk: 24, targetSdk: 36
    │   • Dependencias completas (AndroidX, Material, Navigation,
    │     Lifecycle, Coroutines, RecyclerView, Tests)
    │   • Java 11 source/target compatibility
    │
    ├── .gitignore                       (archivo texto)
    │   Reglas Git para el módulo app (ignora build/).
    │
    └── src/
        └── main/
            │
            │   ════════════════════════════════════════════════
            │   ANDROID MANIFEST
            │   ════════════════════════════════════════════════
            │
            ├── AndroidManifest.xml      (32 líneas,  1.3 KB)
            │   Manifiesto Android. Declara:
            │   • Application class: .BTApplication
            │   • Activity principal: .MainActivity (LAUNCHER)
            │   • FileProvider para exportar PDFs
            │   • Backup y data extraction rules
            │   • Tema por defecto: @style/Theme.BT
            │
            │   ════════════════════════════════════════════════
            │   CARPETA ASSETS (vacía)
            │   ════════════════════════════════════════════════
            │
            ├── assets/                  (vacío)
            │   Directorio para recursos raw empaquetados sin
            │   comprimir. Actualmente sin archivos.
            │
            │   ════════════════════════════════════════════════
            │   CÓDIGO FUENTE KOTLIN (24 archivos, 6.539 líneas)
            │   ════════════════════════════════════════════════
            │
            ├── java/
            │   └── sv/
            │       └── ues/
            │           └── fia/
            │               └── eisi/
            │                   └── bt/
            │                       │
            │                       ├── BTApplication.kt          (9 líneas,   0.3 KB)
            │                       │   Clase Application. Inicializa el modo
            │                       │   nocturno como desactivado (MODE_NIGHT_NO).
            │                       │   Punto de entrada de la aplicación.
            │                       │
            │                       ├── MainActivity.kt           (17 líneas,  0.7 KB)
            │                       │   Activity principal. Contiene el
            │                       │   NavHostFragment y el NavController.
            │                       │   Maneja el botón de navegación Up
            │                       │   (onSupportNavigateUp).
            │                       │
            │                       ├── data/
            │                       │   ├── local/
            │                       │   │   └── ConnectionHelper.kt (595 líneas, 31.9 KB)
            │                       │   │       Capa de base de datos. Extiende
            │                       │   │       SQLiteOpenHelper. Contiene:
            │                       │   │       • 23 CREATE TABLE con SQL embebido
            │                       │   │       • 22 CREATE INDEX
            │                       │   │       • 27 CREATE TRIGGER
            │                       │   │       • dropAllTables() para migraciones
            │                       │   │       • PRAGMA foreign_keys = ON
            │                       │   │       Versión BD: 16. Nombre: bolsadetabajo.db
            │                       │   │
            │                       │   └── repository/
            │                       │       ├── MainRepository.kt (1231 líneas, 63.6 KB)
            │                       │       │   ★ ARCHIVO MÁS GRANDE DEL PROYECTO ★
            │                       │       │   Capa de acceso a datos (DAO/Repository).
            │                       │       │   Contiene:
            │                       │       │   • insertRecord() — INSERT genérico
            │                       │       │   • updateRecord() — UPDATE genérico
            │                       │       │   • deleteRecord() — DELETE con PK
            │                       │       │   • deleteRecordByRow() — DELETE por fila
            │                       │       │   • searchTable() — SELECT con JOINs
            │                       │       │   • checkDuplicateInsert/Update — unicidad
            │                       │       │   • getDeleteDependencies() — árbol dependencias
            │                       │       │   • hasChildRecords() — verificación hijos
            │                       │       │   • getAllDependencies() — mapa FK por tabla
            │                       │       │   • login() — autenticación con PBKDF2
            │                       │       │   • register() — registro de usuarios
            │                       │       │   • insertSeedData() — datos de prueba
            │                       │       │   • getFkReferences() — metadatos de FK
            │                       │       │   • getDropdownOptions() — opciones para UI
            │                       │       │   • getFilteredOptions() — dropdowns en cascada
            │                       │       │   • getPostulantFullData() — CV completo
            │                       │       │   • getOfertaFullData() — vacante completa
            │                       │       │   • Data classes: Usuario, TableInfo, FkReference,
            │                       │       │     DependencyInfo
            │                       │       │
            │                       │       └── SeedData.kt        (143 líneas, 14.5 KB)
            │                       │           Datos de prueba para la base de datos.
            │                       │           Contiene 14 listas/constantes con datos
            │                       │           semilla para poblar el sistema:
            │                       │           • 14 departamentos de El Salvador
            │                       │           • 44 municipios agrupados por departamento
            │                       │           • 262 distritos con su jerarquía geográfica
            │                       │           • 5 categorías de habilidad
            │                       │           • 15 habilidades (3 por categoría)
            │                       │           • 7 grados académicos
            │                       │           • 5 redes sociales
            │                       │           • 5 tipos de certificación
            │                       │           • 3 tipos de documento
            │                       │           • 6 instituciones educativas
            │                       │           • 10 empresas con ubicación
            │                       │           • 5 ofertas académicas
            │                       │           • 2 usuarios de prueba
            │                       │
            │                       ├── ui/
            │                       │   ├── auth/
            │                       │   │   ├── LoginFragment.kt  (97 líneas,  4.3 KB)
            │                       │   │   │   Pantalla de inicio de sesión. Contiene:
            │                       │   │   │   • Formulario con username + password
            │                       │   │   │   • Botón "Iniciar Sesión" y "Registrarse"
            │                       │   │   │   • Botón "Insertar datos de prueba"
            │                       │   │   │   • Validación de campos no vacíos
            │                       │   │   │   • Navegación condicional: si es admin va a
            │                       │   │   │     dashboard, si no, mensaje de bienvenida
            │                       │   │   │   • Persistencia de sesión en SharedPreferences
            │                       │   │   │     (KEY_IS_LOGGED_IN, KEY_USER_ID, etc.)
            │                       │   │   │
            │                       │   │   └── RegisterFragment.kt (119 líneas, 5.7 KB)
            │                       │   │       Pantalla de registro de nuevos usuarios.
            │                       │   │       Contiene:
            │                       │   │       • Formulario: username, password, confirmar
            │                       │   │       • Validación: password >= 8 caracteres, match
            │                       │   │       • Username convertido a minúsculas
            │                       │   │       • Rol forzado a "postulante"
            │                       │   │       • Navegación a Login o Dashboard tras éxito
            │                       │   │
            │                       │   ├── crud/
            │                       │   │   ├── TableDetailFragment.kt (460 líneas, 21.9 KB)
            │                       │   │   │   ★ FRAGMENTO GENÉRICO PARA TODAS LAS TABLAS ★
            │                       │   │   │   Pantalla de detalle de tabla. Contiene:
            │                       │   │   │   • RecyclerView con TableAdapter
            │                       │   │   │   • Barra de búsqueda (filtra en tiempo real)
            │                       │   │   │   • FAB para agregar nuevo registro
            │                       │   │   │   • Menú contextual (editar, eliminar, exportar)
            │                       │   │   │   • Exportación a PDF (CV para POSTULANTE,
            │                       │   │   │     vacante para OFERTA_TRABAJO)
            │                       │   │   │   • Selector de ítem para acciones bulk
            │                       │   │   │   • Diálogo de confirmación de eliminación
            │                       │   │   │   • Abre EditorDialogFragment para crear/editar
            │                       │   │   │   • Control de permisos según rol
            │                       │   │   │
            │                       │   │   ├── EditorDialogFragment.kt (1610 líneas, 81.7 KB)
            │                       │   │   │   ★ ARCHIVO MÁS GRANDE DE UI ★
            │                       │   │   │   Diálogo modal genérico para crear y editar
            │                       │   │   │   registros de cualquier tabla. Contiene:
            │                       │   │   │   • Generación dinámica de campos según tabla
            │                       │   │   │   • TextInputEditText para campos de texto
            │                       │   │   │   • MaterialAutoCompleteTextView para FK dropdowns
            │                       │   │   │   • Dropdowns en cascada: Depto→Municipio→Distrito,
            │                       │   │   │     Categoría→Habilidad, Empresa→Oferta
            │                       │   │   │   • DatePicker de Material Design para fechas
            │                       │   │   │   • Dropdown de NIVEL_DESTREZA (Básico/Intermedio/
            │                       │   │   │     Avanzado)
            │                       │   │   │   • Dropdown de ESTADO_PROCESO (activo/en proceso/
            │                       │   │   │     contratado/rechazado)
            │                       │   │   │   • Dropdown de ROL (postulante/gerente/admon)
            │                       │   │   │   • Máscaras de entrada: DUI, NIT, teléfono
            │                       │   │   │   • Validación client-side con ValidationRules
            │                       │   │   │   • Validación de período de fechas (inicio < fin,
            │                       │   │   │     certificación/obtención post-período, etc.)
            │                       │   │   │   • Bloqueo de campos PK si tiene hijos
            │                       │   │   │   • Control de permisos por rol en POSTULACION
            │                       │   │   │   • Auto-guardado de sesión al editar USUARIO
            │                       │   │   │
            │                       │   │   ├── DeleteConfirmDialog.kt (249 líneas, 11.2 KB)
            │                       │   │   │   Diálogo de confirmación de eliminación.
            │                       │   │   │   Contiene:
            │                       │   │   │   • Consulta de dependencias en segundo plano
            │                       │   │   │   • Visualización de árbol de dependencias
            │                       │   │   │     (qué registros hijos se eliminarán en cascada)
            │                       │   │   │   • Lógica de PK compuesta (split por "|")
            │                       │   │   │   • Lógica de PK manual vs autoincremental
            │                       │   │   │   • Prevención de auto-eliminación de USUARIO
            │                       │   │   │   • Traducción de errores SQL
            │                       │   │   │
            │                       │   │   ├── TableAdapter.kt   (231 líneas, 13.0 KB)
            │                       │   │   │   Adaptador de RecyclerView para mostrar
            │                       │   │   │   filas de cualquier tabla. Contiene:
            │                       │   │   │   • Renderizado específico para 15 tablas:
            │                       │   │   │     POSTULANTE, EXPERIENCIA_LABORAL,
            │                       │   │   │     HABILIDAD_POSTULANTE, POSTULACION,
            │                       │   │   │     RED_SOCIAL_POSTULANTE, MUNICIPIO, DISTRITO,
            │                       │   │   │     HABILIDAD, OFERTA_TRABAJO, DETALLE_REQUISITO,
            │                       │   │   │     OFERTA_ACADEMICA, CERTIFICACION,
            │                       │   │   │     FORMACION_ACADEMICA, USUARIO, EMPRESA
            │                       │   │   │   • Chip de estado para OFERTA_TRABAJO y
            │                       │   │   │     DETALLE_REQUISITO (VIGENTE/VENCIDO)
            │                       │   │   │   • Selector de ítem con borde resaltado
            │                       │   │   │   • Botones de editar/eliminar condicionales
            │                       │   │   │   • DiffUtil para actualizaciones eficientes
            │                       │   │   │
            │                       │   │   └── (TableDetailFragment.kt — ya listado)
            │                       │   │
            │                       │   └── dashboard/
            │                       │       ├── DashboardFragment.kt (167 líneas, 7.6 KB)
            │                       │       │   Pantalla principal después del login.
            │                       │       │   Contiene:
            │                       │       │   • RecyclerView con secciones expandibles
            │                       │       │   • Secciones: CATÁLOGOS, EMPRESA, POSTULANTE
            │                       │       │   • Cada sección muestra tarjetas con nombre
            │                       │       │     de tabla y conteo de registros
            │                       │       │   • Barra de búsqueda de tablas
            │                       │       │   • Botón de cerrar sesión
            │                       │       │   • Botón de insertar datos de prueba (admin)
            │                       │       │   • Navegación a TableDetailFragment al
            │                       │       │     seleccionar una tabla
            │                       │       │
            │                       │       └── DashboardAdapter.kt (101 líneas, 4.5 KB)
            │                       │           Adaptador del dashboard. Renderiza:
            │                       │           • DashboardItem.Section — encabezado de
            │                       │             sección expandible/colapsable
            │                       │           • DashboardItem.Table — tarjeta de tabla
            │                       │             con icono, nombre y conteo
            │                       │
            │                       ├── utils/
            │                       │   ├── Constants.kt          (148 líneas, 9.0 KB)
            │                       │   │   ★ CENTRAL DE METADATOS DEL SISTEMA ★
            │                       │   │   Contiene todas las constantes del sistema:
            │                       │   │   • Nombres de las 23 tablas (TABLE_*)
            │                       │   │   • Lista completa ALL_TABLES
            │                       │   │   • getColumnsForTable() — columnas por tabla
            │                       │   │   • getPrimaryKeyColumns() — PKs por tabla
            │                       │   │   • getAutoGenColumn() — columna autoincremental
            │                       │   │   • getRoleTables() — matriz de permisos por rol
            │                       │   │   • Nombres de roles (ROLE_ADMIN, etc.)
            │                       │   │   • Claves SharedPreferences (PREFS_NAME, KEY_*)
            │                       │   │   • Bundle keys (BUNDLE_TABLE_NAME, etc.)
            │                       │   │   • AccessLevel enum (NONE, READ_ONLY, FULL)
            │                       │   │
            │                       │   ├── ValidationRules.kt    (180 líneas, 13.2 KB)
            │                       │   │   Motor de validación de datos. Contiene:
            │                       │   │   • Data class FieldRule (field, required,
            │                       │   │     minLength, maxLength, pattern, min, max,
            │                       │   │     friendlyName)
            │                       │   │   • getRulesForTable() — reglas para las 23 tablas
            │                       │   │   • validate() — función principal que ejecuta
            │                       │   │     todas las validaciones y retorna mensaje error
            │                       │   │   • Validaciones: requerido, longitud mín/máx,
            │                       │   │     regex pattern, fecha futura, rango numérico
            │                       │   │   • Patrones específicos: códigos (^OF\d{2,}$,
            │                       │   │     ^[A-Z]{2}\d{5}$, ^INS\d{3,}$, etc.), email
            │                       │   │     (^[^@]+@[^@]+\.[^@]+$), URL (^https?://.*),
            │                       │   │     fechas (^\d{4}-\d{2}-\d{2}$)
            │                       │   │
            │                       │   ├── InputMaskUtils.kt     (56 líneas,  2.2 KB)
            │                       │   │   Utilidades de formateo y validación de entrada:
            │                       │   │   • formatDUI() — formato 00000000-0
            │                       │   │   • formatNIT() — formato 0000-000000-000-0
            │                       │   │   • formatTelefono() — formato 0000-0000
            │                       │   │   • formatNitSimple() — solo dígitos (14)
            │                       │   │   • validatePassword() — mínimo 8 caracteres
            │                       │   │   • validateEmail() — patrón email Android
            │                       │   │   • validateFecha() — formato AAAA-MM-DD
            │                       │   │   • removeAccents() — extension function String
            │                       │   │   • Constantes: DUI_LENGTH(9), NUP_LENGTH(12),
            │                       │   │     MIN_PASSWORD(8), TELEFONO_LENGTH(8),
            │                       │   │     NIT_LENGTH_SIMPLE(14)
            │                       │   │
            │                       │   ├── PasswordHasher.kt     (52 líneas,  2.0 KB)
            │                       │   │   Implementación de hash de contraseñas:
            │                       │   │   • Algoritmo: PBKDF2WithHmacSHA256
            │                       │   │   • Iteraciones: 65536
            │                       │   │   • Longitud de clave: 256 bits
            │                       │   │   • Salt: 16 bytes aleatorios (SecureRandom)
            │                       │   │   • hash() — retorna salt:hash en hex
            │                       │   │   • verify() — verifica contra stored hash
            │                       │   │   • Fallback: SHA-256 si PBKDF2 no está disponible
            │                       │   │
            │                       │   ├── CVExportUtil.kt       (390 líneas, 17.8 KB)
            │                       │   │   Utilidad de exportación a PDF. Contiene:
            │                       │   │   • Data class PostulantFullData — modelo completo
            │                       │   │     del postulante con todas sus sub-entidades
            │                       │   │   • Data class OfertaFullData — modelo completo
            │                       │   │     de la oferta con empresa y requisitos
            │                       │   │   • generateCVPdf() — genera PDF de currículum
            │                       │   │     con secciones: Datos Personales, Formación
            │                       │   │     Académica, Certificaciones, Habilidades,
            │                       │   │     Experiencia Laboral, Redes Sociales
            │                       │   │   • generateOfertaPdf() — genera PDF de vacante
            │                       │   │     con: Empresa, Puesto, Requisitos, Descripción
            │                       │   │   • Sistema de paginación automática (PageManager)
            │                       │   │   • Diseño profesional: azul ACCENT, tipografía
            │                       │   │     sans-serif, columnas, bullets, wrap automático
            │                       │   │   • Guardado en filesDir/pdfs/ y URI con FileProvider
            │                       │   │
            │                       │   ├── TriggerErrorTranslator.kt (109 líneas, 10.8 KB)
            │                       │   │   Traductor de errores SQL a mensajes amigables.
            │                       │   │   Contiene mapa de 50+ reglas de traducción:
            │                       │   │   • Errores de constraint (UNIQUE, FK, NOT NULL)
            │                       │   │   • Errores de triggers semánticos (edad, fechas,
            │                       │   │     nivel destreza, grado académico)
            │                       │   │   • Errores de triggers referenciales (departamento
            │                       │   │     no existe, municipio no existe, etc.)
            │                       │   │   • Errores de eliminación con dependencias
            │                       │   │     (20 reglas de "No se puede eliminar")
            │                       │   │   • Errores de validación (password, email, fecha)
            │                       │   │   • translate() — busca coincidencia parcial
            │                       │   │     (ignoreCase) y retorna mensaje legible
            │                       │   │
            │                       │   ├── ThemeToggleHelper.kt   (38 líneas,  1.3 KB)
            │                       │   │   Utilidad de cambio de tema claro/oscuro.
            │                       │   │   Persiste la preferencia en SharedPreferences
            │                       │   │   y aplica AppCompatDelegate.setDefaultNightMode().
            │                       │   │
            │                       │   ├── StyledToast.kt         (45 líneas,  1.8 KB)
            │                       │   │   Toast personalizados con fondo redondeado
            │                       │   │   (GradientDrawable con cornerRadius 32dp y
            │                       │   │   color semi-transparente oscuro). Método show()
            │                       │   │   que acepta Context y mensaje.
            │                       │   │
            │                       │   └── (CVExportUtil.kt — ya listado)
            │                       │
            │                       └── viewmodel/
            │                           ├── AuthViewModel.kt      (62 líneas,  2.6 KB)
            │                           │   ViewModel de autenticación. Expone:
            │                           │   • loginResult: LiveData<Resource>
            │                           │   • registerResult: LiveData<Resource>
            │                           │   • login() — llama a repository.login()
            │                           │   • register() — llama a repository.register()
            │                           │   • Manejo de estados Success/Error con mensajes
            │                           │   • Resource sealed class (Success + Error)
            │                           │
            │                           ├── CrudViewModel.kt      (154 líneas,  7.1 KB)
            │                           │   ★ VIEWMODEL CENTRAL DE CRUD ★
            │                           │   ViewModel genérico para operaciones CRUD
            │                           │   sobre cualquier tabla. Expone:
            │                           │   • items: LiveData<List<List<Any>>>
            │                           │   • isLoading: LiveData<Boolean>
            │                           │   • operationResult: LiveData<Resource>
            │                           │   • deleteDependencies: LiveData<List<DependencyInfo>>
            │                           │   • setTable() — establece tabla actual y carga datos
            │                           │   • loadItems() — carga todos los registros
            │                           │   • deleteItem() / deleteItemByRow()
            │                           │   • insertRecord()
            │                           │   • updateRecord()
            │                           │   • checkDeleteDependencies()
            │                           │   • getFkReferences() / getDropdownOptions()
            │                           │   • getFilteredOptions()
            │                           │   • hasChildRecords()
            │                           │   • getPostulantFullData() / getOfertaFullData()
            │                           │
            │                           └── DashboardViewModel.kt (170 líneas,  8.6 KB)
            │                               ViewModel del dashboard. Expone:
            │                               • items: LiveData<List<DashboardItem>>
            │                               • isLoading: LiveData<Boolean>
            │                               • seedResult: LiveData<Resource>
            │                               • loadTables() — carga tablas filtradas por rol
            │                               • toggleSection() — expandir/colapsar sección
            │                               • filterTables() — búsqueda de tablas
            │                               • refreshCounts() — actualizar conteos
            │                               • insertSeedData() — insertar datos de prueba
            │                               • DashboardItem sealed class (Section + Table)
            │                               • Agrupación: CATÁLOGOS (13 tablas), EMPRESA
            │                                 (3 tablas), POSTULANTE (7 tablas)
            │
            │   ════════════════════════════════════════════════════════════
            │   RECURSOS ANDROID (XML, PNG, animaciones)
            │   ════════════════════════════════════════════════════════════
            │
            └── res/
                │
                ├── anim/                          (4 archivos XML)
                │   ├── slide_in_left.xml          (11 líneas, 0.3 KB)
                │   │   Animación: deslizar entrada desde la izquierda
                │   │   (usado en navegación hacia atrás).
                │   ├── slide_in_right.xml         (11 líneas, 0.3 KB)
                │   │   Animación: deslizar entrada desde la derecha
                │   │   (usado en navegación hacia adelante).
                │   ├── slide_out_left.xml         (11 líneas, 0.3 KB)
                │   │   Animación: deslizar salida hacia la izquierda
                │   │   (usado en navegación hacia adelante).
                │   └── slide_out_right.xml        (11 líneas, 0.3 KB)
                │       Animación: deslizar salida hacia la derecha
                │       (usado en navegación hacia atrás).
                │
                ├── drawable/                      (16 archivos)
                │   ├── badge_rounded.xml          (6 líneas,  0.2 KB)
                │   │   Drawable de forma ovalada para chips de estado.
                │   ├── bg_dialog.xml              (5 líneas,  0.2 KB)
                │   │   Fondo blanco redondeado para diálogos.
                │   ├── cerrar_sesion_c.xml        (10 líneas, 0.4 KB)
                │   │   Icono de cerrar sesión — versión "cerrado"
                │   │   (contraste, probablemente para modo no seleccionado).
                │   ├── cerrar_sesion_o.xml        (10 líneas, 0.4 KB)
                │   │   Icono de cerrar sesión — versión "abierto"
                │   │   (probablemente para modo seleccionado/resaltado).
                │   ├── ic_delete.xml              (10 líneas, 0.4 KB)
                │   │   Icono de eliminar (vector Drawable).
                │   ├── ic_edit.xml                (10 líneas, 0.5 KB)
                │   │   Icono de editar/lápiz (vector Drawable).
                │   ├── ic_insertar_datos_c.xml    (9 líneas,  1.2 KB)
                │   │   Icono de insertar datos — versión "cerrado".
                │   ├── ic_insertar_datos_o.xml    (9 líneas,  1.2 KB)
                │   │   Icono de insertar datos — versión "abierto".
                │   ├── ic_launcher_background.xml (10 líneas, 0.3 KB)
                │   │   Fondo del icono del launcher (blanco).
                │   ├── ic_launcher_foreground.xml (6 líneas,  0.2 KB)
                │   │   Primer plano del icono del launcher
                │   │   (vector adaptive icon).
                │   ├── logo_bt.png                (20 KB, binario PNG)
                │   │   Logo de la aplicación "BT" en PNG.
                │   ├── luna.xml                   (13 líneas, 0.6 KB)
                │   │   Icono de luna para toggle de tema oscuro
                │   │   (vector Drawable de Material Design).
                │   ├── sol.xml                    (13 líneas, 0.6 KB)
                │   │   Icono de sol para toggle de tema claro
                │   │   (vector Drawable de Material Design).
                │   └── toast_background.xml       (9 líneas,  0.3 KB)
                │       Fondo para los toast personalizados.
                │       GradientDrawable con cornerRadius 32dp
                │       y color oscuro semi-transparente.
                │
                ├── layout/                        (10 archivos XML)
                │   ├── activity_main.xml          (18 líneas,  0.9 KB)
                │   │   Layout de la Activity principal. Contiene
                │   │   un FragmentContainerView con NavHostFragment.
                │   │
                │   ├── fragment_login.xml         (112 líneas, 5.7 KB)
                │   │   Layout de inicio de sesión. Campos:
                │   │   • ImageView (logo_bt)
                │   │   • TextInputLayout + TextInputEditText (username)
                │   │   • TextInputLayout + TextInputEditText (password)
                │   │   • MaterialButton (Iniciar Sesión)
                │   │   • MaterialButton (Registrarse)
                │   │   • MaterialButton (Insertar datos de prueba)
                │   │
                │   ├── fragment_register.xml      (150 líneas, 7.8 KB)
                │   │   Layout de registro. Campos:
                │   │   • TextInputLayout (username)
                │   │   • TextInputLayout (password)
                │   │   • TextInputLayout (confirmar password)
                │   │   • MaterialButton (Registrarse)
                │   │   • MaterialButton (Volver al login)
                │   │
                │   ├── fragment_dashboard.xml     (82 líneas,  4.0 KB)
                │   │   Layout del dashboard:
                │   │   • AppBarLayout con Toolbar (título + toggle tema
                │   │     + cerrar sesión)
                │   │   • TextInputLayout con SearchView (búsqueda)
                │   │   • RecyclerView (lista de tablas por sección)
                │   │
                │   ├── fragment_table_detail.xml  (95 líneas,  4.5 KB)
                │   │   Layout de detalle de tabla:
                │   │   • AppBarLayout con Toolbar (título + botón exportar)
                │   │   • TextInputLayout con SearchView (búsqueda de registros)
                │   │   • RecyclerView (lista de registros)
                │   │   • FloatingActionButton (agregar registro)
                │   │
                │   ├── dialog_editor.xml          (53 líneas,  2.1 KB)
                │   │   Layout del editor de registros:
                │   │   • NestedScrollView
                │   │   • LinearLayout vertical para campos dinámicos
                │   │   • Botones Guardar/Cancelar
                │   │
                │   ├── custom_toast.xml           (15 líneas,  0.6 KB)
                │   │   Layout de toast personalizado: TextView con
                │   │   fondo redondeado y padding.
                │   │
                │   ├── item_table_card.xml        (52 líneas,  2.2 KB)
                │   │   Tarjeta individual del dashboard:
                │   │   • MaterialCardView con icono, nombre de tabla
                │   │     y conteo de registros.
                │   │
                │   ├── item_table_row.xml         (92 líneas,  4.3 KB)
                │   │   Fila individual en el listado de registros:
                │   │   • 3 TextViews (ID, Primary, Secondary)
                │   │   • Chip de estado (TextView estilizado)
                │   │   • 2 FABs (editar, eliminar)
                │   │
                │   └── item_section_header.xml    (39 líneas,  1.6 KB)
                │       Encabezado de sección colapsable en el
                │       dashboard: icono expandir/colapsar + título
                │       + conteo total de tablas en la sección.
                │
                ├── mipmap-anydpi-v26/             (2 archivos XML)
                │   ├── ic_launcher.xml            (5 líneas,  0.3 KB)
                │   │   Icono adaptativo del launcher (Android 8+).
                │   ├── ic_launcher_round.xml      (5 líneas,  0.3 KB)
                │   │   Icono adaptativo redondo del launcher.
                │
                ├── mipmap-hdpi/                   (1 archivo PNG)
                │   └── ic_launcher.png            (1.5 KB)
                │       Icono PNG para densidad hdpi (~240 dpi).
                │
                ├── mipmap-mdpi/                   (1 archivo PNG)
                │   └── ic_launcher.png            (1.0 KB)
                │       Icono PNG para densidad mdpi (~160 dpi).
                │
                ├── mipmap-xhdpi/                  (1 archivo PNG)
                │   └── ic_launcher.png            (1.9 KB)
                │       Icono PNG para densidad xhdpi (~320 dpi).
                │
                ├── mipmap-xxhdpi/                 (1 archivo PNG)
                │   └── ic_launcher.png            (2.9 KB)
                │       Icono PNG para densidad xxhdpi (~480 dpi).
                │
                ├── mipmap-xxxhdpi/                (1 archivo PNG)
                │   └── ic_launcher.png            (3.9 KB)
                │       Icono PNG para densidad xxxhdpi (~640 dpi).
                │
                ├── navigation/
                │   └── nav_graph.xml              (88 líneas,  3.5 KB)
                │       Grafo de navegación de Jetpack Navigation.
                │       Define todas las rutas de la aplicación:
                │       • loginFragment → registerFragment
                │       • loginFragment → dashboardFragment
                │       • registerFragment → loginFragment
                │       • registerFragment → dashboardFragment
                │       • dashboardFragment → tableDetailFragment
                │       • dashboardFragment → loginFragment
                │       • editorDialogFragment (dialog, accesible desde
                │         tableDetailFragment)
                │       Argumentos: tableName, tableDisplayName,
                │       isEditMode, itemData.
                │       Animaciones: slide_in/out para cada acción.
                │
                ├── values/
                │   ├── colors.xml                 (37 líneas,  1.8 KB)
                │   │   Paleta de colores de Material Design 3:
                │   │   • primary, onPrimary, primaryContainer
                │   │   • secondary, onSecondary, secondaryContainer
                │   │   • tertiary, onTertiary
                │   │   • error, onError
                │   │   • background, surface, outline
                │   │   • custom: primary_light, primary_dark,
                │   │     accent_blue, accent_green, accent_red
                │   │
                │   ├── strings.xml                (81 líneas,  5.2 KB)
                │   │   Todos los textos de la aplicación en español.
                │   │   Incluye: auth, dashboard, CRUD, diálogos,
                │   │   validaciones, navegación, seed data, permisos.
                │   │   80+ strings con soporte para format (%s, %d).
                │   │
                │   └── themes.xml                 (61 líneas,  3.3 KB)
                │       Tema claro (Theme.BT). Hereda de
                │       Theme.Material3.Light.NoActionBar.
                │       Personaliza: colorPrimary, colorSecondary,
                │       colorSurface, statusBar, navigationBar.
                │       Estilos para: AestheticDialog (diálogo CRUD),
                │       botones, FABs, chips.
                │
                ├── values-night/
                │   └── themes.xml                 (56 líneas,  3.2 KB)
                │       Tema oscuro (Theme.BT). Hereda de
                │       Theme.Material3.Dark.NoActionBar.
                │       Misma estructura que themes.xml pero con
                │       colores adaptados para modo oscuro.
                │
                └── xml/
                    ├── backup_rules.xml            (13 líneas,  0.5 KB)
                    │   Reglas de backup automático en Android.
                    │   Configura backup completo en la nube.
                    │
                    ├── data_extraction_rules.xml   (19 líneas,  0.6 KB)
                    │   Reglas de extracción de datos para Android 12+.
                    │   Permite extraer datos de la aplicación.
                    │
                    └── file_paths.xml              (4 líneas,   0.1 KB)
                        Configuración del FileProvider. Define la
                        ruta `filesDir/pdfs/` como ubicación para
                        compartir archivos PDF exportados.
```

### 2.2 Resumen de Tamaño y Complejidad

| Métrica | Valor |
|---------|-------|
| **Archivos Kotlin** | 24 |
| **Líneas de código Kotlin** | ~6.539 |
| **Archivos XML (layout + recursos)** | 30 |
| **Archivos de configuración** | 9 |
| **Archivos SQL/DDL** | 1 |
| **Documentos** | 3 (README.md, documentacion.md, Triggers.docx) |
| **Archivo más grande** | `MainRepository.kt` (1.231 líneas, 63.6 KB) |
| **Archivo UI más grande** | `EditorDialogFragment.kt` (1.610 líneas, 81.7 KB) |
| **Total líneas (src main)** | ~6.539 líneas Kotlin + ~1.000 líneas XML ≈ **7.500 líneas** |

### 2.3 Mapa de Responsabilidades (Archivo → Función)

| Archivo | Responsabilidad Principal |
|---------|--------------------------|
| `BTApplication.kt` | Inicialización de la app (tema) |
| `MainActivity.kt` | Host de navegación, ciclo de vida |
| `ConnectionHelper.kt` | Esquema BD, triggers, índices, migraciones |
| `MainRepository.kt` | CRUD completo, autenticación, seed data, PDF data |
| `SeedData.kt` | Datos de prueba (geografía, empresas, etc.) |
| `LoginFragment.kt` | UI de inicio de sesión |
| `RegisterFragment.kt` | UI de registro |
| `DashboardFragment.kt` | Panel principal con lista de tablas |
| `DashboardAdapter.kt` | Renderizado de tarjetas del dashboard |
| `TableDetailFragment.kt` | Listado/búsqueda/exportación por tabla |
| `EditorDialogFragment.kt` | Editor genérico crear/editar registros |
| `DeleteConfirmDialog.kt` | Confirmación de eliminación con dependencias |
| `TableAdapter.kt` | Renderizado de filas con display específico |
| `Constants.kt` | Metadatos: columnas, PKs, roles, permisos |
| `ValidationRules.kt` | Reglas de validación por tabla/campo |
| `InputMaskUtils.kt` | Formateo DUI, NIT, teléfono, validación email |
| `PasswordHasher.kt` | Hash PBKDF2 de contraseñas |
| `CVExportUtil.kt` | Generación de PDFs (CV + Vacante) |
| `TriggerErrorTranslator.kt` | Traducción errores SQL → español |
| `StyledToast.kt` | Toast personalizados |
| `ThemeToggleHelper.kt` | Cambio tema claro/oscuro |
| `AuthViewModel.kt` | Lógica de login/register |
| `CrudViewModel.kt` | Lógica CRUD genérica |
| `DashboardViewModel.kt` | Lógica del dashboard |
| `nav_graph.xml` | Rutas de navegación entre pantallas |
| `esquema_bolsa_trabajo.sql` | DDL de referencia de la base de datos |

## 3. Esquema de la Base de Datos

### 3.1 Vista General

La base de datos `bolsadetabajo.db` (SQLite 3) consta de **23 tablas** organizadas en 8 grupos funcionales. A continuación se presenta un diagrama general de relaciones y luego el detalle exhaustivo de cada tabla.

#### 3.1.1 Clasificación de Tablas por Tipo de Clave Primaria

| Tipo de PK | Cantidad | Tablas |
|------------|----------|--------|
| **Simple autoincremental** (INTEGER PK AUTOINCREMENT) | 8 | CATEGORIA_HABILIDAD, GENERO, TIPO_DOCUMENTO, DEPARTAMENTO, GRADO_ACADEMICO, RED_SOCIAL, TIPO_CERTIFICACION, USUARIO |
| **Simple manual** (VARCHAR PK) | 5 | INSTITUCION, EMPRESA, POSTULANTE, POSTULACION, OFERTA_ACADEMICA |
| **Compuesta 2 columnas** | 3 | MUNICIPIO, HABILIDAD, FORMACION_ACADEMICA, RED_SOCIAL_POSTULANTE |
| **Compuesta 3 columnas** | 5 | DISTRITO, OFERTA_TRABAJO, DETALLE_REQUISITO, EXPERIENCIA_LABORAL, CERTIFICACION, HABILIDAD_POSTULANTE |

**Nota:** `POSTULACION` es un caso especial: tiene PK simple manual (`ID_POSTULACION`) pero una UNIQUE compuesta de 3 columnas (`ID_POSTULANTE, NIT, ID_OFERTA`) que actúa como restricción de negocio para evitar postulaciones duplicadas a la misma oferta.

#### 3.1.2 Diagrama General de Relaciones (Padre → Hijo)

```
CATEGORIA_HABILIDAD ──→ HABILIDAD ──→ HABILIDAD_POSTULANTE
                      (1:N)           (1:N)
                       
DEPARTAMENTO ──→ MUNICIPIO ──→ DISTRITO
               (1:N)         (1:N)
               
DISTRITO ──→ EMPRESA                          ──→ OFERTA_TRABAJO ──→ DETALLE_REQUISITO
           (1:N)                              (1:N)                (1:N)
                                             
DISTRITO ──→ POSTULANTE ──→ EXPERIENCIA_LABORAL     ──→ POSTULACION
           (1:N)          (1:N con EMPRESA)         (1:N con OFERTA_TRABAJO)
                          
GENERO ──→ POSTULANTE
         (1:N)
         
TIPO_DOCUMENTO ──→ POSTULANTE
               (1:N)
               
GRADO_ACADEMICO ──→ POSTULANTE      ──→ OFERTA_TRABAJO
                 (1:N)              (1:N)
                 ──→ OFERTA_ACADEMICA
                    (1:N)

INSTITUCION ──→ CERTIFICACION      ──→ OFERTA_ACADEMICA
             (1:N)                 (1:N)
             
TIPO_CERTIFICACION ──→ CERTIFICACION
                   (1:N)
                   
POSTULANTE ──→ CERTIFICACION      ──→ FORMACION_ACADEMICA
            (1:N)                 (1:N)
            ──→ HABILIDAD_POSTULANTE   ──→ RED_SOCIAL_POSTULANTE
               (1:N)                      (1:N)
               
EMPRESA ──→ OFERTA_TRABAJO      ──→ EXPERIENCIA_LABORAL
         (1:N)                   (1:N)
         
OFERTA_ACADEMICA ──→ FORMACION_ACADEMICA
                 (1:N)
                 
OFERTA_TRABAJO ──→ DETALLE_REQUISITO      ──→ POSTULACION
               (1:N)                      (1:N)
               
RED_SOCIAL ──→ RED_SOCIAL_POSTULANTE
           (1:N)
```

#### 3.1.3 Árbol de Dependencias de Eliminación

El siguiente árbol muestra qué tablas deben eliminarse primero antes de poder eliminar un registro padre (definido en `MainRepository.getAllDependencies()`):

```
DEPARTAMENTO
  └── MUNICIPIO (via ID_DEPARTAMENTO)

MUNICIPIO
  └── DISTRITO (via ID_DEPARTAMENTO + ID_MUNICIPIO)

DISTRITO
  ├── POSTULANTE (via ID_DISTRITO_DEPTO + ID_DISTRITO_MUNICIPIO + ID_DISTRITO_ID)
  └── EMPRESA (via ID_DISTRITO_DEPTO + ID_DISTRITO_MUNICIPIO + ID_DISTRITO_ID)

GENERO
  └── POSTULANTE (via ID_GENERO)

TIPO_DOCUMENTO
  └── POSTULANTE (via ID_TIPO_DOCUMENTO)

INSTITUCION
  ├── OFERTA_ACADEMICA (via ID_INSTITUCION)
  └── CERTIFICACION (via ID_INSTITUCION)

GRADO_ACADEMICO
  ├── OFERTA_TRABAJO (via ID_GRADO_ACADEMICO)
  ├── OFERTA_ACADEMICA (via ID_GRADO_ACADEMICO)
  └── POSTULANTE (via ID_GRADO_ACADEMICO)

TIPO_CERTIFICACION
  └── CERTIFICACION (via ID_TIPO_CERTIFICACION)

CATEGORIA_HABILIDAD
  └── HABILIDAD (via ID_CATEGORIA_HABILIDAD)

HABILIDAD
  └── HABILIDAD_POSTULANTE (via ID_CATEGORIA_HABILIDAD + ID_HABILIDAD)

RED_SOCIAL
  └── RED_SOCIAL_POSTULANTE (via ID_RED_SOCIAL)

EMPRESA
  ├── OFERTA_TRABAJO (via NIT)
  └── EXPERIENCIA_LABORAL (via NIT)

POSTULANTE
  ├── POSTULACION (via ID_POSTULANTE)
  ├── EXPERIENCIA_LABORAL (via ID_POSTULANTE)
  ├── FORMACION_ACADEMICA (via ID_POSTULANTE)
  ├── CERTIFICACION (via ID_POSTULANTE)
  ├── HABILIDAD_POSTULANTE (via ID_POSTULANTE)
  └── RED_SOCIAL_POSTULANTE (via ID_POSTULANTE)

OFERTA_TRABAJO
  ├── DETALLE_REQUISITO (via NIT + ID_OFERTA)
  └── POSTULACION (via NIT + ID_OFERTA)

OFERTA_ACADEMICA
  └── FORMACION_ACADEMICA (via ID_OFERTA_ACADEMICA)
```

---

### 3.2 TABLAS CATÁLOGO (7 tablas)

Las tablas catálogo son tablas de referencia con clave primaria autoincremental simple. Proporcionan valores estándar para campos de otras tablas.

---

#### 3.2.1 CATEGORIA_HABILIDAD

**Descripción:** Catálogo de categorías para clasificar las habilidades (ej: "Desarrollo de Software y Lógica", "Bases de Datos").

**Tipo de tabla:** Catálogo

**DDL (Power Designer):**
```sql
CREATE TABLE CATEGORIA_HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    NOMBRE_CATEGORIA VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD),
    UNIQUE (NOMBRE_CATEGORIA)
);
```

**DDL (SQLite — ConnectionHelper.kt):**
```sql
CREATE TABLE CATEGORIA_HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_CATEGORIA VARCHAR(50) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_CATEGORIA_HABILIDAD` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `NOMBRE_CATEGORIA` | VARCHAR(50) | VARCHAR(50) | SÍ | Nombre único de la categoría |

**Clave primaria:** Simple, autoincremental (`ID_CATEGORIA_HABILIDAD`).

**Restricciones UNIQUE:** `NOMBRE_CATEGORIA` — no pueden existir dos categorías con el mismo nombre.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `HABILIDAD` | `ID_CATEGORIA_HABILIDAD` |

**Índices asociados:** Ninguno directo (la PK y UNIQUE tienen índices implícitos).

**Datos de semilla (SeedData):** 5 categorías: "Desarrollo de Software y Lógica", "Infraestructura y Cloud Computing", "Redes y Telecomunicaciones", "Bases de Datos", "Herramientas de Inteligencia Artificial".

**Trigger asociado:** `TR_HABILIDAD_CATEGORIA` (ver sección 4) — verifica que la categoría exista antes de insertar/actualizar una HABILIDAD.

---

#### 3.2.2 GENERO

**Descripción:** Catálogo de géneros para los postulantes (ej: "Femenino", "Masculino", "No binario").

**Tipo de tabla:** Catálogo

**DDL (Power Designer):**
```sql
CREATE TABLE GENERO (
    ID_GENERO INTEGER NOT NULL,
    NOMBRE_GENERO VARCHAR(20) NOT NULL,
    PRIMARY KEY (ID_GENERO),
    UNIQUE (NOMBRE_GENERO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE GENERO (
    ID_GENERO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_GENERO VARCHAR(20) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_GENERO` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `NOMBRE_GENERO` | VARCHAR(20) | VARCHAR(20) | SÍ | Nombre único del género |

**Clave primaria:** Simple, autoincremental (`ID_GENERO`).

**Restricciones UNIQUE:** `NOMBRE_GENERO`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `POSTULANTE` | `ID_GENERO` |

**Índices asociados:** `IDX_POSTULANTE_GENERO` ON POSTULANTE(ID_GENERO).

**Datos de semilla (SeedData):** 5 géneros: "Femenino", "Masculino", "No binario", "Prefiero no decirlo", "Otro".

**Trigger asociado:** `TR_POSTULANTE_FK` (ver sección 4) — verifica que el género exista al insertar/actualizar un POSTULANTE.

---

#### 3.2.3 TIPO_DOCUMENTO

**Descripción:** Catálogo de tipos de documento de identidad (DUI, NIT, Pasaporte).

**Tipo de tabla:** Catálogo

**DDL (Power Designer):**
```sql
CREATE TABLE TIPO_DOCUMENTO (
    ID_TIPO_DOCUMENTO INTEGER NOT NULL,
    NOMBRE_TIPO VARCHAR(25) NOT NULL,
    PRIMARY KEY (ID_TIPO_DOCUMENTO),
    UNIQUE (NOMBRE_TIPO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE TIPO_DOCUMENTO (
    ID_TIPO_DOCUMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_TIPO VARCHAR(25) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_TIPO_DOCUMENTO` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `NOMBRE_TIPO` | VARCHAR(25) | VARCHAR(25) | SÍ | Nombre único del tipo (DUI, NIT, Pasaporte) |

**Clave primaria:** Simple, autoincremental (`ID_TIPO_DOCUMENTO`).

**Restricciones UNIQUE:** `NOMBRE_TIPO`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `POSTULANTE` | `ID_TIPO_DOCUMENTO` |

**Índices asociados:** `IDX_POSTULANTE_TIPO_DOC` ON POSTULANTE(ID_TIPO_DOCUMENTO).

**Datos de semilla (SeedData):** 3 tipos: "DUI", "NIT", "Pasaporte".

**Trigger asociado:** `TR_POSTULANTE_FK` — verifica que el tipo de documento exista.

---

#### 3.2.4 DEPARTAMENTO

**Descripción:** Catálogo de departamentos de El Salvador (14 departamentos).

**Tipo de tabla:** Catálogo / Geográfica (nivel 1)

**DDL (Power Designer):**
```sql
CREATE TABLE DEPARTAMENTO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    NOMBRE_DEPARTAMENTO VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID_DEPARTAMENTO),
    UNIQUE (NOMBRE_DEPARTAMENTO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE DEPARTAMENTO (
    ID_DEPARTAMENTO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_DEPARTAMENTO VARCHAR(50) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_DEPARTAMENTO` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental (1-14) |
| `NOMBRE_DEPARTAMENTO` | VARCHAR(50) | VARCHAR(50) | SÍ | Nombre único del departamento |

**Clave primaria:** Simple, autoincremental (`ID_DEPARTAMENTO`).

**Restricciones UNIQUE:** `NOMBRE_DEPARTAMENTO`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `MUNICIPIO` | `ID_DEPARTAMENTO` |
| Es abuelo de | 1:N→1:N | `DISTRITO` | `ID_DEPARTAMENTO` (via MUNICIPIO) |

**Índices asociados:** `IDX_MUNICIPIO_DEPTO` ON MUNICIPIO(ID_DEPARTAMENTO).

**Datos de semilla (SeedData):** 14 departamentos de El Salvador (Ahuachapán, Santa Ana, Sonsonate, Chalatenango, Cuscatlán, San Salvador, La Libertad, La Paz, Cabañas, San Vicente, Usulután, San Miguel, Morazán, La Unión).

**Trigger asociado:** `TR_MUNICIPIO_DEPTO` (ver sección 4) — verifica que el departamento exista antes de insertar/actualizar un MUNICIPIO.

---

#### 3.2.5 GRADO_ACADEMICO

**Descripción:** Catálogo de grados académicos (Bachiller, Técnico Superior, Licenciatura, Ingeniería, etc.).

**Tipo de tabla:** Catálogo

**DDL (Power Designer):**
```sql
CREATE TABLE GRADO_ACADEMICO (
    ID_GRADO_ACADEMICO INTEGER NOT NULL,
    NOMBRE_GRADO VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID_GRADO_ACADEMICO),
    UNIQUE (NOMBRE_GRADO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE GRADO_ACADEMICO (
    ID_GRADO_ACADEMICO INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_GRADO VARCHAR(50) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_GRADO_ACADEMICO` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `NOMBRE_GRADO` | VARCHAR(50) | VARCHAR(50) | SÍ | Nombre único del grado académico |

**Clave primaria:** Simple, autoincremental (`ID_GRADO_ACADEMICO`).

**Restricciones UNIQUE:** `NOMBRE_GRADO`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `POSTULANTE` | `ID_GRADO_ACADEMICO` |
| Es padre de | 1:N | `OFERTA_TRABAJO` | `ID_GRADO_ACADEMICO` |
| Es padre de | 1:N | `OFERTA_ACADEMICA` | `ID_GRADO_ACADEMICO` |

**Es una de las tablas con más dependencias** (3 tablas hijas).

**Índices asociados:** `IDX_OFERTA_GRADO` ON OFERTA_TRABAJO(ID_GRADO_ACADEMICO), `IDX_OA_GRADO` ON OFERTA_ACADEMICA(ID_GRADO_ACADEMICO).

**Datos de semilla (SeedData):** 7 grados: "Bachiller", "Técnico Superior", "Profesorado", "Licenciatura", "Ingeniería", "Maestría", "Doctorado".

**Triggers asociados:**
- `TR_POSTULANTE_GRADO` — impide que un postulante tenga grado "Bachiller" (debe ser superior).
- `TR_POSTULANTE_FK` — verifica que el grado exista.

---

#### 3.2.6 RED_SOCIAL

**Descripción:** Catálogo de redes sociales (GitHub, LinkedIn, Discord, etc.).

**Tipo de tabla:** Catálogo

**DDL (Power Designer):**
```sql
CREATE TABLE RED_SOCIAL (
    ID_RED_SOCIAL INTEGER NOT NULL,
    NOMBRE_RED VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID_RED_SOCIAL),
    UNIQUE (NOMBRE_RED)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE RED_SOCIAL (
    ID_RED_SOCIAL INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_RED VARCHAR(50) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_RED_SOCIAL` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `NOMBRE_RED` | VARCHAR(50) | VARCHAR(50) | SÍ | Nombre único de la red social |

**Clave primaria:** Simple, autoincremental (`ID_RED_SOCIAL`).

**Restricciones UNIQUE:** `NOMBRE_RED`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `RED_SOCIAL_POSTULANTE` | `ID_RED_SOCIAL` |

**Datos de semilla (SeedData):** 5 redes: "GitHub", "Steam", "LinkedIn", "Discord", "X (Twitter)".

---

#### 3.2.7 TIPO_CERTIFICACION

**Descripción:** Catálogo de tipos de certificación (Certificación Profesional, Diplomado, Curso, Idioma, Seminario).

**Tipo de tabla:** Catálogo

**DDL (Power Designer):**
```sql
CREATE TABLE TIPO_CERTIFICACION (
    ID_TIPO_CERTIFICACION INTEGER NOT NULL,
    NOMBRE_TIPO VARCHAR(100) NOT NULL,
    PRIMARY KEY (ID_TIPO_CERTIFICACION),
    UNIQUE (NOMBRE_TIPO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE TIPO_CERTIFICACION (
    ID_TIPO_CERTIFICACION INTEGER PRIMARY KEY AUTOINCREMENT,
    NOMBRE_TIPO VARCHAR(100) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_TIPO_CERTIFICACION` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `NOMBRE_TIPO` | VARCHAR(100) | VARCHAR(100) | SÍ | Nombre único del tipo de certificación |

**Clave primaria:** Simple, autoincremental (`ID_TIPO_CERTIFICACION`).

**Restricciones UNIQUE:** `NOMBRE_TIPO`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `CERTIFICACION` | `ID_TIPO_CERTIFICACION` |

**Índices asociados:** Ninguno directo.

**Datos de semilla (SeedData):** 5 tipos: "Certificacion Profesional", "Diplomado", "Curso", "Idioma", "Seminario".

---

### 3.3 TABLAS GEOGRÁFICAS (2 tablas)

Tablas con claves primarias compuestas que modelan la jerarquía geográfica de El Salvador: Departamento → Municipio → Distrito.

---

#### 3.3.1 MUNICIPIO

**Descripción:** Municipios agrupados por departamento. Cada municipio pertenece a un único departamento.

**Tipo de tabla:** Geográfica (nivel 2)

**Tipo de PK:** Compuesta (2 columnas: `ID_DEPARTAMENTO + ID_MUNICIPIO`)

**DDL (Power Designer):**
```sql
CREATE TABLE MUNICIPIO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    NOMBRE_MUNICIPIO VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO),
    UNIQUE (ID_DEPARTAMENTO, NOMBRE_MUNICIPIO),
    FOREIGN KEY (ID_DEPARTAMENTO) REFERENCES DEPARTAMENTO (ID_DEPARTAMENTO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE MUNICIPIO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    NOMBRE_MUNICIPIO VARCHAR(50),
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO),
    FOREIGN KEY (ID_DEPARTAMENTO) REFERENCES DEPARTAMENTO (ID_DEPARTAMENTO),
    UNIQUE (ID_DEPARTAMENTO, NOMBRE_MUNICIPIO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_DEPARTAMENTO` | INTEGER | INTEGER | SÍ | FK → DEPARTAMENTO. Primera parte de la PK compuesta |
| `ID_MUNICIPIO` | INTEGER | INTEGER | SÍ | Identificador del municipio dentro del departamento. Segunda parte de la PK compuesta |
| `NOMBRE_MUNICIPIO` | VARCHAR(50) | VARCHAR(50) | SÍ | Nombre del municipio (único dentro del mismo departamento) |

**Clave primaria:** Compuesta: `(ID_DEPARTAMENTO, ID_MUNICIPIO)`. Esto significa que el mismo `ID_MUNICIPIO` puede existir en diferentes departamentos.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `ID_DEPARTAMENTO` | DEPARTAMENTO | `ID_DEPARTAMENTO` | Restrictiva |

**Restricciones UNIQUE:** `(ID_DEPARTAMENTO, NOMBRE_MUNICIPIO)` — no pueden existir dos municipios con el mismo nombre dentro del mismo departamento.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `DEPARTAMENTO` | `ID_DEPARTAMENTO` |
| Es padre de | 1:N | `DISTRITO` | `(ID_DEPARTAMENTO, ID_MUNICIPIO)` |

**Índices asociados:** `IDX_MUNICIPIO_DEPTO` ON MUNICIPIO(ID_DEPARTAMENTO) — optimiza búsquedas por departamento.

**Datos de semilla (SeedData):** 44 municipios (distribuidos en los 14 departamentos, ej: "Ahuachapán Norte", "Santa Ana Centro", "San Salvador Norte", etc.).

**Triggers asociados:**
- `TR_MUNICIPIO_DEPTO` (INSERT/UPDATE) — verifica que `ID_DEPARTAMENTO` exista en DEPARTAMENTO.
- `TR_DISTRITO_MUNICIPIO` (INSERT/UPDATE en DISTRITO) — verifica que el par `(ID_DEPARTAMENTO, ID_MUNICIPIO)` exista en MUNICIPIO cuando se inserta un DISTRITO.

**Nota importante:** Esta tabla **no tiene** clave autoincremental. Los valores de `ID_MUNICIPIO` se asignan manualmente (1, 2, 3... dentro de cada departamento).

---

#### 3.3.2 DISTRITO

**Descripción:** Distritos agrupados por municipio y departamento. Es la unidad geográfica más específica.

**Tipo de tabla:** Geográfica (nivel 3)

**Tipo de PK:** Compuesta (3 columnas: `ID_DEPARTAMENTO + ID_MUNICIPIO + ID_DISTRITO`)

**DDL (Power Designer):**
```sql
CREATE TABLE DISTRITO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO INTEGER NOT NULL,
    NOMBRE_DISTRITO VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO),
    UNIQUE (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_DISTRITO),
    FOREIGN KEY (ID_DEPARTAMENTO, ID_MUNICIPIO) REFERENCES MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE DISTRITO (
    ID_DEPARTAMENTO INTEGER NOT NULL,
    ID_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO INTEGER NOT NULL,
    NOMBRE_DISTRITO VARCHAR(50),
    PRIMARY KEY (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO),
    FOREIGN KEY (ID_DEPARTAMENTO, ID_MUNICIPIO) REFERENCES MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO),
    UNIQUE (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_DISTRITO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_DEPARTAMENTO` | INTEGER | INTEGER | SÍ | 1ra parte de PK compuesta y FK → MUNICIPIO |
| `ID_MUNICIPIO` | INTEGER | INTEGER | SÍ | 2da parte de PK compuesta y FK → MUNICIPIO |
| `ID_DISTRITO` | INTEGER | INTEGER | SÍ | 3ra parte de PK compuesta. Identificador único dentro del municipio |
| `NOMBRE_DISTRITO` | VARCHAR(50) | VARCHAR(50) | SÍ | Nombre del distrito (único dentro del mismo municipio) |

**Clave primaria:** Compuesta: `(ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)`. Esta es la única tabla con una PK de 3 columnas que es completamente manual (no autoincremental).

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `(ID_DEPARTAMENTO, ID_MUNICIPIO)` | MUNICIPIO | `(ID_DEPARTAMENTO, ID_MUNICIPIO)` | **FK compuesta** — referencia la PK compuesta de MUNICIPIO |

**Restricciones UNIQUE:** `(ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_DISTRITO)` — no pueden existir dos distritos con el mismo nombre dentro del mismo municipio.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `MUNICIPIO` | `(ID_DEPARTAMENTO, ID_MUNICIPIO)` |
| Es padre de | 1:N | `EMPRESA` | `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` |
| Es padre de | 1:N | `POSTULANTE` | `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` |

**DISTRITO es referenciado por 2 tablas principales** (EMPRESA y POSTULANTE) a través de una FK compuesta de 3 columnas.

**Índices asociados:** `IDX_DISTRITO_MUNICIPIO` ON DISTRITO(ID_DEPARTAMENTO, ID_MUNICIPIO).

**Datos de semilla (SeedData):** 262 distritos (la tabla más grande en cantidad de datos).

**Triggers asociados:**
- `TR_DISTRITO_MUNICIPIO` (INSERT/UPDATE) — verifica que el par `(ID_DEPARTAMENTO, ID_MUNICIPIO)` exista en MUNICIPIO.
- `TR_EMPRESA_DISTRITO` (INSERT/UPDATE en EMPRESA) — verifica que el distrito exista.
- `TR_POSTULANTE_FK` (INSERT/UPDATE en POSTULANTE) — solo verifica género, tipo_documento y grado (el distrito se verifica mediante NOT NULL o trigger adicional).

**Nota sobre complejidad:** Esta es la tabla con la clave primaria compuesta más larga del sistema (3 columnas). Las tablas EMPRESA y POSTULANTE deben replicar estas 3 columnas para referenciar un distrito, lo que hace que la relación sea conceptualmente compleja.

---

### 3.4 TABLAS DE HABILIDADES E INSTITUCIONES (2 tablas)

---

#### 3.4.1 INSTITUCION

**Descripción:** Catálogo de instituciones educativas que pueden emitir certificaciones y ofrecer carreras.

**Tipo de tabla:** Catálogo (clave manual VARCHAR)

**Tipo de PK:** Simple, manual (VARCHAR(20))

**DDL (Power Designer):**
```sql
CREATE TABLE INSTITUCION (
    ID_INSTITUCION VARCHAR(20) NOT NULL,
    NOMBRE_INSTITUCION VARCHAR(150) NOT NULL,
    PRIMARY KEY (ID_INSTITUCION),
    UNIQUE (NOMBRE_INSTITUCION)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE INSTITUCION (
    ID_INSTITUCION VARCHAR(20) PRIMARY KEY,
    NOMBRE_INSTITUCION VARCHAR(150) UNIQUE
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_INSTITUCION` | VARCHAR(20) | VARCHAR(20) | SÍ | Clave primaria manual. Formato: `INS` + 3 dígitos (ej: INS001) |
| `NOMBRE_INSTITUCION` | VARCHAR(150) | VARCHAR(150) | SÍ | Nombre único de la institución |

**Clave primaria:** Simple, manual (`ID_INSTITUCION` VARCHAR(20)). El usuario debe ingresar el código siguiendo el patrón `^INS\d{3,}$`.

**Restricciones UNIQUE:** `NOMBRE_INSTITUCION`.

**Relaciones:**

| Relación | Tipo | Tabla hija | Columna FK |
|----------|------|------------|------------|
| Es padre de | 1:N | `OFERTA_ACADEMICA` | `ID_INSTITUCION` |
| Es padre de | 1:N | `CERTIFICACION` | `ID_INSTITUCION` |

**Índices asociados:** `IDX_CERT_INSTITUCION` ON CERTIFICACION(ID_INSTITUCION), `IDX_OA_INSTITUCION` ON OFERTA_ACADEMICA(ID_INSTITUCION).

**Datos de semilla (SeedData):** 6 instituciones: "Universidad de El Salvador (UES)", "Escuela Nacional de Agricultura(ENA)", "Fundación Gloria de Kriete", "Universidad Don Bosco", "Universidad José Matías Delgado", "Ministerio de Educación, Ciencia y Tecnología (MINED)".

---

#### 3.4.2 HABILIDAD

**Descripción:** Habilidades agrupadas por categoría. Cada habilidad pertenece a una categoría.

**Tipo de tabla:** Catálogo (PK compuesta)

**Tipo de PK:** Compuesta (2 columnas: `ID_CATEGORIA_HABILIDAD + ID_HABILIDAD`)

**DDL (Power Designer):**
```sql
CREATE TABLE HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    NOMBRE_HABILIDAD VARCHAR(100) NOT NULL,
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    UNIQUE (NOMBRE_HABILIDAD),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD) REFERENCES CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE HABILIDAD (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    NOMBRE_HABILIDAD VARCHAR(100) UNIQUE,
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD) REFERENCES CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_CATEGORIA_HABILIDAD` | INTEGER | INTEGER | SÍ | FK → CATEGORIA_HABILIDAD. 1ra parte de la PK compuesta |
| `ID_HABILIDAD` | VARCHAR(10) | VARCHAR(10) | SÍ | Código de habilidad dentro de la categoría. 2da parte de PK. Formato: `^H\d{2,}$` (ej: H01) |
| `NOMBRE_HABILIDAD` | VARCHAR(100) | VARCHAR(100) | SÍ | Nombre único de la habilidad (a nivel global, no solo por categoría) |

**Clave primaria:** Compuesta: `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)`. El código de habilidad (`ID_HABILIDAD`) se reinicia por categoría (ej: H01 en categoría 1 es "Programación en Python", H01 en categoría 2 es "Despliegue en Google Cloud").

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `ID_CATEGORIA_HABILIDAD` | CATEGORIA_HABILIDAD | `ID_CATEGORIA_HABILIDAD` | Simple |

**Restricciones UNIQUE:** `NOMBRE_HABILIDAD` — a diferencia del ID, el nombre es único globalmente.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `CATEGORIA_HABILIDAD` | `ID_CATEGORIA_HABILIDAD` |
| Es padre de | 1:N | `HABILIDAD_POSTULANTE` | `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)` — **FK compuesta** |

**Índices asociados:** `IDX_HABILIDAD_CATEGORIA` ON HABILIDAD(ID_CATEGORIA_HABILIDAD).

**Datos de semilla (SeedData):** 15 habilidades (3 por cada una de las 5 categorías).

**Triggers asociados:**
- `TR_HABILIDAD_CATEGORIA` (INSERT/UPDATE) — verifica que `ID_CATEGORIA_HABILIDAD` exista en CATEGORIA_HABILIDAD.

---

### 3.5 TABLAS PRINCIPALES (2 tablas)

---

#### 3.5.1 EMPRESA

**Descripción:** Empresas registradas en el sistema. Cada empresa tiene una ubicación geográfica (distrito) y datos de contacto.

**Tipo de tabla:** Principal

**Tipo de PK:** Simple, manual (VARCHAR(20) — NIT)

**DDL (Power Designer):**
```sql
CREATE TABLE EMPRESA (
    NIT VARCHAR(20) NOT NULL,
    ID_DISTRITO_DEPTO INTEGER NOT NULL,
    ID_DISTRITO_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO_ID INTEGER NOT NULL,
    NOMBRE_EMPRESA VARCHAR(150) NOT NULL,
    CONTACTO_DIRECTO VARCHAR(100),
    PRIMARY KEY (NIT),
    UNIQUE (NOMBRE_EMPRESA),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)
        REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE EMPRESA (
    NIT VARCHAR(20) PRIMARY KEY,
    ID_DISTRITO_DEPTO INTEGER NOT NULL,
    ID_DISTRITO_MUNICIPIO INTEGER NOT NULL,
    ID_DISTRITO_ID INTEGER NOT NULL,
    NOMBRE_EMPRESA VARCHAR(150) UNIQUE,
    CONTACTO_DIRECTO VARCHAR(100),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)
        REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `NIT` | VARCHAR(20) | VARCHAR(20) | SÍ | **Clave primaria.** NIT de la empresa (14 dígitos, formato 0000-000000-000-0) |
| `ID_DISTRITO_DEPTO` | INTEGER | INTEGER | SÍ | FK → DISTRITO.ID_DEPARTAMENTO. 1ra parte de FK compuesta |
| `ID_DISTRITO_MUNICIPIO` | INTEGER | INTEGER | SÍ | FK → DISTRITO.ID_MUNICIPIO. 2da parte de FK compuesta |
| `ID_DISTRITO_ID` | INTEGER | INTEGER | SÍ | FK → DISTRITO.ID_DISTRITO. 3ra parte de FK compuesta |
| `NOMBRE_EMPRESA` | VARCHAR(150) | VARCHAR(150) | SÍ | Nombre único de la empresa |
| `CONTACTO_DIRECTO` | VARCHAR(100) | VARCHAR(100) | NO | Teléfono de contacto (formato 0000-0000) |

**Clave primaria:** Simple, manual (`NIT` VARCHAR(20)). El NIT debe tener exactamente 14 dígitos (formato validado en el código).

**Claves foráneas:**

| Columna(s) FK | Tabla padre | Columna(s) padre | Tipo |
|---------------|-------------|------------------|------|
| `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` | DISTRITO | `(ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)` | **FK compuesta de 3 columnas** |

**Restricciones UNIQUE:** `NOMBRE_EMPRESA`.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `DISTRITO` | `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` — FK compuesta 3 cols |
| Es padre de | 1:N | `OFERTA_TRABAJO` | `NIT` |
| Es padre de | 1:N | `EXPERIENCIA_LABORAL` | `NIT` |

**Índices asociados:** `IDX_EMPRESA_DISTRITO` ON EMPRESA(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID).

**Datos de semilla (SeedData):** 10 empresas (Banco Agrícola, Nequi, Súper Selectos, Holcim, AES CLESA, Grupo Campestre, CASSA, La Geo, Cooperativa Los Ausoles, Embutidos La Única).

**Triggers asociados:** `TR_EMPRESA_DISTRITO` (INSERT/UPDATE) — verifica que el distrito exista.

---

#### 3.5.2 POSTULANTE

**Descripción:** Personas que buscan empleo. Es la tabla central del sistema con más relaciones (6 tablas hijas y 5 FK padres).

**Tipo de tabla:** Principal (central)

**Tipo de PK:** Simple, manual (VARCHAR(20) — código de postulante)

**DDL (Power Designer):**
```sql
CREATE TABLE POSTULANTE (
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_GENERO INTEGER NOT NULL,
    ID_TIPO_DOCUMENTO INTEGER NOT NULL,
    NUM_DOCUMENTO VARCHAR(20) NOT NULL,
    ID_GRADO_ACADEMICO INTEGER NOT NULL,
    ID_DISTRITO_DEPTO INTEGER,
    ID_DISTRITO_MUNICIPIO INTEGER,
    ID_DISTRITO_ID INTEGER,
    NOMBRE VARCHAR(100),
    APELLIDO VARCHAR(100),
    FECHA_NACIMIENTO DATE,
    NUP VARCHAR(20) NOT NULL,
    DIRECCION_DETALLE VARCHAR(250),
    TELEFONO_CASA VARCHAR(15),
    TELEFONO_CELULAR VARCHAR(15),
    EMAIL VARCHAR(100) NOT NULL,
    PRIMARY KEY (ID_POSTULANTE),
    UNIQUE (NUM_DOCUMENTO),
    UNIQUE (NUP),
    UNIQUE (EMAIL),
    FOREIGN KEY (ID_GENERO) REFERENCES GENERO (ID_GENERO),
    FOREIGN KEY (ID_TIPO_DOCUMENTO) REFERENCES TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)
        REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE POSTULANTE (
    ID_POSTULANTE VARCHAR(20) PRIMARY KEY,
    ID_GENERO INTEGER NOT NULL,
    ID_TIPO_DOCUMENTO INTEGER NOT NULL,
    NUM_DOCUMENTO VARCHAR(20) UNIQUE,
    ID_GRADO_ACADEMICO INTEGER NOT NULL,
    ID_DISTRITO_DEPTO INTEGER,
    ID_DISTRITO_MUNICIPIO INTEGER,
    ID_DISTRITO_ID INTEGER,
    NOMBRE VARCHAR(100),
    APELLIDO VARCHAR(100),
    FECHA_NACIMIENTO DATE,
    NUP VARCHAR(20) UNIQUE,
    DIRECCION_DETALLE VARCHAR(250),
    TELEFONO_CASA VARCHAR(15),
    TELEFONO_CELULAR VARCHAR(15),
    EMAIL VARCHAR(100) UNIQUE COLLATE NOCASE,
    FOREIGN KEY (ID_GENERO) REFERENCES GENERO (ID_GENERO),
    FOREIGN KEY (ID_TIPO_DOCUMENTO) REFERENCES TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO),
    FOREIGN KEY (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)
        REFERENCES DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | **Clave primaria manual.** Formato: 2 letras + 5 dígitos (ej: AB12345), patrón `^[A-Z]{2}\d{5}$` |
| `ID_GENERO` | INTEGER | INTEGER | SÍ | FK → GENERO |
| `ID_TIPO_DOCUMENTO` | INTEGER | INTEGER | SÍ | FK → TIPO_DOCUMENTO |
| `NUM_DOCUMENTO` | VARCHAR(20) | VARCHAR(20) | SÍ | Número de documento (DUI: 00000000-0, NIT: 0000-000000-000-0, Pasaporte: texto). UNIQUE |
| `ID_GRADO_ACADEMICO` | INTEGER | INTEGER | SÍ | FK → GRADO_ACADEMICO |
| `ID_DISTRITO_DEPTO` | INTEGER | INTEGER | NO | FK → DISTRITO.ID_DEPARTAMENTO (1ra parte FK compuesta) |
| `ID_DISTRITO_MUNICIPIO` | INTEGER | INTEGER | NO | FK → DISTRITO.ID_MUNICIPIO (2da parte FK compuesta) |
| `ID_DISTRITO_ID` | INTEGER | INTEGER | NO | FK → DISTRITO.ID_DISTRITO (3ra parte FK compuesta) |
| `NOMBRE` | VARCHAR(100) | VARCHAR(100) | NO | Nombre(s) del postulante |
| `APELLIDO` | VARCHAR(100) | VARCHAR(100) | NO | Apellido(s) del postulante |
| `FECHA_NACIMIENTO` | DATE | DATE | NO | Fecha de nacimiento (formato YYYY-MM-DD) |
| `NUP` | VARCHAR(20) | VARCHAR(20) | SÍ | Número Único de Persona (12 dígitos). UNIQUE |
| `DIRECCION_DETALLE` | VARCHAR(250) | VARCHAR(250) | NO | Dirección detallada (calle, casa, etc.) |
| `TELEFONO_CASA` | VARCHAR(15) | VARCHAR(15) | NO | Teléfono de casa |
| `TELEFONO_CELULAR` | VARCHAR(15) | VARCHAR(15) | NO | Teléfono celular |
| `EMAIL` | VARCHAR(100) | VARCHAR(100) | SÍ | Correo electrónico. UNIQUE (case-insensitive: COLLATE NOCASE) |

**Clave primaria:** Simple, manual (`ID_POSTULANTE` VARCHAR(20)). Sigue el patrón `^[A-Z]{2}\d{5}$` (2 letras + 5 dígitos).

**Claves foráneas (4 FKs — es la tabla con más FKs padres):**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `ID_GENERO` | GENERO | `ID_GENERO` | Simple |
| `ID_TIPO_DOCUMENTO` | TIPO_DOCUMENTO | `ID_TIPO_DOCUMENTO` | Simple |
| `ID_GRADO_ACADEMICO` | GRADO_ACADEMICO | `ID_GRADO_ACADEMICO` | Simple |
| `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` | DISTRITO | `(ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO)` | **FK compuesta de 3 columnas** |

**Restricciones UNIQUE (3 — la mayor cantidad en una tabla):**
- `NUM_DOCUMENTO` — NIT/DUI/Pasaporte único
- `NUP` — Número Único de Persona único
- `EMAIL` — Correo electrónico único (con COLLATE NOCASE para comparación case-insensitive)

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `GENERO` | `ID_GENERO` |
| Es hija de | N:1 | `TIPO_DOCUMENTO` | `ID_TIPO_DOCUMENTO` |
| Es hija de | N:1 | `GRADO_ACADEMICO` | `ID_GRADO_ACADEMICO` |
| Es hija de | N:1 | `DISTRITO` | `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` |
| **Es padre de** | **1:N** | **`POSTULACION`** | **`ID_POSTULANTE`** |
| **Es padre de** | **1:N** | **`EXPERIENCIA_LABORAL`** | **`ID_POSTULANTE`** |
| **Es padre de** | **1:N** | **`FORMACION_ACADEMICA`** | **`ID_POSTULANTE`** |
| **Es padre de** | **1:N** | **`CERTIFICACION`** | **`ID_POSTULANTE`** |
| **Es padre de** | **1:N** | **`HABILIDAD_POSTULANTE`** | **`ID_POSTULANTE`** |
| **Es padre de** | **1:N** | **`RED_SOCIAL_POSTULANTE`** | **`ID_POSTULANTE`** |

**POSTULANTE es la tabla con MÁS relaciones del sistema**: 4 FK padres y **6 tablas hijas**.

**Índices asociados (3):**
- `IDX_POSTULANTE_GENERO` ON POSTULANTE(ID_GENERO)
- `IDX_POSTULANTE_TIPO_DOC` ON POSTULANTE(ID_TIPO_DOCUMENTO)
- `IDX_POSTULANTE_DISTRITO` ON POSTULANTE(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)

**Triggers asociados (3 pares = 6 triggers):**
- `TR_POSTULANTE_EDAD` (INSERT/UPDATE) — valida mayoría de edad (18+) y fecha no futura.
- `TR_POSTULANTE_GRADO` (INSERT/UPDATE) — impide grado "Bachiller".
- `TR_POSTULANTE_FK` (INSERT/UPDATE) — verifica existencia de género, tipo_documento y grado_academico.

---

### 3.6 TABLA DE SISTEMA (1 tabla)

---

#### 3.6.1 USUARIO

**Descripción:** Cuentas de usuario para acceder al sistema. Almacena credenciales de autenticación con contraseñas hasheadas (PBKDF2) y roles.

**Tipo de tabla:** Sistema / Autenticación

**Tipo de PK:** Simple, autoincremental (INTEGER)

**DDL (Power Designer):**
```sql
CREATE TABLE USUARIO (
    ID_USUARIO INTEGER NOT NULL,
    USERNAME VARCHAR(30) NOT NULL,
    PASSWORD VARCHAR(128),
    ROL VARCHAR(20),
    PRIMARY KEY (ID_USUARIO),
    UNIQUE (USERNAME)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE USUARIO (
    ID_USUARIO INTEGER PRIMARY KEY AUTOINCREMENT,
    USERNAME VARCHAR(30) UNIQUE COLLATE NOCASE,
    PASSWORD VARCHAR(128),
    ROL VARCHAR(20)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_USUARIO` | INTEGER | INTEGER | SÍ | Clave primaria autoincremental |
| `USERNAME` | VARCHAR(30) | VARCHAR(30) | SÍ | Nombre de usuario. UNIQUE case-insensitive (COLLATE NOCASE). Mínimo 3 caracteres |
| `PASSWORD` | VARCHAR(128) | VARCHAR(128) | SÍ | Hash de contraseña. Formato: `salt_hex:hash_hex` (PBKDF2, 64+64=128 chars) |
| `ROL` | VARCHAR(20) | VARCHAR(20) | SÍ | Rol del usuario: "administrador", "postulante", "gerente de empresa" |

**Clave primaria:** Simple, autoincremental (`ID_USUARIO`).

**Restricciones UNIQUE:** `USERNAME` con COLLATE NOCASE — los nombres de usuario no distinguen mayúsculas/minúsculas.

**Relaciones:** Ninguna (USUARIO es independiente, no tiene FK padres ni hijas).

**Nota importante sobre PASSWORD:** La columna almacena el hash en formato `salt:hash` donde:
- Salt: 16 bytes aleatorios → 32 caracteres hex
- Hash: 256 bits (32 bytes) → 64 caracteres hex
- Total: 32 + 1 + 64 = 97 caracteres (cabe en VARCHAR(128))

**Mecanismo de hash (PasswordHasher.kt):**
- Algoritmo: `PBKDF2WithHmacSHA256`
- Iteraciones: 65.536
- Longitud de clave: 256 bits
- Salt: 16 bytes generados con `SecureRandom`
- La verificación usa `MessageDigest.isEqual()` para prevenir timing attacks

**Datos de semilla (SeedData):** 2 usuarios:
- `postulante` / `12345678` / rol: `postulante`
- `empresa` / `12345678` / rol: `gerente de empresa`

---

### 3.7 TABLAS DE OFERTAS (3 tablas)

---

#### 3.7.1 OFERTA_ACADEMICA

**Descripción:** Ofertas académicas disponibles (carreras, programas) ofrecidas por instituciones educativas para un grado específico.

**Tipo de tabla:** Oferta

**Tipo de PK:** Simple, manual (VARCHAR(10))

**DDL (Power Designer):**
```sql
CREATE TABLE OFERTA_ACADEMICA (
    ID_OFERTA_ACADEMICA VARCHAR(10) NOT NULL,
    ID_GRADO_ACADEMICO INTEGER,
    ID_INSTITUCION VARCHAR(20),
    PRIMARY KEY (ID_OFERTA_ACADEMICA),
    UNIQUE (ID_INSTITUCION, ID_GRADO_ACADEMICO),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE OFERTA_ACADEMICA (
    ID_OFERTA_ACADEMICA VARCHAR(10) PRIMARY KEY,
    ID_GRADO_ACADEMICO INTEGER,
    ID_INSTITUCION VARCHAR(20),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO),
    UNIQUE (ID_INSTITUCION, ID_GRADO_ACADEMICO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_OFERTA_ACADEMICA` | VARCHAR(10) | VARCHAR(10) | SÍ | Clave primaria manual. Formato: `^OFA\d{2,}$` (ej: OFA01) |
| `ID_GRADO_ACADEMICO` | INTEGER | INTEGER | NO | FK → GRADO_ACADEMICO (puede ser NULL) |
| `ID_INSTITUCION` | VARCHAR(20) | VARCHAR(20) | NO | FK → INSTITUCION (puede ser NULL) |

**Clave primaria:** Simple, manual (`ID_OFERTA_ACADEMICA` VARCHAR(10)). Patrón: `^OFA\d{2,}$`.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre |
|------------|-------------|---------------|
| `ID_INSTITUCION` | INSTITUCION | `ID_INSTITUCION` |
| `ID_GRADO_ACADEMICO` | GRADO_ACADEMICO | `ID_GRADO_ACADEMICO` |

**Restricciones UNIQUE:** `(ID_INSTITUCION, ID_GRADO_ACADEMICO)` — no pueden existir dos ofertas académicas con la misma combinación institución+grado.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `INSTITUCION` | `ID_INSTITUCION` |
| Es hija de | N:1 | `GRADO_ACADEMICO` | `ID_GRADO_ACADEMICO` |
| Es padre de | 1:N | `FORMACION_ACADEMICA` | `ID_OFERTA_ACADEMICA` |

**Índices asociados:**
- `IDX_OA_INSTITUCION` ON OFERTA_ACADEMICA(ID_INSTITUCION)
- `IDX_OA_GRADO` ON OFERTA_ACADEMICA(ID_GRADO_ACADEMICO)

**Datos de semilla (SeedData):** 5 ofertas académicas (ej: OFA01 → UES + Licenciatura, OFA02 → ENA + Ingeniería).

---

#### 3.7.2 OFERTA_TRABAJO

**Descripción:** Ofertas de trabajo publicadas por empresas. Contiene los detalles del puesto, fechas de vigencia, requisitos de edad y experiencia.

**Tipo de tabla:** Oferta (principal)

**Tipo de PK:** Compuesta (2 columnas: `NIT + ID_OFERTA`)

**DDL (Power Designer):**
```sql
CREATE TABLE OFERTA_TRABAJO (
    NIT VARCHAR(20) NOT NULL,
    ID_OFERTA VARCHAR(10) NOT NULL,
    ID_GRADO_ACADEMICO INTEGER,
    TITULO_PUESTO VARCHAR(150) NOT NULL,
    FECHA_PUBLICACION DATE,
    FECHA_CADUCIDAD DATE,
    EXPERIENCIA_ANIOS INTEGER,
    EDAD_MINIMA INTEGER,
    EDAD_MAXIMA INTEGER,
    DESCRIPCION_OFERTA_TRABAJO VARCHAR(5000),
    PRIMARY KEY (NIT, ID_OFERTA),
    UNIQUE (NIT, TITULO_PUESTO),
    FOREIGN KEY (NIT) REFERENCES EMPRESA (NIT),
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO)
);
```

**DDL (SQLite):**
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
    FOREIGN KEY (ID_GRADO_ACADEMICO) REFERENCES GRADO_ACADEMICO (ID_GRADO_ACADEMICO),
    UNIQUE (NIT, TITULO_PUESTO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `NIT` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → EMPRESA. 1ra parte de la PK compuesta |
| `ID_OFERTA` | VARCHAR(10) | VARCHAR(10) | SÍ | Código de oferta dentro de la empresa. 2da parte de PK. Formato: `^OF\d{2,}$` (ej: OF01) |
| `ID_GRADO_ACADEMICO` | INTEGER | INTEGER | NO | FK → GRADO_ACADEMICO (grado requerido, opcional) |
| `TITULO_PUESTO` | VARCHAR(150) | VARCHAR(150) | SÍ | Título del puesto (único dentro de la misma empresa) |
| `FECHA_PUBLICACION` | DATE | DATE | NO | Fecha de publicación de la oferta |
| `FECHA_CADUCIDAD` | DATE | DATE | NO | Fecha de caducidad (debe ser posterior a publicación) |
| `EXPERIENCIA_ANIOS` | INTEGER | INTEGER | NO | Años de experiencia requeridos |
| `EDAD_MINIMA` | INTEGER | INTEGER | NO | Edad mínima requerida (>= 18, validado por trigger) |
| `EDAD_MAXIMA` | INTEGER | INTEGER | NO | Edad máxima requerida (debe ser >= edad_minima) |
| `DESCRIPCION_OFERTA_TRABAJO` | VARCHAR(5000) | VARCHAR(5000) | NO | Descripción detallada de la oferta (hasta 5000 caracteres) |

**Clave primaria:** Compuesta: `(NIT, ID_OFERTA)`. El mismo `ID_OFERTA` puede existir para diferentes empresas (diferentes NIT).

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `NIT` | EMPRESA | `NIT` | Simple |
| `ID_GRADO_ACADEMICO` | GRADO_ACADEMICO | `ID_GRADO_ACADEMICO` | Simple |

**Restricciones UNIQUE:** `(NIT, TITULO_PUESTO)` — no pueden existir dos ofertas con el mismo título dentro de la misma empresa.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `EMPRESA` | `NIT` |
| Es hija de | N:1 | `GRADO_ACADEMICO` | `ID_GRADO_ACADEMICO` |
| Es padre de | 1:N | `DETALLE_REQUISITO` | `(NIT, ID_OFERTA)` — FK compuesta |
| Es padre de | 1:N | `POSTULACION` | `(NIT, ID_OFERTA)` — FK compuesta |

**Índices asociados:** `IDX_OFERTA_GRADO` ON OFERTA_TRABAJO(ID_GRADO_ACADEMICO).

**Triggers asociados (2 pares = 4 triggers):**
- `TR_OFERTA_RANGO_EDAD` (INSERT/UPDATE) — valida EDAD_MINIMA >= 18 y EDAD_MINIMA <= EDAD_MAXIMA.
- `TR_OFERTA_VIGENCIA` (INSERT/UPDATE) — valida FECHA_CADUCIDAD > FECHA_PUBLICACION.

**Nota:** Tiene la columna más grande del esquema: `DESCRIPCION_OFERTA_TRABAJO VARCHAR(5000)`.

---

#### 3.7.3 DETALLE_REQUISITO

**Descripción:** Requisitos específicos asociados a una oferta de trabajo. Cada oferta puede tener múltiples requisitos.

**Tipo de tabla:** Detalle / Child de OFERTA_TRABAJO

**Tipo de PK:** Compuesta (3 columnas: `NIT + ID_OFERTA + ID_DETALLE`)

**DDL (Power Designer):**
```sql
CREATE TABLE DETALLE_REQUISITO (
    NIT VARCHAR(20) NOT NULL,
    ID_OFERTA VARCHAR(10) NOT NULL,
    ID_DETALLE VARCHAR(10) NOT NULL,
    DESCRIPCION_REQUISITO VARCHAR(100) NOT NULL,
    PRIMARY KEY (NIT, ID_OFERTA, ID_DETALLE),
    UNIQUE (NIT, ID_OFERTA, DESCRIPCION_REQUISITO),
    FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE DETALLE_REQUISITO (
    NIT VARCHAR(20) NOT NULL,
    ID_OFERTA VARCHAR(10) NOT NULL,
    ID_DETALLE VARCHAR(10) NOT NULL,
    DESCRIPCION_REQUISITO VARCHAR(100),
    PRIMARY KEY (NIT, ID_OFERTA, ID_DETALLE),
    FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA),
    UNIQUE (NIT, ID_OFERTA, DESCRIPCION_REQUISITO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `NIT` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → OFERTA_TRABAJO. 1ra parte de PK compuesta |
| `ID_OFERTA` | VARCHAR(10) | VARCHAR(10) | SÍ | FK → OFERTA_TRABAJO. 2da parte de PK compuesta |
| `ID_DETALLE` | VARCHAR(10) | VARCHAR(10) | SÍ | Código del requisito dentro de la oferta. 3ra parte de PK. Formato: `^D\d{1,}$` (ej: D1) |
| `DESCRIPCION_REQUISITO` | VARCHAR(100) | VARCHAR(100) | SÍ | Descripción del requisito (único dentro de la misma oferta) |

**Clave primaria:** Compuesta: `(NIT, ID_OFERTA, ID_DETALLE)`. Al heredar NIT+ID_OFERTA de OFERTA_TRABAJO, y agregar ID_DETALLE.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `(NIT, ID_OFERTA)` | OFERTA_TRABAJO | `(NIT, ID_OFERTA)` | **FK compuesta** |

**Restricciones UNIQUE:** `(NIT, ID_OFERTA, DESCRIPCION_REQUISITO)`.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `OFERTA_TRABAJO` | `(NIT, ID_OFERTA)` — FK compuesta |

**Índices asociados:** `IDX_DETALLE_OFERTA` ON DETALLE_REQUISITO(NIT, ID_OFERTA).

---

### 3.8 TABLAS DE POSTULANTE (5 tablas)

Estas 5 tablas almacenan información detallada del perfil de cada postulante.

---

#### 3.8.1 EXPERIENCIA_LABORAL

**Descripción:** Experiencias laborales previas de un postulante en una empresa específica.

**Tipo de tabla:** Child de POSTULANTE + EMPRESA

**Tipo de PK:** Compuesta (3 columnas: `ID_POSTULANTE + NIT + ID_EXPERIENCIA`)

**DDL (Power Designer):**
```sql
CREATE TABLE EXPERIENCIA_LABORAL (
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    NIT VARCHAR(20) NOT NULL,
    ID_EXPERIENCIA VARCHAR(10) NOT NULL,
    PUESTO_TRABAJO VARCHAR(100) NOT NULL,
    FECHA_INICIO DATE,
    FECHA_FIN DATE,
    DESCP_EXPERIENCIA_LABORAL VARCHAR(500),
    CONTACTO_REFERENCIA VARCHAR(100),
    PRIMARY KEY (ID_POSTULANTE, NIT, ID_EXPERIENCIA),
    UNIQUE (ID_POSTULANTE, NIT, PUESTO_TRABAJO),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (NIT) REFERENCES EMPRESA (NIT)
);
```

**DDL (SQLite):**
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
    FOREIGN KEY (NIT) REFERENCES EMPRESA (NIT),
    UNIQUE (ID_POSTULANTE, NIT, PUESTO_TRABAJO)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → POSTULANTE. 1ra parte de PK |
| `NIT` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → EMPRESA. 2da parte de PK |
| `ID_EXPERIENCIA` | VARCHAR(10) | VARCHAR(10) | SÍ | Código de experiencia. 3ra parte de PK. Formato: `^EL\d{2,}$` (ej: EL01) |
| `PUESTO_TRABAJO` | VARCHAR(100) | VARCHAR(100) | SÍ | Nombre del puesto (único por postulante+empresa) |
| `FECHA_INICIO` | DATE | DATE | NO | Fecha de inicio del período laboral |
| `FECHA_FIN` | DATE | DATE | NO | Fecha de fin del período laboral |
| `DESCP_EXPERIENCIA_LABORAL` | VARCHAR(500) | VARCHAR(500) | NO | Descripción de funciones y logros |
| `CONTACTO_REFERENCIA` | VARCHAR(100) | VARCHAR(100) | NO | Teléfono de contacto de referencia |

**Clave primaria:** Compuesta: `(ID_POSTULANTE, NIT, ID_EXPERIENCIA)`. Relaciona un postulante con una empresa y un código de experiencia.

**Claves foráneas (2 FKs):**

| Columna FK | Tabla padre | Columna padre |
|------------|-------------|---------------|
| `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| `NIT` | EMPRESA | `NIT` |

**Restricciones UNIQUE:** `(ID_POSTULANTE, NIT, PUESTO_TRABAJO)` — no puede repetirse el mismo puesto para el mismo postulante en la misma empresa.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `POSTULANTE` | `ID_POSTULANTE` |
| Es hija de | N:1 | `EMPRESA` | `NIT` |

**Índices asociados:**
- `IDX_EXP_POSTULANTE` ON EXPERIENCIA_LABORAL(ID_POSTULANTE)
- `IDX_EXP_EMPRESA` ON EXPERIENCIA_LABORAL(NIT)

**Trigger asociado:** `TR_EXP_LABORAL_FECHAS` (INSERT/UPDATE) — valida que `FECHA_INICIO < FECHA_FIN`.

---

#### 3.8.2 CERTIFICACION

**Descripción:** Certificaciones obtenidas por un postulante en una institución específica.

**Tipo de tabla:** Child de POSTULANTE + INSTITUCION + TIPO_CERTIFICACION

**Tipo de PK:** Compuesta (3 columnas: `ID_CERTIFICACION + ID_INSTITUCION + ID_POSTULANTE`)

**DDL (Power Designer):**
```sql
CREATE TABLE CERTIFICACION (
    ID_CERTIFICACION VARCHAR(10) NOT NULL,
    ID_INSTITUCION VARCHAR(20) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_TIPO_CERTIFICACION INTEGER,
    NOMBRE_CERTIFICACION VARCHAR(150) NOT NULL,
    FECHA_CERTIFICACION DATE,
    FECHA_INICIO DATE NOT NULL,
    FECHA_FIN DATE NOT NULL,
    PRIMARY KEY (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE),
    UNIQUE (ID_POSTULANTE, NOMBRE_CERTIFICACION),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_TIPO_CERTIFICACION) REFERENCES TIPO_CERTIFICACION (ID_TIPO_CERTIFICACION)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE CERTIFICACION (
    ID_CERTIFICACION VARCHAR(10) NOT NULL,
    ID_INSTITUCION VARCHAR(20) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_TIPO_CERTIFICACION INTEGER,
    NOMBRE_CERTIFICACION VARCHAR(150),
    FECHA_CERTIFICACION DATE,
    FECHA_INICIO DATE NOT NULL,
    FECHA_FIN DATE NOT NULL,
    PRIMARY KEY (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE),
    FOREIGN KEY (ID_INSTITUCION) REFERENCES INSTITUCION (ID_INSTITUCION),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_TIPO_CERTIFICACION) REFERENCES TIPO_CERTIFICACION (ID_TIPO_CERTIFICACION),
    UNIQUE (ID_POSTULANTE, NOMBRE_CERTIFICACION)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_CERTIFICACION` | VARCHAR(10) | VARCHAR(10) | SÍ | Código de certificación. 1ra parte de PK. Formato: `^C\d{3,}$` (ej: C001) |
| `ID_INSTITUCION` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → INSTITUCION. 2da parte de PK |
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → POSTULANTE. 3ra parte de PK |
| `ID_TIPO_CERTIFICACION` | INTEGER | INTEGER | NO | FK → TIPO_CERTIFICACION |
| `NOMBRE_CERTIFICACION` | VARCHAR(150) | VARCHAR(150) | SÍ | Nombre de la certificación (único por postulante) |
| `FECHA_CERTIFICACION` | DATE | DATE | NO | Fecha en que se emitió la certificación |
| `FECHA_INICIO` | DATE | DATE | SÍ | Fecha de inicio del período de estudio |
| `FECHA_FIN` | DATE | DATE | SÍ | Fecha de fin del período de estudio |

**Clave primaria:** Compuesta: `(ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE)`. Tres tablas diferentes conforman la PK.

**Claves foráneas (3 FKs — la mayor cantidad en una tabla hija):**

| Columna FK | Tabla padre | Columna padre |
|------------|-------------|---------------|
| `ID_INSTITUCION` | INSTITUCION | `ID_INSTITUCION` |
| `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| `ID_TIPO_CERTIFICACION` | TIPO_CERTIFICACION | `ID_TIPO_CERTIFICACION` |

**Restricciones UNIQUE:** `(ID_POSTULANTE, NOMBRE_CERTIFICACION)`.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `INSTITUCION` | `ID_INSTITUCION` |
| Es hija de | N:1 | `POSTULANTE` | `ID_POSTULANTE` |
| Es hija de | N:1 | `TIPO_CERTIFICACION` | `ID_TIPO_CERTIFICACION` |

**Índices asociados:**
- `IDX_CERT_POSTULANTE` ON CERTIFICACION(ID_POSTULANTE)
- `IDX_CERT_INSTITUCION` ON CERTIFICACION(ID_INSTITUCION)

**Trigger asociado:** `TR_CERTIFICACION_FECHAS` (INSERT/UPDATE) — el trigger más complejo del sistema. Valida:
1. `FECHA_INICIO < FECHA_FIN`
2. `FECHA_INICIO` no puede ser futura
3. `FECHA_FIN` no puede ser futura
4. `FECHA_CERTIFICACION >= FECHA_FIN` (no puede certificarse antes de terminar)
5. `FECHA_CERTIFICACION <= FECHA_FIN + 1 año`
6. `FECHA_CERTIFICACION` no puede ser futura

---

#### 3.8.3 FORMACION_ACADEMICA

**Descripción:** Formación académica de un postulante (títulos obtenidos, relacionados a una oferta académica opcional).

**Tipo de tabla:** Child de POSTULANTE + OFERTA_ACADEMICA

**Tipo de PK:** Compuesta (2 columnas: `ID_FORMACION + ID_POSTULANTE`)

**DDL (Power Designer):**
```sql
CREATE TABLE FORMACION_ACADEMICA (
    ID_FORMACION VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_OFERTA_ACADEMICA VARCHAR(10),
    TITULO_OBTENIDO VARCHAR(150),
    FECHA_INICIO DATE NOT NULL,
    FECHA_FIN DATE NOT NULL,
    FECHA_OBTENCION DATE,
    PRIMARY KEY (ID_FORMACION, ID_POSTULANTE),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_OFERTA_ACADEMICA) REFERENCES OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE FORMACION_ACADEMICA (
    ID_FORMACION VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_OFERTA_ACADEMICA VARCHAR(10),
    TITULO_OBTENIDO VARCHAR(150),
    FECHA_INICIO DATE NOT NULL,
    FECHA_FIN DATE NOT NULL,
    FECHA_OBTENCION DATE,
    PRIMARY KEY (ID_FORMACION, ID_POSTULANTE),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_OFERTA_ACADEMICA) REFERENCES OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_FORMACION` | VARCHAR(10) | VARCHAR(10) | SÍ | Código de formación. 1ra parte de PK. Formato: `^FOA\d{3,}$` (ej: FOA001) |
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → POSTULANTE. 2da parte de PK |
| `ID_OFERTA_ACADEMICA` | VARCHAR(10) | VARCHAR(10) | NO | FK → OFERTA_ACADEMICA (opcional) |
| `TITULO_OBTENIDO` | VARCHAR(150) | VARCHAR(150) | NO | Título obtenido tras completar la formación |
| `FECHA_INICIO` | DATE | DATE | SÍ | Fecha de inicio del período académico |
| `FECHA_FIN` | DATE | DATE | SÍ | Fecha de fin del período académico |
| `FECHA_OBTENCION` | DATE | DATE | NO | Fecha de obtención del título |

**Clave primaria:** Compuesta: `(ID_FORMACION, ID_POSTULANTE)`.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre |
|------------|-------------|---------------|
| `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| `ID_OFERTA_ACADEMICA` | OFERTA_ACADEMICA | `ID_OFERTA_ACADEMICA` |

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `POSTULANTE` | `ID_POSTULANTE` |
| Es hija de | N:1 | `OFERTA_ACADEMICA` | `ID_OFERTA_ACADEMICA` |

**Índices asociados:** `IDX_FORM_POSTULANTE` ON FORMACION_ACADEMICA(ID_POSTULANTE).

**Trigger asociado:** `TR_FORMACION_FECHAS` (INSERT/UPDATE) — similar al de CERTIFICACION, valida:
1. `FECHA_INICIO < FECHA_FIN`
2. `FECHA_INICIO` y `FECHA_FIN` no futuras
3. `FECHA_OBTENCION >= FECHA_FIN`
4. `FECHA_OBTENCION <= FECHA_FIN + 1 año`
5. `FECHA_OBTENCION` no futura

---

#### 3.8.4 HABILIDAD_POSTULANTE

**Descripción:** Asignación de habilidades a un postulante con un nivel de destreza específico.

**Tipo de tabla:** Child de HABILIDAD + POSTULANTE (tabla asociativa)

**Tipo de PK:** Compuesta (3 columnas: `ID_CATEGORIA_HABILIDAD + ID_HABILIDAD + ID_POSTULANTE`)

**DDL (Power Designer):**
```sql
CREATE TABLE HABILIDAD_POSTULANTE (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    NIVEL_DESTREZA VARCHAR(12),
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)
        REFERENCES HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
);
```

**DDL (SQLite):**
```sql
CREATE TABLE HABILIDAD_POSTULANTE (
    ID_CATEGORIA_HABILIDAD INTEGER NOT NULL,
    ID_HABILIDAD VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    NIVEL_DESTREZA VARCHAR(12),
    PRIMARY KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE),
    FOREIGN KEY (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD) REFERENCES HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_CATEGORIA_HABILIDAD` | INTEGER | INTEGER | SÍ | FK → HABILIDAD. 1ra parte de PK |
| `ID_HABILIDAD` | VARCHAR(10) | VARCHAR(10) | SÍ | FK → HABILIDAD. 2da parte de PK |
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → POSTULANTE. 3ra parte de PK |
| `NIVEL_DESTREZA` | VARCHAR(12) | VARCHAR(12) | NO | Nivel de destreza: "Básico", "Intermedio", "Avanzado" |

**Clave primaria:** Compuesta: `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE)`. Toda la PK son FKs.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)` | HABILIDAD | `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)` | **FK compuesta** |
| `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` | Simple |

**Esta es la única tabla que referencia una FK compuesta de HABILIDAD.**

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `HABILIDAD` | `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)` |
| Es hija de | N:1 | `POSTULANTE` | `ID_POSTULANTE` |

**Índices asociados:**
- `IDX_HAB_POST_POSTULANTE` ON HABILIDAD_POSTULANTE(ID_POSTULANTE)
- `IDX_HAB_POST_HABILIDAD` ON HABILIDAD_POSTULANTE(ID_HABILIDAD)

**Trigger asociado:** `TR_HABILIDAD_NIVEL` (INSERT/UPDATE) — valida que `NIVEL_DESTREZA` sea uno de: 'Básico', 'Intermedio', 'Avanzado'.

---

#### 3.8.5 RED_SOCIAL_POSTULANTE

**Descripción:** Redes sociales vinculadas a un postulante con la URL de su perfil.

**Tipo de tabla:** Child de POSTULANTE + RED_SOCIAL (tabla asociativa)

**Tipo de PK:** Compuesta (2 columnas: `ID_POSTULANTE + ID_RED_SOCIAL`)

**DDL (Power Designer):**
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

**DDL (SQLite):**
```sql
CREATE TABLE RED_SOCIAL_POSTULANTE (
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    ID_RED_SOCIAL INTEGER NOT NULL,
    URL_PERFIL VARCHAR(100),
    PRIMARY KEY (ID_POSTULANTE, ID_RED_SOCIAL),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    FOREIGN KEY (ID_RED_SOCIAL) REFERENCES RED_SOCIAL (ID_RED_SOCIAL)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → POSTULANTE. 1ra parte de PK |
| `ID_RED_SOCIAL` | INTEGER | INTEGER | SÍ | FK → RED_SOCIAL. 2da parte de PK |
| `URL_PERFIL` | VARCHAR(100) | VARCHAR(100) | NO | URL del perfil en la red social (debe comenzar con http:// o https://) |

**Clave primaria:** Compuesta: `(ID_POSTULANTE, ID_RED_SOCIAL)`. Un postulante no puede tener la misma red social dos veces.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre |
|------------|-------------|---------------|
| `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| `ID_RED_SOCIAL` | RED_SOCIAL | `ID_RED_SOCIAL` |

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `POSTULANTE` | `ID_POSTULANTE` |
| Es hija de | N:1 | `RED_SOCIAL` | `ID_RED_SOCIAL` |

**Índices asociados:** `IDX_RED_POST_POSTULANTE` ON RED_SOCIAL_POSTULANTE(ID_POSTULANTE).

---

### 3.9 TABLA DE POSTULACIÓN (1 tabla)

---

#### 3.9.1 POSTULACION

**Descripción:** Registro de postulaciones de un postulante a una oferta de trabajo. Representa la aplicación a una vacante.

**Tipo de tabla:** Postulación / Transaccional

**Tipo de PK:** Simple, manual (VARCHAR(10)) — con UNIQUE compuesta de negocio

**DDL (Power Designer):**
```sql
CREATE TABLE POSTULACION (
    ID_POSTULACION VARCHAR(10) NOT NULL,
    NIT VARCHAR(20) NOT NULL,
    ID_OFERTA VARCHAR(10) NOT NULL,
    ID_POSTULANTE VARCHAR(20) NOT NULL,
    FECHA_APLICACION DATE,
    ESTADO_PROCESO VARCHAR(50),
    PRIMARY KEY (ID_POSTULACION),
    UNIQUE (ID_POSTULANTE, NIT, ID_OFERTA),
    FOREIGN KEY (NIT, ID_OFERTA) REFERENCES OFERTA_TRABAJO (NIT, ID_OFERTA),
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE)
);
```

**DDL (SQLite):**
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
    FOREIGN KEY (ID_POSTULANTE) REFERENCES POSTULANTE (ID_POSTULANTE),
    UNIQUE (ID_POSTULANTE, NIT, ID_OFERTA)
)
```

**Atributos:**

| Columna | Tipo SQL | SQLite | NOT NULL | Descripción |
|---------|----------|--------|----------|-------------|
| `ID_POSTULACION` | VARCHAR(10) | VARCHAR(10) | SÍ | **Clave primaria manual.** Formato: `^POS\d{3,}$` (ej: POS001) |
| `NIT` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → OFERTA_TRABAJO |
| `ID_OFERTA` | VARCHAR(10) | VARCHAR(10) | SÍ | FK → OFERTA_TRABAJO |
| `ID_POSTULANTE` | VARCHAR(20) | VARCHAR(20) | SÍ | FK → POSTULANTE |
| `FECHA_APLICACION` | DATE | DATE | NO | Fecha en que se realizó la postulación |
| `ESTADO_PROCESO` | VARCHAR(50) | VARCHAR(50) | NO | Estado del proceso: "activo", "en proceso", "contratado", "rechazado" |

**Clave primaria:** Simple, manual (`ID_POSTULACION` VARCHAR(10)). Patrón: `^POS\d{3,}$`.

**Claves foráneas:**

| Columna FK | Tabla padre | Columna padre | Tipo |
|------------|-------------|---------------|------|
| `(NIT, ID_OFERTA)` | OFERTA_TRABAJO | `(NIT, ID_OFERTA)` | **FK compuesta** |
| `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` | Simple |

**Restricciones UNIQUE:** `(ID_POSTULANTE, NIT, ID_OFERTA)` — un postulante no puede postularse dos veces a la misma oferta. Esta UNIQUE **actúa como PK lógica**.

**Caso especial:** POSTULACION es la única tabla donde la PK formal (`ID_POSTULACION`) es diferente de la PK lógica. La PK formal es un código secuencial, pero la restricción UNIQUE de 3 columnas evita duplicados de negocio.

**Relaciones:**

| Relación | Tipo | Tabla | Columnas |
|----------|------|-------|----------|
| Es hija de | N:1 | `OFERTA_TRABAJO` | `(NIT, ID_OFERTA)` — FK compuesta |
| Es hija de | N:1 | `POSTULANTE` | `ID_POSTULANTE` |

**Índices asociados:**
- `IDX_POSTULACION_POSTULANTE` ON POSTULACION(ID_POSTULANTE)
- `IDX_POSTULACION_OFERTA` ON POSTULACION(NIT, ID_OFERTA)

**Trigger asociado:** `TR_POSTULACION_VIGENCIA` (solo INSERT) — verifica que la oferta de trabajo no haya vencido (`FECHA_CADUCIDAD >= date('now')`).

---

### 3.10 Resumen de Métricas del Esquema

| Métrica | Valor |
|---------|-------|
| **Total de tablas** | 23 |
| **Total de columnas** | 134 |
| **Total de claves primarias declarativas** | 23 (1 por tabla) |
| **Total de claves foráneas declarativas** | 22 |
| **Total de restricciones UNIQUE** | 23 |
| **Total de índices (implícitos no incluidos)** | 22 |
| **Total de triggers** | 23 |
| **Total de datos de semilla (registros)** | ~370 (14 deptos + 44 municipios + 262 distritos + 5 categorías + 15 habilidades + 5 géneros + 7 grados + 5 redes + 5 tipos cert + 3 tipos doc + 6 instituciones + 10 empresas + 5 ofertas académicas + 2 usuarios) |

#### 3.10.1 Tablas con más restricciones UNIQUE

| Tabla | UNIQUEs | Columnas |
|-------|---------|----------|
| `POSTULANTE` | 3 | NUM_DOCUMENTO, NUP, EMAIL |
| `EMPRESA` | 2 | NOMBRE_EMPRESA (implícita PK es NIT) |

#### 3.10.2 Tablas con más claves foráneas (como hija)

| Tabla | FK padres | Columnas FK |
|-------|-----------|-------------|
| `POSTULANTE` | 4 FK | GENERO, TIPO_DOCUMENTO, GRADO_ACADEMICO, DISTRITO (3 cols) |
| `CERTIFICACION` | 3 FK | INSTITUCION, POSTULANTE, TIPO_CERTIFICACION |
| `EXPERIENCIA_LABORAL` | 2 FK | POSTULANTE, EMPRESA |
| `FORMACION_ACADEMICA` | 2 FK | POSTULANTE, OFERTA_ACADEMICA |
| `HABILIDAD_POSTULANTE` | 2 FK | HABILIDAD (compuesta), POSTULANTE |
| `RED_SOCIAL_POSTULANTE` | 2 FK | POSTULANTE, RED_SOCIAL |

#### 3.10.3 Tablas con más dependencias (como padre)

| Tabla | Hijas | Tablas hijas |
|-------|-------|--------------|
| `POSTULANTE` | 6 | POSTULACION, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, CERTIFICACION, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE |
| `GRADO_ACADEMICO` | 3 | OFERTA_TRABAJO, OFERTA_ACADEMICA, POSTULANTE |
| `DISTRITO` | 2 | EMPRESA, POSTULANTE |
| `INSTITUCION` | 2 | OFERTA_ACADEMICA, CERTIFICACION |
| `EMPRESA` | 2 | OFERTA_TRABAJO, EXPERIENCIA_LABORAL |
| `OFERTA_TRABAJO` | 2 | DETALLE_REQUISITO, POSTULACION |

## 4. Triggers de la Base de Datos

### 4.1 Introducción a los Triggers

Los triggers son procedimientos almacenados que se ejecutan automáticamente en respuesta a eventos específicos en una tabla. En esta base de datos, todos los triggers son de tipo **BEFORE** (se ejecutan **antes** de que la operación se complete), lo que permite **cancelar la operación** mediante `RAISE(ABORT, ...)` si no se cumplen las validaciones.

#### 4.1.1 Características Generales

| Característica | Valor |
|----------------|-------|
| **Total de triggers** | 27 |
| **Triggers semánticos** | 17 (validan reglas de negocio) |
| **Triggers de integridad referencial** | 10 (verifican existencia de padres) |
| **Momento de ejecución** | Todos son `BEFORE` |
| **Eventos** | `INSERT` (17 triggers) y `UPDATE` (10 triggers) |
| **Tablas afectadas** | 9 de 23 tablas tienen triggers |
| **Mecanismo de cancelación** | `SELECT CASE WHEN ... THEN RAISE(ABORT, 'mensaje') END` |
| **Sintaxis SQLite** | `CREATE TRIGGER [IF NOT EXISTS] nombre BEFORE INSERT/UPDATE ON tabla FOR EACH ROW BEGIN ... END` |

#### 4.1.2 Mecanismo de Funcionamiento

Todos los triggers siguen el mismo patrón de diseño:

```sql
CREATE TRIGGER nombre_trigger
BEFORE INSERT OR UPDATE ON nombre_tabla
FOR EACH ROW
BEGIN
    SELECT CASE WHEN <condicion_de_error>
    THEN RAISE(ABORT, 'Mensaje de error para el usuario') END;
    -- Pueden haber múltiples CASE WHEN en un mismo trigger
END
```

**Explicación del patrón:**

1. **`BEFORE INSERT/UPDATE`**: El trigger se ejecuta antes de que la modificación se escriba en la base de datos. Si el trigger lanza un error, la operación se cancela y la base de datos no se modifica.

2. **`FOR EACH ROW`**: El trigger se ejecuta una vez por cada fila afectada (no por cada sentencia).

3. **`NEW.columna`**: Referencia al nuevo valor que se está insertando o actualizando. `NEW.FECHA_NACIMIENTO` se refiere al valor que el usuario intenta guardar.

4. **`SELECT CASE WHEN ... THEN RAISE(ABORT, ...) END`**: Esta es una construcción de SQLite que permite lanzar una excepción personalizada. Si la condición se cumple, se ejecuta `RAISE(ABORT, 'mensaje')`, lo que **aborta inmediatamente** la operación y devuelve el mensaje de error al llamante.

5. **`date('now')`**: Función de SQLite que devuelve la fecha actual en formato `YYYY-MM-DD`.

6. **`strftime('%Y', fecha)`**: Extrae el año de una fecha. Se usa para calcular la edad restando el año de nacimiento del año actual.

7. **Subconsultas**: Los triggers pueden hacer SELECT a otras tablas para validar reglas, como `(SELECT 1 FROM GENERO WHERE ID_GENRO = NEW.ID_GENERO) IS NULL` para verificar que un género existe.

#### 4.1.3 Clasificación

Los triggers se clasifican en **3 tipos funcionales** según la naturaleza de la validación que realizan:

| Tipo | Cantidad | Propósito | Subtipo |
|------|----------|-----------|---------|
| **Restricción CHECK** | 8 | Validan que los valores de una columna cumplan condiciones específicas (valores permitidos, rangos numéricos, valores prohibidos) | Actúan como `CHECK` constraints que SQLite no tiene de forma declarativa completa |
| **Validación Temporal** | 9 | Validan relaciones de orden y consistencia entre fechas (anterior/posterior, futura/no futura, vigencia) | Aseguran la coherencia cronológica de los datos |
| **Integridad Referencial** | 10 | Verifican que las claves foráneas apunten a registros existentes en las tablas padre | Complementan las FK declarativas de SQLite con mensajes de error personalizados |

**Nota:** Aunque SQLite soporta `FOREIGN KEY` declarativas, los triggers de integridad referencial se implementan explícitamente porque SQLite no siempre garantiza la integridad referencial de forma estricta (las FK deben habilitarse con `PRAGMA foreign_keys = ON` en cada conexión). Los triggers aseguran la validación incluso si el PRAGMA no está activo.

---

### 4.2 TRIGGERS DE RESTRICCIÓN CHECK (8 triggers)

Estos triggers actúan como **restricciones CHECK** (check constraints) que SQLite parser acepta sintácticamente pero no siempre valida de forma rigurosa en todas las versiones. Validan que una columna o combinación de columnas cumpla condiciones específicas de dominio: valores dentro de un conjunto, rangos numéricos, o valores excluidos.

---

#### 4.2.1 TR_POSTULANTE_EDAD / TR_POSTULANTE_EDAD_UPD

**Propósito:** Validar que el postulante sea mayor de edad (18+ años) y que la fecha de nacimiento no sea futura.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Restricción CHECK (con componente temporal) |
| **Tabla** | POSTULANTE |
| **Evento INSERT** | `TR_POSTULANTE_EDAD` — `BEFORE INSERT` |
| **Evento UPDATE** | `TR_POSTULANTE_EDAD_UPD` — `BEFORE UPDATE` |
| **Se dispara cuando** | Se inserta o actualiza un registro en POSTULANTE |

**Código SQL (INSERT y UPDATE son idénticos):**

```sql
CREATE TRIGGER TR_POSTULANTE_EDAD BEFORE INSERT ON POSTULANTE
FOR EACH ROW BEGIN
    -- Validación 1: Fecha de nacimiento no puede ser futura
    SELECT CASE WHEN NEW.FECHA_NACIMIENTO > date('now')
    THEN RAISE(ABORT, 'La fecha de nacimiento no puede ser futura') END;

    -- Validación 2: El postulante debe tener al menos 18 años
    SELECT CASE WHEN (strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)) < 18
    THEN RAISE(ABORT, 'El postulante debe ser mayor de edad') END;
END
```

**Explicación línea por línea:**

| Línea | Explicación |
|-------|-------------|
| `NEW.FECHA_NACIMIENTO > date('now')` | Compara la fecha de nacimiento ingresada con la fecha actual. Si la fecha de nacimiento es mayor (posterior) a hoy, es inválida |
| `(strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)) < 18` | Calcula la edad restando el año actual menos el año de nacimiento. Si el resultado es menor a 18, el postulante es menor de edad |

**Mensajes de error:**
- `"La fecha de nacimiento no puede ser futura"` — cuando `FECHA_NACIMIENTO > date('now')`
- `"El postulante debe ser mayor de edad"` — cuando la edad calculada < 18 años

**Limitación del cálculo de edad:** El cálculo `strftime('%Y', 'now') - strftime('%Y', NEW.FECHA_NACIMIENTO)` solo compara años, no la fecha exacta. Por ejemplo, alguien nacido el 31 de diciembre de 2007 sería considerado mayor de edad el 1 de enero de 2025 (2025 - 2007 = 18), aunque realmente cumpliría 18 hasta diciembre. Es una simplificación aceptable para este contexto.

**Validación complementaria en código:** `ValidationRules.kt` valida que `FECHA_NACIMIENTO` tenga formato `^\d{4}-\d{2}-\d{2}$` y que no sea fecha futura.

---

#### 4.2.2 TR_POSTULANTE_GRADO / TR_POSTULANTE_GRADO_UPD

**Propósito:** Impedir que un postulante tenga un grado académico de "Bachiller" (el sistema requiere un grado superior).

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Restricción CHECK (valor prohibido) |
| **Tabla** | POSTULANTE |
| **Evento INSERT** | `TR_POSTULANTE_GRADO` — `BEFORE INSERT` |
| **Evento UPDATE** | `TR_POSTULANTE_GRADO_UPD` — `BEFORE UPDATE` |
| **Se dispara cuando** | Se inserta o actualiza un registro en POSTULANTE |

**Código SQL (INSERT y UPDATE son idénticos):**

```sql
CREATE TRIGGER TR_POSTULANTE_GRADO BEFORE INSERT ON POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN (
        SELECT LOWER(NOMBRE_GRADO) FROM GRADO_ACADEMICO
        WHERE ID_GRADO_ACADEMICO = NEW.ID_GRADO_ACADEMICO
    ) IN ('bachiller')
    THEN RAISE(ABORT, 'El postulante debe tener un grado academico superior a Bachiller') END;
END
```

**Explicación línea por línea:**

| Línea | Explicación |
|-------|-------------|
| `SELECT LOWER(NOMBRE_GRADO) FROM GRADO_ACADEMICO WHERE ID_GRADO_ACADEMICO = NEW.ID_GRADO_ACADEMICO` | Subconsulta que busca el nombre del grado académico asociado al postulante. `LOWER()` convierte a minúsculas para comparación case-insensitive |
| `IN ('bachiller')` | Si el grado es exactamente "bachiller" (en minúsculas), la condición se cumple y se aborta la operación |

**Mensaje de error:**
- `"El postulante debe tener un grado academico superior a Bachiller"`

**Nota importante:** Este trigger valida el grado **actual** del postulante, no su formación académica. La tabla POSTULANTE tiene `ID_GRADO_ACADEMICO` que indica el grado máximo alcanzado. El trigger impide que un postulante tenga solo bachillerato, obligando a que tenga al menos un grado superior (Técnico, Licenciatura, Ingeniería, etc.).

**Datos de semilla — grados permitidos:** Técnico Superior, Profesorado, Licenciatura, Ingeniería, Maestría, Doctorado. **No permitido:** Bachiller.

**Validación complementaria en código:** No hay validación adicional en el código Kotlin para esta regla (depende exclusivamente del trigger).

---

#### 4.2.3 TR_OFERTA_RANGO_EDAD / TR_OFERTA_RANGO_EDAD_UPD

**Propósito:** Validar que la edad mínima de una oferta sea >= 18 y que no sea mayor a la edad máxima.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Restricción CHECK (rango numérico) |
| **Tabla** | OFERTA_TRABAJO |
| **Evento INSERT** | `TR_OFERTA_RANGO_EDAD` — `BEFORE INSERT` |
| **Evento UPDATE** | `TR_OFERTA_RANGO_EDAD_UPD` — `BEFORE UPDATE` |
| **Se dispara cuando** | Se inserta o actualiza un registro en OFERTA_TRABAJO |

**Código SQL (INSERT y UPDATE son idénticos):**

```sql
CREATE TRIGGER TR_OFERTA_RANGO_EDAD BEFORE INSERT ON OFERTA_TRABAJO
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.EDAD_MINIMA < 18
    THEN RAISE(ABORT, 'Edad minima debe ser mayor o igual a 18') END;
    SELECT CASE WHEN NEW.EDAD_MINIMA > NEW.EDAD_MAXIMA
    THEN RAISE(ABORT, 'Edad minima no puede ser mayor a la maxima') END;
END
```

**Explicación línea por línea:**

| Línea | Explicación |
|-------|-------------|
| `NEW.EDAD_MINIMA < 18` | La edad mínima requerida no puede ser menor a 18 años |
| `NEW.EDAD_MINIMA > NEW.EDAD_MAXIMA` | El rango de edad debe ser válido: mín <= máx |

**Mensajes de error:**
- `"Edad minima debe ser mayor o igual a 18"`
- `"Edad minima no puede ser mayor a la maxima"`

**Validación complementaria en código:** `ValidationRules.kt` define `EDAD_MINIMA` con `min = 18, max = 100` y `EDAD_MAXIMA` con `min = 16, max = 100`. El trigger es más restrictivo que la validación de código.

---

#### 4.2.4 TR_HABILIDAD_NIVEL / TR_HABILIDAD_NIVEL_UPD

**Propósito:** Validar que el nivel de destreza asignado a una habilidad del postulante sea uno de los valores permitidos: "Básico", "Intermedio" o "Avanzado".

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Restricción CHECK (valores permitidos) |
| **Tabla** | HABILIDAD_POSTULANTE |
| **Evento INSERT** | `TR_HABILIDAD_NIVEL` — `BEFORE INSERT` |
| **Evento UPDATE** | `TR_HABILIDAD_NIVEL_UPD` — `BEFORE UPDATE` |
| **Se dispara cuando** | Se inserta o actualiza un registro en HABILIDAD_POSTULANTE |

**Código SQL (INSERT y UPDATE son idénticos):**

```sql
CREATE TRIGGER TR_HABILIDAD_NIVEL BEFORE INSERT ON HABILIDAD_POSTULANTE
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.NIVEL_DESTREZA NOT IN ('Básico', 'Intermedio', 'Avanzado')
    THEN RAISE(ABORT, 'Nivel de destreza debe ser Basico, Intermedio o Avanzado') END;
END
```

**Explicación:**

| Línea | Explicación |
|-------|-------------|
| `NEW.NIVEL_DESTREZA NOT IN ('Básico', 'Intermedio', 'Avanzado')` | Verifica que el valor ingresado sea exactamente uno de los tres niveles permitidos. La comparación es exacta (case-sensitive, con acentos: 'Básico' con tilde) |

**Mensaje de error:**
- `"Nivel de destreza debe ser Basico, Intermedio o Avanzado"`

**Validación complementaria en el código:** En `EditorDialogFragment.kt`, el campo `NIVEL_DESTREZA` se renderiza como un dropdown con las 3 opciones predefinidas, por lo que el usuario nunca puede ingresar un valor inválido desde la UI.

---

### 4.3 TRIGGERS DE VALIDACIÓN TEMPORAL (9 triggers)

Estos triggers validan la **coherencia cronológica** de los datos: relaciones de orden entre fechas (inicio < fin), fechas futuras, vigencia de ofertas, y cálculos de edad. Aseguran que la línea de tiempo de los registros tenga sentido lógico.

---

#### 4.3.1 TR_OFERTA_VIGENCIA / TR_OFERTA_VIGENCIA_UPD

**Propósito:** Validar que la fecha de caducidad de una oferta sea posterior a la fecha de publicación.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Validación Temporal |
| **Tabla** | OFERTA_TRABAJO |
| **Evento INSERT** | `TR_OFERTA_VIGENCIA` — `BEFORE INSERT` |
| **Evento UPDATE** | `TR_OFERTA_VIGENCIA_UPD` — `BEFORE UPDATE` |
| **Se dispara cuando** | Se inserta o actualiza un registro en OFERTA_TRABAJO |

**Código SQL (INSERT y UPDATE son idénticos):**

```sql
CREATE TRIGGER TR_OFERTA_VIGENCIA BEFORE INSERT ON OFERTA_TRABAJO
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_CADUCIDAD <= NEW.FECHA_PUBLICACION
    THEN RAISE(ABORT, 'La oferta ya caduco o fecha invalida') END;
END
```

**Explicación:**

| Línea | Explicación |
|-------|-------------|
| `NEW.FECHA_CADUCIDAD <= NEW.FECHA_PUBLICACION` | La fecha de caducidad debe ser estrictamente posterior a la fecha de publicación |

**Mensaje de error:**
- `"La oferta ya caduco o fecha invalida"`

**Validación complementaria en código:** `ValidationRules.kt` valida formato de fechas.

---

#### 4.3.2 TR_POSTULACION_VIGENCIA

**Propósito:** Impedir que un postulante se postule a una oferta que ya ha vencido.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Validación Temporal |
| **Tabla** | POSTULACION |
| **Eventos** | Solo `INSERT` (**no tiene versión UPDATE**) |
| **Se dispara cuando** | Se inserta una nueva postulación |

**Código SQL:**

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

**Mensaje de error:**
- `"La oferta de trabajo ha vencido"`

---

#### 4.3.3 TR_EXP_LABORAL_FECHAS / TR_EXP_LABORAL_FECHAS_UPD

**Propósito:** Validar que la fecha de inicio de una experiencia laboral sea anterior a la fecha de fin.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Validación Temporal |
| **Tabla** | EXPERIENCIA_LABORAL |
| **Eventos** | INSERT y UPDATE |

**Código SQL:**

```sql
CREATE TRIGGER TR_EXP_LABORAL_FECHAS BEFORE INSERT ON EXPERIENCIA_LABORAL
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_INICIO >= NEW.FECHA_FIN
    THEN RAISE(ABORT, 'Fecha inicio debe ser menor a fecha fin') END;
END
```

**Mensaje de error:**
- `"Fecha inicio debe ser menor a fecha fin"`

---

#### 4.3.4 TR_CERTIFICACION_FECHAS / TR_CERTIFICACION_FECHAS_UPD

**Propósito:** **Trigger más complejo del sistema.** Valida 6 reglas de fechas para certificaciones.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Validación Temporal |
| **Tabla** | CERTIFICACION |
| **Eventos** | INSERT y UPDATE |
| **Número de validaciones** | **6** |

**Código SQL:**

```sql
CREATE TRIGGER TR_CERTIFICACION_FECHAS BEFORE INSERT ON CERTIFICACION
FOR EACH ROW BEGIN
    SELECT CASE WHEN NEW.FECHA_INICIO >= NEW.FECHA_FIN
    THEN RAISE(ABORT, 'Fecha inicio debe ser menor a fecha fin') END;
    SELECT CASE WHEN NEW.FECHA_INICIO > date('now')
    THEN RAISE(ABORT, 'Fecha inicio no puede ser una fecha futura') END;
    SELECT CASE WHEN NEW.FECHA_FIN > date('now')
    THEN RAISE(ABORT, 'Fecha fin no puede ser una fecha futura') END;
    SELECT CASE WHEN NEW.FECHA_CERTIFICACION < NEW.FECHA_FIN
    THEN RAISE(ABORT, 'Fecha de certificacion no puede ser menor a la fecha fin del periodo') END;
    SELECT CASE WHEN NEW.FECHA_CERTIFICACION > date(NEW.FECHA_FIN, '+1 years')
    THEN RAISE(ABORT, 'Fecha de certificacion no puede exceder un año despues de la fecha fin del periodo') END;
    SELECT CASE WHEN NEW.FECHA_CERTIFICACION > date('now')
    THEN RAISE(ABORT, 'Fecha de certificacion no puede ser una fecha futura') END;
END
```

**Mensajes de error (6):**
- `"Fecha inicio debe ser menor a fecha fin"`
- `"Fecha inicio no puede ser una fecha futura"`
- `"Fecha fin no puede ser una fecha futura"`
- `"Fecha de certificacion no puede ser menor a la fecha fin del periodo"`
- `"Fecha de certificacion no puede exceder un año despues de la fecha fin del periodo"`
- `"Fecha de certificacion no puede ser una fecha futura"`

---

#### 4.3.5 TR_FORMACION_FECHAS / TR_FORMACION_FECHAS_UPD

**Propósito:** Misma lógica que CERTIFICACION_FECHAS pero aplicada a la formación académica.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Validación Temporal |
| **Tabla** | FORMACION_ACADEMICA |
| **Eventos** | INSERT y UPDATE |
| **Número de validaciones** | **6** |

**Mensaje de error (6, análogos a CERTIFICACION):**
- `"Fecha inicio debe ser menor a fecha fin"`
- `"Fecha inicio no puede ser una fecha futura"`
- `"Fecha fin no puede ser una fecha futura"`
- `"Fecha de obtencion no puede ser menor a la fecha fin del periodo"`
- `"Fecha de obtencion no puede exceder un año despues de la fecha fin del periodo"`
- `"Fecha de obtencion no puede ser una fecha futura"`

---

### 4.4 TRIGGERS DE INTEGRIDAD REFERENCIAL (10 triggers)

Estos triggers verifican que las claves foráneas apunten a registros que realmente existen en las tablas padre.

---

#### 4.4.1 TR_MUNICIPIO_DEPTO / TR_MUNICIPIO_DEPTO_UPD

**Propósito:** Verificar que el departamento asociado a un municipio exista.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Integridad Referencial |
| **Tabla** | MUNICIPIO |
| **Eventos** | INSERT y UPDATE |
| **Verifica** | `ID_DEPARTAMENTO` → DEPARTAMENTO |

**Mensaje de error:** `"El departamento asociado no existe"`

---

#### 4.4.2 TR_DISTRITO_MUNICIPIO / TR_DISTRITO_MUNICIPIO_UPD

**Propósito:** Verificar que el municipio asociado a un distrito exista (PK compuesta).

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Integridad Referencial |
| **Tabla** | DISTRITO |
| **Eventos** | INSERT y UPDATE |
| **Verifica** | `(ID_DEPARTAMENTO, ID_MUNICIPIO)` → MUNICIPIO(PK compuesta) |

**Mensaje de error:** `"El municipio asociado no existe"`

---

#### 4.4.3 TR_HABILIDAD_CATEGORIA / TR_HABILIDAD_CATEGORIA_UPD

**Propósito:** Verificar que la categoría asociada a una habilidad exista.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Integridad Referencial |
| **Tabla** | HABILIDAD |
| **Eventos** | INSERT y UPDATE |
| **Verifica** | `ID_CATEGORIA_HABILIDAD` → CATEGORIA_HABILIDAD |

**Mensaje de error:** `"La categoria asociada no existe"`

---

#### 4.4.4 TR_EMPRESA_DISTRITO / TR_EMPRESA_DISTRITO_UPD

**Propósito:** Verificar que el distrito asociado a una empresa exista (PK compuesta de 3 columnas).

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Integridad Referencial |
| **Tabla** | EMPRESA |
| **Eventos** | INSERT y UPDATE |
| **Verifica** | `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` → DISTRITO(PK 3 cols) |

**Mensaje de error:** `"El distrito asociado no existe"`

---

#### 4.4.5 TR_POSTULANTE_FK / TR_POSTULANTE_FK_UPD

**Propósito:** Verificar que el género, tipo de documento y grado académico asociados a un postulante existan.

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | Integridad Referencial |
| **Tabla** | POSTULANTE |
| **Eventos** | INSERT y UPDATE |
| **Verifica** | 3 tablas: GENERO, TIPO_DOCUMENTO, GRADO_ACADEMICO |

**Mensajes de error:**
- `"El genero asociado no existe"`
- `"El tipo de documento asociado no existe"`
- `"El grado academico asociado no existe"`

---

### 4.5 Resumen de Triggers

#### 4.5.1 Tabla Resumen Completa

| # | Trigger | Subtipo | Tabla | Evento | Validaciones |
|---|---------|---------|-------|--------|--------------|
| 1 | `TR_POSTULANTE_EDAD` | Restricción CHECK | POSTULANTE | INSERT | 2: fecha no futura, edad >= 18 |
| 2 | `TR_POSTULANTE_EDAD_UPD` | Restricción CHECK | POSTULANTE | UPDATE | 2: (ídem) |
| 3 | `TR_POSTULANTE_GRADO` | Restricción CHECK | POSTULANTE | INSERT | 1: grado ≠ Bachiller |
| 4 | `TR_POSTULANTE_GRADO_UPD` | Restricción CHECK | POSTULANTE | UPDATE | 1: (ídem) |
| 5 | `TR_OFERTA_RANGO_EDAD` | Restricción CHECK | OFERTA_TRABAJO | INSERT | 2: mín >= 18, mín <= máx |
| 6 | `TR_OFERTA_RANGO_EDAD_UPD` | Restricción CHECK | OFERTA_TRABAJO | UPDATE | 2: (ídem) |
| 7 | `TR_HABILIDAD_NIVEL` | Restricción CHECK | HABILIDAD_POSTULANTE | INSERT | 1: nivel ∈ {Básico, Intermedio, Avanzado} |
| 8 | `TR_HABILIDAD_NIVEL_UPD` | Restricción CHECK | HABILIDAD_POSTULANTE | UPDATE | 1: (ídem) |
| 9 | `TR_OFERTA_VIGENCIA` | Validación Temporal | OFERTA_TRABAJO | INSERT | 1: caducidad > publicación |
| 10 | `TR_OFERTA_VIGENCIA_UPD` | Validación Temporal | OFERTA_TRABAJO | UPDATE | 1: (ídem) |
| 11 | `TR_POSTULACION_VIGENCIA` | Validación Temporal | POSTULACION | **INSERT** (solo) | 1: oferta no vencida |
| 12 | `TR_EXP_LABORAL_FECHAS` | Validación Temporal | EXPERIENCIA_LABORAL | INSERT | 1: inicio < fin |
| 13 | `TR_EXP_LABORAL_FECHAS_UPD` | Validación Temporal | EXPERIENCIA_LABORAL | UPDATE | 1: (ídem) |
| 14 | `TR_CERTIFICACION_FECHAS` | Validación Temporal | CERTIFICACION | INSERT | **6**: período, fechas, certificación |
| 15 | `TR_CERTIFICACION_FECHAS_UPD` | Validación Temporal | CERTIFICACION | UPDATE | **6**: (ídem) |
| 16 | `TR_FORMACION_FECHAS` | Validación Temporal | FORMACION_ACADEMICA | INSERT | **6**: período, fechas, obtención |
| 17 | `TR_FORMACION_FECHAS_UPD` | Validación Temporal | FORMACION_ACADEMICA | UPDATE | **6**: (ídem) |
| 18 | `TR_MUNICIPIO_DEPTO` | Integridad Referencial | MUNICIPIO | INSERT | 1: departamento existe |
| 19 | `TR_MUNICIPIO_DEPTO_UPD` | Integridad Referencial | MUNICIPIO | UPDATE | 1: (ídem) |
| 20 | `TR_DISTRITO_MUNICIPIO` | Integridad Referencial | DISTRITO | INSERT | 1: municipio existe (PK compuesta) |
| 21 | `TR_DISTRITO_MUNICIPIO_UPD` | Integridad Referencial | DISTRITO | UPDATE | 1: (ídem) |
| 22 | `TR_HABILIDAD_CATEGORIA` | Integridad Referencial | HABILIDAD | INSERT | 1: categoría existe |
| 23 | `TR_HABILIDAD_CATEGORIA_UPD` | Integridad Referencial | HABILIDAD | UPDATE | 1: (ídem) |
| 24 | `TR_EMPRESA_DISTRITO` | Integridad Referencial | EMPRESA | INSERT | 1: distrito existe (PK 3 cols) |
| 25 | `TR_EMPRESA_DISTRITO_UPD` | Integridad Referencial | EMPRESA | UPDATE | 1: (ídem) |
| 26 | `TR_POSTULANTE_FK` | Integridad Referencial | POSTULANTE | INSERT | **3**: género, tipo_doc, grado existen |
| 27 | `TR_POSTULANTE_FK_UPD` | Integridad Referencial | POSTULANTE | UPDATE | **3**: (ídem) |

#### 4.5.2 Triggers por Tabla

| Tabla | Restricción CHECK | Validación Temporal | Integridad Referencial | Total |
|-------|:-----------------:|:-------------------:|:---------------------:|:-----:|
| POSTULANTE | TR_POSTULANTE_EDAD, TR_POSTULANTE_GRADO (2 pares) | — | TR_POSTULANTE_FK (1 par) | **6** |
| OFERTA_TRABAJO | TR_OFERTA_RANGO_EDAD (1 par) | TR_OFERTA_VIGENCIA (1 par) | — | **4** |
| CERTIFICACION | — | TR_CERTIFICACION_FECHAS (1 par) | — | **2** |
| FORMACION_ACADEMICA | — | TR_FORMACION_FECHAS (1 par) | — | **2** |
| HABILIDAD_POSTULANTE | TR_HABILIDAD_NIVEL (1 par) | — | — | **2** |
| EXPERIENCIA_LABORAL | — | TR_EXP_LABORAL_FECHAS (1 par) | — | **2** |
| POSTULACION | — | TR_POSTULACION_VIGENCIA (solo INSERT) | — | **1** |
| MUNICIPIO | — | — | TR_MUNICIPIO_DEPTO (1 par) | **2** |
| DISTRITO | — | — | TR_DISTRITO_MUNICIPIO (1 par) | **2** |
| HABILIDAD | — | — | TR_HABILIDAD_CATEGORIA (1 par) | **2** |
| EMPRESA | — | — | TR_EMPRESA_DISTRITO (1 par) | **2** |
| **Totales** | **8** | **9** | **10** | **27** |

#### 4.5.3 Tablas sin Triggers

| Tabla | Razón |
|-------|-------|
| CATEGORIA_HABILIDAD | Catálogo simple. Unicidad vía UNIQUE |
| GENERO | Catálogo simple. Unicidad vía UNIQUE |
| TIPO_DOCUMENTO | Catálogo simple. Unicidad vía UNIQUE |
| DEPARTAMENTO | Catálogo simple. Unicidad vía UNIQUE |
| GRADO_ACADEMICO | Catálogo simple. Unicidad vía UNIQUE |
| RED_SOCIAL | Catálogo simple. Unicidad vía UNIQUE |
| TIPO_CERTIFICACION | Catálogo simple. Unicidad vía UNIQUE |
| USUARIO | Sin reglas semánticas especiales |
| INSTITUCION | Catálogo simple. Unicidad vía UNIQUE |
| OFERTA_ACADEMICA | Sin reglas de negocio adicionales |
| DETALLE_REQUISITO | Child simple de OFERTA_TRABAJO |
| RED_SOCIAL_POSTULANTE | Tabla asociativa simple |

#### 4.5.4 Mapa de Traducción de Errores (TriggerErrorTranslator.kt)

El archivo `TriggerErrorTranslator.kt` contiene un mapa de 50+ reglas que traduce los mensajes de error de los triggers (y otros errores SQL) a mensajes amigables para el usuario. A continuación se muestra la correspondencia entre los errores de triggers y su traducción:

| Mensaje del Trigger | Traducción al Usuario |
|---------------------|----------------------|
| `"La fecha de nacimiento no puede ser futura"` | *Mensaje directo (ya es amigable)* |
| `"El postulante debe ser mayor de edad"` | `"El postulante debe ser mayor de 18 años"` |
| `"grado academico superior"` | `"El postulante debe tener un grado academico superior a Bachiller"` |
| `"Edad minima debe ser mayor o igual a 18"` | `"La edad minima debe ser mayor o igual a 18"` |
| `"Edad minima no puede ser mayor a la maxima"` | `"La edad minima no puede ser mayor a la edad maxima"` |
| `"La oferta ya caduco o fecha invalida"` | `"La fecha de caducidad debe ser posterior a la fecha de publicacion"` |
| `"Fecha inicio debe ser menor a fecha fin"` | `"La fecha de inicio debe ser anterior a la fecha de finalizacion"` |
| `"Nivel de destreza debe ser 1, 2 o 3"` | `"El nivel de destreza debe ser 1 (Basico), 2 (Intermedio) o 3 (Avanzado)"` |
| `"El departamento asociado no existe"` | `"El departamento seleccionado no existe"` |
| `"El municipio asociado no existe"` | `"El municipio seleccionado no existe"` |
| `"La categoria asociada no existe"` | `"La categoria seleccionada no existe"` |
| `"El distrito asociado no existe"` | `"El distrito seleccionado no existe"` |
| `"El genero asociado no existe"` | `"El genero seleccionado no existe"` |
| `"El tipo de documento asociado no existe"` | `"El tipo de documento seleccionado no existe"` |
| `"fecha de certificacion no puede ser menor a la fecha fin"` | `"La fecha de certificacion no puede ser menor a la fecha fin del periodo"` |
| `"fecha de certificacion no puede exceder un año"` | `"La fecha de certificacion no puede exceder un año despues de la fecha fin del periodo"` |
| `"fecha inicio no puede ser una fecha futura"` | `"La fecha de inicio no puede ser una fecha futura"` |
| `"fecha fin no puede ser una fecha futura"` | `"La fecha fin no puede ser una fecha futura"` |
| Errores de FK constraint | `"No se puede modificar: tiene registros asociados. Elimine primero los registros dependientes"` |
| Errores de UNIQUE constraint | `"Ya existe un registro con esos datos"` |

## 5. Validaciones en Código

### 5.1 Arquitectura de Validación

El sistema implementa **4 capas de validación** que actúan en secuencia antes de que un dato sea persistido en la base de datos:

```
┌─────────────────────────────────────────────────────────────────────┐
│ CAPA 1: UI INPUT CONTROLS (EditorDialogFragment.kt)                │
│                                                                     │
│  • InputFilter: longitud máxima, solo dígitos, caracteres           │
│  • InputType: teclado numérico, email, teléfono, password, multilínea│
│  • InputMask: formato automático DUI (00000000-0), NIT, teléfono   │
│  • Dropdowns fijos: NIVEL_DESTREZA, ESTADO_PROCESO, ROL            │
│  • Dropdowns dinámicos FK: evitan IDs inválidos                     │
│  • DatePicker: solo fechas válidas (1926 - año actual)              │
│  • TextWatcher: validación inline al escribir (email, password,     │
│    fecha, y formateo automático)                                   │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│ CAPA 2: VALIDATIONRULES.KT (motor de reglas por tabla)              │
│                                                                     │
│  • Campo requerido (isBlank)                                        │
│  • Longitud mínima y máxima                                         │
│  • Patrón regex (códigos, fechas, email, URL)                       │
│  • Rango numérico mínimo/máximo                                     │
│  • Fechas futuras prohibidas (lista por tabla)                      │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│ CAPA 3: EDITORDIALOGFRAGMENT (validaciones adicionales)             │
│                                                                     │
│  • Permisos de rol (solo FULL ACCESS puede guardar)                 │
│  • Periodo de fechas (inicio < fin, obtención post-fin, ≤ 1 año)   │
│  • FK dropdowns: valor seleccionado debe existir en tabla padre     │
│  • NIVEL_DESTREZA: dropdown obligatorio                             │
│  • ESTADO_PROCESO: mapeo texto → valor interno                      │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│ CAPA 4: MAINREPOSITORY.KT (duplicados + normalización)              │
│                                                                     │
│  • checkDuplicateInsert/Update: unicidad forzada por tabla          │
│  • Normalización: minúsculas en nombres, emails, descripciones      │
│  • Hash de contraseña (PBKDF2) en tabla USUARIO                     │
│  • Cálculo de ID autoincremental                                    │
│  • Los errores se traducen vía TriggerErrorTranslator               │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ▼
                      ┌─────────────────────────┐
                      │  BASE DE DATOS SQLite    │
                      │  (triggers + FKs + PKs) │
                      └─────────────────────────┘
```

### 5.2 CAPA 1 — Controles de UI en EditorDialogFragment

#### 5.2.1 InputFilter (Restricciones de Caracteres)

El método `getFilters(column: String)` en `EditorDialogFragment.kt` (línea 1366) aplica filtros dinámicos a los campos de texto según la columna:

| Columna(s) | Filtro aplicado | Longitud máx |
|------------|-----------------|:------------:|
| `TELEFONO_CASA`, `TELEFONO_CELULAR`, `TEL_*` | Solo dígitos | 9 |
| `CONTACTO_REFERENCIA` | Solo dígitos | 9 |
| `CONTACTO_DIRECTO` | Solo dígitos y guiones | 9 |
| `NIT` (en EMPRESA) | Solo dígitos | 14 |
| `NUP` | Solo dígitos | 12 |
| `NUM_DOCUMENTO` | DUI: dígitos+guión; NIT: dígitos+guión | 17 |
| `EXPERIENCIA_ANIOS` | Solo dígitos | 2 |
| `EDAD_MINIMA`, `EDAD_MAXIMA` | Solo dígitos | 2 |
| *Cualquier código* (`ID_*`, `CODIGO*`) | Sin filtro de caracteres | 30 |

**Mecanismo de filtro personalizado:**
```kotlin
// Ejemplo: filtro solo dígitos para EXPERIENCIA_ANIOS
InputFilter { source, start, end, _, _, _ ->
    for (i in start until end) { 
        if (!source[i].isDigit()) return@InputFilter "" 
    }
    null
}
```
Cuando el filtro retorna `""` (string vacío), el carácter no deseado es bloqueado inmediatamente, impidiendo físicamente que el usuario lo escriba.

#### 5.2.2 InputType (Teclado Contextual)

El método `getInputType(column: String)` asigna el tipo de teclado Android apropiado:

| Tipo de columna | InputType | Efecto |
|-----------------|-----------|--------|
| IDs numéricos (`ID_GENERO`, `ID_DEPARTAMENTO`, `NUP`) | `TYPE_CLASS_NUMBER` | Teclado numérico |
| IDs alfanuméricos (`ID_HABILIDAD`, `ID_POSTULANTE`, `NIT`) | `TYPE_CLASS_TEXT` | Teclado alfanumérico |
| Fechas (`FECHA_*`, `DATE_*`) | `TYPE_CLASS_TEXT` | Teclado + DatePicker al tocar |
| Email (`EMAIL`) | `TYPE_CLASS_TEXT \| TYPE_TEXT_VARIATION_EMAIL_ADDRESS` | Teclado con @ y . |
| Teléfono (`TELEFONO_*`, `CONTACTO_*`) | `TYPE_CLASS_PHONE` | Teclado telefónico |
| Contraseña (`PASSWORD`) | `TYPE_CLASS_TEXT \| TYPE_TEXT_VARIATION_PASSWORD` | Caracteres ocultos |
| Numéricos (`EXPERIENCIA_ANIOS`, `EDAD_MIN`, `EDAD_MAX`) | `TYPE_CLASS_NUMBER` | Teclado numérico |
| Descripciones largas | `TYPE_CLASS_TEXT \| TYPE_TEXT_FLAG_MULTI_LINE` | Multilínea con saltos de línea |

#### 5.2.3 InputMask (Formato Automático)

El método `formatInput(column, text)` en `EditorDialogFragment.kt` (línea 1349) aplica formato automático mientras el usuario escribe, utilizando las funciones de `InputMaskUtils.kt`:

| Columna | Función | Formato |
|---------|---------|---------|
| `NUM_DOCUMENTO` (cuando tipo = DUI) | `InputMaskUtils.formatDUI()` | `00000000-0` (8 dígitos + guión + 1 dígito verificador) |
| `NUM_DOCUMENTO` (cuando tipo = NIT) | `InputMaskUtils.formatNIT()` | `0000-000000-000-0` (14 dígitos con guiones) |
| `NUM_DOCUMENTO` (cuando tipo = PASAPORTE) | Sin formato | Texto libre hasta 17 caracteres |
| `TELEFONO_CASA`, `TELEFONO_CELULAR`, `CONTACTO_DIRECTO`, `CONTACTO_REFERENCIA` | `InputMaskUtils.formatTelefono()` | `0000-0000` (8 dígitos) |
| `NIT` (en EMPRESA) | `InputMaskUtils.formatNitSimple()` | Solo dígitos, máx 14 |

**Mecanismo de Formateo:** Cada función extrae solo dígitos del texto ingresado, luego aplica el formato según la longitud. El `TextWatcher.afterTextChanged()` detecta cambios y reemplaza el texto automáticamente, reposicionando el cursor.

#### 5.2.4 Tipos de Documento (DUI/NIT/Pasaporte) — Validación Cruzada

Cuando el usuario selecciona un `ID_TIPO_DOCUMENTO` en POSTULANTE, el sistema activa el método `refreshNumDocHintAndValidation()` (línea 1106):

1. **Detección del tipo:** El método `getSelectedDocType()` analiza el texto del dropdown de tipo de documento y retorna "DUI", "NIT" o "PASAPORTE".

2. **Cambio de hint:** El TextInputLayout de `NUM_DOCUMENTO` cambia su hint según el tipo:
   - DUI → "DUI"
   - NIT → "NIT"
   - Pasaporte → "Pasaporte"

3. **Filtros dinámicos:** Se reemplazan los filtros del campo:
   - DUI: solo dígitos y guiones, máx 10 caracteres
   - NIT: solo dígitos y guiones, máx 17 caracteres
   - Pasaporte: sin restricción de caracteres, máx 17

4. **Formato automático:** El TextWatcher aplica `formatDUI()` o `formatNIT()` según corresponda.

5. **Bloqueo:** Si no se ha seleccionado un tipo de documento, el campo `NUM_DOCUMENTO` se deshabilita.

#### 5.2.5 Dropdowns Fijos con Valores Controlados

Para ciertos campos, se usan dropdowns con valores fijos que impiden valores inválidos:

| Campo | Tabla | Valores |
|-------|-------|---------|
| `NIVEL_DESTREZA` | HABILIDAD_POSTULANTE | Básico, Intermedio, Avanzado |
| `ESTADO_PROCESO` | POSTULACION | Activo, En Proceso, Contratado, Rechazado |
| `ROL` | USUARIO | postulante, gerente de empresa, administrador |

Estos dropdowns usan `MaterialAutoCompleteTextView` con `setAdapter(ArrayAdapter)` de opciones fijas. El usuario no puede escribir valores libres.

#### 5.2.6 Dropdowns Dinámicos FK (Cascading)

Para campos que son claves foráneas, el editor carga dinámicamente las opciones desde la tabla padre. Algunos dropdowns están **encadenados** (cascading):

| Tabla | Dependencia | Cadena |
|-------|-------------|--------|
| DISTRITO | `ID_DEPARTAMENTO` → filtra `ID_MUNICIPIO` → filtra `ID_DISTRITO` | Depto → Municipio → Distrito |
| EMPRESA | `ID_DISTRITO_DEPTO` → filtra `ID_DISTRITO_MUNICIPIO` → filtra `ID_DISTRITO_ID` | Depto → Municipio → Distrito |
| POSTULANTE | `ID_DISTRITO_DEPTO` → filtra `ID_DISTRITO_MUNICIPIO` → filtra `ID_DISTRITO_ID` | Depto → Municipio → Distrito |
| HABILIDAD_POSTULANTE | `ID_CATEGORIA_HABILIDAD` → filtra `ID_HABILIDAD` | Categoría → Habilidad |
| POSTULACION | `NIT` → filtra `ID_OFERTA` (solo ofertas vigentes) | Empresa → Ofertas Vigentes |
| DETALLE_REQUISITO | `NIT` → filtra `ID_OFERTA` | Empresa → Ofertas |
| MUNICIPIO (genérico) | `ID_DEPARTAMENTO` → filtra `ID_MUNICIPIO` | Depto → Municipio |

El mecanismo se implementa en `createDropdownField()` (línea 250) y `refreshDependentDropdown()` (línea 1168), que detectan cambios en el dropdown padre y actualizan el adapter del dropdown hijo mediante `getFilteredOptions()`.

#### 5.2.7 DatePicker (Selector de Fechas)

Para todas las columnas que contienen "FECHA", el `TextInputEditText` se configura como:
- `isFocusable = false` (no se puede escribir)
- `isClickable = true` (se puede tocar)
- `setOnClickListener { showDatePicker(et) }` (abre el picker)

El DatePicker (`MaterialDatePicker`) tiene las siguientes restricciones:
- **Fecha mínima:** 1 de enero de 1926
- **Fecha máxima:** 31 de diciembre del año actual
- **Zona horaria:** UTC (para evitar desfases)
- **Formato retornado:** `yyyy-MM-dd`

#### 5.2.8 Validación Inline (TextWatcher)

Cada campo de texto (`TextInputEditText`) tiene un `TextWatcher` que ejecuta validación en tiempo real:

```kotlin
et.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        val text = s?.toString() ?: ""
        val formatted = formatInput(column, text)     // Formato automático
        if (formatted != text) {
            et.setText(formatted)                      // Reemplaza con formato
            et.setSelection(formatted.length)
        }
        val error = getFieldValidationError(column, formatted)  // Validación inline
        til.error = error                               // Muestra error en el campo
    }
})
```

El método `getFieldValidationError()` (línea 1576) aplica validaciones específicas:

| Columna | Validación | Mensaje |
|---------|------------|---------|
| `EMAIL` | `InputMaskUtils.validateEmail()` | "Correo electrónico inválido" |
| `PASSWORD` | `InputMaskUtils.validatePassword()` | "Mínimo 8 caracteres" |
| `FECHA_*` | `InputMaskUtils.validateFecha()` | "Formato: AAAA-MM-DD" |

---

### 5.3 CAPA 2 — ValidationRules.kt (Por Tabla)

El motor de validación se encuentra en `ValidationRules.kt`. Define una estructura `FieldRule` y un mapa de reglas por tabla.

#### 5.3.1 Estructura FieldRule

```kotlin
data class FieldRule(
    val field: String,          // Nombre de la columna
    val required: Boolean,      // ¿Es obligatorio?
    val minLength: Int,         // Longitud mínima (caracteres)
    val maxLength: Int,         // Longitud máxima (caracteres)
    val pattern: String?,       // Expresión regular
    val min: Int?,              // Valor numérico mínimo
    val max: Int?,              // Valor numérico máximo
    val friendlyName: String    // Nombre para mostrar en mensajes de error
)
```

#### 5.3.2 Matriz Completa de Reglas por Tabla

**USUARIO:**

| Columna | Requerido | MinLength | MaxLength | Pattern | Min | Max | FriendlyName |
|---------|:---------:|:---------:|:---------:|---------|:---:|:---:|-------------|
| USERNAME | ✓ | 3 | — | — | — | — | "Usuario" |
| PASSWORD | ✓ | 8 | — | — | — | — | "Contraseña" |
| ROL | ✓ | — | — | — | — | — | "Rol" |

**POSTULANTE:**

| Columna | Requerido | MinLength | MaxLength | Pattern | Min | Max | FriendlyName |
|---------|:---------:|:---------:|:---------:|---------|:---:|:---:|-------------|
| ID_POSTULANTE | ✓ | — | 7 | `^[A-Z]{2}\d{5}$` | — | — | "Codigo postulante" |
| NOMBRE | ✓ | — | — | — | — | — | "Nombre" |
| APELLIDO | ✓ | — | — | — | — | — | "Apellido" |
| NUM_DOCUMENTO | ✓ | — | — | — | — | — | "Numero de documento" |
| EMAIL | ✓ | — | — | `^[^@]+@[^@]+\.[^@]+$` | — | — | "Correo electronico" |
| FECHA_NACIMIENTO | ✓ | — | — | `^\d{4}-\d{2}-\d{2}$` | — | — | "Fecha de nacimiento" |
| ID_GENERO | ✓ | — | — | — | — | — | "Genero" |
| ID_TIPO_DOCUMENTO | ✓ | — | — | — | — | — | "Tipo de documento" |
| ID_GRADO_ACADEMICO | ✓ | — | — | — | — | — | "Grado academico" |
| NUP | ✓ | — | — | — | — | — | "NUP" |
| DIRECCION_DETALLE | ✓ | — | — | — | — | — | "Direccion" |
| TELEFONO_CASA | ✓ | — | — | — | — | — | "Telefono casa" |
| TELEFONO_CELULAR | ✓ | — | — | — | — | — | "Telefono celular" |

**EMPRESA:**

| Columna | Requerido | MinLength | MaxLength | Pattern | Min | Max | FriendlyName |
|---------|:---------:|:---------:|:---------:|---------|:---:|:---:|-------------|
| NIT | ✓ | 14 | 14 | `^\d{14}$` | — | — | "NIT" |
| NOMBRE_EMPRESA | ✓ | — | — | — | — | — | "Nombre de empresa" |
| CONTACTO_DIRECTO | ✓ | 9 | 9 | `^\d{4}-\d{4}$` | — | — | "Contacto directo" |

**OFERTA_TRABAJO:**

| Columna | Requerido | MinLength | MaxLength | Pattern | Min | Max | FriendlyName |
|---------|:---------:|:---------:|:---------:|---------|:---:|:---:|-------------|
| ID_OFERTA | ✓ | — | 10 | `^OF\d{2,}$` | — | — | "Codigo oferta" |
| TITULO_PUESTO | ✓ | — | — | — | — | — | "Titulo del puesto" |
| FECHA_PUBLICACION | ✓ | — | — | `^\d{4}-\d{2}-\d{2}$` | — | — | "Fecha de publicacion" |
| FECHA_CADUCIDAD | ✓ | — | — | `^\d{4}-\d{2}-\d{2}$` | — | — | "Fecha de caducidad" |
| EXPERIENCIA_ANIOS | ✓ | — | — | — | — | — | "Anios de experiencia" |
| EDAD_MINIMA | — | — | — | — | 18 | 100 | "Edad minima" |
| EDAD_MAXIMA | — | — | — | — | 16 | 100 | "Edad maxima" |
| DESCRIPCION_OFERTA_TRABAJO | ✓ | — | — | — | — | — | "Descripcion de la oferta" |

**CATEGORIA_HABILIDAD:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_CATEGORIA | ✓ | "Nombre de categoria" |

**GENERO:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_GENERO | ✓ | "Nombre de genero" |

**TIPO_DOCUMENTO:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_TIPO | ✓ | "Nombre de tipo" |

**DEPARTAMENTO:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_DEPARTAMENTO | ✓ | "Nombre de departamento" |

**MUNICIPIO:**

| Columna | Requerido | Min | Max | FriendlyName |
|---------|:---------:|:---:|:---:|-------------|
| ID_MUNICIPIO | ✓ | 1 | 9999 | "Codigo municipio" |
| NOMBRE_MUNICIPIO | ✓ | — | — | "Nombre de municipio" |

**DISTRITO:**

| Columna | Requerido | Min | Max | FriendlyName |
|---------|:---------:|:---:|:---:|-------------|
| ID_DISTRITO | ✓ | 1 | 9999 | "Codigo distrito" |
| NOMBRE_DISTRITO | ✓ | — | — | "Nombre de distrito" |

**INSTITUCION:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_INSTITUCION | ✓ | 20 | `^INS\d{3,}$` | "Codigo institucion" |
| NOMBRE_INSTITUCION | ✓ | — | — | "Nombre de institucion" |

**GRADO_ACADEMICO:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_GRADO | ✓ | "Nombre de grado" |

**RED_SOCIAL:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_RED | ✓ | "Nombre de red social" |

**HABILIDAD:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_CATEGORIA_HABILIDAD | ✓ | — | — | "Categoria" |
| ID_HABILIDAD | ✓ | 10 | `^H\d{2,}$` | "Codigo habilidad" |
| NOMBRE_HABILIDAD | ✓ | — | — | "Nombre de habilidad" |

**HABILIDAD_POSTULANTE:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NIVEL_DESTREZA | ✓ | "Nivel de destreza" |

**CERTIFICACION:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_CERTIFICACION | ✓ | 10 | `^C\d{3,}$` | "Codigo certificacion" |
| NOMBRE_CERTIFICACION | ✓ | — | — | "Nombre de certificacion" |
| FECHA_CERTIFICACION | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha de certificacion" |
| FECHA_INICIO | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha inicio" |
| FECHA_FIN | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha fin" |

**TIPO_CERTIFICACION:**

| Columna | Requerido | FriendlyName |
|---------|:---------:|-------------|
| NOMBRE_TIPO | ✓ | "Nombre de tipo" |

**EXPERIENCIA_LABORAL:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_EXPERIENCIA | ✓ | 10 | `^EL\d{2,}$` | "Codigo experiencia" |
| PUESTO_TRABAJO | ✓ | — | — | "Puesto de trabajo" |
| FECHA_INICIO | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha de inicio" |
| FECHA_FIN | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha de fin" |
| DESCP_EXPERIENCIA_LABORAL | ✓ | — | — | "Descripcion de experiencia" |
| CONTACTO_REFERENCIA | ✓ | — | — | "Contacto de referencia" |

**FORMACION_ACADEMICA:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_FORMACION | ✓ | 10 | `^FOA\d{3,}$` | "Codigo formacion" |
| TITULO_OBTENIDO | ✓ | — | — | "Titulo obtenido" |
| FECHA_INICIO | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha inicio" |
| FECHA_FIN | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha fin" |
| FECHA_OBTENCION | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha de obtencion" |

**OFERTA_ACADEMICA:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_OFERTA_ACADEMICA | ✓ | 10 | `^OFA\d{2,}$` | "Codigo oferta academica" |

**POSTULACION:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_POSTULACION | ✓ | 10 | `^POS\d{3,}$` | "Codigo postulacion" |
| FECHA_APLICACION | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | "Fecha de aplicacion" |
| ESTADO_PROCESO | ✓ | — | — | "Estado del proceso" |

**DETALLE_REQUISITO:**

| Columna | Requerido | MaxLength | Pattern | FriendlyName |
|---------|:---------:|:---------:|---------|-------------|
| ID_DETALLE | ✓ | 10 | `^D\d{1,}$` | "Codigo detalle" |
| DESCRIPCION_REQUISITO | ✓ | — | — | "Descripcion del requisito" |

**RED_SOCIAL_POSTULANTE:**

| Columna | Requerido | Pattern | FriendlyName |
|---------|:---------:|---------|-------------|
| URL_PERFIL | ✓ | `^https?://.*` | "URL del perfil" |

#### 5.3.3 Patrones Regex por Código

Un aspecto crítico de la validación son los patrones de código que identifican de forma única cada registro:

| Tabla | Columna | Patrón | Ejemplo Válido | Ejemplo Inválido |
|-------|---------|--------|----------------|------------------|
| POSTULANTE | `ID_POSTULANTE` | `^[A-Z]{2}\d{5}$` | AB12345 | ab12345, ABC1234 |
| INSTITUCION | `ID_INSTITUCION` | `^INS\d{3,}$` | INS001 | INS01, 001INS |
| HABILIDAD | `ID_HABILIDAD` | `^H\d{2,}$` | H01, H999 | H1, HAB01 |
| OFERTA_TRABAJO | `ID_OFERTA` | `^OF\d{2,}$` | OF01, OF999 | OF1, OFERTA01 |
| CERTIFICACION | `ID_CERTIFICACION` | `^C\d{3,}$` | C001 | C1, C01 |
| EXPERIENCIA_LABORAL | `ID_EXPERIENCIA` | `^EL\d{2,}$` | EL01 | EL1, EXP01 |
| FORMACION_ACADEMICA | `ID_FORMACION` | `^FOA\d{3,}$` | FOA001 | FOA01, FORM001 |
| OFERTA_ACADEMICA | `ID_OFERTA_ACADEMICA` | `^OFA\d{2,}$` | OFA01 | OFA1, OFERTA01 |
| POSTULACION | `ID_POSTULACION` | `^POS\d{3,}$` | POS001 | POS01, POST01 |
| DETALLE_REQUISITO | `ID_DETALLE` | `^D\d{1,}$` | D1, D999 | DD1, DET1 |
| EMPRESA | `NIT` | `^\d{14}$` | 06141234560101 | 0614-123456-010-1 |
| POSTULANTE | `EMAIL` | `^[^@]+@[^@]+\.[^@]+$` | user@domain.com | user@.com |
| RED_SOCIAL_POSTULANTE | `URL_PERFIL` | `^https?://.*` | https://linkedin.com/in/... | linkedin.com/in/... |
| EMPRESA | `CONTACTO_DIRECTO` | `^\d{4}-\d{4}$` | 2200-0001 | 22000001 |
| Fechas | `*FECHA*` | `^\d{4}-\d{2}-\d{2}$` | 2024-01-15 | 15-01-2024 |

#### 5.3.4 Validación de Fechas Futuras

Además del patrón regex, el método `validate()` verifica que ciertas fechas no sean futuras comparando contra `date('now')` en UTC:

| Tabla | Columna | Validación |
|-------|---------|------------|
| CERTIFICACION | FECHA_CERTIFICACION | No futura |
| CERTIFICACION | FECHA_INICIO | No futura |
| CERTIFICACION | FECHA_FIN | No futura |
| EXPERIENCIA_LABORAL | FECHA_FIN | No futura |
| POSTULACION | FECHA_APLICACION | No futura |
| OFERTA_TRABAJO | FECHA_PUBLICACION | No futura |
| FORMACION_ACADEMICA | FECHA_OBTENCION | No futura |
| FORMACION_ACADEMICA | FECHA_INICIO | No futura |
| FORMACION_ACADEMICA | FECHA_FIN | No futura |

#### 5.3.5 Validación de Rango Numérico

| Tabla | Columna | Mínimo | Máximo |
|-------|---------|:------:|:------:|
| MUNICIPIO | ID_MUNICIPIO | 1 | 9999 |
| DISTRITO | ID_DISTRITO | 1 | 9999 |
| OFERTA_TRABAJO | EDAD_MINIMA | 18 | 100 |
| OFERTA_TRABAJO | EDAD_MAXIMA | 16 | 100 |

---

### 5.4 CAPA 3 — EditorDialogFragment (Validaciones Avanzadas)

Además de las validaciones de campo individual, el `EditorDialogFragment.saveData()` implementa validaciones que involucran **múltiples campos** y **reglas de negocio**:

#### 5.4.1 Control de Permisos

Antes de cualquier operación de guardado, se verifica:

```kotlin
val role = obtener_rol_de_sharedpreferences()
val access = Constants.getRoleTables(role)[tableName]
if (access != AccessLevel.FULL) {
    mostrar_error("No tienes permiso para modificar esta tabla")
    return
}
```

Y para POSTULACION en modo edición:
```kotlin
if (role == ROLE_POSTULANTE && tableName == "POSTULACION" && isEditMode) {
    mostrar_error("Solo la empresa puede modificar esta postulación")
    return
}
```

#### 5.4.2 Validación de Periodo de Fechas (Certificación y Formación)

Antes de guardar, se ejecuta `validatePeriodDates()` para CERTIFICACION y FORMACION_ACADEMICA:

```kotlin
fun validatePeriodDates(inicioIdx, finIdx, fechaRefIdx, fechaRefName):
    val fechaInicio = values[inicioIdx]
    val fechaFin = values[finIdx]
    val fechaRef = values[fechaRefIdx]
    
    // 1. inicio < fin
    if (!fechaInicio.before(fechaFin)) {
        error("Fecha inicio debe ser menor a fecha fin")
    }
    // 2. fechaRef >= fin (certificación/obtención no antes de terminar)
    if (fechaRef.before(fechaFin)) {
        error("$fechaRefName no puede ser menor a la fecha fin del periodo")
    }
    // 3. fechaRef <= fin + 1 año
    val cal = Calendar.getInstance()
    cal.time = fechaFin
    cal.add(Calendar.YEAR, 1)
    if (fechaRef.after(cal.time)) {
        error("$fechaRefName no puede exceder un año después de la fecha fin del periodo")
    }
```

#### 5.4.3 Validación de FK Dropdowns

Para cada campo FK, se verifica:
1. Que la tabla padre tenga datos (`options.isEmpty()` → error).
2. Que se haya seleccionado un valor (`selectedText.isBlank()` → error).
3. Que el valor seleccionado exista realmente en la tabla padre (`selectedOption == null` → error).

#### 5.4.4 Mapeo de Valores de Dropdown

| Campo | Valor UI | Valor BD |
|-------|----------|----------|
| `NIVEL_DESTREZA` | "Básico" → "Básico", "Intermedio" → "Intermedio", "Avanzado" → "Avanzado" |
| `ESTADO_PROCESO` | "Activo" → "activo", "En Proceso" → "en proceso", "Contratado" → "contratado", "Rechazado" → "rechazado" |
| `ROL` | "postulante" → "postulante", "gerente de empresa" → "gerente de empresa", "administrador" → "administrador" |

#### 5.4.5 Bloqueo de PKs con Hijos

Si un registro tiene dependencias (hijos), las columnas de la clave primaria se bloquean en modo edición (`isEnabled = false`) para evitar que se cambie la identidad del registro.

#### 5.4.6 Normalización de Texto

Antes de enviar al repositorio, los valores textuales se convierten a minúsculas. Esto aplica a los siguientes campos:

```
NOMBRE_CATEGORIA, NOMBRE_GENERO, NOMBRE_TIPO, NOMBRE_DEPARTAMENTO,
NOMBRE_MUNICIPIO, NOMBRE_DISTRITO, NOMBRE_INSTITUCION, NOMBRE_GRADO,
NOMBRE_RED, NOMBRE_HABILIDAD, NOMBRE_EMPRESA, CONTACTO_DIRECTO,
NOMBRE, APELLIDO, DIRECCION_DETALLE, EMAIL,
TITULO_PUESTO, DESCRIPCION_OFERTA_TRABAJO, DESCRIPCION_REQUISITO,
NOMBRE_CERTIFICACION, PUESTO_TRABAJO, DESCP_EXPERIENCIA_LABORAL,
CONTACTO_REFERENCIA, TITULO_OBTENIDO, ESTADO_PROCESO, URL_PERFIL,
USERNAME, ROL
```

---

### 5.5 CAPA 4 — MainRepository (Duplicados y Persistencia)

#### 5.5.1 Verificación de Duplicados (checkDuplicateInsert/Update)

El repositorio verifica unicidad **antes** de cada INSERT/UPDATE usando reglas específicas por tabla definidas en `getDuplicateCheckFields()`. Estas reglas verifican que no exista otro registro con los mismos valores únicos:

| Tabla | Regla de duplicado | Mensaje de error |
|-------|--------------------|------------------|
| GENERO | `LOWER(NOMBRE_GENERO)` | "Ya existe un genero con ese nombre" |
| CATEGORIA_HABILIDAD | `LOWER(NOMBRE_CATEGORIA)` | "Ya existe una categoria con ese nombre" |
| TIPO_DOCUMENTO | `LOWER(NOMBRE_TIPO)` | "Ya existe un tipo de documento con ese nombre" |
| DEPARTAMENTO | `LOWER(NOMBRE_DEPARTAMENTO)` | "Ya existe un departamento con ese nombre" |
| GRADO_ACADEMICO | `LOWER(NOMBRE_GRADO)` | "Ya existe un grado academico con ese nombre" |
| RED_SOCIAL | `LOWER(NOMBRE_RED)` | "Ya existe una red social con ese nombre" |
| TIPO_CERTIFICACION | `LOWER(NOMBRE_TIPO)` | "Ya existe un tipo de certificacion con ese nombre" |
| INSTITUCION | `LOWER(NOMBRE_INSTITUCION)` | "Ya existe una institucion con ese nombre" |
| MUNICIPIO | `ID_DEPARTAMENTO + LOWER(NOMBRE_MUNICIPIO)` | "Ya existe un municipio con ese nombre en el departamento" |
| DISTRITO | `ID_DEPARTAMENTO + ID_MUNICIPIO + LOWER(NOMBRE_DISTRITO)` | "Ya existe un distrito con ese nombre en el municipio" |
| HABILIDAD | `LOWER(NOMBRE_HABILIDAD)` | "Ya existe una habilidad con ese nombre" |
| EMPRESA | `NIT` **o** `LOWER(NOMBRE_EMPRESA)` | "Ya existe una empresa con ese NIT/nombre" |
| POSTULANTE | `NUM_DOCUMENTO` **o** `NUP` **o** `LOWER(EMAIL)` | "Ya existe un postulante con ese documento/NUP/email" |
| USUARIO | `LOWER(USERNAME)` | "Ya existe un usuario con ese nombre" |
| OFERTA_TRABAJO | `NIT + LOWER(TITULO_PUESTO)` | "Ya existe una oferta con ese titulo en la empresa" |
| DETALLE_REQUISITO | `NIT + ID_OFERTA + LOWER(DESCRIPCION_REQUISITO)` | "Ya existe un requisito con esa descripcion en la oferta" |
| EXPERIENCIA_LABORAL | `ID_POSTULANTE + NIT + LOWER(PUESTO_TRABAJO)` | "Ya existe una experiencia con ese puesto para el postulante" |
| FORMACION_ACADEMICA | `ID_POSTULANTE + TITULO + FECHA_INICIO + FECHA_FIN` | "Ya existe una formacion academica con ese titulo en el mismo periodo" |
| CERTIFICACION | `ID_POSTULANTE + LOWER(NOMBRE_CERTIFICACION)` | "Ya existe una certificacion con ese nombre para el postulante" |
| POSTULACION | `ID_POSTULANTE + NIT + ID_OFERTA` | "El postulante ya aplico a esta oferta" |
| RED_SOCIAL_POSTULANTE | `ID_POSTULANTE + ID_RED_SOCIAL` | "La red social ya esta vinculada al postulante" |
| HABILIDAD_POSTULANTE | `CATEGORIA + ID_HABILIDAD + ID_POSTULANTE` | "La habilidad ya esta asignada al postulante" |
| OFERTA_ACADEMICA | `ID_INSTITUCION + ID_GRADO_ACADEMICO` | "Ya existe una oferta academica para esa institucion y grado" |

**Mecanismo de placeholder:** Las reglas usan `{NOMBRE_COLUMNA}` como placeholders que se reemplazan con los valores reales antes de ejecutar la consulta SQL:
```
"LOWER(NOMBRE_GENERO) = LOWER('{NOMBRE_GENERO}')" 
→ "LOWER(NOMBRE_GENERO) = LOWER('femenino')"
```

#### 5.5.2 Verificación de PK Compuesta (para tablas sin AUTOINCREMENT)

Para tablas con PK manual (sin columna autoincremental), adicionalmente se verifica que la clave primaria completa no exista ya:
```kotlin
if (idCol == null) { // Sin AUTOINCREMENT
    val pkCols = getPrimaryKeyColumns(tableName)
    val whereClause = pkCols.map { "$pk = '${cols[pk]}'" }.joinToString(" AND ")
    val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName WHERE $whereClause")
    if (cursor > 0) throw Exception("Duplicado: Ya existe un registro con esa clave")
}
```

#### 5.5.3 Hash de Contraseñas (USUARIO)

En `insertRecord()`, cuando `tableName == "USUARIO"`:
```kotlin
if (tableName == "USUARIO") {
    val pwdIndex = columns.indexOf("PASSWORD")
    val plainPassword = finalValues[pwdIndex].toString()
    if (plainPassword.isNotBlank() && plainPassword.length < 50) {
        finalValues[pwdIndex] = PasswordHasher.hash(plainPassword)  // PBKDF2
    }
}
```
Las contraseñas se hashean con PBKDF2. Si la contraseña tiene más de 50 caracteres, se asume que ya está hasheada (por ejemplo, en una operación de seed data).

#### 5.5.4 Normalización a Minúsculas

En `insertRecord()` y `updateRecord()`, todos los campos textuales se convierten a minúsculas para garantizar búsquedas case-insensitive:
```kotlin
val processedValues = finalValues.mapIndexed { i, v ->
    val col = columns.getOrNull(i) ?: ""
    when {
        col in lowerFields -> v.toString().lowercase()  // 26 campos
        else -> v
    }
}
```

---

### 5.6 Validaciones de Autenticación (Login + Register)

#### 5.6.1 LoginFragment

| Validación | Código | Mensaje |
|------------|--------|---------|
| Username no vacío | `username.isBlank()` → error | tilUsername.error = "Este campo es requerido" |
| Password no vacío | `password.isBlank()` → error | tilPassword.error = "Este campo es requerido" |
| Credenciales válidas | `repository.login()` retorna null | Toast: "Usuario o contraseña incorrectos" |

#### 5.6.2 RegisterFragment

| Validación | Código | Mensaje |
|------------|--------|---------|
| Username no vacío | `username.isBlank()` → error | tilUsername.error = "Este campo es requerido" |
| Password no vacío | `password.isBlank()` → error | tilPassword.error = "Este campo es requerido" |
| Password >= 8 caracteres | `password.length < 8` → error | "La contraseña debe tener al menos 8 caracteres" |
| Confirmación no vacía | `confirmPassword.isBlank()` → error | tilConfirmPassword.error = "Este campo es requerido" |
| Passwords coinciden | `password != confirmPassword` → error | "Las contraseñas no coinciden" |
| Username único (servidor) | repository.register retorna -2 | "El nombre de usuario ya existe" |
| Registro exitoso | repository.register retorna > 0 | Toast: "Registro exitoso" y navega a Login |

#### 5.6.3 Manejo de Sesión (SharedPreferences)

Tras un login exitoso, se persisten 4 valores en `SharedPreferences`:

| Clave | Tipo | Valor |
|-------|------|-------|
| `is_logged_in` | Boolean | true |
| `user_id` | Int | ID del usuario |
| `username` | String | Nombre de usuario (minúsculas) |
| `user_role` | String | Rol: "administrador", "postulante", "gerente de empresa" |

---

### 5.7 Traducción de Errores (TriggerErrorTranslator)

El archivo `TriggerErrorTranslator.kt` contiene un mapa de más de 50 reglas que intercepta los mensajes de error lanzados por:

1. **Triggers de la BD** (errores semánticos y de integridad referencial)
2. **Restricciones UNIQUE** de SQLite
3. **Restricciones FOREIGN KEY** de SQLite
4. **Restricciones NOT NULL** de SQLite
5. **Errores personalizados** del repositorio (duplicados, validaciones)

El método `translate(errorMessage: String?): String` busca coincidencias parciales (case-insensitive) en el mensaje de error y retorna la versión amigable. Si no encuentra coincidencia, retorna el mensaje original.

**Ejemplos de traducción:**

| Error original | Traducción |
|----------------|------------|
| `"UNIQUE constraint failed: USUARIO.USERNAME"` | "Ya existe un registro con esos datos" |
| `"FOREIGN KEY constraint failed"` | "No se puede modificar: tiene registros asociados. Elimine primero los registros dependientes" |
| `"NOT NULL constraint failed"` | "Un campo obligatorio esta vacio" |
| `"El postulante debe ser mayor de edad"` | "El postulante debe ser mayor de 18 años" |
| `"grado academico superior"` | "El postulante debe tener un grado academico superior a Bachiller" |
| `"No se puede eliminar: el postulante tiene postulaciones"` | "Elimine primero las postulaciones de este postulante" |

---

### 5.8 Matriz Completa de Validación por Pantalla/Tabla

A continuación se presenta la matriz completa que resume TODAS las validaciones que se aplican a cada tabla/pantalla en las 4 capas:

| Pantalla/Tabla | CAPA 1: UI Controls | CAPA 2: ValidationRules | CAPA 3: Editor Avanzado | CAPA 4: Repositorio |
|----------------|---------------------|------------------------|------------------------|---------------------|
| **LoginFragment** | — | — | — | Auth (PBKDF2 verify) |
| **RegisterFragment** | Rol dropdown fijo | USERNAME req + min3, PASSWORD req+min8 | Confirmación password match | Username duplicado |
| **DashboardFragment** | — | — | — | — |
| **CATEGORIA_HABILIDAD** | TextInput | NOMBRE_CATEGORIA req | — | Duplicado nombre |
| **GENERO** | TextInput | NOMBRE_GENERO req | — | Duplicado nombre |
| **TIPO_DOCUMENTO** | TextInput | NOMBRE_TIPO req | — | Duplicado nombre |
| **DEPARTAMENTO** | TextInput | NOMBRE_DEPARTAMENTO req | — | Duplicado nombre |
| **GRADO_ACADEMICO** | TextInput | NOMBRE_GRADO req | — | Duplicado nombre |
| **RED_SOCIAL** | TextInput | NOMBRE_RED req | — | Duplicado nombre |
| **TIPO_CERTIFICACION** | TextInput | NOMBRE_TIPO req | — | Duplicado nombre |
| **USUARIO** | ROL dropdown, PASSWORD oculta | USERNAME req+min3, PASSWORD req+min8, ROL req | — | Username duplicado, PBKDF2 hash |
| **MUNICIPIO** | ID_MUNICIPIO numérico, dropdown DEPARTAMENTO (cascading) | ID_MUNICIPIO req (1-9999), NOMBRE_MUNICIPIO req | FK dropdown obligatorio | Duplicado nombre por depto |
| **DISTRITO** | ID_DISTRITO numérico, dropdowns DEPARTAMENTO→MUNICIPIO (cascading) | ID_DISTRITO req (1-9999), NOMBRE_DISTRITO req | FK cascading obligatorio | Duplicado nombre por municipio |
| **INSTITUCION** | ID_INSTITUCION con helper "Ej: INS001" | ID_INSTITUCION req+pattern+max20, NOMBRE_INSTITUCION req | — | Duplicado nombre |
| **HABILIDAD** | ID_HABILIDAD con helper "Ej: H01", dropdown CATEGORIA | ID_CATEGORIA req, ID_HABILIDAD req+pattern+max10, NOMBRE_HABILIDAD req | FK dropdown obligatorio | Duplicado nombre |
| **EMPRESA** | NIT solo dígitos (14), CONTACTO_DIRECTO 0000-0000, dropdowns DISTRITO (cascading) | NIT req+pattern+max14, NOMBRE_EMPRESA req, CONTACTO_DIRECTO req+pattern | FK cascading obligatorio | Duplicado NIT o nombre |
| **POSTULANTE** | ID_POSTULANTE helper "Ej: AB12345", NUP solo dígitos, DUI/NIT dinámico según tipo doc, dropdowns GÉNERO/TIPO_DOC/DISTRITO/GRADO | ID_POSTULANTE req+pattern+max7, NOMBRE req, APELLIDO req, EMAIL req+pattern, FECHA_NACIMIENTO req+pattern, 12 campos req total | FK dropdowns obligatorios, tipo documento dinámico | Duplicado documento, NUP o email; triggers edad+grado |
| **OFERTA_ACADEMICA** | ID_OFERTA_ACADEMICA helper "Ej: OFA01", dropdowns INSTITUCION/GRADO | ID_OFERTA_ACADEMICA req+pattern | FK dropdowns obligatorios | Duplicado institución+grado |
| **OFERTA_TRABAJO** | ID_OFERTA helper "Ej: OF001", EXPERIENCIA/EDAD solo dígitos, fechas con DatePicker | ID_OFERTA req+pattern+max10, TITULO req, FECHAS req+pattern, EDAD (18-100)/(16-100), DESCRIPCION req | Fechas no futuras, triggers edad+vigencia | Duplicado título por empresa |
| **DETALLE_REQUISITO** | ID_DETALLE helper "Ej: D1", dropdown EMPRESA→OFERTA (cascading) | ID_DETALLE req+pattern+max10, DESCRIPCION_REQUISITO req | FK cascading obligatorio | Duplicado descripción por oferta |
| **EXPERIENCIA_LABORAL** | ID_EXPERIENCIA helper "Ej: EL01", fechas DatePicker | ID_EXPERIENCIA req+pattern+max10, PUESTO req, FECHAS req+pattern, DESCRIPCION req, CONTACTO req | Periodo fechas (inicio<fin), fecha_fin no futura | Duplicado puesto por postulante+empresa |
| **CERTIFICACION** | ID_CERTIFICACION helper "Ej: C001", fechas DatePicker, dropdown TIPO_CERT | ID_CERTIFICACION req+pattern+max10, NOMBRE req, FECHAS req+pattern | Periodo fechas (6 validaciones), fechas no futuras | Duplicado nombre por postulante |
| **FORMACION_ACADEMICA** | ID_FORMACION helper "Ej: FOA001", fechas DatePicker, dropdown OFERTA_ACAD | ID_FORMACION req+pattern+max10, TITULO req, FECHAS req+pattern | Periodo fechas (6 validaciones), fechas no futuras | Duplicado título por mismo período |
| **HABILIDAD_POSTULANTE** | NIVEL_DESTREZA dropdown fijo, dropdowns CATEGORIA→HABILIDAD (cascading) | NIVEL_DESTREZA req | FK cascading obligatorio, nivel seleccionado | Duplicado habilidad+postulante |
| **POSTULACION** | ID_POSTULACION helper "Ej: POS001", ESTADO_PROCESO dropdown, dropdown NIT→OFERTA (solo vigentes), fecha DatePicker | ID_POSTULACION req+pattern+max10, FECHA_APLICACION req+pattern, ESTADO_PROCESO req | FK cascading obligatorio, permiso por rol (postulante no edita, empresa solo estado), ofertas vigentes | Duplicado postulante+oferta |
| **RED_SOCIAL_POSTULANTE** | URL_PERFIL con helper "Ej: https://", dropdowns POSTULANTE/RED_SOCIAL | URL_PERFIL req+pattern | FK dropdowns obligatorios | Duplicado postulante+red |

## 6. Pantallas

### 6.1 Flujo de Navegación General

La aplicación consta de **6 pantallas principales** que cubren todas las operaciones sobre las 23 tablas. El flujo de navegación se define en `res/navigation/nav_graph.xml`:

```
┌──────────────────────────────────────────────────────────┐
│                     LoginFragment                        │
│                   (Autenticación)                        │
│                                                          │
│   ┌─────────────────────┐    ┌───────────────────────┐   │
│   │ "Iniciar Sesión"    │    │ "Registrarse"         │   │
│   │ → DashboardFragment │    │ → RegisterFragment    │   │
│   └─────────────────────┘    └───────────────────────┘   │
│                                                          │
│   ┌─────────────────────┐                                │
│   │ "Insertar datos"    │                                │
│   │ → SeedData (modal)  │                                │
│   └─────────────────────┘                                │
└──────────────────────────┬───────────────────────────────┘
                           │ Login exitoso
                           ▼
┌──────────────────────────────────────────────────────────┐
│                   DashboardFragment                       │
│               (Panel de tablas por sección)               │
│                                                          │
│   ┌──────────────┐                                       │
│   │ CATÁLOGOS    │ → 13 tablas (expandible)              │
│   │ EMPRESA      │ → 3 tablas (expandible)               │
│   │ POSTULANTE   │ → 7 tablas (expandible)               │
│   └──────┬───────┘                                       │
│          │ Al hacer clic en una tarjeta                  │
│          ▼                                               │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│                  TableDetailFragment                      │
│            (Listado de registros de una tabla)            │
│                                                          │
│   ┌────────────────────────────┐                         │
│   │ Barra de búsqueda          │                         │
│   │ (filtra en tiempo real)    │                         │
│   ├────────────────────────────┤                         │
│   │ RecyclerView con TableAdapter │                      │
│   │ (display específico por tabla) │                     │
│   ├────────────────────────────┤                         │
│   │ FAB "+" → EditorDialog     │                         │
│   │ Editar → EditorDialog      │                         │
│   │ Eliminar → DeleteConfirm   │                         │
│   │ Exportar → PDF (solo POST) │                         │
│   └────────────────────────────┘                         │
│                    │                │                    │
│           ┌────────┘                └────────┐           │
│           ▼                                    ▼         │
│   ┌──────────────┐                   ┌──────────────────┐ │
│   │ EditorDialog │                   │ DeleteConfirm    │ │
│   │ (Crear/Editar)│                  │ (Eliminar)       │ │
│   └──────────────┘                   └──────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

### 6.2 LoginFragment

**Archivo:** `ui/auth/LoginFragment.kt` (97 líneas)
**Layout:** `res/layout/fragment_login.xml` (112 líneas)
**ViewModel:** `AuthViewModel.kt`
**Tabla asociada:** USUARIO (solo lectura)

#### Elementos de la UI

| Elemento | Tipo | Propósito |
|----------|------|-----------|
| `logo_bt` | ImageView | Logo de la aplicación (PNG) |
| `etUsername` | TextInputEditText | Campo de nombre de usuario |
| `tilUsername` | TextInputLayout | Contenedor con hint y error |
| `etPassword` | TextInputEditText | Campo de contraseña (oculta) |
| `tilPassword` | TextInputLayout | Contenedor con hint y error |
| `btnLogin` | MaterialButton | Botón "Iniciar Sesión" |
| `btnRegister` | MaterialButton | Botón "Registrarse" → RegisterFragment |
| `btnThemeToggle` | ImageButton | Toggle de tema claro/oscuro |
| `btnSeedData` | MaterialButton | Botón "Insertar datos de prueba" (solo visible para admin logueado o en primera ejecución) |

#### Flujo de Operación

1. Usuario ingresa username y password
2. Al presionar "Iniciar Sesión":
   - `validateInput()` verifica que ambos campos no estén en blanco
   - Si pasa validación, llama a `viewModel.login(username, password)`
3. `MainRepository.login()`:
   - Busca el username en USUARIO (`SELECT * FROM USUARIO WHERE USERNAME = ?`)
   - Verifica el hash con `PasswordHasher.verify(password, storedHash)`
   - Si es correcto, retorna un objeto `Usuario(id, username, password, rol)`
4. Si login exitoso:
   - Guarda sesión en SharedPreferences (`is_logged_in`, `user_id`, `username`, `user_role`)
   - Navega a `DashboardFragment` (con `popUpToInclusive=true` para no poder volver atrás)
5. Si login falla:
   - Muestra Toast "Usuario o contraseña incorrectos"

#### Acciones Disponibles

| Acción | Destino | Condición |
|--------|---------|-----------|
| Iniciar Sesión | DashboardFragment | Username + password válidos |
| Registrarse | RegisterFragment | Siempre disponible |
| Toggle tema | — | Siempre disponible |
| Insertar datos de prueba | SeedData | Siempre disponible (verifica duplicados internamente) |

---

### 6.3 RegisterFragment

**Archivo:** `ui/auth/RegisterFragment.kt` (119 líneas)
**Layout:** `res/layout/fragment_register.xml` (150 líneas)
**ViewModel:** `AuthViewModel.kt`
**Tabla asociada:** USUARIO (solo inserción)

#### Elementos de la UI

| Elemento | Tipo | Propósito |
|----------|------|-----------|
| `etUsername` | TextInputEditText | Nombre de usuario |
| `etPassword` | TextInputEditText | Contraseña (mínimo 8 caracteres) |
| `etConfirmPassword` | TextInputEditText | Confirmación de contraseña |
| `actvRol` | MaterialAutoCompleteTextView | Dropdown de rol (pre-seleccionado: postulante) |
| `btnRegister` | MaterialButton | Botón "Registrarse" |
| `btnLogin` | MaterialButton | Botón "Volver al inicio de sesión" |
| `btnThemeToggle` | ImageButton | Toggle de tema |

#### Flujo de Operación

1. Usuario completa el formulario (username, password, confirmar, rol)
2. Al presionar "Registrarse":
   - `validateInput()` verifica: campos no vacíos, password ≥ 8, passwords coinciden
   - Si pasa, llama a `viewModel.register(username, password, rol)`
3. `MainRepository.register()`:
   - Verifica si el username ya existe (retorna -2 si existe)
   - Hashea la contraseña con PBKDF2
   - Inserta en USUARIO con `INSERT INTO USUARIO (USERNAME, PASSWORD, ROL) VALUES (...)`
   - Retorna el nuevo ID_USUARIO
4. Si registro exitoso: Toast "Registro exitoso" y navega a LoginFragment
5. Si username existe: muestra error "El nombre de usuario ya existe"

#### Roles Disponibles en el Dropdown

| Valor | Descripción |
|-------|-------------|
| postulante | **(seleccionado por defecto)** Buscador de empleo |
| gerente de empresa | Reclutador |
| administrador | Acceso total |

**Nota:** Aunque el dropdown permite seleccionar cualquier rol, en la práctica el registro público debería usar solo "postulante". Los roles administrativos son asignados por un administrador existente.

---

### 6.4 DashboardFragment

**Archivo:** `ui/dashboard/DashboardFragment.kt` (167 líneas)
**Layout:** `res/layout/fragment_dashboard.xml` (82 líneas)
**ViewModel:** `DashboardViewModel.kt`
**Adaptador:** `DashboardAdapter.kt` (101 líneas)
**Tablas asociadas:** Todas las 23 tablas (filtradas por rol)

#### Elementos de la UI

| Elemento | Tipo | Propósito |
|----------|------|-----------|
| `toolbar` | MaterialToolbar | Título "Dashboard" + menú de cerrar sesión |
| `searchView` | SearchView | Búsqueda de tablas por nombre |
| `recyclerTables` | RecyclerView | Lista de secciones con tarjetas de tablas |
| `btnThemeToggle` | ImageButton | Toggle de tema |

#### Secciones del Dashboard

El dashboard organiza las tablas en **3 secciones expandibles** según el tipo de datos, más una sección "OTRAS" para tablas no clasificadas:

| Sección | Tablas incluidas | Orden de visualización |
|---------|------------------|------------------------|
| **CATÁLOGOS** | DEPARTAMENTO, MUNICIPIO, DISTRITO, GENERO, TIPO_DOCUMENTO, GRADO_ACADEMICO, INSTITUCION, OFERTA_ACADEMICA, TIPO_CERTIFICACION, RED_SOCIAL, CATEGORIA_HABILIDAD, HABILIDAD, USUARIO | 13 tablas en orden específico |
| **EMPRESA** | EMPRESA, OFERTA_TRABAJO, DETALLE_REQUISITO | 3 tablas |
| **POSTULANTE** | POSTULANTE, FORMACION_ACADEMICA, CERTIFICACION, EXPERIENCIA_LABORAL, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE, POSTULACION | 7 tablas |

#### Comportamiento por Rol

| Rol | Tablas visibles en Dashboard |
|-----|------------------------------|
| **administrador** | Las 23 tablas (FULL ACCESS en todas) |
| **postulante** | 14 tablas: las 13 catálogos en READ_ONLY + POSTULANTE y sus 6 tablas hijas en FULL (EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, HABILIDAD_POSTULANTE, CERTIFICACION, RED_SOCIAL_POSTULANTE, POSTULACION) + EMPRESA, OFERTA_TRABAJO, DETALLE_REQUISITO en READ_ONLY |
| **gerente de empresa** | 14 tablas: las 13 catálogos en READ_ONLY + EMPRESA y OFERTA_TRABAJO/DETALLE_REQUISITO en FULL + POSTULANTE y sus 5 tablas hijas (no POSTULACION) en READ_ONLY |

#### Representación Visual de Cada Tarjeta

Cada tarjeta de tabla en el dashboard (`item_table_card.xml`) muestra:
- **Nombre de la tabla**: formateado (ej: "CATEGORIA_HABILIDAD" → "Categoria habilidad")
- **Conteo de registros**: número + "registros"
- **Badge de acceso**: si es READ_ONLY, muestra "Solo lectura" en un badge

#### Acciones Disponibles

| Acción | Comportamiento |
|--------|---------------|
| **Clic en tarjeta** | Navega a TableDetailFragment con `tableName` y `tableDisplayName` |
| **Clic en sección** | Expande/colapsa la sección para mostrar/ocultar sus tablas |
| **Búsqueda** | Filtra las tarjetas por nombre de tabla (con remoción de acentos) |
| **Toggle tema** | Cambia entre tema claro y oscuro |
| **Cerrar sesión** | Confirma y navega a LoginFragment (limpia SharedPreferences) |

---

### 6.5 TableDetailFragment

**Archivo:** `ui/crud/TableDetailFragment.kt` (460 líneas)
**Layout:** `res/layout/fragment_table_detail.xml` (95 líneas)
**ViewModel:** `CrudViewModel.kt`
**Adaptador:** `TableAdapter.kt` (231 líneas)
**Tablas asociadas:** Las 23 tablas (genérico con comportamiento específico por tabla)

Este es el fragmento central del CRUD. Recibe el nombre de la tabla como argumento y se comporta de manera genérica, con personalizaciones para tablas específicas.

#### Elementos de la UI

| Elemento | Tipo | Propósito |
|----------|------|-----------|
| `toolbar` | MaterialToolbar | Título = tableDisplayName, botón de retroceso |
| `searchView` | SearchView | Búsqueda en los registros (con debounce de 300ms) |
| `recyclerItems` | RecyclerView | Listado de registros con TableAdapter |
| `fabAdd` | FAB | Agregar nuevo registro → EditorDialogFragment |
| `fabExport` | FAB | Exportar PDFs — solo visible en POSTULACION |
| `progressBar` | ProgressBar | Indicador de carga mientras se obtienen datos |
| `btnThemeToggle` | ImageButton | Toggle de tema |

#### Comportamiento por Tabla

A continuación se detalla el comportamiento específico de TableDetailFragment para cada tabla:

##### Tablas con Display Personalizado en TableAdapter

El `TableAdapter` tiene lógica de renderizado específica para 15 tablas. Las 8 tablas restantes usan el renderizado genérico (columna 0 = ID, columna 1 = nombre, columna 2 = descripción).

| Tabla | Columnas mostradas | Display ID | Display Primary | Display Secondary |
|-------|-------------------|------------|-----------------|-------------------|
| **POSTULANTE** | [0]=ID, [8]=Nombre, [9]=Apellido | ID_POSTULANTE | NOMBRE | APELLIDO |
| **EXPERIENCIA_LABORAL** | [0-2]=PK, [3]=Puesto, [8-9]=Nombre | (ID_POST, NIT, ID_EXP) | "Nombre Apellido" | PUESTO_TRABAJO |
| **HABILIDAD_POSTULANTE** | [0-2]=PK, [3]=Nivel, [4-5]=Nombre, [6]=Habilidad | (CAT, HAB, POST) | "Nombre Apellido" | "Habilidad • Nivel" |
| **POSTULACION** | [0]=ID, [1]=NIT, [2]=ID_OFERTA, [3]=ID_POST, [5]=Estado, [6-8]=Nombre+Puesto | ID_POSTULACION | "Nombre Apellido" | TITULO_PUESTO |
| **RED_SOCIAL_POSTULANTE** | [0-1]=PK, [3-4]=Nombre, [5]=Red | (POST, RED) | "Nombre Apellido" | NOMBRE_RED |
| **MUNICIPIO** | [0-1]=PK, [2]=Municipio, [3]=Depto | (ID_DEPTO, ID_MUN) | NOMBRE_DEPARTAMENTO | NOMBRE_MUNICIPIO |
| **DISTRITO** | [0-2]=PK, [3]=Distrito, [4]=Municipio | (ID_DEPTO, ID_MUN, ID_DIST) | NOMBRE_MUNICIPIO | NOMBRE_DISTRITO |
| **HABILIDAD** | [0-1]=PK, [2]=Nombre, [3]=Categoría | (CAT, HAB) | NOMBRE_HABILIDAD | NOMBRE_CATEGORIA |
| **OFERTA_TRABAJO** | [0-1]=PK, [3]=Título, [5]=Caducidad, [10]=Empresa | (NIT, ID_OF) | TITULO_PUESTO | NOMBRE_EMPRESA (+ chip VIGENTE/VENCIDA) |
| **DETALLE_REQUISITO** | [0-2]=PK, [3]=Desc, [4]=Título, [6]=Caducidad | (NIT, ID_OF, ID_DET) | DESCRIPCION_REQUISITO | TITULO_PUESTO (+ chip VIGENTE/VENCIDO) |
| **OFERTA_ACADEMICA** | [0]=ID, [3]=Institución, [4]=Grado | ID_OFERTA_ACADEMICA | NOMBRE_GRADO | NOMBRE_INSTITUCION |
| **CERTIFICACION** | [0-2]=PK, [4]=Nombre, [5-6]=Fechas | (ID_CERT, ID_INST, ID_POST) | NOMBRE_CERTIFICACION | "FECHA_INICIO → FECHA_FIN" |
| **FORMACION_ACADEMICA** | [0-1]=PK, [3]=Título, [4-5]=Fechas | (ID_FORM, ID_POST) | TITULO_OBTENIDO | "FECHA_INICIO → FECHA_FIN" |
| **USUARIO** | [0]=ID, [1]=Username, [3]=Rol | ID_USUARIO | USERNAME | ROL |
| **EMPRESA** | [0]=NIT, [4]=Nombre, [5]=Contacto | NIT | NOMBRE_EMPRESA | CONTACTO_DIRECTO |
| **Resto (genérico)** | [0]=col0, [1]=col1, [2]=col2 | col0 (ID) | col1 | col2 |

#### Búsqueda con JOINs

El método `searchTable()` en `MainRepository.kt` realiza consultas JOIN para mostrar datos de tablas relacionadas. Las siguientes tablas tienen consultas JOIN específicas:

| Tabla | JOINs en la búsqueda |
|-------|----------------------|
| **HABILIDAD** | LEFT JOIN CATEGORIA_HABILIDAD para mostrar nombre de categoría |
| **MUNICIPIO** | LEFT JOIN DEPARTAMENTO para mostrar nombre de departamento |
| **DISTRITO** | LEFT JOIN MUNICIPIO para mostrar nombre de municipio |
| **EXPERIENCIA_LABORAL** | LEFT JOIN POSTULANTE para mostrar nombre y apellido |
| **HABILIDAD_POSTULANTE** | LEFT JOIN POSTULANTE (nombre) + LEFT JOIN HABILIDAD (nombre habilidad) |
| **POSTULACION** | LEFT JOIN POSTULANTE (nombre) + LEFT JOIN OFERTA_TRABAJO (título puesto) |
| **RED_SOCIAL_POSTULANTE** | LEFT JOIN POSTULANTE (nombre) + LEFT JOIN RED_SOCIAL (nombre red) |
| **OFERTA_TRABAJO** | LEFT JOIN EMPRESA (nombre empresa) + LEFT JOIN GRADO_ACADEMICO (nombre grado) |
| **DETALLE_REQUISITO** | LEFT JOIN OFERTA_TRABAJO (título puesto) + LEFT JOIN EMPRESA (nombre empresa) |
| **OFERTA_ACADEMICA** | LEFT JOIN INSTITUCION (nombre inst) + LEFT JOIN GRADO_ACADEMICO (nombre grado) |
| **CERTIFICACION** | LEFT JOIN POSTULANTE + LEFT JOIN INSTITUCION + LEFT JOIN TIPO_CERTIFICACION |
| **FORMACION_ACADEMICA** | LEFT JOIN POSTULANTE + LEFT JOIN OFERTA_ACADEMICA |

#### Filtros de Búsqueda Especiales

Además de la búsqueda genérica por cualquier campo, tres tablas tienen filtros de palabras clave:

| Tabla | Palabras clave | Filtro aplicado |
|-------|----------------|-----------------|
| **OFERTA_TRABAJO** | "vigente" / "vencida" | Compara FECHA_CADUCIDAD con fecha actual |
| **DETALLE_REQUISITO** | "vigente" / "vencido" | Compara FECHA_CADUCIDAD con fecha actual |
| **POSTULACION** | "activo" / "en proceso" / "contratado" / "rechazado" | Filtra por ESTADO_PROCESO |

#### Chips de Estado Visual

Dos tablas muestran un chip de estado codificado por color:

| Tabla | Chip verde | Chip rojo | Condición |
|-------|-----------|-----------|-----------|
| **OFERTA_TRABAJO** | "VIGENTE" | "VENCIDA" | `FECHA_CADUCIDAD >= hoy` → vigente |
| **DETALLE_REQUISITO** | "VIGENTE" | "VENCIDO" | `FECHA_CADUCIDAD >= hoy` → vigente |

#### Exportación a PDF

La exportación a PDF es una característica **exclusiva de la tabla POSTULACION**:

1. El usuario selecciona una postulación (clic en la fila)
2. Se habilita el `fabExport` (FAB de exportar)
3. Al presionar exportar:
   - Obtiene `PostulantFullData` (CV completo del postulante)
   - Obtiene `OfertaFullData` (detalle completo de la vacante)
   - Genera dos PDFs usando `CVExportUtil`:
     - `CV_{ID_POSTULANTE}.pdf` — Currículum del postulante
     - `Vacante_{NIT}_{ID_OFERTA}.pdf` — Detalle de la vacante
   - Muestra diálogo con botones "VER CV" y "VER VACANTE"
   - Los PDFs se abren con el visor PDF del dispositivo

#### Acciones Disponibles por Fila

Cada fila en el RecyclerView tiene:

| Acción | Botón | Condición |
|--------|-------|-----------|
| **Editar** | FAB editar (lápiz) | `canEdit == true` (FULL ACCESS según rol) |
| **Eliminar** | FAB eliminar (papelera) | `canDelete == true` (FULL ACCESS según rol) |
| **Ver** | Clic en fila (solo si no puede editar) | `canEdit == false` y `onViewClick != null` |
| **Seleccionar** | Clic en fila (solo POSTULACION) | `onItemSelected != null` → habilita FAB exportar |

---

### 6.6 EditorDialogFragment

**Archivo:** `ui/crud/EditorDialogFragment.kt` (1.610 líneas)
**Layout:** `res/layout/dialog_editor.xml` (53 líneas)
**ViewModel:** `CrudViewModel.kt`
**Tablas asociadas:** Las 23 tablas

Este es el diálogo modal genérico para crear y editar registros de cualquier tabla. Genera dinámicamente los campos según las columnas de la tabla.

#### Modos de Operación

| Modo | Origen | Comportamiento |
|------|--------|----------------|
| **Crear** | FAB "+" en TableDetailFragment | Campos vacíos, todos editables |
| **Editar** | FAB editar en una fila | Campos precargados con datos existentes, PK bloqueadas si tiene hijos |
| **Ver** | Clic en fila (solo lectura) | Todos los campos deshabilitados, botón Guardar oculto |

#### Campos Generados por Tipo

El método `setupFields()` recorre las columnas de la tabla y genera el control UI apropiado según el tipo de columna:

| Tipo de columna | Control generado | Comportamiento |
|-----------------|------------------|----------------|
| **Texto normal** | TextInputEditText | Validación via ValidationRules, InputFilter, InputMask |
| **Fecha** | TextInputEditText (no editable) | DatePicker al hacer clic, rango 1926-año actual |
| **Descripción larga** | TextInputEditText multilínea | maxLines = 5, sin滚动 horizontal |
| **FK simple** | MaterialAutoCompleteTextView | Opciones cargadas desde tabla padre |
| **FK en cascada** | MaterialAutoCompleteTextView | Opciones filtradas según padre seleccionado |
| **NIVEL_DESTREZA** | MaterialAutoCompleteTextView | 3 opciones fijas: Básico, Intermedio, Avanzado |
| **ESTADO_PROCESO** | MaterialAutoCompleteTextView | 4 opciones fijas: Activo, En Proceso, Contratado, Rechazado |
| **ROL** | MaterialAutoCompleteTextView | 3 opciones fijas: postulante, gerente de empresa, administrador |
| **Password (USUARIO)** | TextInputEditText (deshabilitado en edición) | No se puede modificar la contraseña existente |
| **Autoincremental** | No se genera | El ID se calcula automáticamente |

#### Comportamiento Especial por Tabla

| Tabla | Comportamiento especial |
|-------|------------------------|
| **POSTULANTE** | Departamento→Municipio→Distrito en cascada; tipo documento afecta formato de NUM_DOCUMENTO |
| **EMPRESA** | Departamento→Municipio→Distrito en cascada |
| **DISTRITO** | Departamento→Municipio en cascada |
| **POSTULACION** | En modo edición: postulante no edita nada; empresa solo edita ESTADO_PROCESO |
| **CERTIFICACION** | Validación de 6 reglas de fechas al guardar |
| **FORMACION_ACADEMICA** | Validación de 6 reglas de fechas al guardar |
| **USUARIO** | Al editar, si se cambia username/rol del usuario logueado, se actualiza SharedPreferences |
| **HABILIDAD_POSTULANTE** | Categoría→Habilidad en cascada; nivel fijo dropdown |
| **DETALLE_REQUISITO** | Empresa→Oferta en cascada (incluye ofertas vencidas) |

#### Controles de Permiso en el Editor

Antes de guardar, `saveData()` verifica:
1. `Constants.getRoleTables(role)[tableName]` debe ser FULL ACCESS
2. Para POSTULACION en edición: postulante no puede editar (mensaje específico)
3. Para POSTULACION: empresa puede crear una nueva, pero en edición solo modifica estado

---

### 6.7 DeleteConfirmDialog

**Archivo:** `ui/crud/DeleteConfirmDialog.kt` (249 líneas)
**ViewModel:** `CrudViewModel.kt`
**Tablas asociadas:** Todas las 23 tablas

#### Flujo de Operación

1. Se abre un diálogo con título "Eliminar {nombre tabla}"
2. Muestra ProgressBar mientras consulta dependencias (`checkDeleteDependencies`)
3. Según el resultado:

**Caso A: Sin dependencias**
- Muestra mensaje "No posee registros asociados"
- Botones: "Cerrar" (vuelve atrás) y "Eliminar" (rojo, ejecuta borrado)

**Caso B: Con dependencias**
- Muestra mensaje "Posee registros asociados" con la lista de dependencias
- Botones: Solo "Cerrar" (no permite eliminar)

#### Manejo de PK Compuesta

El diálogo detecta si la tabla tiene PK compuesta y construye el identificador correcto:

| Tabla | Columnas PK | Separador |
|-------|-------------|-----------|
| MUNICIPIO | ID_DEPARTAMENTO + ID_MUNICIPIO | `valor1\|valor2` |
| DISTRITO | ID_DEPARTAMENTO + ID_MUNICIPIO + ID_DISTRITO | `v1\|v2\|v3` |
| OFERTA_TRABAJO | NIT + ID_OFERTA | `v1\|v2` |
| DETALLE_REQUISITO | NIT + ID_OFERTA + ID_DETALLE | `v1\|v2\|v3` |
| EXPERIENCIA_LABORAL | ID_POSTULANTE + NIT + ID_EXPERIENCIA | `v1\|v2\|v3` |
| CERTIFICACION | ID_CERTIFICACION + ID_INSTITUCION + ID_POSTULANTE | `v1\|v2\|v3` |
| FORMACION_ACADEMICA | ID_FORMACION + ID_POSTULANTE | `v1\|v2` |
| HABILIDAD_POSTULANTE | ID_CATEGORIA_HABILIDAD + ID_HABILIDAD + ID_POSTULANTE | `v1\|v2\|v3` |
| RED_SOCIAL_POSTULANTE | ID_POSTULANTE + ID_RED_SOCIAL | `v1\|v2` |
| HABILIDAD | ID_CATEGORIA_HABILIDAD + ID_HABILIDAD | `v1\|v2` |
| OFERTA_ACADEMICA | Solo ID_OFERTA_ACADEMICA | Normal |

#### Prevención de Auto-Eliminación

Si la tabla es USUARIO y el ID del registro coincide con el usuario logueado, se muestra el error "No puedes eliminar tu propio usuario mientras está activo" y se cancela la operación.

---

### 6.8 Matriz Completa Pantalla ↔ Tabla

| Pantalla | Tabla(s) | Operaciones | Permisos | Search JOINs | Export |
|----------|----------|-------------|----------|-------------|--------|
| **LoginFragment** | USUARIO | Read (login) | — | — | — |
| **RegisterFragment** | USUARIO | Create (register) | — | — | — |
| **DashboardFragment** | 23 tablas | Leer conteos | Según rol | — | — |
| **TableDetailFragment** | CATEGORIA_HABILIDAD | CRUD | Según rol | No | No |
| **TableDetailFragment** | GENERO | CRUD | Según rol | No | No |
| **TableDetailFragment** | TIPO_DOCUMENTO | CRUD | Según rol | No | No |
| **TableDetailFragment** | DEPARTAMENTO | CRUD | Según rol | No | No |
| **TableDetailFragment** | GRADO_ACADEMICO | CRUD | Según rol | No | No |
| **TableDetailFragment** | RED_SOCIAL | CRUD | Según rol | No | No |
| **TableDetailFragment** | TIPO_CERTIFICACION | CRUD | Según rol | No | No |
| **TableDetailFragment** | USUARIO | CRUD (+prevención auto-eliminar) | FULL solo admin | No | No |
| **TableDetailFragment** | MUNICIPIO | CRUD (cascading Depto) | Según rol | LEFT JOIN DEPARTAMENTO | No |
| **TableDetailFragment** | DISTRITO | CRUD (cascading Depto→Mun) | Según rol | LEFT JOIN MUNICIPIO | No |
| **TableDetailFragment** | INSTITUCION | CRUD | Según rol | No | No |
| **TableDetailFragment** | HABILIDAD | CRUD (cascading Categoría) | Según rol | LEFT JOIN CATEGORIA_HABILIDAD | No |
| **TableDetailFragment** | EMPRESA | CRUD (cascading Distrito) | Según rol | LEFT JOIN (search implícito) | No |
| **TableDetailFragment** | POSTULANTE | CRUD (cascading Distrito + tipo doc) | Según rol | JOIN POSTULANTE (experiencia) | No |
| **TableDetailFragment** | OFERTA_ACADEMICA | CRUD | Según rol | LEFT JOIN INSTITUCION + GRADO | No |
| **TableDetailFragment** | OFERTA_TRABAJO | CRUD (+chip VIGENTE/VENCIDA) | Según rol | LEFT JOIN EMPRESA + GRADO | No |
| **TableDetailFragment** | DETALLE_REQUISITO | CRUD (cascading Empresa→Oferta) | Según rol | LEFT JOIN OFERTA_TRABAJO + EMPRESA | No |
| **TableDetailFragment** | EXPERIENCIA_LABORAL | CRUD | Según rol | LEFT JOIN POSTULANTE | No |
| **TableDetailFragment** | CERTIFICACION | CRUD (+6 validaciones fecha) | Según rol | LEFT JOIN POSTULANTE + INST + TIPO | No |
| **TableDetailFragment** | FORMACION_ACADEMICA | CRUD (+6 validaciones fecha) | Según rol | LEFT JOIN POSTULANTE + OFERTA_ACAD | No |
| **TableDetailFragment** | HABILIDAD_POSTULANTE | CRUD (cascading Cat→Hab) | Según rol | LEFT JOIN POSTULANTE + HABILIDAD | No |
| **TableDetailFragment** | POSTULACION | CRUD (roles: postulante crea, empresa modifica estado) + **CHIP VIGENTE** + **EXPORTAR PDF** | Según rol (con restricciones) | LEFT JOIN POSTULANTE + OFERTA_TRABAJO | ✅ **CV + Vacante PDF** |
| **TableDetailFragment** | RED_SOCIAL_POSTULANTE | CRUD | Según rol | LEFT JOIN POSTULANTE + RED_SOCIAL | No |
| **EditorDialogFragment** | 23 tablas | Create / Update | FULL ACCESS requerido | — | — |
| **DeleteConfirmDialog** | 23 tablas | Delete | FULL ACCESS requerido | — | — |

## 7. Matriz de Roles y Permisos

### 7.1 Modelo de Acceso

El sistema implementa un modelo de **control de acceso basado en roles (RBAC)** con 3 roles y 3 niveles de acceso. La definición central se encuentra en `Constants.kt`, método `getRoleTables()`.

#### 7.1.1 Niveles de Acceso

```kotlin
enum class AccessLevel {
    NONE,        // Sin acceso: la tabla no está disponible
    READ_ONLY,   // Solo lectura: puede ver datos pero no crear/editar/eliminar
    FULL         // Acceso completo: puede crear, editar y eliminar
}
```

#### 7.1.2 Roles del Sistema

| Rol | Constante | Valor en BD | Descripción |
|-----|-----------|-------------|-------------|
| **Administrador** | `ROLE_ADMIN` | `"administrador"` | Acceso FULL a las 23 tablas |
| **Postulante** | `ROLE_POSTULANTE` | `"postulante"` | Acceso FULL a su perfil y datos relacionados; READ_ONLY a empresas y ofertas |
| **Gerente de Empresa** | `ROLE_EMPRESA` | `"gerente de empresa"` | Acceso FULL a su empresa y ofertas; READ_ONLY a postulantes |

#### 7.1.3 Ubicación de la Definición

- **Definición del enum y mapa:** `Constants.kt` — líneas 62-159
- **Constantes de rol:** `Constants.kt` — líneas 13-15
- **Lista completa de tablas:** `Constants.kt` — líneas 42-52 (`ALL_TABLES`)
- **Enforcement en UI:** `TableDetailFragment.kt`, `EditorDialogFragment.kt`, `DeleteConfirmDialog.kt`
- **Enforcement en ViewModel:** `DashboardViewModel.kt` (filtrado de tablas visibles)

---

### 7.2 Matriz de Acceso por Tabla

| # | Tabla | Administrador | Postulante | Gerente de Empresa |
|---|-------|:-------------:|:----------:|:------------------:|
| 1 | CATEGORIA_HABILIDAD | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 2 | GENERO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 3 | TIPO_DOCUMENTO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 4 | DEPARTAMENTO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 5 | GRADO_ACADEMICO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 6 | RED_SOCIAL | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 7 | TIPO_CERTIFICACION | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 8 | MUNICIPIO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 9 | DISTRITO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 10 | INSTITUCION | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 11 | HABILIDAD | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 12 | OFERTA_ACADEMICA | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 13 | USUARIO | 🟢 FULL | ⚪ Sin acceso | ⚪ Sin acceso |
| 14 | EMPRESA | 🟢 FULL | 🔵 READ_ONLY | 🟢 FULL |
| 15 | OFERTA_TRABAJO | 🟢 FULL | 🔵 READ_ONLY | 🟢 FULL |
| 16 | DETALLE_REQUISITO | 🟢 FULL | 🔵 READ_ONLY | 🟢 FULL |
| 17 | POSTULANTE | 🟢 FULL | 🟢 FULL | 🔵 READ_ONLY |
| 18 | EXPERIENCIA_LABORAL | 🟢 FULL | 🟢 FULL | 🔵 READ_ONLY |
| 19 | FORMACION_ACADEMICA | 🟢 FULL | 🟢 FULL | 🔵 READ_ONLY |
| 20 | HABILIDAD_POSTULANTE | 🟢 FULL | 🟢 FULL | 🔵 READ_ONLY |
| 21 | CERTIFICACION | 🟢 FULL | 🟢 FULL | 🔵 READ_ONLY |
| 22 | RED_SOCIAL_POSTULANTE | 🟢 FULL | 🟢 FULL | 🔵 READ_ONLY |
| 23 | POSTULACION | 🟢 FULL | 🟢 FULL | 🟢 FULL |

**Totales:**

| Rol | FULL | READ_ONLY | NONE |
|-----|:----:|:---------:|:----:|
| Administrador | **23** | 0 | 0 |
| Postulante | **7** (POSTULANTE + 6 hijas) | **3** (EMPRESA, OFERTA_TRABAJO, DETALLE_REQUISITO) | **13** (catálogos y administrativas) |
| Gerente de Empresa | **4** (EMPRESA, OFERTA_TRABAJO, DETALLE_REQUISITO, POSTULACION) | **6** (POSTULANTE + 5 hijas) | **13** (catálogos y administrativas) |

---

### 7.3 Reglas de Negocio Adicionales

Además del acceso por rol, existen **reglas de negocio específicas** que restringen operaciones incluso cuando el rol tiene FULL ACCESS:

#### 7.3.1 POSTULACION — Modo Edición (Postulante)

**Archivo:** `EditorDialogFragment.kt` — línea 115 y 1655

Un postulante **no puede modificar ninguna postulación existente**. Si intenta editar una postulación:

```kotlin
// En onCreateView (línea 115):
if (isEditMode && tableName == "POSTULACION") {
    when (userRole) {
        ROLE_POSTULANTE -> {
            disableAllFields()  // Bloquea TODOS los campos
            mostrar_toast("Solo la empresa puede modificar esta postulación")
        }
    }
}

// En saveData (línea 1655):
if (role == ROLE_POSTULANTE && tableName == "POSTULACION" && isEditMode) {
    mostrar_toast("No tienes permiso para editar esta postulación")
    return  // Cancela el guardado
}
```

Un postulante **sí puede crear** una nueva postulación (INSERT). La postulación se crea con estado "activo" por defecto.

#### 7.3.2 POSTULACION — Modo Edición (Empresa)

**Archivo:** `EditorDialogFragment.kt` — líneas 121-124

Una empresa **solo puede modificar el campo `ESTADO_PROCESO`** de una postulación existente. Los demás campos se bloquean:

```kotlin
ROLE_EMPRESA -> {
    disableNonEstadoFields()  // Bloquea todos los campos excepto ESTADO_PROCESO
}
```

Esto permite a la empresa cambiar el estado de una postulación (activo → en proceso → contratado/rechazado) sin modificar otros datos.

#### 7.3.3 POSTULACION — Creación (Empresa vs Botón "+" )

**Archivo:** `TableDetailFragment.kt` — líneas 115-117

Si el rol es "gerente de empresa" y la tabla es POSTULACION, el botón FAB "+" se oculta:

```kotlin
if (role == ROLE_EMPRESA && tableName == "POSTULACION") {
    fabAdd.visibility = View.GONE  // No puede crear postulaciones manualmente
}
```

**Nota:** La empresa puede crear postulaciones indirectamente, pero el FAB "+" está oculto por diseño de UI.

#### 7.3.4 USUARIO — Prevención de Auto-Eliminación

**Archivo:** `DeleteConfirmDialog.kt` — líneas 250-258

Un usuario **no puede eliminar su propio registro** de la tabla USUARIO mientras tenga la sesión activa:

```kotlin
if (tableName == TABLE_USUARIO && idToDelete != null) {
    val activeUserId = prefs.getInt(KEY_USER_ID, -1)
    if (idToDelete == activeUserId.toString()) {
        mostrar_toast("No puedes eliminar tu propio usuario mientras está activo")
        return  // Cancela la eliminación
    }
}
```

#### 7.3.5 USUARIO — Edición de Contraseña

**Archivo:** `EditorDialogFragment.kt` — líneas 1270-1274

En modo edición de USUARIO, el campo PASSWORD está deshabilitado:
```kotlin
if (column == "PASSWORD" && tableName == "USUARIO" && isEditMode) {
    et.isEnabled = false
    et.isFocusable = false
    til.hint = "Contraseña (bloqueada)"
}
```
La contraseña solo se puede establecer al crear el usuario o mediante inserción directa desde el repositorio.

#### 7.3.6 Reglas de Eliminación por Tabla (Protección de Catálogos)

| Tabla | Restricción de eliminación |
|-------|---------------------------|
| DEPARTAMENTO | No se puede eliminar si tiene municipios asociados |
| MUNICIPIO | No se puede eliminar si tiene distritos asociados |
| DISTRITO | No se puede eliminar si tiene postulantes o empresas asociados |
| GENERO | No se puede eliminar si tiene postulantes asociados |
| TIPO_DOCUMENTO | No se puede eliminar si tiene postulantes asociados |
| GRADO_ACADEMICO | No se puede eliminar si tiene ofertas de trabajo, ofertas académicas o postulantes asociados |
| INSTITUCION | No se puede eliminar si tiene certificaciones u ofertas académicas asociadas |
| TIPO_CERTIFICACION | No se puede eliminar si tiene certificaciones asociadas |
| CATEGORIA_HABILIDAD | No se puede eliminar si tiene habilidades asociadas |
| HABILIDAD | No se puede eliminar si está asignada a postulantes |
| RED_SOCIAL | No se puede eliminar si tiene postulantes vinculados |
| EMPRESA | No se puede eliminar si tiene ofertas de trabajo o experiencias laborales |
| POSTULANTE | No se puede eliminar si tiene postulaciones, experiencias, formaciones, certificaciones, habilidades o redes sociales |
| OFERTA_TRABAJO | No se puede eliminar si tiene requisitos o postulaciones |
| OFERTA_ACADEMICA | No se puede eliminar si tiene formaciones académicas asociadas |
| USUARIO | No se puede eliminar si es el usuario logueado |

Estas reglas se implementan en dos capas:
1. **Programática:** `MainRepository.getDeleteDependencies()` consulta los registros hijos y muestra advertencia.
2. **TRIGGER:** Los triggers de integridad referencial en la BD también bloquean la eliminación si hay dependencias (mediante `RAISE(ABORT)`).

---

### 7.4 Enforcement en el Código

Los permisos se verifican en **4 puntos del código** antes de permitir cualquier operación:

#### 7.4.1 TableDetailFragment — Acceso a Pantalla

```kotlin
// Líneas 105-110
val access = Constants.getRoleTables(role)[tableName] ?: AccessLevel.NONE
if (access == AccessLevel.NONE) {
    StyledToast.show(requireContext(), "No tienes acceso a esta tabla")
    requireActivity().onBackPressedDispatcher.onBackPressed()  // Vuelve al dashboard
    return
}
canEdit = access == AccessLevel.FULL
canDelete = access == AccessLevel.FULL
```

#### 7.4.2 TableDetailFragment — Visibilidad de FABs

```kotlin
if (!canEdit) fabAdd.visibility = View.GONE                        // Ocultar botón "+"
// Edit/Delete buttons se ocultan en TableAdapter según canEdit/canDelete
```

#### 7.4.3 EditorDialogFragment — Guardado

```kotlin
// Líneas 1648-1653
val access = Constants.getRoleTables(role)[tableName] ?: AccessLevel.NONE
if (access != AccessLevel.FULL) {
    StyledToast.show(requireContext(), "No tienes permiso para modificar esta tabla")
    return
}
```

#### 7.4.4 DeleteConfirmDialog — Eliminación

```kotlin
// Líneas 239-245
val access = Constants.getRoleTables(role)[tableName] ?: AccessLevel.NONE
if (access != AccessLevel.FULL) {
    StyledToast.show(requireContext(), "No tienes permiso para eliminar registros de esta tabla")
    dismiss()
    return
}
```

#### 7.4.5 DashboardViewModel — Filtrado de Tablas Visibles

```kotlin
// Líneas 56-57
val roleTables = Constants.getRoleTables(currentRole)
val filtered = all.filter { roleTables.containsKey(it.name) }
// Solo se muestran en el dashboard las tablas que el rol tiene en su mapa (FULL o READ_ONLY)
```

#### 7.4.6 DashboardAdapter — Badge de Solo Lectura

```kotlin
// Líneas 91-96
if (table.isReadOnly) {
    tvAccessBadge.visibility = View.VISIBLE
    tvAccessBadge.text = "Solo lectura"
} else {
    tvAccessBadge.visibility = View.GONE
}
```

---

### 7.5 Resumen de Permisos por Operación

| Operación | Administrador | Postulante (propias) | Postulante (otras) | Empresa (propias) | Empresa (otras) |
|-----------|:------------:|:--------------------:|:------------------:|:-----------------:|:---------------:|
| **Ver dashboard** | ✅ 23 tablas | ✅ 10 tablas | — | ✅ 10 tablas | — |
| **Ver detalle de tabla** | ✅ Todas | ✅ 10 tablas | — | ✅ 10 tablas | — |
| **Crear registro** | ✅ Todas | ✅ 7 tablas propias | ❌ | ✅ 4 tablas propias | ❌ |
| **Editar registro** | ✅ Todas | ⚠️ POSTULACION no (solo crear) | ❌ | ✅ Empresa/Oferta/Detalle; ⚠️ POSTULACION solo estado | ❌ |
| **Eliminar registro** | ✅ Todas (excepto su propio USUARIO) | ✅ 7 tablas propias | ❌ | ✅ 4 tablas propias | ❌ |
| **Exportar PDF** | ✅ POSTULACION | ✅ POSTULACION | — | ✅ POSTULACION | — |
| **Insertar seed data** | ✅ | ❌ | — | ❌ | — |

---

### 7.6 Mapa de Archivos que Implementan Permisos

| Archivo | Función | Qué verifica |
|---------|---------|-------------|
| `Constants.kt:130-159` | `getRoleTables()` | Define el mapa rol→tablas→nivel |
| `TableDetailFragment.kt:105-110` | `onViewCreated()` | Acceso NONE → redirige al dashboard |
| `TableDetailFragment.kt:111-117` | `onViewCreated()` | FULL → permite edición; empresa+POSTULACION oculta FAB |
| `EditorDialogFragment.kt:1648-1653` | `saveData()` | FULL → permite guardar |
| `EditorDialogFragment.kt:115-125` | `onViewCreated()` | POSTULACION+postulante bloquea todo; empresa solo estado |
| `EditorDialogFragment.kt:1655-1658` | `saveData()` | POSTULACION+postulante+edición → cancela guardado |
| `DeleteConfirmDialog.kt:239-245` | `performDelete()` | FULL → permite eliminar |
| `DeleteConfirmDialog.kt:250-258` | `performDelete()` | USUARIO+propio → cancela eliminación |
| `DashboardViewModel.kt:56-57` | `loadTables()` | Filtra tablas según mapa de roles |
| `DashboardAdapter.kt:91-96` | `bind()` | Muestra badge "Solo lectura" |
| `TableAdapter.kt:55-56` | `init()` | Oculta FABs editar/eliminar según flags |

---

### 7.7 Flujo de Decisión de Permisos

```
Usuario hace clic en una tabla en el Dashboard
         │
         ▼
┌─────────────────────────────────────┐
│ DashboardViewModel.loadTables()     │
│ ¿La tabla está en el mapa del rol?  │
├──────────────────┬──────────────────┤
│       SÍ         │       NO         │
└────────┬─────────┘                  │
         │                            │
         ▼                            ▼
┌──────────────────────┐   ┌──────────────────────┐
│ Navega a             │   │ No se muestra en el  │
│ TableDetailFragment  │   │ dashboard            │
└──────────┬───────────┘   └──────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ TableDetailFragment.onViewCreated() │
│ Verifica AccessLevel:               │
│   NONE → "No tienes acceso" + back  │
│   READ_ONLY → solo ver (sin FABs)   │
│   FULL → CRUD completo              │
└──────────────────┬──────────────────┘
                   │
         ┌─────────┴─────────┐
         ▼                   ▼
┌─────────────────┐   ┌─────────────────┐
│ EDITAR (FAB)    │   │ ELIMINAR (FAB)  │
│                 │   │                 │
│ EditorDialog    │   │ DeleteConfirm   │
│ .saveData()     │   │ .performDelete()│
│ Verifica FULL   │   │ Verifica FULL   │
│ + reglas POST   │   │ + no auto-elim  │
└─────────────────┘   └─────────────────┘
```

## 8. Índices

### 8.1 Introducción

La base de datos cuenta con **22 índices** creados explícitamente mediante sentencias `CREATE INDEX`. Estos índices optimizan las búsquedas por claves foráneas y por columnas utilizadas frecuentemente en consultas JOIN y cláusulas WHERE.

Además de estos 22 índices, SQLite crea índices implícitos automáticamente para:
- Cada `PRIMARY KEY` (23 índices implícitos)
- Cada `UNIQUE` constraint (23 índices implícitos)

**Total de índices en el sistema:** 22 explícitos + 46 implícitos = **68 índices**.

### 8.2 Listado Completo de Índices

| # | Nombre | Tabla | Columna(s) | Propósito |
|---|--------|-------|------------|-----------|
| 1 | `IDX_MUNICIPIO_DEPTO` | MUNICIPIO | `ID_DEPARTAMENTO` | Optimiza búsquedas de municipios por departamento (FK) |
| 2 | `IDX_DISTRITO_MUNICIPIO` | DISTRITO | `ID_DEPARTAMENTO, ID_MUNICIPIO` | Optimiza búsquedas de distritos por municipio (FK compuesta) |
| 3 | `IDX_HABILIDAD_CATEGORIA` | HABILIDAD | `ID_CATEGORIA_HABILIDAD` | Optimiza búsquedas de habilidades por categoría (FK) |
| 4 | `IDX_EMPRESA_DISTRITO` | EMPRESA | `ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID` | Optimiza búsquedas de empresas por distrito (FK compuesta 3 cols) |
| 5 | `IDX_POSTULANTE_GENERO` | POSTULANTE | `ID_GENERO` | Optimiza búsquedas de postulantes por género (FK) |
| 6 | `IDX_POSTULANTE_TIPO_DOC` | POSTULANTE | `ID_TIPO_DOCUMENTO` | Optimiza búsquedas de postulantes por tipo documento (FK) |
| 7 | `IDX_POSTULANTE_DISTRITO` | POSTULANTE | `ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID` | Optimiza búsquedas de postulantes por distrito (FK compuesta 3 cols) |
| 8 | `IDX_OFERTA_GRADO` | OFERTA_TRABAJO | `ID_GRADO_ACADEMICO` | Optimiza búsquedas de ofertas por grado académico (FK) |
| 9 | `IDX_DETALLE_OFERTA` | DETALLE_REQUISITO | `NIT, ID_OFERTA` | Optimiza búsquedas de requisitos por oferta (FK compuesta) |
| 10 | `IDX_EXP_POSTULANTE` | EXPERIENCIA_LABORAL | `ID_POSTULANTE` | Optimiza búsquedas de experiencias por postulante (FK). Usado en getExperienciasForPostulant() |
| 11 | `IDX_EXP_EMPRESA` | EXPERIENCIA_LABORAL | `NIT` | Optimiza búsquedas de experiencias por empresa (FK) |
| 12 | `IDX_CERT_POSTULANTE` | CERTIFICACION | `ID_POSTULANTE` | Optimiza búsquedas de certificaciones por postulante (FK). Usado en getCertificacionesForPostulant() |
| 13 | `IDX_CERT_INSTITUCION` | CERTIFICACION | `ID_INSTITUCION` | Optimiza búsquedas de certificaciones por institución (FK) |
| 14 | `IDX_FORM_POSTULANTE` | FORMACION_ACADEMICA | `ID_POSTULANTE` | Optimiza búsquedas de formaciones por postulante (FK). Usado en getFormacionesForPostulant() |
| 15 | `IDX_HAB_POST_POSTULANTE` | HABILIDAD_POSTULANTE | `ID_POSTULANTE` | Optimiza búsquedas de habilidades por postulante (FK). Usado en getHabilidadesForPostulant() |
| 16 | `IDX_HAB_POST_HABILIDAD` | HABILIDAD_POSTULANTE | `ID_HABILIDAD` | Optimiza búsquedas de postulantes por habilidad |
| 17 | `IDX_POSTULACION_POSTULANTE` | POSTULACION | `ID_POSTULANTE` | Optimiza búsquedas de postulaciones por postulante (FK) |
| 18 | `IDX_POSTULACION_OFERTA` | POSTULACION | `NIT, ID_OFERTA` | Optimiza búsquedas de postulaciones por oferta (FK compuesta). Usado en TR_POSTULACION_VIGENCIA |
| 19 | `IDX_RED_POST_POSTULANTE` | RED_SOCIAL_POSTULANTE | `ID_POSTULANTE` | Optimiza búsquedas de redes por postulante (FK). Usado en getRedesForPostulant() |
| 20 | `IDX_OA_INSTITUCION` | OFERTA_ACADEMICA | `ID_INSTITUCION` | Optimiza búsquedas de ofertas académicas por institución (FK) |
| 21 | `IDX_OA_GRADO` | OFERTA_ACADEMICA | `ID_GRADO_ACADEMICO` | Optimiza búsquedas de ofertas académicas por grado (FK) |

### 8.3 Índices por Tabla

| Tabla | Índices explícitos | Columnas indexadas |
|-------|:------------------:|--------------------|
| CATEGORIA_HABILIDAD | 0 | — |
| GENERO | 0 | — |
| TIPO_DOCUMENTO | 0 | — |
| DEPARTAMENTO | 0 | — |
| GRADO_ACADEMICO | 0 | — |
| RED_SOCIAL | 0 | — |
| TIPO_CERTIFICACION | 0 | — |
| USUARIO | 0 | — |
| INSTITUCION | 0 | — |
| HABILIDAD | 0 | — |
| OFERTA_ACADEMICA | 0 | — |
| **MUNICIPIO** | **1** | ID_DEPARTAMENTO |
| **DISTRITO** | **1** | ID_DEPARTAMENTO, ID_MUNICIPIO |
| **HABILIDAD** | **1** | ID_CATEGORIA_HABILIDAD |
| **EMPRESA** | **1** | ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID |
| **POSTULANTE** | **3** | ID_GENERO, ID_TIPO_DOCUMENTO, (ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) |
| **OFERTA_TRABAJO** | **1** | ID_GRADO_ACADEMICO |
| **DETALLE_REQUISITO** | **1** | NIT, ID_OFERTA |
| **EXPERIENCIA_LABORAL** | **2** | ID_POSTULANTE, NIT |
| **CERTIFICACION** | **2** | ID_POSTULANTE, ID_INSTITUCION |
| **FORMACION_ACADEMICA** | **1** | ID_POSTULANTE |
| **HABILIDAD_POSTULANTE** | **2** | ID_POSTULANTE, ID_HABILIDAD |
| **POSTULACION** | **2** | ID_POSTULANTE, (NIT, ID_OFERTA) |
| **RED_SOCIAL_POSTULANTE** | **1** | ID_POSTULANTE |

### 8.4 Tablas sin Índices Explícitos

12 tablas no tienen índices explícitos porque:
- Son tablas catálogo pequeñas donde la búsqueda siempre es por PK o nombre (que tiene UNIQUE implícito).
- La cantidad de registros es baja (< 100).

### 8.5 DDL de Índices

La creación de índices se realiza tanto en `esquema_bolsa_trabajo.sql` como en `ConnectionHelper.kt` con la sintaxis:

```sql
CREATE INDEX IF NOT EXISTS IDX_MUNICIPIO_DEPTO ON MUNICIPIO (ID_DEPARTAMENTO);
CREATE INDEX IF NOT EXISTS IDX_DISTRITO_MUNICIPIO ON DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO);
CREATE INDEX IF NOT EXISTS IDX_HABILIDAD_CATEGORIA ON HABILIDAD (ID_CATEGORIA_HABILIDAD);
-- ... 19 índices adicionales
```

---

## 9. Datos de Prueba

### 9.1 Introducción

El archivo `SeedData.kt` contiene los datos de prueba (seed data) que se insertan al presionar el botón "Insertar datos de prueba" en LoginFragment (o mediante `MainRepository.insertSeedData()`).

La inserción se realiza dentro de una **transacción** (`beginTransaction()` / `setTransactionSuccessful()` / `endTransaction()`) para garantizar atomicidad: si algo falla, no queda la base de datos a medio llenar.

Antes de insertar, se verifica que **ninguna** de las tablas a poblar tenga datos. Si alguna tabla ya tiene registros, se cancela la operación con el mensaje "La base de datos ya fue llenada".

### 9.2 Datos por Tabla

#### 9.2.1 DEPARTAMENTO (14 registros)

| # | ID_DEPARTAMENTO | NOMBRE_DEPARTAMENTO |
|:-:|:---------------:|---------------------|
| 1 | 1 | Ahuachapán |
| 2 | 2 | Santa Ana |
| 3 | 3 | Sonsonate |
| 4 | 4 | Chalatenango |
| 5 | 5 | Cuscatlán |
| 6 | 6 | San Salvador |
| 7 | 7 | La Libertad |
| 8 | 8 | La Paz |
| 9 | 9 | Cabañas |
| 10 | 10 | San Vicente |
| 11 | 11 | Usulután |
| 12 | 12 | San Miguel |
| 13 | 13 | Morazán |
| 14 | 14 | La Unión |

#### 9.2.2 GENERO (5 registros)

| ID_GENERO | NOMBRE_GENERO |
|:---------:|--------------|
| 1 | Femenino |
| 2 | Masculino |
| 3 | No binario |
| 4 | Prefiero no decirlo |
| 5 | Otro |

#### 9.2.3 CATEGORIA_HABILIDAD (5 registros)

| ID_CATEGORIA | NOMBRE_CATEGORIA |
|:-----------:|-----------------|
| 1 | Desarrollo de Software y Lógica |
| 2 | Infraestructura y Cloud Computing |
| 3 | Redes y Telecomunicaciones |
| 4 | Bases de Datos |
| 5 | Herramientas de Inteligencia Artificial |

#### 9.2.4 GRADO_ACADEMICO (7 registros)

| ID_GRADO | NOMBRE_GRADO |
|:--------:|-------------|
| 1 | Bachiller |
| 2 | Técnico Superior |
| 3 | Profesorado |
| 4 | Licenciatura |
| 5 | Ingeniería |
| 6 | Maestría |
| 7 | Doctorado |

#### 9.2.5 TIPO_DOCUMENTO (3 registros)

| ID_TIPO | NOMBRE_TIPO |
|:-------:|-------------|
| 1 | DUI |
| 2 | NIT |
| 3 | Pasaporte |

#### 9.2.6 RED_SOCIAL (5 registros)

| ID_RED | NOMBRE_RED |
|:------:|------------|
| 1 | GitHub |
| 2 | Steam |
| 3 | LinkedIn |
| 4 | Discord |
| 5 | X (Twitter) |

#### 9.2.7 TIPO_CERTIFICACION (5 registros)

| ID_TIPO | NOMBRE_TIPO |
|:-------:|-------------|
| 1 | Certificacion Profesional |
| 2 | Diplomado |
| 3 | Curso |
| 4 | Idioma |
| 5 | Seminario |

#### 9.2.8 INSTITUCION (6 registros)

| ID_INSTITUCION | NOMBRE_INSTITUCION |
|----------------|-------------------|
| INS001 | Universidad de El Salvador (UES) |
| INS002 | Escuela Nacional de Agricultura (ENA) |
| INS003 | Fundación Gloria de Kriete |
| INS004 | Universidad Don Bosco |
| INS005 | Universidad José Matías Delgado |
| INS006 | Ministerio de Educación, Ciencia y Tecnología (MINED) |

#### 9.2.9 MUNICIPIO (44 registros)

Los municipios se organizan por departamento (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_MUNICIPIO):

| Depto | ID_Mun | Nombre |
|:-----:|:------:|--------|
| 1 (Ahuachapán) | 1 | Ahuachapán Norte |
| 1 | 2 | Ahuachapán Centro |
| 1 | 3 | Ahuachapán Sur |
| 2 (Santa Ana) | 1-4 | Santa Ana Norte, Centro, Este, Oeste |
| 3 (Sonsonate) | 1-4 | Sonsonate Norte, Centro, Este, Oeste |
| 4 (Chalatenango) | 1-3 | Chalatenango Norte, Centro, Sur |
| 5 (Cuscatlán) | 1-2 | Cuscatlán Norte, Sur |
| 6 (San Salvador) | 1-5 | San Salvador Norte, Oeste, Este, Centro, Sur |
| 7 (La Libertad) | 1-6 | La Libertad Norte, Centro, Oeste, Este, Costa, Sur |
| 8 (La Paz) | 1-3 | La Paz Oeste, Centro, Este |
| 9 (Cabañas) | 1-2 | Cabañas Este, Oeste |
| 10 (San Vicente) | 1-2 | San Vicente Norte, Sur |
| 11 (Usulután) | 1-3 | Usulután Norte, Este, Oeste |
| 12 (San Miguel) | 1-3 | San Miguel Norte, Centro, Oeste |
| 13 (Morazán) | 1-2 | Morazán Norte, Sur |
| 14 (La Unión) | 1-2 | La Unión Norte, Sur |

#### 9.2.10 DISTRITO (262 registros)

Los distritos son la tabla más grande de datos semilla. Cada distrito pertenece a un departamento y municipio específico. Ejemplos representativos:

| Depto | Municipio | ID_Distrito | NOMBRE_DISTRITO |
|:-----:|:---------:|:-----------:|-----------------|
| 1 | 1 | 1 | Atiquizaya |
| 1 | 1 | 2 | El Refugio |
| 1 | 1 | 3 | San Lorenzo |
| 1 | 1 | 4 | Turín |
| 1 | 2 | 1 | Ahuachapán |
| ... | ... | ... | ... |
| 6 | 4 | 3 | San Salvador (capital) |
| ... | ... | ... | ... |
| 14 | 2 | 8 | Yucuaiquín |

Total: **262 distritos** que cubren los 14 departamentos de El Salvador.

#### 9.2.11 HABILIDAD (15 registros)

| ID_CAT | ID_HAB | NOMBRE_HABILIDAD |
|:------:|:------:|------------------|
| 1 | H01 | Programación en Python |
| 1 | H02 | Desarrollo en C++ |
| 1 | H03 | Diseño y consumo de APIs |
| 2 | H01 | Despliegue de proyectos en Google Cloud |
| 2 | H02 | Administración de sistemas Linux |
| 2 | H03 | Gestión de entornos en Vercel y Netlify |
| 3 | H01 | Configuración de topologías en Cisco Packet Tracer |
| 3 | H02 | Cálculo y diseño de Subnetting |
| 3 | H03 | Análisis de tráfico de red |
| 4 | H01 | Diseño de modelos relacionales (SQL) |
| 4 | H02 | Integración y gestión con Supabase |
| 4 | H03 | Manejo de bases de datos NoSQL |
| 5 | H01 | Integración de Gemini API y Google AI Studio |
| 5 | H02 | Ingeniería de prompts avanzados |
| 5 | H03 | Automatización de procesos con IA |

#### 9.2.12 EMPRESA (10 registros)

| NIT | ID_DISTRITO_DEPTO | ID_DISTRITO_MUN | ID_DISTRITO_ID | NOMBRE_EMPRESA | CONTACTO_DIRECTO |
|-----|:-----------------:|:---------------:|:--------------:|----------------|:----------------:|
| 06141234560101 | 6 | 4 | 3 | Banco Agricola | 2200-0001 |
| 06141234560102 | 6 | 4 | 3 | Nequi El Salvador | 2200-0002 |
| 06141234560103 | 6 | 4 | 3 | Super Selectos Sede Central | 2200-0003 |
| 06141234560104 | 7 | 6 | 2 | Holcim El Salvador | 2200-0004 |
| 06141234560105 | 2 | 2 | 1 | AES CLESA | 2200-0005 |
| 06141234560106 | 12 | 2 | 1 | Grupo Campestre | 2200-0006 |
| 06141234560107 | 3 | 2 | 1 | Compania Azucarera Salvadorena - CASSA | 2200-0007 |
| 06141234560108 | 11 | 1 | 3 | La Geo Planta Geotermica | 2200-0008 |
| 06141234560109 | 1 | 2 | 1 | Cooperativa Los Ausoles | 2200-0009 |
| 06141234560110 | 5 | 2 | 1 | Embutidos La Unica | 2200-0010 |

#### 9.2.13 OFERTA_ACADEMICA (5 registros)

| ID_OFERTA_ACADEMICA | ID_GRADO_ACADEMICO | ID_INSTITUCION |
|:-------------------:|:------------------:|:--------------:|
| OFA01 | 2 | INS002 |
| OFA02 | 5 | INS001 |
| OFA03 | 4 | INS001 |
| OFA04 | 6 | INS002 |
| OFA05 | 7 | INS001 |

#### 9.2.14 USUARIO (2 registros)

| ID_USUARIO | USERNAME | PASSWORD (hash PBKDF2) | ROL |
|:----------:|----------|------------------------|---------------------|
| 1 | postulante | `PBKDF2_SALT:PBKDF2_HASH` de "12345678" | postulante |
| 2 | empresa | `PBKDF2_SALT:PBKDF2_HASH` de "12345678" | gerente de empresa |

### 9.3 Resumen de Datos de Prueba

| Tabla | Registros | Depende de |
|-------|:---------:|------------|
| DEPARTAMENTO | 14 | — |
| GENERO | 5 | — |
| CATEGORIA_HABILIDAD | 5 | — |
| GRADO_ACADEMICO | 7 | — |
| TIPO_DOCUMENTO | 3 | — |
| RED_SOCIAL | 5 | — |
| TIPO_CERTIFICACION | 5 | — |
| INSTITUCION | 6 | — |
| MUNICIPIO | 44 | DEPARTAMENTO |
| DISTRITO | 262 | DEPARTAMENTO, MUNICIPIO |
| HABILIDAD | 15 | CATEGORIA_HABILIDAD |
| EMPRESA | 10 | DISTRITO |
| OFERTA_ACADEMICA | 5 | INSTITUCION, GRADO_ACADEMICO |
| USUARIO | 2 | — |
| **Total** | **~388** | |

### 9.4 Orden de Inserción

El método `insertSeedData()` inserta los datos en el siguiente orden para respetar las dependencias de FK:

```
1. DEPARTAMENTO      (padre de MUNICIPIO)
2. GENERO             (padre de POSTULANTE)
3. CATEGORIA_HABILIDAD (padre de HABILIDAD)
4. GRADO_ACADEMICO    (padre de POSTULANTE, OFERTA_TRABAJO, OFERTA_ACADEMICA)
5. RED_SOCIAL         (padre de RED_SOCIAL_POSTULANTE)
6. TIPO_CERTIFICACION (padre de CERTIFICACION)
7. INSTITUCION        (padre de OFERTA_ACADEMICA, CERTIFICACION)
8. TIPO_DOCUMENTO     (padre de POSTULANTE)
9. MUNICIPIO          (hijo de DEPARTAMENTO, padre de DISTRITO)
10. DISTRITO          (hijo de MUNICIPIO, padre de EMPRESA/POSTULANTE)
11. HABILIDAD         (hijo de CATEGORIA_HABILIDAD, padre de HABILIDAD_POSTULANTE)
12. EMPRESA           (hijo de DISTRITO, padre de OFERTA_TRABAJO/EXPERIENCIA_LABORAL)
13. OFERTA_ACADEMICA  (hijo de INSTITUCION/GRADO_ACADEMICO, padre de FORMACION_ACADEMICA)
14. USUARIO           (independiente)
```
