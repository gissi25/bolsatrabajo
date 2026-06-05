package sv.ues.fia.eisi.bt.ui.servicios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio4Fragment : Fragment() {

    private lateinit var spTipo: MaterialAutoCompleteTextView
    private lateinit var tilTipo: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var etAnio: TextInputEditText
    private lateinit var btnBuscar: MaterialButton
    private lateinit var rvResultados: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultado: TextView

    private val resultados = mutableListOf<JSONObject>()
    private lateinit var adapter: ResultadoAdapter

    private data class TipoCertItem(val id: Int, val nombre: String) {
        override fun toString() = nombre
    }
    private var tipos = listOf<TipoCertItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio4, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spTipo = view.findViewById(R.id.spTipo); tilTipo = view.findViewById(R.id.tilTipo)
        etNombre = view.findViewById(R.id.etNombre)
        etAnio = view.findViewById(R.id.etAnio)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        rvResultados = view.findViewById(R.id.rvResultados)
        progressBar = view.findViewById(R.id.progressBar)
        tvResultado = view.findViewById(R.id.tvResultado)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = ResultadoAdapter(resultados) { mostrarDetalle(it) }
        rvResultados.layoutManager = LinearLayoutManager(requireContext())
        rvResultados.adapter = adapter

        spTipo.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < tipos.size) spTipo.setTag(tipos[pos].id.toString())
        }

        btnBuscar.setOnClickListener { buscar() }

        lifecycleScope.launch {
            for (attempt in 1..3) {
                progressBar.visibility = View.VISIBLE
                if (cargarTiposRemotos()) break
                progressBar.visibility = View.GONE
                if (attempt < 3) delay(3000)
            }
            progressBar.visibility = View.GONE
        }
    }

    private suspend fun cargarTiposRemotos(): Boolean {
        return try {
            val json = ApiService.getTiposCertificacion()
            tipos = json.map { TipoCertItem(it.getInt("ID_TIPO_CERTIFICACION"), it.getString("NOMBRE_TIPO")) }
            spTipo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos))
            spTipo.setThreshold(0); tilTipo.setOnClickListener { spTipo.showDropDown() }
            true
        } catch (e: Exception) {
            cargarTiposLocales()
        }
    }

    private fun cargarTiposLocales(): Boolean {
        return try {
            val dbHelper = ConnectionHelper(requireContext())
            val db = dbHelper.writableDb
            val cursor = db.rawQuery("SELECT ID_TIPO_CERTIFICACION, NOMBRE_TIPO FROM TIPO_CERTIFICACION ORDER BY NOMBRE_TIPO", null)
            val lista = mutableListOf<TipoCertItem>()
            while (cursor.moveToNext()) {
                lista.add(TipoCertItem(cursor.getInt(0), cursor.getString(1)))
            }
            cursor.close(); db.close()
            tipos = lista
            spTipo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos))
            spTipo.setThreshold(0); tilTipo.setOnClickListener { spTipo.showDropDown() }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buscar() {
        val tipoId = spTipo.tag?.toString()?.toIntOrNull()
        if (tipoId == null) {
            Snackbar.make(requireView(), getString(R.string.s4_error_tipo), Snackbar.LENGTH_LONG).show()
            return
        }

        val nombre = etNombre.text?.toString()?.trim()
        val anio = etAnio.text?.toString()?.toIntOrNull()

        btnBuscar.isEnabled = false; btnBuscar.text = getString(R.string.s4_cargando)
        progressBar.visibility = View.VISIBLE
        tvResultado.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Paso 1: leer datos locales que coinciden con el filtro
                val locales = withContext(Dispatchers.IO) { queryLocales(tipoId, nombre, anio) }

                if (locales.isNotEmpty()) {
                    // Paso 2: sincronizar al servidor
                    val body = JSONObject().apply { put("postulantes", JSONArray(locales)) }
                    try {
                        ApiService.sincronizarCertificaciones(body)
                    } catch (_: Exception) { }
                }

                // Paso 3: intentar consultar servidor remoto
                var datosMostrar = locales
                try {
                    val result = ApiService.buscarCertificaciones(tipoId, nombre, anio)
                    val arr = result.optJSONArray("data")
                    if (arr != null && arr.length() > 0) {
                        datosMostrar = (0 until arr.length()).map { arr.getJSONObject(it) }
                    }
                } catch (_: Exception) { }

                resultados.clear()
                resultados.addAll(datosMostrar)
                adapter.notifyDataSetChanged()

                if (resultados.isNotEmpty()) {
                    tvResultado.text = getString(R.string.s4_resultados, resultados.size)
                    tvResultado.visibility = View.VISIBLE
                } else {
                    tvResultado.text = getString(R.string.s4_sin_resultados)
                    tvResultado.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            btnBuscar.isEnabled = true; btnBuscar.text = getString(R.string.s4_btn_buscar)
        }
    }

    private fun queryLocales(tipoId: Int, nombre: String?, anio: Int?): List<JSONObject> {
        val dbHelper = ConnectionHelper(requireContext())
        val db = dbHelper.writableDb
        val postulantesMap = linkedMapOf<String, JSONObject>()

        try {
            var sql = """
                SELECT p.ID_POSTULANTE, p.ID_GENERO, p.ID_TIPO_DOCUMENTO, p.ID_GRADO_ACADEMICO,
                       p.ID_DISTRITO_DEPTO, p.ID_DISTRITO_MUNICIPIO, p.ID_DISTRITO_ID,
                       p.NOMBRE, p.APELLIDO, p.EMAIL, p.NUM_DOCUMENTO, p.NUP,
                       p.FECHA_NACIMIENTO, p.DIRECCION_DETALLE, p.TELEFONO_CASA, p.TELEFONO_CELULAR,
                       ga.NOMBRE_GRADO,
                       c.ID_CERTIFICACION, c.ID_INSTITUCION, c.ID_TIPO_CERTIFICACION,
                       c.NOMBRE_CERTIFICACION, c.FECHA_CERTIFICACION, c.FECHA_INICIO, c.FECHA_FIN,
                       tc.NOMBRE_TIPO, i.NOMBRE_INSTITUCION
                FROM POSTULANTE p
                JOIN CERTIFICACION c ON p.ID_POSTULANTE = c.ID_POSTULANTE
                LEFT JOIN TIPO_CERTIFICACION tc ON c.ID_TIPO_CERTIFICACION = tc.ID_TIPO_CERTIFICACION
                LEFT JOIN INSTITUCION i ON c.ID_INSTITUCION = i.ID_INSTITUCION
                LEFT JOIN GRADO_ACADEMICO ga ON p.ID_GRADO_ACADEMICO = ga.ID_GRADO_ACADEMICO
                WHERE c.ID_TIPO_CERTIFICACION = ?
            """
            val args = mutableListOf<String>()
            args.add(tipoId.toString())

            if (!nombre.isNullOrBlank()) {
                sql += " AND c.NOMBRE_CERTIFICACION LIKE ?"
                args.add("%$nombre%")
            }

            if (anio != null && anio > 0) {
                sql += " AND c.FECHA_CERTIFICACION >= ? AND c.FECHA_CERTIFICACION <= ?"
                args.add("$anio-01-01")
                args.add("$anio-12-31")
            }

            sql += " ORDER BY p.ID_POSTULANTE, c.FECHA_CERTIFICACION DESC"

            val cursor = db.rawQuery(sql, args.toTypedArray())
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val postulante = postulantesMap.getOrPut(id) {
                    JSONObject().apply {
                        put("id_postulante", id)
                        put("id_genero", cursor.getInt(1).coerceAtLeast(1))
                        put("id_tipo_documento", cursor.getInt(2).coerceAtLeast(1))
                        put("id_grado_academico", cursor.getInt(3).coerceAtLeast(1))
                        put("id_distrito_depto", cursor.getString(4)?.toIntOrNull())
                        put("id_distrito_municipio", cursor.getString(5)?.toIntOrNull())
                        put("id_distrito_id", cursor.getString(6)?.toIntOrNull())
                        put("nombre", cursor.getString(7) ?: "")
                        put("apellido", cursor.getString(8) ?: "")
                        put("email", cursor.getString(9) ?: "")
                        put("num_documento", cursor.getString(10) ?: "")
                        put("nup", cursor.getString(11) ?: "")
                        put("fecha_nacimiento", cursor.getString(12) ?: "")
                        put("direccion", cursor.getString(13) ?: "")
                        put("telefono_casa", cursor.getString(14) ?: "")
                        put("telefono_celular", cursor.getString(15) ?: "")
                        put("grado", cursor.getString(16) ?: "")
                        put("certificaciones", JSONArray())
                    }
                }
                val certs = postulante.getJSONArray("certificaciones")
                certs.put(JSONObject().apply {
                    put("id_certificacion", cursor.getString(17) ?: "")
                    put("id_institucion", cursor.getString(18) ?: "")
                    put("id_tipo_certificacion", cursor.getInt(19))
                    put("nombre", cursor.getString(20) ?: "")
                    val fecha = cursor.getString(21) ?: ""
                    put("fecha_certificacion", fecha)
                    put("anio", if (fecha.length >= 4) fecha.substring(0, 4) else "")
                    put("fecha_inicio", cursor.getString(22) ?: "")
                    put("fecha_fin", cursor.getString(23) ?: "")
                    put("tipo", cursor.getString(24) ?: "")
                    put("institucion", cursor.getString(25) ?: "")
                })
            }
            cursor.close()
        } catch (e: Exception) {
            throw e
        } finally {
            db.close()
        }

        return postulantesMap.values.toList()
    }

    private fun mostrarDetalle(postulante: JSONObject) {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_postulante_detalle, null)

        view.findViewById<TextView>(R.id.tvDialogTitle).text = "${postulante.optString("nombre")} ${postulante.optString("apellido")}"
        view.findViewById<TextView>(R.id.tvDialogNombre).text = "${postulante.optString("nombre")} ${postulante.optString("apellido")}"
        view.findViewById<TextView>(R.id.tvDialogEmail).text = postulante.optString("email", "")
        view.findViewById<TextView>(R.id.tvDialogNup).text = "ID: ${postulante.optString("id_postulante", "")}"
        view.findViewById<TextView>(R.id.tvDialogDoc).text = "Grado: ${postulante.optString("grado", "")}"

        val certs = postulante.optJSONArray("certificaciones")
        val certText = StringBuilder()
        if (certs != null) {
            for (i in 0 until certs.length()) {
                val c = certs.getJSONObject(i)
                certText.append("📜 ${c.optString("nombre")}")
                val inst = c.optString("institucion", "")
                if (inst.isNotEmpty()) certText.append(" — $inst")
                val anio = c.optString("anio", "")
                if (anio.isNotEmpty()) certText.append(" ($anio)")
                certText.append("\n")
            }
        }
        view.findViewById<TextView>(R.id.tvDialogCert).text =
            if (certText.isNotEmpty()) certText.trim().toString() else getString(R.string.s3_sin_datos)

        view.findViewById<TextView>(R.id.tvDialogFormacion).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvDialogExp).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvDialogHabilidades).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvDialogRedes).visibility = View.GONE

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(view)
            .setNeutralButton(getString(R.string.s4_sincronizar)) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val body = JSONObject().apply {
                            put("postulantes", JSONArray(listOf(postulante)))
                        }
                        ApiService.sincronizarCertificaciones(body)
                        Snackbar.make(requireView(), R.string.s4_sincronizado_ok, Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private class ResultadoAdapter(
        private val items: List<JSONObject>,
        private val onClick: (JSONObject) -> Unit
    ) : RecyclerView.Adapter<ResultadoAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_certificacion_resultado, p, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.tvNombre.text = "${item.optString("nombre")} ${item.optString("apellido")}".trim()
            h.tvEmail.text = item.optString("email", "")
            h.tvGrado.text = item.optString("grado", "")
            val certs = item.optJSONArray("certificaciones")
            val total = certs?.length() ?: 0
            h.tvCertCount.text = h.itemView.context.getString(R.string.s4_total_cert, total)

            val sb = StringBuilder()
            if (certs != null) {
                for (i in 0 until certs.length()) {
                    val c = certs.getJSONObject(i)
                    sb.append("📜 ${c.optString("nombre")}")
                    val inst = c.optString("institucion", "")
                    if (inst.isNotEmpty()) sb.append(" — $inst")
                    val anio = c.optString("anio", "")
                    if (anio.isNotEmpty()) sb.append(" ($anio)")
                    if (i < certs.length() - 1) sb.append("\n")
                }
            }
            h.tvCertificaciones.text = sb.toString()
            h.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNombre: TextView = itemView.findViewById(R.id.tvCardNombre)
            val tvEmail: TextView = itemView.findViewById(R.id.tvCardEmail)
            val tvGrado: TextView = itemView.findViewById(R.id.tvCardGrado)
            val tvCertCount: TextView = itemView.findViewById(R.id.tvCardCertCount)
            val tvCertificaciones: TextView = itemView.findViewById(R.id.tvCardCertificaciones)
        }
    }
}
