package sv.ues.fia.eisi.bt.ui.servicios

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio3Fragment : Fragment() {

    private lateinit var btnRecuperar: MaterialButton
    private lateinit var btnSincronizar: MaterialButton
    private lateinit var rvPostulantes: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultado: TextView

    private val postulantesList = mutableListOf<PostulanteSync>()
    private lateinit var adapter: PostulanteAdapter

    data class PostulanteSync(
        val idPostulante: String,
        val json: JSONObject
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnRecuperar = view.findViewById(R.id.btnRecuperar)
        btnSincronizar = view.findViewById(R.id.btnSincronizar)
        rvPostulantes = view.findViewById(R.id.rvPostulantes)
        progressBar = view.findViewById(R.id.progressBar)
        tvResultado = view.findViewById(R.id.tvResultado)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = PostulanteAdapter(postulantesList) { post -> mostrarDetalle(post) }
        rvPostulantes.layoutManager = LinearLayoutManager(requireContext())
        rvPostulantes.adapter = adapter

        btnRecuperar.setOnClickListener { recuperarPostulantes() }
        btnSincronizar.setOnClickListener { sincronizar() }
    }

    private fun recuperarPostulantes() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE; btnRecuperar.isEnabled = false; tvResultado.visibility = View.GONE
            postulantesList.clear()
            try {
                val lista = withContext(Dispatchers.IO) { queryPostulantesLocales() }
                postulantesList.addAll(lista)
                adapter.notifyDataSetChanged()
                tvResultado.text = "Se encontraron ${lista.size} postulantes en la BD local"
                tvResultado.visibility = View.VISIBLE
                btnSincronizar.isEnabled = lista.isNotEmpty()
                btnSincronizar.text = "Subir ${lista.size} postulante${if (lista.size != 1) "s" else ""} al servidor"
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE; btnRecuperar.isEnabled = true
        }
    }

    private fun queryPostulantesLocales(): List<PostulanteSync> {
        val dbHelper = ConnectionHelper(requireContext())
        val db = dbHelper.writableDb
        val resultado = mutableListOf<PostulanteSync>()

        val cursor = db.rawQuery("SELECT ID_POSTULANTE FROM POSTULANTE", null)
        val ids = mutableListOf<String>()
        while (cursor.moveToNext()) ids.add(cursor.getString(0))
        cursor.close()

        for (id in ids) {
            val json = buildPostulanteJson(db, id)
            if (json != null) resultado.add(PostulanteSync(id, json))
        }
        db.close()
        return resultado
    }

    private fun buildPostulanteJson(db: SQLiteDatabase, id: String): JSONObject? {
        val cur = db.rawQuery("""
            SELECT p.*, g.NOMBRE_GENERO, td.NOMBRE_TIPO, ga.NOMBRE_GRADO,
                   dep.NOMBRE_DEPARTAMENTO, m.NOMBRE_MUNICIPIO, d.NOMBRE_DISTRITO
            FROM POSTULANTE p
            LEFT JOIN GENERO g ON p.ID_GENERO = g.ID_GENERO
            LEFT JOIN TIPO_DOCUMENTO td ON p.ID_TIPO_DOCUMENTO = td.ID_TIPO_DOCUMENTO
            LEFT JOIN GRADO_ACADEMICO ga ON p.ID_GRADO_ACADEMICO = ga.ID_GRADO_ACADEMICO
            LEFT JOIN DISTRITO d ON p.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO AND p.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO AND p.ID_DISTRITO_ID = d.ID_DISTRITO
            LEFT JOIN DEPARTAMENTO dep ON d.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO
            LEFT JOIN MUNICIPIO m ON d.ID_DEPARTAMENTO = m.ID_DEPARTAMENTO AND d.ID_MUNICIPIO = m.ID_MUNICIPIO
            WHERE p.ID_POSTULANTE = ?
        """, arrayOf(id))
        if (!cur.moveToFirst()) { cur.close(); return null }
        val json = JSONObject()
        try {
            json.put("id_postulante", cur.getString(cur.getColumnIndexOrThrow("ID_POSTULANTE")))
            json.put("id_genero", cur.getInt(cur.getColumnIndexOrThrow("ID_GENERO")))
            json.put("id_tipo_documento", cur.getInt(cur.getColumnIndexOrThrow("ID_TIPO_DOCUMENTO")))
            json.put("num_documento", cur.getString(cur.getColumnIndexOrThrow("NUM_DOCUMENTO")) ?: "")
            json.put("id_grado_academico", cur.getInt(cur.getColumnIndexOrThrow("ID_GRADO_ACADEMICO")))
            json.put("nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE")) ?: "")
            json.put("apellido", cur.getString(cur.getColumnIndexOrThrow("APELLIDO")) ?: "")
            json.put("fecha_nacimiento", cur.getString(cur.getColumnIndexOrThrow("FECHA_NACIMIENTO")) ?: "")
            json.put("nup", cur.getString(cur.getColumnIndexOrThrow("NUP")) ?: "")
            json.put("email", cur.getString(cur.getColumnIndexOrThrow("EMAIL")) ?: "")
            json.put("direccion", cur.getString(cur.getColumnIndexOrThrow("DIRECCION_DETALLE")) ?: "")
            json.put("telefono_casa", cur.getString(cur.getColumnIndexOrThrow("TELEFONO_CASA")) ?: "")
            json.put("telefono_celular", cur.getString(cur.getColumnIndexOrThrow("TELEFONO_CELULAR")) ?: "")
            // Distrito FK + nombres
            val distDepto = cur.getString(cur.getColumnIndexOrThrow("ID_DISTRITO_DEPTO"))?.toIntOrNull()
            val distMuni = cur.getString(cur.getColumnIndexOrThrow("ID_DISTRITO_MUNICIPIO"))?.toIntOrNull()
            val distId = cur.getString(cur.getColumnIndexOrThrow("ID_DISTRITO_ID"))?.toIntOrNull()
            if (distDepto != null) json.put("id_distrito_depto", distDepto)
            if (distMuni != null) json.put("id_distrito_municipio", distMuni)
            if (distId != null) json.put("id_distrito_id", distId)
            json.put("depto_nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE_DEPARTAMENTO")) ?: "")
            json.put("municipio_nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE_MUNICIPIO")) ?: "")
            json.put("distrito_nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE_DISTRITO")) ?: "")
            json.put("genero_nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE_GENERO")) ?: "")
            json.put("tipo_doc_nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE_TIPO")) ?: "")
            json.put("grado_nombre", cur.getString(cur.getColumnIndexOrThrow("NOMBRE_GRADO")) ?: "")
        } catch (e: Exception) { cur.close(); return null }
        cur.close()

        // Tablas hijas con nombres legibles
        json.put("formaciones", queryFormaciones(db, id))
        json.put("experiencias", queryExperiencias(db, id))
        json.put("certificaciones", queryCertificaciones(db, id))
        json.put("habilidades", queryHabilidades(db, id))
        json.put("redes", queryRedes(db, id))

        return json
    }

    private fun queryFormaciones(db: SQLiteDatabase, idPost: String): JSONArray {
        val arr = JSONArray()
        try {
            val c = db.rawQuery("""
                SELECT f.ID_FORMACION, f.ID_OFERTA_ACADEMICA, f.TITULO_OBTENIDO, f.FECHA_INICIO, f.FECHA_FIN, f.FECHA_OBTENCION,
                       i.NOMBRE_INSTITUCION, ga.NOMBRE_GRADO
                FROM FORMACION_ACADEMICA f
                LEFT JOIN OFERTA_ACADEMICA oa ON f.ID_OFERTA_ACADEMICA = oa.ID_OFERTA_ACADEMICA
                LEFT JOIN INSTITUCION i ON oa.ID_INSTITUCION = i.ID_INSTITUCION
                LEFT JOIN GRADO_ACADEMICO ga ON oa.ID_GRADO_ACADEMICO = ga.ID_GRADO_ACADEMICO
                WHERE f.ID_POSTULANTE = ?
            """, arrayOf(idPost))
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id_formacion", c.getString(0) ?: "")
                obj.put("id_oferta_academica", c.getString(1) ?: "")
                obj.put("titulo_obtenido", c.getString(2) ?: "")
                obj.put("fecha_inicio", c.getString(3) ?: "")
                obj.put("fecha_fin", c.getString(4) ?: "")
                obj.put("fecha_obtencion", c.getString(5) ?: "")
                obj.put("institucion_nombre", c.getString(6) ?: "")
                obj.put("grado_nombre", c.getString(7) ?: "")
                arr.put(obj)
            }
            c.close()
        } catch (_: Exception) {}
        return arr
    }

    private fun queryExperiencias(db: SQLiteDatabase, idPost: String): JSONArray {
        val arr = JSONArray()
        try {
            val c = db.rawQuery("""
                SELECT e.NIT, e.ID_EXPERIENCIA, e.PUESTO_TRABAJO, e.FECHA_INICIO, e.FECHA_FIN, e.DESCP_EXPERIENCIA_LABORAL, e.CONTACTO_REFERENCIA,
                       em.NOMBRE_EMPRESA
                FROM EXPERIENCIA_LABORAL e
                LEFT JOIN EMPRESA em ON e.NIT = em.NIT
                WHERE e.ID_POSTULANTE = ?
            """, arrayOf(idPost))
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("nit", c.getString(0) ?: "")
                obj.put("id_experiencia", c.getString(1) ?: "")
                obj.put("puesto_trabajo", c.getString(2) ?: "")
                obj.put("fecha_inicio", c.getString(3) ?: "")
                obj.put("fecha_fin", c.getString(4) ?: "")
                obj.put("descripcion", c.getString(5) ?: "")
                obj.put("contacto", c.getString(6) ?: "")
                obj.put("empresa_nombre", c.getString(7) ?: "")
                arr.put(obj)
            }
            c.close()
        } catch (_: Exception) {}
        return arr
    }

    private fun queryCertificaciones(db: SQLiteDatabase, idPost: String): JSONArray {
        val arr = JSONArray()
        try {
            val c = db.rawQuery("""
                SELECT c.ID_CERTIFICACION, c.ID_INSTITUCION, c.ID_TIPO_CERTIFICACION, c.NOMBRE_CERTIFICACION,
                       c.FECHA_CERTIFICACION, c.FECHA_INICIO, c.FECHA_FIN,
                       i.NOMBRE_INSTITUCION, tc.NOMBRE_TIPO
                FROM CERTIFICACION c
                LEFT JOIN INSTITUCION i ON c.ID_INSTITUCION = i.ID_INSTITUCION
                LEFT JOIN TIPO_CERTIFICACION tc ON c.ID_TIPO_CERTIFICACION = tc.ID_TIPO_CERTIFICACION
                WHERE c.ID_POSTULANTE = ?
            """, arrayOf(idPost))
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id_certificacion", c.getString(0) ?: "")
                obj.put("id_institucion", c.getString(1) ?: "")
                obj.put("id_tipo_certificacion", c.getString(2) ?: "")
                obj.put("nombre_certificacion", c.getString(3) ?: "")
                obj.put("fecha_certificacion", c.getString(4) ?: "")
                obj.put("fecha_inicio", c.getString(5) ?: "")
                obj.put("fecha_fin", c.getString(6) ?: "")
                obj.put("institucion_nombre", c.getString(7) ?: "")
                obj.put("tipo_nombre", c.getString(8) ?: "")
                arr.put(obj)
            }
            c.close()
        } catch (_: Exception) {}
        return arr
    }

    private fun queryHabilidades(db: SQLiteDatabase, idPost: String): JSONArray {
        val arr = JSONArray()
        try {
            val c = db.rawQuery("""
                SELECT hp.ID_CATEGORIA_HABILIDAD, hp.ID_HABILIDAD, hp.NIVEL_DESTREZA,
                       h.NOMBRE_HABILIDAD
                FROM HABILIDAD_POSTULANTE hp
                LEFT JOIN HABILIDAD h ON hp.ID_CATEGORIA_HABILIDAD = h.ID_CATEGORIA_HABILIDAD AND hp.ID_HABILIDAD = h.ID_HABILIDAD
                WHERE hp.ID_POSTULANTE = ?
            """, arrayOf(idPost))
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id_categoria_habilidad", c.getString(0) ?: "")
                obj.put("id_habilidad", c.getString(1) ?: "")
                obj.put("nivel_destreza", c.getString(2) ?: "")
                obj.put("habilidad_nombre", c.getString(3) ?: "")
                arr.put(obj)
            }
            c.close()
        } catch (_: Exception) {}
        return arr
    }

    private fun queryRedes(db: SQLiteDatabase, idPost: String): JSONArray {
        val arr = JSONArray()
        try {
            val c = db.rawQuery("""
                SELECT rp.ID_RED_SOCIAL, rp.URL_PERFIL, r.NOMBRE_RED
                FROM RED_SOCIAL_POSTULANTE rp
                LEFT JOIN RED_SOCIAL r ON rp.ID_RED_SOCIAL = r.ID_RED_SOCIAL
                WHERE rp.ID_POSTULANTE = ?
            """, arrayOf(idPost))
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id_red_social", c.getString(0) ?: "")
                obj.put("url_perfil", c.getString(1) ?: "")
                obj.put("red_nombre", c.getString(2) ?: "")
                arr.put(obj)
            }
            c.close()
        } catch (_: Exception) {}
        return arr
    }

    private fun mostrarDetalle(post: PostulanteSync) {
        val json = post.json
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_postulante_detalle, null)

        view.findViewById<TextView>(R.id.tvDialogTitle).text = "${json.optString("id_postulante")} - ${json.optString("nombre")} ${json.optString("apellido")}"
        view.findViewById<TextView>(R.id.tvDialogNombre).text = "${json.optString("nombre")} ${json.optString("apellido")} | ${json.optString("genero_nombre")}"
        view.findViewById<TextView>(R.id.tvDialogEmail).text = "${json.optString("email")}  |  NUP: ${json.optString("nup")}"
        view.findViewById<TextView>(R.id.tvDialogNup).text = "${json.optString("tipo_doc_nombre")}: ${json.optString("num_documento")}  |  ${json.optString("grado_nombre")}"
        view.findViewById<TextView>(R.id.tvDialogDoc).text = "Nac: ${json.optString("fecha_nacimiento")}"
        view.findViewById<TextView>(R.id.tvDialogFormacion).text = formatearLista(json.optJSONArray("formaciones"), "formaciones")

        // Add phone, address, distrito info to existing TextViews
        val extraInfo = buildString {
            val dir = json.optString("direccion", "")
            if (dir.isNotEmpty()) append("Dir: $dir\n")
            val telC = json.optString("telefono_casa", "")
            if (telC.isNotEmpty()) append("Casa: $telC  ")
            val telM = json.optString("telefono_celular", "")
            if (telM.isNotEmpty()) append("Cel: $telM")
            val dName = json.optString("depto_nombre", "")
            val mName = json.optString("municipio_nombre", "")
            val distName = json.optString("distrito_nombre", "")
            if (dName.isNotEmpty() || mName.isNotEmpty() || distName.isNotEmpty()) {
                append("\n$dName, $mName, $distName")
            }
        }
        val tvDoc = view.findViewById<TextView>(R.id.tvDialogDoc)
        if (extraInfo.isNotEmpty()) tvDoc.text = tvDoc.text.toString() + "\n" + extraInfo.trim()
        view.findViewById<TextView>(R.id.tvDialogExp).text = formatearLista(json.optJSONArray("experiencias"), "experiencias")
        view.findViewById<TextView>(R.id.tvDialogCert).text = formatearLista(json.optJSONArray("certificaciones"), "certificaciones")
        view.findViewById<TextView>(R.id.tvDialogHabilidades).text = formatearLista(json.optJSONArray("habilidades"), "habilidades")
        view.findViewById<TextView>(R.id.tvDialogRedes).text = formatearLista(json.optJSONArray("redes"), "redes")

        AlertDialog.Builder(context).setView(view).setPositiveButton("Cerrar", null).show()
    }

    private fun formatearLista(arr: JSONArray?, tipo: String): String {
        if (arr == null || arr.length() == 0) return getString(R.string.s3_sin_datos)
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            when (tipo) {
                "formaciones" -> {
                    val titulo = item.optString("titulo_obtenido", "—")
                    val inst = item.optString("institucion_nombre", "")
                    val grad = item.optString("grado_nombre", "")
                    val fechas = listOf(item.optString("fecha_inicio","").take(7), item.optString("fecha_fin","").take(7)).filter { it.isNotEmpty() }
                    sb.append("• $titulo")
                    if (inst.isNotEmpty()) sb.append(" — $inst")
                    if (grad.isNotEmpty()) sb.append(" ($grad)")
                    if (fechas.isNotEmpty()) sb.append(" [${fechas.joinToString(" - ")}]")
                    sb.append("\n")
                }
                "experiencias" -> {
                    val puesto = item.optString("puesto_trabajo", "—")
                    val empresa = item.optString("empresa_nombre", item.optString("nit", ""))
                    val fechas = listOf(item.optString("fecha_inicio","").take(7), item.optString("fecha_fin","").take(7)).filter { it.isNotEmpty() }
                    sb.append("• $puesto en $empresa")
                    if (fechas.isNotEmpty()) sb.append(" [${fechas.joinToString(" - ")}]")
                    sb.append("\n")
                }
                "certificaciones" -> {
                    val nombre = item.optString("nombre_certificacion", "—")
                    val inst = item.optString("institucion_nombre", "")
                    val tipo = item.optString("tipo_nombre", "")
                    sb.append("• $nombre")
                    if (inst.isNotEmpty()) sb.append(" — $inst")
                    if (tipo.isNotEmpty()) sb.append(" ($tipo)")
                    sb.append("\n")
                }
                "habilidades" -> {
                    val nom = item.optString("habilidad_nombre", item.optString("id_habilidad", ""))
                    val nivel = item.optString("nivel_destreza", "")
                    sb.append("• $nom")
                    if (nivel.isNotEmpty()) sb.append(" — $nivel")
                    sb.append("\n")
                }
                "redes" -> {
                    val red = item.optString("red_nombre", "Red ${item.optString("id_red_social", "")}")
                    val url = item.optString("url_perfil", "")
                    sb.append("• $red")
                    if (url.isNotEmpty()) sb.append(": $url")
                    sb.append("\n")
                }
            }
        }
        return sb.trimEnd().toString()
    }

    private fun sincronizar() {
        btnSincronizar.isEnabled = false
        lifecycleScope.launch {
            var insertados = 0
            var actualizados = 0
            val errores = mutableListOf<String>()
            val total = postulantesList.size

            for (i in postulantesList.indices) {
                val p = postulantesList[i]
                btnSincronizar.text = "Sincronizando ${i+1}/$total: ${p.idPostulante}..."
                try {
                    val result = ApiService.sincronizarUnPostulante(p.json)
                    if (result.optBoolean("exito", false)) {
                        if (result.optString("accion", "") == "actualizado") actualizados++
                        else insertados++
                    } else throw Exception(result.optString("error", "Error"))
                } catch (e: Exception) {
                    errores.add("${p.idPostulante}: ${e.message}")
                }
            }

            val msg = buildString {
                append("$insertados insertados, $actualizados actualizados")
                if (errores.isNotEmpty()) append("\n${errores.size} errores:\n${errores.joinToString("\n")}")
            }
            btnSincronizar.isEnabled = true
                btnSincronizar.text = getString(R.string.s3_btn_sincronizar)
            AlertDialog.Builder(requireContext())
                .setTitle("Resultado")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private class PostulanteAdapter(
        private val items: List<PostulanteSync>,
        private val onClick: (PostulanteSync) -> Unit
    ) : RecyclerView.Adapter<PostulanteAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.card_postulante_resumen, p, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]; val j = item.json
            h.tvId.text = j.optString("id_postulante", "")
            h.tvNombre.text = "${j.optString("nombre")} ${j.optString("apellido")}".trim()
            h.tvEmail.text = "${j.optString("email", "")}"
            val nForm = j.optJSONArray("formaciones")?.length() ?: 0
            val nExp = j.optJSONArray("experiencias")?.length() ?: 0
            val nCert = j.optJSONArray("certificaciones")?.length() ?: 0
            h.tvResumen.text = "${j.optString("grado_nombre")}  |  Exp: $nExp  |  Cert: $nCert"
            h.btnDetalle.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = items.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvId: TextView = itemView.findViewById(R.id.tvCardId)
            val tvNombre: TextView = itemView.findViewById(R.id.tvCardNombre)
            val tvEmail: TextView = itemView.findViewById(R.id.tvCardEmail)
            val tvResumen: TextView = itemView.findViewById(R.id.tvCardResumen)
            val btnDetalle: MaterialButton = itemView.findViewById(R.id.btnVerDetalle)
        }
    }
}
