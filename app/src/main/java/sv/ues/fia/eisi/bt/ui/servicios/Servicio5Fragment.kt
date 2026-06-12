package sv.ues.fia.eisi.bt.ui.servicios

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.setupMarqueeTitle

class Servicio5Fragment : Fragment() {

    data class PreviewItem(val linea1: String, val linea2: String, val nit: String = "", val idOferta: String = "", val idPostulacion: String = "", val yaPostulado: Boolean = false, val estadoPostulacion: String? = null)

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var scrollView: ScrollView

    private lateinit var cardOfertas: MaterialCardView
    private lateinit var tvBadgeOfertas: TextView
    private lateinit var tvStatusOfertas: TextView
    private lateinit var rvOfertas: RecyclerView
    private lateinit var btnVerMasOfertas: MaterialButton
    private val ofertasList = mutableListOf<PreviewItem>()

    private lateinit var cardPostulaciones: MaterialCardView
    private lateinit var tvBadgePostulaciones: TextView
    private lateinit var tvStatusPostulaciones: TextView
    private lateinit var rvPostulaciones: RecyclerView
    private lateinit var btnVerMasPostulaciones: MaterialButton
    private val postulacionesList = mutableListOf<PreviewItem>()

    private lateinit var cardEmpresas: MaterialCardView
    private lateinit var tvBadgeEmpresas: TextView
    private lateinit var tvStatusEmpresas: TextView
    private lateinit var rvEmpresas: RecyclerView
    private lateinit var btnVerMasEmpresas: MaterialButton
    private val empresasList = mutableListOf<PreviewItem>()

    private lateinit var cardPerfil: MaterialCardView
    private lateinit var tvPerfilNombre: TextView
    private lateinit var tvPerfilEmail: TextView
    private lateinit var tvPerfilTelefono: TextView
    private lateinit var tvPerfilGrado: TextView
    private lateinit var tvPerfilUbicacion: TextView

    private var role = Constants.ROLE_POSTULANTE
    private var idPostulanteSeleccionado: String? = null

    private var ofertasApiData: JSONArray? = null
    private var empresasApiData: JSONArray? = null
    private var postulantesApiData: JSONArray? = null
    private var postulacionesApiData: JSONArray? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio5, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_ADMIN) ?: Constants.ROLE_ADMIN

        toolbar = view.findViewById(R.id.toolbar)
        toolbar.setupMarqueeTitle()
        toolbar.menu.clear()
        toolbar.menu.add(getString(R.string.s5_recargar)).setOnMenuItemClickListener {
            cargarDatosDesdeAPI()
            true
        }
        toolbar.menu.add("Cambiar Perfil").setOnMenuItemClickListener {
            cambiarPerfil()
            true
        }

        progressBar = view.findViewById(R.id.progressBar)
        scrollView = view.findViewById(R.id.scrollView)
        view.findViewById<View>(R.id.tvEstadoDescarga).visibility = View.GONE
        view.findViewById<View>(R.id.btnReintentar).visibility = View.GONE

        cardOfertas = view.findViewById(R.id.cardOfertas)
        tvBadgeOfertas = view.findViewById(R.id.tvBadgeOfertas); tvStatusOfertas = view.findViewById(R.id.tvStatusOfertas)
        rvOfertas = view.findViewById(R.id.rvOfertas); btnVerMasOfertas = view.findViewById(R.id.btnVerMasOfertas)
        cardPostulaciones = view.findViewById(R.id.cardPostulaciones)
        tvBadgePostulaciones = view.findViewById(R.id.tvBadgePostulaciones); tvStatusPostulaciones = view.findViewById(R.id.tvStatusPostulaciones)
        rvPostulaciones = view.findViewById(R.id.rvPostulaciones); btnVerMasPostulaciones = view.findViewById(R.id.btnVerMasPostulaciones)
        cardEmpresas = view.findViewById(R.id.cardEmpresas)
        tvBadgeEmpresas = view.findViewById(R.id.tvBadgeEmpresas); tvStatusEmpresas = view.findViewById(R.id.tvStatusEmpresas)
        rvEmpresas = view.findViewById(R.id.rvEmpresas); btnVerMasEmpresas = view.findViewById(R.id.btnVerMasEmpresas)
        cardPerfil = view.findViewById(R.id.cardPerfil)
        tvPerfilNombre = view.findViewById(R.id.tvPerfilNombre)
        tvPerfilEmail = view.findViewById(R.id.tvPerfilEmail)
        tvPerfilTelefono = view.findViewById(R.id.tvPerfilTelefono)
        tvPerfilGrado = view.findViewById(R.id.tvPerfilGrado)
        tvPerfilUbicacion = view.findViewById(R.id.tvPerfilUbicacion)

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupRecyclerViews()
        verificarSeleccion()
    }

    private fun setupRecyclerViews() {
        rvOfertas.layoutManager = LinearLayoutManager(requireContext())
        rvPostulaciones.layoutManager = LinearLayoutManager(requireContext())
        rvEmpresas.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun verificarSeleccion() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        idPostulanteSeleccionado = prefs.getString(Constants.KEY_ID_POSTULANTE, null)

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            scrollView.visibility = View.GONE
            try {
                val json = ApiService.getPostulantesFull()
                postulantesApiData = json.getJSONArray("data")
                val lista = mutableListOf<Pair<String, String>>()
                for (i in 0 until postulantesApiData!!.length()) {
                    val p = postulantesApiData!!.getJSONObject(i)
                    lista.add(Pair(p.getString("ID_POSTULANTE"), "${p.getString("NOMBRE")} ${p.getString("APELLIDO")}"))
                }
                if (lista.isEmpty()) {
                    Snackbar.make(requireView(), getString(R.string.s5_sin_postulantes_servidor), Snackbar.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    scrollView.visibility = View.VISIBLE
                } else {
                    mostrarDialogoSeleccionPostulante(lista, idPostulanteSeleccionado)
                }
            } catch (e: Exception) {
                Log.e("Servicio5", "Error fetch postulantes", e)
                Snackbar.make(requireView(), getString(R.string.s5_error_conexion, e.message), Snackbar.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun cambiarPerfil() {
        lifecycleScope.launch {
            try {
                val arr = postulantesApiData
                if (arr == null || arr.length() == 0) {
                    val json = ApiService.getPostulantesFull()
                    postulantesApiData = json.getJSONArray("data")
                }
                val lista = mutableListOf<Pair<String, String>>()
                for (i in 0 until postulantesApiData!!.length()) {
                    val p = postulantesApiData!!.getJSONObject(i)
                    lista.add(Pair(p.getString("ID_POSTULANTE"), "${p.getString("NOMBRE")} ${p.getString("APELLIDO")}"))
                }
                if (lista.isNotEmpty()) {
                    mostrarDialogoSeleccionPostulanteCambiar(lista)
                } else {
                    Snackbar.make(requireView(), "No hay perfiles de postulantes disponibles", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoSeleccionPostulante(lista: List<Pair<String, String>>, preSeleccionado: String? = null) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 8) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tvTitulo = TextView(context).apply { text = getString(R.string.s5_seleccionar_postulante); textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 12); setTextColor(0xFF0D1A4A.toInt()) }
        container.addView(tvTitulo)
        val radioGroup = RadioGroup(context)
        val radioButtons = lista.mapIndexed { idx, (id, nombre) ->
            RadioButton(context).apply { text = "$id - $nombre"; tag = idx; this.id = View.generateViewId(); setPadding(4, 8, 4, 8); setTextColor(0xDD000000.toInt()) }
        }
        radioButtons.forEach { radioGroup.addView(it) }
        if (radioButtons.isNotEmpty()) {
            val preIdx = if (!preSeleccionado.isNullOrBlank()) lista.indexOfFirst { it.first == preSeleccionado } else -1
            radioGroup.check(radioButtons[if (preIdx >= 0) preIdx else 0].id)
        }
        container.addView(radioGroup)
        sv.addView(container)
        AlertDialog.Builder(context)
            .setView(sv)
            .setPositiveButton(getString(R.string.s5_confirmar)) { _, _ ->
                val checked = radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                val idx = checked?.tag as? Int
                if (idx != null && idx < lista.size) {
                    idPostulanteSeleccionado = lista[idx].first
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit { putString(Constants.KEY_ID_POSTULANTE, idPostulanteSeleccionado) }
                }
                progressBar.visibility = View.GONE
                cargarDatosDesdeAPI()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                progressBar.visibility = View.GONE
                cargarDatosDesdeAPI()
            }
            .create().apply { setCancelable(false); show() }
    }

    private fun mostrarDialogoSeleccionPostulanteCambiar(lista: List<Pair<String, String>>) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 8) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tvTitulo = TextView(context).apply { text = getString(R.string.s5_seleccionar_postulante); textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 12); setTextColor(0xFF0D1A4A.toInt()) }
        container.addView(tvTitulo)
        val radioGroup = RadioGroup(context)
        val radioButtons = lista.mapIndexed { idx, (id, nombre) ->
            RadioButton(context).apply { text = "$id - $nombre"; tag = idx; this.id = View.generateViewId(); setPadding(4, 8, 4, 8); setTextColor(0xDD000000.toInt()) }
        }
        radioButtons.forEach { radioGroup.addView(it) }
        if (radioButtons.isNotEmpty()) radioGroup.check(radioButtons.first().id)
        container.addView(radioGroup)
        sv.addView(container)
        AlertDialog.Builder(context)
            .setView(sv)
            .setPositiveButton(getString(R.string.s5_confirmar)) { _, _ ->
                val checked = radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                val idx = checked?.tag as? Int
                if (idx != null && idx < lista.size) {
                    idPostulanteSeleccionado = lista[idx].first
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit { putString(Constants.KEY_ID_POSTULANTE, idPostulanteSeleccionado) }
                    cargarDatosDesdeAPI()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
    }

    private fun cargarDatosDesdeAPI() {
        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                coroutineScope {
                    val jobOfertas = async {
                        try {
                            val json = ApiService.getOfertasFull()
                            ofertasApiData = json.getJSONArray("data")
                        } catch (e: Exception) {
                            Log.e("Servicio5", "Error fetch ofertas", e)
                        }
                    }
                    val jobEmpresas = async {
                        try {
                            val json = ApiService.getEmpresasFull()
                            empresasApiData = json.getJSONArray("data")
                        } catch (e: Exception) {
                            Log.e("Servicio5", "Error fetch empresas", e)
                        }
                    }
                    val jobPostulaciones = async {
                        try {
                            val json = ApiService.getPostulacionesFull(idPostulanteSeleccionado)
                            postulacionesApiData = json.getJSONArray("data")
                        } catch (e: Exception) {
                            Log.e("Servicio5", "Error fetch postulaciones", e)
                        }
                    }
                    listOf(jobOfertas, jobEmpresas, jobPostulaciones).awaitAll()
                }

                withContext(Dispatchers.Main) {
                    populateSecciones()
                    progressBar.visibility = View.GONE
                    scrollView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("Servicio5", "Error cargarDatos", e)
                Snackbar.make(requireView(), getString(R.string.s5_error_conexion, e.message), Snackbar.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun populateSecciones() {
        mostrarPerfil()

        val arrOf = ofertasApiData
        if (arrOf != null) {
            tvStatusOfertas.text = getString(R.string.s5_descargado)
            tvStatusOfertas.setTextColor(0xFF2E7D32.toInt())
            ofertasList.clear()
            ofertasList.addAll(queryOfertasFromApiData(arrOf))
            tvBadgeOfertas.text = ofertasList.size.toString()
        } else {
            tvStatusOfertas.text = "Error"
            tvStatusOfertas.setTextColor(0xFFC62828.toInt())
        }
        rvOfertas.adapter = PreviewAdapter(ofertasList) { item -> mostrarDetalleOferta(item.nit, item.idOferta) }
        btnVerMasOfertas.text = getString(R.string.s5_ver_mas, ofertasList.size)
        btnVerMasOfertas.setOnClickListener { mostrarTodosOfertas() }
        btnVerMasOfertas.visibility = if (ofertasList.isNotEmpty()) View.VISIBLE else View.GONE

        val arrPost = postulacionesApiData
        if (arrPost != null) {
            tvStatusPostulaciones.text = getString(R.string.s5_descargado)
            tvStatusPostulaciones.setTextColor(0xFF2E7D32.toInt())
            postulacionesList.clear()
            postulacionesList.addAll(queryPostulacionesFromApiData(arrPost))
            tvBadgePostulaciones.text = postulacionesList.size.toString()
        } else {
            tvStatusPostulaciones.text = "Error"
            tvStatusPostulaciones.setTextColor(0xFFC62828.toInt())
        }
        rvPostulaciones.adapter = PreviewAdapter(postulacionesList) { item -> mostrarDetallePostulacion(item.idPostulacion) }
        btnVerMasPostulaciones.text = getString(R.string.s5_ver_mas, postulacionesList.size)
        btnVerMasPostulaciones.setOnClickListener { mostrarTodasPostulaciones(null, null) }
        btnVerMasPostulaciones.visibility = if (postulacionesList.isNotEmpty()) View.VISIBLE else View.GONE

        val arrEmp = empresasApiData
        if (arrEmp != null) {
            tvStatusEmpresas.text = getString(R.string.s5_descargado)
            tvStatusEmpresas.setTextColor(0xFF2E7D32.toInt())
            empresasList.clear()
            empresasList.addAll(queryEmpresasFromApiData(arrEmp))
            tvBadgeEmpresas.text = empresasList.size.toString()
        } else {
            tvStatusEmpresas.text = "Error"
            tvStatusEmpresas.setTextColor(0xFFC62828.toInt())
        }
        rvEmpresas.adapter = PreviewAdapter(empresasList) {}
        btnVerMasEmpresas.text = getString(R.string.s5_ver_mas, empresasList.size)
        btnVerMasEmpresas.setOnClickListener { mostrarTodasEmpresas() }
        btnVerMasEmpresas.visibility = if (empresasList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun queryOfertasFromApiData(arr: JSONArray): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        val postulacionesSet = mutableSetOf<String>()
        val postulacionesEstado = mutableMapOf<String, String>()
        postulacionesApiData?.let { postArr ->
            for (i in 0 until postArr.length()) {
                val p = postArr.getJSONObject(i)
                val key = "${p.getString("NIT")}|${p.getString("ID_OFERTA")}"
                postulacionesSet.add(key)
                postulacionesEstado[key] = p.optString("ESTADO_PROCESO", "activo")
            }
        }
        val empresaNames = mutableMapOf<String, String>()
        empresasApiData?.let { empArr ->
            for (i in 0 until empArr.length()) {
                val e = empArr.getJSONObject(i)
                empresaNames[e.getString("NIT")] = e.optString("NOMBRE_EMPRESA", "")
            }
        }
        val limit = minOf(arr.length(), 5)
        for (i in 0 until limit) {
            val o = arr.getJSONObject(i)
            val titulo = o.optString("TITULO_PUESTO", "")
            val nit = o.optString("NIT", "")
            val idOf = o.optString("ID_OFERTA", "")
            val fecha = o.optString("FECHA_PUBLICACION", "")
            val empresa = empresaNames[nit] ?: nit
            val key = "$nit|$idOf"
            val yaPost = postulacionesSet.contains(key)
            val estPost = postulacionesEstado[key]
            lista.add(PreviewItem(
                if (yaPost) "$titulo (Postulado)" else titulo,
                if (yaPost) "${estPost ?: "activo"} - $empresa - $fecha" else "$empresa - $fecha",
                nit = nit, idOferta = idOf, yaPostulado = yaPost, estadoPostulacion = estPost
            ))
        }
        return lista
    }

    private fun queryPostulacionesFromApiData(arr: JSONArray): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        val limit = minOf(arr.length(), 5)
        for (i in 0 until limit) {
            val p = arr.getJSONObject(i)
            val id = p.optString("ID_POSTULACION", "")
            val estado = p.optString("ESTADO_PROCESO", "")
            val fecha = p.optString("FECHA_APLICACION", "")
            val nit = p.optString("NIT", "")
            val idOf = p.optString("ID_OFERTA", "")
            var ofertaTitulo = idOf
            ofertasApiData?.let { ofArr ->
                for (j in 0 until ofArr.length()) {
                    val o = ofArr.getJSONObject(j)
                    if (o.optString("NIT") == nit && o.optString("ID_OFERTA") == idOf) {
                        ofertaTitulo = o.optString("TITULO_PUESTO", idOf)
                        break
                    }
                }
            }
            lista.add(PreviewItem("$id - $ofertaTitulo", "$estado - $fecha", idPostulacion = id))
        }
        return lista
    }

    private fun queryEmpresasFromApiData(arr: JSONArray): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        val nitSet = if (!idPostulanteSeleccionado.isNullOrBlank()) {
            val set = mutableSetOf<String>()
            postulacionesApiData?.let { postArr ->
                for (i in 0 until postArr.length()) {
                    set.add(postArr.getJSONObject(i).getString("NIT"))
                }
            }
            set
        } else null

        val limit = minOf(arr.length(), 5)
        var added = 0
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val nit = e.optString("NIT", "")
            if (nitSet != null && nit !in nitSet) continue
            val nombre = e.optString("NOMBRE_EMPRESA", "")
            val contacto = e.optString("CONTACTO_DIRECTO", "")
            lista.add(PreviewItem("$nombre - $nit", if (contacto.isNotBlank()) contacto else "-", nit = nit))
            added++
            if (added >= limit) break
        }
        return lista
    }

    private fun mostrarPerfil() {
        val idPost = idPostulanteSeleccionado
        if (idPost.isNullOrBlank()) {
            tvPerfilNombre.text = getString(R.string.s5_sin_perfil)
            tvPerfilEmail.text = ""
            tvPerfilTelefono.text = ""
            tvPerfilGrado.text = ""
            tvPerfilUbicacion.text = ""
            return
        }
        val arr = postulantesApiData ?: run {
            tvPerfilNombre.text = getString(R.string.s5_sin_perfil)
            return
        }
        try {
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                if (p.getString("ID_POSTULANTE") == idPost) {
                    tvPerfilNombre.text = "${p.optString("NOMBRE", "")} ${p.optString("APELLIDO", "")}"
                    tvPerfilEmail.text = p.optString("EMAIL", "").takeIf { it.isNotBlank() } ?: "-"
                    tvPerfilTelefono.text = p.optString("TELEFONO_CELULAR", "").takeIf { it.isNotBlank() } ?: "-"
                    val idGrado = p.optString("ID_GRADO_ACADEMICO", "")
                    tvPerfilGrado.text = if (idGrado.isNotBlank()) buscarNombreGrado(idGrado) else "-"
                    val distDepto = p.optString("ID_DISTRITO_DEPTO", "")
                    val distMuni = p.optString("ID_DISTRITO_MUNICIPIO", "")
                    val distId = p.optString("ID_DISTRITO_ID", "")
                    tvPerfilUbicacion.text = if (distDepto.isNotBlank()) buscarUbicacion(distDepto, distMuni, distId) else "-"
                    break
                }
            }
        } catch (_: Exception) {}
    }

    private fun buscarNombreGrado(idGrado: String): String {
        try {
            val db = ConnectionHelper(requireContext()).readableDatabase
            val c = db.rawQuery("SELECT NOMBRE_GRADO FROM GRADO_ACADEMICO WHERE ID_GRADO_ACADEMICO = ?", arrayOf(idGrado))
            val nombre = if (c.moveToFirst()) c.getString(0) else idGrado
            c.close(); db.close()
            return nombre ?: idGrado
        } catch (_: Exception) { return idGrado }
    }

    private fun buscarUbicacion(distDepto: String, distMuni: String, distId: String): String {
        try {
            val db = ConnectionHelper(requireContext()).readableDatabase
            val c = db.rawQuery("SELECT IFNULL(d.NOMBRE_DISTRITO,''), IFNULL(m.NOMBRE_MUNICIPIO,''), IFNULL(dep.NOMBRE_DEPARTAMENTO,'') FROM DISTRITO d LEFT JOIN MUNICIPIO m ON d.ID_DEPARTAMENTO = m.ID_DEPARTAMENTO AND d.ID_MUNICIPIO = m.ID_MUNICIPIO LEFT JOIN DEPARTAMENTO dep ON d.ID_DEPARTAMENTO = dep.ID_DEPARTAMENTO WHERE d.ID_DEPARTAMENTO = ? AND d.ID_MUNICIPIO = ? AND d.ID_DISTRITO = ?", arrayOf(distDepto, distMuni, distId))
            val ubicacion = if (c.moveToFirst()) {
                listOfNotNull(c.getString(0)?.takeIf { it.isNotBlank() }, c.getString(1)?.takeIf { it.isNotBlank() }, c.getString(2)?.takeIf { it.isNotBlank() }).joinToString(", ")
            } else ""
            c.close(); db.close()
            return ubicacion.ifBlank { "-" }
        } catch (_: Exception) { return "-" }
    }

    // =========================================================================
    // POSTULACION
    // =========================================================================

    private fun postularse(nit: String, idOferta: String) {
        lifecycleScope.launch {
            try {
                val idPostulante = idPostulanteSeleccionado ?: run {
                    val arr = postulantesApiData
                    if (arr == null || arr.length() == 0) {
                        Snackbar.make(requireView(), "No hay perfiles de postulantes disponibles", Snackbar.LENGTH_LONG).show()
                    } else {
                        val lista = mutableListOf<Pair<String, String>>()
                        for (i in 0 until arr.length()) {
                            val p = arr.getJSONObject(i)
                            lista.add(Pair(p.getString("ID_POSTULANTE"), "${p.getString("NOMBRE")} ${p.getString("APELLIDO")}"))
                        }
                        mostrarDialogoSeleccionPostulanteParaPostularse(lista, nit, idOferta)
                    }
                    return@launch
                }
                val jsonPost = ApiService.getPostulacionesFull(idPostulante)
                postulacionesApiData = jsonPost.getJSONArray("data")
                val yaPostulado = postulacionesApiData?.let { postArr ->
                    (0 until postArr.length()).any { i ->
                        val p = postArr.getJSONObject(i)
                        p.optString("ID_POSTULANTE") == idPostulante && p.optString("NIT") == nit && p.optString("ID_OFERTA") == idOferta
                    }
                } ?: false
                if (yaPostulado) { Snackbar.make(requireView(), getString(R.string.s5_error_ya_postulado), Snackbar.LENGTH_LONG).show(); return@launch }
                val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                try {
                    val json = JSONObject().apply {
                        put("nit", nit); put("id_oferta", idOferta)
                        put("id_postulante", idPostulante); put("fecha_aplicacion", fecha); put("estado_proceso", "en proceso")
                    }
                    ApiService.insertarPostulacion(json)
                } catch (e: Exception) {
                    Log.e("Servicio5", "Error al enviar postulacion a InfinityFree", e)
                    Snackbar.make(requireView(), "No se pudo enviar la postulacion: ${e.message}", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                Snackbar.make(requireView(), getString(R.string.s5_postulacion_exitosa), Snackbar.LENGTH_LONG).show()
                cargarDatosDesdeAPI()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show() }
        }
    }

    private fun mostrarDialogoSeleccionPostulanteParaPostularse(lista: List<Pair<String, String>>, nit: String, idOferta: String) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 8) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tvTitulo = TextView(context).apply { text = getString(R.string.s5_seleccionar_postulante); textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 12); setTextColor(0xFF0D1A4A.toInt()) }
        container.addView(tvTitulo)
        val radioGroup = RadioGroup(context)
        val radioButtons = lista.mapIndexed { idx, (id, nombre) ->
            RadioButton(context).apply { text = "$id - $nombre"; tag = idx; this.id = View.generateViewId(); setPadding(4, 8, 4, 8); setTextColor(0xDD000000.toInt()) }
        }
        radioButtons.forEach { radioGroup.addView(it) }
        if (radioButtons.isNotEmpty()) radioGroup.check(radioButtons.first().id)
        container.addView(radioGroup)
        sv.addView(container)
        AlertDialog.Builder(context)
            .setView(sv)
            .setPositiveButton(getString(R.string.s5_confirmar)) { _, _ ->
                val checked = radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                val idx = checked?.tag as? Int
                if (idx != null && idx < lista.size) {
                    idPostulanteSeleccionado = lista[idx].first
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit { putString(Constants.KEY_ID_POSTULANTE, idPostulanteSeleccionado) }
                    postularse(nit, idOferta)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
    }

    // =========================================================================
    // DETALLE DE OFERTA + POSTULACION
    // =========================================================================

    private fun mostrarDetalleOferta(nit: String, idOferta: String) {
        lifecycleScope.launch {
            try {
                val ofertaJson = findOfertaEnCache(nit, idOferta)
                if (ofertaJson == null) { Snackbar.make(requireView(), "Oferta no encontrada", Snackbar.LENGTH_SHORT).show(); return@launch }

                val yaPostulado = if (idPostulanteSeleccionado.isNullOrBlank()) false
                else postulacionesApiData?.let { postArr ->
                    (0 until postArr.length()).any { i ->
                        val p = postArr.getJSONObject(i)
                        p.optString("ID_POSTULANTE") == idPostulanteSeleccionado && p.optString("NIT") == nit && p.optString("ID_OFERTA") == idOferta
                    }
                } ?: false

                var empresaNombre = nit
                empresasApiData?.let { empArr ->
                    for (i in 0 until empArr.length()) {
                        val e = empArr.getJSONObject(i)
                        if (e.optString("NIT") == nit) {
                            empresaNombre = e.optString("NOMBRE_EMPRESA", nit)
                            break
                        }
                    }
                }
                val idGrado = ofertaJson.optString("ID_GRADO_ACADEMICO", "")
                val gradoNombre = if (idGrado.isNotBlank()) buscarNombreGrado(idGrado) else "-"

                val titulo = ofertaJson.optString("TITULO_PUESTO", "")
                val fechaPub = ofertaJson.optString("FECHA_PUBLICACION", "")
                val fechaCad = ofertaJson.optString("FECHA_CADUCIDAD", "")
                val exp = ofertaJson.optString("EXPERIENCIA_ANIOS", "")
                val edadMin = ofertaJson.optString("EDAD_MINIMA", "")
                val edadMax = ofertaJson.optString("EDAD_MAXIMA", "")
                val desc = ofertaJson.optString("DESCRIPCION_OFERTA_TRABAJO", "")
                val edadStr = if (edadMin.isNotEmpty() || edadMax.isNotEmpty()) "$edadMin - $edadMax" else ""
                val requisitos = ofertaJson.optJSONArray("requisitos")

                val context = requireContext()
                val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
                val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                val tvTitulo = TextView(context).apply { text = titulo; textSize = 18f; setTextColor(0xFF0D1A4A.toInt()); setTypeface(null, android.graphics.Typeface.BOLD) }
                container.addView(tvTitulo)
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }; setBackgroundColor(0xFF3366FF.toInt()) })
                addSectionHeader(context, container, getString(R.string.s5_datos_oferta))
                addDetailRow(context, container, getString(R.string.s5_empresa), "$empresaNombre - $nit")
                addDetailRow(context, container, getString(R.string.s5_grado), gradoNombre)
                addDetailRow(context, container, getString(R.string.s5_publicacion), fechaPub.ifBlank { "-" })
                addDetailRow(context, container, getString(R.string.s5_caducidad), fechaCad.ifBlank { "-" })
                addDetailRow(context, container, getString(R.string.s5_experiencia), if (exp.isNotEmpty()) "$exp a\u00F1os" else "-")
                addDetailRow(context, container, getString(R.string.s5_edad), edadStr.ifBlank { "-" })
                addDetailRow(context, container, getString(R.string.s5_descripcion), desc.ifBlank { "-" })
                if (requisitos != null && requisitos.length() > 0) {
                    container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 4 }; setBackgroundColor(0x22000000) })
                    addSectionHeader(context, container, "${getString(R.string.s5_requisitos)} (${requisitos.length()})")
                    for (j in 0 until requisitos.length()) {
                        val req = requisitos.getJSONObject(j)
                        val tv = TextView(context).apply { text = "- ${req.optString("DESCRIPCION_REQUISITO", "")}"; textSize = 13f; setTextColor(0xDD000000.toInt()); setPadding(16, 2, 0, 2) }
                        container.addView(tv)
                    }
                }
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 12 }; setBackgroundColor(0x22000000) })
                val btnPostular = MaterialButton(context).apply {
                    if (yaPostulado) {
                        text = getString(R.string.s5_error_ya_postulado)
                        isEnabled = false
                        setBackgroundColor(0xFF2E7D32.toInt())
                    } else {
                        text = getString(R.string.s5_postularse)
                        setBackgroundColor(0xFF3366FF.toInt())
                    }
                    setTextColor(0xFFFFFFFF.toInt())
                    cornerRadius = 24.toDp(context)
                    isAllCaps = false
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56.toDp(context))
                }.also { container.addView(it) }

                sv.addView(container)
                val dialog = AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).create()
                if (!yaPostulado) {
                    btnPostular.setOnClickListener { dialog.dismiss(); postularse(nit, idOferta) }
                }
                dialog.show()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun findOfertaEnCache(nit: String, idOferta: String): JSONObject? {
        val arr = ofertasApiData ?: return null
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("NIT") == nit && o.optString("ID_OFERTA") == idOferta) return o
        }
        return null
    }

    private fun mostrarDetallePostulacion(idPostulacion: String) {
        lifecycleScope.launch {
            try {
                var datos = mutableMapOf<String, String>()
                postulacionesApiData?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        if (p.optString("ID_POSTULACION") == idPostulacion) {
                            datos["id"] = p.optString("ID_POSTULACION", "")
                            datos["estado"] = p.optString("ESTADO_PROCESO", "")
                            datos["fecha"] = p.optString("FECHA_APLICACION", "")
                            datos["postulante"] = p.optString("ID_POSTULANTE", "")
                            val nit = p.optString("NIT", "")
                            val idOf = p.optString("ID_OFERTA", "")
                            datos["oferta"] = idOf
                            datos["empresa"] = nit
                            ofertasApiData?.let { ofArr ->
                                for (j in 0 until ofArr.length()) {
                                    val o = ofArr.getJSONObject(j)
                                    if (o.optString("NIT") == nit && o.optString("ID_OFERTA") == idOf) {
                                        datos["oferta"] = o.optString("TITULO_PUESTO", idOf)
                                        break
                                    }
                                }
                            }
                            empresasApiData?.let { empArr ->
                                for (j in 0 until empArr.length()) {
                                    val e = empArr.getJSONObject(j)
                                    if (e.optString("NIT") == nit) {
                                        datos["empresa"] = e.optString("NOMBRE_EMPRESA", nit)
                                        break
                                    }
                                }
                            }
                            break
                        }
                    }
                }

                if (datos.isEmpty()) { Snackbar.make(requireView(), "Postulacion no encontrada", Snackbar.LENGTH_SHORT).show(); return@launch }
                val context = requireContext()
                val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
                val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                val tvTitulo = TextView(context).apply { text = datos["id"] ?: ""; textSize = 18f; setTextColor(0xFF0D1A4A.toInt()); setTypeface(null, android.graphics.Typeface.BOLD) }
                container.addView(tvTitulo)
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }; setBackgroundColor(0xFF3366FF.toInt()) })
                addSectionHeader(context, container, getString(R.string.s5_titulo_postulaciones))
                addDetailRow(context, container, getString(R.string.s5_oferta), "${datos["oferta"]} (${datos["empresa"]})")
                addDetailRow(context, container, getString(R.string.s5_estado), datos["estado"] ?: "-")
                addDetailRow(context, container, getString(R.string.s5_fecha), datos["fecha"] ?: "-")
                addDetailRow(context, container, "ID Postulante", datos["postulante"] ?: "-")
                sv.addView(container)
                AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).show()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
        }
    }

    // =========================================================================
    // DIALOGOS "VER MAS"
    // =========================================================================

    private fun mostrarTodosOfertas() {
        val arr = ofertasApiData
        if (arr == null) { Snackbar.make(requireView(), "No hay datos de ofertas", Snackbar.LENGTH_SHORT).show(); return }
        val lista = queryOfertasFromApiDataFull(arr)
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        var dialog: AlertDialog? = null
        if (lista.isEmpty()) {
            val tv = TextView(context).apply { text = getString(R.string.s3_sin_datos); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24) }
            container.addView(tv)
        } else {
            for (item in lista) {
                val itemView = LayoutInflater.from(context).inflate(R.layout.card_preview_fila, container, false)
                val tv1 = itemView.findViewById<TextView>(R.id.tvLinea1)
                val tv2 = itemView.findViewById<TextView>(R.id.tvLinea2)
                tv1.text = item.linea1; tv2.text = item.linea2
                if (item.yaPostulado) tv1.setTextColor(0xFF2E7D32.toInt()) else tv1.setTextColor(0xFF0D1A4A.toInt())
                itemView.setOnClickListener { dialog?.dismiss(); mostrarDetalleOferta(item.nit, item.idOferta) }
                container.addView(itemView)
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(0x11000000) })
            }
        }
        sv.addView(container)
        dialog = AlertDialog.Builder(context).setTitle("Todas las Ofertas - ${lista.size} registros").setView(sv).setPositiveButton(getString(R.string.cerrar), null).create()
        dialog.show()
    }

    private fun queryOfertasFromApiDataFull(arr: JSONArray): List<PreviewItem> {
        val postulacionesSet = mutableSetOf<String>()
        val postulacionesEstado = mutableMapOf<String, String>()
        postulacionesApiData?.let { postArr ->
            for (i in 0 until postArr.length()) {
                val p = postArr.getJSONObject(i)
                val key = "${p.getString("NIT")}|${p.getString("ID_OFERTA")}"
                postulacionesSet.add(key)
                postulacionesEstado[key] = p.optString("ESTADO_PROCESO", "activo")
            }
        }
        val empresaNames = mutableMapOf<String, String>()
        empresasApiData?.let { empArr ->
            for (i in 0 until empArr.length()) {
                val e = empArr.getJSONObject(i)
                empresaNames[e.getString("NIT")] = e.optString("NOMBRE_EMPRESA", "")
            }
        }
        val lista = mutableListOf<PreviewItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val titulo = o.optString("TITULO_PUESTO", "")
            val nit = o.optString("NIT", "")
            val idOf = o.optString("ID_OFERTA", "")
            val fecha = o.optString("FECHA_PUBLICACION", "")
            val empresa = empresaNames[nit] ?: nit
            val key = "$nit|$idOf"
            val yaPost = postulacionesSet.contains(key)
            val estPost = postulacionesEstado[key]
            lista.add(PreviewItem(
                if (yaPost) "$titulo (Postulado)" else titulo,
                if (yaPost) "${estPost ?: "activo"} - $empresa - $fecha" else "$empresa - $fecha",
                nit = nit, idOferta = idOf, yaPostulado = yaPost, estadoPostulacion = estPost
            ))
        }
        return lista
    }

    private fun mostrarTodasEmpresas() {
        val arr = empresasApiData
        if (arr == null) { Snackbar.make(requireView(), "No hay datos de empresas", Snackbar.LENGTH_SHORT).show(); return }
        val nitSet = if (!idPostulanteSeleccionado.isNullOrBlank()) {
            val set = mutableSetOf<String>()
            postulacionesApiData?.let { postArr ->
                for (i in 0 until postArr.length()) {
                    set.add(postArr.getJSONObject(i).getString("NIT"))
                }
            }
            set
        } else null

        val lista = mutableListOf<PreviewItem>()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val nit = e.optString("NIT", "")
            if (nitSet != null && nit !in nitSet) continue
            val nombre = e.optString("NOMBRE_EMPRESA", "")
            val contacto = e.optString("CONTACTO_DIRECTO", "")
            lista.add(PreviewItem("$nombre - $nit", if (contacto.isNotBlank()) contacto else "-", nit = nit))
        }

        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        if (lista.isEmpty()) {
            val tv = TextView(context).apply { text = getString(R.string.s3_sin_datos); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24) }
            container.addView(tv)
        } else {
            for (item in lista) {
                val itemView = LayoutInflater.from(context).inflate(R.layout.card_preview_fila, container, false)
                itemView.findViewById<TextView>(R.id.tvLinea1).text = item.linea1
                itemView.findViewById<TextView>(R.id.tvLinea2).text = item.linea2
                container.addView(itemView)
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(0x11000000) })
            }
        }
        sv.addView(container)
        AlertDialog.Builder(context).setTitle("Empresas - ${lista.size} registros").setView(sv).setPositiveButton(getString(R.string.cerrar), null).show()
    }

    private fun mostrarTodasPostulaciones(filtro: String?, args: Array<String>?) {
        val arr = postulacionesApiData
        val lista = mutableListOf<PreviewItem>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val id = p.optString("ID_POSTULACION", "")
                val estado = p.optString("ESTADO_PROCESO", "")
                val fecha = p.optString("FECHA_APLICACION", "")
                val nit = p.optString("NIT", "")
                val idOf = p.optString("ID_OFERTA", "")
                var ofertaTitulo = idOf
                ofertasApiData?.let { ofArr ->
                    for (j in 0 until ofArr.length()) {
                        val o = ofArr.getJSONObject(j)
                        if (o.optString("NIT") == nit && o.optString("ID_OFERTA") == idOf) {
                            ofertaTitulo = o.optString("TITULO_PUESTO", idOf)
                            break
                        }
                    }
                }
                lista.add(PreviewItem("$id - $ofertaTitulo", "$estado - $fecha", idPostulacion = id))
            }
        }

        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        var dialog: AlertDialog? = null
        if (lista.isEmpty()) {
            val tv = TextView(context).apply { text = getString(R.string.s3_sin_datos); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24) }
            container.addView(tv)
        } else {
            for (item in lista) {
                val itemView = LayoutInflater.from(context).inflate(R.layout.card_preview_fila, container, false)
                itemView.findViewById<TextView>(R.id.tvLinea1).text = item.linea1
                itemView.findViewById<TextView>(R.id.tvLinea2).text = item.linea2
                itemView.setOnClickListener { dialog?.dismiss(); mostrarDetallePostulacion(item.idPostulacion) }
                container.addView(itemView)
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(0x11000000) })
            }
        }
        sv.addView(container)
        dialog = AlertDialog.Builder(context).setTitle("Todas las Postulaciones - ${lista.size} registros").setView(sv).setPositiveButton(getString(R.string.cerrar), null).create()
        dialog.show()
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private fun addSectionHeader(context: Context, container: LinearLayout, text: String) {
        val tv = TextView(context).apply { this.text = text; textSize = 14f; setTextColor(0xFF3366FF.toInt()); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 8, 0, 4) }; container.addView(tv)
    }
    private fun addDetailRow(context: Context, container: LinearLayout, label: String, value: String) {
        if (value.isBlank()) return
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 3, 8, 3) }
        val tvL = TextView(context).apply { text = label; textSize = 12f; setTextColor(0x99000000.toInt()); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f) }
        val tvV = TextView(context).apply { text = value; textSize = 13f; setTextColor(0xDD000000.toInt()); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f) }
        row.addView(tvL); row.addView(tvV); container.addView(row)
    }
    private fun Int.toDp(context: Context) = (this * context.resources.displayMetrics.density).toInt()

    // =========================================================================
    // ADAPTER
    // =========================================================================

    private class PreviewAdapter(private val items: List<PreviewItem>, private val onClick: ((PreviewItem) -> Unit)? = null) : RecyclerView.Adapter<PreviewAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH { val v = LayoutInflater.from(parent.context).inflate(R.layout.card_preview_fila, parent, false); return VH(v) }
        override fun onBindViewHolder(holder: VH, pos: Int) { val item = items[pos]; holder.tvLinea1.text = item.linea1; holder.tvLinea2.text = item.linea2; if (item.yaPostulado) holder.tvLinea1.setTextColor(0xFF2E7D32.toInt()) else holder.tvLinea1.setTextColor(0xFF0D1A4A.toInt()); if (onClick != null) holder.itemView.setOnClickListener { onClick(item) } }
        override fun getItemCount() = items.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) { val tvLinea1: TextView = itemView.findViewById(R.id.tvLinea1); val tvLinea2: TextView = itemView.findViewById(R.id.tvLinea2) }
    }
}
