package sv.ues.fia.eisi.bt.ui.crud

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
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
    private val onViewClick: ((List<Any>, Int) -> Unit)? = null,
    private val onItemSelected: ((List<Any>, Int) -> Unit)? = null
) : ListAdapter<List<Any>, TableAdapter.ViewHolder>(RowDiffCallback()) {

    var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_table_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    fun getSelectedItem(): List<Any>? {
        return if (selectedPosition in 0 until itemCount) getItem(selectedPosition) else null
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
            if (onItemSelected != null) {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val oldPos = selectedPosition
                        selectedPosition = if (selectedPosition == pos) -1 else pos
                        if (oldPos != -1) notifyItemChanged(oldPos)
                        if (selectedPosition != -1) notifyItemChanged(selectedPosition)
                        onItemSelected(if (selectedPosition >= 0) getItem(pos) else emptyList(), selectedPosition)
                    }
                }
            } else if (!canEdit && onViewClick != null) {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onViewClick(getItem(pos), pos)
                }
            }
        }

        fun bind(item: List<Any>, position: Int) {
            val ctx = itemView.context
            val field0 = getStringSafely(item, 0)
            chipEstado.visibility = View.GONE

            if (onItemSelected != null) {
                val card = itemView as? com.google.android.material.card.MaterialCardView
                if (card != null) {
                    card.setStrokeColor(
                        ContextCompat.getColor(ctx,
                            if (selectedPosition == position) R.color.primary else R.color.outline
                        )
                    )
                    card.strokeWidth = if (selectedPosition == position) 6 else 1
                }
            }

            if (tableName == "POSTULANTE") {
                val nombre = getStringSafely(item, 8)
                val apellido = getStringSafely(item, 9)
                tvId.text = field0
                tvPrimary.text = nombre.ifBlank { ctx.getString(R.string.fallback_sin_nombre) }
                tvSecondary.text = apellido.ifBlank { ctx.getString(R.string.fallback_sin_apellido) }
            } else if (tableName == "EXPERIENCIA_LABORAL") {
                val nombre = getStringSafely(item, 8)
                val apellido = getStringSafely(item, 9)
                val puesto = getStringSafely(item, 3)
                tvId.text = String.format("(%s, %s, %s)", getStringSafely(item, 0), getStringSafely(item, 1), getStringSafely(item, 2))
                tvPrimary.text = String.format("%s %s", nombre, apellido).trim().ifBlank { ctx.getString(R.string.fallback_sin_nombre) }
                tvSecondary.text = puesto.ifBlank { ctx.getString(R.string.fallback_sin_puesto) }
            } else if (tableName == "HABILIDAD_POSTULANTE") {
                val nombre = getStringSafely(item, 4)
                val apellido = getStringSafely(item, 5)
                val habilidad = getStringSafely(item, 6)
                val nivelText = getStringSafely(item, 3)

                tvId.text = String.format("(%s, %s, %s)", getStringSafely(item, 0), getStringSafely(item, 1), getStringSafely(item, 2))
                tvPrimary.text = String.format("%s %s", nombre, apellido).trim().ifBlank { ctx.getString(R.string.fallback_sin_nombre) }
                tvSecondary.text = if (nivelText.isNotBlank()) "$habilidad • $nivelText" else habilidad
            } else if (tableName == "POSTULACION") {
                val nombre = getStringSafely(item, 6)
                val apellido = getStringSafely(item, 7)
                val puesto = getStringSafely(item, 8)
                tvId.text = field0
                tvPrimary.text = String.format("%s %s", nombre, apellido).trim().ifBlank { ctx.getString(R.string.fallback_sin_nombre) }
                tvSecondary.text = puesto.ifBlank { ctx.getString(R.string.fallback_sin_puesto) }
            } else if (tableName == "RED_SOCIAL_POSTULANTE") {
                val nombre = getStringSafely(item, 3)
                val apellido = getStringSafely(item, 4)
                val redSocial = getStringSafely(item, 5)
                tvId.text = String.format("(%s, %s)", getStringSafely(item, 0), getStringSafely(item, 1))
                tvPrimary.text = String.format("%s %s", nombre, apellido).trim().ifBlank { ctx.getString(R.string.fallback_sin_nombre) }
                tvSecondary.text = redSocial.ifBlank { ctx.getString(R.string.fallback_sin_red_social) }
            } else if (tableName == "MUNICIPIO") {
                val deptoNombre = getStringSafely(item, 3)
                val munNombre = getStringSafely(item, 2)
                tvId.text = String.format("(%s, %s)", getStringSafely(item, 0), getStringSafely(item, 1))
                tvPrimary.text = deptoNombre.ifBlank { ctx.getString(R.string.fallback_sin_departamento) }
                tvSecondary.text = munNombre.ifBlank { ctx.getString(R.string.fallback_sin_municipio) }
            } else if (tableName == "DISTRITO") {
                val munNombre = getStringSafely(item, 4)
                val distNombre = getStringSafely(item, 3)
                tvId.text = String.format("(%s, %s, %s)", getStringSafely(item, 0), getStringSafely(item, 1), getStringSafely(item, 2))
                tvPrimary.text = munNombre.ifBlank { ctx.getString(R.string.fallback_sin_municipio) }
                tvSecondary.text = distNombre.ifBlank { ctx.getString(R.string.fallback_sin_distrito) }
            } else if (tableName == "HABILIDAD") {
                val nombre = getStringSafely(item, 2)
                val categoria = getStringSafely(item, 3)
                tvId.text = String.format("(%s, %s)", getStringSafely(item, 0), getStringSafely(item, 1))
                tvPrimary.text = nombre.ifBlank { ctx.getString(R.string.fallback_sin_habilidad) }
                tvSecondary.text = categoria.ifBlank { ctx.getString(R.string.fallback_sin_categoria) }
            } else if (tableName == "OFERTA_TRABAJO") {
                val titulo = getStringSafely(item, 3)
                val empresa = getStringSafely(item, 10)
                val fechaCad = getStringSafely(item, 5)
                tvId.text = String.format("(%s, %s)", getStringSafely(item, 0), getStringSafely(item, 1))
                tvPrimary.text = titulo.ifBlank { ctx.getString(R.string.fallback_sin_titulo) }
                tvSecondary.text = empresa.ifBlank { ctx.getString(R.string.fallback_sin_empresa) }

                val utc = TimeZone.getTimeZone("UTC")
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
                val hoy = df.format(Date())
                val vencida = fechaCad.isNotBlank() && fechaCad <= hoy
                chipEstado.visibility = View.VISIBLE
                chipEstado.text = if (vencida) ctx.getString(R.string.estado_vencida) else ctx.getString(R.string.estado_vigente)
                chipEstado.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 48f
                    setColor(if (vencida) 0xFFE53935.toInt() else 0xFF43A047.toInt())
                }
            } else if (tableName == "DETALLE_REQUISITO") {
                val descripcion = getStringSafely(item, 3)
                val titulo = getStringSafely(item, 4)
                val fechaCad = getStringSafely(item, 6)
                tvId.text = String.format("(%s, %s, %s)", getStringSafely(item, 0), getStringSafely(item, 1), getStringSafely(item, 2))
                tvPrimary.text = descripcion.ifBlank { ctx.getString(R.string.fallback_sin_descripcion) }
                tvSecondary.text = titulo.ifBlank { ctx.getString(R.string.fallback_sin_puesto) }

                val utc = TimeZone.getTimeZone("UTC")
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
                val hoy = df.format(Date())
                val vencido = fechaCad.isNotBlank() && fechaCad <= hoy
                chipEstado.visibility = View.VISIBLE
                chipEstado.text = if (vencido) ctx.getString(R.string.estado_vencido) else ctx.getString(R.string.estado_vigente)
                chipEstado.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 48f
                    setColor(if (vencido) 0xFFE53935.toInt() else 0xFF43A047.toInt())
                }
            } else if (tableName == "OFERTA_ACADEMICA") {
                val grado = getStringSafely(item, 4)
                val institucion = getStringSafely(item, 3)
                tvId.text = field0
                tvPrimary.text = grado.ifBlank { ctx.getString(R.string.fallback_sin_grado) }
                tvSecondary.text = institucion.ifBlank { ctx.getString(R.string.fallback_sin_institucion) }
            } else if (tableName == "CERTIFICACION") {
                val nombre = getStringSafely(item, 4)
                val fechaInicio = getStringSafely(item, 5)
                val fechaFin = getStringSafely(item, 6)
                tvId.text = String.format("(%s, %s, %s)", getStringSafely(item, 0), getStringSafely(item, 1), getStringSafely(item, 2))
                tvPrimary.text = nombre.ifBlank { ctx.getString(R.string.fallback_sin_certificacion) }
                tvSecondary.text = if (fechaInicio.isNotBlank() && fechaFin.isNotBlank()) "$fechaInicio → $fechaFin" else ctx.getString(R.string.fallback_sin_periodo)
            } else if (tableName == "FORMACION_ACADEMICA") {
                val titulo = getStringSafely(item, 3)
                val fechaInicio = getStringSafely(item, 4)
                val fechaFin = getStringSafely(item, 5)
                tvId.text = String.format("(%s, %s)", getStringSafely(item, 0), getStringSafely(item, 1))
                tvPrimary.text = titulo.ifBlank { ctx.getString(R.string.fallback_sin_titulo) }
                tvSecondary.text = if (fechaInicio.isNotBlank() && fechaFin.isNotBlank()) "$fechaInicio → $fechaFin" else ctx.getString(R.string.fallback_sin_periodo)
            } else if (tableName == "USUARIO") {
                val username = getStringSafely(item, 1)
                val rol = getStringSafely(item, 3)
                tvId.text = field0
                tvPrimary.text = username.ifBlank { ctx.getString(R.string.fallback_sin_usuario) }
                tvSecondary.text = rol.ifBlank { ctx.getString(R.string.fallback_sin_rol) }
            } else if (tableName == "EMPRESA") {
                val nombreEmpresa = getStringSafely(item, 4)
                val contactoDirecto = getStringSafely(item, 5)
                tvId.text = field0
                tvPrimary.text = nombreEmpresa.ifBlank { ctx.getString(R.string.fallback_sin_nombre) }
                tvSecondary.text = contactoDirecto.ifBlank { ctx.getString(R.string.fallback_sin_contacto) }
            } else {
                val field1 = getStringSafely(item, 1)
                val field2 = getStringSafely(item, 2)
                tvId.text = field0
                tvPrimary.text = field1.ifBlank { field0.ifBlank { ctx.getString(R.string.fallback_vacio) } }
                tvSecondary.text = field2
            }
        }

        private fun getStringSafely(list: List<Any>, index: Int): String {
            return try {
                list.getOrNull(index)?.toString()?.trim() ?: ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    class RowDiffCallback : DiffUtil.ItemCallback<List<Any>>() {
        override fun areItemsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
            return oldItem.firstOrNull() == newItem.firstOrNull()
        }

        override fun areContentsTheSame(oldItem: List<Any>, newItem: List<Any>): Boolean {
            return oldItem.size == newItem.size && oldItem.zip(newItem).all { (a, b) -> a == b }
        }
    }
}
