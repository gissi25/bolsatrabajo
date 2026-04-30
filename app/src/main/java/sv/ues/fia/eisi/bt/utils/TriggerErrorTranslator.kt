package sv.ues.fia.eisi.bt.utils

object TriggerErrorTranslator {

private val ERROR_MAP = mapOf(
        "Nivel académico insuficiente" to "No se permiten niveles de bachillerato o inferior",
        "nivel de bachillerato" to "No se permiten niveles de bachillerato o inferior",
        "Fecha inicio debe ser menor a fecha fin" to "La fecha de inicio debe ser anterior a la fecha de finalización",
        "fecha de inicio" to "La fecha de inicio debe ser anterior a la fecha de finalización",
        "Edad mínima no puede ser mayor a la máxima" to "La edad mínima no puede ser mayor a la edad máxima",
        "edad mínima" to "La edad mínima no puede ser mayor a la edad máxima",
        "La oferta ya caducó o fecha inválida" to "La fecha de caducidad debe ser posterior a la fecha de publicación",
        "fecha de caducidad" to "La fecha de caducidad debe ser posterior a la fecha de publicación",
        "El departamento asociado no existe" to "El departamento seleccionado no existe",
        "departamento" to "El departamento seleccionado no es válido",
        "Password mínimo 8 caracteres" to "La contraseña debe tener al menos 8 caracteres",
        "contraseña" to "La contraseña debe tener al menos 8 caracteres",
        "fecha de publicación es obligatoria" to "La fecha de publicación es obligatoria",
        "Nivel de destreza debe ser 1, 2 o 3" to "El nivel de destreza debe ser 1 (Básico), 2 (Intermedio) o 3 (Avanzado)",
        "nivel de destreza" to "El nivel de destreza debe ser 1, 2 o 3",
        "postulante debe ser mayor de edad" to "El postulante debe ser mayor de 18 años",
        "mayor de edad" to "El postulante debe ser mayor de 18 años",
        "Correo electrónico no válido" to "El formato del correo electrónico no es válido",
        "correo electrónico" to "El formato del correo electrónico no es válido",
        "Vacío" to "Este campo no puede estar vacío",
        "Duplicado" to "Ya existe un registro con estos datos",
        "Campos clave no pueden ser vacíos" to "Los campos obligatorios no pueden estar vacíos",
        "Campos clave vacíos" to "Los campos obligatorios no pueden estar vacíos",
        "Documento o Email ya registrado" to "Ya existe un postulante con ese documento o correo",
        "Empresa (NIT/Nombre) ya registrada" to "Ya existe una empresa con ese NIT o nombre",
        "Usuario ya existe" to "Ese nombre de usuario ya está registrado",
        "El usuario ya aplicó a esta oferta" to "Ya has aplicado a esta oferta anteriormente",
        "Red social ya vinculada al postulante" to "Esta red social ya está vinculada al postulante",
        "Habilidad ya asignada al postulante" to "Esta habilidad ya está asignada al postulante",
        "Oferta académica duplicada" to "Ya existe una oferta académica con esa institución y grado",
        "código de certificación" to "Ya existe una certificación con ese código para este postulante",
        "UNIQUE constraint failed" to "Ya existe un registro con esos datos",
        "FOREIGN KEY constraint failed" to "El valor seleccionado no existe en la tabla correspondiente",
        "NOT NULL constraint failed" to "Un campo obligatorio está vacío",
        "constraint failed" to "Operación no permitida por las reglas de validación"
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