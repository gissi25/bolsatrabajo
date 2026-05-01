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
        setStyle(STYLE_NORMAL, R.style.AestheticDialog)
        arguments?.let {
            itemData = it.getString("itemData", "")
            tableName = it.getString(Constants.BUNDLE_TABLE_NAME, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 48, 64, 48)
            setBackgroundResource(R.drawable.bg_dialog)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val titleTv = TextView(context).apply {
            text = "Eliminar ${tableName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6)
            setTextColor(context.getColor(R.color.text_primary))
            setPadding(0, 0, 0, 32)
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(titleTv)

        val progressBar = ProgressBar(context).apply {
            visibility = View.VISIBLE
            isIndeterminate = true
            setPadding(0, 0, 0, 32)
        }
        layout.addView(progressBar)

        val loadingText = TextView(context).apply {
            text = "Consultando dependencias..."
            setTextColor(context.getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 32)
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(loadingText)

        val depsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        layout.addView(depsLayout)

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
        }

        val btnCancel = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Cancelar"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 16
            }
            setOnClickListener { dismiss() }
        }
        buttonsLayout.addView(btnCancel)

        val btnDelete = MaterialButton(context).apply {
            text = "Eliminar"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { performDelete() }
        }
        buttonsLayout.addView(btnDelete)
        layout.addView(buttonsLayout)

        viewModel.deleteDependencies.observe(viewLifecycleOwner, Observer { deps ->
            if (deps == null) return@Observer
            progressBar.visibility = View.GONE
            loadingText.visibility = View.GONE

            if (deps.isEmpty()) {
                showSimpleConfirm(titleTv, depsLayout, buttonsLayout)
            } else {
                showDependenciesConfirm(deps, titleTv, depsLayout, buttonsLayout)
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
        depsLayout: LinearLayout,
        buttonsLayout: LinearLayout
    ) {
        titleTv.text = "¿Eliminar este registro?"
        depsLayout.removeAllViews()
        val noDepsText = TextView(requireContext()).apply {
            text = "Este registro no tiene dependencias."
            setTextColor(requireContext().getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 32)
            gravity = android.view.Gravity.CENTER
        }
        depsLayout.addView(noDepsText)
        depsLayout.visibility = View.VISIBLE
        buttonsLayout.visibility = View.VISIBLE
    }

    private fun showDependenciesConfirm(
        deps: List<MainRepository.DependencyInfo>,
        titleTv: TextView,
        depsLayout: LinearLayout,
        buttonsLayout: LinearLayout
    ) {
        titleTv.text = "¡Atención!"
        depsLayout.removeAllViews()
        val warningText = TextView(requireContext()).apply {
            text = "Este registro tiene datos asociados que también se eliminarán:"
            setPadding(0, 0, 0, 16)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
        }
        depsLayout.addView(warningText)

        val totalRecords = deps.sumOf { it.count }
        for (dep in deps) {
            val depText = TextView(requireContext()).apply {
                text = "• ${dep.count} ${dep.displayName}"
                setPadding(16, 0, 0, 8)
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2)
            }
            depsLayout.addView(depText)
        }

        val totalText = TextView(requireContext()).apply {
            text = "\nTotal: $totalRecords registros vinculados"
            setPadding(0, 0, 0, 32)
            gravity = android.view.Gravity.CENTER
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
