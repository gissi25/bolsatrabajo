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
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Observer
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.utils.getTableDisplayName
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

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
            text = getString(R.string.eliminar_tabla, context.getTableDisplayName(tableName))
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
            text = context.getString(R.string.consultando_dependencias)
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
            text = context.getString(R.string.cancel)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 16
            }
            setOnClickListener { dismiss() }
        }
        buttonsLayout.addView(btnCancel)

        val btnDelete = MaterialButton(context).apply {
            text = context.getString(R.string.delete)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor("#D32F2F".toColorInt())
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
                showCannotDeleteDialog(deps, titleTv, depsLayout, buttonsLayout)
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
            val needsComposite = tableName in listOf(
                "MUNICIPIO", "DISTRITO",
                "OFERTA_TRABAJO", "DETALLE_REQUISITO",
                "EXPERIENCIA_LABORAL", "CERTIFICACION",
                "FORMACION_ACADEMICA", "HABILIDAD_POSTULANTE",
                "RED_SOCIAL_POSTULANTE", "HABILIDAD", "OFERTA_ACADEMICA"
            )
            if (needsComposite) {
                val pkChunks = when (tableName) {
                    "MUNICIPIO" -> 2; "DISTRITO" -> 3; "OFERTA_TRABAJO" -> 2
                    "DETALLE_REQUISITO" -> 3; "EXPERIENCIA_LABORAL" -> 3
                    "CERTIFICACION" -> 3; "FORMACION_ACADEMICA" -> 2
                    "HABILIDAD_POSTULANTE" -> 3; "RED_SOCIAL_POSTULANTE" -> 2
                    "HABILIDAD" -> 2; "OFERTA_ACADEMICA" -> 1
                    else -> 1
                }
                val pkString = (0 until pkChunks).joinToString("|") { dataList.getOrElse(it) { "" }.trim() }
                viewModel.checkDeleteDependencies(pkString)
            } else {
                viewModel.checkDeleteDependencies(dataList.first().trim())
            }
        }

        return layout
    }

    private fun showSimpleConfirm(
        titleTv: TextView,
        depsLayout: LinearLayout,
        buttonsLayout: LinearLayout
    ) {
        titleTv.text = getString(R.string.confirmar_eliminar)
        depsLayout.removeAllViews()
        val noDepsText = TextView(requireContext()).apply {
            text = getString(R.string.sin_dependencias)
            setTextColor(requireContext().getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 32)
            gravity = android.view.Gravity.CENTER
        }
        depsLayout.addView(noDepsText)
        depsLayout.visibility = View.VISIBLE

        buttonsLayout.removeAllViews()
        val btnCancel = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.cerrar)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 16
            }
            setOnClickListener { dismiss() }
        }
        buttonsLayout.addView(btnCancel)
        val btnDelete = MaterialButton(requireContext()).apply {
            text = getString(R.string.delete)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor("#D32F2F".toColorInt())
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { performDelete() }
        }
        buttonsLayout.addView(btnDelete)
        buttonsLayout.visibility = View.VISIBLE
    }

    private fun showCannotDeleteDialog(
        @Suppress("UNUSED_PARAMETER") deps: List<MainRepository.DependencyInfo>,
        titleTv: TextView,
        depsLayout: LinearLayout,
        buttonsLayout: LinearLayout
    ) {
        titleTv.text = getString(R.string.no_se_puede_eliminar)
        depsLayout.removeAllViews()
        val msgText = TextView(requireContext()).apply {
            text = getString(R.string.dependencias_en_cadena)
            setPadding(0, 0, 0, 32)
            setTextColor(requireContext().getColor(R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
        }
        depsLayout.addView(msgText)
        depsLayout.visibility = View.VISIBLE

        buttonsLayout.removeAllViews()
        val btnClose = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.cerrar)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { dismiss() }
        }
        buttonsLayout.addView(btnClose)
        buttonsLayout.visibility = View.VISIBLE
    }

    private fun performDelete() {
        if (isLoading) return

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE) ?: Constants.ROLE_POSTULANTE
        val access = Constants.getRoleTables(role)[tableName] ?: Constants.AccessLevel.NONE
        if (access != Constants.AccessLevel.FULL) {
            StyledToast.show(requireContext(), getString(R.string.sin_permiso_eliminar))
            dismiss()
            return
        }

        val dataList = itemData.split(",").map { it.trim() }
        val idToDelete = dataList.firstOrNull()

        if (tableName == Constants.TABLE_USUARIO && idToDelete != null) {
            val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val activeUserId = prefs.getInt(Constants.KEY_USER_ID, -1)
            if (idToDelete == activeUserId.toString()) {
                StyledToast.show(requireContext(), getString(R.string.no_eliminar_propio_usuario))
                dismiss()
                return
            }
        }

        isLoading = true

        val needsComposite = tableName in listOf(
            "MUNICIPIO", "DISTRITO",
            "OFERTA_TRABAJO", "DETALLE_REQUISITO",
            "EXPERIENCIA_LABORAL", "CERTIFICACION",
            "FORMACION_ACADEMICA", "HABILIDAD_POSTULANTE",
            "RED_SOCIAL_POSTULANTE", "HABILIDAD"
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
