package sv.ues.fia.eisi.bt.ui.servicios

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio8Fragment : Fragment() {

    private lateinit var etNit: TextInputEditText
    private lateinit var btnResumen: MaterialButton
    private lateinit var btnRanking: MaterialButton
    private lateinit var btnEstados: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollResultados: ScrollView
    private lateinit var containerResultados: LinearLayout
    private lateinit var rvRanking: RecyclerView
    private lateinit var rankingAdapter: RankingAdapter

    private val rankingList = mutableListOf<JSONObject>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio8, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etNit = view.findViewById(R.id.etNit)
        btnResumen = view.findViewById(R.id.btnResumen)
        btnRanking = view.findViewById(R.id.btnRanking)
        btnEstados = view.findViewById(R.id.btnEstados)
        progressBar = view.findViewById(R.id.progressBar)
        scrollResultados = view.findViewById(R.id.scrollResultados)
        containerResultados = view.findViewById(R.id.containerResultados)
        rvRanking = view.findViewById(R.id.rvRanking)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        rankingAdapter = RankingAdapter(rankingList)
        rvRanking.layoutManager = LinearLayoutManager(requireContext())
        rvRanking.adapter = rankingAdapter

        btnResumen.setOnClickListener { consultarResumen() }
        btnRanking.setOnClickListener { consultarRanking() }
        btnEstados.setOnClickListener { consultarEstados() }
    }

    private fun getNit(): String? {
        val nit = etNit.text?.toString()?.trim()
        if (nit.isNullOrEmpty()) {
            Snackbar.make(requireView(), getString(R.string.s8_error_nit), Snackbar.LENGTH_LONG).show()
            return null
        }
        return nit
    }

    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        btnResumen.isEnabled = !mostrar
        btnRanking.isEnabled = !mostrar
        btnEstados.isEnabled = !mostrar
        etNit.isEnabled = !mostrar
    }

    private fun mostrarCards() {
        scrollResultados.visibility = View.VISIBLE
        rvRanking.visibility = View.GONE
        containerResultados.removeAllViews()
    }

    private fun mostrarRanking() {
        scrollResultados.visibility = View.GONE
        rvRanking.visibility = View.VISIBLE
        rankingList.clear()
        rankingAdapter.notifyDataSetChanged()
    }

    private fun agregarKpi(valor: String, label: String) {
        val card = layoutInflater.inflate(R.layout.card_s8_kpi, containerResultados, false)
        card.findViewById<TextView>(R.id.tvKpiValue).text = valor
        card.findViewById<TextView>(R.id.tvKpiLabel).text = label
        containerResultados.addView(card)
    }

    private fun agregarTituloSeccion(titulo: String) {
        val tv = TextView(requireContext())
        tv.text = titulo
        tv.setTextSize(16f)
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
        tv.setTextColor(requireContext().getColor(R.color.primary))
        tv.setPadding(0, 24, 0, 12)
        containerResultados.addView(tv)
    }

    private fun consultarResumen() {
        val nit = getNit() ?: return
        mostrarCargando(true)
        mostrarCards()

        lifecycleScope.launch {
            try {
                val res = ApiService.getResumenReclutamiento(nit)
                if (res.optBoolean("exito", false)) {
                    agregarTituloSeccion("Resumen de reclutamiento")

                    val row = LinearLayout(requireContext())
                    row.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    row.orientation = LinearLayout.HORIZONTAL

                    val inflater = LayoutInflater.from(requireContext())

                    val card1 = inflater.inflate(R.layout.card_s8_kpi, row, false)
                    card1.findViewById<TextView>(R.id.tvKpiValue).text = res.optInt("ofertas_activas", 0).toString()
                    card1.findViewById<TextView>(R.id.tvKpiLabel).text = getString(R.string.s8_ofertas_activas)
                    row.addView(card1)

                    val card2 = inflater.inflate(R.layout.card_s8_kpi, row, false)
                    card2.findViewById<TextView>(R.id.tvKpiValue).text = res.optInt("ofertas_vencidas", 0).toString()
                    card2.findViewById<TextView>(R.id.tvKpiLabel).text = getString(R.string.s8_ofertas_vencidas)
                    row.addView(card2)

                    val card3 = inflater.inflate(R.layout.card_s8_kpi, row, false)
                    card3.findViewById<TextView>(R.id.tvKpiValue).text = res.optInt("total_postulaciones", 0).toString()
                    card3.findViewById<TextView>(R.id.tvKpiLabel).text = getString(R.string.s8_total_postulaciones)
                    row.addView(card3)

                    containerResultados.addView(row)
                } else {
                    throw Exception(res.optString("error", "Error del servidor"))
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private fun consultarRanking() {
        val nit = getNit() ?: return
        mostrarCargando(true)
        mostrarRanking()

        lifecycleScope.launch {
            try {
                val ofertas = ApiService.getRankingOfertas(nit)
                rankingList.addAll(ofertas)
                rankingAdapter.notifyDataSetChanged()
                if (ofertas.isEmpty()) {
                    Snackbar.make(requireView(), "No se encontraron ofertas para este NIT.", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private fun consultarEstados() {
        val nit = getNit() ?: return
        mostrarCargando(true)
        mostrarCards()

        lifecycleScope.launch {
            try {
                val estados = ApiService.getPostulantesPorEstado(nit)
                if (estados.isEmpty()) {
                    Snackbar.make(requireView(), "No hay postulaciones registradas para este NIT.", Snackbar.LENGTH_LONG).show()
                } else {
                    agregarTituloSeccion("Postulantes por estado")

                    val mapaColores = mapOf(
                        "Activo" to Color.parseColor("#2196F3"),
                        "En Proceso" to Color.parseColor("#FF9800"),
                        "Contratado" to Color.parseColor("#4CAF50"),
                        "Rechazado" to Color.parseColor("#F44336")
                    )
                    val mapaEstados = mapOf(
                        "Activo" to getString(R.string.s8_estado_activo),
                        "En Proceso" to getString(R.string.s8_estado_en_proceso),
                        "Contratado" to getString(R.string.s8_estado_contratado),
                        "Rechazado" to getString(R.string.s8_estado_rechazado)
                    )

                    val row = LinearLayout(requireContext())
                    row.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    row.orientation = LinearLayout.HORIZONTAL

                    val inflater = LayoutInflater.from(requireContext())

                    estados.forEach { e ->
                        val estado = e.optString("ESTADO_PROCESO", "—")
                        val total = e.optInt("total", 0)
                        val label = mapaEstados[estado] ?: estado
                        val color = mapaColores[estado] ?: Color.parseColor("#9E9E9E")

                        val card = inflater.inflate(R.layout.card_s8_estado, row, false)
                        card.findViewById<TextView>(R.id.tvEstadoCount).text = total.toString()
                        card.findViewById<TextView>(R.id.tvEstadoLabel).text = label
                        (card as MaterialCardView).strokeColor = color
                        row.addView(card)
                    }

                    containerResultados.addView(row)
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private class RankingAdapter(
        private val items: List<JSONObject>
    ) : RecyclerView.Adapter<RankingAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_s8_ranking, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.tvPosicion.text = "#${pos + 1}"
            h.tvTitulo.text = item.optString("TITULO_PUESTO", "—")
            h.tvIdOferta.text = "ID: ${item.optString("ID_OFERTA", "—")}"
            h.tvPostulaciones.text = "${item.optInt("total_postulaciones", 0)} postulaciones"

            val pub = item.optString("FECHA_PUBLICACION", "").take(10)
            val cad = item.optString("FECHA_CADUCIDAD", "").take(10)
            h.tvPublicacion.text = "Publicación: $pub"
            h.tvCaducidad.text = "Caducidad: $cad"
        }

        override fun getItemCount() = items.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvPosicion: TextView = itemView.findViewById(R.id.tvPosicion)
            val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
            val tvIdOferta: TextView = itemView.findViewById(R.id.tvIdOferta)
            val tvPostulaciones: TextView = itemView.findViewById(R.id.tvPostulaciones)
            val tvPublicacion: TextView = itemView.findViewById(R.id.tvPublicacion)
            val tvCaducidad: TextView = itemView.findViewById(R.id.tvCaducidad)
        }
    }
}
