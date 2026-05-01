package sv.ues.fia.eisi.bt.ui.crud

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.viewmodel.CrudViewModel
import sv.ues.fia.eisi.bt.viewmodel.Resource

class DeleteConfirmDialog : DialogFragment() {

    private val viewModel: CrudViewModel by viewModels({ requireParentFragment() })
    private var itemData: String = ""
    private var tableName: String = ""
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.AestheticDialog)
        arguments?.let {
            itemData = it.getString("itemData", "")
            tableName = it.getString(Constants.BUNDLE_TABLE_NAME, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val titleTv = TextView(context).apply {
            text = "Eliminar ${tableName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6)
            setPadding(0, 0, 0, 24)
        }
        layout.addView(titleTv)

        val progressBar = ProgressBar(context).apply {
            visibility = View.VISIBLE
            isIndeterminate = true
            setPadding(0, 24, 0, 24)
        }
        layout.addView(progressBar)

        val loadingText = TextView(context).apply {
            text = "Consultando dependencias..."
            setPadding(0, 0, 0, 24)
        }
        layout.addView(loadingText)

        val depsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        layout.addView(depsLayout)

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            visibility = View.GONE
        }

        val btnCancel = MaterialButton(context).apply {
            text = "Cancelar"
            setOnClickListener { dismiss() }
        }
        buttonsLayout.addView(btnCancel)

        val btnDelete = MaterialButton(context).apply {
            text = "Eliminar todo"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
            setOnClickListener { performDelete() }
        }
        buttonsLayout.addView(btnDelete)
        layout.addView(buttonsLayout)

        viewModel.deleteDependencies.observe(viewLifecycleOwner, Observer { deps ->
            if (deps == null) return@Observer
            progressBar.visibility = View.GONE
            loadingText.visibility = View.GONE

            if (deps.isEmpty()) {
                showSimpleConfirm(titleTv, layout, depsLayout, buttonsLayout)
            } else {
                showDependenciesConfirm(deps, titleTv, layout, depsLayout, buttonsLayout)
            }
        })

        viewModel.operationResult.removeObservers(this)
        viewModel.operationResult.observe(this) { result ->
            if (result == null) return@observe
            if (!isAdded) return@observe
            isLoading = false
            when (result) {
                is Resource.Success -> {
                    StyledToast.show(requireContext(), result.message)
                    viewModel.clearResult()
                    dismiss()
                }
                is Resource.Error -> {
                    StyledToast.show(requireContext(), result.translatedMessage)
                    viewModel.clearResult()
                    dismiss()
                }
            }
        }

        val dataList = itemData.split(",")
        if (dataList.isNotEmpty()) {
            viewModel.checkDeleteDependencies(dataList.first().trim())
        }

        return layout
    }

    private fun showSimpleConfirm(
        titleTv: TextView,
        layout: LinearLayout,
        depsLayout: LinearLayout,
        buttonsLayout: LinearLayout
    ) {
        titleTv.text = "¿Eliminar este registro?"
        val noDepsText = TextView(requireContext()).apply {
            text = "Este registro no tiene dependencias."
            setPadding(0, 0, 0, 16)
        }
        depsLayout.addView(noDepsText)
        depsLayout.visibility = View.VISIBLE
        buttonsLayout.visibility = View.VISIBLE
    }

    private fun showDependenciesConfirm(
        deps: List<MainRepository.DependencyInfo>,
        titleTv: TextView,
        layout: LinearLayout,
        depsLayout: LinearLayout,
        buttonsLayout: LinearLayout
    ) {
        titleTv.text = "⚠ Este registro tiene dependencias"
        val warningText = TextView(requireContext()).apply {
            text = "Se eliminarán TODOS los registros asociados:"
            setPadding(0, 0, 0, 8)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
        }
        depsLayout.addView(warningText)

        val totalRecords = deps.sumOf { it.count }
        for (dep in deps) {
            val depText = TextView(requireContext()).apply {
                text = "  • ${dep.count} ${dep.displayName}"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2)
            }
            depsLayout.addView(depText)
        }

        val totalText = TextView(requireContext()).apply {
            text = "\nTotal: $totalRecords registros vinculados"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
            setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        }
        depsLayout.addView(totalText)

        depsLayout.visibility = View.VISIBLE
        buttonsLayout.visibility = View.VISIBLE
    }

    private fun performDelete() {
        if (isLoading) return

        val dataList = itemData.split(",").map { it.trim() }
        val idToDelete = dataList.firstOrNull()

        // Validar que el usuario activo no se pueda borrar a sí mismo
        if (tableName == Constants.TABLE_USUARIO && idToDelete != null) {
            val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val activeUserId = prefs.getInt(Constants.KEY_USER_ID, -1)
            if (idToDelete == activeUserId.toString()) {
                StyledToast.show(requireContext(), "No puedes eliminar tu propio usuario mientras está activo")
                dismiss()
                return
            }
        }

        isLoading = true

        val columns = viewModel.getFkReferences(tableName)

        val needsComposite = tableName in listOf(
            "OFERTA_TRABAJO", "EXPERIENCIA_LABORAL", "CERTIFICACION",
            "HABILIDAD_POSTULANTE", "POSTULACION", "DETALLE_REQUISITO"
        )

        if (needsComposite && dataList.size > 1) {
            viewModel.deleteItemByRow(dataList)
        } else if (dataList.isNotEmpty()) {
            viewModel.deleteItem(dataList.first())
        } else {
            dismiss()
        }
    }
}
