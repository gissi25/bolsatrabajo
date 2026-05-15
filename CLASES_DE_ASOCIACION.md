# Clases de asociación en el modelo de Bolsa de Trabajo

> Documento técnico que explica qué es una **clase de asociación**, por qué en el
> script SQL aparece como un `CREATE TABLE` normal y por qué Power Designer la
> interpreta como una asociación con atributos durante la ingeniería inversa.

---

## 1. ¿Qué es una clase de asociación?

Una **clase de asociación** (también llamada *association class* en UML o
*entidad de asociación con atributos* en el modelo Entidad-Relación) es una
construcción que se utiliza cuando:

1. Existe una **relación muchos a muchos (M:N)** entre dos entidades, y
2. Esa relación **necesita guardar atributos propios** que no pertenecen a
   ninguna de las dos entidades por separado.

En lugar de inventar una entidad fuerte nueva con su propio identificador, se
crea una "clase" que vive **dentro de la relación** y cuya identidad se forma
exclusivamente con las llaves de las dos entidades que conecta.

### 1.1 Forma gráfica (modelo conceptual UML)

```
   POSTULANTE  ─────────────M ── N──────────  HABILIDAD
                            │
                            │   (línea de la relación)
                            │
                  ┌─────────┴──────────┐
                  │  HABILIDAD_POST.   │  ← clase de asociación
                  │  NIVEL_DESTREZA    │
                  └────────────────────┘
```

La caja **cuelga** de la línea de la relación, no es una entidad independiente.

### 1.2 Reglas para que una tabla califique como clase de asociación

| Regla | Detalle |
|------|---------|
| 1 | Conecta exactamente **dos** entidades fuertes en una relación M:N. |
| 2 | Su **llave primaria** es **la unión de las dos llaves foráneas** (no hay un ID propio). |
| 3 | Tiene al menos **un atributo descriptivo** de la relación. |
| 4 | No tiene sentido de existir sin las dos entidades padre. |

---

## 2. Las dos clases de asociación en este proyecto

### 2.1 `HABILIDAD_POSTULANTE`

Conecta `POSTULANTE` con `HABILIDAD` (una habilidad es a su vez débil de
`CATEGORIA_HABILIDAD`, por eso la llave primaria de `HABILIDAD` es compuesta).

**Definición real en el script (`ConnectionHelper.kt`):**

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

**Verificación de las reglas:**

| Regla | Cumple |
|------|--------|
| Conecta dos entidades fuertes (`POSTULANTE`, `HABILIDAD`) | ✅ |
| PK = (FK1 + FK2), sin ID propio | ✅  `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE)` |
| Atributo descriptivo de la relación | ✅  `NIVEL_DESTREZA` |
| Sin sentido fuera de la relación | ✅  un nivel de destreza solo existe entre alguien y una habilidad |

**Lectura semántica:**

> "El **postulante** *Juan* tiene la **habilidad** *Java* con **nivel** *Avanzado*."

El nivel cambia según la pareja postulante–habilidad, por lo tanto pertenece a
la asociación.

### 2.2 `RED_SOCIAL_POSTULANTE`

Conecta `POSTULANTE` con `RED_SOCIAL`.

**Definición real en el script (`ConnectionHelper.kt`):**

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

**Verificación de las reglas:**

| Regla | Cumple |
|------|--------|
| Conecta dos entidades fuertes (`POSTULANTE`, `RED_SOCIAL`) | ✅ |
| PK = (FK1 + FK2), sin ID propio | ✅  `(ID_POSTULANTE, ID_RED_SOCIAL)` |
| Atributo descriptivo de la relación | ✅  `URL_PERFIL` |
| Sin sentido fuera de la relación | ✅  una URL pertenece al *postulante en esa red* |

**Lectura semántica:**

> "El **postulante** *Juan* tiene perfil en **LinkedIn** cuya **URL** es
> *linkedin.com/in/juan*."

---

## 3. ¿Por qué en el SQL aparece como `CREATE TABLE` normal?

Porque **el concepto de clase de asociación no existe a nivel relacional**.

| Nivel del modelo | ¿Existe "clase de asociación"? | Cómo se representa |
|------------------|-------------------------------|--------------------|
| **Conceptual** (UML / E-R) | ✅ Sí | Cajita "colgada" de la línea M:N |
| **Lógico** (transición) | ⚠️ Solo de forma indirecta | Entidad cuya PK son las llaves importadas |
| **Físico** (SQL / DDL) | ❌ No | `CREATE TABLE` con PK compuesta de las FK |

SQL no tiene una palabra clave del tipo `CREATE ASSOCIATION CLASS …`. El estándar
relacional (Codd, 1970) solo conoce **tablas, columnas, llaves y restricciones**.
Cualquier construcción semántica más rica (entidades débiles, jerarquías,
clases de asociación) **debe codificarse usando esas piezas básicas**.

Por eso en `ConnectionHelper.kt` las dos clases de asociación se ven idénticas
a cualquier otra tabla: se "esconden" detrás de un `CREATE TABLE` con PK
compuesta y dos FK.

---

## 4. ¿Cómo es que Power Designer las "detecta" como asociaciones?

Power Designer no usa una palabra mágica del SQL para identificarlas. Aplica
una **inferencia estructural** al hacer ingeniería inversa:

> Cuando ve una tabla cuya **llave primaria es exactamente la concatenación de
> sus llaves foráneas**, deduce que es una **tabla puente** y, si existen
> columnas adicionales (atributos), la marca como una **clase de asociación**.

### 4.1 Algoritmo simplificado que aplica Power Designer

```
Para cada tabla T:
    sea FKs   = conjunto de columnas que son FK
    sea PK    = columnas de la llave primaria
    sea EXTRA = columnas que no son PK ni FK

    Si  PK == FKs   y   |FKs| >= 2:
        Si  EXTRA es vacío:
            → tabla puente pura (solo conexión)
        Si  EXTRA no es vacío:
            → CLASE DE ASOCIACIÓN
    En caso contrario:
        → entidad fuerte / entidad débil / entidad asociativa
```

### 4.2 Aplicando el algoritmo a nuestras tablas

**`HABILIDAD_POSTULANTE`**

| Conjunto | Columnas |
|----------|----------|
| PK       | `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE)` |
| FKs      | `(ID_CATEGORIA_HABILIDAD, ID_HABILIDAD)` → `HABILIDAD` + `(ID_POSTULANTE)` → `POSTULANTE` |
| EXTRA    | `NIVEL_DESTREZA` |

→ `PK == FKs` ✅ y `EXTRA = {NIVEL_DESTREZA}` ≠ ∅ → **clase de asociación**.

**`RED_SOCIAL_POSTULANTE`**

| Conjunto | Columnas |
|----------|----------|
| PK       | `(ID_POSTULANTE, ID_RED_SOCIAL)` |
| FKs      | `(ID_POSTULANTE)` → `POSTULANTE` + `(ID_RED_SOCIAL)` → `RED_SOCIAL` |
| EXTRA    | `URL_PERFIL` |

→ `PK == FKs` ✅ y `EXTRA = {URL_PERFIL}` ≠ ∅ → **clase de asociación**.

---

## 5. Comparación con una tabla que NO es clase de asociación

Hay que distinguir las clases de asociación de las **entidades asociativas con
identidad propia**. Veamos `POSTULACION`:

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

| Conjunto | Columnas |
|----------|----------|
| PK       | `(ID_POSTULACION)`  ← un identificador propio inventado |
| FKs      | `(NIT, ID_OFERTA)` + `(ID_POSTULANTE)` |
| EXTRA    | `FECHA_APLICACION`, `ESTADO_PROCESO` |

→ `PK ≠ FKs` (porque la PK es `ID_POSTULACION`, no las FK).

Por eso Power Designer NO la marca como clase de asociación, sino como una
**entidad asociativa con identidad propia** (también llamada *entidad fuerte
que nació modelando una relación*).

> Aunque conecte `POSTULANTE` con `OFERTA_TRABAJO` y tenga atributos propios,
> el hecho de tener su propio identificador (`ID_POSTULACION`) la convierte en
> una entidad independiente.

### 5.1 Tabla comparativa

| Característica                | `HABILIDAD_POSTULANTE`             | `RED_SOCIAL_POSTULANTE`           | `POSTULACION`                          |
|-------------------------------|------------------------------------|-----------------------------------|----------------------------------------|
| Conecta dos entidades         | `POSTULANTE` + `HABILIDAD`         | `POSTULANTE` + `RED_SOCIAL`       | `POSTULANTE` + `OFERTA_TRABAJO`         |
| ID propio                     | ❌ No                              | ❌ No                             | ✅ Sí (`ID_POSTULACION`)               |
| PK = unión de las FK          | ✅ Sí                              | ✅ Sí                             | ❌ No                                  |
| Atributos propios             | `NIVEL_DESTREZA`                   | `URL_PERFIL`                      | `FECHA_APLICACION`, `ESTADO_PROCESO`   |
| Clasificación                 | **Clase de asociación**            | **Clase de asociación**           | **Entidad asociativa (con identidad)** |

---

## 6. Por qué importa la distinción

| Aspecto | Clase de asociación | Entidad asociativa |
|--------|---------------------|--------------------|
| Identidad | Heredada de las dos entidades padres | Propia |
| Borrado en cascada | Cuando muere uno de los dos padres, ella muere | Sobrevive aunque cambien los padres |
| Crecimiento | Pensada para describir relaciones puntuales | Pensada para gestionar un proceso (puede tener varios estados, historial, etc.) |
| Modelo conceptual | Cajita colgada de la relación | Entidad rectangular con líneas a las dos entidades |

En el caso de `POSTULACION` se eligió darle identidad propia porque:

- Cambia de estado a lo largo del tiempo (`ESTADO_PROCESO`).
- Es la unidad de seguimiento del proceso de selección.
- Puede ser referenciada por otras tablas futuras (notificaciones,
  entrevistas, etc.) sin tener que arrastrar tres llaves.

---

## 7. Resumen ejecutivo

1. **SQL** no conoce el concepto de clase de asociación; todas las tablas se
   crean con `CREATE TABLE`.
2. La distinción **entidad fuerte / débil / clase de asociación** vive en el
   **modelo conceptual y lógico**, no en el script.
3. **Power Designer** reconstruye esa información al hacer ingeniería inversa
   inspeccionando la estructura de cada tabla: si **`PK = unión de FKs` y
   existen atributos extra**, la clasifica como clase de asociación.
4. En este proyecto hay **dos clases de asociación**:
   - `HABILIDAD_POSTULANTE` (POSTULANTE ↔ HABILIDAD, atributo `NIVEL_DESTREZA`)
   - `RED_SOCIAL_POSTULANTE` (POSTULANTE ↔ RED_SOCIAL, atributo `URL_PERFIL`)
5. **`POSTULACION` no es** clase de asociación, sino **entidad asociativa con
   identidad propia**, porque tiene un identificador inventado
   (`ID_POSTULACION`) en vez de heredar la PK de las FK.

---

## 8. Anexo. Verificación rápida con un solo `SELECT`

Si quieres confirmar que una tabla calza como clase de asociación, basta con
revisar tres cosas en el `CREATE TABLE`:

```text
1. ¿La PRIMARY KEY usa SOLO columnas que también son FOREIGN KEY?
2. ¿Hay al menos UN atributo que no esté en la PK?
3. ¿Las FK apuntan a ENTIDADES FUERTES (no a otras asociaciones)?

Si las 3 respuestas son SÍ → es clase de asociación.
```

| Tabla                  | (1) PK ⊆ FKs | (2) ≥1 atributo extra | (3) FKs → fuertes | ¿Clase de asociación? |
|------------------------|:-----------:|:--------------------:|:----------------:|:--------------------:|
| `HABILIDAD_POSTULANTE` | ✅          | ✅ `NIVEL_DESTREZA`  | ✅               | **Sí**               |
| `RED_SOCIAL_POSTULANTE`| ✅          | ✅ `URL_PERFIL`      | ✅               | **Sí**               |
| `POSTULACION`          | ❌          | ✅                   | ✅               | No (es asociativa)   |
| `MUNICIPIO`            | ❌          | ✅                   | ✅               | No (es entidad débil)|
| `DEPARTAMENTO`         | ❌ (sin FK) | ✅                   | —                | No (es entidad fuerte)|

---

*Última revisión: ANSI SQL Nivel 2 — compatible con PowerDesigner.*
