package sv.ues.fia.eisi.bt.ui.servicios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.service.ApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Servicio10Fragment : Fragment() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var tvVacio: TextView
    private lateinit var rvPostulaciones: RecyclerView
    private lateinit var tilPostulante: TextInputLayout
    private lateinit var spPostulante: MaterialAutoCompleteTextView
    private lateinit var tvSelectorLabel: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var idPostulante: String? = null
    private var todasPostulaciones = listOf<MainRepository.PostulacionCompleta>()
    private var filtroActual = ""
    private lateinit var adapter: PostulacionAdapter

    private data class PostulanteItem(val id: String, val nombre: String) {
        override fun toString() = nombre
    }
    private var postulantes = listOf<PostulanteItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio10, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroup = view.findViewById(R.id.chipGroup)
        tvVacio = view.findViewById(R.id.tvVacio)
        rvPostulaciones = view.findViewById(R.id.rvPostulaciones)
        tilPostulante = view.findViewById(R.id.tilPostulante)
        spPostulante = view.findViewById(R.id.spPostulante)
        tvSelectorLabel = view.findViewById(R.id.tvSelectorLabel)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        swipeRefresh.setOnRefreshListener { cargarPostulaciones() }

        adapter = PostulacionAdapter(todasPostulaciones) { post ->
            mostrarDetalle(post)
        }
        rvPostulaciones.layoutManager = LinearLayoutManager(requireContext())
        rvPostulaciones.adapter = adapter

        tvSelectorLabel.visibility = View.VISIBLE
        tilPostulante.visibility = View.VISIBLE
        cargarPostulantesLocal()
        spPostulante.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < postulantes.size) {
                spPostulante.setTag(postulantes[pos].id)
                idPostulante = postulantes[pos].id
                cargarPostulaciones()
            }
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            filtroActual = when (checkedIds.firstOrNull()) {
                R.id.chipActivo -> "activo"
                R.id.chipEnProceso -> "en proceso"
                R.id.chipContratado -> "contratado"
                R.id.chipRechazado -> "rechazado"
                else -> ""
            }
            aplicarFiltro()
        }

        cargarPostulaciones()
    }

    private fun cargarPostulantesLocal() {
        val repo = MainRepository(requireContext())
        val raw = repo.getDropdownOptions("POSTULANTE", "NOMBRE")
        postulantes = raw.map { PostulanteItem(it.first, it.second) }
        spPostulante.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, postulantes))
    }

    private fun cargarPostulaciones() {
        val id = idPostulante ?: return
        swipeRefresh.isRefreshing = true
        tvVacio.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    ApiService.getMisPostulaciones(id)
                }
                val postulanteExiste = json.optBoolean("postulante_existe", true)
                if (!postulanteExiste) {
                    Snackbar.make(requireView(), "El postulante no existe en el servidor remoto", Snackbar.LENGTH_LONG).show()
                }
                val data = json.optJSONArray("data")
                val lista = mutableListOf<MainRepository.PostulacionCompleta>()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        lista.add(MainRepository.PostulacionCompleta(
                            idPostulacion = item.optString("ID_POSTULACION", ""),
                            fechaAplicacion = item.optString("FECHA_APLICACION", ""),
                            estadoProceso = item.optString("ESTADO_PROCESO", "activo").lowercase(),
                            tituloPuesto = item.optString("TITULO_PUESTO", ""),
                            fechaPublicacion = item.optString("FECHA_PUBLICACION", ""),
                            fechaCaducidad = item.optString("FECHA_CADUCIDAD", ""),
                            nombreEmpresa = item.optString("NOMBRE_EMPRESA", ""),
                            nit = item.optString("NIT", ""),
                            idOferta = item.optString("ID_OFERTA", "")
                        ))
                    }
                }
                todasPostulaciones = lista
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error al obtener datos: ${e.message}", Snackbar.LENGTH_LONG).show()
                todasPostulaciones = emptyList()
            }
            aplicarFiltro()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun aplicarFiltro() {
        val filtradas = if (filtroActual.isEmpty()) todasPostulaciones
        else todasPostulaciones.filter { it.estadoProceso.lowercase() == filtroActual }
        adapter.actualizar(filtradas)
        tvVacio.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarDetalle(post: MainRepository.PostulacionCompleta) {
        val contexto = requireContext()
        val view = LayoutInflater.from(contexto).inflate(R.layout.dialog_postulacion_detalle, null)

        view.findViewById<TextView>(R.id.tvDialogTituloPuesto).text = post.tituloPuesto
        view.findViewById<TextView>(R.id.tvDialogNombreEmpresa).text = post.nombreEmpresa

        val color = ContextCompat.getColor(contexto, getEstadoColor(post.estadoProceso))
        val badge = view.findViewById<TextView>(R.id.tvDialogEstadoBadge)
        badge.text = getEstadoLabel(post.estadoProceso)
        badge.setBackgroundColor(color)

        view.findViewById<TextView>(R.id.tvDialogEstado).text = getEstadoLabel(post.estadoProceso)
        view.findViewById<TextView>(R.id.tvDialogFechaAplicacion).text = post.fechaAplicacion
        view.findViewById<TextView>(R.id.tvDialogFechaPublicacion).text = post.fechaPublicacion

        val vencida = estaVencida(post.fechaCaducidad)
        view.findViewById<TextView>(R.id.tvDialogFechaVencimiento).text = post.fechaCaducidad
        val tvVencida = view.findViewById<TextView>(R.id.tvDialogVencidaWarning)
        tvVencida.visibility = if (vencida) View.VISIBLE else View.GONE

        view.findViewById<TextView>(R.id.tvDialogIdPostulacion).text = post.idPostulacion

        MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun getEstadoColor(estado: String): Int {
        return when (estado.lowercase()) {
            "activo" -> R.color.estado_activo
            "en proceso" -> R.color.estado_en_proceso
            "contratado" -> R.color.estado_contratado
            "rechazado" -> R.color.estado_rechazado
            else -> R.color.estado_activo
        }
    }

    private fun getEstadoLabel(estado: String): String {
        return when (estado.lowercase()) {
            "activo" -> getString(R.string.s10_estado_activo)
            "en proceso" -> getString(R.string.s10_estado_en_proceso)
            "contratado" -> getString(R.string.s10_estado_contratado)
            "rechazado" -> getString(R.string.s10_estado_rechazado)
            else -> estado
        }
    }

    private fun estaVencida(fechaCad: String): Boolean {
        if (fechaCad.isBlank()) return false
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cad = fmt.parse(fechaCad)
            cad != null && cad.before(Date())
        } catch (_: Exception) { false }
    }

    private inner class PostulacionAdapter(
        private var items: List<MainRepository.PostulacionCompleta>,
        private val onClick: (MainRepository.PostulacionCompleta) -> Unit
    ) : RecyclerView.Adapter<PostulacionAdapter.VH>() {

        fun actualizar(nueva: List<MainRepository.PostulacionCompleta>) {
            items = nueva
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_postulacion, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]

            h.tvTitulo.text = item.tituloPuesto
            h.tvEmpresa.text = item.nombreEmpresa
            h.tvFechaAplicacion.text = "${getString(R.string.s10_aplicado)}: ${item.fechaAplicacion}"
            h.tvFechaCaducidad.text = "${getString(R.string.s10_vencimiento)}: ${item.fechaCaducidad}"

            val color = ContextCompat.getColor(h.itemView.context, getEstadoColor(item.estadoProceso))
            h.tvBadgeEstado.text = getEstadoLabel(item.estadoProceso)
            h.tvBadgeEstado.setBackgroundColor(color)

            val vencida = estaVencida(item.fechaCaducidad)
            h.tvVencida.visibility = if (vencida) View.VISIBLE else View.GONE

            h.btnDetalle.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvBadgeEstado: TextView = itemView.findViewById(R.id.tvBadgeEstado)
            val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloPuesto)
            val tvEmpresa: TextView = itemView.findViewById(R.id.tvNombreEmpresa)
            val tvFechaAplicacion: TextView = itemView.findViewById(R.id.tvFechaAplicacion)
            val tvFechaCaducidad: TextView = itemView.findViewById(R.id.tvFechaCaducidad)
            val tvVencida: TextView = itemView.findViewById(R.id.tvVencida)
            val btnDetalle: MaterialButton = itemView.findViewById(R.id.btnVerDetalle)
        }
    }
}
