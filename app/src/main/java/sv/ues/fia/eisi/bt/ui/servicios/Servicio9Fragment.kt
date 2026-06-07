package sv.ues.fia.eisi.bt.ui.servicios

import android.content.Context
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.Constants

class Servicio9Fragment : Fragment() {

    private lateinit var btnModoPostulante: MaterialButton
    private lateinit var btnModoOferta: MaterialButton
    private lateinit var layoutPostulante: View
    private lateinit var layoutOferta: View
    private lateinit var spPostulante: MaterialAutoCompleteTextView
    private lateinit var tilPostulante: TextInputLayout
    private lateinit var spEmpresa: MaterialAutoCompleteTextView
    private lateinit var tilEmpresa: TextInputLayout
    private lateinit var spOferta: MaterialAutoCompleteTextView
    private lateinit var tilOferta: TextInputLayout
    private lateinit var btnBuscar: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultado: TextView
    private lateinit var rvResultados: RecyclerView

    private var modoPostulante = true

    private data class PostulanteItem(val id: String, val nombre: String) {
        override fun toString() = nombre
    }
    private data class EmpresaItem(val nit: String, val nombre: String) {
        override fun toString() = nombre
    }
    private data class OfertaItem(val idOferta: String, val titulo: String) {
        override fun toString() = titulo
    }

    private var postulantes = listOf<PostulanteItem>()
    private var empresas = listOf<EmpresaItem>()
    private var ofertasTemp = listOf<OfertaItem>()
    private var resultados = mutableListOf<JSONObject>()
    private lateinit var adapter: MatchAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio9, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnModoPostulante = view.findViewById(R.id.btnModoPostulante)
        btnModoOferta = view.findViewById(R.id.btnModoOferta)
        layoutPostulante = view.findViewById(R.id.layoutPostulante)
        layoutOferta = view.findViewById(R.id.layoutOferta)
        spPostulante = view.findViewById(R.id.spPostulante); tilPostulante = view.findViewById(R.id.tilPostulante)
        spEmpresa = view.findViewById(R.id.spEmpresa); tilEmpresa = view.findViewById(R.id.tilEmpresa)
        spOferta = view.findViewById(R.id.spOferta); tilOferta = view.findViewById(R.id.tilOferta)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        progressBar = view.findViewById(R.id.progressBar)
        tvResultado = view.findViewById(R.id.tvResultado)
        rvResultados = view.findViewById(R.id.rvResultados)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = MatchAdapter(resultados)
        rvResultados.layoutManager = LinearLayoutManager(requireContext())
        rvResultados.adapter = adapter

        loadDropdownData()
    }

    private fun loadDropdownData() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val postulantesRaw = ApiService.getPostulantes()
                val empresasRaw = ApiService.getEmpresas()

                postulantes = postulantesRaw.map {
                    PostulanteItem(
                        it.getString("ID_POSTULANTE"),
                        "${it.optString("NOMBRE", "")} ${it.optString("APELLIDO", "")}".trim()
                    )
                }
                empresas = empresasRaw.map { EmpresaItem(it.getString("NIT"), it.getString("NOMBRE_EMPRESA")) }

                spPostulante.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, postulantes))
                spPostulante.setThreshold(0); tilPostulante.setOnClickListener { spPostulante.showDropDown() }

                spEmpresa.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, empresas))
                spEmpresa.setThreshold(0); tilEmpresa.setOnClickListener { spEmpresa.showDropDown() }

                setupRoleAndListeners()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error al cargar datos: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupRoleAndListeners() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val role = prefs.getString(Constants.KEY_USER_ROLE, "") ?: ""

        when (role) {
            Constants.ROLE_POSTULANTE -> {
                requireView().findViewById<View>(R.id.layoutToggle).visibility = View.GONE
                setModo(true)
                val savedId = prefs.getString(Constants.KEY_POSTULANTE_ID, null)
                if (savedId != null) {
                    val idx = postulantes.indexOfFirst { it.id == savedId }
                    if (idx >= 0) {
                        spPostulante.setText(postulantes[idx].nombre, false)
                        spPostulante.setTag(savedId)
                    }
                }
            }
            Constants.ROLE_EMPRESA -> {
                requireView().findViewById<View>(R.id.layoutToggle).visibility = View.GONE
                setModo(false)
            }
            else -> {
                btnModoPostulante.setOnClickListener { setModo(true) }
                btnModoOferta.setOnClickListener { setModo(false) }
                setModo(true)
            }
        }

        spPostulante.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < postulantes.size) spPostulante.setTag(postulantes[pos].id)
        }
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

    private fun setModo(postulante: Boolean) {
        modoPostulante = postulante
        btnModoPostulante.isEnabled = !postulante
        btnModoOferta.isEnabled = postulante
        layoutPostulante.visibility = if (postulante) View.VISIBLE else View.GONE
        layoutOferta.visibility = if (postulante) View.GONE else View.VISIBLE

        resultados.clear(); adapter.notifyDataSetChanged()
        tvResultado.visibility = View.GONE

        spPostulante.setText("", false); spPostulante.setTag(null)
        spEmpresa.setText("", false); spEmpresa.setTag(null)
        spOferta.setText("", false); spOferta.setTag(null)
        ofertasTemp = emptyList()
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
        btnBuscar.isEnabled = false; progressBar.visibility = View.VISIBLE; tvResultado.visibility = View.GONE
        resultados.clear(); adapter.notifyDataSetChanged()

        lifecycleScope.launch {
            try {
                if (modoPostulante) {
                    val idPost = spPostulante.tag?.toString()
                    if (idPost.isNullOrEmpty()) {
                        Snackbar.make(requireView(), R.string.s9_error_seleccionar, Snackbar.LENGTH_LONG).show()
                        btnBuscar.isEnabled = true; progressBar.visibility = View.GONE; return@launch
                    }
                    val json = ApiService.matchingPostulante(idPost)
                    val data = json.optJSONArray("data")
                    if (data != null) for (i in 0 until data.length()) resultados.add(data.getJSONObject(i))
                } else {
                    val nit = spEmpresa.tag?.toString()
                    val idOferta = spOferta.tag?.toString()
                    if (nit.isNullOrEmpty() || idOferta.isNullOrEmpty()) {
                        Snackbar.make(requireView(), R.string.s9_error_seleccionar, Snackbar.LENGTH_LONG).show()
                        btnBuscar.isEnabled = true; progressBar.visibility = View.GONE; return@launch
                    }
                    val json = ApiService.matchingOferta(nit, idOferta)
                    val data = json.optJSONArray("data")
                    if (data != null) for (i in 0 until data.length()) resultados.add(data.getJSONObject(i))
                }
                adapter.notifyDataSetChanged()
                tvResultado.text = getString(R.string.s9_resultados, resultados.size)
                tvResultado.visibility = View.VISIBLE
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE; btnBuscar.isEnabled = true
        }
    }

    private class MatchAdapter(
        private val items: List<JSONObject>
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
