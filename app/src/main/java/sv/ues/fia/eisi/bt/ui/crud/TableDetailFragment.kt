package sv.ues.fia.eisi.bt.ui.crud

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import sv.ues.fia.eisi.bt.utils.getTableDisplayName
import sv.ues.fia.eisi.bt.utils.LocaleHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sv.ues.fia.eisi.bt.utils.CVExportUtil
import sv.ues.fia.eisi.bt.utils.OfertaFullData
import sv.ues.fia.eisi.bt.utils.PostulantFullData
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.ThemeToggleHelper
import sv.ues.fia.eisi.bt.utils.removeAccents
import sv.ues.fia.eisi.bt.viewmodel.CrudViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TableDetailFragment : Fragment() {

    private val viewModel: CrudViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabExport: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var adapter: TableAdapter

    private var tableName: String = ""
    private var allItems: List<List<Any>> = emptyList()
    private var canEdit: Boolean = false
    private var canDelete: Boolean = false
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var lastTableQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tableName = arguments?.getString(Constants.BUNDLE_TABLE_NAME) ?: ""
        canEdit = arguments?.getBoolean(Constants.BUNDLE_CAN_EDIT) ?: false
        canDelete = arguments?.getBoolean(Constants.BUNDLE_CAN_DELETE) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_table_detail, container, false)
        
        // Sincronizando con los IDs del layout XML
        recyclerView = view.findViewById(R.id.recyclerItems)
        searchView = view.findViewById(R.id.searchView)
        toolbar = view.findViewById(R.id.toolbar)
        fabAdd = view.findViewById(R.id.fabAdd)
        fabExport = view.findViewById(R.id.fabExport)
        progressBar = view.findViewById(R.id.progressBar)
        btnThemeToggle = view.findViewById(R.id.btnThemeToggle)
        btnLanguage = view.findViewById(R.id.btnLanguage)

        setupToolbar()
        setupSearchView()
        setupFab()

        btnThemeToggle.setImageResource(ThemeToggleHelper.getIconRes(requireContext()))
        btnThemeToggle.setOnClickListener {
            ThemeToggleHelper.toggle(requireActivity())
        }
        btnLanguage.setImageResource(ThemeToggleHelper.getWorldIconRes(requireContext()))
        btnLanguage.setOnClickListener { showLanguageMenu() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(
            Constants.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE)
            ?: Constants.ROLE_POSTULANTE
        val access = Constants.getRoleTables(role)[tableName] ?: Constants.AccessLevel.NONE
        if (access == Constants.AccessLevel.NONE) {
            StyledToast.show(requireContext(), getString(R.string.error_sin_acceso_tabla))
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }
        canEdit = access == Constants.AccessLevel.FULL
        canDelete = access == Constants.AccessLevel.FULL

        if (!canEdit) fabAdd.visibility = View.GONE
        if (role == Constants.ROLE_EMPRESA && tableName == "POSTULACION") {
            fabAdd.visibility = View.GONE
        }

        if (tableName == "POSTULACION") {
            fabExport.visibility = View.VISIBLE
            fabExport.isEnabled = false
            fabExport.alpha = 0.4f
        }

        setupRecyclerView()

        viewModel.setTable(tableName)

        viewModel.items.observe(viewLifecycleOwner) { items ->
            allItems = items
            adapter.submitList(items)
            progressBar.visibility = View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupToolbar() {
        toolbar.title = requireContext().getTableDisplayName(tableName)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val onItemSelected: ((List<Any>, Int) -> Unit)? = if (tableName == "POSTULACION") {
            { item, position ->
                if (position < 0 || item.isEmpty()) {
                    fabExport.isEnabled = false
                    fabExport.alpha = 0.4f
                } else {
                    fabExport.isEnabled = true
                    fabExport.alpha = 1.0f
                }
            }
        } else null

        adapter = TableAdapter(
            tableName = tableName,
            canEdit = canEdit,
            canDelete = canDelete,
            onEditClick = { item, position ->
                showEditDialog(item, true)
            },
            onDeleteClick = { item, position ->
                showDeleteDialog(item, position)
            },
            onViewClick = if (!canEdit) { item, _ ->
                showViewDialog(item)
            } else null,
            onItemSelected = onItemSelected
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                query?.let { filterItems(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    if (newText.isNullOrBlank()) {
                        adapter.submitList(allItems)
                    } else {
                        filterItems(newText)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
                return true
            }
        })
    }

    private fun filterItems(query: String) {
        val q = query.removeAccents()
        if (q.isBlank()) {
            adapter.submitList(allItems)
            return
        }

        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val hoy = df.format(Date())

        val filtered = allItems.filter { item ->
            val matchesNormal = item.any { value ->
                value.toString().removeAccents().contains(q, ignoreCase = true)
            }

            if (matchesNormal) {
                true
            } else if (tableName == "OFERTA_TRABAJO") {
                val fechaCad = item.getOrNull(5)?.toString() ?: ""
                when {
                    q.contains("vigente", ignoreCase = true) ->
                        fechaCad.isNotBlank() && fechaCad >= hoy
                    q.contains("vencida", ignoreCase = true) ->
                        fechaCad.isNotBlank() && fechaCad <= hoy
                    else -> false
                }
            } else if (tableName == "DETALLE_REQUISITO") {
                val fechaCad = item.getOrNull(6)?.toString() ?: ""
                when {
                    q.contains("vigente", ignoreCase = true) ->
                        fechaCad.isNotBlank() && fechaCad >= hoy
                    q.contains("vencido", ignoreCase = true) ->
                        fechaCad.isNotBlank() && fechaCad <= hoy
                    else -> false
                }
            } else if (tableName == "POSTULACION") {
                val estado = item.getOrNull(5)?.toString() ?: ""
                when {
                    q.contains("activo", ignoreCase = true) ->
                        estado.contains("activo", ignoreCase = true)
                    q.contains("en proceso", ignoreCase = true) ->
                        estado.contains("en proceso", ignoreCase = true)
                    q.contains("contratado", ignoreCase = true) ->
                        estado.contains("contratado", ignoreCase = true)
                    q.contains("rechazado", ignoreCase = true) ->
                        estado.contains("rechazado", ignoreCase = true)
                    else -> false
                }
            } else {
                false
            }
        }
        adapter.submitList(filtered)

        val currentQ = query.trim()
        if (filtered.isEmpty() && currentQ.isNotBlank() && currentQ != lastTableQuery) {
            StyledToast.show(requireContext(), getString(R.string.error_sin_resultados))
        }
        lastTableQuery = if (filtered.isNotEmpty()) null else currentQ
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            showEditDialog(emptyList(), false)
        }

        fabExport.setOnClickListener {
            val selectedItem = adapter.getSelectedItem() ?: return@setOnClickListener
            exportPdfs(selectedItem)
        }
    }

    private fun exportPdfs(item: List<Any>) {
        val idPostulante = item.getOrNull(3)?.toString()?.trim() ?: ""
        val nit = item.getOrNull(1)?.toString()?.trim() ?: ""
        val idOferta = item.getOrNull(2)?.toString()?.trim() ?: ""

        if (idPostulante.isBlank() || nit.isBlank() || idOferta.isBlank()) {
            StyledToast.show(requireContext(), getString(R.string.error_datos_incompletos))
            return
        }

        val progressLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 50, 80, 50)
            gravity = Gravity.CENTER
            addView(ProgressBar(requireContext(), null, android.R.attr.progressBarStyle).apply {
                isIndeterminate = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER }
            })
        }
        val tvMsg = TextView(requireContext()).apply {
            text = getString(R.string.pdf_generando_cv)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 32 }
        }
        progressLayout.addView(tvMsg)
        val loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.pdf_titulo))
            .setView(progressLayout)
            .setCancelable(false)
            .show()

        Thread {
            val postulantData: PostulantFullData? = viewModel.getPostulantFullData(idPostulante)
            val ofertaData: OfertaFullData? = viewModel.getOfertaFullData(nit, idOferta)

            if (postulantData == null || ofertaData == null) {
                Handler(Looper.getMainLooper()).post {
                    loadingDialog.dismiss()
                    StyledToast.show(requireContext(), getString(R.string.error_obtener_datos_pdf))
                }
                return@Thread
            }

            Handler(Looper.getMainLooper()).postDelayed({
                tvMsg.text = getString(R.string.generando_pdfs)
            }, 1500)

            try {
                val cvFile = CVExportUtil.generateCVPdf(
                    requireContext(), postulantData,
                    "CV_${postulantData.idPostulante}.pdf"
                )
                val ofertaFile = CVExportUtil.generateOfertaPdf(
                    requireContext(), ofertaData,
                    "Vacante_${nit}_${idOferta}.pdf"
                )

                Handler(Looper.getMainLooper()).post {
                    loadingDialog.dismiss()
                    val cvUri = CVExportUtil.getPdfUri(requireContext(), cvFile)
                    val ofertaUri = CVExportUtil.getPdfUri(requireContext(), ofertaFile)

                    // DISEÑO MODERNO DEL MODAL DE RESULTADOS
                    val resultLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(70, 60, 70, 50)
                        gravity = Gravity.CENTER_HORIZONTAL

                        // Título llamativo
                        addView(TextView(requireContext()).apply {
                            text = getString(R.string.pdf_listos)
                            textSize = 22f
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            setTextColor(requireContext().getColor(R.color.text_primary))
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 20 }
                        })

                        // Cuerpo descriptivo
                        addView(TextView(requireContext()).apply {
                            text = getString(R.string.pdf_descripcion)
                            textSize = 15f
                            gravity = Gravity.CENTER
                            setLineSpacing(0f, 1.2f)
                            setTextColor(requireContext().getColor(R.color.text_secondary))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 45 }
                        })

                        // Botón principal: VER VACANTE (Relleno)
                        val btnVacante = MaterialButton(requireContext()).apply {
                            text = getString(R.string.pdf_ver_vacante)
                            backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
                            setTextColor(requireContext().getColor(R.color.on_primary))
                            cornerRadius = 28
                            setPadding(0, 35, 0, 35)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 16 }
                        }
                        addView(btnVacante)

                        // Botón secundario: VER CV (Relleno)
                        val btnCV = MaterialButton(requireContext()).apply {
                            text = getString(R.string.pdf_ver_cv)
                            backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
                            setTextColor(requireContext().getColor(R.color.on_primary))
                            cornerRadius = 28
                            setPadding(0, 35, 0, 35)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = 16 }
                        }
                        addView(btnCV)

                        // Botón Cerrar: Estilo Texto/Chip (Más pequeño)
                        val btnCerrar = MaterialButton(requireContext(), null).apply {
                            text = getString(R.string.cerrar)
                            isAllCaps = false
                            textSize = 14f
                            setTextColor(requireContext().getColor(R.color.text_secondary))
                            backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                            elevation = 0f
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = 30 }
                        }
                        addView(btnCerrar)

                        val finalDialog = MaterialAlertDialogBuilder(requireContext())
                            .setView(this)
                            .create()

                        btnVacante.setOnClickListener {
                            try {
                                requireActivity().startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(ofertaUri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                StyledToast.show(requireContext(), getString(R.string.error_sin_visor_pdf))
                            }
                        }

                        btnCV.setOnClickListener {
                            try {
                                requireActivity().startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(cvUri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                StyledToast.show(requireContext(), getString(R.string.error_sin_visor_pdf))
                            }
                        }

                        btnCerrar.setOnClickListener { finalDialog.dismiss() }

                        finalDialog.setOnDismissListener {
                            adapter.selectedPosition = -1
                            adapter.notifyDataSetChanged()
                            fabExport.isEnabled = false
                            fabExport.alpha = 0.4f
                        }

                        finalDialog.show()
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    loadingDialog.dismiss()
                    StyledToast.show(requireContext(), getString(R.string.error_generar_pdf, e.message ?: ""))
                }
            }
        }.start()
    }

    private fun showEditDialog(itemData: List<Any>, isEditMode: Boolean) {
        if (!canEdit) {
            StyledToast.show(requireContext(), getString(R.string.error_sin_permiso_editar))
            return
        }
        val dialog = EditorDialogFragment()
        val bundle = Bundle().apply {
            putString(Constants.BUNDLE_TABLE_NAME, tableName)
            putBoolean(Constants.BUNDLE_IS_EDIT_MODE, isEditMode)
            putString(Constants.BUNDLE_TABLE_DATA, itemData.joinToString(","))
        }
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "editor")
    }

    private fun showDeleteDialog(itemData: List<Any>, position: Int) {
        if (!canDelete) {
            StyledToast.show(
                requireContext(),
                getString(R.string.error_sin_permiso_eliminar)
            )
            return
        }
        val dialog = DeleteConfirmDialog()
        val bundle = Bundle().apply {
            putString("itemData", itemData.joinToString(","))
            putInt("position", position)
            putString(Constants.BUNDLE_TABLE_NAME, tableName)
        }
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "delete_confirm")
    }

    private fun showViewDialog(itemData: List<Any>) {
        val dialog = EditorDialogFragment()
        val bundle = Bundle().apply {
            putString(Constants.BUNDLE_TABLE_NAME, tableName)
            putBoolean(Constants.BUNDLE_IS_VIEW_MODE, true)
            putString(Constants.BUNDLE_TABLE_DATA, itemData.joinToString(","))
        }
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "editor")
    }

    private fun showLanguageMenu() {
        val languages = arrayOf(
            getString(R.string.espanol) to "es",
            getString(R.string.ingles) to "en",
            getString(R.string.portugues) to "pt"
        )
        val labels = languages.map { it.first }.toTypedArray()
        val currentLang = LocaleHelper.getLanguage(requireContext())

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.idiomas))
            .setItems(labels) { _, which ->
                val lang = languages[which].second
                if (lang != currentLang) {
                    LocaleHelper.setLocale(requireContext(), lang)
                    requireActivity().recreate()
                }
            }
            .show()
    }
}
