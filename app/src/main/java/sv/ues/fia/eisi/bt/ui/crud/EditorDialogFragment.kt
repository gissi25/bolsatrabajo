package sv.ues.fia.eisi.bt.ui.crud

import android.content.res.TypedArray
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.InputMaskUtils
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.utils.TriggerErrorTranslator
import sv.ues.fia.eisi.bt.utils.ValidationRules
import sv.ues.fia.eisi.bt.viewmodel.CrudViewModel
import sv.ues.fia.eisi.bt.viewmodel.Resource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditorDialogFragment : DialogFragment() {

    private val viewModel: CrudViewModel by viewModels({ requireParentFragment() })
    private var tableName: String = ""
    private var isEditMode: Boolean = false
    private var itemData: List<String> = emptyList()

    private lateinit var tilFieldsContainer: LinearLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var columns: List<String>
    private lateinit var fkRefs: Map<String, MainRepository.FkReference>

    private val dropDownFields = mutableMapOf<Int, Pair<String, MaterialAutoCompleteTextView>>()
    private val textFields = mutableMapOf<Int, Pair<String, TextInputEditText>>()
    private val nivelDestrezaFields = mutableMapOf<Int, String>()
    private val estadoProcesoFields = mutableMapOf<Int, String>()

    private var docTypeColumnIndex: Int = -1
    private var numDocColumnIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AestheticDialog)
        arguments?.let {
            tableName = it.getString(Constants.BUNDLE_TABLE_NAME, "")
            isEditMode = it.getBoolean(Constants.BUNDLE_IS_EDIT_MODE, false)
            val dataString = it.getString(Constants.BUNDLE_TABLE_DATA, "")
            itemData = if (dataString.isNotBlank()) dataString.split(",") else emptyList()
        }
        columns = getColumnsForTable(tableName)
        fkRefs = viewModel.getFkReferences(tableName)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvTitle)
        tilFieldsContainer = view.findViewById(R.id.tilFields)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        setupTitle()
        setupFields()
        setupButtons()

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            btnSave.isEnabled = true
            btnSave.text = if (isEditMode) "Actualizar" else "Guardar"
            when (result) {
                is Resource.Success -> {
                    StyledToast.show(requireContext(), result.message)
                    viewModel.clearResult()
                    dismiss()
                }
                is Resource.Error -> {
                    StyledToast.show(requireContext(), result.translatedMessage)
                    viewModel.clearResult()
                }
            }
        }
    }

    private fun setupTitle() {
        btnSave.text = if (isEditMode) getString(R.string.save) else getString(R.string.add)
        tvTitle.text = if (isEditMode) "Editar $tableName" else "Nuevo $tableName"
    }

    private fun setupFields() {
        tilFieldsContainer.removeAllViews()
        dropDownFields.clear()
        textFields.clear()
        nivelDestrezaFields.clear()
        docTypeColumnIndex = -1
        numDocColumnIndex = -1

        if (columns.isEmpty()) {
            columns = listOf("NOMBRE")
        }

        columns.filter { it != getAutoGenColumn(tableName) }.forEachIndexed { idx, column ->
            val colIndex = columns.indexOf(column)
            val isFk = fkRefs.containsKey(column)
            val isNivelDestreza = column == "NIVEL_DESTREZA"
            val isEstadoProceso = column == "ESTADO_PROCESO"
            val isRol = column == "ROL"

            if (column == "ID_TIPO_DOCUMENTO") docTypeColumnIndex = colIndex
            if (column == "NUM_DOCUMENTO") numDocColumnIndex = colIndex
            
            if (isFk) {
                createDropdownField(idx, column, colIndex)
            } else if (isNivelDestreza) {
                createNivelDestrezaDropdown(idx, column, colIndex)
            } else if (isEstadoProceso) {
                createEstadoProcesoDropdown(idx, column, colIndex)
            } else if (isRol) {
                createRolDropdown(idx, column, colIndex)
            } else {
                createTextInputField(idx, column, colIndex)
            }
        }
    }

    private fun createDropdownField(idx: Int, column: String, colIndex: Int) {
        val fkRef = fkRefs[column] ?: return
        
        var parentId: String? = null
        if (column == "ID_MUNICIPIO" && fkRefs.containsKey("ID_DEPARTAMENTO")) {
            val parentAutoComplete = dropDownFields.entries.find { it.value.first == "ID_DEPARTAMENTO" }?.value?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
        }
        if (column == "ID_DISTRITO" && fkRefs.containsKey("ID_MUNICIPIO")) {
            val parentAutoComplete = dropDownFields.entries.find { it.value.first == "ID_MUNICIPIO" }?.value?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
        }

        val options = if (parentId != null && parentId.isNotBlank()) {
            val parentFk = fkRefs[if (column == "ID_MUNICIPIO") "ID_DEPARTAMENTO" else "ID_MUNICIPIO"]!!
            viewModel.getFilteredOptions(fkRef.refTable, parentFk.fkColumn, parentId)
        } else {
            viewModel.getDropdownOptions(fkRef.refTable, fkRef.refDisplayColumn)
        }

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = column.replace("ID_", "").replace("_", " ")
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = options.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)
        
        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if (isEditMode && colIndex < itemData.size) {
            val currentId = itemData[colIndex].trim()
            val optionIndex = options.indexOfFirst { it.first == currentId }
            if (optionIndex >= 0) {
                autoComplete.setText(displayOptions[optionIndex], false)
            }
        }

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < options.size) {
                if (column == "ID_DEPARTAMENTO") {
                    refreshDependentDropdown("ID_MUNICIPIO")
                }
                if (column == "ID_MUNICIPIO") {
                    refreshDependentDropdown("ID_DISTRITO")
                }
                if (column == "ID_TIPO_DOCUMENTO") {
                    refreshNumDocHintAndValidation()
                }
            }
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        dropDownFields[colIndex] = Pair(column, autoComplete)
    }

    private fun createNivelDestrezaDropdown(idx: Int, column: String, colIndex: Int) {
        val nivelOptions = listOf(
            "1" to "Básico",
            "2" to "Intermedio",
            "3" to "Avanzado"
        )
        
        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Nivel Destreza"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = nivelOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)
        
        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if (isEditMode && colIndex < itemData.size) {
            val currentValue = itemData[colIndex].trim()
            val optionIndex = nivelOptions.indexOfFirst { it.first == currentValue }
            if (optionIndex >= 0) {
                autoComplete.setText(displayOptions[optionIndex], false)
            }
        }

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < nivelOptions.size) {
                nivelDestrezaFields[colIndex] = nivelOptions[position].first
            }
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        nivelDestrezaFields[colIndex]?.let { 
            val idx = nivelOptions.indexOfFirst { pair -> pair.first == it }
            if (idx >= 0) autoComplete.setText(displayOptions[idx], false)
        }
        dropDownFields[colIndex] = Pair(column, autoComplete)
    }

    private fun createEstadoProcesoDropdown(idx: Int, column: String, colIndex: Int) {
        val estadoOptions = listOf(
            "activo" to "Activo",
            "en proceso" to "En Proceso",
            "contratado" to "Contratado",
            "rechazado" to "Rechazado"
        )
        
        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Estado Proceso"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = estadoOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)
        
        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if (isEditMode && colIndex < itemData.size) {
            val currentValue = itemData[colIndex].trim()
            val optionIndex = estadoOptions.indexOfFirst { it.first == currentValue }
            if (optionIndex >= 0) {
                autoComplete.setText(displayOptions[optionIndex], false)
            }
        }

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < estadoOptions.size) {
                estadoProcesoFields[colIndex] = estadoOptions[position].first
            }
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        dropDownFields[colIndex] = Pair(column, autoComplete)
    }

    private fun createRolDropdown(idx: Int, column: String, colIndex: Int) {
        val roles = arrayOf("postulante", "empresa", "admin")
        
        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Rol"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        autoComplete.setAdapter(adapter)
        
        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if (isEditMode && colIndex < itemData.size) {
            val currentValue = itemData[colIndex].trim()
            if (roles.contains(currentValue)) {
                autoComplete.setText(currentValue, false)
            } else {
                autoComplete.setText(roles[0], false)
            }
        } else {
            autoComplete.setText("postulante", false)
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        dropDownFields[colIndex] = Pair(column, autoComplete)
    }

    private fun getSelectedDropdownValue(autoComplete: MaterialAutoCompleteTextView?): String? {
        if (autoComplete == null) return null
        val text = autoComplete.text.toString()
        for ((idx, pair) in dropDownFields) {
            if (pair.second == autoComplete) {
                val fkRef = fkRefs[pair.first] ?: continue
                val options = viewModel.getDropdownOptions(fkRef.refTable, fkRef.refDisplayColumn)
                val index = options.indexOfFirst { it.second == text }
                return if (index >= 0) options[index].first else null
            }
        }
        return null
    }

    private fun getSelectedDocType(): String? {
        if (docTypeColumnIndex == -1 || !fkRefs.containsKey("ID_TIPO_DOCUMENTO")) return null
        val autoComplete = dropDownFields[docTypeColumnIndex]?.second ?: return null
        val text = autoComplete.text?.toString()?.trim() ?: return null
        if (text.isBlank()) return null
        return when (text.lowercase()) {
            "dui" -> "DUI"
            "nit" -> "NIT"
            "pasaporte" -> "PASAPORTE"
            else -> text.uppercase()
        }
    }

    private fun refreshNumDocHintAndValidation() {
        if (numDocColumnIndex == -1) return
        val pair = textFields[numDocColumnIndex] ?: return
        val et = pair.second
        val til = et.parent as? TextInputLayout ?: return

        val tipo = getSelectedDocType()
        til.hint = when (tipo) {
            "DUI" -> "DUI: 12345678-9"
            "NIT" -> "NIT: 0614-111222-333-4"
            "PASAPORTE" -> "Pasaporte (máx 9 caracteres)"
            else -> "DUI: 12345678-9 / NIT: 0614-111222-333-4 / Pasaporte"
        }

        val text = et.text?.toString() ?: return
        if (text.isNotBlank()) {
            val formatted = formatInput("NUM_DOCUMENTO", text, tipo)
            if (formatted != text) {
                et.setText(formatted)
                et.setSelection(formatted.length)
            }
            val error = getFieldValidationError("NUM_DOCUMENTO", formatted)
            til.error = error
        }
    }

    private fun refreshDependentDropdown(childColumn: String) {
        val childInfo = dropDownFields.entries.find { it.value.first == childColumn } ?: return
        val childIdx = childInfo.key
        val childAutoComplete = childInfo.value.second
        val childTil = childAutoComplete.parent as? TextInputLayout ?: return
        
        val parentFkColumn = when (childColumn) {
            "ID_MUNICIPIO" -> "ID_DEPARTAMENTO"
            "ID_DISTRITO" -> "ID_MUNICIPIO"
            else -> return
        }
        val parentFk = fkRefs[parentFkColumn] ?: return
        val parentAutoComplete = dropDownFields.entries.find { it.value.first == parentFkColumn }?.value?.second
        val parentId = getSelectedDropdownValue(parentAutoComplete) ?: return

        val newOptions = viewModel.getFilteredOptions(
            fkRefs[childColumn]!!.refTable,
            parentFk.fkColumn,
            parentId
        )
        
        val displayOptions = newOptions.map { it.second }
        childAutoComplete.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        )
        childAutoComplete.setText("", false)
        childTil.hint = childColumn.replace("ID_", "").replace("_", " ")
    }

    private fun createTextInputField(idx: Int, column: String, colIndex: Int) {
        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = getHintText(column)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val et = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            inputType = getInputType(column)
            filters = getFilters(column)
        }

        // PASSWORD bloqueado en edición de USUARIO (se guarda encriptado)
        if (column == "PASSWORD" && tableName == "USUARIO" && isEditMode) {
            et.isEnabled = false
            et.isFocusable = false
            til.hint = "Contraseña (bloqueada en edición)"
        }

        if (column.contains("FECHA")) {
            et.isFocusable = false
            et.isClickable = true
            et.setOnClickListener { showDatePicker(et) }
        }

        if (isEditMode && colIndex < itemData.size) {
            et.setText(itemData[colIndex].trim())
        }

        // TextWatcher para máscaras y validación en tiempo real
        et.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val text = s?.toString() ?: ""
                val formatted = formatInput(column, text)
                if (formatted != text) {
                    et.setText(formatted)
                    et.setSelection(formatted.length)
                }

                val error = getFieldValidationError(column, formatted)
                til.error = error

                isFormatting = false
            }
        })

        til.addView(et)
        tilFieldsContainer.addView(til)
        textFields[colIndex] = Pair(column, et)
    }

    private fun showDatePicker(editText: TextInputEditText) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val currentText = editText.text?.toString() ?: ""
        val initialMillis = if (currentText.isNotEmpty()) {
            try {
                dateFormat.parse(currentText)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
        
        val picker = MaterialDatePicker.Builder
            .datePicker()
            .setTitleText("Seleccionar fecha")
            .setSelection(initialMillis)
            .build()
        
        picker.addOnPositiveButtonClickListener { selection ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = selection
            editText.setText(dateFormat.format(cal.time))
        }
        
        picker.show(parentFragmentManager, "date_picker")
    }

    private fun formatInput(column: String, text: String, docType: String? = null): String {
        if (text.isEmpty()) return text

        return when {
            column.contains("NUM_DOCUMENTO") -> {
                val tipo = docType ?: getSelectedDocType()
                when (tipo) {
                    "NIT" -> InputMaskUtils.formatNIT(text)
                    "DUI" -> InputMaskUtils.formatDUI(text)
                    else -> text
                }
            }
            column.contains("TELEFONO") || column.contains("TEL") || column == "CONTACTO_REFERENCIA" -> InputMaskUtils.formatTelefono(text)
            else -> text
        }
    }

    private fun getFilters(column: String): Array<InputFilter> {
        val maxLength = when {
            column.contains("TELEFONO") || column.contains("TEL") -> InputMaskUtils.TELEFONO_LENGTH + 1
            column == "CONTACTO_REFERENCIA" -> InputMaskUtils.TELEFONO_LENGTH + 1
            column.contains("NUP") -> InputMaskUtils.NUP_LENGTH
            column.contains("NUM_DOCUMENTO") -> 17
            column.contains("CODIGO") || column.contains("CERTIFICACION") -> 30
            column.contains("NIVEL_DESTREZA") -> 1
            column.contains("EXPERIENCIA_ANIOS") -> 2
            column.contains("EDAD_MINIMA") || column.contains("EDAD_MAXIMA") -> 2
            else -> 0
        }

        val filters = mutableListOf<InputFilter>()
        if (maxLength > 0) filters.add(InputFilter.LengthFilter(maxLength))

        if (column.contains("EXPERIENCIA_ANIOS") || column.contains("EDAD_MIN") || column.contains("EDAD_MAX") || column == "NIVEL_DESTREZA") {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    if (!source[i].isDigit()) return@InputFilter ""
                }
                null
            })
        }

        return filters.toTypedArray()
    }

    private fun getInputType(column: String): Int {
        return when {
            column.startsWith("ID_") || column.contains("NUP") ->
                android.text.InputType.TYPE_CLASS_NUMBER
            column.contains("NUM_") ->
                android.text.InputType.TYPE_CLASS_TEXT
            column.contains("FECHA") || column.contains("DATE") ->
                android.text.InputType.TYPE_CLASS_TEXT
            column.contains("EMAIL") -> 
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            column.contains("TELEFONO") || column.contains("TEL") || column == "CONTACTO_REFERENCIA" -> 
                android.text.InputType.TYPE_CLASS_PHONE
            column.contains("DOCUMENTO") -> 
                android.text.InputType.TYPE_CLASS_TEXT
            column.contains("PASSWORD") || column.contains("CONTRA") -> 
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            column.contains("EXPERIENCIA_ANIOS") || column.contains("EDAD_MIN") || column.contains("EDAD_MAX") || column.contains("NIVEL_DESTREZA") ->
                android.text.InputType.TYPE_CLASS_NUMBER
            column.contains("DESCRIPCION") || column.contains("DETALLE") -> 
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val ta: TypedArray = requireContext().obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, android.graphics.Color.BLACK)
        ta.recycle()
        return color
    }

    private fun getHintText(column: String): String {
        return when (column.uppercase()) {
            "NOMBRE", "NOMBRE_CATEGORIA", "NOMBRE_GENERO", "NOMBRE_TIPO", "NOMBRE_DEPARTAMENTO", "NOMBRE_MUNICIPIO", "NOMBRE_DISTRITO", "NOMBRE_HABILIDAD", "NOMBRE_EMPRESA", "NOMBRE_INSTITUCION", "NOMBRE_GRADO", "NOMBRE_RED" -> "Ingrese el nombre"
            "APELLIDO" -> "Ingrese el apellido"
            "EMAIL", "CORREO" -> "ejemplo@correo.com"
            "TELEFONO", "TEL" -> "Ingrese teléfono (ej: 2222-1111)"
            "TELEFONO_CASA" -> "Ingrese telefono casa"
            "TELEFONO_CELULAR" -> "Ingrese telefono celular"
            "NUP" -> "Ingrese NUP"
            "NUM_DOCUMENTO" -> "DUI: 12345678-9 / NIT: 0614-111222-333-4 / Pasaporte"
            "CODIGO" -> "Ingrese código"
            "CONTACTO_REFERENCIA" -> "Ingrese contacto de referencia"
            "PUESTO_TRABAJO" -> "Ingrese nombre del puesto"
            "TITULO_PUESTO", "TITULO_OBTENIDO" -> "Ingrese el título"
            "DESCRIPCION", "DESCRIPCION_OFERTA", "DESCRIPCION_REQUISITO" -> "Ingrese la descripción"
            "EXPERIENCIA_ANIOS" -> "Años de experiencia (0-${InputMaskUtils.EXPERIENCIA_MAX})"
            "EDAD_MINIMA" -> "Edad mínima (${InputMaskUtils.EDAD_MIN}-${InputMaskUtils.EDAD_MAX})"
            "EDAD_MAXIMA" -> "Edad máxima (${InputMaskUtils.EDAD_MIN}-${InputMaskUtils.EDAD_MAX})"
            "NIVEL_DESTREZA" -> "1=Básico, 2=Intermedio, 3=Avanzado"
            "FECHA_NACIMIENTO" -> "Fecha de nacimiento (AAAA-MM-DD)"
            "FECHA_INICIO" -> "Fecha de inicio (AAAA-MM-DD)"
            "FECHA_FIN" -> "Fecha de fin (AAAA-MM-DD)"
            "FECHA_CERTIFICACION" -> "Fecha de certificación (AAAA-MM-DD)"
            "FECHA_PUBLICACION" -> "Fecha de publicación (AAAA-MM-DD)"
            "FECHA_CADUCIDAD" -> "Fecha de caducidad (AAAA-MM-DD)"
            "FECHA_APLICACION" -> "Fecha de aplicación (AAAA-MM-DD)"
            "FECHA_OBTENCION" -> "Fecha de obtención (AAAA-MM-DD)"
            "ESTADO_PROCESO" -> "Ingrese estado (activo, en proceso, contratado, rechazado)"
            "USERNAME", "USER" -> "Ingrese nombre de usuario"
            "PASSWORD", "CONTRA" -> "Mínimo 8 caracteres"
            "ROL" -> "postulante, empresa, admin"
            "URL_PERFIL" -> "https://..."
            "NOMBRE_CERTIFICACION" -> "Ingrese nombre de certificación"
            "CODIGO_CERTIFICACION" -> "Ingrese código de certificación"
            "DESCP_EXPERIENCIA_LABORAL", "DESC" -> "Ingrese descripción de experiencia"
            else -> "Ingrese $column"
        }
    }

private fun validateField(column: String, value: String): String? {
        // Solo validar campos requeridos específicos
        val requiredColumns = listOf("NOMBRE", "APELLIDO", "NOMBRE_EMPRESA", "NOMBRE_GENERO", 
            "NOMBRE_TIPO", "NOMBRE_DEPARTAMENTO", "NOMBRE_MUNICIPIO", "NOMBRE_DISTRITO",
            "NOMBRE_HABILIDAD", "NOMBRE_CATEGORIA", "NOMBRE_GRADO", "NOMBRE_RED",
            "TITULO_PUESTO", "NOMBRE_CERTIFICACION", "PUESTO_TRABAJO", "TITULO_OBTENIDO")
        
        if (value.isBlank() && column.uppercase() in requiredColumns) {
            return "El campo $column es requerido"
        }
        
        if (value.isBlank()) return null  // Campo opcional vacío es válido
        
        when (column.uppercase()) {
            "EMAIL", "CORREO" -> {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    return "Correo electrónico inválido"
                }
            }
            "FECHA_NACIMIENTO", "FECHA_INICIO", "FECHA_FIN", "FECHA_CERTIFICACION", "FECHA_PUBLICACION", "FECHA_CADUCIDAD", "FECHA_APLICACION", "FECHA_OBTENCION" -> {
                if (!value.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                    return "Formato fecha inválido (use AAAA-MM-DD)"
                }
            }
            "NIVEL_DESTREZA" -> {
                if (value.toIntOrNull() !in 1..3) {
                    return "Nivel debe ser 1, 2 o 3"
                }
            }
            "EDAD_MINIMA", "EDAD_MAXIMA" -> {
                val edad = value.toIntOrNull()
                if (edad == null || edad < 16 || edad > 100) {
                    return "Edad debe estar entre 16 y 100"
                }
            }
            "EXPERIENCIA_ANIOS" -> {
                val años = value.toIntOrNull()
                if (años == null || años < 0 || años > 50) {
                    return "Años de experiencia inválidos"
                }
            }
            "PASSWORD", "CONTRA" -> {
                if (value.length < 8) {
                    return "La contraseña debe tener al menos 8 caracteres"
                }
            }
            "URL_PERFIL" -> {
                if (!value.startsWith("http://") && !value.startsWith("https://")) {
                    return "URL debe comenzar con http:// o https://"
                }
            }
        }
        return null
    }

    private fun getFieldValidationError(column: String, value: String): String? {
        if (value.isBlank()) return null

        return when {
            column.contains("EMAIL") -> InputMaskUtils.validateEmail(value)
            column.contains("NIVEL_DESTREZA") -> {
                if (value.toIntOrNull() !in 1..3) "Nivel debe ser 1, 2 o 3" else null
            }
            column.contains("EDAD_MINIMA") || column.contains("EDAD_MAXIMA") ->
                InputMaskUtils.validateRango(value, InputMaskUtils.EDAD_MIN, InputMaskUtils.EDAD_MAX, "Edad")
            column.contains("EXPERIENCIA_ANIOS") ->
                InputMaskUtils.validateRango(value, InputMaskUtils.EXPERIENCIA_MIN, InputMaskUtils.EXPERIENCIA_MAX, "Experiencia")
            column.contains("PASSWORD") || column.contains("CONTRA") -> InputMaskUtils.validatePassword(value)
            column.contains("URL_PERFIL") -> InputMaskUtils.validateURL(value)
            column.contains("NUP") -> {
                if (value.length != InputMaskUtils.NUP_LENGTH || !value.all { it.isDigit() })
                    "NUP debe tener ${InputMaskUtils.NUP_LENGTH} dígitos" else null
            }
            column.contains("NUM_DOCUMENTO") -> {
                val tipo = getSelectedDocType()
                val digits = value.filter { it.isDigit() }
                when (tipo) {
                    "NIT" -> {
                        if (digits.length != 14) "NIT debe tener 14 dígitos" else null
                    }
                    "DUI" -> {
                        if (digits.length != InputMaskUtils.DUI_LENGTH) "DUI debe tener ${InputMaskUtils.DUI_LENGTH} dígitos" else null
                    }
                    else -> {
                        if (value.length > InputMaskUtils.PASAPORTE_LENGTH) "Pasaporte: máximo ${InputMaskUtils.PASAPORTE_LENGTH} caracteres" else null
                    }
                }
            }
            column.contains("FECHA") -> InputMaskUtils.validateFecha(value)
            column.contains("TELEFONO") || column.contains("TEL") || column == "CONTACTO_REFERENCIA" -> {
                val digits = value.filter { it.isDigit() }
                if (digits.length != InputMaskUtils.TELEFONO_LENGTH && digits.isNotEmpty())
                    "Teléfono debe tener ${InputMaskUtils.TELEFONO_LENGTH} dígitos" else null
            }
            else -> null
        }
    }

    private fun getColumnsForTable(table: String): List<String> {
        return when (table) {
            "USUARIO" -> listOf("ID_USUARIO", "USERNAME", "PASSWORD", "ROL")
            "POSTULANTE" -> listOf("ID_POSTULANTE", "ID_GENERO", "ID_TIPO_DOCUMENTO", "NUM_DOCUMENTO", "ID_DISTRITO", "NOMBRE", "APELLIDO", "FECHA_NACIMIENTO", "NUP", "DIRECCION_DETALLE", "TELEFONO_CASA", "TELEFONO_CELULAR", "EMAIL")
            "GENERO" -> listOf("ID_GENERO", "NOMBRE_GENERO")
            "TIPO_DOCUMENTO" -> listOf("ID_TIPO_DOCUMENTO", "NOMBRE_TIPO")
            "DEPARTAMENTO" -> listOf("ID_DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
            "MUNICIPIO" -> listOf("ID_MUNICIPIO", "ID_DEPARTAMENTO", "NOMBRE_MUNICIPIO")
            "DISTRITO" -> listOf("ID_DISTRITO", "ID_MUNICIPIO", "NOMBRE_DISTRITO")
            "HABILIDAD" -> listOf("ID_HABILIDAD", "ID_CATEGORIA_HABILIDAD", "NOMBRE_HABILIDAD")
            "CATEGORIA_HABILIDAD" -> listOf("ID_CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA")
            "EMPRESA" -> listOf("ID_EMPRESA", "ID_DISTRITO", "NOMBRE_EMPRESA", "CONTACTO_DIRECTO", "NIT")
            "INSTITUCION" -> listOf("ID_INSTITUCION", "NOMBRE_INSTITUCION")
            "GRADO_ACADEMICO" -> listOf("ID_GRADO_ACADEMICO", "NOMBRE_GRADO")
            "RED_SOCIAL" -> listOf("ID_RED_SOCIAL", "NOMBRE_RED")
            "OFERTA_ACADEMICA" -> listOf("ID_OFERTA_ACADEMICA", "ID_GRADO_ACADEMICO", "ID_INSTITUCION")
            "OFERTA_TRABAJO" -> listOf("ID_EMPRESA", "ID_OFERTA", "ID_GRADO_ACADEMICO", "TITULO_PUESTO", "FECHA_PUBLICACION", "FECHA_CADUCIDAD", "EXPERIENCIA_ANIOS", "EDAD_MINIMA", "EDAD_MAXIMA", "DESCRIPCION_OFERTA_TRABAJO")
            "CERTIFICACION" -> listOf("ID_POSTULANTE", "ID_CERTIFICACION", "ID_INSTITUCION", "NOMBRE_CERTIFICACION", "CODIGO_CERTIFICACION", "FECHA_CERTIFICACION")
            "EXPERIENCIA_LABORAL" -> listOf("ID_POSTULANTE", "ID_EXPERIENCIA", "ID_EMPRESA", "PUESTO_TRABAJO", "FECHA_INICIO", "FECHA_FIN", "DESCP_EXPERIENCIA_LABORAL", "CONTACTO_REFERENCIA")
            "FORMACION_ACADEMICA" -> listOf("ID_FORMACION", "ID_POSTULANTE", "ID_OFERTA_ACADEMICA", "TITULO_OBTENIDO", "FECHA_OBTENCION")
            "HABILIDAD_POSTULANTE" -> listOf("ID_POSTULANTE", "ID_HABILIDAD", "ID_HABILIDAD_POSTULANTE", "NIVEL_DESTREZA")
            "POSTULACION" -> listOf("ID_EMPRESA", "ID_OFERTA", "ID_POSTULANTE", "ID_POSTULACION", "FECHA_APLICACION", "ESTADO_PROCESO")
            "DETALLE_REQUISITO" -> listOf("ID_DETALLE", "ID_EMPRESA", "ID_OFERTA", "DESCRIPCION_REQUISITO")
            "RED_SOCIAL_POSTULANTE" -> listOf("ID_RED_POSTUALNTE", "ID_POSTULANTE", "ID_RED_SOCIAL", "URL_PERFIL")
            else -> listOf("ID", "NOMBRE")
        }
    }

    private fun setupButtons() {
        btnSave.setOnClickListener { saveData() }
        btnCancel.setOnClickListener { dismiss() }
    }

    private fun saveData() {
        val editableColumns = columns.filter { it != getAutoGenColumn(tableName) }
        val values = mutableListOf<String>()
        var hasRequiredFk = false

        // Step 1: Clear all field errors
        for (entry in textFields.values) {
            val parent = entry.second.parent?.parent
            if (parent is TextInputLayout) parent.error = null
        }

        // Step 2: Validate all fields locally
        for (col in editableColumns) {
            val fkRef = fkRefs[col]

            if (col == "NIVEL_DESTREZA") {
                val autoComplete = dropDownFields.entries.find { it.value.first == col }?.value?.second
                val selectedText = autoComplete?.text?.toString()?.trim() ?: ""
                if (selectedText.isBlank()) {
                    StyledToast.show(requireContext(), "Debe seleccionar un nivel de destreza")
                    return
                }
                val nivelValue = when (selectedText) {
                    "Básico" -> "1"
                    "Intermedio" -> "2"
                    "Avanzado" -> "3"
                    else -> ""
                }
                values.add(nivelValue)
            } else if (col == "ESTADO_PROCESO") {
                val autoComplete = dropDownFields.entries.find { it.value.first == col }?.value?.second
                val selectedText = autoComplete?.text?.toString()?.trim() ?: ""
                if (selectedText.isBlank()) {
                    StyledToast.show(requireContext(), "Debe seleccionar un estado")
                    return
                }
                val estadoValue = when (selectedText) {
                    "Activo" -> "activo"
                    "En Proceso" -> "en proceso"
                    "Contratado" -> "contratado"
                    "Rechazado" -> "rechazado"
                    else -> ""
                }
                values.add(estadoValue)
            } else if (col == "ROL") {
                val autoComplete = dropDownFields.entries.find { it.value.first == col }?.value?.second
                val selectedText = autoComplete?.text?.toString()?.trim() ?: "postulante"
                values.add(selectedText)
            } else if (fkRef != null) {
                val options = viewModel.getDropdownOptions(fkRef.refTable, fkRef.refDisplayColumn)
                if (options.isEmpty()) {
                    StyledToast.show(requireContext(), "No hay datos en ${fkRef.refTable}. Créelos primero.")
                    return
                }
                hasRequiredFk = true

                val autoComplete = dropDownFields.entries.find { it.value.first == col }?.value?.second
                val selectedText = autoComplete?.text?.toString()?.trim() ?: ""
                if (selectedText.isBlank()) {
                    StyledToast.show(requireContext(), "Debe seleccionar ${fkRef.refTable}")
                    return
                }

                val selectedOption = options.find { it.second == selectedText }
                if (selectedOption == null) {
                    StyledToast.show(requireContext(), "Seleccione una opción válida de ${fkRef.refTable}: $selectedText")
                    return
                }
                values.add(selectedOption.first)
            } else {
                val et = textFields.entries.find { it.value.first == col }?.value?.second
                val textValue = et?.text?.toString()?.trim() ?: ""

                val errorMsg = ValidationRules.validate(tableName, col, textValue)
                        ?: validateField(col, textValue)
                if (errorMsg != null) {
                    val parent = et?.parent?.parent
                    if (parent is TextInputLayout) {
                        parent.error = errorMsg
                    }
                    StyledToast.show(requireContext(), errorMsg)
                    return
                }

                values.add(textValue)
            }
        }

        if (values.isEmpty()) {
            StyledToast.show(requireContext(), getString(R.string.error))
            return
        }

        // Step 3: Disable save button while operation in progress
        btnSave.isEnabled = false
        btnSave.text = "Guardando..."

        // Step 4: Execute DB operation
        if (isEditMode && itemData.isNotEmpty()) {
            val id = itemData.first().trim()
            viewModel.updateRecord(tableName, id, values)
        } else {
            viewModel.insertRecord(tableName, values)
        }
    }

    private fun getAutoGenColumn(table: String): String {
        return when (table) {
            "OFERTA_TRABAJO" -> "ID_OFERTA"
            "EXPERIENCIA_LABORAL" -> "ID_EXPERIENCIA"
            "CERTIFICACION" -> "ID_CERTIFICACION"
            "HABILIDAD_POSTULANTE" -> "ID_HABILIDAD_POSTULANTE"
            "POSTULACION" -> "ID_POSTULACION"
            else -> columns.firstOrNull() ?: "ID"
        }
    }
}