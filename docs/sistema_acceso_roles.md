# Sistema de Control de Acceso por Roles (RBAC)

## Documentación Técnica y Justificación de Diseño

---

## Índice

1. [Introducción](#1-introducción)
2. [Arquitectura General del Sistema](#2-arquitectura-general-del-sistema)
3. [Definición de Roles y Niveles de Acceso](#3-definición-de-roles-y-niveles-de-acceso)
4. [Archivos del Sistema](#4-archivos-del-sistema)
5. [Flujo Completo del Control de Acceso](#5-flujo-completo-del-control-de-acceso)
6. [Diagrama de Flujo](#6-diagrama-de-flujo)
7. [Justificación de Implementación a Nivel de Código](#7-justificación-de-implementación-a-nivel-de-código)
8. [Conclusión](#8-conclusión)

---

## 1. Introducción

Este documento describe el sistema de control de acceso basado en roles (RBAC - Role-Based Access Control) implementado en la aplicación **Bolsa de Trabajo (BT)** para Android. El sistema define qué operaciones puede realizar cada tipo de usuario sobre las diferentes tablas de la base de datos.

### 1.1. Resumen

El sistema maneja **3 roles** de usuario:

| Rol | Constante | Descripción |
|---|---|---|
| Administrador | `ROLE_ADMIN = "administrador"` | Acceso completo a todas las tablas |
| Postulante | `ROLE_POSTULANTE = "postulante"` | Gestiona su perfil y postulaciones |
| Gerente de Empresa | `ROLE_EMPRESA = "gerente de empresa"` | Gestiona su empresa y ofertas |

Y define **3 niveles de acceso** para cada combinación rol-tabla:

| Nivel | Significado |
|---|---|
| `NONE` | Sin acceso (la tabla no aparece en el menú) |
| `READ_ONLY` | Puede ver los registros pero no crear, editar ni eliminar |
| `FULL` | Puede ver, crear, editar y eliminar registros |

---

## 2. Arquitectura General del Sistema

### 2.1. ¿Dónde está definido el control de acceso?

Todo el sistema RBAC está implementado **exclusivamente en el código Kotlin** de la capa de presentación/lógica de la aplicación. No existen tablas de permisos ni consultas SQL que regulen el acceso a nivel de base de datos.

### 2.2. Componentes Clave

```
┌──────────────────────────────────────────────────────────┐
│                    SharedPreferences                      │
│              (Sesión del usuario)                         │
│  Claves: user_role, user_id, username, is_logged_in       │
└────────────────────┬─────────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────────────────────┐
│                    Constants.kt                           │
│         (Corazón del RBAC - getRoleTables())              │
│  Define: roles, niveles de acceso, tablas permitidas      │
└────────────────────┬─────────────────────────────────────┘
                     │
         ┌───────────┼───────────────┐
         ▼           ▼               ▼
┌────────────┐ ┌──────────┐ ┌──────────────┐
│ Dashboard  │ │TableDetail│ │EditorDialog  │
│ ViewModel  │ │ Fragment  │ │ Fragment     │
│ (filtra    │ │(verifica  │ │(verifica en  │
│  tablas)   │ │ canEdit/  │ │  guardado)   │
│            │ │ canDelete)│ │              │
└────────────┘ └──────────┘ └──────────────┘
         │           │               │
         ▼           ▼               ▼
┌──────────────────────────────────────────────────────────┐
│  TableAdapter.kt     DeleteConfirmDialog.kt               │
│  (oculta botones     (bloquea eliminación)                │
│   según permisos)                                         │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Definición de Roles y Niveles de Acceso

### 3.1. Archivo: `Constants.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/utils/Constants.kt`

Este es el archivo central de todo el sistema RBAC. Contiene:

#### 3.1.1. Constantes de Roles (Líneas 16-18)

```kotlin
const val ROLE_ADMIN = "administrador"
const val ROLE_POSTULANTE = "postulante"
const val ROLE_EMPRESA = "gerente de empresa"
```

Estos strings son los valores que se almacenan en la base de datos (columna `ROL` de la tabla `USUARIO`) y también en la sesión de `SharedPreferences`.

#### 3.1.2. Clave de sesión (Línea 14)

```kotlin
const val KEY_USER_ROLE = "user_role"
```

Es la clave usada para guardar y recuperar el rol del usuario desde `SharedPreferences`.

#### 3.1.3. Enum de Niveles de Acceso (Línea 65)

```kotlin
enum class AccessLevel { NONE, READ_ONLY, FULL }
```

Define los tres niveles de permiso que se pueden asignar a cada combinación rol-tabla.

#### 3.1.4. Función Central: `getRoleTables()` (Líneas 133-162)

Esta función es **el núcleo del sistema RBAC**. Recibe un rol y devuelve un `Map<String, AccessLevel>` donde:
- **Key:** nombre de la tabla (ej. `"POSTULANTE"`, `"EMPRESA"`)
- **Value:** nivel de acceso (`NONE`, `READ_ONLY` o `FULL`)

```kotlin
fun getRoleTables(role: String): Map<String, AccessLevel> {
    return when (role) {
        ROLE_ADMIN -> ALL_TABLES.associateWith { AccessLevel.FULL }
        ROLE_POSTULANTE -> mapOf(
            TABLE_POSTULANTE to AccessLevel.FULL,
            TABLE_EXPERIENCIA_LABORAL to AccessLevel.FULL,
            TABLE_FORMACION_ACADEMICA to AccessLevel.FULL,
            TABLE_HABILIDAD_POSTULANTE to AccessLevel.FULL,
            TABLE_CERTIFICACION to AccessLevel.FULL,
            TABLE_RED_SOCIAL_POSTULANTE to AccessLevel.FULL,
            TABLE_POSTULACION to AccessLevel.FULL,
            TABLE_EMPRESA to AccessLevel.READ_ONLY,
            TABLE_OFERTA_TRABAJO to AccessLevel.READ_ONLY,
            TABLE_DETALLE_REQUISITO to AccessLevel.READ_ONLY
        )
        ROLE_EMPRESA -> mapOf(
            TABLE_EMPRESA to AccessLevel.FULL,
            TABLE_OFERTA_TRABAJO to AccessLevel.FULL,
            TABLE_DETALLE_REQUISITO to AccessLevel.FULL,
            TABLE_POSTULACION to AccessLevel.FULL,
            TABLE_POSTULANTE to AccessLevel.READ_ONLY,
            TABLE_EXPERIENCIA_LABORAL to AccessLevel.READ_ONLY,
            TABLE_FORMACION_ACADEMICA to AccessLevel.READ_ONLY,
            TABLE_HABILIDAD_POSTULANTE to AccessLevel.READ_ONLY,
            TABLE_CERTIFICACION to AccessLevel.READ_ONLY,
            TABLE_RED_SOCIAL_POSTULANTE to AccessLevel.READ_ONLY
        )
        else -> emptyMap()
    }
}
```

#### 3.1.5. Matriz de Acceso Completa

| Tabla | Administrador | Postulante | Gerente de Empresa |
|---|---|---|---|
| CATEGORIA_HABILIDAD | FULL | — | — |
| GENERO | FULL | — | — |
| TIPO_DOCUMENTO | FULL | — | — |
| DEPARTAMENTO | FULL | — | — |
| INSTITUCION | FULL | — | — |
| GRADO_ACADEMICO | FULL | — | — |
| RED_SOCIAL | FULL | — | — |
| MUNICIPIO | FULL | — | — |
| DISTRITO | FULL | — | — |
| HABILIDAD | FULL | — | — |
| TIPO_CERTIFICACION | FULL | — | — |
| OFERTA_ACADEMICA | FULL | — | — |
| USUARIO | FULL | — | — |
| **POSTULANTE** | FULL | **FULL** | READ_ONLY |
| **EXPERIENCIA_LABORAL** | FULL | **FULL** | READ_ONLY |
| **FORMACION_ACADEMICA** | FULL | **FULL** | READ_ONLY |
| **HABILIDAD_POSTULANTE** | FULL | **FULL** | READ_ONLY |
| **CERTIFICACION** | FULL | **FULL** | READ_ONLY |
| **RED_SOCIAL_POSTULANTE** | FULL | **FULL** | READ_ONLY |
| **POSTULACION** | FULL | **FULL** | **FULL** |
| **EMPRESA** | FULL | READ_ONLY | **FULL** |
| **OFERTA_TRABAJO** | FULL | READ_ONLY | **FULL** |
| **DETALLE_REQUISITO** | FULL | READ_ONLY | **FULL** |

#### 3.1.6. Constantes para pasar permisos entre Fragments (Líneas 62-63)

```kotlin
const val BUNDLE_CAN_EDIT = "can_edit"
const val BUNDLE_CAN_DELETE = "can_delete"
```

---

## 4. Archivos del Sistema

### 4.1. Captura del Rol: `LoginFragment.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/auth/LoginFragment.kt`

#### 4.1.1. Líneas 72-79 — Inicio de Sesión Exitoso

Cuando el usuario inicia sesión correctamente, se obtienen sus datos desde la base de datos (incluyendo el rol):

```kotlin
LoginSuccess(usuario) -> {
    saveSession(
        usuario.id_usuario,
        usuario.username,
        usuario.rol    // ← "administrador" | "postulante" | "gerente de empresa"
    )
}
```

#### 4.1.2. Líneas 132-141 — Guardado en Sesión

Aquí es donde se persiste el rol en `SharedPreferences`:

```kotlin
private fun saveSession(userId: Int, username: String, rol: String) {
    val prefs = requireContext().getSharedPreferences(
        Constants.PREFS_NAME, Context.MODE_PRIVATE
    )
    prefs.edit()
        .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
        .putInt(Constants.KEY_USER_ID, userId)
        .putString(Constants.KEY_USERNAME, username)
        .putString(Constants.KEY_USER_ROLE, rol)   // ← Línea 138
        .apply()
}
```

**Importante:** Este es el punto único donde se establece la sesión del rol. Una vez guardado, cualquier componente de la app puede leer el rol desde `SharedPreferences`.

---

### 4.2. Selección del Rol: `RegisterFragment.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/auth/RegisterFragment.kt`

#### 4.2.1. Líneas 117-124 — Poblar el Dropdown de Roles

```kotlin
val roles = arrayOf(
    getString(R.string.rol_postulante),
    getString(R.string.rol_empresa),
    getString(R.string.rol_admin)
)
actvRol.setAdapter(ArrayAdapter(requireContext(),
    android.R.layout.simple_dropdown_item_1line, roles))
actvRol.setText(getString(R.string.rol_postulante), false)
```

#### 4.2.2. Líneas 73-79 — Mapeo del String Seleccionado al Valor del Rol

```kotlin
val selected = actvRol.text.toString().trim()
val rol = when {
    selected == getString(R.string.rol_postulante) -> "postulante"
    selected == getString(R.string.rol_empresa) -> "gerente de empresa"
    selected == getString(R.string.rol_admin) -> "administrador"
    else -> "postulante"
}
```

Este valor `rol` se envía al `ViewModel` que lo inserta en la columna `ROL` de la tabla `USUARIO` en la base de datos.

---

### 4.3. Filtrado del Dashboard: `DashboardViewModel.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/viewmodel/DashboardViewModel.kt`

#### 4.3.1. Líneas 50-59 — Carga de Tablas según el Rol

```kotlin
fun loadTables(role: String? = null) {
    if (role != null) currentRole = role
    viewModelScope.launch {
        val all = repository.getAllTableInfo()
        val roleTables = Constants.getRoleTables(currentRole)  // ← Línea 58
        val filtered = all.filter { roleTables.containsKey(it.name) }  // ← Línea 59
        _allTables.value = filtered
        buildSectionedList()
    }
}
```

**Línea 58:** Obtiene el mapa de tablas permitidas para el rol actual.
**Línea 59:** Filtra la lista completa de 23 tablas, conservando solo aquellas que existen en el mapa del rol.

#### 4.3.2. Líneas 89-117 — Construcción de la Lista con Indicador Read-Only

```kotlin
fun buildSectionedList() {
    val roleTables = Constants.getRoleTables(currentRole)
    // ...
    fun accessLevel(name: String) = roleTables[name] != Constants.AccessLevel.FULL
    // ...
    result.addAll(sortByOrder(catalog, catalogOrder)
        .map { DashboardItem.Table(it, accessLevel(it.name), SECTION_CATALOGOS) })
}
```

**Línea 96:** Crea la función `accessLevel()` que devuelve `true` si la tabla NO tiene acceso `FULL`, marcándola como `isReadOnly = true` en el `DashboardItem.Table`.

**Efecto visual:** Las tarjetas de tablas con solo `READ_ONLY` se muestran con un indicador visual distinto en el dashboard.

---

### 4.4. Ocultar Botones de Administración: `DashboardFragment.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/dashboard/DashboardFragment.kt`

#### 4.4.1. Líneas 87-90 — Ocultar "Insertar Datos de Prueba"

```kotlin
val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE)
    ?: Constants.ROLE_POSTULANTE
if (role != Constants.ROLE_ADMIN) {
    btnInsertScript.visibility = View.GONE  // Solo admin ve este botón
}
```

Solo el **administrador** puede ver y usar el botón para insertar datos de prueba en la base de datos.

---

### 4.5. Verificación de Permisos al Entrar a una Tabla: `TableDetailFragment.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/crud/TableDetailFragment.kt`

Este es **el archivo más importante** para la verificación de acceso a nivel de tabla.

#### 4.5.1. Líneas 57-58 — Variables de Permiso

```kotlin
private var canEdit: Boolean = false
private var canDelete: Boolean = false
```

#### 4.5.2. Líneas 103-121 — Determinación de Permisos

```kotlin
// Leer el rol desde SharedPreferences
val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE)
    ?: Constants.ROLE_POSTULANTE

// Obtener el nivel de acceso para esta tabla según el rol
val access = Constants.getRoleTables(role)[tableName]
    ?: Constants.AccessLevel.NONE

// Si no tiene acceso, mostrar error y regresar
if (access == Constants.AccessLevel.NONE) {
    StyledToast.show(requireContext(), getString(R.string.error_sin_acceso_tabla))
    findNavController().popBackStack()
    return@observe
}

// Solo FULL permite editar y eliminar
canEdit = access == Constants.AccessLevel.FULL
canDelete = access == Constants.AccessLevel.FULL

// Ocultar botón "Agregar" si no puede editar
if (!canEdit) fabAdd.visibility = View.GONE

// Regla especial: Empresa no puede crear nuevas postulaciones
if (role == Constants.ROLE_EMPRESA && tableName == "POSTULACION") {
    fabAdd.visibility = View.GONE
}
```

**Explicación línea por línea:**
- **Línea 107-108:** Lee el rol desde la sesión.
- **Línea 109:** Consulta `getRoleTables(role)` para obtener el `AccessLevel` de esta tabla específica.
- **Líneas 110-113:** Si el acceso es `NONE` (la tabla no está en el mapa del rol), muestra un mensaje de error y navega hacia atrás.
- **Líneas 115-116:** `canEdit` y `canDelete` son `true` SOLO si el acceso es `FULL`. Si es `READ_ONLY`, ambas son `false`.
- **Línea 118:** Si no puede editar, oculta el botón flotante de "Agregar".
- **Línea 119-121:** Regla de negocio: la empresa no puede crear postulaciones desde cero, solo puede cambiar el estado de las existentes.

#### 4.5.3. Líneas 164-167 — Pasar Permisos al Adapter

```kotlin
binding.recyclerView.adapter = TableAdapter(
    emptyList(),
    tableName,
    canEdit = canEdit,      // ← Línea 166
    canDelete = canDelete,  // ← Línea 167
    onViewClick = if (!canEdit) { item, _ -> showViewDialog(item) } else null
)
```

#### 4.5.4. Líneas 475-478 — Guardia en Edición

```kotlin
private fun showEditDialog(item: Map<String, Any?>) {
    if (!canEdit) {
        StyledToast.show(requireContext(), getString(R.string.error_sin_permiso_editar))
        return
    }
    // ... abrir editor
}
```

#### 4.5.5. Líneas 490-496 — Guardia en Eliminación

```kotlin
private fun showDeleteDialog(item: Map<String, Any?>) {
    if (!canDelete) {
        StyledToast.show(requireContext(),
            getString(R.string.error_sin_permiso_eliminar))
        return
    }
    // ... abrir diálogo de confirmación
}
```

---

### 4.6. Ocultar Botones por Fila: `TableAdapter.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/crud/TableAdapter.kt`

#### 4.6.1. Líneas 21-29 — Constructor con Permisos

```kotlin
class TableAdapter(
    private val data: List<Map<String, Any?>>,
    private val tableName: String,
    private val canEdit: Boolean = true,    // ← Línea 23
    private val canDelete: Boolean = true,  // ← Línea 24
    private val onViewClick: ((Map<String, Any?>, Int) -> Unit)? = null
)
```

#### 4.6.2. Líneas 56-57 — Visibilidad de Botones por Fila

```kotlin
fabEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
fabDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
```

Cuando `canEdit` o `canDelete` es `false`, los botones de editar y eliminar no se renderizan en ninguna fila.

#### 4.6.3. Líneas 75-80 — Click en Fila como Solo Vista

```kotlin
} else if (!canEdit && onViewClick != null) {
    onViewClick(item, position)
}
```

Si no puede editar pero hay un `onViewClick`, al tocar la fila se abre un diálogo de solo lectura en lugar del editor.

---

### 4.7. Verificación en el Editor: `EditorDialogFragment.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/crud/EditorDialogFragment.kt`

Este archivo contiene **la verificación más granular** del sistema, incluyendo reglas de negocio específicas para la tabla `POSTULACION`.

#### 4.7.1. Líneas 109-126 — Reglas de POSTULACION por Rol

```kotlin
userRole = requireContext().getSharedPreferences(Constants.PREFS_NAME, ...)
    .getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE)
    ?: Constants.ROLE_POSTULANTE

if (isEditMode && tableName == "POSTULACION") {
    when (userRole) {
        Constants.ROLE_POSTULANTE -> {
            disableAllFields()        // Postulante: TODO deshabilitado
            StyledToast.show(... R.string.solo_empresa_modificar_postulacion)
        }
        Constants.ROLE_EMPRESA -> {
            disableNonEstadoFields()  // Empresa: solo puede editar ESTADO_PROCESO
            StyledToast.show(... R.string.solo_empresa_cambiar_estado)
        }
    }
}
```

#### 4.7.2. Líneas 1598-1615 — `disableNonEstadoFields()`

```kotlin
private fun disableNonEstadoFields() {
    for (i in binding.editableFields.indexOfFirst { it.column == "ESTADO_PROCESO" } ..
         binding.editableFields.lastIndex) {
        // Deshabilita todo excepto el campo ESTADO_PROCESO
    }
}
```

#### 4.7.3. Líneas 1617-1628 — `disableAllFields()`

```kotlin
private fun disableAllFields() {
    binding.editableFields.forEach { (view, _, _) ->
        view.isEnabled = false
    }
}
```

#### 4.7.4. Líneas 1634-1645 — Verificación Final al Guardar

```kotlin
val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE)
    ?: Constants.ROLE_POSTULANTE
val access = Constants.getRoleTables(role)[tableName]
    ?: Constants.AccessLevel.NONE

if (access != Constants.AccessLevel.FULL) {
    StyledToast.show(requireContext(), getString(R.string.sin_permiso_modificar_tabla))
    return@setPositiveButton
}

if (role == Constants.ROLE_POSTULANTE && tableName == "POSTULACION" && isEditMode) {
    StyledToast.show(requireContext(), getString(R.string.sin_permiso_editar_postulacion))
    return@setPositiveButton
}
```

Esta doble verificación garantiza que aunque un usuario malintencionado intente enviar datos modificados, el guardado será rechazado.

#### 4.7.5. Líneas 1669-1671 — Forzar Valor de ESTADO_PROCESO para Postulante

```kotlin
if (role == Constants.ROLE_POSTULANTE && tableName == "POSTULACION") {
    estadoProcesoValue = "activo"  // Fuerza a "activo" sin importar lo que envíe
}
```

---

### 4.8. Verificación en Eliminación: `DeleteConfirmDialog.kt`

**Ruta:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/crud/DeleteConfirmDialog.kt`

#### 4.8.1. Líneas 239-246 — Bloqueo de Eliminación

```kotlin
val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE)
    ?: Constants.ROLE_POSTULANTE
val access = Constants.getRoleTables(role)[tableName]
    ?: Constants.AccessLevel.NONE

if (access != Constants.AccessLevel.FULL) {
    StyledToast.show(requireContext(), getString(R.string.sin_permiso_eliminar))
    dismiss()
    return@setPositiveButton
}
```

Solo los roles con `FULL` pueden eliminar registros. Si es `READ_ONLY` o `NONE`, se cancela la operación.

#### 4.8.2. Líneas 251-258 — Protección de Propio Usuario

```kotlin
if (tableName == "USUARIO") {
    val currentUserId = prefs.getInt(Constants.KEY_USER_ID, -1)
    val idToDelete = (item[primaryKey] as? Number)?.toInt() ?: -1
    if (idToDelete == currentUserId) {
        StyledToast.show(requireContext(),
            getString(R.string.no_eliminar_propio_usuario))
        dismiss()
        return@setPositiveButton
    }
}
```

---

## 5. Flujo Completo del Control de Acceso

### 5.1. Diagrama de Flujo

```
┌──────────────────────────────────────────────────────────────────┐
│                        REGISTRO                                  │
│  RegisterFragment                                                 │
│  - Usuario selecciona rol del dropdown                           │
│  - Se mapea: "Postulante" → "postulante"                        │
│  - Se guarda en DB (tabla USUARIO, columna ROL)                  │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                        INICIO DE SESIÓN                          │
│  LoginFragment                                                    │
│  - Se consulta el usuario en DB                                  │
│  - Se extrae el valor de la columna ROL                          │
│  - Línea 138: se guarda en SharedPreferences:                    │
│    putString(Constants.KEY_USER_ROLE, rol)                      │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                        DASHBOARD                                 │
│  DashboardFragment + DashboardViewModel                          │
│  1. Línea 87: leer rol de SharedPreferences                     │
│  2. Línea 88: si NO es admin, ocultar botón seed data           │
│  3. Línea 123: pasar rol al ViewModel                           │
│  4. Línea 58 (VM): getRoleTables(rol) → obtiene mapa de tablas  │
│  5. Línea 59 (VM): filtrar solo tablas en el mapa               │
│  6. Línea 96 (VM): marcar como readOnly si no es FULL           │
│  7. Mostrar solo las tarjetas de tablas permitidas              │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼ (usuario toca una tarjeta)
┌──────────────────────────────────────────────────────────────────┐
│                      TABLE DETAIL                                │
│  TableDetailFragment                                             │
│  1. Línea 107: leer rol de SharedPreferences                    │
│  2. Línea 109: getRoleTables(rol)[nombreTabla] → AccessLevel    │
│  3. Línea 110-113: si NONE → error y regresar                   │
│  4. Línea 115-116: canEdit = (access == FULL)                   │
│  5. Línea 118: si no canEdit → ocultar FAB de agregar           │
│  6. Línea 166-167: pasar canEdit/canDelete al TableAdapter      │
│  7. Línea 475: showEditDialog() verifica canEdit               │
│  8. Línea 490: showDeleteDialog() verifica canDelete            │
└──────────────────────────┬───────────────────────────────────────┘
         ┌─────────────────┴─────────────────┐
         ▼                                   ▼
┌────────────────────┐           ┌──────────────────────────┐
│   TABLE ADAPTER     │           │   DELETE CONFIRM DIALOG   │
│  (por fila)         │           │                           │
│  Línea 56:          │           │   Línea 242:              │
│  si no canEdit      │           │   si access != FULL       │
│  → ocultar ✏️       │           │   → cancelar eliminación  │
│  Línea 57:          │           │                           │
│  si no canDelete    │           │   Línea 252:              │
│  → ocultar 🗑️       │           │   si es propio usuario    │
│                     │           │   → cancelar eliminación  │
└────────────────────┘           └──────────────────────────┘
         │
         ▼ (toca editar)
┌──────────────────────────────────────────────────────────────────┐
│                      EDITOR DIALOG                               │
│  EditorDialogFragment                                            │
│  1. Línea 109: leer rol de SharedPreferences                    │
│  2. Línea 116-126: si POSTULACION:                               │
│     - Postulante → disableAllFields()                           │
│     - Empresa → disableNonEstadoFields() (solo estado)          │
│  3. Línea 1636-1639: al guardar, verificar FULL otra vez        │
│  4. Línea 1642-1645: postulante NO puede editar POSTULACION     │
│  5. Línea 1670-1671: forzar estado = "activo" si postulante     │
└──────────────────────────────────────────────────────────────────┘
```

### 5.2. Resumen de verificación en 4 capas

| Capa | Archivo | ¿Qué verifica? |
|---|---|---|
| 1. Dashboard | `DashboardViewModel.kt:58` | Qué tablas son visibles para el rol |
| 2. Entrada a tabla | `TableDetailFragment.kt:109` | Nivel de acceso (`NONE`/`READ_ONLY`/`FULL`) |
| 3. Botones por fila | `TableAdapter.kt:56-57` | Muestra/oculta editar/eliminar |
| 4. Acción final | `EditorDialogFragment.kt:1636` y `DeleteConfirmDialog.kt:241` | Bloquea guardado/eliminación si no es FULL |

---

## 6. Archivos Afectados

| # | Archivo | Líneas Clave | Rol en el Sistema |
|---|---|---|---|
| 1 | `app/.../utils/Constants.kt` | 14, 16-18, 62-63, 65, 133-162 | Define roles, niveles de acceso y la función `getRoleTables()` |
| 2 | `app/.../ui/auth/LoginFragment.kt` | 73-75, 132-141 | Guarda el rol en la sesión al iniciar sesión |
| 3 | `app/.../ui/auth/RegisterFragment.kt` | 69-81, 117-124 | Permite seleccionar rol durante el registro |
| 4 | `app/.../viewmodel/DashboardViewModel.kt` | 30, 50-59, 89-96, 139 | Filtra las tablas del menú según el rol |
| 5 | `app/.../ui/dashboard/DashboardFragment.kt` | 87-90, 123, 129-131 | Oculta botón de seed data para no-admins |
| 6 | `app/.../ui/crud/TableDetailFragment.kt` | 57-58, 66-67, 103-121, 164-176, 475-496 | Determina `canEdit`/`canDelete` al entrar a una tabla |
| 7 | `app/.../ui/crud/TableAdapter.kt` | 21-29, 56-57, 75-80 | Oculta botones de editar/eliminar por fila |
| 8 | `app/.../ui/crud/EditorDialogFragment.kt` | 46, 109-126, 518, 1598-1645, 1669-1691 | Verifica permisos al editar/guardar |
| 9 | `app/.../ui/crud/DeleteConfirmDialog.kt` | 239-258 | Verifica permisos antes de eliminar |

---

## 7. Justificación de Implementación a Nivel de Código

### 7.1. ¿Por qué no se implementó a nivel de base de datos?

La pregunta inicial del profesor sugiere implementar el control de acceso usando tablas de permisos en la base de datos (ej. tablas `ROL`, `PERMISO`, `ROL_TABLA_PERMISO`). A continuación se presentan las razones técnicas y de diseño por las cuales se optó por una implementación en código:

#### 7.1.1. Simplicidad y Mantenibilidad

**En base de datos:**
- Requiere crear al menos 2 tablas adicionales (`ROL`, `PERMISO_ACCESO` y `ROL_TABLA_PERMISO`).
- Requiere insertar datos semilla (23 tablas × 3 roles = 69 registros de permisos).
- Cualquier cambio en los permisos requiere modificar registros en la base de datos.

**En código (implementación actual):**
- Los permisos se definen en una sola función `getRoleTables()` de ~30 líneas.
- Cambiar un permiso implica modificar un mapa en Kotlin, no una consulta SQL.
- No requiere migraciones de base de datos ni datos semilla adicionales.

#### 7.1.2. Rendimiento

**En base de datos:**
- Cada vez que se necesita verificar un permiso, se debe ejecutar una consulta SQL (join entre tablas).
- Para una app Android con SQLite local, las consultas SQL tienen overhead de E/S de disco.

**En código (implementación actual):**
- La verificación es un lookup en un `Map` en memoria (O(1) en promedio).
- No hay llamadas a la base de datos para verificar permisos, solo una lectura de `SharedPreferences`.
- El mapa de permisos se carga desde `Constants.kt`, que ya está en memoria.

#### 7.2.3. Separación de Concerns

La base de datos debe encargarse de **almacenar datos persistentes**, no de **regular la lógica de negocio**. Mezclar ambas responsabilidades tiene desventajas:

- **Acoplamiento:** Cambiar la lógica de permisos requeriría cambiar el esquema de la base de datos.
- **Portabilidad:** Si en el futuro se migra a una API REST o a Firebase, la lógica de permisos en la base de datos habría que reescribirla. La lógica en código se adapta más fácilmente.

#### 7.2.4. Flexibilidad para Reglas de Negocio Dinámicas

Algunas reglas de acceso no pueden expresarse fácilmente con tablas de permisos:

| Regla | Implementada en | ¿Posible en DB? |
|---|---|---|
| "Postulante no puede cambiar ESTADO_PROCESO" | `EditorDialogFragment.kt:1670-1671` | Difícil (depende del valor del campo) |
| "Empresa solo puede editar ESTADO_PROCESO de POSTULACION" | `EditorDialogFragment.kt:122-125` y `disableNonEstadoFields()` | No sin lógica procedural |
| "No eliminar tu propio USUARIO" | `DeleteConfirmDialog.kt:251-258` | Imposible (depende de la sesión) |
| "Solo admin puede insertar datos de prueba" | `DashboardFragment.kt:88-90` | No (es un botón, no una tabla) |

#### 7.2.5. Seguridad en Capas (Defense in Depth)

La implementación actual verifica permisos en **4 capas diferentes**:

```
Capa 1: Dashboard (filtra tablas visibles)
Capa 2: TableDetail (determina canEdit/canDelete)
Capa 3: TableAdapter (oculta botones)
Capa 4: EditorDialog / DeleteDialog (bloquea acción final)
```

Si se implementara solo en base de datos, se necesitarían igualmente verificaciones en código para actualizar la UI (mostrar/ocultar botones). Esto implicaría **duplicar la lógica de permisos** en dos lugares, aumentando el riesgo de inconsistencias.

#### 7.2.6. Tiempo de Desarrollo y Contexto Académico

- El proyecto es una aplicación Android con SQLite local, donde el único usuario que accede directamente a la base de datos es la propia aplicación.
- No hay múltiples aplicaciones ni usuarios externos accediendo a la misma base de datos.
- En este contexto, implementar una estructura completa de tablas de permisos en SQLite sería **sobreingeniería**: agregaría complejidad sin beneficio real de seguridad.

### 7.2. Ventajas de la Implementación Actual

| Aspecto | Implementación en Código | Implementación en DB |
|---|---|---|
| **Líneas de código** | ~30 líneas (función `getRoleTables()`) | ~50 líneas SQL + ~30 líneas código + datos semilla |
| **Rendimiento** | Lookup en memoria (O(1)) | Consulta SQL (lectura de disco) |
| **Mantenibilidad** | Cambio en 1 archivo | Cambio en migración + datos semilla |
| **Reglas de negocio** | Soportadas directamente | Difíciles o imposibles |
| **Curva de aprendizaje** | Baja | Media (requiere SQL + joins) |
| **Migraciones** | No requiere | Requiere migración de DB + versión |

### 7.3. ¿Cuándo SÍ tendría sentido usar una implementación en base de datos?

- Si la aplicación tuviera un **panel de administración web** donde un superadmin pudiera crear/quitar permisos dinámicamente sin recompilar la app.
- Si los permisos fueran **configurables por el usuario final** (ej. "el gerente puede dar permisos personalizados a cada empleado").
- Si hubiera **múltiples aplicaciones** compartiendo la misma base de datos y todas debieran respetar los mismos permisos.

Ninguno de estos escenarios aplica a este proyecto académico.

---

## 8. Conclusión

El sistema de control de acceso por roles implementado en la aplicación Bolsa de Trabajo cumple con todos los requisitos funcionales:

1. **Tres roles diferenciados** con distintos niveles de acceso.
2. **Verificación en 4 capas** que garantiza que ningún usuario pueda realizar operaciones no autorizadas.
3. **Reglas de negocio específicas** para la tabla `POSTULACION`.
4. **Protección contra auto-eliminación** de usuarios.

La decisión de implementarlo a nivel de código (vs. base de datos) está justificada por:
- **Simplicidad:** Una función de 30 líneas reemplaza tablas, migraciones y datos semilla.
- **Rendimiento:** Verificaciones en memoria sin consultas SQL adicionales.
- **Flexibilidad:** Reglas de negocio complejas (como "postulante no puede cambiar estado") serían imposibles con tablas de permisos simples.
- **Contexto:** Aplicación Android con SQLite local sin necesidad de administración dinámica de permisos.

---

*Documento generado para la cátedra de la materia correspondiente al proyecto "Bolsa de Trabajo" (BT).*
*Facultad de Ingeniería y Arquitectura, Universidad de El Salvador.*
