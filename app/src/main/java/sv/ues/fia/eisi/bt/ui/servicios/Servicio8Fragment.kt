package sv.ues.fia.eisi.bt.ui.servicios

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.setupMarqueeTitle

class Servicio8Fragment : Fragment() {

    private lateinit var etNit: TextInputEditText
    private lateinit var btnCargar: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollDashboard: ScrollView
    private lateinit var containerDashboard: LinearLayout

    private val mapaColores = mapOf(
        "activo" to Color.parseColor("#2196F3"),
        "en proceso" to Color.parseColor("#FF9800"),
        "contratado" to Color.parseColor("#4CAF50"),
        "rechazado" to Color.parseColor("#F44336")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio8, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etNit = view.findViewById(R.id.etNit)
        btnCargar = view.findViewById(R.id.btnCargar)
        progressBar = view.findViewById(R.id.progressBar)
        scrollDashboard = view.findViewById(R.id.scrollDashboard)
        containerDashboard = view.findViewById(R.id.containerDashboard)

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setupMarqueeTitle()
        }

        btnCargar.setOnClickListener { cargarDashboard() }
        etNit.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                cargarDashboard()
                true
            } else false
        }
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
        btnCargar.isEnabled = !mostrar
        etNit.isEnabled = !mostrar
        if (mostrar) {
            scrollDashboard.visibility = View.GONE
            containerDashboard.removeAllViews()
        }
    }

    private fun agregarTituloSeccion(titulo: String) {
        val tv = TextView(requireContext())
        tv.text = titulo
        tv.setTextSize(17f)
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
        tv.setTextColor(requireContext().getColor(R.color.primary))
        tv.setPadding(0, 20, 0, 8)
        containerDashboard.addView(tv)
    }

    private fun agregarKpiRow(ofertasActivas: Int, ofertasVencidas: Int, totalPostulaciones: Int) {
        val row = LinearLayout(requireContext())
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.orientation = LinearLayout.HORIZONTAL

        val inflater = LayoutInflater.from(requireContext())

        val card1 = inflater.inflate(R.layout.card_s8_kpi, row, false)
        card1.findViewById<TextView>(R.id.tvKpiValue).text = ofertasActivas.toString()
        card1.findViewById<TextView>(R.id.tvKpiLabel).text = getString(R.string.s8_ofertas_activas)
        row.addView(card1)

        val card2 = inflater.inflate(R.layout.card_s8_kpi, row, false)
        card2.findViewById<TextView>(R.id.tvKpiValue).text = ofertasVencidas.toString()
        card2.findViewById<TextView>(R.id.tvKpiLabel).text = getString(R.string.s8_ofertas_vencidas)
        row.addView(card2)

        val card3 = inflater.inflate(R.layout.card_s8_kpi, row, false)
        card3.findViewById<TextView>(R.id.tvKpiValue).text = totalPostulaciones.toString()
        card3.findViewById<TextView>(R.id.tvKpiLabel).text = getString(R.string.s8_total_postulaciones)
        row.addView(card3)

        containerDashboard.addView(row)
    }

    private fun agregarRanking(ranking: List<JSONObject>) {
        if (ranking.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = getString(R.string.s8_sin_ofertas)
            tv.setTextSize(14f)
            tv.setPadding(4, 8, 0, 0)
            containerDashboard.addView(tv)
            return
        }

        val inflater = LayoutInflater.from(requireContext())

        ranking.forEachIndexed { index, item ->
            val card = inflater.inflate(R.layout.card_s8_ranking, containerDashboard, false)
            card.findViewById<TextView>(R.id.tvPosicion).text = "#${index + 1}"
            card.findViewById<TextView>(R.id.tvTitulo).text = item.optString("TITULO_PUESTO", "—")
            card.findViewById<TextView>(R.id.tvIdOferta).text = "ID: ${item.optString("ID_OFERTA", "—")}"
            card.findViewById<TextView>(R.id.tvPostulaciones).text = getString(R.string.s8_postulaciones_count, item.optInt("total_postulaciones", 0))

            val pub = item.optString("FECHA_PUBLICACION", "").take(10)
            val cad = item.optString("FECHA_CADUCIDAD", "").take(10)
            card.findViewById<TextView>(R.id.tvPublicacion).text = "${getString(R.string.s8_publicacion)}: $pub"
            card.findViewById<TextView>(R.id.tvCaducidad).text = "${getString(R.string.s8_caducidad)}: $cad"

            containerDashboard.addView(card)
        }
    }

    private fun agregarEstados(estados: List<JSONObject>) {
        if (estados.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = getString(R.string.s8_sin_postulaciones)
            tv.setTextSize(14f)
            tv.setPadding(4, 8, 0, 0)
            containerDashboard.addView(tv)
            return
        }

        val mapaEstados = mapOf(
            "activo" to getString(R.string.s8_estado_activo),
            "en proceso" to getString(R.string.s8_estado_en_proceso),
            "contratado" to getString(R.string.s8_estado_contratado),
            "rechazado" to getString(R.string.s8_estado_rechazado)
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

        containerDashboard.addView(row)

        val total = estados.sumOf { it.optInt("total", 0) }
        estados.forEach { e ->
            val count = e.optInt("total", 0)
            val pct = if (total > 0) (count * 100 / total) else 0
            val label = mapaEstados[e.optString("ESTADO_PROCESO", "—")] ?: e.optString("ESTADO_PROCESO", "—")
            agregarBarraProgreso(label, count, pct)
        }
    }

    private fun agregarBarraProgreso(label: String, count: Int, pct: Int) {
        val color = mapaColores.entries.firstOrNull { e ->
            label.lowercase().contains(e.key.lowercase())
        }?.value ?: Color.parseColor("#9E9E9E")

        val row = LinearLayout(requireContext())
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(4, 6, 4, 6)

        val tvLabel = TextView(requireContext())
        tvLabel.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.35f)
        tvLabel.text = label
        tvLabel.setTextSize(13f)
        row.addView(tvLabel)

        val barContainer = LinearLayout(requireContext())
        barContainer.layoutParams = LinearLayout.LayoutParams(0, 24, 0.5f)
        barContainer.orientation = LinearLayout.HORIZONTAL
        barContainer.setBackgroundColor(Color.parseColor("#E0E0E0"))

        val fill = View(requireContext())
        fill.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct.toFloat())
        fill.setBackgroundColor(color)
        barContainer.addView(fill)
        row.addView(barContainer)

        val tvPct = TextView(requireContext())
        tvPct.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.15f)
        tvPct.text = "$pct%"
        tvPct.setTextSize(12f)
        tvPct.gravity = android.view.Gravity.END
        row.addView(tvPct)

        containerDashboard.addView(row)
    }

    private fun cargarDashboard() {
        val nit = getNit() ?: return
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val res = ApiService.getDashboardEmpresa(nit)
                if (!res.optBoolean("exito", false)) {
                    throw Exception(res.optString("error", "Error del servidor"))
                }

                scrollDashboard.visibility = View.VISIBLE

                agregarTituloSeccion(getString(R.string.s8_seccion_resumen))
                agregarKpiRow(
                    res.optInt("ofertas_activas", 0),
                    res.optInt("ofertas_vencidas", 0),
                    res.optInt("total_postulaciones", 0)
                )

                val rankingArray = res.optJSONArray("ranking")
                val ranking = if (rankingArray != null) {
                    (0 until rankingArray.length()).map { rankingArray.getJSONObject(it) }
                } else emptyList()

                if (ranking.isNotEmpty()) {
                    agregarTituloSeccion(getString(R.string.s8_seccion_ranking))
                    agregarRanking(ranking)
                }

                val estadosArray = res.optJSONArray("estados")
                val estados = if (estadosArray != null) {
                    (0 until estadosArray.length()).map { estadosArray.getJSONObject(it) }
                } else emptyList()

                if (estados.isNotEmpty()) {
                    agregarTituloSeccion(getString(R.string.s8_seccion_estados))
                    agregarEstados(estados)
                }

            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            } finally {
                mostrarCargando(false)
            }
        }
    }
}
