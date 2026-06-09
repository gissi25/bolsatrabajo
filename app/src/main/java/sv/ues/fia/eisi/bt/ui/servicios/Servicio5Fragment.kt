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

class Servicio5Fragment : Fragment() {

    data class ResultadoTabla(val nombre: String, val insertados: Int)
    data class ResultadoSeccion(
        val titulo: String, val icono: String,
        val tablas: List<ResultadoTabla>, val exito: Boolean,
        val error: String? = null
    )
    data class PreviewItem(val linea1: String, val linea2: String, val nit: String = "", val idOferta: String = "", val idPostulacion: String = "")

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var tvEstadoDescarga: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnReintentar: MaterialButton

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

    private lateinit var cardPostulantes: MaterialCardView
    private lateinit var tvBadgePostulantes: TextView
    private lateinit var tvStatusPostulantes: TextView
    private lateinit var rvPostulantes: RecyclerView
    private lateinit var btnVerMasPostulantes: MaterialButton
    private val postulantesList = mutableListOf<PreviewItem>()

    private lateinit var cardCatalogos: MaterialCardView
    private lateinit var tvBadgeCatalogos: TextView
    private lateinit var tvStatusCatalogos: TextView
    private lateinit var rvCatalogos: RecyclerView
    private lateinit var btnVerMasCatalogos: MaterialButton
    private val catalogosList = mutableListOf<PreviewItem>()

    private var role = Constants.ROLE_POSTULANTE
    private var idPostulanteSeleccionado: String? = null
    private var nitEmpresaSeleccionado: String? = null
    private var estadoCatalogos = false
    private var estadoEmpresas = false
    private var estadoOfertas = false
    private var estadoPostulantes = false
    private var estadoPostulaciones = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio5, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        role = Constants.ROLE_ADMIN

        toolbar = view.findViewById(R.id.toolbar)
        toolbar.menu.clear()
        toolbar.menu.add("Cambiar Perfil").setOnMenuItemClickListener {
            val lista = queryPostulantesParaSeleccion()
            if (lista.isNotEmpty()) {
                mostrarDialogoSeleccionPostulanteCambiar(lista)
            } else {
                Snackbar.make(requireView(), "No hay perfiles de postulantes disponibles", Snackbar.LENGTH_LONG).show()
            }
            true
        }

        progressBar = view.findViewById(R.id.progressBar)
        tvEstadoDescarga = view.findViewById(R.id.tvEstadoDescarga)
        scrollView = view.findViewById(R.id.scrollView)
        btnReintentar = view.findViewById(R.id.btnReintentar)
        cardOfertas = view.findViewById(R.id.cardOfertas)
        tvBadgeOfertas = view.findViewById(R.id.tvBadgeOfertas); tvStatusOfertas = view.findViewById(R.id.tvStatusOfertas)
        rvOfertas = view.findViewById(R.id.rvOfertas); btnVerMasOfertas = view.findViewById(R.id.btnVerMasOfertas)
        cardPostulaciones = view.findViewById(R.id.cardPostulaciones)
        tvBadgePostulaciones = view.findViewById(R.id.tvBadgePostulaciones); tvStatusPostulaciones = view.findViewById(R.id.tvStatusPostulaciones)
        rvPostulaciones = view.findViewById(R.id.rvPostulaciones); btnVerMasPostulaciones = view.findViewById(R.id.btnVerMasPostulaciones)
        cardEmpresas = view.findViewById(R.id.cardEmpresas)
        tvBadgeEmpresas = view.findViewById(R.id.tvBadgeEmpresas); tvStatusEmpresas = view.findViewById(R.id.tvStatusEmpresas)
        rvEmpresas = view.findViewById(R.id.rvEmpresas); btnVerMasEmpresas = view.findViewById(R.id.btnVerMasEmpresas)
        cardPostulantes = view.findViewById(R.id.cardPostulantes)
        tvBadgePostulantes = view.findViewById(R.id.tvBadgePostulantes); tvStatusPostulantes = view.findViewById(R.id.tvStatusPostulantes)
        rvPostulantes = view.findViewById(R.id.rvPostulantes); btnVerMasPostulantes = view.findViewById(R.id.btnVerMasPostulantes)
        cardCatalogos = view.findViewById(R.id.cardCatalogos)
        tvBadgeCatalogos = view.findViewById(R.id.tvBadgeCatalogos); tvStatusCatalogos = view.findViewById(R.id.tvStatusCatalogos)
        rvCatalogos = view.findViewById(R.id.rvCatalogos); btnVerMasCatalogos = view.findViewById(R.id.btnVerMasCatalogos)

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        btnReintentar.setOnClickListener { iniciarDescarga() }
        setupRecyclerViews()
        aplicarVisibilidadPorRol()
        verificarSeleccion()
    }

    private fun setupRecyclerViews() {
        rvOfertas.layoutManager = LinearLayoutManager(requireContext())
        rvPostulaciones.layoutManager = LinearLayoutManager(requireContext())
        rvEmpresas.layoutManager = LinearLayoutManager(requireContext())
        rvPostulantes.layoutManager = LinearLayoutManager(requireContext())
        rvCatalogos.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun aplicarVisibilidadPorRol() {
        cardCatalogos.visibility = View.VISIBLE
        cardPostulantes.visibility = View.VISIBLE
        cardOfertas.visibility = View.VISIBLE
        cardPostulaciones.visibility = View.VISIBLE
        cardEmpresas.visibility = View.VISIBLE
    }

    private fun verificarSeleccion() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        idPostulanteSeleccionado = prefs.getString(Constants.KEY_ID_POSTULANTE, null)
        nitEmpresaSeleccionado = prefs.getString(Constants.KEY_NIT_EMPRESA, null)

        val lista = queryPostulantesParaSeleccion()
        if (lista.isEmpty()) {
            iniciarDescarga()
        } else {
            mostrarDialogoSeleccionPostulante(lista)
        }
    }

    private fun queryPostulantesParaSeleccion(): List<Pair<String, String>> {
        val lista = mutableListOf<Pair<String, String>>()
        try {
            val db = ConnectionHelper(requireContext()).writableDb
            val c = db.rawQuery("SELECT ID_POSTULANTE, NOMBRE, APELLIDO FROM POSTULANTE ORDER BY APELLIDO, NOMBRE", null)
            while (c.moveToNext()) lista.add(Pair(c.getString(0), "${c.getString(1)} ${c.getString(2)}"))
            c.close(); db.close()
        } catch (_: Exception) {}
        return lista
    }

    private fun mostrarDialogoSeleccionPostulante(lista: List<Pair<String, String>>) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 8) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tvTitulo = TextView(context).apply { text = getString(R.string.s5_seleccionar_postulante); textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 12); setTextColor(0xFF0D1A4A.toInt()) }
        container.addView(tvTitulo)
        val radioGroup = RadioGroup(context)
        val radioButtons = lista.mapIndexed { idx, (id, nombre) ->
            RadioButton(context).apply { text = "$id — $nombre"; tag = idx; this.id = View.generateViewId(); setPadding(4, 8, 4, 8); setTextColor(0xDD000000.toInt()) }
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
                    iniciarDescarga()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                iniciarDescarga()
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
            RadioButton(context).apply { text = "$id — $nombre"; tag = idx; this.id = View.generateViewId(); setPadding(4, 8, 4, 8); setTextColor(0xDD000000.toInt()) }
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
                    populateSecciones()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
    }

    // =========================================================================
    // AUTO-DESCARGA + POPULACION DE SECCIONES
    // =========================================================================

    private fun iniciarDescarga() {
        btnReintentar.isEnabled = false; btnReintentar.visibility = View.GONE
        scrollView.visibility = View.GONE; progressBar.visibility = View.VISIBLE
        tvEstadoDescarga.visibility = View.VISIBLE; tvEstadoDescarga.text = getString(R.string.s5_descargando)
        estadoCatalogos = false; estadoEmpresas = false; estadoOfertas = false
        estadoPostulantes = false; estadoPostulaciones = false

        lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { syncCatalogos(ApiService.getCatalogos()) }; estadoCatalogos = true } catch (_: Exception) {}
            try { withContext(Dispatchers.IO) { syncEmpresas(ApiService.getEmpresasFull()) }; estadoEmpresas = true } catch (_: Exception) {} 
            try { withContext(Dispatchers.IO) { syncOfertas(ApiService.getOfertasFull()) }; estadoOfertas = true } catch (_: Exception) {}
            try { withContext(Dispatchers.IO) { syncPostulantes(ApiService.getPostulantesFull()) }; estadoPostulantes = true } catch (_: Exception) {}
            try { withContext(Dispatchers.IO) { syncPostulaciones(ApiService.getPostulacionesFull(null)) }; estadoPostulaciones = true } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                populateSecciones()
                progressBar.visibility = View.GONE; tvEstadoDescarga.visibility = View.GONE
                scrollView.visibility = View.VISIBLE; btnReintentar.isEnabled = true; btnReintentar.visibility = View.VISIBLE
            }
        }
    }

    private fun populateSecciones() {
        val (filtroPostulaciones: String?, argsPostulaciones: Array<String>?) = if (!idPostulanteSeleccionado.isNullOrBlank()) {
            " AND p.ID_POSTULANTE = ?" to arrayOf(idPostulanteSeleccionado!!)
        } else {
            null to null
        }

        // --- OFERTAS ---
        if (estadoOfertas) {
            tvStatusOfertas.text = "[OK]"; tvStatusOfertas.setTextColor(0xFF2E7D32.toInt())
            ofertasList.clear(); ofertasList.addAll(queryOfertasPreview()); tvBadgeOfertas.text = ofertasList.size.toString()
        } else { tvStatusOfertas.text = "[X]"; tvStatusOfertas.setTextColor(0xFFC62828.toInt()) }
        rvOfertas.adapter = PreviewAdapter(ofertasList) { item -> mostrarDetalleOferta(item.nit, item.idOferta) }
        btnVerMasOfertas.text = getString(R.string.s5_ver_mas, ofertasList.size)
        btnVerMasOfertas.setOnClickListener { mostrarSubDialogo("OFERTA_TRABAJO", null) }
        btnVerMasOfertas.visibility = if (ofertasList.isNotEmpty()) View.VISIBLE else View.GONE

        // --- POSTULACIONES ---
        if (estadoPostulaciones) {
            tvStatusPostulaciones.text = "[OK]"; tvStatusPostulaciones.setTextColor(0xFF2E7D32.toInt())
            postulacionesList.clear(); postulacionesList.addAll(queryPostulacionesPreview()); tvBadgePostulaciones.text = postulacionesList.size.toString()
        } else { tvStatusPostulaciones.text = "[X]"; tvStatusPostulaciones.setTextColor(0xFFC62828.toInt()) }
        rvPostulaciones.adapter = PreviewAdapter(postulacionesList) { item -> mostrarDetallePostulacion(item.idPostulacion) }
        btnVerMasPostulaciones.text = getString(R.string.s5_ver_mas, postulacionesList.size)
        btnVerMasPostulaciones.setOnClickListener { mostrarSubDialogo("POSTULACION", filtroPostulaciones, argsPostulaciones) }
        btnVerMasPostulaciones.visibility = if (postulacionesList.isNotEmpty()) View.VISIBLE else View.GONE

        // --- EMPRESAS ---
        if (estadoEmpresas) {
            tvStatusEmpresas.text = "[OK]"; tvStatusEmpresas.setTextColor(0xFF2E7D32.toInt())
            empresasList.clear(); empresasList.addAll(queryEmpresasPreview()); tvBadgeEmpresas.text = empresasList.size.toString()
        } else { tvStatusEmpresas.text = "[X]"; tvStatusEmpresas.setTextColor(0xFFC62828.toInt()) }
        rvEmpresas.adapter = PreviewAdapter(empresasList)
        btnVerMasEmpresas.text = getString(R.string.s5_ver_mas, empresasList.size)
        btnVerMasEmpresas.setOnClickListener { mostrarSubDialogo("EMPRESA", null) }
        btnVerMasEmpresas.visibility = if (empresasList.isNotEmpty()) View.VISIBLE else View.GONE

        // --- POSTULANTES ---
        if (cardPostulantes.visibility == View.VISIBLE) {
            if (estadoPostulantes) {
                tvStatusPostulantes.text = "[OK]"; tvStatusPostulantes.setTextColor(0xFF2E7D32.toInt())
                postulantesList.clear(); postulantesList.addAll(queryPostulantesPreview()); tvBadgePostulantes.text = postulantesList.size.toString()
            } else { tvStatusPostulantes.text = "[X]"; tvStatusPostulantes.setTextColor(0xFFC62828.toInt()) }
            rvPostulantes.adapter = PreviewAdapter(postulantesList)
            btnVerMasPostulantes.text = getString(R.string.s5_ver_mas, postulantesList.size)
            btnVerMasPostulantes.setOnClickListener { mostrarSubDialogo("POSTULANTE") }
            btnVerMasPostulantes.visibility = if (postulantesList.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // --- CATALOGOS ---
        if (cardCatalogos.visibility == View.VISIBLE) {
            if (estadoCatalogos) {
                tvStatusCatalogos.text = "[OK]"; tvStatusCatalogos.setTextColor(0xFF2E7D32.toInt())
                catalogosList.clear(); catalogosList.addAll(queryCatalogosPreview()); tvBadgeCatalogos.text = getString(R.string.s5_n_tablas, catalogosList.size)
            } else { tvStatusCatalogos.text = "[X]"; tvStatusCatalogos.setTextColor(0xFFC62828.toInt()) }
            rvCatalogos.adapter = PreviewAdapter(catalogosList)
            btnVerMasCatalogos.text = getString(R.string.s5_ver_catalogos)
            btnVerMasCatalogos.setOnClickListener { mostrarDialogoCatalogos() }
            btnVerMasCatalogos.visibility = if (catalogosList.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    // =========================================================================
    // QUERIES DE PREVIEW
    // =========================================================================

    private fun queryOfertasPreview(): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        try {
            val db = ConnectionHelper(requireContext()).writableDb
            val c = db.rawQuery("SELECT o.TITULO_PUESTO, e.NOMBRE_EMPRESA, o.FECHA_PUBLICACION, o.NIT, o.ID_OFERTA FROM OFERTA_TRABAJO o LEFT JOIN EMPRESA e ON o.NIT = e.NIT ORDER BY o.FECHA_PUBLICACION DESC LIMIT 5", null)
            while (c.moveToNext()) { val titulo = c.getString(0) ?: ""; val empresa = c.getString(1) ?: ""; val fecha = c.getString(2) ?: ""; val nit = c.getString(3) ?: ""; val idOf = c.getString(4) ?: ""; lista.add(PreviewItem(titulo, "$empresa · $fecha", nit = nit, idOferta = idOf)) }
            c.close(); db.close()
        } catch (_: Exception) {}
        return lista
    }

    private fun queryPostulacionesPreview(): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        try {
            val db = ConnectionHelper(requireContext()).writableDb
            val whereLast = mutableListOf<String>(); val args = mutableListOf<String>()
            if (!idPostulanteSeleccionado.isNullOrBlank()) { whereLast.add("p.ID_POSTULANTE = ?"); args.add(idPostulanteSeleccionado!!) }
            val where = if (whereLast.isNotEmpty()) " WHERE " + whereLast.joinToString(" AND ") else ""
            val c = db.rawQuery("SELECT p.ID_POSTULACION, o.TITULO_PUESTO, p.ESTADO_PROCESO, p.FECHA_APLICACION FROM POSTULACION p LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA$where ORDER BY p.FECHA_APLICACION DESC LIMIT 5", if (args.isNotEmpty()) args.toTypedArray() else null)
            while (c.moveToNext()) { val id = c.getString(0) ?: ""; val oferta = c.getString(1) ?: ""; val estado = c.getString(2) ?: ""; val fecha = c.getString(3) ?: ""; lista.add(PreviewItem("$id → $oferta", "$estado · $fecha", idPostulacion = id)) }
            c.close(); db.close()
        } catch (_: Exception) {}
        return lista
    }

    private fun queryEmpresasPreview(): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        try {
            val db = ConnectionHelper(requireContext()).writableDb
            val c = db.rawQuery("SELECT NOMBRE_EMPRESA, NIT, CONTACTO_DIRECTO FROM EMPRESA ORDER BY NOMBRE_EMPRESA LIMIT 5", null)
            while (c.moveToNext()) { val nombre = c.getString(0) ?: ""; val nit = c.getString(1) ?: ""; val contacto = c.getString(2) ?: ""; lista.add(PreviewItem("$nombre · $nit", if (!contacto.isNullOrBlank()) contacto else "—")) }
            c.close(); db.close()
        } catch (_: Exception) {}
        return lista
    }

    private fun queryPostulantesPreview(): List<PreviewItem> {
        val lista = mutableListOf<PreviewItem>()
        try {
            val db = ConnectionHelper(requireContext()).writableDb
            val c = db.rawQuery("SELECT NOMBRE, APELLIDO, ID_POSTULANTE, EMAIL FROM POSTULANTE ORDER BY APELLIDO, NOMBRE LIMIT 5", null)
            while (c.moveToNext()) { val nombre = c.getString(0) ?: ""; val apellido = c.getString(1) ?: ""; val id = c.getString(2) ?: ""; val email = c.getString(3) ?: ""; lista.add(PreviewItem("$nombre $apellido · $id", if (!email.isNullOrBlank()) email else "—")) }
            c.close()
            db.close()
        } catch (_: Exception) {}
        return lista
    }

    private fun queryCatalogosPreview(): List<PreviewItem> {
        val tablas = listOf("CATEGORIA_HABILIDAD", "GENERO", "TIPO_DOCUMENTO", "DEPARTAMENTO", "MUNICIPIO", "DISTRITO", "INSTITUCION", "GRADO_ACADEMICO", "RED_SOCIAL", "TIPO_CERTIFICACION", "HABILIDAD", "OFERTA_ACADEMICA")
        return tablas.map { tabla ->
            val count = try { val db = ConnectionHelper(requireContext()).writableDb; val c = db.rawQuery("SELECT COUNT(*) FROM $tabla", null); val n = if (c.moveToFirst()) c.getInt(0) else 0; c.close(); db.close(); n } catch (_: Exception) { 0 }
            val nombre = tabla.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }; PreviewItem(nombre, getString(R.string.records) + ": $count")
        }
    }

    // =========================================================================
    // DIALOGOS DE DETALLE
    // =========================================================================

    private fun mostrarDialogoCatalogos() {
        val context = requireContext(); val sv = ScrollView(context).apply { setPadding(32, 16, 32, 16) }; val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tablas = listOf("CATEGORIA_HABILIDAD", "GENERO", "TIPO_DOCUMENTO", "DEPARTAMENTO", "MUNICIPIO", "DISTRITO", "INSTITUCION", "GRADO_ACADEMICO", "RED_SOCIAL", "TIPO_CERTIFICACION", "HABILIDAD", "OFERTA_ACADEMICA")
        for (tabla in tablas) {
            val count = try { val db = ConnectionHelper(context).writableDb; val c = db.rawQuery("SELECT COUNT(*) FROM $tabla", null); val n = if (c.moveToFirst()) c.getInt(0) else 0; c.close(); db.close(); n } catch (_: Exception) { 0 }
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 4) }
            val tvNombre = TextView(context).apply { text = tabla.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }; textSize = 13f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f) }
            val tvCount = TextView(context).apply { text = "+$count"; textSize = 13f; setTextColor(0xFF1565C0.toInt()); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f) }
            val btnVer = MaterialButton(context).apply { text = "[Ver]"; textSize = 11f; setPadding(8, 0, 8, 0); isAllCaps = false; layoutParams = LinearLayout.LayoutParams(0, 60, 0.3f); setOnClickListener { mostrarSubDialogo(tabla) } }
            row.addView(tvNombre); row.addView(tvCount); row.addView(btnVer); container.addView(row)
        }
        sv.addView(container); AlertDialog.Builder(context).setTitle(getString(R.string.s5_titulo_catalogos)).setView(sv).setPositiveButton(getString(R.string.cerrar), null).show()
    }

    private fun mostrarSubDialogo(nombreTabla: String, whereExtra: String? = null, args: Array<String>? = null) {
        lifecycleScope.launch {
            try {
                val datos = withContext(Dispatchers.IO) { consultarTablaLocal(nombreTabla, whereExtra, args) }; val context = requireContext()
                val sv = ScrollView(context).apply { setPadding(24, 8, 24, 8) }; val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                if (datos.isEmpty()) { val tv = TextView(context).apply { text = getString(R.string.s3_sin_datos); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24) }; container.addView(tv) }
                else for (fila in datos) { val tv = TextView(context).apply { text = fila; textSize = 12f; setPadding(0, 6, 0, 6); setTextColor(0xDD000000.toInt()) }; container.addView(tv); container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(0x11000000) }) }
                sv.addView(container); AlertDialog.Builder(context).setTitle("$nombreTabla — ${datos.size} ${getString(R.string.records)}").setView(sv).setPositiveButton(getString(R.string.cerrar), null).show()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun consultarTablaLocal(nombreTabla: String, whereExtra: String? = null, args: Array<String>? = null): List<String> {
        val db = ConnectionHelper(requireContext()).writableDb; val resultado = mutableListOf<String>()
        try {
            when (nombreTabla) {
                "CATEGORIA_HABILIDAD" -> { val c = db.rawQuery("SELECT NOMBRE_CATEGORIA FROM CATEGORIA_HABILIDAD ORDER BY NOMBRE_CATEGORIA", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "GENERO" -> { val c = db.rawQuery("SELECT NOMBRE_GENERO FROM GENERO ORDER BY NOMBRE_GENERO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "TIPO_DOCUMENTO" -> { val c = db.rawQuery("SELECT NOMBRE_TIPO FROM TIPO_DOCUMENTO ORDER BY NOMBRE_TIPO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "DEPARTAMENTO" -> { val c = db.rawQuery("SELECT NOMBRE_DEPARTAMENTO FROM DEPARTAMENTO ORDER BY NOMBRE_DEPARTAMENTO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "MUNICIPIO" -> { val c = db.rawQuery("SELECT m.ID_MUNICIPIO || ' - ' || m.NOMBRE_MUNICIPIO || ' (' || d.NOMBRE_DEPARTAMENTO || ')' FROM MUNICIPIO m LEFT JOIN DEPARTAMENTO d ON m.ID_DEPARTAMENTO = d.ID_DEPARTAMENTO ORDER BY d.NOMBRE_DEPARTAMENTO, m.NOMBRE_MUNICIPIO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "DISTRITO" -> { val c = db.rawQuery("SELECT d.ID_DISTRITO || ' - ' || d.NOMBRE_DISTRITO || ' (' || m.NOMBRE_MUNICIPIO || ')' FROM DISTRITO d LEFT JOIN MUNICIPIO m ON d.ID_DEPARTAMENTO = m.ID_DEPARTAMENTO AND d.ID_MUNICIPIO = m.ID_MUNICIPIO ORDER BY m.NOMBRE_MUNICIPIO, d.NOMBRE_DISTRITO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "INSTITUCION" -> { val c = db.rawQuery("SELECT NOMBRE_INSTITUCION FROM INSTITUCION ORDER BY NOMBRE_INSTITUCION", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "GRADO_ACADEMICO" -> { val c = db.rawQuery("SELECT NOMBRE_GRADO FROM GRADO_ACADEMICO ORDER BY NOMBRE_GRADO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "RED_SOCIAL" -> { val c = db.rawQuery("SELECT NOMBRE_RED FROM RED_SOCIAL ORDER BY NOMBRE_RED", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "TIPO_CERTIFICACION" -> { val c = db.rawQuery("SELECT NOMBRE_TIPO FROM TIPO_CERTIFICACION ORDER BY NOMBRE_TIPO", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "HABILIDAD" -> { val c = db.rawQuery("SELECT h.NOMBRE_HABILIDAD || ' [' || IFNULL(ch.NOMBRE_CATEGORIA, '—') || ']' FROM HABILIDAD h LEFT JOIN CATEGORIA_HABILIDAD ch ON h.ID_CATEGORIA_HABILIDAD = ch.ID_CATEGORIA_HABILIDAD ORDER BY h.NOMBRE_HABILIDAD", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "OFERTA_ACADEMICA" -> { val c = db.rawQuery("SELECT oa.ID_OFERTA_ACADEMICA || ' — ' || i.NOMBRE_INSTITUCION || ' (' || g.NOMBRE_GRADO || ')' FROM OFERTA_ACADEMICA oa LEFT JOIN INSTITUCION i ON oa.ID_INSTITUCION = i.ID_INSTITUCION LEFT JOIN GRADO_ACADEMICO g ON oa.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO ORDER BY i.NOMBRE_INSTITUCION", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "EMPRESA" -> { val sql = "SELECT NIT || ' — ' || NOMBRE_EMPRESA || '  [' || IFNULL(CONTACTO_DIRECTO, '—') || ']' FROM EMPRESA WHERE 1=1${whereExtra ?: ""} ORDER BY NOMBRE_EMPRESA"; val c = db.rawQuery(sql, args); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "OFERTA_TRABAJO" -> { val sql = "SELECT o.ID_OFERTA || ' — ' || o.TITULO_PUESTO || ' (' || e.NOMBRE_EMPRESA || ')  ' || IFNULL(o.FECHA_PUBLICACION, '') || ' → ' || IFNULL(o.FECHA_CADUCIDAD, '') FROM OFERTA_TRABAJO o LEFT JOIN EMPRESA e ON o.NIT = e.NIT WHERE 1=1${whereExtra ?: ""} ORDER BY o.FECHA_PUBLICACION DESC"; val c = db.rawQuery(sql, args); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "DETALLE_REQUISITO" -> { val c = db.rawQuery("SELECT d.ID_DETALLE || ' — ' || d.DESCRIPCION_REQUISITO || '  [' || o.TITULO_PUESTO || ']' FROM DETALLE_REQUISITO d LEFT JOIN OFERTA_TRABAJO o ON d.NIT = o.NIT AND d.ID_OFERTA = o.ID_OFERTA ORDER BY o.TITULO_PUESTO, d.ID_DETALLE", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "POSTULANTE" -> { val c = db.rawQuery("SELECT ID_POSTULANTE || ' — ' || NOMBRE || ' ' || APELLIDO || '  [' || IFNULL(EMAIL, '—') || ']' FROM POSTULANTE ORDER BY APELLIDO, NOMBRE", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "FORMACION_ACADEMICA" -> { val c = db.rawQuery("SELECT f.TITULO_OBTENIDO || ' — ' || IFNULL(i.NOMBRE_INSTITUCION, '—') || '  [' || IFNULL(f.FECHA_INICIO, '?') || ' → ' || IFNULL(f.FECHA_FIN, '?') || ']' FROM FORMACION_ACADEMICA f LEFT JOIN OFERTA_ACADEMICA oa ON f.ID_OFERTA_ACADEMICA = oa.ID_OFERTA_ACADEMICA LEFT JOIN INSTITUCION i ON oa.ID_INSTITUCION = i.ID_INSTITUCION ORDER BY f.FECHA_FIN DESC", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "EXPERIENCIA_LABORAL" -> { val c = db.rawQuery("SELECT e.PUESTO_TRABAJO || ' en ' || IFNULL(em.NOMBRE_EMPRESA, e.NIT) || '  [' || IFNULL(e.FECHA_INICIO, '?') || ' → ' || IFNULL(e.FECHA_FIN, '?') || ']' FROM EXPERIENCIA_LABORAL e LEFT JOIN EMPRESA em ON e.NIT = em.NIT ORDER BY e.FECHA_FIN DESC", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "CERTIFICACION" -> { val c = db.rawQuery("SELECT c.NOMBRE_CERTIFICACION || ' — ' || IFNULL(i.NOMBRE_INSTITUCION, '—') || ' (' || IFNULL(tc.NOMBRE_TIPO, '—') || ')' FROM CERTIFICACION c LEFT JOIN INSTITUCION i ON c.ID_INSTITUCION = i.ID_INSTITUCION LEFT JOIN TIPO_CERTIFICACION tc ON c.ID_TIPO_CERTIFICACION = tc.ID_TIPO_CERTIFICACION ORDER BY c.FECHA_CERTIFICACION DESC", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "HABILIDAD_POSTULANTE" -> { val c = db.rawQuery("SELECT h.NOMBRE_HABILIDAD || ' — ' || hp.NIVEL_DESTREZA FROM HABILIDAD_POSTULANTE hp LEFT JOIN HABILIDAD h ON hp.ID_CATEGORIA_HABILIDAD = h.ID_CATEGORIA_HABILIDAD AND hp.ID_HABILIDAD = h.ID_HABILIDAD ORDER BY h.NOMBRE_HABILIDAD", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "RED_SOCIAL_POSTULANTE" -> { val c = db.rawQuery("SELECT rs.NOMBRE_RED || ': ' || IFNULL(rp.URL_PERFIL, '—') FROM RED_SOCIAL_POSTULANTE rp LEFT JOIN RED_SOCIAL rs ON rp.ID_RED_SOCIAL = rs.ID_RED_SOCIAL ORDER BY rs.NOMBRE_RED", null); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
                "POSTULACION" -> { val sql = "SELECT p.ID_POSTULACION || ' — ' || p.ID_POSTULANTE || ' → ' || IFNULL(o.TITULO_PUESTO, p.ID_OFERTA) || '  [' || p.ESTADO_PROCESO || ']  ' || IFNULL(p.FECHA_APLICACION, '') FROM POSTULACION p LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA WHERE 1=1${whereExtra ?: ""} ORDER BY p.FECHA_APLICACION DESC"; val c = db.rawQuery(sql, args); while (c.moveToNext()) resultado.add(c.getString(0)); c.close() }
            }
        } catch (_: Exception) {} finally { db.close() }
        return resultado
    }

    // =========================================================================
    // DETALLE DE OFERTA + POSTULACION
    // =========================================================================

    private fun mostrarDetalleOferta(nit: String, idOferta: String) {
        lifecycleScope.launch {
            try {
                val (oferta, requisitos) = withContext(Dispatchers.IO) { queryDetalleOferta(nit, idOferta) }
                if (oferta == null) { Snackbar.make(requireView(), "Oferta no encontrada", Snackbar.LENGTH_SHORT).show(); return@launch }
                val context = requireContext(); val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }; val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                val tvTitulo = TextView(context).apply { text = oferta["titulo"]; textSize = 18f; setTextColor(0xFF0D1A4A.toInt()); setTypeface(null, android.graphics.Typeface.BOLD) }
                container.addView(tvTitulo)
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }; setBackgroundColor(0xFF3366FF.toInt()) })
                addSectionHeader(context, container, getString(R.string.s5_datos_oferta))
                addDetailRow(context, container, getString(R.string.s5_empresa), "${oferta["empresa"]} · ${oferta["nit"]}")
                addDetailRow(context, container, getString(R.string.s5_grado), oferta["grado"] ?: "—")
                addDetailRow(context, container, getString(R.string.s5_publicacion), oferta["fechaPub"] ?: "—")
                addDetailRow(context, container, getString(R.string.s5_caducidad), oferta["fechaCad"] ?: "—")
                addDetailRow(context, container, getString(R.string.s5_experiencia), oferta["exp"] ?: "—")
                addDetailRow(context, container, getString(R.string.s5_edad), oferta["edad"] ?: "—")
                addDetailRow(context, container, getString(R.string.s5_descripcion), oferta["desc"] ?: "—")
                if (requisitos.isNotEmpty()) {
                    container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 4 }; setBackgroundColor(0x22000000) })
                    addSectionHeader(context, container, "${getString(R.string.s5_requisitos)} (${requisitos.size})")
                    for (req in requisitos) { val tv = TextView(context).apply { text = "• $req"; textSize = 13f; setTextColor(0xDD000000.toInt()); setPadding(16, 2, 0, 2) }; container.addView(tv) }
                }
                container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 12 }; setBackgroundColor(0x22000000) })
                val btnPostular = MaterialButton(context).apply { text = getString(R.string.s5_postularse); setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0xFF3366FF.toInt()); cornerRadius = 24.toDp(context); isAllCaps = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56.toDp(context)) }.also { container.addView(it) }
                sv.addView(container)
                val dialog = AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).create()
                btnPostular.setOnClickListener { dialog.dismiss(); postularse(nit, idOferta) }
                dialog.show()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun queryDetalleOferta(nit: String, idOferta: String): Pair<Map<String, String>?, List<String>> {
        val db = ConnectionHelper(requireContext()).writableDb
        try {
            val c = db.rawQuery("SELECT o.TITULO_PUESTO, e.NOMBRE_EMPRESA, o.NIT, g.NOMBRE_GRADO, o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, o.EXPERIENCIA_ANIOS, o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO FROM OFERTA_TRABAJO o LEFT JOIN EMPRESA e ON o.NIT = e.NIT LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO WHERE o.NIT = ? AND o.ID_OFERTA = ?", arrayOf(nit, idOferta))
            if (!c.moveToFirst()) { c.close(); db.close(); return Pair(null, emptyList()) }
            val exp = c.getString(6) ?: ""; val edadMin = c.getString(7) ?: ""; val edadMax = c.getString(8) ?: ""
            val edadStr = if (edadMin.isNotEmpty() || edadMax.isNotEmpty()) "$edadMin - $edadMax" else ""
            val oferta = mapOf("titulo" to (c.getString(0) ?: ""), "empresa" to (c.getString(1) ?: ""), "nit" to (c.getString(2) ?: ""), "grado" to (c.getString(3) ?: ""), "fechaPub" to (c.getString(4) ?: ""), "fechaCad" to (c.getString(5) ?: ""), "exp" to (if (exp.isNotEmpty()) "$exp años" else ""), "edad" to edadStr, "desc" to (c.getString(9) ?: ""))
            c.close()
            val requisitos = mutableListOf<String>(); val rc = db.rawQuery("SELECT DESCRIPCION_REQUISITO FROM DETALLE_REQUISITO WHERE NIT = ? AND ID_OFERTA = ? ORDER BY ID_DETALLE", arrayOf(nit, idOferta))
            while (rc.moveToNext()) { rc.getString(0)?.trim()?.takeIf { it.isNotEmpty() }?.let { requisitos.add(it) } }; rc.close(); db.close()
            return Pair(oferta, requisitos)
        } catch (_: Exception) { db.close(); return Pair(null, emptyList()) }
    }

    private fun mostrarDetallePostulacion(idPostulacion: String) {
        lifecycleScope.launch {
            try {
                val datos = withContext(Dispatchers.IO) {
                    val db = ConnectionHelper(requireContext()).writableDb; val c = db.rawQuery("SELECT p.ID_POSTULACION, o.TITULO_PUESTO, e.NOMBRE_EMPRESA, p.ESTADO_PROCESO, p.FECHA_APLICACION, p.ID_POSTULANTE FROM POSTULACION p LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA LEFT JOIN EMPRESA e ON p.NIT = e.NIT WHERE p.ID_POSTULACION = ?", arrayOf(idPostulacion))
                    val map = mutableMapOf<String, String>(); if (c.moveToFirst()) { map["id"] = c.getString(0) ?: ""; map["oferta"] = c.getString(1) ?: ""; map["empresa"] = c.getString(2) ?: ""; map["estado"] = c.getString(3) ?: ""; map["fecha"] = c.getString(4) ?: ""; map["postulante"] = c.getString(5) ?: "" }; c.close(); db.close(); map
                }
                if (datos.isEmpty()) { Snackbar.make(requireView(), "Postulacion no encontrada", Snackbar.LENGTH_SHORT).show(); return@launch }
                val context = requireContext(); val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }; val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                val tvTitulo = TextView(context).apply { text = datos["id"] ?: ""; textSize = 18f; setTextColor(0xFF0D1A4A.toInt()); setTypeface(null, android.graphics.Typeface.BOLD) }
                container.addView(tvTitulo); container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }; setBackgroundColor(0xFF3366FF.toInt()) })
                addSectionHeader(context, container, getString(R.string.s5_titulo_postulaciones))
                addDetailRow(context, container, getString(R.string.s5_oferta), "${datos["oferta"]} (${datos["empresa"]})"); addDetailRow(context, container, getString(R.string.s5_estado), datos["estado"] ?: "—")
                addDetailRow(context, container, getString(R.string.s5_fecha), datos["fecha"] ?: "—"); addDetailRow(context, container, "ID Postulante", datos["postulante"] ?: "—")
                sv.addView(container); AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).show()
            } catch (e: Exception) { Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun postularse(nit: String, idOferta: String) {
        lifecycleScope.launch {
            try {
                val idPostulante = idPostulanteSeleccionado ?: run {
                    val lista = queryPostulantesParaSeleccion()
                    if (lista.isEmpty()) {
                        Snackbar.make(requireView(), "No hay perfiles de postulantes disponibles", Snackbar.LENGTH_LONG).show()
                    } else {
                        mostrarDialogoSeleccionPostulanteParaPostularse(lista, nit, idOferta)
                    }
                    return@launch
                }
                val yaPostulado = withContext(Dispatchers.IO) { val db = ConnectionHelper(requireContext()).writableDb; val c = db.rawQuery("SELECT 1 FROM POSTULACION WHERE ID_POSTULANTE = ? AND NIT = ? AND ID_OFERTA = ?", arrayOf(idPostulante, nit, idOferta)); val r = c.moveToFirst(); c.close(); db.close(); r }
                if (yaPostulado) { Snackbar.make(requireView(), getString(R.string.s5_error_ya_postulado), Snackbar.LENGTH_LONG).show(); return@launch }
                val idPostulacion = withContext(Dispatchers.IO) { val db = ConnectionHelper(requireContext()).writableDb; val c = db.rawQuery("SELECT MAX(CAST(SUBSTR(ID_POSTULACION, 4) AS INTEGER)) FROM POSTULACION", null); val next = if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) + 1 else 1; c.close(); db.close(); "POS${next.toString().padStart(3, '0')}" }
                val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                withContext(Dispatchers.IO) { val db = ConnectionHelper(requireContext()).writableDb; db.execSQL("INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO) VALUES ('$idPostulacion', '$nit', '$idOferta', '$idPostulante', '$fecha', 'Pendiente')"); db.close() }
                try {
                    val json = JSONObject().apply { put("id_postulacion", idPostulacion); put("nit", nit); put("id_oferta", idOferta); put("id_postulante", idPostulante); put("fecha_aplicacion", fecha); put("estado_proceso", "Pendiente") }
                    ApiService.insertarPostulacion(json)
                } catch (e: Exception) {
                    Log.e("Servicio5", "Error al sincronizar postulacion", e)
                    Snackbar.make(requireView(), "Guardado local. No se pudo sincronizar: ${e.message}", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                Snackbar.make(requireView(), getString(R.string.s5_postulacion_exitosa), Snackbar.LENGTH_LONG).show()
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
            RadioButton(context).apply { text = "$id — $nombre"; tag = idx; this.id = View.generateViewId(); setPadding(4, 8, 4, 8); setTextColor(0xDD000000.toInt()) }
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
                    populateSecciones()
                    postularse(nit, idOferta)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
    }

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
    // UTILIDADES
    // =========================================================================

    private fun normalizarNivel(valor: String): String {
        val v = valor.lowercase().trim()
        return when { v.contains("basic") -> "Básico"; v.contains("inter") -> "Intermedio"; v.contains("avan") || v.contains("advan") -> "Avanzado"; else -> "Básico" }
    }
    private fun escapar(s: String) = s.replace("'", "''")
    private fun escaparNullable(s: String) = if (s.isBlank()) "NULL" else "'${s.replace("'", "''")}'"

    // =========================================================================
    // SYNC A SQLITE
    // =========================================================================

    private fun syncCatalogos(json: JSONObject) {
        val db = ConnectionHelper(requireContext()).writableDb
        try { db.beginTransaction()
            val mapeo = listOf(
                Triple("categoria_habilidad", "CATEGORIA_HABILIDAD", listOf("ID_CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA")),
                Triple("genero", "GENERO", listOf("ID_GENERO", "NOMBRE_GENERO")), Triple("tipo_documento", "TIPO_DOCUMENTO", listOf("ID_TIPO_DOCUMENTO", "NOMBRE_TIPO")),
                Triple("departamento", "DEPARTAMENTO", listOf("ID_DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")), Triple("municipio", "MUNICIPIO", listOf("ID_DEPARTAMENTO", "ID_MUNICIPIO", "NOMBRE_MUNICIPIO")),
                Triple("distrito", "DISTRITO", listOf("ID_DISTRITO", "NOMBRE_DISTRITO", "ID_DEPARTAMENTO", "ID_MUNICIPIO")), Triple("institucion", "INSTITUCION", listOf("ID_INSTITUCION", "NOMBRE_INSTITUCION")),
                Triple("grado_academico", "GRADO_ACADEMICO", listOf("ID_GRADO_ACADEMICO", "NOMBRE_GRADO")), Triple("red_social", "RED_SOCIAL", listOf("ID_RED_SOCIAL", "NOMBRE_RED")),
                Triple("tipo_certificacion", "TIPO_CERTIFICACION", listOf("ID_TIPO_CERTIFICACION", "NOMBRE_TIPO")), Triple("habilidad", "HABILIDAD", listOf("ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD", "NOMBRE_HABILIDAD")),
                Triple("oferta_academica", "OFERTA_ACADEMICA", listOf("ID_OFERTA_ACADEMICA", "ID_GRADO_ACADEMICO", "ID_INSTITUCION"))
            )
            for ((key, tabla, cols) in mapeo) { val arr = json.optJSONArray(key) ?: continue; val nombresCols = cols.joinToString(", "); for (i in 0 until arr.length()) { val item = arr.getJSONObject(i); val valores = cols.joinToString(", ") { col -> "'${item.optString(col, "").replace("'", "''")}'" }; try { db.execSQL("INSERT OR IGNORE INTO $tabla ($nombresCols) VALUES ($valores)") } catch (_: Exception) {} } }
            db.setTransactionSuccessful()
        } finally { db.endTransaction(); db.close() }
    }

    private fun syncEmpresas(json: JSONObject) {
        val db = ConnectionHelper(requireContext()).writableDb
        try { val arr = json.getJSONArray("data"); db.beginTransaction(); for (i in 0 until arr.length()) { val e = arr.getJSONObject(i); val nit = escapar(e.optString("NIT")); val depto = e.optString("ID_DISTRITO_DEPTO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val muni = e.optString("ID_DISTRITO_MUNICIPIO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val dist = e.optString("ID_DISTRITO_ID", "").takeIf { it.isNotEmpty() } ?: "NULL"; val nombre = escapar(e.optString("NOMBRE_EMPRESA")); val contacto = escapar(e.optString("CONTACTO_DIRECTO")); db.execSQL("INSERT OR REPLACE INTO EMPRESA (NIT, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID, NOMBRE_EMPRESA, CONTACTO_DIRECTO) VALUES ('$nit', $depto, $muni, $dist, '$nombre', '$contacto')") }; db.setTransactionSuccessful() }
        finally { db.endTransaction(); db.close() }
    }

    private fun syncOfertas(json: JSONObject) {
        val db = ConnectionHelper(requireContext()).writableDb
        try { val arr = json.getJSONArray("data"); db.beginTransaction(); for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); val nit = escapar(o.optString("NIT")); val idOferta = escapar(o.optString("ID_OFERTA")); val idGrado = o.optString("ID_GRADO_ACADEMICO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val titulo = escapar(o.optString("TITULO_PUESTO")); val fechaPub = escaparNullable(o.optString("FECHA_PUBLICACION")); val fechaCad = escaparNullable(o.optString("FECHA_CADUCIDAD")); val expAnios = o.optString("EXPERIENCIA_ANIOS", "").takeIf { it.isNotEmpty() } ?: "NULL"; val edadMin = o.optString("EDAD_MINIMA", "").takeIf { it.isNotEmpty() } ?: "NULL"; val edadMax = o.optString("EDAD_MAXIMA", "").takeIf { it.isNotEmpty() } ?: "NULL"; val descripcion = escapar(o.optString("DESCRIPCION_OFERTA_TRABAJO")); db.execSQL("INSERT OR REPLACE INTO OFERTA_TRABAJO (NIT, ID_OFERTA, ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, FECHA_CADUCIDAD, EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, DESCRIPCION_OFERTA_TRABAJO) VALUES ('$nit', '$idOferta', $idGrado, '$titulo', $fechaPub, $fechaCad, $expAnios, $edadMin, $edadMax, '$descripcion')"); db.execSQL("DELETE FROM DETALLE_REQUISITO WHERE NIT = '$nit' AND ID_OFERTA = '$idOferta'"); val requisitos = o.optJSONArray("requisitos"); if (requisitos != null) for (j in 0 until requisitos.length()) { val r = requisitos.getJSONObject(j); db.execSQL("INSERT INTO DETALLE_REQUISITO (NIT, ID_OFERTA, ID_DETALLE, DESCRIPCION_REQUISITO) VALUES ('$nit', '$idOferta', '${escapar(r.optString("ID_DETALLE"))}', '${escapar(r.optString("DESCRIPCION_REQUISITO"))}')") } }; db.setTransactionSuccessful() }
        finally { db.endTransaction(); db.close() }
    }

    private fun syncPostulantes(json: JSONObject) {
        val db = ConnectionHelper(requireContext()).writableDb
        try { val arr = json.getJSONArray("data"); db.beginTransaction(); for (i in 0 until arr.length()) { val p = arr.getJSONObject(i); val idPost = escapar(p.optString("ID_POSTULANTE")); val idGenero = p.optString("ID_GENERO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val idTipoDoc = p.optString("ID_TIPO_DOCUMENTO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val numDoc = escapar(p.optString("NUM_DOCUMENTO")); val idGrado = p.optString("ID_GRADO_ACADEMICO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val nombre = escapar(p.optString("NOMBRE")); val apellido = escapar(p.optString("APELLIDO")); val fechaNac = escaparNullable(p.optString("FECHA_NACIMIENTO")); val nup = escapar(p.optString("NUP")); val email = escapar(p.optString("EMAIL")); val direccion = escapar(p.optString("DIRECCION_DETALLE")); val telCasa = escapar(p.optString("TELEFONO_CASA")); val telCel = escapar(p.optString("TELEFONO_CELULAR")); val distDepto = p.optString("ID_DISTRITO_DEPTO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val distMuni = p.optString("ID_DISTRITO_MUNICIPIO", "").takeIf { it.isNotEmpty() } ?: "NULL"; val distId = p.optString("ID_DISTRITO_ID", "").takeIf { it.isNotEmpty() } ?: "NULL"
            db.execSQL("INSERT OR REPLACE INTO POSTULANTE (ID_POSTULANTE, ID_GENERO, ID_TIPO_DOCUMENTO, NUM_DOCUMENTO, ID_GRADO_ACADEMICO, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUP, EMAIL, DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID) VALUES ('$idPost', $idGenero, $idTipoDoc, '$numDoc', $idGrado, '$nombre', '$apellido', $fechaNac, '$nup', '$email', '$direccion', '$telCasa', '$telCel', $distDepto, $distMuni, $distId)")
            db.execSQL("DELETE FROM FORMACION_ACADEMICA WHERE ID_POSTULANTE = '$idPost'"); val formaciones = p.optJSONArray("formaciones"); if (formaciones != null) for (j in 0 until formaciones.length()) { val f = formaciones.getJSONObject(j); db.execSQL("INSERT INTO FORMACION_ACADEMICA (ID_FORMACION, ID_POSTULANTE, ID_OFERTA_ACADEMICA, TITULO_OBTENIDO, FECHA_INICIO, FECHA_FIN, FECHA_OBTENCION) VALUES ('${escapar(f.optString("ID_FORMACION"))}', '$idPost', '${escapar(f.optString("ID_OFERTA_ACADEMICA"))}', '${escapar(f.optString("TITULO_OBTENIDO"))}', ${escaparNullable(f.optString("FECHA_INICIO"))}, ${escaparNullable(f.optString("FECHA_FIN"))}, ${escaparNullable(f.optString("FECHA_OBTENCION"))})") }
            db.execSQL("DELETE FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = '$idPost'"); val experiencias = p.optJSONArray("experiencias"); if (experiencias != null) for (j in 0 until experiencias.length()) { val e = experiencias.getJSONObject(j); db.execSQL("INSERT INTO EXPERIENCIA_LABORAL (ID_POSTULANTE, NIT, ID_EXPERIENCIA, PUESTO_TRABAJO, FECHA_INICIO, FECHA_FIN, DESCP_EXPERIENCIA_LABORAL, CONTACTO_REFERENCIA) VALUES ('$idPost', '${escapar(e.optString("NIT"))}', '${escapar(e.optString("ID_EXPERIENCIA"))}', '${escapar(e.optString("PUESTO_TRABAJO"))}', ${escaparNullable(e.optString("FECHA_INICIO"))}, ${escaparNullable(e.optString("FECHA_FIN"))}, '${escapar(e.optString("DESCP_EXPERIENCIA_LABORAL"))}', '${escapar(e.optString("CONTACTO_REFERENCIA"))}')") }
            db.execSQL("DELETE FROM CERTIFICACION WHERE ID_POSTULANTE = '$idPost'"); val certificaciones = p.optJSONArray("certificaciones"); if (certificaciones != null) for (j in 0 until certificaciones.length()) { val c = certificaciones.getJSONObject(j); val idTipo = c.optString("ID_TIPO_CERTIFICACION", "").takeIf { it.isNotEmpty() } ?: "NULL"; db.execSQL("INSERT INTO CERTIFICACION (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE, ID_TIPO_CERTIFICACION, NOMBRE_CERTIFICACION, FECHA_CERTIFICACION, FECHA_INICIO, FECHA_FIN) VALUES ('${escapar(c.optString("ID_CERTIFICACION"))}', '${escapar(c.optString("ID_INSTITUCION"))}', '$idPost', $idTipo, '${escapar(c.optString("NOMBRE_CERTIFICACION"))}', ${escaparNullable(c.optString("FECHA_CERTIFICACION"))}, ${escaparNullable(c.optString("FECHA_INICIO"))}, ${escaparNullable(c.optString("FECHA_FIN"))})") }
            db.execSQL("DELETE FROM HABILIDAD_POSTULANTE WHERE ID_POSTULANTE = '$idPost'"); val habilidades = p.optJSONArray("habilidades"); if (habilidades != null) for (j in 0 until habilidades.length()) { val h = habilidades.getJSONObject(j); val idCatHab = h.optString("ID_CATEGORIA_HABILIDAD", "").takeIf { it.isNotEmpty() } ?: "NULL"; val nivel = normalizarNivel(h.optString("NIVEL_DESTREZA", "")); db.execSQL("INSERT INTO HABILIDAD_POSTULANTE (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE, NIVEL_DESTREZA) VALUES ($idCatHab, '${escapar(h.optString("ID_HABILIDAD"))}', '$idPost', '${escapar(nivel)}')") }
            db.execSQL("DELETE FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = '$idPost'"); val redes = p.optJSONArray("redes"); if (redes != null) for (j in 0 until redes.length()) { val r = redes.getJSONObject(j); val idRed = r.optString("ID_RED_SOCIAL", "").takeIf { it.isNotEmpty() } ?: "NULL"; db.execSQL("INSERT INTO RED_SOCIAL_POSTULANTE (ID_POSTULANTE, ID_RED_SOCIAL, URL_PERFIL) VALUES ('$idPost', $idRed, '${escapar(r.optString("URL_PERFIL"))}')") } }; db.setTransactionSuccessful() }
        finally { db.endTransaction(); db.close() }
    }

    private fun syncPostulaciones(json: JSONObject) {
        val db = ConnectionHelper(requireContext()).writableDb
        try { val arr = json.getJSONArray("data"); db.beginTransaction(); for (i in 0 until arr.length()) { val p = arr.getJSONObject(i); db.execSQL("INSERT OR REPLACE INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO) VALUES ('${escapar(p.optString("ID_POSTULACION"))}', '${escapar(p.optString("NIT"))}', '${escapar(p.optString("ID_OFERTA"))}', '${escapar(p.optString("ID_POSTULANTE"))}', '${escapar(p.optString("FECHA_APLICACION"))}', '${escapar(p.optString("ESTADO_PROCESO"))}')") }; db.setTransactionSuccessful() }
        finally { db.endTransaction(); db.close() }
    }

    // =========================================================================
    // ADAPTER
    // =========================================================================

    private class PreviewAdapter(private val items: List<PreviewItem>, private val onClick: ((PreviewItem) -> Unit)? = null) : RecyclerView.Adapter<PreviewAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH { val v = LayoutInflater.from(parent.context).inflate(R.layout.card_preview_fila, parent, false); return VH(v) }
        override fun onBindViewHolder(holder: VH, pos: Int) { val item = items[pos]; holder.tvLinea1.text = item.linea1; holder.tvLinea2.text = item.linea2; if (onClick != null) holder.itemView.setOnClickListener { onClick(item) } }
        override fun getItemCount() = items.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) { val tvLinea1: TextView = itemView.findViewById(R.id.tvLinea1); val tvLinea2: TextView = itemView.findViewById(R.id.tvLinea2) }
    }
}
