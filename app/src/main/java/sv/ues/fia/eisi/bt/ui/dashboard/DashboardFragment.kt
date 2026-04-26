package sv.ues.fia.eisi.bt.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.viewmodel.DashboardViewModel

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var adapter: DashboardAdapter
    private lateinit var etSearch: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerTables)
        toolbar = view.findViewById(R.id.toolbar)
        etSearch = view.findViewById(R.id.etSearch)

        setupToolbar()
        setupSearch()
        setupRecyclerView()

        viewModel.tables.observe(viewLifecycleOwner) { tables ->
            adapter.submitList(tables)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCounts()
    }

    private fun setupToolbar() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val username = prefs.getString(Constants.KEY_USERNAME, "")
        
        toolbar.title = getString(R.string.welcome_user, username)
        
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_dark_mode -> {
                    toggleDarkMode()
                    true
                }
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleDarkMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
        requireActivity().recreate()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            if (query.isEmpty()) {
                viewModel.loadOriginalTables()
            } else {
                viewModel.filterTables(query)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter { table ->
            val bundle = Bundle().apply {
                putString(Constants.BUNDLE_TABLE_NAME, table.name)
                putString("tableDisplayName", table.displayName)
            }
            findNavController().navigate(R.id.action_dashboard_to_tableDetail, bundle)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    private fun logout() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                findNavController().navigate(R.id.action_dashboard_to_login)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}