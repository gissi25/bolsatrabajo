package sv.ues.fia.eisi.bt.ui.servicios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio9Fragment : Fragment() {

    private lateinit var layoutOferta: View
    private lateinit var spEmpresa: MaterialAutoCompleteTextView
    private lateinit var tilEmpresa: TextInputLayout
    private lateinit var spOferta: MaterialAutoCompleteTextView
    private lateinit var tilOferta: TextInputLayout
    private lateinit var btnBuscar: MaterialButton
    private lateinit var tvResultado: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private data class EmpresaItem(val nit: String, val nombre: String) {
        override fun toString() = nombre
    }
    private data class OfertaItem(val idOferta: String, val titulo: String) {
        override fun toString() = titulo
    }

    private var empresas = listOf<EmpresaItem>()
    private var ofertasTemp = listOf<OfertaItem>()
    private var resultados = mutableListOf<JSONObject>()
    private lateinit var adapter: MatchAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio9, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutOferta = view.findViewById(R.id.layoutOferta)
        spEmpresa = view.findViewById(R.id.spEmpresa); tilEmpresa = view.findViewById(R.id.tilEmpresa)
        spOferta = view.findViewById(R.id.spOferta); tilOferta = view.findViewById(R.id.tilOferta)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        tvResultado = view.findViewById(R.id.tvResultado)
        rvResultados = view.findViewById(R.id.rvResultados)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        swipeRefresh.setOnRefreshListener {
            val nit = spEmpresa.tag?.toString()
            val idOferta = spOferta.tag?.toString()
            if (!nit.isNullOrEmpty() && !idOferta.isNullOrEmpty()) {
                buscar()
            } else {
                loadDropdownData()
            }
        }

        adapter = MatchAdapter(resultados) { mostrarDetalleMatching(it) }
        rvResultados.layoutManager = LinearLayoutManager(requireContext())
        rvResultados.adapter = adapter

        loadDropdownData()
    }

    private fun loadDropdownData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val empresasRaw = ApiService.getEmpresas()
                empresas = empresasRaw.map { EmpresaItem(it.getString("NIT"), it.getString("NOMBRE_EMPRESA")) }

                spEmpresa.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, empresas))
                spEmpresa.setThreshold(0); tilEmpresa.setOnClickListener { spEmpresa.showDropDown() }

                setupRoleAndListeners()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error al cargar datos: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupRoleAndListeners() {
        spEmpresa.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < empresas.size) {
                spEmpresa.setTag(empresas[pos].nit)
                spOferta.setText("", false); spOferta.setTag(null)
                cargarOfertasRemotas(empresas[pos].nit)
            }
        }
        spOferta.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < ofertasTemp.size) spOferta.setTag(ofertasTemp[pos].idOferta)
        }

        btnBuscar.setOnClickListener { buscar() }
    }

    private fun cargarOfertasRemotas(nit: String) {
        lifecycleScope.launch {
            try {
                val raw = ApiService.getOfertasVigentes(nit)
                ofertasTemp = raw.map { OfertaItem(it.getString("ID_OFERTA"), it.getString("TITULO_PUESTO")) }
                spOferta.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ofertasTemp))
                spOferta.setThreshold(0); tilOferta.setOnClickListener { spOferta.showDropDown() }
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error al cargar ofertas: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun buscar() {
        btnBuscar.isEnabled = false; swipeRefresh.isRefreshing = true; tvResultado.visibility = View.GONE
        resultados.clear(); adapter.notifyDataSetChanged()

        val nit = spEmpresa.tag?.toString()
        val idOferta = spOferta.tag?.toString()
        if (nit.isNullOrEmpty() || idOferta.isNullOrEmpty()) {
            Snackbar.make(requireView(), R.string.s9_error_seleccionar, Snackbar.LENGTH_LONG).show()
            btnBuscar.isEnabled = true; swipeRefresh.isRefreshing = false; return
        }

        lifecycleScope.launch {
            try {
                val json = ApiService.matchingOferta(nit, idOferta)
                val data = json.optJSONArray("data")
                if (data != null) for (i in 0 until data.length()) resultados.add(data.getJSONObject(i))
                adapter.notifyDataSetChanged()
                tvResultado.text = getString(R.string.s9_resultados, resultados.size)
                tvResultado.visibility = View.VISIBLE
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            btnBuscar.isEnabled = true; swipeRefresh.isRefreshing = false
        }
    }

    private fun mostrarDetalleMatching(item: JSONObject) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_match_detalle, null)
        val total = item.optInt("puntaje_total", 0)
        val clasificacion = item.optString("clasificacion", "")

        if (item.has("titulo_puesto")) {
            view.findViewById<TextView>(R.id.tvDialogOfertaTitulo).text = item.optString("titulo_puesto", "")
            view.findViewById<TextView>(R.id.tvDialogOfertaEmpresa).text = item.optString("nombre_empresa", "")
        } else {
            view.findViewById<TextView>(R.id.tvDialogOfertaTitulo).text =
                "${item.optString("nombre", "")} ${item.optString("apellido", "")}".trim()
            view.findViewById<TextView>(R.id.tvDialogOfertaEmpresa).text =
                "NUP: ${item.optString("postulante_nup", "")}  |  ${item.optString("postulante_email", "")}"
        }

        val postGrado = item.optString("postulante_grado_nombre", "N/A")
        val ofGrado = item.optString("oferta_grado_nombre", "N/A")
        view.findViewById<TextView>(R.id.tvDialogGrado).text = buildString {
            append(getString(R.string.s9_detalle_postulante_grado, postGrado))
            append("\n")
            append(getString(R.string.s9_detalle_oferta_grado, ofGrado))
            append("\n")
            append("Puntaje: ${item.optInt("puntaje_grado", 0)}/30")
        }

        val postExp = item.optString("postulante_experiencia_total", "0")
        val ofExp = item.optInt("oferta_experiencia_anios", 0)
        view.findViewById<TextView>(R.id.tvDialogExperiencia).text = buildString {
            append(getString(R.string.s9_detalle_postulante_exp, postExp))
            append("\n")
            append(getString(R.string.s9_detalle_oferta_exp, ofExp))
            append("\n")
            append("Puntaje: ${item.optInt("puntaje_experiencia", 0)}/25")
        }

        val habilidadesArr = item.optJSONArray("postulante_habilidades")
        val reqList = item.optJSONArray("oferta_requisitos")
        val habilidadesCoincidentes = item.optInt("habilidades_coincidentes", 0)
        val requisitosTotales = item.optInt("requisitos_totales", 0)

        val sbHabilidades = StringBuilder()
        if (habilidadesArr != null && habilidadesArr.length() > 0) {
            for (i in 0 until habilidadesArr.length()) {
                val h = habilidadesArr.getJSONObject(i)
                val nombre = h.optString("name", "")
                val nivel = h.optString("nivel", "")
                sbHabilidades.append("• $nombre")
                if (nivel.isNotEmpty()) sbHabilidades.append(" ($nivel)")
                sbHabilidades.append("\n")
            }
            sbHabilidades.append("\n")
            sbHabilidades.append(getString(R.string.s9_detalle_habilidades_match, habilidadesCoincidentes, requisitosTotales))
        } else {
            sbHabilidades.append(getString(R.string.s9_detalle_sin_requisitos))
        }
        if (reqList != null && reqList.length() > 0) {
            sbHabilidades.append("\n\nRequisitos de la oferta:")
            for (i in 0 until reqList.length()) {
                sbHabilidades.append("\n• ${reqList.optString(i, "")}")
            }
        }
        view.findViewById<TextView>(R.id.tvDialogHabilidades).text = sbHabilidades.toString()

        val postEdad = item.optInt("postulante_edad", 0)
        val ofEdadMin = item.optInt("oferta_edad_min", 0)
        val ofEdadMax = item.optInt("oferta_edad_max", 0)
        view.findViewById<TextView>(R.id.tvDialogEdad).text = buildString {
            append(getString(R.string.s9_detalle_postulante_edad, postEdad))
            append("\n")
            if (ofEdadMin > 0 && ofEdadMax > 0) {
                append(getString(R.string.s9_detalle_oferta_edad, ofEdadMin, ofEdadMax))
            } else {
                append(getString(R.string.s9_detalle_oferta_edad, 0, 0))
            }
            append("\n")
            append("Puntaje: ${item.optInt("puntaje_edad", 0)}/15")
        }

        val descripcion = item.optString("oferta_descripcion", "")
        view.findViewById<TextView>(R.id.tvDialogDescripcion).text =
            if (descripcion.isNotEmpty()) descripcion else getString(R.string.s9_detalle_sin_requisitos)

        val reqText = if (reqList != null && reqList.length() > 0) {
            buildString {
                append("Requisitos (${reqList.length()}):")
                for (i in 0 until reqList.length()) {
                    append("\n• ${reqList.optString(i, "")}")
                }
            }
        } else ""
        view.findViewById<TextView>(R.id.tvDialogRequisitos).text = reqText

        view.findViewById<TextView>(R.id.tvDialogPuntajes).text = buildString {
            append(getString(R.string.s9_detalle_puntaje_linea,
                item.optInt("puntaje_grado", 0),
                item.optInt("puntaje_habilidades", 0),
                item.optInt("puntaje_experiencia", 0),
                item.optInt("puntaje_edad", 0)))
            append("\n")
            append(getString(R.string.s9_detalle_total, total, clasificacion))
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private class MatchAdapter(
        private val items: List<JSONObject>,
        private val onItemClick: (JSONObject) -> Unit
    ) : RecyclerView.Adapter<MatchAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_match_result, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            val total = item.optDouble("puntaje_total", 0.0).toInt().coerceIn(0, 100)

            val titulo = if (item.has("titulo_puesto"))
                "${item.optString("titulo_puesto", "")} - ${item.optString("nombre_empresa", "")}"
            else
                "${item.optString("nombre", "")} ${item.optString("apellido", "")}".trim()
            h.tvTitulo.text = titulo
            h.tvPuntaje.text = "$total/100"
            h.progressMatch.progress = total

            val color = when {
                total >= 85 -> android.R.color.holo_green_dark
                total >= 60 -> android.R.color.holo_orange_dark
                total >= 30 -> android.R.color.holo_orange_light
                else -> android.R.color.holo_red_dark
            }
            h.progressMatch.progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(h.itemView.context, color)
            )

            h.tvClasificacion.text = item.optString("clasificacion", "")

            h.tvDesglose.text = h.itemView.context.getString(R.string.s9_desglose,
                item.optInt("puntaje_grado", 0),
                item.optInt("puntaje_habilidades", 0),
                item.optInt("puntaje_experiencia", 0),
                item.optInt("puntaje_edad", 0))

            h.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
            val tvPuntaje: TextView = itemView.findViewById(R.id.tvPuntaje)
            val progressMatch: ProgressBar = itemView.findViewById(R.id.progressMatch)
            val tvClasificacion: TextView = itemView.findViewById(R.id.tvClasificacion)
            val tvDesglose: TextView = itemView.findViewById(R.id.tvDesglose)
        }
    }
}
