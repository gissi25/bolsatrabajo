package sv.ues.fia.eisi.bt.ui.crud

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.viewmodel.CrudViewModel

class DeleteConfirmDialog : DialogFragment() {

    private val viewModel: CrudViewModel by viewModels({ requireParentFragment() })
    private var itemData: String = ""
    private var tableName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.AestheticDialog)
        arguments?.let {
            itemData = it.getString("itemData", "")
            tableName = it.getString(Constants.BUNDLE_TABLE_NAME, "")
        }
    }

    override fun onStart() {
        super.onStart()
        showDeleteConfirmation()
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_warning)
            .setPositiveButton(R.string.delete) { _, _ -> deleteItem() }
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .setCancelable(true)
            .show()
    }

    private fun deleteItem() {
        val dataList = itemData.split(",")
        if (dataList.isNotEmpty()) {
            val id = dataList.first().trim()
            viewModel.deleteItem(id)
            StyledToast.show(requireContext(), "Eliminado")
        }
        dismiss()
    }
}