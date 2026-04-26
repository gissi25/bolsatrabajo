# BT - Bolsa de Trabajo

Aplicación Android nativa para la gestión de una bolsa de trabajo, desarrollada en Kotlin con arquitectura MVVM, base de datos SQLite local y Material Design 3.

---

## Arquitectura

El proyecto sigue el patrón **MVVM** (Model-View-ViewModel) con las siguientes capas:

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Fragments + Dialogs)                 │
│  ─ sv/ues/fia/eisi/bt/ui/                       │
│    ├── auth/        (Login, Register)            │
│    ├── crud/        (TableDetail, Editor, etc.)  │
│    └── dashboard/   (Dashboard)                  │
├─────────────────────────────────────────────────┤
│  ViewModel Layer                                 │
│  ─ sv/ues/fia/eisi/bt/viewmodel/                 │
│    ├── AuthViewModel.kt                          │
│    ├── CrudViewModel.kt                          │
│    └── DashboardViewModel.kt                     │
├─────────────────────────────────────────────────┤
│  Repository Layer                                │
│  ─ sv/ues/fia/eisi/bt/data/repository/           │
│    └── MainRepository.kt                         │
├─────────────────────────────────────────────────┤
│  Data Layer                                      │
│  ─ sv/ues/fia/eisi/bt/data/local/                │
│    ├── ConnectionHelper.kt (SQLite wrapper)      │
│    ├── dao/           (22 DAOs)                  │
│    └── entities/      (22 entidades)             │
└─────────────────────────────────────────────────┘
```

---

## Estructura del proyecto

```
BT/
├── app/
│   └── src/main/
│       ├── assets/
│       │   └── si.db                    # Base de datos SQLite pre-poblada
│       ├── java/sv/ues/fia/eisi/bt/
│       │   ├── BTApplication.kt         # Application class (fuerza modo oscuro)
│       │   ├── MainActivity.kt          # Actividad única con NavHostFragment
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── ConnectionHelper.kt   # Wrapper SQLite (open, copy, exec)
│       │   │   │   ├── dao/                  # 22 DAOs (CRUD por tabla)
│       │   │   │   └── entities/             # 22 data classes
│       │   │   └── repository/
│       │   │       └── MainRepository.kt     # CRUD genérico + FK references
│       │   ├── ui/
│       │   │   ├── auth/                     # LoginFragment, RegisterFragment
│       │   │   ├── crud/                     # TableDetailFragment, EditorDialogFragment, etc.
│       │   │   └── dashboard/                # DashboardFragment, DashboardAdapter
│       │   ├── utils/
│       │   │   ├── Constants.kt              # Constantes de la app
│       │   │   ├── PasswordHasher.kt         # Hashing PBKDF2
│       │   │   └── StyledToast.kt            # Toast con tema adaptable
│       │   └── viewmodel/
│       │       ├── AuthViewModel.kt          # Login + Register
│       │       ├── CrudViewModel.kt          # CRUD genérico
│       │       └── DashboardViewModel.kt     # Tablas + conteos
│       ├── res/
│       │   ├── layout/           # 10 layouts XML
│       │   ├── drawable/         # Iconos vectoriales + toast_background
│       │   ├── values/           # colors.xml, strings.xml, themes.xml (oscuro)
│       │   ├── values-night/     # themes.xml (claro - toggle)
│       │   ├── navigation/       # nav_graph.xml
│       │   └── menu/             # menu_dashboard.xml
│       └── AndroidManifest.xml
├── build.gradle.kts               # Proyecto
├── settings.gradle.kts
└── gradle/libs.versions.toml      # Version catalog
```

---

## Base de datos

22 tablas SQLite pre-pobladas en `assets/si.db`. La base se copia al almacenamiento interno en el primer inicio.

### Tablas principales

| Tabla | Propósito |
|-------|-----------|
| `USUARIO` | Autenticación (username, password hash, rol) |
| `POSTULANTE` | Datos personales del solicitante |
| `EMPRESA` | Información de la empresa |
| `OFERTA_TRABAJO` | Ofertas de empleo publicadas |
| `POSTULACION` | Postulaciones a ofertas |
| `HABILIDAD` | Catálogo de habilidades |
| `HABILIDAD_POSTULANTE` | Habilidades por postulante (+ nivel 1-3) |
| `CERTIFICACION` | Certificaciones del postulante |
| `EXPERIENCIA_LABORAL` | Experiencia laboral |
| `FORMACION_ACADEMICA` | Formación académica |
| `DETALLE_REQUISITO` | Requisitos de una oferta |
| `OFERTA_ACADEMICA` | Ofertas académicas por institución |
| `RED_SOCIAL` | Catálogo de redes sociales |
| `RED_SOCIAL_POSTULANTE` | Redes sociales del postulante |
| `CATEGORIA_HABILIDAD` | Categorías de habilidades |
| `GRADO_ACADEMICO` | Grados académicos |
| `INSTITUCION` | Instituciones educativas |
| `GENERO` | Géneros |
| `TIPO_DOCUMENTO` | Tipos de documento |
| `DEPARTAMENTO` | Departamentos geográficos |
| `MUNICIPIO` | Municipios (FK → DEPARTAMENTO) |
| `DISTRITO` | Distritos (FK → MUNICIPIO) |
| `RED_SOCIAL` | Redes sociales disponibles |

### Triggers (22)

| Trigger | Función |
|---------|---------|
| `TR_CASCADA_HABILIDADES` | Elimina HABILIDAD_POSTULANTE al borrar POSTULANTE |
| `TR_CASCADA_REDES` | Elimina RED_SOCIAL_POSTULANTE al borrar POSTULANTE |
| `TR_CASCADA_REQUISITOS` | Elimina DETALLE_REQUISITO al borrar OFERTA_TRABAJO |
| `TR_CASCADA_USUARIO` | Elimina USUARIO al borrar POSTULANTE |
| `TR_CERTIFICACION_CODIGO` | Evita códigos de certificación duplicados |
| `TR_EMPRESA_NOMBRE_UNICO` | Evita nombres de empresa duplicados |
| `TR_EXP_LABORAL_FECHAS` | Valida fecha_inicio < fecha_fin |
| `TR_HABILIDAD_NIVEL` | Valida nivel_destreza ∈ {1,2,3} |
| `TR_MUNICIPIO_DEPTO` | Valida que DEPARTAMENTO exista |
| `TR_NIVEL_EDUCATIVO` | Rechaza bachillerato o inferior en FORMACION_ACADEMICA |
| `TR_OFERTA_FECHA_AUTO` | Valida fecha_publicacion no NULL |
| `TR_OFERTA_RANGO_EDAD` | Valida edad_minima <= edad_maxima |
| `TR_OFERTA_VIGENCIA` | Valida fecha_caducidad >= fecha_publicacion |
| `TR_POSTULACION_UNICA` | Evita postulaciones duplicadas |
| `TR_POSTULANTE_EDAD` | Valida mayoría de edad (≥18) |
| `TR_POSTULANTE_EMAIL` | Valida formato email (@) |
| `TR_PROTEC_CATEGORIA` | Protege borrado si tiene habilidades vinculadas |
| `TR_PROTEC_DISTRITO` | Protege borrado si tiene postulantes/empresas |
| `TR_PROTEC_GRADO` | Protege borrado si está en ofertas |
| `TR_PROTEC_INSTITUCION` | Protege borrado si tiene ofertas/certificaciones |
| `TR_PROTEC_TIPO_DOC` | Protege borrado si hay postulantes |
| `TR_USUARIO_FORMATO` | Valida contraseña ≥ 8 caracteres |

---

## Flujo de navegación

```
Login ──→ Register
  │
  └──→ Dashboard ──→ TableDetail (CRUD)
```

- **Login**: Autenticación con username + contraseña hasheada (PBKDF2)
- **Register**: Creación de cuenta con selección de rol (postulante, empresa, admin)
- **Dashboard**: Grid de tarjetas con todas las tablas y conteo de registros + buscador
- **TableDetail**: CRUD completo por tabla con:
  - Lista de registros en RecyclerView
  - Búsqueda por texto
  - FAB para crear nuevo registro
  - EditorDialogFragment dinámico (genera campos según las columnas)
  - FK dropdowns (autocompletado desde tabla referenciada)
  - Filtros en cascada (Departamento → Municipio → Distrito)
  - DatePicker con MaterialDatePicker
  - DeleteConfirmDialog

---

## Características implementadas

### Autenticación
- Registro con username, contraseña y rol
- Login con verificación de hash PBKDF2
- Sesión persistente en SharedPreferences
- Logout con limpieza de sesión
- Validación de contraseña (≥ 8 caracteres, vía trigger)

### CRUD Genérico
- Editor dinámico que genera campos según las columnas de cada tabla
- PK auto-generado (MAX+1) — campos PK ocultos en formularios
- FK dropdowns con búsqueda autocompletable
- Filtros dependientes en cascada (Departamento → Municipio → Distrito)
- Validación de campos requeridos, formatos (email, NIT, teléfono, fechas)
- Input masks para NIT (15 dígitos) y teléfono (8 dígitos)
- DatePicker moderno con selector de año (MaterialDatePicker)
- Dropdowns especiales: NIVEL_DESTREZA (1=Básico, 2=Intermedio, 3=Avanzado), ESTADO_PROCESO

### Interfaz
- Material Design 3 con tema claro/oscuro
- Modo oscuro por defecto (con toggle en el menú)
- Cards, FAB, BottomNavigation, Chips, SearchView
- Animaciones de navegación (slide in/out)
- StyledToast adaptable al tema
- Colores dinámicos vía atributos del tema (`?attr/`)

### Base de Datos
- 22 tablas con 22 triggers de integridad
- PKs auto-incrementales (MAX+1)
- Validaciones vía triggers (formato email, edad mínima, unicidad, etc.)
- Protección de borrado en cascada y referencial
- Datos seed pre-cargados

---

## Tecnologías

| Tecnología | Versión |
|------------|---------|
| Kotlin | 1.9+ |
| Android SDK | 36 (target), 24 (min) |
| Material Design 3 | 1.13.0 |
| Jetpack Navigation | Fragment + UI |
| Lifecycle ViewModel | KTX |
| Kotlin Coroutines | IO + Main |
| SQLite | Nativo (pre-poblado) |
| PBKDF2 | Hashing de contraseñas |
| Gradle Version Catalog | libs.versions.toml |

---

## Cómo compilar

```bash
# Linux/macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

El APK generado estará en `app/build/outputs/apk/debug/app-debug.apk`.

---

## Licencia

Proyecto académico — Universidad de El Salvador, Facultad de Ingeniería y Arquitectura.
