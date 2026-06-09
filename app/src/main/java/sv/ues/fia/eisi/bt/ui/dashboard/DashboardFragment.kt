package sv.ues.fia.eisi.bt.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Context
import androidx.core.content.edit
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.LocaleHelper
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.utils.ThemeToggleHelper
import sv.ues.fia.eisi.bt.viewmodel.DashboardItem
import sv.ues.fia.eisi.bt.viewmodel.DashboardViewModel
import sv.ues.fia.eisi.bt.viewmodel.Resource

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var adapter: DashboardAdapter
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnInsertScript: ImageButton
    private lateinit var btnOverflow: ImageButton
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var lastSearchQuery: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionPrefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!sessionPrefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false)) {
            findNavController().navigate(R.id.action_dashboard_to_login)
            return
        }

        recyclerView = view.findViewById(R.id.recyclerTables)
        toolbar = view.findViewById(R.id.toolbar)
        etSearch = view.findViewById(R.id.etSearch)
        btnThemeToggle = view.findViewById(R.id.btnThemeToggle)
        btnInsertScript = view.findViewById(R.id.btnInsertScript)
        btnOverflow = view.findViewById(R.id.btnOverflow)

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val username = prefs.getString(Constants.KEY_USERNAME, "")
        toolbar.title = getString(R.string.welcome_user, username)
        toolbar.post {
            for (i in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(i)
                if (child is TextView && child.text == toolbar.title) {
                    child.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
                    child.marqueeRepeatLimit = -1
                    child.isSingleLine = true
                    child.isSelected = true
                    break
                }
            }
        }

        btnThemeToggle.setImageResource(ThemeToggleHelper.getIconRes(requireContext()))
        btnThemeToggle.setOnClickListener {
            ThemeToggleHelper.toggle(requireActivity())
        }

        btnInsertScript.setImageResource(ThemeToggleHelper.getInsertIconRes(requireContext()))
        val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE) ?: Constants.ROLE_POSTULANTE
        if (role != Constants.ROLE_ADMIN) {
            btnInsertScript.visibility = View.GONE
        }
        btnInsertScript.setOnClickListener {
            showSeedConfirm()
        }

        btnOverflow.setOnClickListener { showOverflowMenu() }

        viewModel.seedResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    StyledToast.show(requireContext(), result.message)
                    viewModel.clearSeedResult()
                }
                is Resource.Error -> {
                    StyledToast.show(requireContext(), result.translatedMessage)
                    viewModel.clearSeedResult()
                }
                null -> {}
            }
        }

        setupSearch()
        setupRecyclerView()

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val currentQuery = etSearch.text?.toString()
            if (items.isEmpty() && currentQuery?.isNotBlank() == true && currentQuery != lastSearchQuery) {
                StyledToast.show(requireContext(), getString(R.string.error_sin_resultados))
            }
            lastSearchQuery = if (items.isNotEmpty()) null else currentQuery
        }

        viewModel.loadTables(role)
    }

    override fun onResume() {
        super.onResume()
        etSearch.setText("")
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE) ?: Constants.ROLE_POSTULANTE
        viewModel.loadTables(role)
    }

    private fun showOverflowMenu() {
        val labels = arrayOf(getString(R.string.idiomas), getString(R.string.logout))
        val icons = intArrayOf(ThemeToggleHelper.getWorldIconRes(requireContext()), ThemeToggleHelper.getLogoutIconRes(requireContext()))

        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(icons[position], 0, 0, 0)
                view.compoundDrawablePadding = 24
                view.setPadding(32, 20, 32, 20)
                return view
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> showLanguageMenu()
                    1 -> showLogoutConfirm()
                }
            }
            .show()
    }

    private fun showLanguageMenu() {
        val languages = arrayOf(
            getString(R.string.espanol) to "es",
            getString(R.string.ingles) to "en",
            getString(R.string.portugues) to "pt"
        )
        val labels = languages.map { it.first }.toTypedArray()
        val currentLang = LocaleHelper.getLanguage(requireContext())

        MaterialAlertDialogBuilder(requireContext())
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

    private fun setupSearch() {
        etSearch.addTextChangedListener { text ->
            searchRunnable?.let { searchHandler.removeCallbacks(it) }
            searchRunnable = Runnable {
                val query = text?.toString() ?: ""
                if (query.isEmpty()) {
                    viewModel.loadOriginalTables()
                } else {
                    viewModel.filterTables(query)
                }
            }
            searchHandler.postDelayed(searchRunnable!!, 300)
        }
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter(
            onItemClick = { tableItem ->
                val bundle = Bundle().apply {
                    putString(Constants.BUNDLE_TABLE_NAME, tableItem.info.name)
                }
                findNavController().navigate(R.id.action_dashboard_to_tableDetail, bundle)
            },
            onSectionClick = { title ->
                viewModel.toggleSection(title)
            },
                    onServiceClick = { service ->
                when (service.id) {
                    1 -> findNavController().navigate(R.id.action_dashboard_to_bulkOferta)
                    2 -> findNavController().navigate(R.id.action_dashboard_to_servicio2)
                    3 -> findNavController().navigate(R.id.action_dashboard_to_servicio3)
                    4 -> findNavController().navigate(R.id.action_dashboard_to_servicio4)
                    5 -> findNavController().navigate(R.id.action_dashboard_to_servicio5)
                    6 -> findNavController().navigate(R.id.action_dashboard_to_servicio6)
                    7 -> findNavController().navigate(R.id.action_dashboard_to_servicio7)
                    8 -> findNavController().navigate(R.id.action_dashboard_to_servicio8)
                    9 -> findNavController().navigate(R.id.action_dashboard_to_servicio9)
                    10 -> findNavController().navigate(R.id.action_dashboard_to_servicio10)
                }
            }
        )

        val glm = GridLayoutManager(requireContext(), 2)
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.currentList.getOrNull(position)) {
                    is DashboardItem.Section -> 2
                    is DashboardItem.Table -> 1
                    is DashboardItem.WebService -> 1
                    null -> 1
                }
            }
        }
        recyclerView.layoutManager = glm
        recyclerView.adapter = adapter
    }

    private fun showLogoutConfirm() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit { clear() }
                findNavController().navigate(R.id.action_dashboard_to_login)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showSeedConfirm() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seed_confirm_title)
            .setMessage(R.string.seed_confirm_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.insertSeedData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
