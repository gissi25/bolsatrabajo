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
import sv.ues.fia.eisi.bt.utils.getTableDisplayName
import sv.ues.fia.eisi.bt.viewmodel.DashboardItem
import sv.ues.fia.eisi.bt.viewmodel.DashboardViewModel

class DashboardAdapter(
    private val onItemClick: (DashboardItem.Table) -> Unit,
    private val onSectionClick: (String) -> Unit,
    private val onServiceClick: (DashboardItem.WebService) -> Unit = {}
) : ListAdapter<DashboardItem, RecyclerView.ViewHolder>(DashboardDiffCallback()) {

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_TABLE = 1
        private const val TYPE_WEB_SERVICE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DashboardItem.Section -> TYPE_SECTION
            is DashboardItem.Table -> TYPE_TABLE
            is DashboardItem.WebService -> TYPE_WEB_SERVICE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionViewHolder(view)
            }
            TYPE_WEB_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_web_service_card, parent, false) as MaterialCardView
                WebServiceViewHolder(view)
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
            is DashboardItem.WebService -> (holder as WebServiceViewHolder).bind(item)
        }
    }

    private fun sectionKeyToResId(key: String): Int {
        return when (key) {
            DashboardViewModel.SECTION_CATALOGOS -> R.string.section_catalogos
            DashboardViewModel.SECTION_EMPRESA -> R.string.section_empresa
            DashboardViewModel.SECTION_POSTULANTE -> R.string.section_postulante
            DashboardViewModel.SECTION_OTRAS -> R.string.section_otras
            DashboardItem.SECTION_SERVICIOS_WEB -> R.string.section_servicios_web
            else -> R.string.section_otras
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
                    if (item is DashboardItem.Section) onSectionClick(item.sectionKey)
                }
            }
        }

        fun bind(section: DashboardItem.Section) {
            tvSectionTitle.text = itemView.context.getString(sectionKeyToResId(section.sectionKey))
            tvArrow.text = if (section.isExpanded) itemView.context.getString(R.string.arrow_up) else itemView.context.getString(R.string.arrow_down)
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
            tvTableName.text = itemView.context.getTableDisplayName(table.info.name)
            tvRecordCount.text = String.format("%d %s", table.info.count, itemView.context.getString(R.string.records))
            if (table.isReadOnly) {
                tvAccessBadge.visibility = View.VISIBLE
                tvAccessBadge.text = itemView.context.getString(R.string.solo_lectura)
            } else {
                tvAccessBadge.visibility = View.GONE
            }
        }
    }

    inner class WebServiceViewHolder(itemView: MaterialCardView) : RecyclerView.ViewHolder(itemView) {
        private val tvServiceTitle: TextView = itemView.findViewById(R.id.tvServiceTitle)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    if (item is DashboardItem.WebService) {
                        onServiceClick(item)
                    }
                }
            }
        }

        fun bind(service: DashboardItem.WebService) {
            tvServiceTitle.text = service.title
        }
    }

    class DashboardDiffCallback : DiffUtil.ItemCallback<DashboardItem>() {
        override fun areItemsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return when (oldItem) {
                is DashboardItem.Section -> newItem is DashboardItem.Section && oldItem.sectionKey == newItem.sectionKey
                is DashboardItem.Table -> newItem is DashboardItem.Table && oldItem.info.name == newItem.info.name
                is DashboardItem.WebService -> newItem is DashboardItem.WebService && oldItem.id == newItem.id
            }
        }

        override fun areContentsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return oldItem == newItem
        }
    }
}
