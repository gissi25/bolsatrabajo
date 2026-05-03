# BT - Bolsa de Trabajo

Aplicación Android nativa para la gestión de una bolsa de trabajo, desarrollada en **Kotlin** con arquitectura **MVVM**, base de datos **SQLite** local y **Material Design 3**.

---

## Arquitectura

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
│       │   └── si.db                    # BD SQLite pre-poblada
│       ├── java/sv/ues/fia/eisi/bt/
│       │   ├── BTApplication.kt         # Application class
│       │   ├── MainActivity.kt          # Actividad única con NavHostFragment
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── ConnectionHelper.kt   # Wrapper SQLite
│       │   │   │   ├── dao/                  # 22 DAOs
│       │   │   │   └── entities/             # 22 data classes
│       │   │   └── repository/
│       │   │       └── MainRepository.kt     # CRUD genérico
│       │   ├── ui/
│       │   │   ├── auth/              # LoginFragment, RegisterFragment
│       │   │   ├── crud/              # TableDetailFragment, EditorDialogFragment,
│       │   │   │                         DeleteConfirmDialog, TableAdapter
│       │   │   └── dashboard/         # DashboardFragment, DashboardAdapter
│       │   ├── utils/
│       │   │   ├── Constants.kt       # Constantes de la app
│       │   │   ├── InputMaskUtils.kt  # Máscaras (teléfono, NIT, DUI, etc.)
│       │   │   ├── PasswordHasher.kt  # Hashing PBKDF2
│       │   │   ├── StyledToast.kt     # Toast adaptable al tema
│       │   │   ├── ThemeToggleHelper.kt  # Helper cambio modo claro/oscuro
│       │   │   ├── TriggerErrorTranslator.kt  # Traducción errores SQLite
│       │   │   └── ValidationRules.kt # Reglas de validación por tabla
│       │   └── viewmodel/
│       │       ├── AuthViewModel.kt
│       │       ├── CrudViewModel.kt
│       │       └── DashboardViewModel.kt
│       ├── res/
│       │   ├── layout/            # 10 layouts XML
│       │   ├── drawable/          # 11 drawables (iconos, fondos)
│       │   ├── mipmap-*/          # Icono de la app (logo_bt)
│       │   ├── values/            # colors.xml, strings.xml, themes.xml
│       │   ├── values-night/      # themes.xml (modo oscuro)
│       │   └── navigation/        # nav_graph.xml
│       └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

---

## Base de datos

22 tablas SQLite pre-pobladas en `assets/si.db`. Se copia al almacenamiento interno en el primer inicio.

### Tablas

| Tabla | Propósito |
|-------|-----------|
| `USUARIO` | Autenticación (username, password hash, rol) |
| `POSTULANTE` | Datos personales del solicitante |
| `EMPRESA` | Información de la empresa |
| `OFERTA_TRABAJO` | Ofertas de empleo publicadas |
| `POSTULACION` | Postulaciones a ofertas |
| `HABILIDAD` | Catálogo de habilidades |
| `HABILIDAD_POSTULANTE` | Habilidades por postulante con nivel 1-3 |
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
| `MUNICIPIO` | Municipios |
| `DISTRITO` | Distritos |

### Triggers (22)

| Trigger | Validación |
|---------|------------|
| `TR_INS_EMPRESA` | Evita NIT/nombre de empresa vacío o duplicado |
| `TR_MIN_EMPRESA` | Normaliza nombre y contacto a minúsculas |
| `TR_DEL_EMPRESA` | Elimina ofertas y experiencia laboral en cascada |
| `TR_INS_OFERTA_TRABAJO` | Evita título vacío o duplicado por empresa |
| `TR_UPD_OFERTA_TRABAJO` | Evita título duplicado al actualizar |
| `TR_OFERTA_RANGO_EDAD` | Valida edad_min ≤ edad_max |
| `TR_OFERTA_VIGENCIA` | Valida fecha_caducidad > fecha_publicacion |
| `TR_OFERTA_FECHA_AUTO` | Fecha de publicación obligatoria |
| `TR_DEL_OFERTA_TRABAJO` | Elimina requisitos y postulaciones en cascada |
| `TR_INS_CERTIFICACION` | Código 14 dígitos, no vacío, no duplicado |
| `TR_INS_POSTULACION` | Evita postulaciones duplicadas |
| `TR_MIN_POSTULACION` | Normaliza estado_proceso a minúsculas |
| `TR_POSTULANTE_EDAD` | Valida mayoría de edad (≥ 18) |
| `TR_POSTULANTE_EMAIL` | Valida formato email |
| `TR_DEL_POSTULANTE` | Elimina todos los datos asociados en cascada |
| `TR_USUARIO_FORMATO` | Contraseña ≥ 8 caracteres |
| `TR_MUNICIPIO_DEPTO` | Valida que el departamento exista |
| `TR_HABILIDAD_NIVEL` | Nivel de destreza 1, 2 o 3 |
| `TR_DEL_GRADO` | Elimina ofertas al borrar grado académico |
| `TR_DEL_INSTITUCION` | Elimina certificaciones al borrar institución |
| `TR_DEL_CATEGORIA` | Elimina habilidades al borrar categoría |
| `TR_DEL_DISTRITO` | Elimina postulantes y empresas al borrar distrito |

---

## Flujo de navegación

```
Login ──→ Register
  │
  └──→ Dashboard ──→ TableDetail (CRUD)
```

---

## Características implementadas

### Autenticación
- Registro con username, contraseña y rol (postulante, empresa, admin)
- Login con verificación de hash PBKDF2
- Sesión persistente en SharedPreferences
- Logout con diálogo de confirmación

### CRUD Genérico
- Editor dinámico que genera campos según las columnas de la tabla
- PK auto-generado (MAX+1), campos PK ocultos en formularios
- FK dropdowns con búsqueda autocompletable
- Filtros dependientes en cascada:
  - Departamento → Municipio → Distrito
  - Empresa → Oferta de Trabajo (en POSTULACION y DETALLE_REQUISITO)
- Validación de campos requeridos por tabla
- Input masks automáticos:
  - **CONTACTO_DIRECTO**: formato `XXXX-XXXX` (8 dígitos + guion)
  - **NIT (EMPRESA)**: 14 dígitos numéricos, sin guiones
  - **CODIGO_CERTIFICACION**: 14 dígitos exactos, solo números
  - **Teléfono**: formato `XXXX-XXXX`
  - **DUI**: formato `XXXXXXXX-X`
- DatePicker con MaterialDatePicker
- Dropdowns especiales: NIVEL_DESTREZA (Básico/Intermedio/Avanzado), ESTADO_PROCESO

### Interfaz y Temas
- **Material Design 3** con modo claro/oscuro
- **Toggle de tema en todas las pantallas:**
  - Login: botón 🌙/☀️ en esquina superior derecha
  - Registro: botón 🌙/☀️ en esquina superior derecha
  - Dashboard: botón 🌙/☀️ a la derecha del toolbar
  - CRUD de tabla: botón 🌙/☀️ en el toolbar
- **Logout en Dashboard:** botón 🚪 con icono `cerrar_sesion_o` / `cerrar_sesion_c` según el tema
- **Icono de la app:** `logo_bt.png` con fondo blanco, adaptive icon para todas las densidades
- Cards, FAB, SearchView, RecyclerView
- StyledToast adaptable al tema
- Colores dinámicos vía atributos `?attr/`

### Base de Datos
- 22 tablas con 22 triggers de integridad
- PKs auto-incrementales (lógica MAX+1 en Kotlin, no AUTOINCREMENT de SQLite)
- Validaciones vía triggers (formato email, edad mínima, unicidad, etc.)
- Protección de borrado en cascada
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
# Windows
gradlew.bat assembleDebug

# Linux/macOS
./gradlew assembleDebug
```

El APK generado estará en `app/build/outputs/apk/debug/app-debug.apk`.

---

## Licencia

Proyecto académico — Universidad de El Salvador, Facultad de Ingeniería y Arquitectura.
