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
import sv.ues.fia.eisi.bt.data.repository.MainRepository

class DashboardAdapter(
    private val onItemClick: (MainRepository.TableInfo) -> Unit
) : ListAdapter<MainRepository.TableInfo, DashboardAdapter.ViewHolder>(TableDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_table_card, parent, false) as MaterialCardView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: MaterialCardView) : RecyclerView.ViewHolder(itemView) {
        private val tvTableName: TextView = itemView.findViewById(R.id.tvTableName)
        private val tvRecordCount: TextView = itemView.findViewById(R.id.tvRecordCount)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(getItem(pos))
            }
        }

        fun bind(table: MainRepository.TableInfo) {
            tvTableName.text = table.displayName
            tvRecordCount.text = "${table.count} ${itemView.context.getString(R.string.records)}"
        }
    }

    class TableDiffCallback : DiffUtil.ItemCallback<MainRepository.TableInfo>() {
        override fun areItemsTheSame(oldItem: MainRepository.TableInfo, newItem: MainRepository.TableInfo): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: MainRepository.TableInfo, newItem: MainRepository.TableInfo): Boolean {
            return oldItem == newItem
        }
    }
}