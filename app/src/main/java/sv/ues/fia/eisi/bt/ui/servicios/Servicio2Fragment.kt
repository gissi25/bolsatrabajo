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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio2Fragment : Fragment() {

    private lateinit var etIdPostulante: TextInputEditText
    private lateinit var btnAnalizar: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGrados: TextView
    private lateinit var tvInstituciones: TextView
    private lateinit var tvSkills: TextView
    private lateinit var tvSkillsFaltan: TextView
    private lateinit var tvMercado: TextView
    private lateinit var containerResultado: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etIdPostulante = view.findViewById(R.id.etIdPostulante)
        btnAnalizar = view.findViewById(R.id.btnAnalizar)
        progressBar = view.findViewById(R.id.progressBar)
        tvGrados = view.findViewById(R.id.tvGrados)
        tvInstituciones = view.findViewById(R.id.tvInstituciones)
        tvSkills = view.findViewById(R.id.tvSkills)
        tvSkillsFaltan = view.findViewById(R.id.tvSkillsFaltan)
        tvMercado = view.findViewById(R.id.tvMercado)
        containerResultado = view.findViewById(R.id.containerResultado)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        btnAnalizar.setOnClickListener { analizar() }
    }

    private fun analizar() {
        val id = etIdPostulante.text?.toString()?.trim()
        if (id.isNullOrEmpty()) {
            Snackbar.make(requireView(), "Ingresá un ID de postulante", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnAnalizar.isEnabled = false
        containerResultado.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = ApiService.recomendarFormacion(id)
                if (result.optBoolean("exito", false)) {
                    mostrarResultado(result.getJSONObject("data"))
                } else {
                    Snackbar.make(requireView(), result.optString("error", "Error"), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            btnAnalizar.isEnabled = true
        }
    }

    private fun mostrarResultado(data: org.json.JSONObject) {
        val postulante = data.getJSONObject("postulante")

        val arrGrados = data.getJSONArray("grados_mas_demandados")
        val gradosText = StringBuilder("Grados mas demandados:\n")
        if (arrGrados.length() == 0) {
            gradosText.append("  (no hay ofertas activas aun)\n")
        } else {
            for (i in 0 until minOf(arrGrados.length(), 5)) {
                val g = arrGrados.getJSONObject(i)
                val marca = if (g.optBoolean("es_tu_grado", false)) " <-- tu grado" else ""
                gradosText.append("  ${g.getString("NOMBRE_GRADO")}: ${g.getInt("total_ofertas")} ofertas$marca\n")
            }
        }

        val arrInst = data.getJSONArray("instituciones_top")
        val instText = StringBuilder("Instituciones con mas egresados:\n")
        if (arrInst.length() == 0) {
            instText.append("  (sin datos aun)\n")
        } else {
            for (i in 0 until minOf(arrInst.length(), 5)) {
                val inst = arrInst.getJSONObject(i)
                instText.append("  ${inst.getString("NOMBRE_INSTITUCION")}: ${inst.getInt("total_egresados")} egresados\n")
            }
        }

        val arrFaltan = data.getJSONArray("skills_que_te_faltan")
        val faltanText = StringBuilder("Skills que te faltan (${arrFaltan.length()}):\n")
        if (arrFaltan.length() == 0) {
            faltanText.append("  Tenes todas las skills del sistema\n")
        } else {
            for (i in 0 until minOf(arrFaltan.length(), 10)) {
                val s = arrFaltan.getJSONObject(i)
                faltanText.append("  - ${s.getString("NOMBRE_HABILIDAD")} (${s.getString("NOMBRE_CATEGORIA")})\n")
            }
            if (arrFaltan.length() > 10) faltanText.append("  ... y ${arrFaltan.length() - 10} mas\n")
        }

        val arrComunes = data.getJSONArray("skills_mas_comunes")
        val comunesText = StringBuilder("Skills mas comunes entre postulantes:\n")
        if (arrComunes.length() == 0) {
            comunesText.append("  (sin datos)\n")
        } else {
            for (i in 0 until minOf(arrComunes.length(), 10)) {
                val s = arrComunes.getJSONObject(i)
                comunesText.append("  - ${s.getString("NOMBRE_HABILIDAD")}: ${s.getInt("total_postulantes")} postulantes\n")
            }
        }

        val mercado = data.getJSONObject("estadisticas_mercado")
        val mercadoText = buildString {
            append("Mercado: ${mercado.getInt("ofertas_activas")} ofertas activas")
            append(" | ${mercado.getInt("total_postulantes")} postulantes")
            append(" | ${mercado.getInt("total_empresas")} empresas")
            append("\nCompetencia: ${mercado.getString("competencia_promedio")}")
        }

        tvGrados.text = gradosText.trimEnd()
        tvInstituciones.text = instText.trimEnd()
        tvSkillsFaltan.text = faltanText.trimEnd()
        tvSkills.text = comunesText.trimEnd()
        tvMercado.text = mercadoText

        containerResultado.visibility = View.VISIBLE
    }
}
