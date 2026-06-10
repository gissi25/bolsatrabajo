package sv.ues.fia.eisi.bt.ui.servicios

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.setupMarqueeTitle

class Servicio7Fragment : Fragment() {

    private lateinit var tilPostulante: TextInputLayout
    private lateinit var etPostulante: TextInputEditText
    private lateinit var etEdad: TextInputEditText
    private lateinit var btnBuscar: MaterialButton
    private lateinit var rvOfertas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinResultados: TextView

    private val ofertasList = mutableListOf<JSONObject>()
    private lateinit var adapter: OfertaAdapter
    private var esAdmin: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio7, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val rol = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_POSTULANTE) ?: Constants.ROLE_POSTULANTE
        esAdmin = rol == Constants.ROLE_ADMIN

        tilPostulante = view.findViewById(R.id.tilPostulante)
        etPostulante = view.findViewById(R.id.etPostulante)
        etEdad = view.findViewById(R.id.etEdad)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        rvOfertas = view.findViewById(R.id.rvOfertas)
        progressBar = view.findViewById(R.id.progressBar)
        tvSinResultados = view.findViewById(R.id.tvSinResultados)

        if (esAdmin) {
            tilPostulante.visibility = View.GONE
        }

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setupMarqueeTitle()
        }

        adapter = OfertaAdapter(ofertasList, { oferta -> mostrarDetalle(oferta) }, requireContext(), !esAdmin)
        rvOfertas.layoutManager = LinearLayoutManager(requireContext())
        rvOfertas.adapter = adapter

        btnBuscar.setOnClickListener { buscarOfertas() }

        etEdad.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                buscarOfertas()
                true
            } else false
        }
    }

    private fun buscarOfertas() {
        val edadStr = etEdad.text?.toString()?.trim()
        val edad = edadStr?.toIntOrNull()
        val idPostulante = if (esAdmin) "" else (etPostulante.text?.toString()?.trim() ?: "")

        if (edad == null || edad < 1 || edad > 120) {
            Snackbar.make(requireView(), getString(R.string.s7_error_edad), Snackbar.LENGTH_LONG).show()
            return
        }

        btnBuscar.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvSinResultados.visibility = View.GONE
        ofertasList.clear()
        adapter.notifyDataSetChanged()
        etEdad.clearFocus()

        lifecycleScope.launch {
            try {
                val resultados = ApiService.buscarOfertasPorEdad(edad, idPostulante)
                ofertasList.addAll(resultados)
                adapter.notifyDataSetChanged()
                if (ofertasList.isEmpty()) {
                    tvSinResultados.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnBuscar.isEnabled = true
            }
        }
    }

    private fun mostrarDetalle(oferta: JSONObject) {
        val idPostulante = if (esAdmin) "" else (etPostulante.text?.toString()?.trim() ?: "")

        val dialogView = layoutInflater.inflate(R.layout.dialog_oferta_detalle, null)

        dialogView.findViewById<TextView>(R.id.tvDetalleTitulo).text = oferta.optString("TITULO_PUESTO", "")
        dialogView.findViewById<TextView>(R.id.tvDetalleEmpresa).text = oferta.optString("NOMBRE_EMPRESA", "")
        dialogView.findViewById<TextView>(R.id.tvDetalleGrado).text = oferta.optString("GRADO_REQUERIDO", "—")
        dialogView.findViewById<TextView>(R.id.tvDetalleExp).text = "${oferta.optString("EXPERIENCIA_ANIOS", "0")} años"
        dialogView.findViewById<TextView>(R.id.tvDetalleEdadMin).text = oferta.optInt("EDAD_MINIMA", 0).toString()
        dialogView.findViewById<TextView>(R.id.tvDetalleEdadMax).text = oferta.optInt("EDAD_MAXIMA", 0).toString()
        dialogView.findViewById<TextView>(R.id.tvDetallePub).text = oferta.optString("FECHA_PUBLICACION", "").take(10)
        dialogView.findViewById<TextView>(R.id.tvDetalleCad).text = oferta.optString("FECHA_CADUCIDAD", "").take(10)
        dialogView.findViewById<TextView>(R.id.tvDetalleDesc).text = oferta.optString("DESCRIPCION_OFERTA_TRABAJO", "Sin descripción")

        val btnPostular = dialogView.findViewById<MaterialButton>(R.id.btnPostularme)
        if (!esAdmin && idPostulante.isNotEmpty()) {
            btnPostular.visibility = View.VISIBLE
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
            btnPostular.setOnClickListener { postular(oferta, dialogView, dialog) }
        } else {
            btnPostular.visibility = View.GONE
            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun postular(oferta: JSONObject, dialogView: View, dialog: AlertDialog) {
        val idPostulante = etPostulante.text?.toString()?.trim() ?: ""
        val nit = oferta.optString("NIT", "")
        val idOferta = oferta.optString("ID_OFERTA", "")

        if (idPostulante.isEmpty()) {
            Snackbar.make(requireView(), getString(R.string.s7_error_postulante), Snackbar.LENGTH_LONG).show()
            return
        }

        dialogView.findViewById<MaterialButton>(R.id.btnPostularme).isEnabled = false
        dialogView.findViewById<MaterialButton>(R.id.btnPostularme).text = getString(R.string.s7_postulando)

        lifecycleScope.launch {
            try {
                val res = ApiService.postularOferta(idPostulante, nit, idOferta)
                dialog.dismiss()
                if (res.optBoolean("exito", false)) {
                    Snackbar.make(requireView(), getString(R.string.s7_postulacion_exitosa), Snackbar.LENGTH_SHORT).show()
                    buscarOfertas()
                } else {
                    throw Exception(res.optString("error", "Error"))
                }
            } catch (e: Exception) {
                dialog.dismiss()
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private class OfertaAdapter(
        private val items: List<JSONObject>,
        private val onClick: (JSONObject) -> Unit,
        private val context: android.content.Context,
        private val mostrarChips: Boolean = false
    ) : RecyclerView.Adapter<OfertaAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_oferta_edad, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.itemView.setOnClickListener { onClick(item) }

            h.tvEmpresa.text = item.optString("NOMBRE_EMPRESA", "")
            h.tvTitulo.text = item.optString("TITULO_PUESTO", "")

            val grado = item.optString("GRADO_REQUERIDO", "")
            h.tvGrado.text = if (grado.isNotEmpty()) "${context.getString(R.string.s7_grado)}: $grado" else ""

            val exp = item.optString("EXPERIENCIA_ANIOS", "")
            h.tvExperiencia.text = if (exp.isNotEmpty()) "${context.getString(R.string.s7_experiencia)}: $exp años" else ""

            val edadMin = item.optInt("EDAD_MINIMA", 0)
            val edadMax = item.optInt("EDAD_MAXIMA", 0)
            h.tvEdadRango.text = "${context.getString(R.string.s7_edad_minima)} $edadMin - ${context.getString(R.string.s7_edad_maxima)} $edadMax"

            val pub = item.optString("FECHA_PUBLICACION", "").take(10)
            val cad = item.optString("FECHA_CADUCIDAD", "").take(10)
            h.tvFechas.text = "${context.getString(R.string.s7_publicacion)}: $pub  |  ${context.getString(R.string.s7_caducidad)}: $cad"

            h.tvEstadoChip.visibility = View.GONE
            if (mostrarChips) {
                val chip = h.tvEstadoChip
                val estado = normalizarEstadoPostulacion(item.opt("ESTADO_POSTULACION"))
                chip.visibility = View.VISIBLE
                when (estado) {
                    "" -> {
                        chip.text = context.getString(R.string.s7_chip_no_postulado)
                        chip.setTextColor(Color.parseColor("#616161"))
                        chip.setBackgroundColor(Color.parseColor("#E0E0E0"))
                    }
                    "activo", "pendiente" -> {
                        chip.text = context.getString(R.string.s7_chip_activo)
                        chip.setTextColor(Color.parseColor("#FFFFFF"))
                        chip.setBackgroundColor(Color.parseColor("#2196F3"))
                    }
                    "en proceso" -> {
                        chip.text = context.getString(R.string.s7_chip_en_proceso)
                        chip.setTextColor(Color.parseColor("#FFFFFF"))
                        chip.setBackgroundColor(Color.parseColor("#FF9800"))
                    }
                    "contratado" -> {
                        chip.text = context.getString(R.string.s7_chip_contratado)
                        chip.setTextColor(Color.parseColor("#FFFFFF"))
                        chip.setBackgroundColor(Color.parseColor("#4CAF50"))
                    }
                    "rechazado" -> {
                        chip.text = context.getString(R.string.s7_chip_rechazado)
                        chip.setTextColor(Color.parseColor("#FFFFFF"))
                        chip.setBackgroundColor(Color.parseColor("#F44336"))
                    }
                    else -> {
                        chip.text = estado.replaceFirstChar { it.uppercase() }
                        chip.setTextColor(Color.parseColor("#FFFFFF"))
                        chip.setBackgroundColor(Color.parseColor("#9E9E9E"))
                    }
                }
            }
        }

        override fun getItemCount() = items.size

        private fun normalizarEstadoPostulacion(raw: Any?): String {
            if (raw == null || raw == JSONObject.NULL) return ""
            return raw.toString().trim().lowercase()
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvEmpresa: TextView = itemView.findViewById(R.id.tvEmpresa)
            val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
            val tvGrado: TextView = itemView.findViewById(R.id.tvGrado)
            val tvExperiencia: TextView = itemView.findViewById(R.id.tvExperiencia)
            val tvEdadRango: TextView = itemView.findViewById(R.id.tvEdadRango)
            val tvFechas: TextView = itemView.findViewById(R.id.tvFechas)
            val tvEstadoChip: TextView = itemView.findViewById(R.id.tvEstadoChip)
        }
    }
}
