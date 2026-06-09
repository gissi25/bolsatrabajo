# Documentación de Servicios Web 7 y 8

---

## Servicio 7: Filtrado de ofertas por edad

### ¿Qué hace?
Este servicio consulta las ofertas de trabajo que están guardadas en el servidor de InfinityFree (base de datos MySQL) y las filtra según la edad del postulante. Solo devuelve las ofertas activas donde la edad del usuario está dentro del rango permitido por la oferta. No modifica ningún dato, solo consulta.

### ¿Cómo funciona paso a paso?
1. El usuario abre la pantalla y ve un campo para escribir su edad, un botón "Buscar ofertas" y una lista vacía.
2. Escribe su edad (por ejemplo, 25) y toca "Buscar ofertas":
   - La app valida que la edad sea un número entre 1 y 120.
   - La app hace una petición HTTP GET al servidor: `go.php?action=ofertas_por_edad&edad=25`.
   - El servidor ejecuta una consulta SQL en MySQL que cruza las tablas `OFERTA_TRABAJO`, `EMPRESA` y `GRADO_ACADEMICO`.
   - La consulta filtra las ofertas donde `EDAD_MINIMA <= 25 AND EDAD_MAXIMA >= 25`.
   - También excluye ofertas vencidas: `FECHA_CADUCIDAD >= CURDATE()`.
   - Ordena los resultados por fecha de publicación descendente.
   - Devuelve un JSON con todas las ofertas encontradas.
3. La app recibe el JSON y muestra cada oferta en una tarjeta con:
   - Nombre de la empresa.
   - Título del puesto.
   - Grado académico requerido.
   - Años de experiencia solicitados.
   - Rango de edad que acepta la oferta.
   - Fechas de publicación y caducidad.
4. Si no encuentra ofertas, muestra el mensaje "No se encontraron ofertas para tu edad".

### ¿Quién lo usa?
- El postulante (para encontrar rápidamente ofertas compatibles con su edad).

---

## Servicio 8: Inteligencia empresarial para reclutamiento

### ¿Qué hace?
Este servicio permite a una empresa sincronizar sus postulaciones locales con el servidor y luego consultar reportes inteligentes sobre sus procesos de reclutamiento. Responde preguntas como: ¿cuántas ofertas tengo publicadas?, ¿cuántas postulaciones he recibido?, ¿qué oferta es la más popular?, ¿cuántos candidatos están en entrevista, contratados o rechazados?. Todo es de solo lectura excepto la acción de sincronización que sube datos.

### ¿Cómo funciona paso a paso?

**Paso 1: Subir postulaciones al servidor**
1. La empresa ingresa su NIT y toca "Subir postulaciones".
2. La app busca en la base de datos local todas las postulaciones asociadas a ese NIT.
3. La app envía un POST al servidor: `go.php?action=subir_postulaciones` con los datos en JSON.
4. El servidor guarda cada postulación en la tabla `POSTULACION` de MySQL.
   - Si la postulación ya existe, la actualiza.
   - Si es nueva, la inserta.
5. Muestra un resumen de cuántas se insertaron y cuántas se actualizaron.

**Paso 2: Consultar resumen de reclutamiento**
1. La empresa toca "Resumen".
2. La app envía: `go.php?action=resumen_reclutamiento&nit=X`.
3. El servidor devuelve:
   - Total de ofertas activas (no han caducado).
   - Total de ofertas vencidas.
   - Total de postulaciones recibidas.
   - Promedio de postulaciones por oferta.
4. La app muestra los números en pantalla.

**Paso 3: Consultar ranking de ofertas**
1. La empresa toca "Ranking ofertas".
2. La app envía: `go.php?action=ranking_ofertas&nit=X`.
3. El servidor cruza `OFERTA_TRABAJO` con `POSTULACION`, cuenta las postulaciones por oferta y las ordena de mayor a menor.
4. La app muestra la lista ordenada con:
   - Posición en el ranking.
   - ID y título de la oferta.
   - Cantidad de postulaciones recibidas.
   - Fechas de publicación y caducidad.

**Paso 4: Consultar postulantes por estado**
1. La empresa toca "Postulantes por estado".
2. La app envía: `go.php?action=postulantes_por_estado&nit=X`.
3. El servidor agrupa las postulaciones por `ESTADO_PROCESO` y cuenta cuántas hay en cada grupo.
4. La app muestra:
   - Pendiente: cantidad.
   - Entrevista: cantidad.
   - Contratado: cantidad.
   - Rechazado: cantidad.

### ¿Quién lo usa?
- El gerente de empresa (para tomar decisiones informadas sobre sus procesos de contratación).

---

## Nota técnica sobre la fuente de datos

Ambos servicios consultan la base de datos **MySQL remota** alojada en InfinityFree a través del archivo `go.php` que actúa como enrutador. La app Android se conecta mediante la clase `ApiService`, que utiliza la interfaz `fetch()` para hacer las peticiones HTTP. Para que el Servicio 8 funcione correctamente, primero debe ejecutarse la acción "Subir postulaciones" para sincronizar los datos locales de la tabla `POSTULACION` con el servidor. Sin ese paso, los reportes devolverán resultados vacíos.
