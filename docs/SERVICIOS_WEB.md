# Servicios Web — Bolsa de Trabajo PDM

## Arquitectura general

```
┌─────────────────────────────────────────────────────────┐
│ ANDROID (Kotlin)                                        │
│                                                         │
│  MainActivity → ApiService.initChallenge(WebView)       │
│                      │                                  │
│  Fragment → ApiService.metodoX()                        │
│               │                                          │
│               ▼  inyecta JS en WebView invisible         │
│           fetch('https://bolsadetrabajopdm.gt.tc/       │
│                 go.php?action=empresas')                 │
└──────────────────┬──────────────────────────────────────┘
                   │  HTTPS (dentro del WebView)
                   ▼
┌──────────────────────────────────────────────────────────┐
│ INFINITYFREE — bolsadetrabajopdm.gt.tc                   │
│                                                          │
│  go.php  ← ROUTER (único punto de entrada)               │
│    ?action=empresas          → servicio1.php             │
│    ?action=grados            → servicio1.php             │
│    ?action=insertar_ofertas  → servicio1.php             │
│    ?action=recomendar_formacion → servicio2.php          │
│    ?action=panorama_mercado  → servicio2.php             │
│    ?action=sincronizar_postulantes → servicio3.php       │
│                                                          │
│  Cada servicioX.php se conecta a:                        │
│    MySQL → if0_42097646_bolsadetrabajo                   │
└──────────────────────────────────────────────────────────┘
```

---

## Archivos en InfinityFree

| Archivo | Rol | Acciones |
|---------|-----|----------|
| `go.php` | Router principal | Lee `?action=`, incluye el `servicioX.php` correspondiente |
| `servicio1.php` | Carga masiva de ofertas | `empresas`, `grados`, `insertar_ofertas` |
| `servicio2.php` | Recomendador de formación | `recomendar_formacion`, `panorama_mercado` |
| `servicio3.php` | Sincronización de postulantes | `sincronizar_postulantes` |

---

## `go.php` — El Router

Es el **único archivo que recibe las peticiones**. Funciona así:

1. Lee `$_GET['action']`
2. Busca en un mapa qué número de servicio le toca
3. Incluye `servicio{$numero}.php`

```php
$route = [
    'empresas'                  => 1,
    'grados'                    => 1,
    'insertar_ofertas'          => 1,
    'recomendar_formacion'      => 2,
    'panorama_mercado'          => 2,
    'sincronizar_postulantes'   => 3,
];

$serviceFile = "servicio{$route[$action]}.php";
include $serviceFile;
```

La variable `$action` queda disponible dentro del servicio incluido, que la usa para decidir qué función ejecutar.

---

## Servicio 1 — Carga Masiva de Ofertas (MEJORADO)

### ¿Qué hace?

Permite que una empresa (o admin) suba **múltiples ofertas de trabajo** de una sola vez, junto con sus requisitos. Es el punto de entrada para poblar la tabla `OFERTA_TRABAJO`.

### Acciones

| Acción | Método | Body | Respuesta |
|--------|--------|------|-----------|
| `empresas` | GET | — | Lista de empresas con NIT, nombre, contacto, departamento, municipio, distrito |
| `grados` | GET | — | Lista de grados académicos (ID y nombre) |
| `insertar_ofertas` | POST | `{"ofertas": [...]}` | Resultado detallado por oferta |

### Mejoras sobre la versión anterior

| Antes | Ahora |
|-------|-------|
| `empresas` solo devolvía NIT y NOMBRE_EMPRESA | Ahora incluye DEPARTAMENTO, MUNICIPIO, DISTRITO (vía JOINs) |
| `insertar_ofertas` insertaba sin validar | Valida NIT, grado, fechas, edades, experiencia **antes** de insertar |
| Sin transacciones | Cada oferta + sus requisitos se insertan en **transacción** (si falla un requisito, no se inserta la oferta a medias) |
| Respuesta genérica éxito/fallo | Devuelve **detalle por oferta**: cuáles se insertaron, cuáles fallaron y por qué |

### Ejemplo de uso

**Petición:**
```json
POST /go.php?action=insertar_ofertas
{
  "ofertas": [
    {
      "nit": "06141234560101",
      "id_oferta": "OF01",
      "titulo": "Desarrollador Android",
      "id_grado": 5,
      "fecha_publicacion": "2026-06-01",
      "fecha_caducidad": "2026-07-01",
      "experiencia_anios": 2,
      "edad_minima": 22,
      "edad_maxima": 40,
      "descripcion": "Buscamos developer Kotlin con experiencia en Jetpack Compose",
      "requisitos": [
        {"id_detalle": "D1", "descripcion": "Manejo de Kotlin y corrutinas"},
        {"id_detalle": "D2", "descripcion": "Experiencia con Room y SQLite"}
      ]
    }
  ]
}
```

**Respuesta:**
```json
{
  "exito": true,
  "mensaje": "Procesadas: 1 insertadas, 0 fallidas de 1 total",
  "insertadas": 1,
  "fallidas": 0,
  "detalle": [
    {
      "oferta_id": "OF01",
      "titulo": "Desarrollador Android",
      "estado": "insertada",
      "requisitos": 2
    }
  ]
}
```

### Para qué sirve en el sistema

1. **Poblar la BD**: Sin ofertas no hay bolsa de trabajo. Este servicio es el que llena `OFERTA_TRABAJO` y `DETALLE_REQUISITO`.
2. **Validación centralizada**: Los triggers de MySQL están duplicados en SQLite y MySQL. Este servicio agrega una capa extra de validación en PHP antes de llegar a la BD, con mensajes claros en español.
3. **Carga desde CSV/Excel**: El `Servicio1Fragment` en Android permite cargar ofertas desde archivo. El JSON que arma se envía a este endpoint.

---

## Servicio 2 — Recomendador de Formación (NUEVO)

### ¿Qué hace?

Responde la pregunta: **"¿Qué debería estudiar o aprender para ser más competitivo en el mercado laboral?"**

Analiza los datos reales del sistema (ofertas activas, postulantes, habilidades) y devuelve recomendaciones personalizadas para un postulante específico.

### ¿Por qué NO es un matching ni búsqueda?

- **Matching** (lo tienen tus compañeros): "Dado un postulante, encontrá ofertas que le sirvan."
- **Recomendador de Formación** (este servicio): "Dado un postulante, decime qué skills/carreras/instituciones le convienen para mejorar su perfil."

No cruza postulantes con ofertas. Cruza **el perfil del postulante contra el mercado completo** para dar consejos de carrera.

### Acciones

| Acción | Método | Body | Respuesta |
|--------|--------|------|-----------|
| `recomendar_formacion` | POST | `{"id_postulante": "GM12345"}` | Análisis completo con 6 secciones |
| `panorama_mercado` | GET | — | Datos agregados públicos (sin necesidad de ID) |

### Las 6 secciones del análisis

#### 1. Grados más demandados
Los grados académicos que más aparecen en ofertas activas, ordenados por frecuencia. Marca cuál es **tu grado actual**.

```
1. Ingeniería (8 ofertas)     ← ¡Este es tu grado!
2. Licenciatura (5 ofertas)
3. Maestría (3 ofertas)
```

#### 2. Grados que no tenés
Todos los grados del sistema que vos NO tenés (para que consideres seguir estudiando).

```
• Licenciatura
• Maestría
• Doctorado
• Profesorado
• Técnico Superior
```

#### 3. Instituciones Top
Las instituciones con más egresados registrados en el sistema.

```
1. Universidad de El Salvador (UES) — 15 egresados
2. ITCA — 8 egresados
3. Universidad Don Bosco — 5 egresados
```

#### 4. Skills más comunes
Las habilidades que más postulantes tienen registradas. Te da una idea de contra qué estás compitiendo.

```
1. Programación en Python — 12 postulantes
2. Inglés — 10 postulantes
3. Diseño de modelos relacionales (SQL) — 8 postulantes
```

#### 5. Skills que te FALTAN
De todas las habilidades disponibles en el sistema, cuáles **no tenés registradas**. Es tu "lista de pendientes" para mejorar tu perfil.

```
• Administración de sistemas Linux
• Cálculo y diseño de Subnetting
• Configuración de topologías en Cisco
• Desarrollo en C++
• Despliegue de proyectos en Google Cloud
• Francés
• Portugués
... (todas las que no tengas)
```

#### 6. Estadísticas del mercado
Datos agregados para contexto:

```
Ofertas activas: 12
Total postulantes: 45
Total empresas: 10
Competencia promedio: 3.8 postulantes por oferta
```

### Ejemplo de uso

**Petición:**
```json
POST /go.php?action=recomendar_formacion
{
  "id_postulante": "GM12345"
}
```

**Respuesta (resumida):**
```json
{
  "exito": true,
  "data": {
    "postulante": {
      "id": "GM12345",
      "nombre": "gissell martinez",
      "id_grado": 5
    },
    "grados_mas_demandados": [
      {"ID_GRADO_ACADEMICO": 5, "NOMBRE_GRADO": "Ingeniería", "total_ofertas": 8, "es_tu_grado": true}
    ],
    "grados_que_no_tenes": [
      {"ID_GRADO_ACADEMICO": 4, "NOMBRE_GRADO": "Licenciatura"},
      {"ID_GRADO_ACADEMICO": 6, "NOMBRE_GRADO": "Maestría"}
    ],
    "instituciones_top": [
      {"ID_INSTITUCION": "INS001", "NOMBRE_INSTITUCION": "Universidad de El Salvador (UES)", "total_egresados": 2}
    ],
    "skills_mas_comunes": [
      {"NOMBRE_HABILIDAD": "Programación en Python", "NOMBRE_CATEGORIA": "Desarrollo de Software", "total_postulantes": 1}
    ],
    "skills_que_te_faltan": [
      {"NOMBRE_HABILIDAD": "Administración de sistemas Linux", "NOMBRE_CATEGORIA": "Infraestructura y Cloud Computing"}
    ],
    "estadisticas_mercado": {
      "ofertas_activas": 0,
      "total_postulantes": 2,
      "total_empresas": 10,
      "competencia_promedio": "Sin datos (no hay ofertas activas)"
    }
  }
}
```

### Para qué sirve en el sistema

1. **Orientación vocacional**: Un postulante puede ver qué le conviene estudiar basado en datos reales del mercado, no en opiniones.
2. **Guía de upskilling**: La sección de "skills que te faltan" es una lista concreta de qué aprender.
3. **Elección de institución**: Si alguien está decidiendo dónde estudiar, ve qué instituciones tienen más presencia en el mercado laboral.
4. **Panorama general**: `panorama_mercado` es público, no requiere login. Sirve para cualquier visitante.

---

## Convención para crear nuevos servicios

Si en el futuro querés agregar un `servicio4.php`, seguí este patrón:

```php
<?php
// SERVICIO X — Descripción

// 1. Configuración de BD
$host = 'localhost';
$user = 'if0_42097646';
$pass = 'TU_CONTRASEÑA';
$db   = 'if0_42097646_bolsadetrabajo';

header('Content-Type: application/json; charset=utf-8');

// 2. Funciones helper (conectar, responder, error)
function conectar() { ... }
function responder($data) { echo json_encode($data); exit; }

// 3. Router interno
switch ($action) {
    case 'mi_accion': miFuncion(); break;
}

// 4. Lógica de cada acción
function miFuncion() {
    $conn = conectar();
    // queries...
    $conn->close();
    responder(["exito" => true, "data" => $datos]);
}
```

Y en `go.php` agregás la ruta:
```php
'mi_accion' => 4,  // servicio4.php
```

---

## Cómo probar sin Android

Desde el navegador o con curl:

```bash
# Probar empresas (GET)
curl "https://bolsadetrabajopdm.gt.tc/go.php?action=empresas"

# Probar panorama de mercado (GET)
curl "https://bolsadetrabajopdm.gt.tc/go.php?action=panorama_mercado"

# Probar recomendador (POST)
curl -X POST "https://bolsadetrabajopdm.gt.tc/go.php?action=recomendar_formacion" \
  -H "Content-Type: application/json" \
  -d '{"id_postulante":"GM12345"}'

# Probar inserción de ofertas (POST)
curl -X POST "https://bolsadetrabajopdm.gt.tc/go.php?action=insertar_ofertas" \
  -H "Content-Type: application/json" \
  -d '{"ofertas":[{...}]}'
```
