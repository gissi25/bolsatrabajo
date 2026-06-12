package sv.ues.fia.eisi.bt.ui.servicios

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
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

class Servicio6Fragment : Fragment() {

    data class DeptoItem(val id: String, val nombre: String) {
        override fun toString() = nombre
    }
    data class MuniItem(val id: String, val nombre: String) {
        override fun toString() = nombre
    }
    data class DistItem(val id: String, val nombre: String) {
        override fun toString() = nombre
    }
    data class ResultadoOferta(
        val nit: String, val idOferta: String, val titulo: String,
        val empresa: String, val departamento: String, val municipio: String,
        val grado: String, val fechaPub: String, val fechaCad: String,
        val expAnios: String, val edadMin: String, val edadMax: String,
        val descripcion: String, val contacto: String,
        val yaPostulado: Boolean = true,
        val estadoPostulacion: String? = null
    )

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var scrollView: ScrollView
    private lateinit var cardFiltros: MaterialCardView
    private lateinit var spDepartamento: MaterialAutoCompleteTextView
    private lateinit var tilDepartamento: TextInputLayout
    private lateinit var spMunicipio: MaterialAutoCompleteTextView
    private lateinit var tilMunicipio: TextInputLayout
    private lateinit var spDistrito: MaterialAutoCompleteTextView
    private lateinit var tilDistrito: TextInputLayout
    private lateinit var btnBuscar: MaterialButton
    private lateinit var cardResultados: MaterialCardView
    private lateinit var tvTituloResultados: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var tvSinResultados: TextView

    private var role = Constants.ROLE_POSTULANTE
    private var idPostulanteSeleccionado: String? = null

    private val departamentos = mutableListOf<DeptoItem>()
    private val municipios = mutableListOf<MuniItem>()
    private val distritos = mutableListOf<DistItem>()
    private val ofertasResultados = mutableListOf<ResultadoOferta>()

    private var postulantesApiData: JSONArray? = null
    private var postulacionesApiData: JSONArray? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_ADMIN) ?: Constants.ROLE_ADMIN
        idPostulanteSeleccionado = prefs.getString(Constants.KEY_ID_POSTULANTE, null)

        toolbar = view.findViewById(R.id.toolbar)
        toolbar.setupMarqueeTitle()
        progressBar = view.findViewById(R.id.progressBar)
        scrollView = view.findViewById(R.id.scrollView)
        cardFiltros = view.findViewById(R.id.cardFiltros)
        spDepartamento = view.findViewById(R.id.spDepartamento)
        tilDepartamento = view.findViewById(R.id.tilDepartamento)
        spMunicipio = view.findViewById(R.id.spMunicipio)
        tilMunicipio = view.findViewById(R.id.tilMunicipio)
        spDistrito = view.findViewById(R.id.spDistrito)
        tilDistrito = view.findViewById(R.id.tilDistrito)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        cardResultados = view.findViewById(R.id.cardResultados)
        tvTituloResultados = view.findViewById(R.id.tvTituloResultados)
        rvResultados = view.findViewById(R.id.rvResultados)
        tvSinResultados = view.findViewById(R.id.tvSinResultados)

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        rvResultados.layoutManager = LinearLayoutManager(requireContext())

        toolbar.menu.clear()
        toolbar.menu.add("Recargar").setOnMenuItemClickListener {
            buscarOfertas()
            true
        }
        toolbar.menu.add("Cambiar Perfil").setOnMenuItemClickListener {
            cambiarPerfil()
            true
        }

        verificarSeleccion()
    }

    private fun configurarVistaPostulante() {
        toolbar.title = getString(R.string.s6_titulo_postulante)
        cardFiltros.visibility = View.VISIBLE
        btnBuscar.setOnClickListener { buscarOfertas() }
        cargarDepartamentos()
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

    // =========================================================================
    // CARGAR DEPARTAMENTOS DESDE API
    // =========================================================================

    private fun cargarDepartamentos() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val json = ApiService.getDepartamentos()
                val arr = json.getJSONArray("data")
                departamentos.clear()
                departamentos.add(DeptoItem("", "Todos los departamentos"))
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    departamentos.add(DeptoItem(d.getString("ID_DEPARTAMENTO"), d.getString("NOMBRE_DEPARTAMENTO")))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, departamentos)
                spDepartamento.setAdapter(adapter)
                spDepartamento.setThreshold(0)
                spDepartamento.setOnItemClickListener { _, _, pos, _ ->
                    if (pos >= 0 && pos < departamentos.size) {
                        val item = departamentos[pos]
                        spDepartamento.setTag(item.id)
                        if (item.id.isNotBlank()) {
                            cargarMunicipios(item.id)
                        } else {
                            municipios.clear(); distritos.clear()
                            spMunicipio.setText("", false); spMunicipio.setTag("")
                            spMunicipio.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, emptyList<MuniItem>()))
                            spDistrito.setText("", false); spDistrito.setTag("")
                            spDistrito.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, emptyList<DistItem>()))
                        }
                    }
                }
                tilDepartamento.setOnClickListener { spDepartamento.showDropDown() }
                preSeleccionarUbicacionPostulante()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
        }
    }

    // =========================================================================
    // CARGAR MUNICIPIOS DESDE API
    // =========================================================================

    private fun cargarMunicipios(idDepartamento: String) {
        lifecycleScope.launch {
            try {
                val json = ApiService.getMunicipiosPorDepto(idDepartamento)
                val arr = json.getJSONArray("data")
                municipios.clear()
                municipios.add(MuniItem("", "Todos"))
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    municipios.add(MuniItem(m.getString("ID_MUNICIPIO"), m.getString("NOMBRE_MUNICIPIO")))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, municipios)
                spMunicipio.setAdapter(adapter)
                spMunicipio.setThreshold(0)
                spMunicipio.setOnItemClickListener { _, _, pos, _ ->
                    if (pos >= 0 && pos < municipios.size) {
                        spMunicipio.setTag(municipios[pos].id)
                        val deptoId = spDepartamento.tag?.toString() ?: ""
                        val muniId = municipios[pos].id
                        if (muniId.isNotBlank()) {
                            cargarDistritos(deptoId, muniId)
                        } else {
                            distritos.clear()
                            spDistrito.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, distritos))
                            spDistrito.setText("", false)
                            spDistrito.setTag("")
                        }
                    }
                }
                tilMunicipio.setOnClickListener { spMunicipio.showDropDown() }
            } catch (_: Exception) {}
        }
    }

    // =========================================================================
    // CARGAR DISTRITOS DESDE BD LOCAL (sin endpoint API aun)
    // =========================================================================

    private fun cargarDistritos(idDepartamento: String, idMunicipio: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    distritos.clear()
                    val db = ConnectionHelper(requireContext()).writableDb
                    distritos.add(DistItem("", "Todos"))
                    val c = db.rawQuery("SELECT ID_DISTRITO, NOMBRE_DISTRITO FROM DISTRITO WHERE ID_DEPARTAMENTO = ? AND ID_MUNICIPIO = ? ORDER BY NOMBRE_DISTRITO", arrayOf(idDepartamento, idMunicipio))
                    while (c.moveToNext()) distritos.add(DistItem(c.getString(0), c.getString(1)))
                    c.close(); db.close()
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, distritos)
                spDistrito.setAdapter(adapter)
                spDistrito.setThreshold(0)
                spDistrito.setOnItemClickListener { _, _, pos, _ ->
                    if (pos >= 0 && pos < distritos.size) spDistrito.setTag(distritos[pos].id)
                }
                tilDistrito.setOnClickListener { spDistrito.showDropDown() }
            } catch (_: Exception) {}
        }
    }

    // =========================================================================
    // PRE SELECCIONAR UBICACION DEL POSTULANTE
    // =========================================================================

    private fun preSeleccionarUbicacionPostulante() {
        if (idPostulanteSeleccionado.isNullOrBlank()) return
        val arr = postulantesApiData ?: return
        lifecycleScope.launch {
            try {
                var deptoId: String? = null
                var muniId: String? = null
                var distId: String? = null
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    if (p.getString("ID_POSTULANTE") == idPostulanteSeleccionado) {
                        deptoId = p.optString("ID_DISTRITO_DEPTO", "").takeIf { it.isNotBlank() }
                        muniId = p.optString("ID_DISTRITO_MUNICIPIO", "").takeIf { it.isNotBlank() }
                        distId = p.optString("ID_DISTRITO_ID", "").takeIf { it.isNotBlank() }
                        break
                    }
                }
                if (deptoId != null) {
                    val idxDepto = departamentos.indexOfFirst { it.id == deptoId }
                    if (idxDepto >= 0) {
                        spDepartamento.setText(departamentos[idxDepto].nombre, false)
                        spDepartamento.setTag(deptoId)
                        cargarMunicipios(deptoId)
                        if (muniId != null) {
                            spMunicipio.postDelayed({
                                val idxMuni = municipios.indexOfFirst { it.id == muniId }
                                if (idxMuni >= 0) {
                                    spMunicipio.setText(municipios[idxMuni].nombre, false)
                                    spMunicipio.setTag(muniId)
                                    cargarDistritos(deptoId, muniId)
                                    if (distId != null) {
                                        spDistrito.postDelayed({
                                            val idxDist = distritos.indexOfFirst { it.id == distId }
                                            if (idxDist >= 0) { spDistrito.setText(distritos[idxDist].nombre, false); spDistrito.setTag(distId) }
                                            buscarOfertas()
                                        }, 400)
                                    } else {
                                        spDistrito.postDelayed({ buscarOfertas() }, 400)
                                    }
                                }
                            }, 400)
                        } else {
                            buscarOfertas()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // =========================================================================
    // BUSCAR OFERTAS POR UBICACION (API directo)
    // =========================================================================

    private fun buscarOfertas() {
        val deptoId = spDepartamento.tag?.toString() ?: ""
        progressBar.visibility = View.VISIBLE
        cardResultados.visibility = View.GONE
        tvSinResultados.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val muniId = spMunicipio.tag?.toString()
                val distId = spDistrito.tag?.toString()

                var ofertasArr: JSONArray? = null
                coroutineScope {
                    val jobOfertas = async {
                        if (deptoId.isNotBlank()) {
                            try {
                                val json = ApiService.filtrarOfertasPorUbicacion(deptoId, muniId)
                                ofertasArr = json.getJSONArray("data")
                            } catch (e: Exception) { Log.e("Servicio6", "Error fetch ofertas", e) }
                        }
                    }
                    val jobPostulaciones = async {
                        try {
                            val json = ApiService.getPostulacionesFull(idPostulanteSeleccionado)
                            postulacionesApiData = json.getJSONArray("data")
                        } catch (e: Exception) { Log.e("Servicio6", "Error fetch postulaciones", e) }
                    }
                    listOf(jobOfertas, jobPostulaciones).awaitAll()
                }

                val resultados = mutableListOf<ResultadoOferta>()
                if (ofertasArr != null) {
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
                    for (i in 0 until ofertasArr.length()) {
                        val o = ofertasArr.getJSONObject(i)
                        val nit = o.optString("NIT", "")
                        val idOf = o.optString("ID_OFERTA", "")
                        val key = "$nit|$idOf"
                        val yaPost = postulacionesSet.contains(key)
                        val empresaDistrito = o.optString("NOMBRE_DISTRITO", "")
                        if (!distId.isNullOrBlank() && o.optString("ID_DISTRITO_ID", "") != distId) continue
                        resultados.add(ResultadoOferta(
                            nit = nit, idOferta = idOf,
                            titulo = o.optString("TITULO_PUESTO", ""),
                            empresa = o.optString("NOMBRE_EMPRESA", ""),
                            departamento = o.optString("NOMBRE_DEPARTAMENTO", ""),
                            municipio = o.optString("NOMBRE_MUNICIPIO", ""),
                            grado = o.optString("NOMBRE_GRADO", ""),
                            fechaPub = o.optString("FECHA_PUBLICACION", ""),
                            fechaCad = o.optString("FECHA_CADUCIDAD", ""),
                            expAnios = o.optString("EXPERIENCIA_ANIOS", ""),
                            edadMin = o.optString("EDAD_MINIMA", ""),
                            edadMax = o.optString("EDAD_MAXIMA", ""),
                            descripcion = o.optString("DESCRIPCION_OFERTA_TRABAJO", ""),
                            contacto = o.optString("CONTACTO_DIRECTO", ""),
                            yaPostulado = yaPost,
                            estadoPostulacion = if (yaPost) postulacionesEstado[key] else null
                        ))
                    }
                }

                ofertasResultados.clear()
                ofertasResultados.addAll(resultados)

                progressBar.visibility = View.GONE
                if (ofertasResultados.isEmpty()) {
                    cardResultados.visibility = View.GONE
                    tvSinResultados.visibility = View.VISIBLE
                } else {
                    tvSinResultados.visibility = View.GONE
                    cardResultados.visibility = View.VISIBLE
                    tvTituloResultados.text = getString(R.string.s6_n_ofertas, ofertasResultados.size)
                    rvResultados.adapter = OfertaAdapter(ofertasResultados) { item -> mostrarDetalleOferta(item) }
                    rvResultados.post {
                        rvResultados.measure(
                            View.MeasureSpec.makeMeasureSpec(rvResultados.width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )
                        rvResultados.layoutParams = rvResultados.layoutParams.apply { height = rvResultados.measuredHeight }
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // =========================================================================
    // DIALOGO DETALLE OFERTA
    // =========================================================================

    private fun mostrarDetalleOferta(item: ResultadoOferta) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tvTitulo = TextView(context).apply { text = item.titulo; textSize = 18f; setTextColor(0xFF0D1A4A.toInt()); setTypeface(null, android.graphics.Typeface.BOLD) }
        container.addView(tvTitulo)
        container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }; setBackgroundColor(0xFF3366FF.toInt()) })
        addSectionHeader(context, container, getString(R.string.s5_datos_oferta))
        addDetailRow(context, container, getString(R.string.s5_empresa), "${item.empresa} - ${item.nit}")
        addDetailRow(context, container, getString(R.string.s6_ubicacion), "${item.departamento}, ${item.municipio}")
        addDetailRow(context, container, getString(R.string.s5_grado), item.grado.ifBlank { "-" })
        addDetailRow(context, container, getString(R.string.s5_publicacion), item.fechaPub.ifBlank { "-" })
        addDetailRow(context, container, getString(R.string.s5_caducidad), item.fechaCad.ifBlank { "-" })
        addDetailRow(context, container, getString(R.string.s5_experiencia), if (item.expAnios.isNotBlank()) "${item.expAnios} a\u00F1os" else "-")
        val edadStr = listOfNotNull(item.edadMin.takeIf { it.isNotBlank() }, item.edadMax.takeIf { it.isNotBlank() }).joinToString(" - ")
        addDetailRow(context, container, getString(R.string.s5_edad), edadStr.ifBlank { "-" })
        addDetailRow(context, container, getString(R.string.s5_descripcion), item.descripcion.ifBlank { "-" })
        container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 12 }; setBackgroundColor(0x22000000) })
        val btnPostular = MaterialButton(context).apply {
            if (item.yaPostulado) { text = getString(R.string.s5_error_ya_postulado); isEnabled = false; setBackgroundColor(0xFF2E7D32.toInt()) }
            else { text = getString(R.string.s5_postularse); setBackgroundColor(0xFF3366FF.toInt()) }
            setTextColor(0xFFFFFFFF.toInt()); cornerRadius = 24.toDp(context); isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56.toDp(context))
        }.also { container.addView(it) }
        sv.addView(container)
        val dialog = AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).create()
        if (!item.yaPostulado) { btnPostular.setOnClickListener { dialog.dismiss(); postularse(item.nit, item.idOferta) } }
        dialog.show()
    }

    // =========================================================================
    // POSTULARSE DIRECTA A API
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
                val yaPostulado = postulacionesApiData?.let { postArr ->
                    (0 until postArr.length()).any { i ->
                        val p = postArr.getJSONObject(i)
                        p.optString("ID_POSTULANTE") == idPostulante && p.optString("NIT") == nit && p.optString("ID_OFERTA") == idOferta
                    }
                } ?: false
                if (yaPostulado) { Snackbar.make(requireView(), getString(R.string.s5_error_ya_postulado), Snackbar.LENGTH_LONG).show(); return@launch }
                val idPostulacion = run {
                    var maxNum = 0
                    postulacionesApiData?.let { postArr ->
                        for (i in 0 until postArr.length()) {
                            val pid = postArr.getJSONObject(i).optString("ID_POSTULACION", "")
                            if (pid.startsWith("POS")) { val num = pid.substring(3).toIntOrNull() ?: 0; if (num > maxNum) maxNum = num }
                        }
                    }
                    "POS${(maxNum + 1).toString().padStart(3, '0')}"
                }
                val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                try {
                    val json = JSONObject().apply {
                        put("id_postulacion", idPostulacion); put("nit", nit); put("id_oferta", idOferta)
                        put("id_postulante", idPostulante); put("fecha_aplicacion", fecha); put("estado_proceso", "en proceso")
                    }
                    ApiService.insertarPostulacion(json)
                } catch (e: Exception) {
                    Log.e("Servicio6", "Error al enviar postulacion", e)
                    Snackbar.make(requireView(), "No se pudo enviar la postulacion: ${e.message}", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                Snackbar.make(requireView(), getString(R.string.s5_postulacion_exitosa), Snackbar.LENGTH_LONG).show()
                buscarOfertas()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show() }
        }
    }

    // =========================================================================
    // DIALOGOS DE SELECCION DE POSTULANTE
    // =========================================================================

    private fun verificarSeleccion() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        idPostulanteSeleccionado = prefs.getString(Constants.KEY_ID_POSTULANTE, null)
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val json = ApiService.getPostulantesFull()
                postulantesApiData = json.getJSONArray("data")
                val lista = mutableListOf<Pair<String, String>>()
                for (i in 0 until postulantesApiData!!.length()) {
                    val p = postulantesApiData!!.getJSONObject(i)
                    lista.add(Pair(p.getString("ID_POSTULANTE"), "${p.getString("NOMBRE")} ${p.getString("APELLIDO")}"))
                }
                progressBar.visibility = View.GONE
                if (lista.isEmpty()) {
                    configurarVistaPostulante()
                } else {
                    mostrarDialogoSeleccionPostulanteAlInicio(lista, idPostulanteSeleccionado)
                }
            } catch (e: Exception) {
                Log.e("Servicio6", "Error fetch postulantes", e)
                progressBar.visibility = View.GONE
                configurarVistaPostulante()
            }
        }
    }

    private fun mostrarDialogoSeleccionPostulanteAlInicio(lista: List<Pair<String, String>>, preSeleccionado: String? = null) {
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
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit { putString(Constants.KEY_ID_POSTULANTE, idPostulanteSeleccionado) }
                }
                configurarVistaPostulante()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> configurarVistaPostulante() }
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
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit { putString(Constants.KEY_ID_POSTULANTE, idPostulanteSeleccionado) }
                    configurarVistaPostulante()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
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
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit { putString(Constants.KEY_ID_POSTULANTE, idPostulanteSeleccionado) }
                    configurarVistaPostulante()
                    postularse(nit, idOferta)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

    inner class OfertaAdapter(
        private val items: List<ResultadoOferta>,
        private val onClick: (ResultadoOferta) -> Unit
    ) : RecyclerView.Adapter<OfertaAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            if (item.yaPostulado) {
                h.tv1.text = "${item.titulo} - ${item.empresa} (Postulado)"
                val estado = item.estadoPostulacion ?: "activo"
                h.tv2.text = "[Mis Postulaciones] - Estado: $estado - ${item.municipio}, ${item.departamento}"
                h.tv1.setTextColor(0xFF2E7D32.toInt())
            } else {
                h.tv1.text = "${item.titulo} - ${item.empresa}"
                h.tv2.text = "${item.municipio}, ${item.departamento} - ${item.fechaPub}"
                h.tv1.setTextColor(0xFF0D1A4A.toInt())
            }
            h.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = items.size
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tv1: TextView = view.findViewById(android.R.id.text1)
            val tv2: TextView = view.findViewById(android.R.id.text2)
        }
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private fun addSectionHeader(context: Context, container: LinearLayout, text: String) {
        val tv = TextView(context).apply { this.text = text; textSize = 14f; setTextColor(0xFF3366FF.toInt()); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 8, 0, 4) }
        container.addView(tv)
    }
    private fun addDetailRow(context: Context, container: LinearLayout, label: String, value: String) {
        if (value.isBlank()) return
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 3, 8, 3) }
        val tvL = TextView(context).apply { text = label; textSize = 12f; setTextColor(0x99000000.toInt()); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f) }
        val tvV = TextView(context).apply { text = value; textSize = 13f; setTextColor(0xDD000000.toInt()); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f) }
        row.addView(tvL); row.addView(tvV); container.addView(row)
    }
    private fun Int.toDp(context: Context) = (this * context.resources.displayMetrics.density).toInt()
}
