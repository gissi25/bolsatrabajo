package sv.ues.fia.eisi.bt.utils

object TriggerErrorTranslator {

    private val ERROR_MAP = mapOf(
        "Duplicado: Ya existe un registro con esa clave" to "Ya existe un registro con esa combinacion de valores",
        "Ya existe un genero con ese nombre" to "Ya existe un genero con ese nombre",
        "Ya existe una categoria con ese nombre" to "Ya existe una categoria con ese nombre",
        "Ya existe un tipo de documento con ese nombre" to "Ya existe un tipo de documento con ese nombre",
        "Ya existe un departamento con ese nombre" to "Ya existe un departamento con ese nombre",
        "Ya existe un grado academico con ese nombre" to "Ya existe un grado academico con ese nombre",
        "Ya existe una red social con ese nombre" to "Ya existe una red social con ese nombre",
        "Ya existe una institucion con ese nombre" to "Ya existe una institucion con ese nombre",
        "Ya existe un municipio con ese nombre " to "Ya existe un municipio con ese nombre",
        "Ya existe un distrito con ese nombre en el municipio" to "Ya existe un distrito con ese nombre en el municipio",
        "Ya existe una habilidad con ese nombre" to "Ya existe una habilidad con ese nombre",
        "Ya existe una empresa con ese NIT" to "Ya existe una empresa con ese NIT",
        "Ya existe una empresa con ese nombre" to "Ya existe una empresa con ese nombre",
        "Ya existe un postulante con ese documento" to "Ya existe un postulante con ese documento",
        "Ya existe un postulante con ese email" to "Ya existe un postulante con ese email",
        "Ya existe un usuario con ese nombre" to "Ya existe un usuario con ese nombre",
        "Ya existe una oferta con ese titulo en la empresa" to "Ya existe una oferta con ese titulo en la empresa",
        "Ya existe un requisito con esa descripcion en la oferta" to "Ya existe un requisito con esa descripcion en la oferta",
        "Ya existe una experiencia con ese puesto para el postulante" to "Ya existe una experiencia con ese puesto para el postulante",
        "Ya existe una certificacion con ese codigo para el postulante" to "Ya existe una certificacion con ese codigo para el postulante",
        "El postulante ya aplico a esta oferta" to "El postulante ya aplico a esta oferta",
        "La red social ya esta vinculada al postulante" to "La red social ya esta vinculada al postulante",
        "La habilidad ya esta asignada al postulante" to "La habilidad ya esta asignada al postulante",
        "Ya existe una oferta academica para esa institucion y grado" to "Ya existe una oferta academica para esa institucion y grado",
        "Nivel academico insuficiente" to "No se permiten niveles de bachillerato o inferior",
        "nivel de bachillerato" to "No se permiten niveles de bachillerato o inferior",
        "Fecha inicio debe ser menor a fecha fin" to "La fecha de inicio debe ser anterior a la fecha de finalizacion",
        "fecha de inicio" to "La fecha de inicio debe ser anterior a la fecha de finalizacion",
        "Edad minima no puede ser mayor a la maxima" to "La edad minima no puede ser mayor a la edad maxima",
        "edad minima" to "La edad minima no puede ser mayor a la edad maxima",
        "La oferta ya caduco o fecha invalida" to "La fecha de caducidad debe ser posterior a la fecha de publicacion",
        "fecha de caducidad" to "La fecha de caducidad debe ser posterior a la fecha de publicacion",
        "El departamento asociado no existe" to "El departamento seleccionado no existe",
        "departamento" to "ya existe un registro con esa combinacion de valores",
        "El municipio asociado no existe" to "El municipio seleccionado no existe",
        "municipio" to "El municipio seleccionado no es valido",
        "La categoria asociada no existe" to "La categoria seleccionada no existe",
        "categoria" to "La categoria seleccionada no es valida",
        "El distrito asociado no existe" to "El distrito seleccionado no existe",
        "distrito" to "El distrito seleccionado no es valido",
        "El genero asociado no existe" to "El genero seleccionado no existe",
        "genero" to "El genero seleccionado no es valido",
        "El tipo de documento asociado no existe" to "El tipo de documento seleccionado no existe",
        "tipo de documento" to "El tipo de documento seleccionado no es valido",
        "Password minimo 8 caracteres" to "La contrasena debe tener al menos 8 caracteres",
        "contrasena" to "La contrasena debe tener al menos 8 caracteres",
        "Nivel de destreza debe ser 1, 2 o 3" to "El nivel de destreza debe ser 1 (Basico), 2 (Intermedio) o 3 (Avanzado)",
        "nivel de destreza" to "El nivel de destreza debe ser 1, 2 o 3",
        "postulante debe ser mayor de edad" to "El postulante debe ser mayor de 18 anos",
        "mayor de edad" to "El postulante debe ser mayor de 18 anos",
        "Correo electronico no valido" to "El formato del correo electronico no es valido",
        "correo electronico" to "El formato del correo electronico no es valido",
        "fecha de nacimiento" to "La fecha de nacimiento no es valida",
        "UNIQUE constraint failed" to "Ya existe un registro con esos datos",
        "FOREIGN KEY constraint failed" to "NO ES POSIBLE ELIMINAR ",
        "NOT NULL constraint failed" to "Un campo obligatorio esta vacio",
        "constraint failed" to "Operacion no permitida por las reglas de validacion"
    )

    fun translate(errorMessage: String?): String {
        if (errorMessage.isNullOrBlank()) return "Error desconocido"
        ERROR_MAP.entries.forEach { (key, value) ->
            if (errorMessage.contains(key, ignoreCase = true)) {
                return value
            }
        }
        return errorMessage
    }
}
