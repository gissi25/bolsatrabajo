# Documentación de Servicios 7 y 8

---

## Servicio 7: Filtrado de ofertas por edad y postulación

### ¿Qué hace?

Este servicio permite a los postulantes buscar ofertas de trabajo que coincidan con su edad y, si lo desean, postularse a ellas. El sistema filtra automáticamente las ofertas cuyo rango de edad (mínima y máxima) sea compatible con la edad ingresada, excluyendo aquellas que ya hayan vencido.

Además, si el postulante está registrado en el sistema, puede ver si ya se ha postulado a alguna oferta y cuál es el estado de su postulación.

### ¿Cómo funciona paso a paso?

#### Acción: Buscar ofertas por edad

1. El postulante abre la pantalla de búsqueda de ofertas.
2. Ingresa su edad actual y, opcionalmente, su identificador de postulante.
3. La aplicación envía una solicitud al servidor con la edad ingresada.
4. El servidor consulta la tabla de ofertas de trabajo y filtra aquellas donde:
   - La edad del postulante está entre la edad mínima y máxima requerida.
   - La fecha de caducidad de la oferta no ha pasado (o no tiene fecha de vencimiento).
5. Si se proporcionó el identificador del postulante, también consulta la tabla de postulaciones para saber si ya aplicó a cada oferta.
6. Los resultados se ordenan desde la oferta más reciente a la más antigua.
7. El servidor devuelve la lista de ofertas disponibles con todos sus detalles (empresa, título, grado requerido, experiencia, fechas, edades).
8. La aplicación muestra las ofertas en una lista y el postulante puede ver los detalles de cada una.

#### Acción: Postularse a una oferta

1. El postulante selecciona una oferta de la lista y ve sus detalles.
2. Presiona el botón "Postularme".
3. La aplicación envía al servidor el identificador del postulante, el NIT de la empresa y el identificador de la oferta.
4. El servidor realiza las siguientes validaciones:
   - Verifica que la oferta exista en el sistema.
   - Verifica que la oferta no haya caducado.
   - Verifica que el postulante no se haya postulado anteriormente a la misma oferta.
5. Si todo está correcto:
   - Genera un nuevo identificador de postulación con formato POS### (ej. POS001, POS002).
   - Registra la postulación en la tabla de postulaciones con estado "en proceso".
6. Si ocurre algún error:
   - La postulación no se registra.
   - El servidor devuelve un mensaje claro indicando el motivo (oferta no existe, ya caducó, ya postulado).

### ¿Quién lo usa?

- Postulantes que buscan empleo según su edad.
- Personas que desean postularse a ofertas de trabajo.
- Administradores que consultan ofertas disponibles.

---



## Servicio 8: Dashboard empresarial para reclutamiento

### ¿Qué hace?

Este servicio le da a la empresa un panel de control (dashboard) donde puede ver rápida y fácilmente cómo van sus procesos de reclutamiento. La empresa ingresa su NIT y obtiene de un solo golpe toda la información importante.

### ¿Cómo funciona paso a paso?

1. La empresa abre la pantalla del dashboard e ingresa su NIT.
2. Presiona el botón "Cargar".
3. El sistema busca toda la información relacionada con esa empresa y la muestra en pantalla.
4. La empresa puede ver tres cosas principales:

   **Resumen general:**
   - Cuántas ofertas de trabajo tiene activas (las que siguen vigentes).
   - Cuántas ofertas han vencido.
   - Cuántas postulaciones ha recibido en total.
   - En promedio, cuántas personas se postulan por cada oferta.

   **Ranking de ofertas más populares:**
   - Una lista de todas sus ofertas ordenadas desde la que tiene más postulaciones hasta la que tiene menos.
   - Así la empresa sabe rápidamente qué vacantes están llamando más la atención.

   **Postulaciones por estado:**
   - Un desglose que muestra cuántos postulantes están en cada etapa del proceso (activo, en proceso, contratado, rechazado, etc.).
   - Cada estado se muestra con su cantidad y su porcentaje.

5. Toda esta información se actualiza cada vez que la empresa carga el dashboard.

### ¿Quién lo usa?

- Empresas que publican ofertas de trabajo.
- Departamentos de recursos humanos.
- Reclutadores que gestionan el proceso de selección.
- Gerentes que toman decisiones basadas en datos de reclutamiento.


