package sv.ues.fia.eisi.bt.ui.crud

import android.content.res.TypedArray
import android.graphics.Color
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
            hint = getHintText(column)
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
            hint = getHintText(column)
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
            hint = getHintText(column)
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
            hint = getHintText(column)
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
            "DUI" -> "DUI"
            "NIT" -> "NIT"
            "PASAPORTE" -> "Pasaporte"
            else -> getHintText("NUM_DOCUMENTO")
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
        childTil.hint = getHintText(childColumn)
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

            if (isMultiline) {
                setSingleLine(false)
                setHorizontallyScrolling(false)
                gravity = Gravity.TOP or Gravity.START
                minLines = 1
                maxLines = 5
            } else {
                setSingleLine(true)
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            
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
        return when (col) {
            "ID_GENERO" -> "Género"
            "ID_TIPO_DOCUMENTO" -> "Tipo de documento"
            "NUM_DOCUMENTO" -> "Número de documento"
            "ID_DISTRITO" -> "Distrito"
            "NOMBRE" -> "Nombre"
            "APELLIDO" -> "Apellido"
            "FECHA_NACIMIENTO" -> "Fecha de nacimiento"
            "NUP" -> "NUP"
            "DIRECCION_DETALLE" -> "Dirección"
            "TELEFONO_CASA" -> "Teléfono casa"
            "TELEFONO_CELULAR" -> "Teléfono celular"
            "EMAIL", "CORREO" -> "Correo electrónico"
            "ID_DEPARTAMENTO" -> "Departamento"
            "ID_MUNICIPIO" -> "Municipio"
            "NOMBRE_CATEGORIA" -> "Nombre de categoría"
            "NOMBRE_GENERO" -> "Nombre de género"
            "NOMBRE_TIPO" -> "Nombre de tipo"
            "NOMBRE_DEPARTAMENTO" -> "Nombre de departamento"
            "NOMBRE_MUNICIPIO" -> "Nombre de municipio"
            "NOMBRE_DISTRITO" -> "Nombre de distrito"
            "NOMBRE_HABILIDAD" -> "Nombre de habilidad"
            "ID_CATEGORIA_HABILIDAD" -> "Categoría de habilidad"
            "NOMBRE_EMPRESA" -> "Nombre de empresa"
            "CONTACTO_DIRECTO" -> "Contacto directo"
            "NIT" -> "NIT"
            "NOMBRE_INSTITUCION" -> "Nombre de institución"
            "NOMBRE_GRADO" -> "Nombre de grado"
            "NOMBRE_RED" -> "Nombre de red social"
            "ID_INSTITUCION" -> "Institución"
            "ID_GRADO_ACADEMICO" -> "Grado académico"
            "TITULO_PUESTO" -> "Título del puesto"
            "FECHA_PUBLICACION" -> "Fecha de publicación"
            "FECHA_CADUCIDAD" -> "Fecha de caducidad"
            "EXPERIENCIA_ANIOS" -> "Años de experiencia"
            "EDAD_MINIMA" -> "Edad mínima"
            "EDAD_MAXIMA" -> "Edad máxima"
            "DESCRIPCION_OFERTA_TRABAJO", "DESCRIPCION", "DESC" -> "Descripción"
            "DESCRIPCION_REQUISITO" -> "Descripción del requisito"
            "ID_EMPRESA" -> "Empresa"
            "ID_OFERTA" -> "Oferta de trabajo"
            "ID_POSTULANTE" -> "Postulante"
            "ID_CERTIFICACION" -> "Certificación"
            "NOMBRE_CERTIFICACION" -> "Nombre de certificación"
            "CODIGO_CERTIFICACION" -> "Código de certificación"
            "FECHA_CERTIFICACION" -> "Fecha de certificación"
            "ID_EXPERIENCIA" -> "Experiencia"
            "PUESTO_TRABAJO" -> "Puesto de trabajo"
            "FECHA_INICIO" -> "Fecha de inicio"
            "FECHA_FIN" -> "Fecha de fin"
            "DESCP_EXPERIENCIA_LABORAL" -> "Descripción de experiencia"
            "CONTACTO_REFERENCIA" -> "Contacto de referencia"
            "ID_FORMACION" -> "Formación"
            "ID_OFERTA_ACADEMICA" -> "Oferta académica"
            "TITULO_OBTENIDO" -> "Título obtenido"
            "FECHA_OBTENCION" -> "Fecha de obtención"
            "ID_HABILIDAD" -> "Habilidad"
            "ID_HABILIDAD_POSTULANTE" -> "Habilidad postulante"
            "NIVEL_DESTREZA" -> "Nivel de destreza"
            "ID_POSTULACION" -> "Postulación"
            "FECHA_APLICACION" -> "Fecha de aplicación"
            "ESTADO_PROCESO" -> "Estado del proceso"
            "ID_DETALLE" -> "Detalle"
            "ID_RED_POSTUALNTE" -> "Red social postulante"
            "ID_RED_SOCIAL" -> "Red social"
            "URL_PERFIL" -> "URL del perfil"
            "USERNAME", "USER" -> "Nombre de usuario"
            "PASSWORD", "CONTRA" -> "Contraseña"
            "ROL" -> "Rol"
            else -> column.replace("ID_", "").replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
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
        for (col in editableColumns) {
            val fkRef = fkRefs[col]
            if (col == "NIVEL_DESTREZA") {
                val selected = dropDownFields[columns.indexOf(col)]?.second?.text?.toString() ?: ""
                values.add(when(selected) { "Básico" -> "1"; "Intermedio" -> "2"; "Avanzado" -> "3"; else -> "" })
            } else if (col == "ESTADO_PROCESO") {
                values.add(dropDownFields[columns.indexOf(col)]?.second?.text?.toString()?.lowercase() ?: "")
            } else if (col == "ROL") {
                values.add(dropDownFields[columns.indexOf(col)]?.second?.text?.toString() ?: "postulante")
            } else if (fkRef != null) {
                val selected = dropDownFields[columns.indexOf(col)]?.second?.text?.toString() ?: ""
                val options = viewModel.getDropdownOptions(fkRef.refTable, fkRef.refDisplayColumn)
                values.add(options.find { it.second == selected }?.first ?: "")
            } else {
                values.add(textFields[columns.indexOf(col)]?.second?.text?.toString()?.trim() ?: "")
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
