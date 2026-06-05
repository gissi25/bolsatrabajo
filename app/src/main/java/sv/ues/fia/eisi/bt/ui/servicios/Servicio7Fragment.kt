package sv.ues.fia.eisi.bt.ui.servicios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio7Fragment : Fragment() {

    private lateinit var etEdad: TextInputEditText
    private lateinit var btnBuscar: MaterialButton
    private lateinit var rvOfertas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinResultados: TextView

    private val ofertasList = mutableListOf<JSONObject>()
    private lateinit var adapter: OfertaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio7, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etEdad = view.findViewById(R.id.etEdad)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        rvOfertas = view.findViewById(R.id.rvOfertas)
        progressBar = view.findViewById(R.id.progressBar)
        tvSinResultados = view.findViewById(R.id.tvSinResultados)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = OfertaAdapter(ofertasList)
        rvOfertas.layoutManager = LinearLayoutManager(requireContext())
        rvOfertas.adapter = adapter

        btnBuscar.setOnClickListener { buscarOfertas() }

        etEdad.setOnEditorActionListener { _, _, _ ->
            buscarOfertas()
            true
        }
    }

    private fun buscarOfertas() {
        val edadStr = etEdad.text?.toString()?.trim()
        val edad = edadStr?.toIntOrNull()

        if (edad == null || edad < 1 || edad > 120) {
            Snackbar.make(requireView(), getString(R.string.s7_error_edad), Snackbar.LENGTH_LONG).show()
            return
        }

        btnBuscar.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvSinResultados.visibility = View.GONE
        ofertasList.clear()
        adapter.notifyDataSetChanged()

        lifecycleScope.launch {
            try {
                val resultados = ApiService.buscarOfertasPorEdad(edad)
                ofertasList.addAll(resultados)
                adapter.notifyDataSetChanged()

                if (resultados.isEmpty()) {
                    tvSinResultados.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            btnBuscar.isEnabled = true
        }
    }

    private class OfertaAdapter(
        private val items: List<JSONObject>
    ) : RecyclerView.Adapter<OfertaAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_oferta_edad, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.tvEmpresa.text = item.optString("NOMBRE_EMPRESA", "")
            h.tvTitulo.text = item.optString("TITULO_PUESTO", "")

            val grado = item.optString("GRADO_REQUERIDO", "")
            h.tvGrado.text = if (grado.isNotEmpty()) "${h.itemView.context.getString(R.string.s7_grado)}: $grado" else ""

            val exp = item.optString("EXPERIENCIA_ANIOS", "")
            h.tvExperiencia.text = if (exp.isNotEmpty()) "${h.itemView.context.getString(R.string.s7_experiencia)}: $exp años" else ""

            val edadMin = item.optInt("EDAD_MINIMA", 0)
            val edadMax = item.optInt("EDAD_MAXIMA", 0)
            h.tvEdadRango.text = "${h.itemView.context.getString(R.string.s7_edad_minima)} $edadMin - ${h.itemView.context.getString(R.string.s7_edad_maxima)} $edadMax"

            val pub = item.optString("FECHA_PUBLICACION", "").take(10)
            val cad = item.optString("FECHA_CADUCIDAD", "").take(10)
            h.tvFechas.text = "${h.itemView.context.getString(R.string.s7_publicacion)}: $pub  |  ${h.itemView.context.getString(R.string.s7_caducidad)}: $cad"
        }

        override fun getItemCount() = items.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvEmpresa: TextView = itemView.findViewById(R.id.tvEmpresa)
            val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
            val tvGrado: TextView = itemView.findViewById(R.id.tvGrado)
            val tvExperiencia: TextView = itemView.findViewById(R.id.tvExperiencia)
            val tvEdadRango: TextView = itemView.findViewById(R.id.tvEdadRango)
            val tvFechas: TextView = itemView.findViewById(R.id.tvFechas)
        }
    }
}
