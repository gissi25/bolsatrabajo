package sv.ues.fia.eisi.bt.ui.servicios

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Servicio10Fragment : Fragment() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var btnSincronizar: MaterialButton
    private lateinit var tvVacio: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvPostulaciones: RecyclerView
    private lateinit var tilPostulante: TextInputLayout
    private lateinit var spPostulante: MaterialAutoCompleteTextView
    private lateinit var tvSelectorLabel: TextView

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
        btnSincronizar = view.findViewById(R.id.btnSincronizar)
        tvVacio = view.findViewById(R.id.tvVacio)
        progressBar = view.findViewById(R.id.progressBar)
        rvPostulaciones = view.findViewById(R.id.rvPostulaciones)
        tilPostulante = view.findViewById(R.id.tilPostulante)
        spPostulante = view.findViewById(R.id.spPostulante)
        tvSelectorLabel = view.findViewById(R.id.tvSelectorLabel)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = PostulacionAdapter(todasPostulaciones) { post ->
            mostrarDetalle(post)
        }
        rvPostulaciones.layoutManager = LinearLayoutManager(requireContext())
        rvPostulaciones.adapter = adapter

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        idPostulante = prefs.getString(Constants.KEY_POSTULANTE_ID, null)

        if (idPostulante == null) {
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
        } else {
            tvSelectorLabel.visibility = View.GONE
            tilPostulante.visibility = View.GONE
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

        btnSincronizar.setOnClickListener { sincronizar() }

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
        progressBar.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE
        lifecycleScope.launch {
            val lista = withContext(Dispatchers.IO) {
                val repo = MainRepository(requireContext())
                repo.getPostulacionesPorPostulante(id)
            }
            todasPostulaciones = lista
            aplicarFiltro()
            progressBar.visibility = View.GONE
        }
    }

    private fun aplicarFiltro() {
        val filtradas = if (filtroActual.isEmpty()) todasPostulaciones
        else todasPostulaciones.filter { it.estadoProceso.lowercase() == filtroActual }
        adapter.actualizar(filtradas)
        tvVacio.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sincronizar() {
        val id = idPostulante ?: return
        btnSincronizar.isEnabled = false
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val json = ApiService.getMisPostulaciones(id)
                val data = json.optJSONArray("data")

                if (data == null || data.length() == 0) {
                    Snackbar.make(requireView(), R.string.s10_sincronizar_vacio, Snackbar.LENGTH_LONG).show()
                    btnSincronizar.isEnabled = true; progressBar.visibility = View.GONE; return@launch
                }

                var insertados = 0
                var actualizados = 0

                withContext(Dispatchers.IO) {
                    val dbHelper = ConnectionHelper(requireContext())
                    val db = dbHelper.writableDb

                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val idPostulacion = item.optString("ID_POSTULACION", "")

                        val cursor = db.rawQuery("SELECT COUNT(*) FROM POSTULACION WHERE ID_POSTULACION = ?", arrayOf(idPostulacion))
                        cursor.moveToFirst()
                        val exists = cursor.getInt(0) > 0
                        cursor.close()

                        if (exists) {
                            val estado = item.optString("ESTADO_PROCESO", "activo")
                            db.execSQL("UPDATE POSTULACION SET ESTADO_PROCESO = ? WHERE ID_POSTULACION = ?", arrayOf(estado, idPostulacion))
                            actualizados++
                        } else {
                            val nit = item.optString("NIT", "")
                            val idOferta = item.optString("ID_OFERTA", "")
                            val idPost = item.optString("ID_POSTULANTE", id)
                            val fecha = item.optString("FECHA_APLICACION", "")
                            val estado = item.optString("ESTADO_PROCESO", "activo")
                            db.execSQL("INSERT OR IGNORE INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf(idPostulacion, nit, idOferta, idPost, fecha, estado))
                            insertados++
                        }
                    }
                    db.close()
                }

                Snackbar.make(requireView(), getString(R.string.s10_sincronizado_ok, insertados, actualizados), Snackbar.LENGTH_LONG).show()
                cargarPostulaciones()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            btnSincronizar.isEnabled = true
            progressBar.visibility = View.GONE
        }
    }

    private fun mostrarDetalle(post: MainRepository.PostulacionCompleta) {
        val contexto = requireContext()
        AlertDialog.Builder(contexto)
            .setTitle(post.tituloPuesto)
            .setMessage(buildString {
                appendLine("${contexto.getString(R.string.s10_estado)}: ${post.estadoProceso}")
                appendLine("${contexto.getString(R.string.s10_aplicado)}: ${post.fechaAplicacion}")
                appendLine("${contexto.getString(R.string.s10_vencimiento)}: ${post.fechaCaducidad}")
                appendLine("Empresa: ${post.nombreEmpresa}")
                appendLine("ID: ${post.idPostulacion}")
            })
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
