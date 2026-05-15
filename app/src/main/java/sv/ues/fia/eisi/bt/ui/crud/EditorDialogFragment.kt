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
import sv.ues.fia.eisi.bt.utils.getTableDisplayName

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
                    StyledToast.show(requireContext(), getString(R.string.solo_empresa_modificar_postulacion))
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
            btnSave.text = getString(R.string.save)
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
        val displayName = requireContext().getTableDisplayName(tableName)
        tvTitle.text = when {
            isViewMode -> getString(R.string.ver_tabla, displayName)
            isEditMode -> getString(R.string.editar_tabla, displayName)
            else -> getString(R.string.nuevo_tabla, displayName)
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

            if (tableName == "POSTULANTE" && column == "ID_DISTRITO_DEPTO") {
                createPostulanteDepartamentoField()
            } else if (tableName == "POSTULANTE" && column == "ID_DISTRITO_MUNICIPIO") {
                createPostulanteMunicipioField()
            } else if (tableName == "EMPRESA" && column == "ID_DISTRITO_DEPTO") {
                createEmpresaDepartamentoField()
            } else if (tableName == "EMPRESA" && column == "ID_DISTRITO_MUNICIPIO") {
                createEmpresaMunicipioField()
            } else if (isFk) {
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

        if (isEditMode) {
            blockPkFieldsIfHasChildren()
        }
    }

    private fun blockPkFieldsIfHasChildren() {
        val pkCols = getPrimaryKeyColumns(tableName)
        val pkValues = pkCols.map { col ->
            val idx = columns.indexOf(col)
            if (idx >= 0 && idx < itemData.size) itemData[idx].trim() else ""
        }
        if (pkValues.all { it.isBlank() }) return
        if (!viewModel.hasChildRecords(tableName, pkValues)) return

        val blockedHint = getString(R.string.id_bloqueado)
        for (pkCol in pkCols) {
            val tf = textFields.values.find { it.first == pkCol }
            if (tf != null) {
                tf.second.isEnabled = false
                var parent = tf.second.parent
                while (parent != null) {
                    if (parent is TextInputLayout) {
                        parent.hint = blockedHint
                        break
                    }
                    parent = parent.parent
                }
            }
            val dd = dropDownFields.values.find { it.first == pkCol }
            if (dd != null) {
                dd.second.isEnabled = false
                var parent = dd.second.parent
                while (parent != null) {
                    if (parent is TextInputLayout) {
                        parent.hint = blockedHint
                        break
                    }
                    parent = parent.parent
                }
            }
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
        if (column == "ID_DISTRITO_ID" && tableName == "POSTULANTE") {
            val deptoAC = postulanteDepartamentoAutoComplete
            val munAC = postulanteMunicipioAutoComplete
            if (deptoAC != null && munAC != null) {
                val deptoText = deptoAC.text?.toString()?.trim()
                val munText = munAC.text?.toString()?.trim()
                if (!deptoText.isNullOrBlank() && !munText.isNullOrBlank()) {
                    val deptoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
                    val munFiltered = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO",
                        deptoOptions.find { it.second == deptoText }?.first ?: "", "NOMBRE_MUNICIPIO")
                    val deptoId = deptoOptions.find { it.second == deptoText }?.first
                    val munId = munFiltered.find { it.second == munText }?.first
                    if (deptoId != null && munId != null) {
                        parentId = "$deptoId|$munId"
                        filterColumn = "ID_MUNICIPIO"
                    }
                }
            }
        }
        if (column == "ID_DISTRITO_ID" && tableName == "EMPRESA") {
            val deptoAC = empresaDepartamentoAutoComplete
            val munAC = empresaMunicipioAutoComplete
            if (deptoAC != null && munAC != null) {
                val deptoText = deptoAC.text?.toString()?.trim()
                val munText = munAC.text?.toString()?.trim()
                if (!deptoText.isNullOrBlank() && !munText.isNullOrBlank()) {
                    val deptoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
                    val munFiltered = viewModel.getFilteredOptions("MUNICIPIO", "ID_DEPARTAMENTO",
                        deptoOptions.find { it.second == deptoText }?.first ?: "", "NOMBRE_MUNICIPIO")
                    val deptoId = deptoOptions.find { it.second == deptoText }?.first
                    val munId = munFiltered.find { it.second == munText }?.first
                    if (deptoId != null && munId != null) {
                        parentId = "$deptoId|$munId"
                        filterColumn = "ID_MUNICIPIO"
                    }
                }
            }
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
                    textFields[numDocColumnIndex]?.second?.setText("")
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
            "Básico" to getString(R.string.nivel_basico),
            "Intermedio" to getString(R.string.nivel_intermedio),
            "Avanzado" to getString(R.string.nivel_avanzado)
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
            "activo" to getString(R.string.estado_activo),
            "en proceso" to getString(R.string.estado_en_proceso),
            "contratado" to getString(R.string.estado_contratado),
            "rechazado" to getString(R.string.estado_rechazado)
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
                    autoComplete.setText(getString(R.string.estado_activo), false)
                }
            } else {
                autoComplete.setText(getString(R.string.estado_activo), false)
            }
            til.helperText = getString(R.string.solo_empresa_cambiar_estado)
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
        val roles = listOf(
            "postulante" to getString(R.string.rol_postulante),
            "gerente de empresa" to getString(R.string.rol_empresa),
            "administrador" to getString(R.string.rol_admin)
        )
        val displayRoles = roles.map { it.second }

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

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayRoles)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                autoComplete.showDropDown()
            }
            true
        }

        if ((isEditMode || isViewMode) && colIndex < itemData.size) {
            val currentValue = itemData[colIndex].trim()
            val optionIndex = roles.indexOfFirst { it.first == currentValue }
            if (optionIndex >= 0) {
                autoComplete.setText(displayRoles[optionIndex], false)
            } else {
                autoComplete.setText(displayRoles[0], false)
            }
        } else {
            autoComplete.setText(displayRoles[0], false)
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
            hint = getString(R.string.hint_departamento)
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
            var currentMunicipioId = itemData[1].trim()
            val currentDeptoId = itemData[0].trim()
            val departamentoId = viewModel.getDepartamentoByMunicipio(currentDeptoId, currentMunicipioId)
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
            hint = getString(R.string.hint_departamento)
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

        if ((isEditMode || isViewMode) && itemData.size >= 4) {
            val deptoId = itemData[1].trim()
            val munId = itemData[2].trim()
            val distritoId = itemData[3].trim()
            val deptOptionIndex = departamentoOptions.indexOfFirst { it.first == deptoId }
            if (deptOptionIndex >= 0) {
                autoComplete.setText(displayOptions[deptOptionIndex], false)
                post {
                    prefillEmpresaMunicipio(munId)
                    post {
                        prefillEmpresaDistrito(distritoId)
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
        dropDownFields[columns.indexOf("ID_DISTRITO_DEPTO")] = Pair("ID_DISTRITO_DEPTO", autoComplete)
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
            hint = getString(R.string.hint_municipio)
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
        dropDownFields[columns.indexOf("ID_DISTRITO_MUNICIPIO")] = Pair("ID_DISTRITO_MUNICIPIO", autoComplete)
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
        val deptText = empresaDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", "$departamentoId|$municipioId", "NOMBRE_DISTRITO")
        val displayOptions = distritoOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO_ID" }?.second ?: return
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
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO_ID" }?.second ?: return

        val munText = municipioAutoComplete.text?.toString()?.trim() ?: return
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        val municipioId = municipioOptions.find { it.second == munText }?.first ?: return
        val deptText = empresaDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", "$departamentoId|$municipioId", "NOMBRE_DISTRITO")
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

    private fun createPostulanteMunicipioField() {
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")

        val til = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            hint = getString(R.string.hint_municipio)
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
        dropDownFields[columns.indexOf("ID_DISTRITO_MUNICIPIO")] = Pair("ID_DISTRITO_MUNICIPIO", autoComplete)
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
            hint = getString(R.string.hint_departamento)
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

        if ((isEditMode || isViewMode) && itemData.size >= 8) {
            val deptoId = itemData[5].trim()
            val munId = itemData[6].trim()
            val distritoId = itemData[7].trim()
            val deptOptionIndex = departamentoOptions.indexOfFirst { it.first == deptoId }
            if (deptOptionIndex >= 0) {
                autoComplete.setText(displayOptions[deptOptionIndex], false)
                post {
                    prefillPostulanteMunicipio(munId)
                    post {
                        prefillPostulanteDistrito(distritoId)
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
        dropDownFields[columns.indexOf("ID_DISTRITO_DEPTO")] = Pair("ID_DISTRITO_DEPTO", autoComplete)
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
        val deptText = postulanteDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", "$departamentoId|$municipioId", "NOMBRE_DISTRITO")
        val displayOptions = distritoOptions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayOptions)
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO_ID" }?.second ?: return
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
        val distritoAutoComplete = dropDownFields.values.find { it.first == "ID_DISTRITO_ID" }?.second ?: return

        val munText = municipioAutoComplete.text?.toString()?.trim() ?: return
        val municipioOptions = viewModel.getDropdownOptions("MUNICIPIO", "NOMBRE_MUNICIPIO")
        val municipioId = municipioOptions.find { it.second == munText }?.first ?: return
        val deptText = postulanteDepartamentoAutoComplete?.text?.toString()?.trim() ?: return
        val departamentoOptions = viewModel.getDropdownOptions("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
        val departamentoId = departamentoOptions.find { it.second == deptText }?.first ?: return

        val distritoOptions = viewModel.getFilteredOptions("DISTRITO", "ID_MUNICIPIO", "$departamentoId|$municipioId", "NOMBRE_DISTRITO")
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
            "PASAPORTE" -> getString(R.string.doc_pasaporte)
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
            helperText = getHelperText(column)
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
            til.hint = getString(R.string.contrasena_bloqueada)
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
        val titulo = til?.hint?.toString() ?: getString(R.string.seleccionar_fecha)

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

    private fun getHintResId(column: String): Int {
        return when (column.uppercase()) {
            "ID_GENERO" -> R.string.hint_genero
            "ID_TIPO_DOCUMENTO" -> R.string.hint_tipo_documento
            "NUM_DOCUMENTO" -> R.string.hint_numero_documento
            "ID_DISTRITO" -> R.string.hint_id_distrito
            "NOMBRE" -> R.string.hint_nombre
            "APELLIDO" -> R.string.hint_apellido
            "FECHA_NACIMIENTO" -> R.string.hint_fecha_nacimiento
            "NUP" -> R.string.hint_nup
            "DIRECCION_DETALLE" -> R.string.hint_direccion
            "TELEFONO_CASA" -> R.string.hint_telefono_casa
            "TELEFONO_CELULAR" -> R.string.hint_telefono_celular
            "EMAIL", "CORREO" -> R.string.hint_correo_electronico
            "ID_DEPARTAMENTO" -> R.string.hint_departamento
            "ID_MUNICIPIO" -> R.string.hint_id_municipio
            "NOMBRE_CATEGORIA" -> R.string.hint_nombre_categoria
            "NOMBRE_GENERO" -> R.string.hint_nombre_genero
            "NOMBRE_TIPO" -> R.string.hint_nombre_tipo
            "NOMBRE_DEPARTAMENTO" -> R.string.hint_nombre_departamento
            "NOMBRE_MUNICIPIO" -> R.string.hint_nombre_municipio
            "NOMBRE_DISTRITO" -> R.string.hint_nombre_distrito
            "NOMBRE_HABILIDAD" -> R.string.hint_nombre_habilidad
            "ID_CATEGORIA_HABILIDAD" -> R.string.hint_categoria_habilidad
            "NOMBRE_EMPRESA" -> R.string.hint_nombre_empresa
            "CONTACTO_DIRECTO" -> R.string.hint_contacto_directo
            "NOMBRE_INSTITUCION" -> R.string.hint_nombre_institucion
            "NOMBRE_GRADO" -> R.string.hint_nombre_grado
            "NOMBRE_RED" -> R.string.hint_nombre_red_social
            "ID_INSTITUCION" -> R.string.hint_institucion
            "ID_GRADO_ACADEMICO" -> R.string.hint_grado_academico
            "TITULO_PUESTO" -> R.string.hint_titulo_puesto
            "FECHA_PUBLICACION" -> R.string.hint_fecha_publicacion
            "FECHA_CADUCIDAD" -> R.string.hint_fecha_caducidad
            "EXPERIENCIA_ANIOS" -> R.string.hint_anios_experiencia
            "EDAD_MINIMA" -> R.string.hint_edad_minima
            "EDAD_MAXIMA" -> R.string.hint_edad_maxima
            "DESCRIPCION_OFERTA_TRABAJO", "DESCRIPCION", "DESC" -> R.string.hint_descripcion
            "DESCRIPCION_REQUISITO" -> R.string.hint_descripcion_requisito
            "NIT" -> R.string.hint_empresa
            "ID_OFERTA" -> R.string.hint_codigo_oferta
            "ID_POSTULANTE" -> R.string.hint_codigo_postulante
            "ID_CERTIFICACION" -> R.string.hint_codigo_certificacion
            "NOMBRE_CERTIFICACION" -> R.string.hint_nombre_certificacion
            "FECHA_CERTIFICACION" -> R.string.hint_fecha_certificacion
            "ID_EXPERIENCIA" -> R.string.hint_codigo_experiencia
            "PUESTO_TRABAJO" -> R.string.hint_puesto_trabajo
            "FECHA_INICIO" -> R.string.hint_fecha_inicio
            "FECHA_FIN" -> R.string.hint_fecha_fin
            "DESCP_EXPERIENCIA_LABORAL" -> R.string.hint_descripcion_experiencia
            "CONTACTO_REFERENCIA" -> R.string.hint_contacto_referencia
            "ID_FORMACION" -> R.string.hint_codigo_formacion
            "ID_OFERTA_ACADEMICA" -> R.string.hint_codigo_oferta_academica
            "TITULO_OBTENIDO" -> R.string.hint_titulo_obtenido
            "FECHA_OBTENCION" -> R.string.hint_fecha_obtencion
            "ID_HABILIDAD" -> R.string.hint_codigo_habilidad
            "NIVEL_DESTREZA" -> R.string.hint_nivel_destreza
            "ID_POSTULACION" -> R.string.hint_codigo_postulacion
            "FECHA_APLICACION" -> R.string.hint_fecha_aplicacion
            "ESTADO_PROCESO" -> R.string.hint_estado_proceso
            "ID_DETALLE" -> R.string.hint_codigo_detalle
            "ID_RED_SOCIAL" -> R.string.hint_red_social
            "URL_PERFIL" -> R.string.hint_url_perfil
            "ID_DISTRITO_DEPTO" -> R.string.hint_departamento
            "ID_DISTRITO_MUNICIPIO" -> R.string.hint_municipio
            "ID_DISTRITO_ID" -> R.string.hint_id_distrito
            "USERNAME", "USER" -> R.string.hint_nombre_usuario
            "PASSWORD", "CONTRA" -> R.string.hint_contrasena
            "ROL" -> R.string.hint_rol
            else -> 0
        }
    }

    private fun getHintText(column: String): String {
        val resId = getHintResId(column)
        if (resId != 0) return getString(resId)
        val hintOverride = when (column.uppercase()) {
            "ID_TIPO_CERTIFICACION" -> if (tableName == "CERTIFICACION") R.string.hint_tipo_certificacion else 0
            "ID_HABILIDAD" -> if (tableName == "HABILIDAD_POSTULANTE") R.string.hint_habilidad_postulante else 0
            else -> 0
        }
        if (hintOverride != 0) return getString(hintOverride)
        val tableHint = when {
            tableName == "EMPRESA" && column == "NIT" -> R.string.hint_nit_empresa
            tableName == "DETALLE_REQUISITO" && column == "NIT" -> R.string.hint_empresa
            tableName == "DETALLE_REQUISITO" && column == "ID_OFERTA" -> R.string.hint_titulo_puesto
            tableName == "FORMACION_ACADEMICA" && column == "ID_OFERTA_ACADEMICA" -> R.string.hint_oferta_academica
            tableName == "POSTULACION" && column == "ID_OFERTA" -> R.string.hint_oferta_trabajo
            tableName == "POSTULANTE" && column == "ID_GRADO_ACADEMICO" -> R.string.hint_grado_academico
            (tableName == "CERTIFICACION" || tableName == "FORMACION_ACADEMICA" || tableName == "EXPERIENCIA_LABORAL") && column == "FECHA_INICIO" -> R.string.hint_periodo_fecha_inicio
            (tableName == "CERTIFICACION" || tableName == "FORMACION_ACADEMICA" || tableName == "EXPERIENCIA_LABORAL") && column == "FECHA_FIN" -> R.string.hint_periodo_fecha_fin
            else -> 0
        }
        if (tableHint != 0) return getString(tableHint)
        return column.replace("ID_", "").replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun getHelperText(column: String): String? {
        return when (column.uppercase()) {
            "ID_POSTULANTE" -> getString(R.string.helper_format, getString(R.string.helper_ej_ab12345))
            "ID_OFERTA" -> getString(R.string.helper_format, getString(R.string.helper_ej_of001))
            "ID_CERTIFICACION" -> getString(R.string.helper_format, getString(R.string.helper_ej_c001))
            "ID_EXPERIENCIA" -> getString(R.string.helper_format, getString(R.string.helper_ej_el01))
            "ID_FORMACION" -> getString(R.string.helper_format, getString(R.string.helper_ej_foa001))
            "ID_OFERTA_ACADEMICA" -> getString(R.string.helper_format, getString(R.string.helper_ej_ofa01))
            "ID_POSTULACION" -> getString(R.string.helper_format, getString(R.string.helper_ej_pos001))
            "ID_DETALLE" -> getString(R.string.helper_format, getString(R.string.helper_ej_d1))
            "ID_INSTITUCION" -> getString(R.string.helper_format, getString(R.string.helper_ej_ins001))
            "ID_HABILIDAD" -> getString(R.string.helper_format, getString(R.string.helper_ej_h01))
            "NIT" -> if (tableName == "EMPRESA") getString(R.string.helper_14_digitos) else null
            "ID_MUNICIPIO" -> getString(R.string.helper_format, getString(R.string.helper_ej_5))
            "ID_DISTRITO" -> getString(R.string.helper_format, getString(R.string.helper_ej_1))
            "URL_PERFIL" -> getString(R.string.helper_format, getString(R.string.helper_ej_https))
            else -> null
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
            btnCancel.text = getString(R.string.cerrar)
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
            StyledToast.show(requireContext(), getString(R.string.sin_permiso_modificar_tabla))
            return
        }

        if (role == Constants.ROLE_POSTULANTE && tableName == "POSTULACION" && isEditMode) {
            StyledToast.show(requireContext(), getString(R.string.sin_permiso_editar_postulacion))
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
                        StyledToast.show(requireContext(), getString(R.string.debe_seleccionar_nivel))
                        return
                    }
                    val actualValue = when (selectedText) {
                        getString(R.string.nivel_basico) -> "Básico"
                        getString(R.string.nivel_intermedio) -> "Intermedio"
                        getString(R.string.nivel_avanzado) -> "Avanzado"
                        else -> selectedText
                    }
                    values.add(actualValue)
                }
                col == "ESTADO_PROCESO" -> {
                    if (role == Constants.ROLE_POSTULANTE && tableName == "POSTULACION") {
                        values.add("activo")
                    } else {
                        val autoComplete = dropDownFields.values.find { it.first == col }?.second
                        val selectedText = autoComplete?.text?.toString()?.trim() ?: ""
                        if (selectedText.isBlank()) {
                            StyledToast.show(requireContext(), getString(R.string.debe_seleccionar_estado))
                            return
                        }
                        val estadoValue = when (selectedText) {
                            getString(R.string.estado_activo) -> "activo"
                            getString(R.string.estado_en_proceso) -> "en proceso"
                            getString(R.string.estado_contratado) -> "contratado"
                            getString(R.string.estado_rechazado) -> "rechazado"
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
                    val actualValue = when (selectedText) {
                        getString(R.string.rol_postulante) -> "postulante"
                        getString(R.string.rol_empresa) -> "gerente de empresa"
                        getString(R.string.rol_admin) -> "administrador"
                        else -> selectedText
                    }
                    values.add(actualValue)
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
                        StyledToast.show(requireContext(), getString(R.string.no_hay_datos_en, fkRef.refTable))
                        return
                    }

                    val autoComplete = dropDownFields.values.find { it.first == col }?.second
                    val selectedText = autoComplete?.text?.toString()?.trim() ?: ""
                    if (selectedText.isBlank()) {
                        StyledToast.show(requireContext(), getString(R.string.debe_seleccionar, fkRef.refTable))
                        return
                    }

                    val selectedOption = options.find { it.second == selectedText }
                    if (selectedOption == null) {
                        StyledToast.show(requireContext(), getString(R.string.seleccione_opcion_valida, fkRef.refTable, selectedText))
                        return
                    }
                    values.add(selectedOption.first)
                }
                else -> {
                    val et = textFields.values.find { it.first == col }?.second
                    val textValue = et?.text?.toString()?.trim() ?: ""

                    val errorMsg = ValidationRules.validate(requireContext(), tableName, col, textValue)

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

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

        fun validatePeriodDates(inicioIdx: Int, finIdx: Int, fechaRefIdx: Int, fechaRefName: String): Boolean {
            if (inicioIdx < 0 || finIdx < 0 || fechaRefIdx < 0) return true
            val fechaInicio = values.getOrNull(inicioIdx)?.toString()?.trim() ?: ""
            val fechaFin = values.getOrNull(finIdx)?.toString()?.trim() ?: ""
            val fechaRef = values.getOrNull(fechaRefIdx)?.toString()?.trim() ?: ""
            if (fechaInicio.isBlank() || fechaFin.isBlank() || fechaRef.isBlank()) return true
            try {
                val dInicio = dateFormat.parse(fechaInicio)!!
                val dFin = dateFormat.parse(fechaFin)!!
                val dRef = dateFormat.parse(fechaRef)!!
                if (!dInicio.before(dFin)) {
                    StyledToast.show(requireContext(), getString(R.string.fecha_inicio_menor_fin))
                    btnSave.isEnabled = true; return false
                }
                if (dRef.before(dFin)) {
                    StyledToast.show(requireContext(), getString(R.string.fecha_ref_menor_periodo, fechaRefName))
                    btnSave.isEnabled = true; return false
                }
                val cal = java.util.Calendar.getInstance()
                cal.time = dFin
                cal.add(java.util.Calendar.YEAR, 1)
                if (dRef.after(cal.time)) {
                    StyledToast.show(requireContext(), getString(R.string.fecha_ref_excede_anio, fechaRefName))
                    btnSave.isEnabled = true; return false
                }
            } catch (_: Exception) { return true }
            return true
        }

        if (tableName == "CERTIFICACION") {
            val inicioIdx = editableColumns.indexOf("FECHA_INICIO")
            val finIdx = editableColumns.indexOf("FECHA_FIN")
            val certIdx = editableColumns.indexOf("FECHA_CERTIFICACION")
            if (!validatePeriodDates(inicioIdx, finIdx, certIdx, getString(R.string.fecha_ref_name_certificacion))) return
        }
        if (tableName == "FORMACION_ACADEMICA") {
            val inicioIdx = editableColumns.indexOf("FECHA_INICIO")
            val finIdx = editableColumns.indexOf("FECHA_FIN")
            val obtenIdx = editableColumns.indexOf("FECHA_OBTENCION")
            if (!validatePeriodDates(inicioIdx, finIdx, obtenIdx, getString(R.string.fecha_ref_name_obtencion))) return
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