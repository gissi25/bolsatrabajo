package sv.ues.fia.eisi.bt.ui.crud

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sv.ues.fia.eisi.bt.R

class TableAdapter(
    private val tableName: String,
    private val onEditClick: (List<Any>, Int) -> Unit,
    private val onDeleteClick: (List<Any>, Int) -> Unit
) : RecyclerView.Adapter<TableAdapter.ViewHolder>() {

    private var items: List<List<Any>> = emptyList()

    fun submitList(newItems: List<List<Any>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_table_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val tvId: TextView = itemView.findViewById(R.id.tvId)
        private val tvPrimary: TextView = itemView.findViewById(R.id.tvPrimary)
        private val tvSecondary: TextView = itemView.findViewById(R.id.tvSecondary)
        private val fabEdit: FloatingActionButton = itemView.findViewById(R.id.fabEdit)
        private val fabDelete: FloatingActionButton = itemView.findViewById(R.id.fabDelete)

        fun bind(item: List<Any>, position: Int) {
            val field0 = getStringSafely(item, 0)

            if (tableName == "POSTULANTE") {
                // Índices corregidos según getColumnsForTable: 5=NOMBRE, 6=APELLIDO
                val nombre = getStringSafely(item, 5)
                val apellido = getStringSafely(item, 6)
                tvId.text = "ID: $field0"
                tvPrimary.text = nombre.ifBlank { "(sin nombre)" }
                tvSecondary.text = apellido.ifBlank { "(sin apellido)" }
            } else if (tableName == "EXPERIENCIA_LABORAL") {
                val nombre = getStringSafely(item, 8)
                val apellido = getStringSafely(item, 9)
                val puesto = getStringSafely(item, 3)
                tvId.text = "ID: $field0"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = puesto.ifBlank { "(sin puesto)" }
            } else if (tableName == "HABILIDAD_POSTULANTE") {
                val nombre = getStringSafely(item, 5)
                val apellido = getStringSafely(item, 6)
                val habilidad = getStringSafely(item, 7)
                val nivelRaw = getStringSafely(item, 4)
                
                val nivelText = when(nivelRaw) {
                    "1" -> "Básico"
                    "2" -> "Intermedio"
                    "3" -> "Avanzado"
                    else -> nivelRaw
                }

                tvId.text = "ID: ${getStringSafely(item, 3)}"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = if (nivelText.isNotBlank()) "$habilidad • $nivelText" else habilidad
            } else if (tableName == "POSTULACION") {
                val nombre = getStringSafely(item, 6)
                val apellido = getStringSafely(item, 7)
                val puesto = getStringSafely(item, 8)
                tvId.text = "ID: ${getStringSafely(item, 3)}"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = puesto.ifBlank { "(sin puesto)" }
            } else if (tableName == "RED_SOCIAL_POSTULANTE") {
                val nombre = getStringSafely(item, 4)
                val apellido = getStringSafely(item, 5)
                val redSocial = getStringSafely(item, 6)
                tvId.text = "ID: ${getStringSafely(item, 0)}"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = redSocial.ifBlank { "(sin red social)" }
            } else if (tableName == "OFERTA_TRABAJO") {
                val titulo = getStringSafely(item, 3)
                val empresa = getStringSafely(item, 10)
                tvId.text = "ID: ${getStringSafely(item, 1)}"
                tvPrimary.text = titulo.ifBlank { "(sin título)" }
                tvSecondary.text = empresa.ifBlank { "(sin empresa)" }
            } else if (tableName == "DETALLE_REQUISITO") {
                val descripcion = getStringSafely(item, 3)
                val titulo = getStringSafely(item, 4)
                tvId.text = "ID: ${getStringSafely(item, 0)}"
                tvPrimary.text = descripcion.ifBlank { "(sin descripción)" }
                tvSecondary.text = titulo.ifBlank { "(sin puesto)" }
            } else if (tableName == "OFERTA_ACADEMICA") {
                val grado = getStringSafely(item, 4)
                val institucion = getStringSafely(item, 3)
                tvId.text = "ID: ${getStringSafely(item, 0)}"
                tvPrimary.text = grado.ifBlank { "(sin grado)" }
                tvSecondary.text = institucion.ifBlank { "(sin institución)" }
            } else if (tableName == "CERTIFICACION") {
                val nombre = getStringSafely(item, 3)
                val postNombre = getStringSafely(item, 6)
                val postApellido = getStringSafely(item, 7)
                tvId.text = "ID: ${getStringSafely(item, 1)}"
                tvPrimary.text = nombre.ifBlank { "(sin certificación)" }
                tvSecondary.text = "$postNombre $postApellido".trim().ifBlank { "(sin postulante)" }
            } else if (tableName == "FORMACION_ACADEMICA") {
                val titulo = getStringSafely(item, 3)
                val postNombre = getStringSafely(item, 5)
                val postApellido = getStringSafely(item, 6)
                tvId.text = "ID: ${getStringSafely(item, 0)}"
                tvPrimary.text = titulo.ifBlank { "(sin título)" }
                tvSecondary.text = "$postNombre $postApellido".trim().ifBlank { "(sin postulante)" }
            } else if (tableName == "EMPRESA") {
                val nombreEmpresa = getStringSafely(item, 2)
                val contactoDirecto = getStringSafely(item, 3)
                tvId.text = "ID: $field0"
                tvPrimary.text = nombreEmpresa.ifBlank { "(sin nombre)" }
                tvSecondary.text = contactoDirecto.ifBlank { "(sin contacto)" }
            } else {
                val field1 = getStringSafely(item, 1)
                val field2 = getStringSafely(item, 2)
                tvId.text = "ID: $field0"
                tvPrimary.text = field1.ifBlank { field0.ifBlank { "(vacío)" } }
                tvSecondary.text = field2
            }

            fabEdit.setOnClickListener { onEditClick(item, position) }
            fabDelete.setOnClickListener { onDeleteClick(item, position) }
        }

        private fun getStringSafely(list: List<Any>, index: Int): String {
            return try {
                list.getOrNull(index)?.toString()?.trim() ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    class RowDiffCallback : DiffUtil.ItemCallback<List<Any>>() {
        override fun areItemsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
            return oldItem == newItem
        }
    }
}
