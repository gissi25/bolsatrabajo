package sv.ues.fia.eisi.bt.ui.crud

import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
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
import java.util.TimeZone

class EditorDialogFragment : DialogFragment() {

    private val viewModel: CrudViewModel by viewModels({ requireParentFragment() })
    private var tableName: String = ""
    private var isEditMode: Boolean = false
    private var isViewMode: Boolean = false
    private var itemData: List<String> = emptyList()
    private var userRole: String = ""

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
    private var savedUsername: String = ""
    private var savedNewRole: String = ""

    private var distritoDepartamentoAutoComplete: MaterialAutoCompleteTextView? = null
    private var distritoMunicipioAutoComplete: MaterialAutoCompleteTextView? = null

    private var empresaDepartamentoAutoComplete: MaterialAutoCompleteTextView? = null
    private var empresaMunicipioAutoComplete: MaterialAutoCompleteTextView? = null

    private var postulanteDepartamentoAutoComplete: MaterialAutoCompleteTextView? = null
    private var postulanteMunicipioAutoComplete: MaterialAutoCompleteTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AestheticDialog)
        arguments?.let {
        tableName = it.getString(Constants.BUNDLE_TABLE_NAME, "")
        isEditMode = it.getBoolean(Constants.BUNDLE_IS_EDIT_MODE, false)
        isViewMode = it.getBoolean(Constants.BUNDLE_IS_VIEW_MODE, false)
        val dataString = it.getString(Constants.BUNDLE_TABLE_DATA, "")
            itemData = if (dataString.isNotBlank()) dataString.split(",") else emptyList()
        }
        columns = getColumnsForTable(tableName)
        fkRefs = viewModel.getFkReferences(tableName)
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
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

        userRole = requireContext().getSharedPreferences(
            Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE
        ).getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE) ?: Constants.ROLE_POSTULANTE

        setupTitle()
        setupFields()

        if (isEditMode && tableName == "POSTULACION") {
            when (userRole) {
                Constants.ROLE_POSTULANTE -> {
                    disableAllFields()
                    StyledToast.show(requireContext(), "Solo la empresa puede modificar esta postulación")
                }
                Constants.ROLE_EMPRESA -> {
                    disableNonEstadoFields()
                }
            }
        }

        if (isViewMode) setupViewMode()
        setupButtons()

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            btnSave.isEnabled = true
            btnSave.text = if (isEditMode) "Actualizar" else "Guardar"
            when (result) {
                is Resource.Success -> {
                    if (tableName == "USUARIO" && isEditMode) {
                        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        val currentUserId = prefs.getInt(Constants.KEY_USER_ID, -1)
                        val editedUserId = itemData.firstOrNull()?.trim()?.toIntOrNull() ?: -1
                        if (editedUserId == currentUserId) {
                            prefs.edit()
                                .putString(Constants.KEY_USERNAME, savedUsername)
                                .putString(Constants.KEY_USER_ROLE, savedNewRole)
                                .apply()
                        }
                    }
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
        tvTitle.text = when {
            isViewMode -> "Ver $tableName"
            isEditMode -> "Editar $tableName"
            else -> "Nuevo $tableName"
        }
    }

    private fun setupFields() {

        val autoGenCol = getAutoGenColumn(tableName)
        val columnsToIterate = columns.filter { it != autoGenCol }

        columnsToIterate.forEachIndexed { idx, column ->
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

        if (docTypeColumnIndex != -1 && numDocColumnIndex != -1) {
            refreshNumDocHintAndValidation()
        }
    }

    private fun createDropdownField(idx: Int, column: String, colIndex: Int) {
        val fkRef = fkRefs[column] ?: return

        var parentId: String? = null
        var filterColumn: String? = null

        if (column == "ID_MUNICIPIO" && tableName == "DISTRITO") {
            val deptId = getSelectedDistritoDepartamentoValue()
            if (deptId != null) {
                parentId = deptId
                filterColumn = "ID_DEPARTAMENTO"
            }
        }

        if (column == "ID_DISTRITO" && tableName == "EMPRESA") {
            val municipioId = getSelectedEmpresaMunicipioValue()
            if (municipioId != null) {
                parentId = municipioId
                filterColumn = "ID_MUNICIPIO"
            }
        }

        if (column == "ID_DISTRITO" && tableName == "POSTULANTE") {
            val municipioId = getSelectedPostulanteMunicipioValue()
            if (municipioId != null) {
                parentId = municipioId
                filterColumn = "ID_MUNICIPIO"
            }
        }

        if (column == "ID_MUNICIPIO" && fkRefs.containsKey("ID_DEPARTAMENTO")) {
            val parentAutoComplete = dropDownFields.values.find { it.first == "ID_DEPARTAMENTO" }?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
            filterColumn = "ID_DEPARTAMENTO"
        }
        if (column == "ID_DISTRITO" && fkRefs.containsKey("ID_MUNICIPIO")) {
            val parentAutoComplete = dropDownFields.values.find { it.first == "ID_MUNICIPIO" }?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
            filterColumn = "ID_MUNICIPIO"
        }
        if (column == "ID_HABILIDAD" && fkRefs.containsKey("ID_CATEGORIA_HABILIDAD")) {
            val parentAutoComplete = dropDownFields.values.find { it.first == "ID_CATEGORIA_HABILIDAD" }?.second
            parentId = getSelectedDropdownValue(parentAutoComplete)
            filterColumn = "ID_CATEGORIA_HABILIDAD"
        }
        if (column == "ID_OFERTA" && tableName == "POSTULACION") {
            val empresaAutoComplete = dropDownFields.values.find { it.first == "NIT" }?.second
            parentId = getSelectedDropdownValue(empresaAutoComplete)
            filterColumn = "NIT"
        }
        if (column == "ID_OFERTA" && tableName == "DETALLE_REQUISITO") {
            val empresaAutoComplete = dropDownFields.values.find { it.first == "NIT" }?.second
            parentId = getSelectedDropdownValue(empresaAutoComplete)
            filterColumn = "NIT"
        }

        val includeExpired = fkRef.refTable == "OFERTA_TRABAJO" && tableName == "DETALLE_REQUISITO"
        val options = if (parentId != null && parentId.isNotBlank() && filterColumn != null) {
            viewModel.getFilteredOptions(fkRef.refTable, filterColumn, parentId, fkRef.refDisplayColumn, includeExpired)
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

        if ((isEditMode || isViewMode) && colIndex < itemData.size) {
            val currentId = itemData[colIndex].trim()
            val optionIndex = options.indexOfFirst { it.first == currentId }
            if (optionIndex >= 0) {
                autoComplete.setText(displayOptions[optionIndex], false)
            }
        }

        autoComplete.setOnItemClickListener { _, _, _, _ ->
            if (column == "ID_DEPARTAMENTO") refreshDependentDropdown("ID_MUNICIPIO")
            if (column == "ID_CATEGORIA_HABILIDAD") refreshDependentDropdown("ID_HABILIDAD")
            if (column == "NIT" && (tableName == "POSTULACION" || tableName == "DETALLE_REQUISITO")) refreshDependentDropdown("ID_OFERTA")
            if (column == "ID_DISTRITO_DEPTO") refreshDependentDropdown("ID_DISTRITO_MUNICIPIO")
            if (column == "ID_DISTRITO_MUNICIPIO") refreshDependentDropdown("ID_DISTRITO_ID")
        }

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

        if (tableName == "DISTRITO" && column == "ID_MUNICIPIO") {
            distritoMunicipioAutoComplete = autoComplete
        }
    }

    private fun getSelectedDistritoDepartamentoValue(): String? {
        val autoComplete = distritoDepartamentoAutoComplete ?: return null
        val text = autoComplete.text?.toString()?.trim() ?: return null
        if (text.isBlank()) return null
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        return departamentoOptions.find { it.second == text }?.first
    }

    private fun createNivelDestrezaDropdown(idx: Int, column: String, colIndex: Int) {
        val nivelOptions = listOf(
            "Básico" to "Básico",
            "Intermedio" to "Intermedio",
            "Avanzado" to "Avanzado"
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

        if ((isEditMode || isViewMode) && colIndex < itemData.size) {
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
            val id = nivelOptions.indexOfFirst { pair -> pair.first == it }
            if (id >= 0) autoComplete.setText(displayOptions[id], false)
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

        val isPostulanteBloqueado = userRole == Constants.ROLE_POSTULANTE && tableName == "POSTULACION"

        if (isPostulanteBloqueado) {
            autoComplete.isEnabled = false
            autoComplete.isFocusable = false
            autoComplete.isClickable = false
            if ((isEditMode || isViewMode) && colIndex < itemData.size) {
                val currentValue = itemData[colIndex].trim()
                val optionIndex = estadoOptions.indexOfFirst { it.first == currentValue }
                if (optionIndex >= 0) {
                    autoComplete.setText(displayOptions[optionIndex], false)
                } else {
                    autoComplete.setText("Activo", false)
                }
            } else {
                autoComplete.setText("Activo", false)
            }
            til.helperText = "Solo la empresa puede cambiar este estado"
        } else {
            autoComplete.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    autoComplete.showDropDown()
                }
                true
            }

            if ((isEditMode || isViewMode) && colIndex < itemData.size) {
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
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        dropDownFields[colIndex] = Pair(column, autoComplete)
    }

    private fun createRolDropdown(idx: Int, column: String, colIndex: Int) {
        val roles = arrayOf("postulante", "gerente de empresa", "administrador")

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

        if ((isEditMode || isViewMode) && colIndex < itemData.size) {
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

    private fun createDistritoDepartamentoField() {
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Departamento"
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = departamentoOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if ((isEditMode || isViewMode) && itemData.size >= 2) {
            val currentMunicipioId = itemData[1].trim()
            val departamentoId = viewModel.getDepartamentoByMunicipio(currentMunicipioId)
            if (departamentoId != null) {
                val deptOptionIndex = departamentoOptions.indexOfFirst { it.first == departamentoId }
                if (deptOptionIndex >= 0) {
                    autoComplete.setText(displayOptions[deptOptionIndex], false)
                }
                post {
                    prefillDistritoMunicipio(currentMunicipioId)
                }
            }
        }

        autoComplete.setOnItemClickListener { _, _, _, _ ->
            refreshDistritoMunicipioDropdown()
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        distritoDepartamentoAutoComplete = autoComplete
    }

    private fun refreshDistritoMunicipioDropdown() {
        val departamentoAutoComplete = distritoDepartamentoAutoComplete ?: return
        val municipioAutoComplete = distritoMunicipioAutoComplete ?: return

        val deptText = departamentoAutoComplete.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val municipioOptions = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO", departamentoId, "NOMBRE_MUNICIPIO")
        val displayOptions = municipioOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        municipioAutoComplete.setAdapter(adapter)
        municipioAutoComplete.setText("", false)
    }

    private fun prefillDistritoMunicipio(municipioId: String) {
        val municipioAutoComplete = distritoMunicipioAutoComplete ?: return
        val deptText = distritoDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val municipioOptions = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO", departamentoId, "NOMBRE_MUNICIPIO")
        val displayOptions = municipioOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        municipioAutoComplete.setAdapter(adapter)

        val optionIndex = municipioOptions.indexOfFirst { it.first == municipioId }
        if (optionIndex >= 0) {
            municipioAutoComplete.setText(displayOptions[optionIndex], false)
        }
    }

    private fun post(action: () -> Unit) {
        tilFieldsContainer.post(action)
    }

    private fun createEmpresaDepartamentoField() {
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Departamento"
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = departamentoOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if ((isEditMode || isViewMode) && itemData.size >= 2) {
            val currentDistritoId = itemData[1].trim()
            val municipioId = viewModel.getMunicipioByDistrito(currentDistritoId)
            if (municipioId != null) {
                val departamentoId = viewModel.getDepartamentoByMunicipio(municipioId)
                if (departamentoId != null) {
                    val deptOptionIndex = departamentoOptions.indexOfFirst { it.first == departamentoId }
                    if (deptOptionIndex >= 0) {
                        autoComplete.setText(displayOptions[deptOptionIndex], false)
                    }
                    post {
                        prefillEmpresaMunicipio(municipioId)
                        post {
                            prefillEmpresaDistrito(currentDistritoId)
                        }
                    }
                }
            }
        }

        autoComplete.setOnItemClickListener { _, _, _, _ ->
            refreshEmpresaMunicipioDropdown()
            empresaMunicipioAutoComplete?.setText("", false)
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        empresaDepartamentoAutoComplete = autoComplete
    }

    private fun createEmpresaMunicipioField() {
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Municipio"
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = municipioOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        autoComplete.setOnItemClickListener { _, _, _, _ ->
            refreshEmpresaDistritoDropdown()
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        empresaMunicipioAutoComplete = autoComplete
    }

    private fun refreshEmpresaMunicipioDropdown() {
        val departamentoAutoComplete = empresaDepartamentoAutoComplete ?: return
        val municipioAutoComplete = empresaMunicipioAutoComplete ?: return

        val deptText = departamentoAutoComplete.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val municipioOptions = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO", departamentoId, "NOMBRE_MUNICIPIO")
        val displayOptions = municipioOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        municipioAutoComplete.setAdapter(adapter)
        municipioAutoComplete.setText("", false)
    }

    private fun refreshEmpresaDistritoDropdown() {
        val municipioAutoComplete = empresaMunicipioAutoComplete ?: return

        val munText = municipioAutoComplete.text?.toString()?.trim() ?: return
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        val municipioId = municipioOptions.find { it.second == munText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", municipioId, "NOMBRE_DISTRITO")
        val displayOptions = distritoOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO" }?.second ?: return
        distritoAutoComplete.setAdapter(adapter)
        distritoAutoComplete.setText("", false)
    }

    private fun prefillEmpresaMunicipio(municipioId: String) {
        val municipioAutoComplete = empresaMunicipioAutoComplete ?: return
        val deptText = empresaDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val municipioOptions = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO", departamentoId, "NOMBRE_MUNICIPIO")
        val displayOptions = municipioOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        municipioAutoComplete.setAdapter(adapter)

        val optionIndex = municipioOptions.indexOfFirst { it.first == municipioId }
        if (optionIndex >= 0) {
            municipioAutoComplete.setText(displayOptions[optionIndex], false)
        }
    }

    private fun prefillEmpresaDistrito(distritoId: String) {
        val municipioAutoComplete = empresaMunicipioAutoComplete ?: return
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO" }?.second ?: return

        val munText = municipioAutoComplete.text?.toString()?.trim() ?: return
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        val municipioId = municipioOptions.find { it.second == munText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", municipioId, "NOMBRE_DISTRITO")
        val displayOptions = distritoOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        distritoAutoComplete.setAdapter(adapter)

        val optionIndex = distritoOptions.indexOfFirst { it.first == distritoId }
        if (optionIndex >= 0) {
            distritoAutoComplete.setText(displayOptions[optionIndex], false)
        }
    }

    private fun getSelectedEmpresaMunicipioValue(): String? {
        val autoComplete = empresaMunicipioAutoComplete ?: return null
        val text = autoComplete.text?.toString()?.trim() ?: return null
        if (text.isBlank()) return null
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        return municipioOptions.find { it.second == text }?.first
    }

    private fun createPostulanteDepartamentoField() {
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Departamento"
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = departamentoOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if ((isEditMode || isViewMode) && itemData.size >= 5) {
            val currentDistritoId = itemData[4].trim()
            val municipioId = viewModel.getMunicipioByDistrito(currentDistritoId)
            if (municipioId != null) {
                val departamentoId = viewModel.getDepartamentoByMunicipio(municipioId)
                if (departamentoId != null) {
                    val deptOptionIndex = departamentoOptions.indexOfFirst { it.first == departamentoId }
                    if (deptOptionIndex >= 0) {
                        autoComplete.setText(displayOptions[deptOptionIndex], false)
                    }
                    post {
                        prefillPostulanteMunicipio(municipioId)
                        post {
                            prefillPostulanteDistrito(currentDistritoId)
                        }
                    }
                }
            }
        }

        autoComplete.setOnItemClickListener { _, _, _, _ ->
            refreshPostulanteMunicipioDropdown()
            postulanteMunicipioAutoComplete?.setText("", false)
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        postulanteDepartamentoAutoComplete = autoComplete
    }

    private fun createPostulanteMunicipioField() {
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = "Municipio"
        }

        val autoComplete = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
            keyListener = null
        }

        val displayOptions = municipioOptions.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        autoComplete.setOnItemClickListener { _, _, _, _ ->
            refreshPostulanteDistritoDropdown()
        }

        til.addView(autoComplete)
        tilFieldsContainer.addView(til)
        postulanteMunicipioAutoComplete = autoComplete
    }

    private fun refreshPostulanteMunicipioDropdown() {
        val departamentoAutoComplete = postulanteDepartamentoAutoComplete ?: return
        val municipioAutoComplete = postulanteMunicipioAutoComplete ?: return

        val deptText = departamentoAutoComplete.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val municipioOptions = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO", departamentoId, "NOMBRE_MUNICIPIO")
        val displayOptions = municipioOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        municipioAutoComplete.setAdapter(adapter)
        municipioAutoComplete.setText("", false)
    }

    private fun refreshPostulanteDistritoDropdown() {
        val municipioAutoComplete = postulanteMunicipioAutoComplete ?: return

        val munText = municipioAutoComplete.text?.toString()?.trim() ?: return
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        val municipioId = municipioOptions.find { it.second == munText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", municipioId, "NOMBRE_DISTRITO")
        val displayOptions = distritoOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO" }?.second ?: return
        distritoAutoComplete.setAdapter(adapter)
        distritoAutoComplete.setText("", false)
    }

    private fun prefillPostulanteMunicipio(municipioId: String) {
        val municipioAutoComplete = postulanteMunicipioAutoComplete ?: return
        val deptText = postulanteDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val municipioOptions = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO", departamentoId, "NOMBRE_MUNICIPIO")
        val displayOptions = municipioOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        municipioAutoComplete.setAdapter(adapter)

        val optionIndex = municipioOptions.indexOfFirst { it.first == municipioId }
        if (optionIndex >= 0) {
            municipioAutoComplete.setText(displayOptions[optionIndex], false)
        }
    }

    private fun prefillPostulanteDistrito(distritoId: String) {
        val municipioAutoComplete = postulanteMunicipioAutoComplete ?: return
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO" }?.second ?: return

        val munText = municipioAutoComplete.text?.toString()?.trim() ?: return
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        val municipioId = municipioOptions.find { it.second == munText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", municipioId, "NOMBRE_DISTRITO")
        val displayOptions = distritoOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        distritoAutoComplete.setAdapter(adapter)

        val optionIndex = distritoOptions.indexOfFirst { it.first == distritoId }
        if (optionIndex >= 0) {
            distritoAutoComplete.setText(displayOptions[optionIndex], false)
        }
    }

    private fun getSelectedPostulanteMunicipioValue(): String? {
        val autoComplete = postulanteMunicipioAutoComplete ?: return null
        val text = autoComplete.text?.toString()?.trim() ?: return null
        if (text.isBlank()) return null
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        return municipioOptions.find { it.second == text }?.first
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
            "DUI" -> "DUI"
            "NIT" -> "NIT"
            "PASAPORTE" -> "Pasaporte"
            else -> getHintText("NUM_DOCUMENTO")
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
        val childFkRef = fkRefs[childColumn] ?: return

        // Búsqueda robusta del TextInputLayout subiendo en la jerarquía
        var current = childAutoComplete.parent
        var childTil: TextInputLayout? = null
        while (current != null) {
            if (current is TextInputLayout) {
                childTil = current
                break
            }
            current = current.parent
        }
        if (childTil == null) return

        val parentFkColumn = when (childColumn) {
            "ID_MUNICIPIO" -> "ID_DEPARTAMENTO"
            "ID_HABILIDAD" -> "ID_CATEGORIA_HABILIDAD"
            "ID_OFERTA" -> "NIT"
            "ID_DISTRITO_MUNICIPIO" -> "ID_DISTRITO_DEPTO"
            "ID_DISTRITO_ID" -> "ID_DISTRITO_MUNICIPIO"
            else -> return
        }

        val actualFilterColumn = when (childColumn) {
            "ID_DISTRITO_MUNICIPIO" -> "ID_DEPARTAMENTO"
            "ID_DISTRITO_ID" -> "ID_MUNICIPIO"
            else -> parentFkColumn
        }

        val parentAutoComplete = dropDownFields.values.find { it.first == parentFkColumn }?.second
        var parentId = getSelectedDropdownValue(parentAutoComplete) ?: return

        if (childColumn == "ID_DISTRITO_ID") {
            val deptoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO_DEPTO" }?.second
            val deptoId = getSelectedDropdownValue(deptoAutoComplete)
            if (deptoId != null) {
                parentId = "$deptoId|$parentId"
            }
        }

        val includeExpired = childFkRef.refTable == "OFERTA_TRABAJO" && tableName == "DETALLE_REQUISITO"
        val newOptions = viewModel.getFilteredOptions(
            childFkRef.refTable,
            actualFilterColumn,
            parentId,
            childFkRef.refDisplayColumn,
            includeExpired
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

        if ((isEditMode || isViewMode) && colIndex < itemData.size) {
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
        val utc = TimeZone.getTimeZone("UTC")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = utc }
        val currentText = editText.text?.toString() ?: ""
        val initialMillis = if (currentText.isNotEmpty()) {
            try { dateFormat.parse(currentText)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
        } else { System.currentTimeMillis() }

        var til: TextInputLayout? = null
        var parent = editText.parent
        while (parent != null) {
            if (parent is TextInputLayout) { til = parent; break }
            parent = parent.parent
        }
        val titulo = til?.hint?.toString() ?: "Seleccionar fecha"

        val calendar = Calendar.getInstance(utc)
        val currentYear = calendar.get(Calendar.YEAR)
        val startMillis = Calendar.getInstance(utc).apply { set(1926, Calendar.JANUARY, 1) }.timeInMillis
        val endMillis = Calendar.getInstance(utc).apply { set(currentYear, Calendar.DECEMBER, 31) }.timeInMillis

        val constraints = com.google.android.material.datepicker.CalendarConstraints.Builder()
            .setStart(startMillis)
            .setEnd(endMillis)
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(titulo)
            .setSelection(initialMillis)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val cal = Calendar.getInstance(utc)
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
            column.contains("TELEFONO") || column.contains("TEL") || column == "CONTACTO_REFERENCIA" || column == "CONTACTO_DIRECTO" -> InputMaskUtils.formatTelefono(text)
            column == "NIT" && tableName == "EMPRESA" -> InputMaskUtils.formatNitSimple(text)
            column == "PERIODO" -> InputMaskUtils.formatPeriodo(text)
            else -> text
        }
    }

    private fun getFilters(column: String): Array<InputFilter> {
        val maxLength = when {
            column.contains("TELEFONO") || column.contains("TEL") -> InputMaskUtils.TELEFONO_LENGTH + 1
            column == "CONTACTO_REFERENCIA" -> InputMaskUtils.TELEFONO_LENGTH + 1
            column == "CONTACTO_DIRECTO" -> InputMaskUtils.TELEFONO_LENGTH + 1
            column == "NIT" && tableName == "EMPRESA" -> InputMaskUtils.NIT_LENGTH_SIMPLE
            column.contains("NUP") -> InputMaskUtils.NUP_LENGTH
            column.contains("NUM_DOCUMENTO") -> 17
            column.contains("CODIGO") || column.contains("CERTIFICACION") -> 30
            column.contains("NIVEL_DESTREZA") -> 12
            column == "PERIODO" -> 18
            column.contains("EXPERIENCIA_ANIOS") -> 2
            column.contains("EDAD_MINIMA") || column.contains("EDAD_MAXIMA") -> 2
            else -> 0
        }
        val filters = mutableListOf<InputFilter>()
        if (maxLength > 0) filters.add(InputFilter.LengthFilter(maxLength))
        if (column.contains("EXPERIENCIA_ANIOS") || column.contains("EDAD_MIN") || column.contains("EDAD_MAX")) {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) { if (!source[i].isDigit()) return@InputFilter "" }
                null
            })
        }
        if (column == "CONTACTO_DIRECTO") {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) { if (!source[i].isDigit() && source[i] != '-') return@InputFilter "" }
                null
            })
        }
        if (column == "PERIODO") {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) { if (!source[i].isDigit() && source[i] != '/' && source[i] != '-') return@InputFilter "" }
                null
            })
            filters.add(InputFilter.LengthFilter(18))
        }
        if (column == "NIT" && tableName == "EMPRESA") {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) { if (!source[i].isDigit()) return@InputFilter "" }
                null
            })
        }
        if (column.contains("NUP")) {
            filters.add(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) { if (!source[i].isDigit()) return@InputFilter "" }
                null
            })
        }
        return filters.toTypedArray()
    }

    private fun getInputType(column: String): Int {
        val col = column.uppercase()
        val stringIdCols = setOf("ID_HABILIDAD", "ID_POSTULANTE", "ID_INSTITUCION", "ID_OFERTA_ACADEMICA",
            "ID_POSTULACION", "ID_OFERTA", "ID_FORMACION", "ID_CERTIFICACION", "ID_EXPERIENCIA", "ID_DETALLE",
            "NIT")
        val numericIdCols = setOf("ID_GENERO", "ID_TIPO_DOCUMENTO", "ID_GRADO_ACADEMICO", "ID_RED_SOCIAL",
            "ID_CATEGORIA_HABILIDAD", "ID_USUARIO", "ID_DISTRITO_DEPTO", "ID_DISTRITO_MUNICIPIO", "ID_DISTRITO_ID",
            "ID_DEPARTAMENTO", "ID_MUNICIPIO")
        return when {
            col in stringIdCols -> android.text.InputType.TYPE_CLASS_TEXT
            col in numericIdCols || col.contains("NUP") -> android.text.InputType.TYPE_CLASS_NUMBER
            col.contains("NUM_") || col.contains("DOCUMENTO") -> android.text.InputType.TYPE_CLASS_TEXT
            col == "PERIODO" -> android.text.InputType.TYPE_CLASS_PHONE
            col.contains("FECHA") || col.contains("DATE") -> android.text.InputType.TYPE_CLASS_TEXT
            col.contains("EMAIL") -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            col.contains("TELEFONO") || col.contains("TEL") || col == "CONTACTO_REFERENCIA" || col == "CONTACTO_DIRECTO" -> android.text.InputType.TYPE_CLASS_PHONE
            col.contains("PASSWORD") || col.contains("CONTRA") -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            col.contains("EXPERIENCIA_ANIOS") || col.contains("EDAD_MIN") || col.contains("EDAD_MAX") -> android.text.InputType.TYPE_CLASS_NUMBER
            col.contains("DESCRIPCION") || col.contains("DETALLE") || col.contains("DESC") || col.contains("REQUISITO") ->
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }
    }

    private fun getThemeColor(attr: Int): Int {
        return requireContext().obtainStyledAttributes(intArrayOf(attr)).use { ta ->
            ta.getColor(0, android.graphics.Color.BLACK)
        }
    }

    private fun getHintText(column: String): String {
        if (tableName == "EMPRESA" && column == "NIT") {
            return "NIT EMPRESA"
        }
        if (tableName == "DETALLE_REQUISITO") {
            when (column) {
                "NIT" -> return "Empresa"
                "ID_OFERTA" -> return "Titulo puesto"
            }
        }
        if (tableName == "FORMACION_ACADEMICA") {
            when (column) {
                "ID_OFERTA_ACADEMICA" -> return "Oferta academica"
            }
        }
        if (tableName == "POSTULACION") {
            when (column) {
                "ID_OFERTA" -> return "Oferta de trabajo"
            }
        }
        if (tableName == "POSTULANTE" && column == "ID_GRADO_ACADEMICO") {
            return "Grado academico"
        }
        if (tableName == "CERTIFICACION") {
            when (column) {
                "ID_TIPO_CERTIFICACION" -> return "Tipo de certificacion"
                "PERIODO" -> return "Periodo (ej. 2024-2026)"
            }
        }
        if (tableName == "FORMACION_ACADEMICA" && column == "PERIODO") {
            return "Periodo (ej. 2024-2026)"
        }
        val col = column.uppercase()
        return when (col) {
            "ID_GENERO" -> "Género"
            "ID_TIPO_DOCUMENTO" -> "Tipo de documento"
            "NUM_DOCUMENTO" -> "Número de documento"
            "ID_DISTRITO" -> "Id Distrito"
            "NOMBRE" -> "Nombre"
            "APELLIDO" -> "Apellido"
            "FECHA_NACIMIENTO" -> "Fecha de nacimiento"
            "NUP" -> "NUP"
            "DIRECCION_DETALLE" -> "Dirección"
            "TELEFONO_CASA" -> "Teléfono casa"
            "TELEFONO_CELULAR" -> "Teléfono celular"
            "EMAIL", "CORREO" -> "Correo electrónico"
            "ID_DEPARTAMENTO" -> "Departamento"
            "ID_MUNICIPIO" -> "Id Municipio"
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
            "DESCRIPCION_REQUISITO" -> "Descripcion del requisito"
            "NIT" -> "Empresa"
            "ID_OFERTA" -> "Codigo de oferta"
            "ID_POSTULANTE" -> "Codigo postulante"
            "ID_CERTIFICACION" -> "Codigo certificacion"
            "NOMBRE_CERTIFICACION" -> "Nombre de certificacion"
            "FECHA_CERTIFICACION" -> "Fecha de certificacion"
            "ID_EXPERIENCIA" -> "Codigo experiencia"
            "PUESTO_TRABAJO" -> "Puesto de trabajo"
            "FECHA_INICIO" -> "Fecha de inicio"
            "FECHA_FIN" -> "Fecha de fin"
            "DESCP_EXPERIENCIA_LABORAL" -> "Descripcion de experiencia"
            "CONTACTO_REFERENCIA" -> "Contacto de referencia"
            "ID_FORMACION" -> "Codigo formacion"
            "ID_OFERTA_ACADEMICA" -> "Codigo oferta academica"
            "TITULO_OBTENIDO" -> "Titulo obtenido"
            "FECHA_OBTENCION" -> "Fecha de obtencion"
            "ID_HABILIDAD" -> "Codigo habilidad"
            "NIVEL_DESTREZA" -> "Nivel de destreza"
            "ID_POSTULACION" -> "Codigo postulacion"
            "FECHA_APLICACION" -> "Fecha de aplicacion"
            "ESTADO_PROCESO" -> "Estado del proceso"
            "ID_DETALLE" -> "Codigo detalle"
            "ID_RED_SOCIAL" -> "Red social"
            "URL_PERFIL" -> "URL del perfil"
            "ID_DISTRITO_DEPTO" -> "Departamento"
            "ID_DISTRITO_MUNICIPIO" -> "Municipio"
            "ID_DISTRITO_ID" -> "Distrito"
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

    private fun getColumnsForTable(table: String): List<String> = Constants.getColumnsForTable(table)

    private fun setupButtons() {
        if (isViewMode) {
            btnSave.visibility = View.GONE
            btnCancel.text = "Cerrar"
        }
        btnSave.setOnClickListener { saveData() }
        btnCancel.setOnClickListener { dismiss() }
    }

    private fun setupViewMode() {
        for (i in 0 until tilFieldsContainer.childCount) {
            val child = tilFieldsContainer.getChildAt(i)
            if (child is com.google.android.material.textfield.TextInputLayout) {
                val editText = child.editText
                if (editText != null) {
                    editText.isEnabled = false
                    editText.isFocusable = false
                    editText.isClickable = false
                }
            }
        }
    }

    private fun disableNonEstadoFields() {
        for (i in 0 until tilFieldsContainer.childCount) {
            val child = tilFieldsContainer.getChildAt(i)
            if (child is com.google.android.material.textfield.TextInputLayout) {
                val hint = child.hint?.toString()?.lowercase() ?: ""
                val col = dropDownFields.values.find { it.second == child.editText }?.first?.lowercase() ?: ""
                val isEstadoColumn = col.contains("estado_proceso") || hint.contains("estado del proceso")
                if (!isEstadoColumn) {
                    val editText = child.editText
                    if (editText != null) {
                        editText.isEnabled = false
                        editText.isFocusable = false
                        editText.isClickable = false
                    }
                }
            }
        }
    }

    private fun disableAllFields() {
        for (i in 0 until tilFieldsContainer.childCount) {
            val child = tilFieldsContainer.getChildAt(i)
            if (child is com.google.android.material.textfield.TextInputLayout) {
                val editText = child.editText
                if (editText != null) {
                    editText.isEnabled = false
                    editText.isFocusable = false
                    editText.isClickable = false
                }
            }
        }
    }

    private fun saveData() {
        if (isViewMode) return

        val role = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE) ?: Constants.ROLE_POSTULANTE
        val access = Constants.getRoleTables(role)[tableName] ?: Constants.AccessLevel.NONE
        if (access != Constants.AccessLevel.FULL) {
            StyledToast.show(requireContext(), "No tienes permiso para modificar esta tabla")
            return
        }

        if (role == Constants.ROLE_POSTULANTE && tableName == "POSTULACION" && isEditMode) {
            StyledToast.show(requireContext(), "No tienes permiso para editar esta postulación")
            return
        }

        val editableColumns = columns.filter { it != getAutoGenColumn(tableName) }
        val values = mutableListOf<String>()

        for (col in editableColumns) {
            val fkRef = fkRefs[col]

            when {
                col == "NIVEL_DESTREZA" -> {
                    val autoComplete = dropDownFields.values.find { it.first == col }?.second
                    val selectedText = autoComplete?.text?.toString()?.trim() ?: ""
                    if (selectedText.isBlank()) {
                        StyledToast.show(requireContext(), "Debe seleccionar un nivel de destreza")
                        return
                    }
                    values.add(selectedText)
                }
                col == "ESTADO_PROCESO" -> {
                    if (role == Constants.ROLE_POSTULANTE && tableName == "POSTULACION") {
                        values.add("activo")
                    } else {
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
                    }
                }
                col == "ROL" -> {
                    val autoComplete = dropDownFields.values.find { it.first == col }?.second
                    val selectedText = autoComplete?.text?.toString()?.trim() ?: "postulante"
                    values.add(selectedText)
                }
                fkRef != null -> {
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
                }
                else -> {
                    val et = textFields.values.find { it.first == col }?.second
                    val textValue = et?.text?.toString()?.trim() ?: ""

                    val errorMsg = ValidationRules.validate(tableName, col, textValue)

                    if (errorMsg != null) {
                        var parent = et?.parent
                        while (parent != null && parent !is TextInputLayout) {
                            parent = parent.parent
                        }
                        if (parent is TextInputLayout) {
                            parent.error = errorMsg
                        }
                        StyledToast.show(requireContext(), errorMsg)
                        return
                    }

                    values.add(textValue)
                }
            }
        }

        if (tableName == "USUARIO") {
            val usernameIndex = editableColumns.indexOf("USERNAME")
            if (usernameIndex >= 0 && usernameIndex < values.size) {
                savedUsername = values[usernameIndex]
            }
            val roleIndex = editableColumns.indexOf("ROL")
            if (roleIndex >= 0 && roleIndex < values.size) {
                savedNewRole = values[roleIndex]
            }
        }

        btnSave.isEnabled = false
        if (isEditMode) {
            val pkCols = getPrimaryKeyColumns(tableName)
            val pkString = pkCols.map { col ->
                val idx = columns.indexOf(col)
                if (idx >= 0 && idx < itemData.size) itemData[idx].trim() else ""
            }.joinToString("|")
            viewModel.updateRecord(tableName, pkString, values)
        } else {
            viewModel.insertRecord(tableName, values)
        }
    }

    private fun getPrimaryKeyColumns(table: String): List<String> = Constants.getPrimaryKeyColumns(table)

    private fun getAutoGenColumn(table: String): String? = Constants.getAutoGenColumn(table)
}