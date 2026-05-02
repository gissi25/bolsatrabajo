package sv.ues.fia.eisi.bt.ui.crud

import android.content.res.TypedArray
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
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
        tvTitle.text = if (isEditMode) "Editar $tableName" else "Nuevo $tableName"
    }

    private fun setupFields() {
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

        // Refrescar configuración de documento si existen ambos campos
        if (docTypeColumnIndex != -1 && numDocColumnIndex != -1) {
            refreshNumDocHintAndValidation()
        }
    }

    private fun createDropdownField(idx: Int, column: String, colIndex: Int) {
        val fkRef = fkRefs[column] ?: return
        
        var parentId: String? = null
        if (column == "ID_MUNICIPIO" && fkRefs.containsKey("ID_DEPARTAMENTO")) {
            val parentAutoComplete = dropDownFields.values.find { it.first == "ID_DEPARTAMENTO" }?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
        }
        if (column == "ID_DISTRITO" && fkRefs.containsKey("ID_MUNICIPIO")) {
            val parentAutoComplete = dropDownFields.values.find { it.first == "ID_MUNICIPIO" }?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
        }
        if (column == "ID_HABILIDAD" && fkRefs.containsKey("ID_CATEGORIA_HABILIDAD")) {
            val parentAutoComplete = dropDownFields.values.find { it.first == "ID_CATEGORIA_HABILIDAD" }?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
        }

        val options = if (parentId != null && parentId.isNotBlank()) {
            val parentFkColumnName = when (column) {
                "ID_MUNICIPIO" -> "ID_DEPARTAMENTO"
                "ID_DISTRITO" -> "ID_MUNICIPIO"
                "ID_HABILIDAD" -> "ID_CATEGORIA_HABILIDAD"
                else -> ""
            }
            val parentFk = fkRefs[parentFkColumnName]!!
            viewModel.getFilteredOptions(fkRef.refTable, parentFk.fkColumn, parentId, fkRef.refDisplayColumn)
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
                if (column == "ID_DEPARTAMENTO") refreshDependentDropdown("ID_MUNICIPIO")
                if (column == "ID_MUNICIPIO") refreshDependentDropdown("ID_DISTRITO")
                if (column == "ID_CATEGORIA_HABILIDAD") refreshDependentDropdown("ID_HABILIDAD")
            }
        }

        // Escuchar cambios de texto para actualizar la máscara del documento al instante
        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (column == "ID_TIPO_DOCUMENTO") {
                    refreshNumDocHintAndValidation()
                }
            }
        })

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
        if (docTypeColumnIndex == -1) return null
        val autoComplete = dropDownFields[docTypeColumnIndex]?.second ?: return null
        val text = autoComplete.text?.toString()?.trim()?.lowercase() ?: return null
        if (text.isBlank()) return null
        return when {
            text.contains("dui") -> "DUI"
            text.contains("nit") -> "NIT"
            text.contains("pasaporte") -> "PASAPORTE"
            else -> text.uppercase()
        }
    }

    private fun refreshNumDocHintAndValidation() {
        if (numDocColumnIndex == -1) return
        val pair = textFields[numDocColumnIndex] ?: return
        val et = pair.second
        
        // Búsqueda robusta del TextInputLayout subiendo en la jerarquía
        var current = et.parent
        var til: TextInputLayout? = null
        while (current != null) {
            if (current is TextInputLayout) {
                til = current
                break
            }
            current = current.parent
        }
        if (til == null) return

        val tipo = getSelectedDocType()
        
        // BLOQUEO: Deshabilitar el campo si no se ha seleccionado tipo de documento
        et.isEnabled = tipo != null
        
        // 1. Actualizar Hint dinámico
        til.hint = when (tipo) {
            "DUI" -> "DUI (ej: 12345678-9)"
            "NIT" -> "NIT (ej: 0614-111222-333-4)"
            "PASAPORTE" -> "Pasaporte (Letras y Números)"
            null -> "Seleccione Tipo de Documento primero"
            else -> "Número de Documento"
        }

        if (tipo == null) {
            et.setText("")
            til.error = null
            return
        }

        // 2. Cambiar teclado dinámicamente
        val targetInputType = when (tipo) {
            "DUI", "NIT" -> android.text.InputType.TYPE_CLASS_PHONE
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }
        if (et.inputType != targetInputType) {
            et.inputType = targetInputType
        }

        // 3. Aplicar Filtros estrictos de caracteres y longitud
        val filters = mutableListOf<InputFilter>()
        val maxLength = when (tipo) {
            "DUI" -> 10 // 8 digitos + guion + 1 digito
            "NIT" -> 17 // Formato completo con guiones
            else -> 17
        }
        filters.add(InputFilter.LengthFilter(maxLength))
        
        if (tipo == "DUI" || tipo == "NIT") {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    if (!source[i].isDigit() && source[i] != '-') return@InputFilter ""
                }
                null
            })
        }
        et.filters = filters.toTypedArray()

        // 4. Re-formatear si ya hay texto
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
        val childInfo = dropDownFields.values.find { it.first == childColumn } ?: return
        val childAutoComplete = childInfo.second
        val childTil = childAutoComplete.parent as? TextInputLayout ?: return
        
        val parentFkColumn = when (childColumn) {
            "ID_MUNICIPIO" -> "ID_DEPARTAMENTO"
            "ID_DISTRITO" -> "ID_MUNICIPIO"
            "ID_HABILIDAD" -> "ID_CATEGORIA_HABILIDAD"
            else -> return
        }
        val parentFk = fkRefs[parentFkColumn] ?: return
        val parentAutoComplete = dropDownFields.values.find { it.first == parentFkColumn }?.second
        val parentId = getSelectedDropdownValue(parentAutoComplete) ?: return

        val newOptions = viewModel.getFilteredOptions(
            fkRefs[childColumn]!!.refTable,
            parentFk.fkColumn,
            parentId,
            fkRefs[childColumn]!!.refDisplayColumn
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
        }

        val et = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            
            val isMultiline = column.uppercase().contains("DESCRIPCION") || 
                              column.uppercase().contains("DETALLE") || 
                              column.uppercase().contains("DESC") ||
                              column.uppercase().contains("REQUISITO")

            inputType = getInputType(column)

            // Forzamos gravedad para que todos los campos alineen igual su primera línea
            gravity = Gravity.START or Gravity.CENTER_VERTICAL

            if (isMultiline) {
                setSingleLine(false)
                setHorizontallyScrolling(false)
                minLines = 1
                maxLines = 5
            } else {
                setSingleLine(true)
            }
            
            // Forzamos un padding uniforme para que todos los campos tengan la misma altura base
            val density = resources.displayMetrics.density
            val verticalPadding = (12 * density).toInt()
            setPadding(0, verticalPadding, 0, verticalPadding)
            
            includeFontPadding = false
            filters = getFilters(column)
        }

        // PASSWORD bloqueado en edición de USUARIO
        if (column == "PASSWORD" && tableName == "USUARIO" && isEditMode) {
            et.isEnabled = false
            et.isFocusable = false
            til.hint = "Contraseña (bloqueada)"
        }

        if (column.contains("FECHA")) {
            et.isFocusable = false
            et.isClickable = true
            et.setOnClickListener { showDatePicker(et) }
        }

        if (isEditMode && colIndex < itemData.size) {
            et.setText(itemData[colIndex].trim())
        }

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
            try { dateFormat.parse(currentText)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
        } else { System.currentTimeMillis() }
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Seleccionar fecha").setSelection(initialMillis).build()
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
                for (i in start until end) { if (!source[i].isDigit()) return@InputFilter "" }
                null
            })
        }
        return filters.toTypedArray()
    }

    private fun getInputType(column: String): Int {
        val col = column.uppercase()
        return when {
            col.startsWith("ID_") || col.contains("NUP") -> android.text.InputType.TYPE_CLASS_NUMBER
            col.contains("NUM_") || col.contains("DOCUMENTO") -> android.text.InputType.TYPE_CLASS_TEXT
            col.contains("FECHA") || col.contains("DATE") -> android.text.InputType.TYPE_CLASS_TEXT
            col.contains("EMAIL") -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            col.contains("TELEFONO") || col.contains("TEL") || col == "CONTACTO_REFERENCIA" -> android.text.InputType.TYPE_CLASS_PHONE
            col.contains("PASSWORD") || col.contains("CONTRA") -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            col.contains("EXPERIENCIA_ANIOS") || col.contains("EDAD_MIN") || col.contains("EDAD_MAX") || col.contains("NIVEL_DESTREZA") -> android.text.InputType.TYPE_CLASS_NUMBER
            col.contains("DESCRIPCION") || col.contains("DETALLE") || col.contains("DESC") || col.contains("REQUISITO") -> 
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
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
        val col = column.uppercase()
        return when {
            col.contains("NOMBRE") -> "Ingrese el nombre"
            col.contains("APELLIDO") -> "Ingrese el apellido"
            col.contains("EMAIL") || col.contains("CORREO") -> "ejemplo@correo.com"
            col.contains("TELEFONO") || col.contains("TEL") -> "Ingrese teléfono"
            col.contains("NUP") -> "Ingrese NUP"
            col.contains("NUM_DOCUMENTO") -> "Documento de identidad"
            col.contains("TITULO") -> "Ingrese el título"
            col.contains("DESCRIPCION") || col.contains("DETALLE") || col.contains("DESC") || col.contains("REQUISITO") -> "Ingrese la descripción"
            col.contains("EXPERIENCIA_ANIOS") -> "Años de experiencia"
            col.contains("EDAD_MINIMA") -> "Edad mínima"
            col.contains("EDAD_MAXIMA") -> "Edad máxima"
            col.contains("FECHA") -> "AAAA-MM-DD"
            else -> "Ingrese $column"
        }
    }

    private fun getFieldValidationError(column: String, value: String): String? {
        if (value.isBlank()) return null
        return when {
            column.contains("EMAIL") -> InputMaskUtils.validateEmail(value)
            column.contains("PASSWORD") || column.contains("CONTRA") -> InputMaskUtils.validatePassword(value)
            column.contains("FECHA") -> InputMaskUtils.validateFecha(value)
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
            "HABILIDAD_POSTULANTE" -> listOf("ID_POSTULANTE", "ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD", "ID_HABILIDAD_POSTULANTE", "NIVEL_DESTREZA")
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
        for (col in editableColumns) {
            val fkRef = fkRefs[col]
            if (col == "NIVEL_DESTREZA") {
                val autoComplete = dropDownFields.values.find { it.first == col }?.second
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
                val autoComplete = dropDownFields.values.find { it.first == col }?.second
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
                val autoComplete = dropDownFields.values.find { it.first == col }?.second
                val selectedText = autoComplete?.text?.toString()?.trim() ?: "postulante"
                values.add(selectedText)
            } else if (fkRef != null) {
                val options = if (col == "ID_HABILIDAD" && tableName == "HABILIDAD_POSTULANTE") {
                    val categoryAutoComplete = dropDownFields.values.find { it.first == "ID_CATEGORIA_HABILIDAD" }?.second
                    val categoryId = getSelectedDropdownValue(categoryAutoComplete)
                    if (categoryId != null) {
                        viewModel.getFilteredOptions(fkRef.refTable, "ID_CATEGORIA_HABILIDAD", categoryId, fkRef.refDisplayColumn)
                    } else {
                        viewModel.getDropdownOptions(fkRef.refTable, fkRef.refDisplayColumn)
                    }
                } else {
                    viewModel.getDropdownOptions(fkRef.refTable, fkRef.refDisplayColumn)
                }

                if (options.isEmpty()) {
                    StyledToast.show(requireContext(), "No hay datos en ${fkRef.refTable}. Créelos primero.")
                    return
                }
                hasRequiredFk = true

                val autoComplete = dropDownFields.values.find { it.first == col }?.second
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
                val et = textFields.values.find { it.first == col }?.second
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
        if (isEditMode) viewModel.updateRecord(tableName, itemData.first(), values) else viewModel.insertRecord(tableName, values)
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
