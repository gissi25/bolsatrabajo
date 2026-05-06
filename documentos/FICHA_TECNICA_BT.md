# FICHA TÉCNICA - BOLSA DE TRABAJO (BT)

## 1. DATOS GENERALES DEL PROYECTO

| Campo | Valor |
|-------|-------|
| **Nombre del proyecto** | BT (Bolsa de Trabajo) |
| **Paquete base** | `sv.ues.fia.eisi.bt` |
| **Institución** | Universidad de El Salvador (UES) - Facultad de Ingeniería y Arquitectura (FIA) - Escuela de Ingeniería de Sistemas Informáticos (EISI) |
| **Plataforma** | Android Nativo |
| **Lenguaje** | Kotlin 100% (0 archivos Java) |
| **Min SDK** | 24 |
| **Target SDK** | 36 |
| **Sistema de compilación** | Gradle Kotlin DSL (`.kts`) |
| **Arquitectura** | MVVM (Model-View-ViewModel) + Repository Pattern |
| **Base de datos** | SQLite (raw) via `SQLiteOpenHelper` |
| **Nombre BD** | `bolsadetabajo.db` |
| **Versión BD** | 10 |
| **Tablas** | 22 |
| **Índices** | 22 |
| **Triggers** | 14 (28 definiciones: INSERT + UPDATE) |
| **Restricciones FK declarativas** | 25 |
| **Autenticación** | PBKDF2WithHmacSHA256, 65536 iteraciones, salt 16 bytes |
| **Roles de usuario** | `administrador`, `postulante`, `gerente de empresa` |

---

## 2. ESTRUCTURA DEL PROYECTO

```
app/src/main/java/sv/ues/fia/eisi/bt/
├── BTApplication.kt                          # Application class
├── MainActivity.kt                           # Activity principal
├── data/
│   ├── local/
│   │   ├── ConnectionHelper.kt               # SQLiteOpenHelper (22 tablas, 22 índices, 14 triggers)
│   │   ├── dao/                              # 17 DAO classes (no utilizadas por el flujo principal)
│   │   └── entities/                         # 19 Entity data classes
│   └── repository/
│       └── MainRepository.kt                 # ~1050 líneas, lógica central CRUD
├── ui/
│   ├── auth/                                 # LoginFragment, RegisterFragment
│   ├── crud/                                 # EditorDialogFragment, TableDetailFragment, etc.
│   └── dashboard/                            # DashboardFragment, DashboardAdapter
├── utils/
│   ├── Constants.kt                          # Nombres de tablas, columnas, PKs, roles
│   ├── ValidationRules.kt                    # Reglas de validación por campo/tabla
│   ├── TriggerErrorTranslator.kt             # Traducción errores SQLite → español
│   ├── InputMaskUtils.kt                     # Máscaras: DUI, NIT, teléfono, validaciones
│   ├── PasswordHasher.kt                     # PBKDF2 con SHA-256
│   ├── StyledToast.kt                        # Toast personalizados
│   └── ThemeToggleHelper.kt                  # Alternar tema claro/oscuro
└── viewmodel/
    ├── AuthViewModel.kt                      # Login/Register logic
    ├── CrudViewModel.kt                      # CRUD operations logic
    └── DashboardViewModel.kt                 # Dashboard logic
```

---

## 3. TABLA DE TABLAS - CLAVES PRIMARIAS, COMPUESTAS Y FORÁNEAS

| # | Tabla | Tipo PK | Columnas PK | Columnas FK | Tabla Referenciada | Columnas Referenciadas |
|---|-------|---------|-------------|-------------|-------------------|----------------------|
| 1 | **CATEGORIA_HABILIDAD** | AUTOINCREMENT | `ID_CATEGORIA_HABILIDAD` | — | — | — |
| 2 | **GENERO** | AUTOINCREMENT | `ID_GENERO` | — | — | — |
| 3 | **TIPO_DOCUMENTO** | AUTOINCREMENT | `ID_TIPO_DOCUMENTO` | — | — | — |
| 4 | **DEPARTAMENTO** | AUTOINCREMENT | `ID_DEPARTAMENTO` | — | — | — |
| 5 | **GRADO_ACADEMICO** | AUTOINCREMENT | `ID_GRADO_ACADEMICO` | — | — | — |
| 6 | **RED_SOCIAL** | AUTOINCREMENT | `ID_RED_SOCIAL` | — | — | — |
| 7 | **USUARIO** | AUTOINCREMENT | `ID_USUARIO` | — | — | — |
| 8 | **MUNICIPIO** | COMPUESTA | `ID_DEPARTAMENTO`, `ID_MUNICIPIO` | `ID_DEPARTAMENTO` | DEPARTAMENTO | `ID_DEPARTAMENTO` |
| 9 | **DISTRITO** | COMPUESTA (3) | `ID_DEPARTAMENTO`, `ID_MUNICIPIO`, `ID_DISTRITO` | `ID_DEPARTAMENTO`, `ID_MUNICIPIO` | MUNICIPIO | `ID_DEPARTAMENTO`, `ID_MUNICIPIO` |
| 10 | **OFERTA_TRABAJO** | COMPUESTA | `NIT`, `ID_OFERTA` | `NIT` | EMPRESA | `NIT` |
| | | | | `ID_GRADO_ACADEMICO` | GRADO_ACADEMICO | `ID_GRADO_ACADEMICO` |
| 11 | **DETALLE_REQUISITO** | COMPUESTA (3) | `NIT`, `ID_OFERTA`, `ID_DETALLE` | `NIT`, `ID_OFERTA` | OFERTA_TRABAJO | `NIT`, `ID_OFERTA` |
| 12 | **EXPERIENCIA_LABORAL** | COMPUESTA (3) | `ID_POSTULANTE`, `NIT`, `ID_EXPERIENCIA` | `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| | | | | `NIT` | EMPRESA | `NIT` |
| 13 | **CERTIFICACION** | COMPUESTA (3) | `ID_CERTIFICACION`, `ID_INSTITUCION`, `ID_POSTULANTE` | `ID_INSTITUCION` | INSTITUCION | `ID_INSTITUCION` |
| | | | | `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| 14 | **FORMACION_ACADEMICA** | COMPUESTA | `ID_FORMACION`, `ID_POSTULANTE` | `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| | | | | `ID_OFERTA_ACADEMICA` | OFERTA_ACADEMICA | `ID_OFERTA_ACADEMICA` |
| 15 | **HABILIDAD_POSTULANTE** | COMPUESTA (3) | `ID_CATEGORIA_HABILIDAD`, `ID_HABILIDAD`, `ID_POSTULANTE` | `ID_CATEGORIA_HABILIDAD`, `ID_HABILIDAD` | HABILIDAD | `ID_CATEGORIA_HABILIDAD`, `ID_HABILIDAD` |
| | | | | `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| 16 | **POSTULACION** | SIMPLE (VARCHAR) | `ID_POSTULACION` | `NIT`, `ID_OFERTA` | OFERTA_TRABAJO | `NIT`, `ID_OFERTA` |
| | | | | `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| 17 | **RED_SOCIAL_POSTULANTE** | COMPUESTA | `ID_POSTULANTE`, `ID_RED_SOCIAL` | `ID_POSTULANTE` | POSTULANTE | `ID_POSTULANTE` |
| | | | | `ID_RED_SOCIAL` | RED_SOCIAL | `ID_RED_SOCIAL` |
| 18 | **INSTITUCION** | MANUAL (VARCHAR) | `ID_INSTITUCION` | — | — | — |
| 19 | **HABILIDAD** | COMPUESTA | `ID_CATEGORIA_HABILIDAD`, `ID_HABILIDAD` | `ID_CATEGORIA_HABILIDAD` | CATEGORIA_HABILIDAD | `ID_CATEGORIA_HABILIDAD` |
| 20 | **EMPRESA** | MANUAL (VARCHAR) | `NIT` | `ID_DISTRITO_DEPTO`, `ID_DISTRITO_MUNICIPIO`, `ID_DISTRITO_ID` | DISTRITO | `ID_DEPARTAMENTO`, `ID_MUNICIPIO`, `ID_DISTRITO` |
| 21 | **POSTULANTE** | MANUAL (VARCHAR) | `ID_POSTULANTE` | `ID_GENERO` | GENERO | `ID_GENERO` |
| | | | | `ID_TIPO_DOCUMENTO` | TIPO_DOCUMENTO | `ID_TIPO_DOCUMENTO` |
| | | | | `ID_DISTRITO_DEPTO`, `ID_DISTRITO_MUNICIPIO`, `ID_DISTRITO_ID` | DISTRITO | `ID_DEPARTAMENTO`, `ID_MUNICIPIO`, `ID_DISTRITO` |
| 22 | **OFERTA_ACADEMICA** | MANUAL (VARCHAR) | `ID_OFERTA_ACADEMICA` | `ID_INSTITUCION` | INSTITUCION | `ID_INSTITUCION` |
| | | | | `ID_GRADO_ACADEMICO` | GRADO_ACADEMICO | `ID_GRADO_ACADEMICO` |

### Resumen de tipos de PK:
- **AUTOINCREMENT (7):** CATEGORIA_HABILIDAD, GENERO, TIPO_DOCUMENTO, DEPARTAMENTO, GRADO_ACADEMICO, RED_SOCIAL, USUARIO
- **COMPUESTA (9):** MUNICIPIO (2), DISTRITO (3), OFERTA_TRABAJO (2), DETALLE_REQUISITO (3), EXPERIENCIA_LABORAL (3), CERTIFICACION (3), FORMACION_ACADEMICA (2), HABILIDAD_POSTULANTE (3), HABILIDAD (2), RED_SOCIAL_POSTULANTE (2)
- **MANUAL VARCHAR (5):** INSTITUCION, EMPRESA, POSTULANTE, OFERTA_ACADEMICA
- **SIMPLE VARCHAR (1):** POSTULACION

---

## 4. TABLA DE TRIGGERS - CLASIFICACIÓN Y PROPÓSITO

### 4.1 TRIGGERS SEMÁNTICOS (6) — Validan reglas de negocio

| # | Nombre Trigger | Tabla | Evento | Propósito | Mensaje de error |
|---|---------------|-------|--------|-----------|-----------------|
| 1 | `TR_POSTULANTE_EDAD` | POSTULANTE | BEFORE INSERT | Valida que `FECHA_NACIMIENTO` no sea una fecha futura | `"La fecha de nacimiento no puede ser futura"` |
| | `TR_POSTULANTE_EDAD_UPD` | POSTULANTE | BEFORE UPDATE | Valida que el postulante tenga ≥ 18 años | `"El postulante debe ser mayor de edad"` |
| 2 | `TR_OFERTA_RANGO_EDAD` | OFERTA_TRABAJO | BEFORE INSERT | Valida que `EDAD_MINIMA <= EDAD_MAXIMA` | `"Edad minima no puede ser mayor a la maxima"` |
| | `TR_OFERTA_RANGO_EDAD_UPD` | OFERTA_TRABAJO | BEFORE UPDATE | (misma validación en UPDATE) | (mismo mensaje) |
| 3 | `TR_OFERTA_VIGENCIA` | OFERTA_TRABAJO | BEFORE INSERT | Valida que `FECHA_CADUCIDAD > FECHA_PUBLICACION` | `"La oferta ya caduco o fecha invalida"` |
| | `TR_OFERTA_VIGENCIA_UPD` | OFERTA_TRABAJO | BEFORE UPDATE | (misma validación en UPDATE) | (mismo mensaje) |
| 4 | `TR_EXP_LABORAL_FECHAS` | EXPERIENCIA_LABORAL | BEFORE INSERT | Valida que `FECHA_INICIO < FECHA_FIN` | `"Fecha inicio debe ser menor a fecha fin"` |
| | `TR_EXP_LABORAL_FECHAS_UPD` | EXPERIENCIA_LABORAL | BEFORE UPDATE | (misma validación en UPDATE) | (mismo mensaje) |
| 5 | `TR_HABILIDAD_NIVEL` | HABILIDAD_POSTULANTE | BEFORE INSERT | Valida que `NIVEL_DESTREZA` ∈ {Básico, Intermedio, Avanzado} | `"Nivel de destreza debe ser Basico, Intermedio o Avanzado"` |
| | `TR_HABILIDAD_NIVEL_UPD` | HABILIDAD_POSTULANTE | BEFORE UPDATE | (misma validación en UPDATE) | (mismo mensaje) |
| 6 | `TR_USUARIO_FORMATO` | USUARIO | BEFORE INSERT | Valida que `PASSWORD` tenga ≥ 8 caracteres | `"Password minimo 8 caracteres"` |
| | `TR_USUARIO_FORMATO_UPD` | USUARIO | BEFORE UPDATE | (misma validación en UPDATE) | (mismo mensaje) |

### 4.2 TRIGGERS DE ACTUALIZACIÓN EN CASCADA (3) — Eliminación en cascada

| # | Nombre Trigger | Tabla Padre | Evento | Tablas Hijas Afectadas | Propósito |
|---|---------------|-------------|--------|----------------------|-----------|
| 7 | `TR_DEL_DEPARTAMENTO` | DEPARTAMENTO | BEFORE DELETE | MUNICIPIO | Elimina todos los municipios pertenecientes al departamento eliminado |
| 8 | `TR_DEL_CATEGORIA` | CATEGORIA_HABILIDAD | BEFORE DELETE | HABILIDAD_POSTULANTE, HABILIDAD | Elimina todas las habilidades y sus asignaciones de la categoría eliminada |
| 9 | `TR_DEL_POSTULANTE` | POSTULANTE | BEFORE DELETE | POSTULACION, EXPERIENCIA_LABORAL, FORMACION_ACADEMICA, CERTIFICACION, HABILIDAD_POSTULANTE, RED_SOCIAL_POSTULANTE | Elimina todos los registros hijos asociados al postulante (6 tablas) |

### 4.3 TRIGGERS DE INTEGRIDAD REFERENCIAL (5) — Verifican existencia de FK

| # | Nombre Trigger | Tabla | Evento | FK Verificada | Tabla Referencia | Mensaje de error |
|---|---------------|-------|--------|--------------|-----------------|-----------------|
| 10 | `TR_MUNICIPIO_DEPTO` | MUNICIPIO | BEFORE INSERT | `ID_DEPARTAMENTO` | DEPARTAMENTO | `"El departamento asociado no existe"` |
| | `TR_MUNICIPIO_DEPTO_UPD` | MUNICIPIO | BEFORE UPDATE | (misma verificación) | (misma) | (mismo mensaje) |
| 11 | `TR_DISTRITO_MUNICIPIO` | DISTRITO | BEFORE INSERT | `(ID_DEPARTAMENTO, ID_MUNICIPIO)` | MUNICIPIO | `"El municipio asociado no existe"` |
| | `TR_DISTRITO_MUNICIPIO_UPD` | DISTRITO | BEFORE UPDATE | (misma verificación) | (misma) | (mismo mensaje) |
| 12 | `TR_HABILIDAD_CATEGORIA` | HABILIDAD | BEFORE INSERT | `ID_CATEGORIA_HABILIDAD` | CATEGORIA_HABILIDAD | `"La categoria asociada no existe"` |
| | `TR_HABILIDAD_CATEGORIA_UPD` | HABILIDAD | BEFORE UPDATE | (misma verificación) | (misma) | (mismo mensaje) |
| 13 | `TR_EMPRESA_DISTRITO` | EMPRESA | BEFORE INSERT | `(ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID)` | DISTRITO | `"El distrito asociado no existe"` |
| | `TR_EMPRESA_DISTRITO_UPD` | EMPRESA | BEFORE UPDATE | (misma verificación) | (misma) | (mismo mensaje) |
| 14 | `TR_POSTULANTE_FK` | POSTULANTE | BEFORE INSERT | `ID_GENERO`, `ID_TIPO_DOCUMENTO` | GENERO, TIPO_DOCUMENTO | `"El genero asociado no existe"`, `"El tipo de documento asociado no existe"` |
| | `TR_POSTULANTE_FK_UPD` | POSTULANTE | BEFORE UPDATE | (misma verificación) | (mismas) | (mismos mensajes) |

### Nota sobre la arquitectura de triggers:
- Cada trigger **semántico** y de **integridad referencial** tiene dos variantes: `_INSERT` y `_UPD` (total 28 definiciones)
- Los triggers de **cascada** sólo aplican a `BEFORE DELETE`
- SQLite no soporta `FOREIGN KEY ON DELETE CASCADE` de forma nativa en todas las configuraciones, por lo que se implementó vía triggers

---

## 5. TABLA DE VALIDACIONES A NIVEL DE CÓDIGO

### 5.1 ValidationRules.kt — Reglas declarativas por campo

| Tabla | Campo(s) | Requerido | Longitud | Patrón (Regex) | Rango numérico |
|-------|----------|-----------|----------|----------------|---------------|
| USUARIO | USERNAME | ✓ | min 3 | — | — |
| USUARIO | PASSWORD | ✓ | min 8 | — | — |
| USUARIO | ROL | ✓ | — | — | — |
| POSTULANTE | ID_POSTULANTE | ✓ | max 7 | `^[A-Z]{2}\d{5}$` | — |
| POSTULANTE | NOMBRE, APELLIDO | ✓ | — | — | — |
| POSTULANTE | NUM_DOCUMENTO | ✓ | — | — | — |
| POSTULANTE | EMAIL | ✓ | — | `^[^@]+@[^@]+\.[^@]+$` | — |
| POSTULANTE | FECHA_NACIMIENTO | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | — |
| POSTULANTE | ID_GENERO, ID_TIPO_DOCUMENTO | ✓ | — | — | — |
| POSTULANTE | NUP, DIRECCION_DETALLE | ✓ | — | — | — |
| POSTULANTE | TELEFONO_CASA, TELEFONO_CELULAR | ✓ | — | — | — |
| EMPRESA | NIT | ✓ | min 14, max 14 | `^\d{14}$` | — |
| EMPRESA | NOMBRE_EMPRESA | ✓ | — | — | — |
| EMPRESA | CONTACTO_DIRECTO | ✓ | min 9, max 9 | `^\d{4}-\d{4}$` | — |
| OFERTA_TRABAJO | ID_OFERTA | ✓ | max 10 | `^OF\d{2,}$` | — |
| OFERTA_TRABAJO | TITULO_PUESTO | ✓ | — | — | — |
| OFERTA_TRABAJO | FECHA_PUBLICACION, FECHA_CADUCIDAD | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | — |
| OFERTA_TRABAJO | EXPERIENCIA_ANIOS | ✓ | — | — | — |
| OFERTA_TRABAJO | EDAD_MINIMA | — | — | — | 16–100 |
| OFERTA_TRABAJO | EDAD_MAXIMA | — | — | — | 16–100 |
| CATEGORIA_HABILIDAD | NOMBRE_CATEGORIA | ✓ | — | — | — |
| GENERO | NOMBRE_GENERO | ✓ | — | — | — |
| TIPO_DOCUMENTO | NOMBRE_TIPO | ✓ | — | — | — |
| DEPARTAMENTO | NOMBRE_DEPARTAMENTO | ✓ | — | — | — |
| MUNICIPIO | ID_MUNICIPIO | ✓ | — | — | — |
| MUNICIPIO | NOMBRE_MUNICIPIO | ✓ | — | — | — |
| DISTRITO | ID_DISTRITO | ✓ | — | — | — |
| DISTRITO | NOMBRE_DISTRITO | ✓ | — | — | — |
| INSTITUCION | ID_INSTITUCION | ✓ | max 20 | `^[A-Za-z]{2,}\d{2,}$` | — |
| INSTITUCION | NOMBRE_INSTITUCION | ✓ | — | — | — |
| GRADO_ACADEMICO | NOMBRE_GRADO | ✓ | — | — | — |
| RED_SOCIAL | NOMBRE_RED | ✓ | — | — | — |
| HABILIDAD | ID_CATEGORIA_HABILIDAD | ✓ | — | — | — |
| HABILIDAD | ID_HABILIDAD | ✓ | max 10 | `^H\d{2,}$` | — |
| HABILIDAD | NOMBRE_HABILIDAD | ✓ | — | — | — |
| HABILIDAD_POSTULANTE | NIVEL_DESTREZA | ✓ | — | — | — |
| CERTIFICACION | ID_CERTIFICACION | ✓ | max 10 | `^C\d{3,}$` | — |
| CERTIFICACION | NOMBRE_CERTIFICACION | ✓ | — | — | — |
| CERTIFICACION | FECHA_CERTIFICACION | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | — |
| EXPERIENCIA_LABORAL | ID_EXPERIENCIA | ✓ | max 10 | `^EL\d{2,}$` | — |
| EXPERIENCIA_LABORAL | PUESTO_TRABAJO | ✓ | — | — | — |
| EXPERIENCIA_LABORAL | FECHA_INICIO, FECHA_FIN | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | — |
| EXPERIENCIA_LABORAL | DESCP_EXPERIENCIA_LABORAL | ✓ | — | — | — |
| EXPERIENCIA_LABORAL | CONTACTO_REFERENCIA | ✓ | — | — | — |
| FORMACION_ACADEMICA | ID_FORMACION | ✓ | max 10 | `^FOA\d{3,}$` | — |
| FORMACION_ACADEMICA | TITULO_OBTENIDO | ✓ | — | — | — |
| FORMACION_ACADEMICA | FECHA_OBTENCION | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | — |
| OFERTA_ACADEMICA | ID_OFERTA_ACADEMICA | ✓ | max 10 | `^OFA\d{2,}$` | — |
| POSTULACION | ID_POSTULACION | ✓ | max 10 | `^POS\d{3,}$` | — |
| POSTULACION | FECHA_APLICACION | ✓ | — | `^\d{4}-\d{2}-\d{2}$` | — |
| POSTULACION | ESTADO_PROCESO | ✓ | — | — | — |
| DETALLE_REQUISITO | ID_DETALLE | ✓ | max 10 | `^D\d{1,}$` | — |
| DETALLE_REQUISITO | DESCRIPCION_REQUISITO | ✓ | — | — | — |
| RED_SOCIAL_POSTULANTE | URL_PERFIL | ✓ | — | `^https?://.*` | — |

### 5.2 InputMaskUtils.kt — Formateo automático de entrada

| Función | Descripción | Formato |
|---------|-------------|---------|
| `formatDUI()` | Formatea DUI: 8 dígitos + guión + dígito verificador | `########-#` |
| `formatNIT()` | Formatea NIT con guiones posicionales | `####-######-###-#` |
| `formatTelefono()` | Formatea teléfono salvadoreño | `####-####` |
| `formatNitSimple()` | Extrae solo dígitos de NIT (hasta 14) | `############` |
| `validateRango()` | Valida rango numérico min/max | Número entre `min` y `max` |
| `validatePassword()` | Valida longitud mínima de contraseña | ≥ 8 caracteres |
| `validateEmail()` | Valida email vía `android.util.Patterns.EMAIL_ADDRESS` | Formato email |
| `validateURL()` | Valida que URL comience con `http://` o `https://` | Prefijo HTTP/HTTPS |
| `validateFecha()` | Valida formato fecha ISO | `AAAA-MM-DD` |

### 5.3 EditorDialogFragment.kt — Filtros de caracteres por campo

| Columna(s) | Filtro | Longitud máxima |
|------------|--------|----------------|
| TELEFONO*, TEL*, CONTACTO_REFERENCIA, CONTACTO_DIRECTO | Sólo dígitos + guión | 9 |
| NIT (EMPRESA) | Sólo dígitos | 14 |
| NUP | Sólo dígitos | 12 |
| NUM_DOCUMENTO | Sólo dígitos + guión (según tipo DUI/NIT) | 10–17 |
| EXPERIENCIA_ANIOS | Sólo dígitos | 2 |
| EDAD_MINIMA, EDAD_MAXIMA | Sólo dígitos | 2 |
| NIVEL_DESTREZA | — | 12 |
| CERTIFICACION, CODIGO | — | 30 |

### 5.4 EditorDialogFragment.kt — Tipos de teclado por campo

| Columna(s) | `android.text.InputType` |
|------------|-------------------------|
| ID_HABILIDAD, ID_POSTULANTE, ID_INSTITUCION, ID_OFERTA_ACADEMICA, ID_POSTULACION, ID_OFERTA, ID_FORMACION, ID_CERTIFICACION, ID_EXPERIENCIA, ID_DETALLE, NIT | `TYPE_CLASS_TEXT` |
| ID_GENERO, ID_TIPO_DOCUMENTO, ID_GRADO_ACADEMICO, ID_RED_SOCIAL, ID_CATEGORIA_HABILIDAD, ID_USUARIO, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID, ID_DEPARTAMENTO, ID_MUNICIPIO, NUP | `TYPE_CLASS_NUMBER` |
| EMAIL* | `TYPE_CLASS_TEXT \| TYPE_TEXT_VARIATION_EMAIL_ADDRESS` |
| TELEFONO*, TEL*, CONTACTO_REFERENCIA, CONTACTO_DIRECTO | `TYPE_CLASS_PHONE` |
| PASSWORD*, CONTRA* | `TYPE_CLASS_TEXT \| TYPE_TEXT_VARIATION_PASSWORD` |
| EXPERIENCIA_ANIOS, EDAD_MIN*, EDAD_MAX* | `TYPE_CLASS_NUMBER` |
| DESCRIPCION*, DETALLE*, DESC*, REQUISITO* | `TYPE_CLASS_TEXT \| TYPE_TEXT_FLAG_MULTI_LINE \| TYPE_TEXT_FLAG_CAP_SENTENCES` |

### 5.5 Validación de formulario en EditorDialogFragment.kt:saveData()

| Validación | Descripción |
|------------|-------------|
| **Control de acceso por rol** | Verifica que el usuario tenga permiso `FULL` sobre la tabla antes de guardar |
| **Dropdown FK vacío** | Si un dropdown FK está en blanco, muestra error y bloquea guardado |
| **Dropdown FK sin datos** | Si la tabla FK no tiene registros, muestra "No hay datos en X. Créelos primero." |
| **Opción FK inválida** | Si el texto del dropdown no coincide con ninguna opción, muestra error |
| **Validación de campo** | Llama `ValidationRules.validate()` para cada campo de texto y muestra error inline en el `TextInputLayout` |
| **NIVEL_DESTREZA vacío** | Si no selecciona nivel, bloquea guardado |
| **ESTADO_PROCESO vacío** | Si no selecciona estado, bloquea guardado |

### 5.6 Validación de tipo de documento dinámica

En `EditorDialogFragment.kt:refreshNumDocHintAndValidation()`:
- **Hint dinámico**: Cambia la etiqueta del campo NUM_DOCUMENTO según el tipo de documento seleccionado (DUI, NIT, PASAPORTE)
- **Filtros dinámicos**: Aplica filtros de dígitos+guión para DUI y NIT; sin filtro para PASAPORTE
- **Longitud dinámica**: Limita a 10 caracteres para DUI, 17 para NIT
- **Formateo automático**: Aplica máscara DUI o NIT según el tipo
- **Validación en tiempo real**: Muestra error inline mientras escribe

### 5.7 Validación de autenticación

| Archivo | Validación |
|---------|-----------|
| `LoginFragment.kt:validateInput()` | Username no vacío, Password no vacío |
| `RegisterFragment.kt:validateInput()` | Username no vacío, Password no vacío, Password ≥ 8 caracteres, Confirmación de password coincide |
| `AuthViewModel.kt:login()` | Verifica campos vacíos antes de llamar al repositorio |
| `AuthViewModel.kt:register()` | Verifica campos vacíos, Password ≥ 8 caracteres antes de llamar al repositorio |

### 5.8 Capa de traducción de errores (TriggerErrorTranslator.kt)

| Error original (SQLite/Trigger) | Mensaje traducido al español |
|-------------------------------|------------------------------|
| `UNIQUE constraint failed` | `"Ya existe un registro con esos datos"` |
| `FOREIGN KEY constraint failed` | `"NO ES POSIBLE ELIMINAR "` |
| `NOT NULL constraint failed` | `"Un campo obligatorio esta vacio"` |
| `La fecha de nacimiento no puede ser futura` | `"La fecha..."` (se mantiene) |
| `El postulante debe ser mayor de edad` | `"El postulante debe ser mayor de 18 anos"` |
| `Password minimo 8 caracteres` | `"La contrasena debe tener al menos 8 caracteres"` |
| `Nivel de destreza debe ser 1, 2 o 3` | `"El nivel de destreza debe ser 1 (Basico), 2 (Intermedio) o 3 (Avanzado)"` |
| *30+ reglas de coincidencia parcial* | *Mensajes legibles y contextuales* |

### 5.9 Seguridad — PasswordHasher.kt

| Parámetro | Valor |
|-----------|-------|
| Algoritmo | `PBKDF2WithHmacSHA256` |
| Iteraciones | 65,536 |
| Longitud de llave | 256 bits |
| Longitud de salt | 16 bytes (128 bits) |
| Almacenamiento | `salt:hash` (hex) |
| Fallback | SHA-256 (si no disponible PBKDF2) |

---

## 6. RESUMEN DE LA ARQUITECTURA DE VALIDACIÓN

```
┌─────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN                    │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │LoginFragment │  │RegisterFrag. │  │EditorDialog   │  │
│  │· validate()  │  │· validate()  │  │· saveData()   │  │
│  └──────────────┘  └──────────────┘  │· getFilters() │  │
│                                      │· getInputType │  │
│                                      │· formatInput  │  │
│                                      │· fieldError() │  │
│                                      └───────┬───────┘  │
├──────────────────────────────────────────────┼──────────┤
│                 CAPA DE VIEWMODEL             │          │
│  ┌──────────────────┐   ┌────────────────┐    │          │
│  │ AuthViewModel     │   │ CrudViewModel  │    │          │
│  │· login() validate │   │· insertRecord() │   │          │
│  │· register() val.  │   │· updateRecord() │   │          │
│  └──────────────────┘   └────────────────┘    │          │
├──────────────────────────────────────────────┼──────────┤
│               CAPA DE UTILIDADES              │          │
│  ┌──────────────────┐   ┌────────────────┐    │          │
│  │ ValidationRules   │   │ InputMaskUtils │    │          │
│  │· 50+ field rules  │   │· 4 formatters  │    │          │
│  │· validate()       │   │· 6 validators  │    │          │
│  └──────────────────┘   └────────────────┘    │          │
│  ┌──────────────────┐   ┌────────────────┐    │          │
│  │TriggerTranslator  │   │PasswordHasher  │    │          │
│  │· 30+ error maps  │   │· PBKDF2        │    │          │
│  └──────────────────┘   └────────────────┘    │          │
├──────────────────────────────────────────────┼──────────┤
│              CAPA DE BASE DE DATOS             │          │
│  ┌──────────────────────────────────────────┐ │          │
│  │          ConnectionHelper.kt              │ │          │
│  │  ┌──────────┐  ┌──────────┐  ┌────────┐ │ │          │
│  │  │22 Tablas │  │22 Índices│  │14 Trig.│ │ │          │
│  │  └──────────┘  └──────────┘  └────────┘ │ │          │
│  │  ┌────────────────────────────────────┐  │ │          │
│  │  │ 6 Semánticos │ 3 Cascada │ 5 IR    │  │ │          │
│  │  └────────────────────────────────────┘  │ │          │
│  └──────────────────────────────────────────┘ │          │
└───────────────────────────────────────────────┴──────────┘

IR = Integridad Referencial
Triggers: 14 (28 contando variantes INSERT+UPDATE)
25 FOREIGN KEY constraints declarativas
```

---

*Documento generado el 06/05/2026 mediante análisis estático del código fuente.*
*Proyecto: BT (Bolsa de Trabajo) — sv.ues.fia.eisi.bt*
