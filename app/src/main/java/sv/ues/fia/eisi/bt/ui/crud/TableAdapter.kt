package sv.ues.fia.eisi.bt.ui.crud

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sv.ues.fia.eisi.bt.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TableAdapter(
    private val tableName: String,
    private val canEdit: Boolean = true,
    private val canDelete: Boolean = true,
    private val onEditClick: (List<Any>, Int) -> Unit,
    private val onDeleteClick: (List<Any>, Int) -> Unit,
    private val onViewClick: ((List<Any>, Int) -> Unit)? = null
) : ListAdapter<List<Any>, TableAdapter.ViewHolder>(RowDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_table_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvId: TextView = itemView.findViewById(R.id.tvId)
        private val tvPrimary: TextView = itemView.findViewById(R.id.tvPrimary)
        private val tvSecondary: TextView = itemView.findViewById(R.id.tvSecondary)
        private val chipEstado: TextView = itemView.findViewById(R.id.chipEstado)
        private val fabEdit: FloatingActionButton = itemView.findViewById(R.id.fabEdit)
        private val fabDelete: FloatingActionButton = itemView.findViewById(R.id.fabDelete)

        init {
            fabEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
            fabDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
            fabEdit.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onEditClick(getItem(pos), pos)
            }
            fabDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onDeleteClick(getItem(pos), pos)
            }
            if (!canEdit && onViewClick != null) {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onViewClick(getItem(pos), pos)
                }
            }
        }

        fun bind(item: List<Any>, position: Int) {
            val field0 = getStringSafely(item, 0)
            chipEstado.visibility = View.GONE

            if (tableName == "POSTULANTE") {
                val nombre = getStringSafely(item, 8)
                val apellido = getStringSafely(item, 9)
                tvId.text = field0
                tvPrimary.text = nombre.ifBlank { "(sin nombre)" }
                tvSecondary.text = apellido.ifBlank { "(sin apellido)" }
            } else if (tableName == "EXPERIENCIA_LABORAL") {
                val nombre = getStringSafely(item, 8)
                val apellido = getStringSafely(item, 9)
                val puesto = getStringSafely(item, 3)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)}, ${getStringSafely(item, 2)})"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = puesto.ifBlank { "(sin puesto)" }
            } else if (tableName == "HABILIDAD_POSTULANTE") {
                val nombre = getStringSafely(item, 4)
                val apellido = getStringSafely(item, 5)
                val habilidad = getStringSafely(item, 6)
                val nivelText = getStringSafely(item, 3)

                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)}, ${getStringSafely(item, 2)})"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = if (nivelText.isNotBlank()) "$habilidad • $nivelText" else habilidad
            } else if (tableName == "POSTULACION") {
                val nombre = getStringSafely(item, 6)
                val apellido = getStringSafely(item, 7)
                val puesto = getStringSafely(item, 8)
                tvId.text = field0
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = puesto.ifBlank { "(sin puesto)" }
            } else if (tableName == "RED_SOCIAL_POSTULANTE") {
                val nombre = getStringSafely(item, 3)
                val apellido = getStringSafely(item, 4)
                val redSocial = getStringSafely(item, 5)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)})"
                tvPrimary.text = "$nombre $apellido".trim().ifBlank { "(sin nombre)" }
                tvSecondary.text = redSocial.ifBlank { "(sin red social)" }
            } else if (tableName == "MUNICIPIO") {
                val deptoNombre = getStringSafely(item, 3)
                val munNombre = getStringSafely(item, 2)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)})"
                tvPrimary.text = deptoNombre.ifBlank { "(sin departamento)" }
                tvSecondary.text = munNombre.ifBlank { "(sin municipio)" }
            } else if (tableName == "DISTRITO") {
                val munNombre = getStringSafely(item, 4)
                val distNombre = getStringSafely(item, 3)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)}, ${getStringSafely(item, 2)})"
                tvPrimary.text = munNombre.ifBlank { "(sin municipio)" }
                tvSecondary.text = distNombre.ifBlank { "(sin distrito)" }
            } else if (tableName == "HABILIDAD") {
                val nombre = getStringSafely(item, 2)
                val categoria = getStringSafely(item, 3)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)})"
                tvPrimary.text = nombre.ifBlank { "(sin habilidad)" }
                tvSecondary.text = categoria.ifBlank { "(sin categoria)" }
            } else if (tableName == "OFERTA_TRABAJO") {
                val titulo = getStringSafely(item, 3)
                val empresa = getStringSafely(item, 10)
                val fechaCad = getStringSafely(item, 5)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)})"
                tvPrimary.text = titulo.ifBlank { "(sin titulo)" }
                tvSecondary.text = empresa.ifBlank { "(sin empresa)" }

                val utc = TimeZone.getTimeZone("UTC")
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
                val hoy = df.format(Date())
                val vencida = fechaCad.isNotBlank() && fechaCad <= hoy
                chipEstado.visibility = View.VISIBLE
                chipEstado.text = if (vencida) "VENCIDA" else "VIGENTE"
                chipEstado.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 48f
                    setColor(if (vencida) 0xFFE53935.toInt() else 0xFF43A047.toInt())
                }
            } else if (tableName == "DETALLE_REQUISITO") {
                val descripcion = getStringSafely(item, 3)
                val titulo = getStringSafely(item, 4)
                val fechaCad = getStringSafely(item, 6)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)}, ${getStringSafely(item, 2)})"
                tvPrimary.text = descripcion.ifBlank { "(sin descripcion)" }
                tvSecondary.text = titulo.ifBlank { "(sin puesto)" }

                val utc = TimeZone.getTimeZone("UTC")
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
                val hoy = df.format(Date())
                val vencido = fechaCad.isNotBlank() && fechaCad <= hoy
                chipEstado.visibility = View.VISIBLE
                chipEstado.text = if (vencido) "VENCIDO" else "VIGENTE"
                chipEstado.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 48f
                    setColor(if (vencido) 0xFFE53935.toInt() else 0xFF43A047.toInt())
                }
            } else if (tableName == "OFERTA_ACADEMICA") {
                val grado = getStringSafely(item, 4)
                val institucion = getStringSafely(item, 3)
                tvId.text = field0
                tvPrimary.text = grado.ifBlank { "(sin grado)" }
                tvSecondary.text = institucion.ifBlank { "(sin institucion)" }
            } else if (tableName == "CERTIFICACION") {
                val nombre = getStringSafely(item, 4)
                val postNombre = getStringSafely(item, 7)
                val postApellido = getStringSafely(item, 8)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)}, ${getStringSafely(item, 2)})"
                tvPrimary.text = nombre.ifBlank { "(sin certificacion)" }
                tvSecondary.text = "$postNombre $postApellido".trim().ifBlank { "(sin postulante)" }
            } else if (tableName == "FORMACION_ACADEMICA") {
                val titulo = getStringSafely(item, 3)
                val postNombre = getStringSafely(item, 5)
                val postApellido = getStringSafely(item, 6)
                tvId.text = "(${getStringSafely(item, 0)}, ${getStringSafely(item, 1)})"
                tvPrimary.text = titulo.ifBlank { "(sin titulo)" }
                tvSecondary.text = "$postNombre $postApellido".trim().ifBlank { "(sin postulante)" }
            } else if (tableName == "USUARIO") {
                val username = getStringSafely(item, 1)
                val rol = getStringSafely(item, 3)
                tvId.text = field0
                tvPrimary.text = username.ifBlank { "(sin usuario)" }
                tvSecondary.text = rol.ifBlank { "(sin rol)" }
            } else if (tableName == "EMPRESA") {
                val nombreEmpresa = getStringSafely(item, 4)
                val contactoDirecto = getStringSafely(item, 5)
                tvId.text = field0
                tvPrimary.text = nombreEmpresa.ifBlank { "(sin nombre)" }
                tvSecondary.text = contactoDirecto.ifBlank { "(sin contacto)" }
            } else {
                val field1 = getStringSafely(item, 1)
                val field2 = getStringSafely(item, 2)
                tvId.text = field0
                tvPrimary.text = field1.ifBlank { field0.ifBlank { "(vacio)" } }
                tvSecondary.text = field2
            }
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