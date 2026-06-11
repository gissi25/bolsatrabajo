package sv.ues.fia.eisi.bt.ui.servicios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.ArrayAdapter
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
import sv.ues.fia.eisi.bt.utils.setupMarqueeTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Servicio4Fragment : Fragment() {

    private lateinit var spTipo: MaterialAutoCompleteTextView
    private lateinit var tilTipo: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var spVigencia: MaterialAutoCompleteTextView
    private lateinit var tilVigencia: TextInputLayout
    private lateinit var btnBuscar: MaterialButton
    private lateinit var btnGuardarLocal: MaterialButton
    private lateinit var rvResultados: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultado: TextView

    private val resultados = mutableListOf<JSONObject>()
    private lateinit var adapter: ResultadoAdapter

    private data class TipoCertItem(val id: Int, val nombre: String, val duracionVigenciaAnios: Int?) {
        override fun toString() = nombre
    }
    private var tipos = listOf<TipoCertItem>()

    private val opcionesVigencia = listOf("Todos", "Solo Vigentes", "Solo Vencidos")
    private var guardando = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio4, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spTipo = view.findViewById(R.id.spTipo); tilTipo = view.findViewById(R.id.tilTipo)
        etNombre = view.findViewById(R.id.etNombre)
        spVigencia = view.findViewById(R.id.spVigencia); tilVigencia = view.findViewById(R.id.tilVigencia)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        btnGuardarLocal = view.findViewById(R.id.btnGuardarLocal)
        rvResultados = view.findViewById(R.id.rvResultados)
        progressBar = view.findViewById(R.id.progressBar)
        tvResultado = view.findViewById(R.id.tvResultado)

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setupMarqueeTitle()
        }

        adapter = ResultadoAdapter(resultados) { mostrarDetalle(it) }
        rvResultados.layoutManager = LinearLayoutManager(requireContext())
        rvResultados.adapter = adapter

        spTipo.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < tipos.size) spTipo.setTag(tipos[pos].id.toString())
        }

        spVigencia.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opcionesVigencia))
        spVigencia.setThreshold(0)
        spVigencia.setText(opcionesVigencia[0], false)
        tilVigencia.setOnClickListener { spVigencia.showDropDown() }

        btnBuscar.setOnClickListener { buscar() }
        btnGuardarLocal.setOnClickListener { guardarEnLocal() }

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
            tipos = json.map {
                TipoCertItem(
                    it.getInt("ID_TIPO_CERTIFICACION"),
                    it.getString("NOMBRE_TIPO"),
                    if (it.has("DURACION_VIGENCIA_ANIOS") && !it.isNull("DURACION_VIGENCIA_ANIOS"))
                        it.getInt("DURACION_VIGENCIA_ANIOS") else null
                )
            }
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
                lista.add(TipoCertItem(cursor.getInt(0), cursor.getString(1), null))
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
        val vigenteIdx = opcionesVigencia.indexOf(spVigencia.text?.toString())
        val vigenteParam = when (vigenteIdx) {
            1 -> "vigentes"
            2 -> "vencidos"
            else -> "todos"
        }

        btnBuscar.isEnabled = false; btnBuscar.text = getString(R.string.s4_cargando)
        progressBar.visibility = View.VISIBLE
        tvResultado.visibility = View.GONE
        btnGuardarLocal.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.Main) {
                    ApiService.buscarCertificaciones(tipoId, nombre, vigenteParam)
                }

                val data = json.optJSONArray("data")
                resultados.clear()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        resultados.add(data.getJSONObject(i))
                    }
                }
                adapter.notifyDataSetChanged()

                if (resultados.isNotEmpty()) {
                    tvResultado.text = getString(R.string.s4_resultados, resultados.size)
                    tvResultado.visibility = View.VISIBLE
                    btnGuardarLocal.visibility = View.VISIBLE
                } else {
                    tvResultado.text = getString(R.string.s4_sin_resultados)
                    tvResultado.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message?.take(300)}", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            btnBuscar.isEnabled = true; btnBuscar.text = getString(R.string.s4_btn_buscar)
        }
    }

    private fun guardarEnLocal() {
        if (resultados.isEmpty() || guardando) {
            Log.w("Servicio4", "guardarEnLocal bloqueado: empty=${resultados.isEmpty()}, guardando=$guardando")
            return
        }
        guardando = true
        btnGuardarLocal.isEnabled = false
        btnGuardarLocal.text = getString(R.string.s4_guardando)

        val aGuardar = resultados.toList()

        lifecycleScope.launch {
            try {
                val (postulantes, certificaciones) = withContext(Dispatchers.IO) {
                    guardarEnLocalSync(aGuardar)
                }
                if (certificaciones > 0) {
                    Snackbar.make(requireView(), R.string.s4_guardado_ok, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(requireView(), "No se guardo nada", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Servicio4", "Error al guardar en local", e)
                Snackbar.make(requireView(), "Error: ${e.message?.take(200)}", Snackbar.LENGTH_LONG).show()
            } finally {
                guardando = false
                btnGuardarLocal.isEnabled = true
                btnGuardarLocal.text = getString(R.string.s4_guardar_local)
            }
        }
    }

    private fun guardarEnLocalSync(lista: List<JSONObject>): Pair<Int, Int> {
        val ctx = requireContext()
        val dbHelper = ConnectionHelper(ctx)
        val db = dbHelper.writableDb
        var postulantesCount = 0
        var certificacionesCount = 0

        db.execSQL("PRAGMA foreign_keys = OFF")
        db.beginTransaction()
        try {
            for (postulante in lista) {
                val idPost = postulante.optString("id_postulante", "")
                if (idPost.isBlank()) continue

                val idGenero = postulante.optInt("id_genero", 1)
                val idTipoDoc = postulante.optInt("id_tipo_documento", 1)
                val idGrado = postulante.optInt("id_grado_academico", 1)

                val numDoc = postulante.optString("num_documento", "")
                val nombre = postulante.optString("nombre", "").lowercase()
                val apellido = postulante.optString("apellido", "").lowercase()
                val fechaNac = postulante.optString("fecha_nacimiento", "").ifBlank { null }
                val nup = postulante.optString("nup", "")
                val email = postulante.optString("email", "").lowercase()
                val dir = postulante.optString("direccion", "").lowercase()
                val telCasa = postulante.optString("telefono_casa", "")
                val telCel = postulante.optString("telefono_celular", "")
                val depto = postulante.opt("id_distrito_depto")?.let {
                    if (it is Int) it else it.toString().toIntOrNull()
                }
                val muni = postulante.opt("id_distrito_municipio")?.let {
                    if (it is Int) it else it.toString().toIntOrNull()
                }
                val dist = postulante.opt("id_distrito_id")?.let {
                    if (it is Int) it else it.toString().toIntOrNull()
                }

                val certs = postulante.optJSONArray("certificaciones")

                db.execSQL("""
                    INSERT OR IGNORE INTO POSTULANTE
                    (ID_POSTULANTE, ID_GENERO, ID_TIPO_DOCUMENTO, NUM_DOCUMENTO,
                     ID_GRADO_ACADEMICO, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUP, EMAIL,
                     ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID,
                     DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                            ?, ?, ?, ?, ?, ?)
                """, arrayOf(
                    idPost, idGenero, idTipoDoc, numDoc, idGrado,
                    nombre, apellido, fechaNac, nup, email,
                    depto, muni, dist, dir, telCasa, telCel
                ))

                db.execSQL("""
                    UPDATE POSTULANTE SET
                        ID_GENERO=?, ID_TIPO_DOCUMENTO=?, NUM_DOCUMENTO=?,
                        ID_GRADO_ACADEMICO=?, NOMBRE=?, APELLIDO=?,
                        FECHA_NACIMIENTO=?, NUP=?, EMAIL=?,
                        ID_DISTRITO_DEPTO=?, ID_DISTRITO_MUNICIPIO=?, ID_DISTRITO_ID=?,
                        DIRECCION_DETALLE=?, TELEFONO_CASA=?, TELEFONO_CELULAR=?
                    WHERE ID_POSTULANTE=?
                """, arrayOf(
                    idGenero, idTipoDoc, numDoc, idGrado,
                    nombre, apellido, fechaNac, nup, email,
                    depto, muni, dist, dir, telCasa, telCel,
                    idPost
                ))

                if (certs != null) {
                    for (j in 0 until certs.length()) {
                        val c = certs.getJSONObject(j)
                        val idCert = c.optString("id_certificacion", "")
                        val idInst = c.optString("id_institucion", "")
                        val idTipoCert = c.optInt("id_tipo_certificacion", 0)
                        val nomCert = c.optString("nombre", "")
                        val fecCert = c.optString("fecha_certificacion", "")
                        val fecIni = c.optString("fecha_inicio", "")
                        val fecFin = c.optString("fecha_fin", "")
                        if (idCert.isBlank() || idInst.isBlank() || idTipoCert <= 0 || nomCert.isBlank()) continue

                        db.execSQL("""
                            INSERT OR IGNORE INTO CERTIFICACION
                            (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE,
                             ID_TIPO_CERTIFICACION, NOMBRE_CERTIFICACION,
                             FECHA_CERTIFICACION, FECHA_INICIO, FECHA_FIN)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, arrayOf(idCert, idInst, idPost, idTipoCert, nomCert.lowercase(), fecCert, fecIni, fecFin))

                        db.execSQL("""
                            UPDATE CERTIFICACION SET
                                ID_TIPO_CERTIFICACION=?, NOMBRE_CERTIFICACION=?,
                                FECHA_CERTIFICACION=?, FECHA_INICIO=?, FECHA_FIN=?
                            WHERE ID_CERTIFICACION=? AND ID_INSTITUCION=? AND ID_POSTULANTE=?
                        """, arrayOf(idTipoCert, nomCert.lowercase(), fecCert, fecIni, fecFin, idCert, idInst, idPost))
                        certificacionesCount++
                    }
                }
                postulantesCount++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.execSQL("PRAGMA foreign_keys = ON")
            db.close()
        }
        return Pair(postulantesCount, certificacionesCount)
    }

    private fun mostrarDetalle(postulante: JSONObject) {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_certificacion_detalle, null)

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
                certText.append("- ${c.optString("nombre")}")
                val inst = c.optString("institucion", "")
                if (inst.isNotEmpty()) certText.append(" — $inst")
                val anio = c.optString("anio", "")
                if (anio.isNotEmpty()) certText.append(" ($anio)")
                val vigente = c.opt("vigente")
                val fv = c.optString("fecha_vencimiento", "")
                if (vigente != null && !c.isNull("vigente")) {
                    val estado = if (vigente is Boolean && vigente) "VIGENTE" else "VENCIDA"
                    certText.append(" [$estado")
                    if (fv.isNotEmpty()) certText.append(" - ${getString(R.string.s4_fecha_vencimiento)} $fv")
                    certText.append("]")
                }
                certText.append("\n")
            }
        }
        view.findViewById<TextView>(R.id.tvDialogCert).text =
            if (certText.isNotEmpty()) certText.trim().toString() else getString(R.string.s3_sin_datos)

        AlertDialog.Builder(context)
            .setView(view)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private inner class ResultadoAdapter(
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
            var totalVigentes = 0
            var totalVencidas = 0
            if (certs != null) {
                for (i in 0 until certs.length()) {
                    val c = certs.getJSONObject(i)
                    sb.append("- ${c.optString("nombre")}")
                    val inst = c.optString("institucion", "")
                    if (inst.isNotEmpty()) sb.append(" — $inst")
                    val anio = c.optString("anio", "")
                    if (anio.isNotEmpty()) sb.append(" ($anio)")
                    val vigente = c.opt("vigente")
                    if (vigente is Boolean) {
                        if (vigente) totalVigentes++ else totalVencidas++
                    }
                    if (i < certs.length() - 1) sb.append("\n")
                }
            }
            h.tvCertificaciones.text = sb.toString()

            if (totalVigentes > 0 || totalVencidas > 0) {
                h.llEstado.visibility = View.VISIBLE
                val todasVigentes = totalVencidas == 0
                h.tvEstado.text = if (todasVigentes) {
                    h.itemView.context.getString(R.string.s4_vigente)
                } else {
                    h.itemView.context.getString(R.string.s4_vencida)
                }
                    h.tvEstado.setBackgroundColor(
                        if (todasVigentes) android.graphics.Color.parseColor("#4CAF50")
                        else android.graphics.Color.parseColor("#F44336")
                    )
                    h.tvEstado.setTextColor(android.graphics.Color.WHITE)

                val partes = mutableListOf<String>()
                if (totalVigentes > 0) partes.add("$totalVigentes vigente(s)")
                if (totalVencidas > 0) partes.add("$totalVencidas vencida(s)")
                h.tvFechaVenc.text = partes.joinToString(", ")
            } else {
                h.llEstado.visibility = View.GONE
            }

            h.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNombre: TextView = itemView.findViewById(R.id.tvCardNombre)
            val tvEmail: TextView = itemView.findViewById(R.id.tvCardEmail)
            val tvGrado: TextView = itemView.findViewById(R.id.tvCardGrado)
            val tvCertCount: TextView = itemView.findViewById(R.id.tvCardCertCount)
            val tvCertificaciones: TextView = itemView.findViewById(R.id.tvCardCertificaciones)
            val llEstado: View = itemView.findViewById(R.id.llCardEstado)
            val tvEstado: TextView = itemView.findViewById(R.id.tvCardEstado)
            val tvFechaVenc: TextView = itemView.findViewById(R.id.tvCardFechaVenc)
        }
    }
}
