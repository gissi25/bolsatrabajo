# Repartición de Tablas para Testing Independiente

> Cada persona trabaja en su propio teléfono. Nadie depende de datos de otro.
> Cada quien crea sus propios prereqs (datos base) antes de probar sus triggers.

---

## 👤 Marcos (7 tablas — MUY FÁCIL)

| # | Tabla | FK hacia | Prereqs propios | Triggers a probar |
|---|-------|----------|-----------------|-------------------|
| 1 | GENERO | — | Ninguno | Vacío, Duplicado (case insensitive), Minúscula auto |
| 2 | TIPO_DOCUMENTO | — | Ninguno | Vacío, Duplicado, Minúscula auto |
| 3 | CATEGORIA_HABILIDAD | — | Ninguno | Vacío, Duplicado, Minúscula auto, Cascada al borrar |
| 4 | HABILIDAD | CATEGORIA_HABILIDAD | Crear 1 categoría primero | Vacío, Duplicado, Minúscula auto, Cascada al borrar categoría |
| 5 | RED_SOCIAL | — | Ninguno | Vacío, Duplicado, Minúscula auto, Cascada al borrar |
| 6 | GRADO_ACADEMICO | — | Ninguno | Vacío, Duplicado, Minúscula auto, Cascada al borrar |
| 7 | USUARIO | — | Ninguno | Vacío, Duplicado (case insensitive), Password ≥ 8, Minúscula auto en username/rol |

### Datos prereq que Marcos debe crear

Ninguno externo. Solo necesita crear 1 categoría antes de insertar habilidades.

### Checklist de pruebas — Marcos

- [ ] Crear género "Femenino" y luego intentar crear "femenino" → **Duplicado** ✓
- [ ] Crear género "Masculino" y luego intentar crear "MASCULINO" → **Duplicado** ✓
- [ ] Crear género con nombre vacío → **Error: Vacío** ✓
- [ ] Insertar "ADMIN" en username → se guarda como "admin" (minúscula) ✓
- [ ] Insertar "INGENIERÍA" en grado académico → se guarda como "ingeniería" (minúscula) ✓
- [ ] Insertar password "abc" (< 8 caracteres) → **Error: Password mínimo 8 caracteres** ✓
- [ ] Insertar usuario con username vacío → **Error: Vacío** ✓
- [ ] Crear "Técnico" en habilidad y luego intentar "tecnico" → **Duplicado** ✓
- [ ] Crear "Programación" en categoría y luego intentar "programacion" → **Duplicado** ✓
- [ ] Borrar CATEGORIA_HABILIDAD que tiene habilidades → **Cascada** borra todas las habilidades de esa categoría ✓
- [ ] Borrar RED_SOCIAL que tiene postulantes vinculados → **Cascada** ✓
- [ ] Borrar GRADO_ACADEMICO que tiene ofertas vinculadas → **Cascada** ✓

---

## 👤 David (5 tablas — FÁCIL)

| # | Tabla | FK hacia | Prereqs propios | Triggers a probar |
|---|-------|----------|-----------------|-------------------|
| 1 | DEPARTAMENTO | — | Ninguno | Vacío, Duplicado, Minúscula auto, Cascada al borrar |
| 2 | MUNICIPIO | DEPARTAMENTO | Crear 1 depto primero | Vacío, Duplicado por depto, Minúscula auto, Cascada |
| 3 | DISTRITO | MUNICIPIO | Crear depto→municipio | Vacío, Duplicado por municipio, Minúscula auto, Cascada |
| 4 | INSTITUCION | — | Ninguno | Vacío, Duplicado, Minúscula auto, Cascada al borrar |
| 5 | EMPRESA | DISTRITO | Crear depto→municipio→distrito | Vacío en nombre/NIT, Duplicado NIT/nombre, Minúscula auto, Cascada |

### Datos prereq que David debe crear

Solo su propia cadena de geografía:
1. Crear 1 DEPARTAMENTO (ej: "San Salvador")
2. Crear 1 MUNICIPIO en ese depto (ej: "San Salvador")
3. Crear 1 DISTRITO en ese municipio (ej: "San Salvador Centro")
4. Crear 1 INSTITUCION (ej: "Universidad de El Salvador")

### Checklist de pruebas — David

- [ ] Crear "San Salvador" y luego intentar "san salvador" → **Duplicado** ✓
- [ ] Crear "La Libertad" como departamento → se guarda como "la libertad" ✓
- [ ] Crear departamento con nombre vacío → **Error: Vacío** ✓
- [ ] Crear municipio en departamento que no existe → **Error: FK** ✓
- [ ] Crear "San Salvador" en otro departamento → **Permitido** (mismo nombre, diferente depto) ✓
- [ ] Crear "San Salvador" en el mismo departamento → **Duplicado** ✓
- [ ] Crear distrito con mismo nombre en el mismo municipio → **Duplicado** ✓
- [ ] Crear empresa con nombre o NIT vacío → **Error: Campos clave vacíos** ✓
- [ ] Crear empresa con NIT duplicado → **Duplicado** ✓
- [ ] Crear "EMPRESA ABC" → se guarda como "empresa abc" (minúscula) ✓
- [ ] Borrar DEPARTAMENTO → **Cascada** borra municipio, distrito, empresa, postulantes, etc. ✓
- [ ] Borrar INSTITUCION que tiene ofertas académicas → **Cascada** ✓

---

## 👤 Eduardo (5 tablas — MEDIO)

| # | Tabla | FK hacia | Prereqs propios | Triggers a probar |
|---|-------|----------|-----------------|-------------------|
| 1 | OFERTA_ACADEMICA | GRADO_ACADEMICO, INSTITUCION | Crear 1 grado + 1 institución | Duplicado por institución+grado |
| 2 | OFERTA_TRABAJO | EMPRESA, GRADO_ACADEMICO | Crear empresa + 1 grado | Vacío, Duplicado por empresa+título, Fechas, Edad mín<máx, Minúscula auto |
| 3 | DETALLE_REQUISITO | OFERTA_TRABAJO | Crear oferta primero | Vacío, Duplicado, Minúscula auto |
| 4 | CERTIFICACION | POSTULANTE, INSTITUCION | Crear postulante + institución | Vacío, Duplicado código por postulante, Minúscula en nombre, UPPER en código |
| 5 | FORMACION_ACADEMICA | POSTULANTE, OFERTA_ACADEMICA | Crear postulante + oferta académica | Vacío en título, Nivel académico insuficiente |

### Datos prereq que Eduardo debe crear

1. 1 GENERO (ej: "Masculino")
2. 1 TIPO_DOCUMENTO (ej: "DUI")
3. DEPARTAMENTO → MUNICIPIO → DISTRITO (cadena completa)
4. 1 INSTITUCION (ej: "Universidad de El Salvador")
5. 1 GRADO_ACADEMICO (ej: "Ingeniería en Sistemas")
6. 1 EMPRESA (necesita distrito)
7. 1 USUARIO (ej: admin/admin1234)
8. 1 POSTULANTE (necesita genero, tipo_doc, distrito) — fecha nacimiento antes de 2008
9. 1 OFERTA_ACADEMICA (necesita grado + institución)

### Checklist de pruebas — Eduardo

- [ ] Crear OFERTA_TRABAJO con fecha inicio > fecha fin → **Error: La oferta ya caducó o fecha inválida** ✓
- [ ] Crear OFERTA_TRABAJO con edad mínima > edad máxima → **Error: Edad mínima no puede ser mayor a la máxima** ✓
- [ ] Crear oferta con título vacío → **Error: Vacío** ✓
- [ ] Crear "Desarrollador Backend" y luego "desarrollador backend" en misma empresa → **Duplicado** ✓
- [ ] Crear oferta sin fecha de publicación → **Error: La fecha de publicación es obligatoria** ✓
- [ ] Crear OFERTA_ACADEMICA duplicada (misma institución+grado) → **Error: Oferta académica duplicada** ✓
- [ ] Crear DETALLE_REQUISITO con descripción vacía → **Error: Vacío** ✓
- [ ] Crear DETALLE_REQUISITO duplicado → **Duplicado** ✓
- [ ] Crear CERTIFICACION con nombre o código vacío → **Error: Vacío** ✓
- [ ] Crear certificación con código "abc123" → se guarda nombre en minúscula y código como "ABC123" (UPPER) ✓
- [ ] Crear FORMACION_ACADEMICA con título "Bachillerato" → **Error: Nivel académico insuficiente** ✓
- [ ] Crear FORMACION_ACADEMICA con título vacío → **Error: Vacío** ✓
- [ ] Borrar OFERTA_TRABAJO que tiene DETALLE_REQUISITO → **Cascada** borra requisitos ✓
- [ ] Borrar OFERTA_ACADEMICA que tiene FORMACION_ACADEMICA → **Cascada** ✓

---

## 👤 Yaya (5 tablas — MEDIO-ALTO)

| # | Tabla | FK hacia | Prereqs propios | Triggers a probar |
|---|-------|----------|-----------------|-------------------|
| 1 | POSTULANTE | GENERO, TIPO_DOCUMENTO, DISTRITO | Crear genero + tipo_doc + distrito chain | Vacío en campos clave, Duplicado documento/email, Mayor de edad, Email válido, Minúscula auto |
| 2 | EXPERIENCIA_LABORAL | POSTULANTE, EMPRESA | Crear postulante + empresa | Vacío en puesto, Duplicado, Fecha inicio < fin, Minúscula auto |
| 3 | HABILIDAD_POSTULANTE | HABILIDAD, POSTULANTE | Crear categoría→habilidad + postulante | Nivel destreza 1-3, Duplicado |
| 4 | POSTULACION | OFERTA_TRABAJO, POSTULANTE | Crear empresa→oferta + postulante | Duplicado por postulante+oferta, Minúscula auto en estado |
| 5 | RED_SOCIAL_POSTULANTE | POSTULANTE, RED_SOCIAL | Crear postulante + red social | Vacío en URL, Duplicado, Minúscula auto en URL |

### Datos prereq que Yaya debe crear

1. 1 GENERO (ej: "Femenino")
2. 1 TIPO_DOCUMENTO (ej: "DUI")
3. DEPARTAMENTO → MUNICIPIO → DISTRITO (cadena completa)
4. 1 CATEGORIA_HABILIDAD (ej: "Programación") → 1 HABILIDAD (ej: "Java")
5. 1 RED_SOCIAL (ej: "Facebook")
6. 1 GRADO_ACADEMICO (ej: "Ingeniería")
7. 1 EMPRESA (necesita distrito)
8. 1 OFERTA_TRABAJO (necesita empresa + grado académico)
9. 1 POSTULANTE (necesita genero, tipo_doc, distrito) — fecha nacimiento antes de 2008

### Checklist de pruebas — Yaya

- [ ] Crear POSTULANTE menor de edad (fecha nacimiento después de 2008) → **Error: El postulante debe ser mayor de edad** ✓
- [ ] Crear POSTULANTE con email sin @ → **Error: Correo electrónico no válido** ✓
- [ ] Crear POSTULANTE con documento duplicado → **Error: Documento o Email ya registrado** ✓
- [ ] Crear POSTULANTE con email duplicado (case insensitive) → **Error: Documento o Email ya registrado** ✓
- [ ] Crear POSTULANTE con nombre vacío → **Error: Campos clave no pueden ser vacíos** ✓
- [ ] Insertar "MARIA GARCIA" en nombre/apellido → se guarda como "maria garcia" (minúscula) ✓
- [ ] Crear HABILIDAD_POSTULANTE con nivel 5 → **Error: Nivel de destreza debe ser 1, 2 o 3** ✓
- [ ] Crear HABILIDAD_POSTULANTE con nivel 0 → **Error: Nivel de destreza debe ser 1, 2 o 3** ✓
- [ ] Crear HABILIDAD_POSTULANTE con nivel 2 → **Permitido** ✓
- [ ] Asignar misma habilidad al mismo postulante 2 veces → **Error: Habilidad ya asignada** ✓
- [ ] Crear EXPERIENCIA_LABORAL con fecha inicio > fecha fin → **Error: Fecha inicio debe ser menor a fecha fin** ✓
- [ ] Crear experiencia con puesto vacío → **Error: Vacío** ✓
- [ ] Crear experiencia duplicada (mismo postulante, misma empresa, mismo puesto) → **Duplicado** ✓
- [ ] Crear POSTULACION duplicada (mismo postulante + misma oferta) → **Error: El usuario ya aplicó a esta oferta** ✓
- [ ] Insertar "PENDIENTE" en estado_proceso → se guarda como "pendiente" (minúscula) ✓
- [ ] Crear RED_SOCIAL_POSTULANTE con URL vacía → **Error: Vacío** ✓
- [ ] Crear misma red social para el mismo postulante 2 veces → **Error: Red social ya vinculada** ✓
- [ ] Borrar POSTULANTE → **Cascada** borra experiencia, certificaciones, habilidades, redes, postulaciones ✓

---

## Resumen

| Persona | Tablas | Dificultad | Prereqs externos |
|---------|---------|------------|-------------------|
| **Marcos** | 7 (todas 0-1 FK) | Muy fácil | Ninguno |
| **David** | 5 (cadena simple de geografía) | Fácil | Ninguno |
| **Eduardo** | 5 (ofertas, certificaciones, formación) | Medio | 9 prereqs propios |
| **Yaya** | 5 (POSTULANTE y sus tablas hijas) | Medio-alto | 9 prereqs propios |

## Nota importante antes de probar

1. **Desinstalar** la app anterior del dispositivo
2. Instalar el nuevo APK
3. La base de datos se recrea automáticamente (DATABASE_VERSION = 7)