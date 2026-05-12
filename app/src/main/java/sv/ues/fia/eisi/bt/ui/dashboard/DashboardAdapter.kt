package sv.ues.fia.eisi.bt.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.viewmodel.DashboardItem

class DashboardAdapter(
    private val onItemClick: (DashboardItem.Table) -> Unit,
    private val onSectionClick: (String) -> Unit
) : ListAdapter<DashboardItem, RecyclerView.ViewHolder>(DashboardDiffCallback()) {

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_TABLE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DashboardItem.Section -> TYPE_SECTION
            is DashboardItem.Table -> TYPE_TABLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_table_card, parent, false) as MaterialCardView
                TableViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DashboardItem.Section -> (holder as SectionViewHolder).bind(item)
            is DashboardItem.Table -> (holder as TableViewHolder).bind(item)
        }
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionTitle: TextView = itemView.findViewById(R.id.tvSectionTitle)
        private val tvArrow: TextView = itemView.findViewById(R.id.tvArrow)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    if (item is DashboardItem.Section) onSectionClick(item.title)
                }
            }
        }

        fun bind(section: DashboardItem.Section) {
            tvSectionTitle.text = section.title
            tvArrow.text = if (section.isExpanded) "▲" else "▼"
        }
    }

    inner class TableViewHolder(itemView: MaterialCardView) : RecyclerView.ViewHolder(itemView) {
        private val tvTableName: TextView = itemView.findViewById(R.id.tvTableName)
        private val tvRecordCount: TextView = itemView.findViewById(R.id.tvRecordCount)
        private val tvAccessBadge: TextView = itemView.findViewById(R.id.tvAccessBadge)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    if (item is DashboardItem.Table) onItemClick(item)
                }
            }
        }

        fun bind(table: DashboardItem.Table) {
            tvTableName.text = table.info.displayName
            tvRecordCount.text = "${table.info.count} ${itemView.context.getString(R.string.records)}"
            if (table.isReadOnly) {
                tvAccessBadge.visibility = View.VISIBLE
                tvAccessBadge.text = "Solo lectura"
            } else {
                tvAccessBadge.visibility = View.GONE
            }
        }
    }

    class DashboardDiffCallback : DiffUtil.ItemCallback<DashboardItem>() {
        override fun areItemsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return when {
                oldItem is DashboardItem.Section && newItem is DashboardItem.Section ->
                    oldItem.title == newItem.title
                oldItem is DashboardItem.Table && newItem is DashboardItem.Table ->
                    oldItem.info.name == newItem.info.name
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return oldItem == newItem
        }
    }
}
