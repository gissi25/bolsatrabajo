package sv.ues.fia.eisi.bt.ui.crud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.viewmodel.CrudViewModel

class TableDetailFragment : Fragment() {

    private val viewModel: CrudViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: TableAdapter

    private var tableName: String = ""
    private var tableDisplayName: String = ""
    private var allItems: List<List<Any>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tableName = it.getString(Constants.BUNDLE_TABLE_NAME, "")
            tableDisplayName = it.getString("tableDisplayName", tableName)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_table_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerItems)
        searchView = view.findViewById(R.id.searchView)
        toolbar = view.findViewById(R.id.toolbar)
        fabAdd = view.findViewById(R.id.fabAdd)
        progressBar = view.findViewById(R.id.progressBar)

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupFab()

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
        toolbar.title = tableDisplayName
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = TableAdapter(
            tableName = tableName,
            onEditClick = { item, position ->
                showEditDialog(item, true)
            },
            onDeleteClick = { item, position ->
                showDeleteDialog(item, position)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterItems(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    adapter.submitList(allItems)
                } else {
                    filterItems(newText)
                }
                return true
            }
        })
    }

    private fun filterItems(query: String) {
        if (query.isBlank()) {
            adapter.submitList(allItems)
            return
        }

        val filtered = allItems.filter { item ->
            item.any { value ->
                value.toString().contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
        
        if (filtered.isEmpty()) {
            StyledToast.show(requireContext(), "Sin resultados")
        }
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            showEditDialog(emptyList(), false)
        }
    }

    private fun showEditDialog(itemData: List<Any>, isEditMode: Boolean) {
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
        val dialog = DeleteConfirmDialog()
        val bundle = Bundle().apply {
            putString("itemData", itemData.joinToString(","))
            putInt("position", position)
            putString(Constants.BUNDLE_TABLE_NAME, tableName)
        }
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "delete_confirm")
    }
}