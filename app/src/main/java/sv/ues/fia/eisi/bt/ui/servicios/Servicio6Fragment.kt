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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.setupMarqueeTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        role = Constants.ROLE_ADMIN
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
        toolbar.menu.add("Cambiar Perfil").setOnMenuItemClickListener {
            val lista = queryPostulantesParaSeleccion()
            if (lista.isNotEmpty()) {
                mostrarDialogoSeleccionPostulanteCambiar(lista)
            } else {
                Snackbar.make(requireView(), "No hay perfiles de postulantes disponibles", Snackbar.LENGTH_LONG).show()
            }
            true
        }

        verificarSeleccion()
    }

    private fun configurarVistaPostulante() {
        toolbar.title = getString(R.string.s6_titulo_postulante)
        cardFiltros.visibility = View.VISIBLE
        btnBuscar.setOnClickListener { buscarOfertasLocal() }
        cargarDepartamentos()
    }

    // =========================================================================
    // CARGAR DEPARTAMENTOS DESDE SQLITE
    // =========================================================================

    private fun cargarDepartamentos() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                withContext(Dispatchers.IO) {
                    departamentos.clear()
                    val db = ConnectionHelper(requireContext()).writableDb
                    val c = db.rawQuery("SELECT ID_DEPARTAMENTO, NOMBRE_DEPARTAMENTO FROM DEPARTAMENTO ORDER BY NOMBRE_DEPARTAMENTO", null)
                    while (c.moveToNext()) {
                        departamentos.add(DeptoItem(c.getString(0), c.getString(1)))
                    }
                    c.close()
                    db.close()
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, departamentos)
                spDepartamento.setAdapter(adapter)
                spDepartamento.setThreshold(0)
                spDepartamento.setOnItemClickListener { _, _, pos, _ ->
                    if (pos >= 0 && pos < departamentos.size) {
                        spDepartamento.setTag(departamentos[pos].id)
                        cargarMunicipios(departamentos[pos].id)
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

    private fun cargarMunicipios(idDepartamento: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    municipios.clear()
                    val db = ConnectionHelper(requireContext()).writableDb
                    municipios.add(MuniItem("", "Todos"))
                    val c = db.rawQuery("SELECT ID_MUNICIPIO, NOMBRE_MUNICIPIO FROM MUNICIPIO WHERE ID_DEPARTAMENTO = ? ORDER BY NOMBRE_MUNICIPIO", arrayOf(idDepartamento))
                    while (c.moveToNext()) {
                        municipios.add(MuniItem(c.getString(0), c.getString(1)))
                    }
                    c.close()
                    db.close()
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
                            val adapterDist = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, distritos)
                            spDistrito.setAdapter(adapterDist)
                            spDistrito.setText("", false)
                            spDistrito.setTag("")
                        }
                    }
                }
                tilMunicipio.setOnClickListener { spMunicipio.showDropDown() }
            } catch (_: Exception) {}
        }
    }

    private fun cargarDistritos(idDepartamento: String, idMunicipio: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    distritos.clear()
                    val db = ConnectionHelper(requireContext()).writableDb
                    distritos.add(DistItem("", "Todos"))
                    val c = db.rawQuery("SELECT ID_DISTRITO, NOMBRE_DISTRITO FROM DISTRITO WHERE ID_DEPARTAMENTO = ? AND ID_MUNICIPIO = ? ORDER BY NOMBRE_DISTRITO", arrayOf(idDepartamento, idMunicipio))
                    while (c.moveToNext()) {
                        distritos.add(DistItem(c.getString(0), c.getString(1)))
                    }
                    c.close()
                    db.close()
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, distritos)
                spDistrito.setAdapter(adapter)
                spDistrito.setThreshold(0)
                spDistrito.setOnItemClickListener { _, _, pos, _ ->
                    if (pos >= 0 && pos < distritos.size) {
                        spDistrito.setTag(distritos[pos].id)
                    }
                }
                tilDistrito.setOnClickListener { spDistrito.showDropDown() }
            } catch (_: Exception) {}
        }
    }

    private fun preSeleccionarUbicacionPostulante() {
        if (idPostulanteSeleccionado.isNullOrBlank()) return
        lifecycleScope.launch {
            try {
                val datos = withContext(Dispatchers.IO) {
                    val db = ConnectionHelper(requireContext()).writableDb
                    val c = db.rawQuery("SELECT ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID FROM POSTULANTE WHERE ID_POSTULANTE = ?", arrayOf(idPostulanteSeleccionado!!))
                    var depto: String? = null
                    var muni: String? = null
                    var dist: String? = null
                    if (c.moveToFirst()) {
                        depto = c.getString(0)
                        muni = c.getString(1)
                        dist = c.getString(2)
                    }
                    c.close()
                    db.close()
                    Triple(depto, muni, dist)
                }
                val (deptoId, muniId, distId) = datos
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
                                            if (idxDist >= 0) {
                                                spDistrito.setText(distritos[idxDist].nombre, false)
                                                spDistrito.setTag(distId)
                                            }
                                            buscarOfertasInicialesPostulante(deptoId, muniId, distId)
                                        }, 400)
                                    } else {
                                        spDistrito.postDelayed({
                                            buscarOfertasInicialesPostulante(deptoId, muniId, null)
                                        }, 400)
                                    }
                                }
                            }, 400)
                        } else {
                            buscarOfertasInicialesPostulante(deptoId, null, null)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // =========================================================================
    // BUSCAR OFERTAS POR UBICACION (LOCAL SQLITE)
    // =========================================================================

    private fun buscarOfertasLocal() {
        val deptoId = spDepartamento.tag?.toString()
        if (deptoId.isNullOrEmpty()) {
            Snackbar.make(requireView(), getString(R.string.s6_error_depto), Snackbar.LENGTH_LONG).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        cardResultados.visibility = View.GONE
        tvSinResultados.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val muniId = spMunicipio.tag?.toString()
                val distId = spDistrito.tag?.toString()
                try {
                    val remoteData = ApiService.filtrarOfertasPorUbicacion(deptoId, muniId)
                    withContext(Dispatchers.IO) {
                        syncOfertas(remoteData)
                    }
                } catch (e: Exception) {
                    Log.e("Servicio6", "Error al sincronizar ofertas", e)
                }

                val resultados = withContext(Dispatchers.IO) {
                    val lista = mutableListOf<ResultadoOferta>()
                    val db = ConnectionHelper(requireContext()).writableDb
                    val muniId = spMunicipio.tag?.toString()
                    val distId = spDistrito.tag?.toString()
                    val sql = buildString {
                        append("SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO, o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, ")
                        append("o.EXPERIENCIA_ANIOS, o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO, o.ID_GRADO_ACADEMICO, ")
                        append("e.NOMBRE_EMPRESA, e.CONTACTO_DIRECTO, ")
                        append("IFNULL(d.NOMBRE_DISTRITO,''), IFNULL(m.NOMBRE_MUNICIPIO,''), IFNULL(dep.NOMBRE_DEPARTAMENTO,''), ")
                        append("IFNULL(g.NOMBRE_GRADO,''), ")
                        append("p.ID_POSTULACION, p.ESTADO_PROCESO ")
                        append("FROM OFERTA_TRABAJO o ")
                        append("INNER JOIN EMPRESA e ON o.NIT = e.NIT ")
                        append("LEFT JOIN DISTRITO d ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO AND e.ID_DISTRITO_ID = d.ID_DISTRITO ")
                        append("LEFT JOIN MUNICIPIO m ON e.ID_DISTRITO_DEPTO = m.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = m.ID_MUNICIPIO ")
                        append("LEFT JOIN DEPARTAMENTO dep ON e.ID_DISTRITO_DEPTO = dep.ID_DEPARTAMENTO ")
                        append("LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO ")
                        append("LEFT JOIN POSTULACION p ON o.NIT = p.NIT AND o.ID_OFERTA = p.ID_OFERTA AND p.ID_POSTULANTE = ? ")
                        append("WHERE e.ID_DISTRITO_DEPTO = ? ")
                        if (!muniId.isNullOrBlank()) append("AND e.ID_DISTRITO_MUNICIPIO = ? ")
                        if (!distId.isNullOrBlank()) append("AND e.ID_DISTRITO_ID = ? ")
                        append("ORDER BY o.FECHA_PUBLICACION DESC")
                    }
                    val argsList = mutableListOf<String>()
                    argsList.add(idPostulanteSeleccionado ?: "")
                    argsList.add(deptoId)
                    if (!muniId.isNullOrBlank()) argsList.add(muniId)
                    if (!distId.isNullOrBlank()) argsList.add(distId)
                    val c = db.rawQuery(sql, argsList.toTypedArray())
                    while (c.moveToNext()) {
                        val idPostulacion = c.getString(16)
                        val estadoProceso = c.getString(17)
                        lista.add(ResultadoOferta(
                            nit = c.getString(0), idOferta = c.getString(1), titulo = c.getString(2),
                            empresa = c.getString(10) ?: "", departamento = c.getString(14) ?: "",
                            municipio = c.getString(13) ?: "", grado = c.getString(15) ?: "",
                            fechaPub = c.getString(3) ?: "", fechaCad = c.getString(4) ?: "",
                            expAnios = if (!c.isNull(5)) "${c.getInt(5)}" else "",
                            edadMin = if (!c.isNull(6)) "${c.getInt(6)}" else "",
                            edadMax = if (!c.isNull(7)) "${c.getInt(7)}" else "",
                            descripcion = c.getString(8) ?: "", contacto = c.getString(11) ?: "",
                            yaPostulado = idPostulacion != null,
                            estadoPostulacion = estadoProceso
                        ))
                    }
                    c.close()
                    db.close()
                    lista
                }

                ofertasResultados.clear()
                ofertasResultados.addAll(resultados)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (ofertasResultados.isEmpty()) {
                        cardResultados.visibility = View.GONE
                        tvSinResultados.visibility = View.VISIBLE
                    } else {
                        tvSinResultados.visibility = View.GONE
                        cardResultados.visibility = View.VISIBLE
                        tvTituloResultados.text = getString(R.string.s6_n_ofertas, ofertasResultados.size)
                        rvResultados.adapter = OfertaAdapter(ofertasResultados) { item -> mostrarDetalleOferta(item) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
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

        val tvTitulo = TextView(context).apply {
            text = item.titulo
            textSize = 18f
            setTextColor(0xFF0D1A4A.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        container.addView(tvTitulo)
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }
            setBackgroundColor(0xFF3366FF.toInt())
        })
        addSectionHeader(context, container, getString(R.string.s5_datos_oferta))
        addDetailRow(context, container, getString(R.string.s5_empresa), "${item.empresa} · ${item.nit}")
        addDetailRow(context, container, getString(R.string.s6_ubicacion), "${item.departamento}, ${item.municipio}")
        addDetailRow(context, container, getString(R.string.s5_grado), item.grado.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.s5_publicacion), item.fechaPub.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.s5_caducidad), item.fechaCad.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.s5_experiencia), if (item.expAnios.isNotBlank()) "${item.expAnios} years" else "—")
        val edadStr = listOfNotNull(item.edadMin.takeIf { it.isNotBlank() }, item.edadMax.takeIf { it.isNotBlank() }).joinToString(" - ")
        addDetailRow(context, container, getString(R.string.s5_edad), edadStr.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.s5_descripcion), item.descripcion.ifBlank { "—" })

        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 12 }
            setBackgroundColor(0x22000000)
        })
        val btnPostular = MaterialButton(context).apply {
            if (item.yaPostulado) {
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
        if (!item.yaPostulado) {
            btnPostular.setOnClickListener {
                dialog.dismiss()
                postularse(item.nit, item.idOferta)
            }
        }
        dialog.show()
    }

    // =========================================================================
    // POSTULARSE A UNA OFERTA
    // =========================================================================

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
                val yaPostulado = withContext(Dispatchers.IO) {
                    val db = ConnectionHelper(requireContext()).writableDb
                    val c = db.rawQuery("SELECT 1 FROM POSTULACION WHERE ID_POSTULANTE = ? AND NIT = ? AND ID_OFERTA = ?", arrayOf(idPostulante, nit, idOferta))
                    val r = c.moveToFirst(); c.close(); db.close(); r
                }
                if (yaPostulado) {
                    Snackbar.make(requireView(), getString(R.string.s5_error_ya_postulado), Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                val idPostulacion = withContext(Dispatchers.IO) {
                    val db = ConnectionHelper(requireContext()).writableDb
                    val c = db.rawQuery("SELECT MAX(CAST(SUBSTR(ID_POSTULACION, 4) AS INTEGER)) FROM POSTULACION", null)
                    val next = if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) + 1 else 1
                    c.close(); db.close()
                    "POS${next.toString().padStart(3, '0')}"
                }
                val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                withContext(Dispatchers.IO) {
                    val db = ConnectionHelper(requireContext()).writableDb
                    db.execSQL("INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO) VALUES (?,?,?,?,?,'Pendiente')", arrayOf(idPostulacion, nit, idOferta, idPostulante, fecha))
                    db.close()
                }
                try {
                    val json = JSONObject().apply {
                        put("id_postulacion", idPostulacion)
                        put("nit", nit)
                        put("id_oferta", idOferta)
                        put("id_postulante", idPostulante)
                        put("fecha_aplicacion", fecha)
                        put("estado_proceso", "Pendiente")
                    }
                    ApiService.insertarPostulacion(json)
                } catch (e: Exception) {
                    Log.e("Servicio6", "Error al sincronizar postulacion", e)
                    Snackbar.make(requireView(), "Guardado local. No se pudo sincronizar: ${e.message}", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                Snackbar.make(requireView(), getString(R.string.s5_postulacion_exitosa), Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun verificarSeleccion() {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        idPostulanteSeleccionado = prefs.getString(Constants.KEY_ID_POSTULANTE, null)

        if (idPostulanteSeleccionado == null) {
            val lista = queryPostulantesParaSeleccion()
            if (lista.isEmpty()) {
                configurarVistaPostulante()
            } else {
                mostrarDialogoSeleccionPostulanteAlInicio(lista)
            }
        } else {
            configurarVistaPostulante()
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

    private fun mostrarDialogoSeleccionPostulanteAlInicio(lista: List<Pair<String, String>>) {
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
                    configurarVistaPostulante()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                configurarVistaPostulante()
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
                    configurarVistaPostulante()
                    postularse(nit, idOferta)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply { show() }
    }

    private fun buscarOfertasInicialesPostulante(deptoId: String, muniId: String?, distId: String?) {
        progressBar.visibility = View.VISIBLE
        cardResultados.visibility = View.GONE
        tvSinResultados.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val resultados = withContext(Dispatchers.IO) {
                    val lista = mutableListOf<ResultadoOferta>()
                    val db = ConnectionHelper(requireContext()).writableDb
                    val sql = buildString {
                        append("SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO, o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, ")
                        append("o.EXPERIENCIA_ANIOS, o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO, o.ID_GRADO_ACADEMICO, ")
                        append("e.NOMBRE_EMPRESA, e.CONTACTO_DIRECTO, ")
                        append("IFNULL(d.NOMBRE_DISTRITO,''), IFNULL(m.NOMBRE_MUNICIPIO,''), IFNULL(dep.NOMBRE_DEPARTAMENTO,''), ")
                        append("IFNULL(g.NOMBRE_GRADO,''), ")
                        append("p.ID_POSTULACION, p.ESTADO_PROCESO ")
                        append("FROM OFERTA_TRABAJO o ")
                        append("INNER JOIN EMPRESA e ON o.NIT = e.NIT ")
                        append("LEFT JOIN DISTRITO d ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO AND e.ID_DISTRITO_ID = d.ID_DISTRITO ")
                        append("LEFT JOIN MUNICIPIO m ON e.ID_DISTRITO_DEPTO = m.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = m.ID_MUNICIPIO ")
                        append("LEFT JOIN DEPARTAMENTO dep ON e.ID_DISTRITO_DEPTO = dep.ID_DEPARTAMENTO ")
                        append("LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO ")
                        append("LEFT JOIN POSTULACION p ON o.NIT = p.NIT AND o.ID_OFERTA = p.ID_OFERTA AND p.ID_POSTULANTE = ? ")
                        append("WHERE e.ID_DISTRITO_DEPTO = ? ")
                        if (!muniId.isNullOrBlank()) append("AND e.ID_DISTRITO_MUNICIPIO = ? ")
                        if (!distId.isNullOrBlank()) append("AND e.ID_DISTRITO_ID = ? ")
                        append("ORDER BY o.FECHA_PUBLICACION DESC")
                    }
                    val argsList = mutableListOf<String>()
                    argsList.add(idPostulanteSeleccionado ?: "")
                    argsList.add(deptoId)
                    if (!muniId.isNullOrBlank()) argsList.add(muniId)
                    if (!distId.isNullOrBlank()) argsList.add(distId)
                    val c = db.rawQuery(sql, argsList.toTypedArray())
                    while (c.moveToNext()) {
                        val idPostulacion = c.getString(16)
                        val estadoProceso = c.getString(17)
                        lista.add(ResultadoOferta(
                            nit = c.getString(0), idOferta = c.getString(1), titulo = c.getString(2),
                            empresa = c.getString(10) ?: "", departamento = c.getString(14) ?: "",
                            municipio = c.getString(13) ?: "", grado = c.getString(15) ?: "",
                            fechaPub = c.getString(3) ?: "", fechaCad = c.getString(4) ?: "",
                            expAnios = if (!c.isNull(5)) "${c.getInt(5)}" else "",
                            edadMin = if (!c.isNull(6)) "${c.getInt(6)}" else "",
                            edadMax = if (!c.isNull(7)) "${c.getInt(7)}" else "",
                            descripcion = c.getString(8) ?: "", contacto = c.getString(11) ?: "",
                            yaPostulado = idPostulacion != null,
                            estadoPostulacion = estadoProceso
                        ))
                    }
                    c.close()
                    db.close()
                    lista
                }

                ofertasResultados.clear()
                ofertasResultados.addAll(resultados)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (ofertasResultados.isEmpty()) {
                        cardResultados.visibility = View.GONE
                        tvSinResultados.visibility = View.VISIBLE
                    } else {
                        tvSinResultados.visibility = View.GONE
                        cardResultados.visibility = View.VISIBLE
                        tvTituloResultados.text = getString(R.string.s6_n_ofertas, ofertasResultados.size)
                        rvResultados.adapter = OfertaAdapter(ofertasResultados) { item -> mostrarDetalleOferta(item) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    // =========================================================================
    // ADAPTERS
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
                h.tv1.text = "${item.titulo} — ${item.empresa} (Postulado)"
                val estado = item.estadoPostulacion ?: "Pendiente"
                h.tv2.text = "[Mis Postulaciones] · Estado: $estado · ${item.municipio}, ${item.departamento}"
                h.tv1.setTextColor(0xFF2E7D32.toInt())
            } else {
                h.tv1.text = "${item.titulo} — ${item.empresa}"
                h.tv2.text = "${item.municipio}, ${item.departamento} · ${item.fechaPub}"
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
        val tv = TextView(context).apply {
            this.text = text; textSize = 14f; setTextColor(0xFF3366FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 8, 0, 4)
        }
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

    private fun escapar(s: String) = s.replace("'", "''")
    private fun escaparNullable(s: String) = if (s.isBlank()) "NULL" else "'${s.replace("'", "''")}'"

    private fun syncOfertas(json: JSONObject) {
        val db = ConnectionHelper(requireContext()).writableDb
        try {
            val arr = json.getJSONArray("data")
            db.beginTransaction()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val nit = escapar(o.optString("NIT"))
                val idOferta = escapar(o.optString("ID_OFERTA"))
                val idGrado = o.optString("ID_GRADO_ACADEMICO", "").takeIf { it.isNotEmpty() } ?: "NULL"
                val titulo = escapar(o.optString("TITULO_PUESTO"))
                val fechaPub = escaparNullable(o.optString("FECHA_PUBLICACION"))
                val fechaCad = escaparNullable(o.optString("FECHA_CADUCIDAD"))
                val expAnios = o.optString("EXPERIENCIA_ANIOS", "").takeIf { it.isNotEmpty() } ?: "NULL"
                val edadMin = o.optString("EDAD_MINIMA", "").takeIf { it.isNotEmpty() } ?: "NULL"
                val edadMax = o.optString("EDAD_MAXIMA", "").takeIf { it.isNotEmpty() } ?: "NULL"
                val descripcion = escapar(o.optString("DESCRIPCION_OFERTA_TRABAJO"))
                
                val nombreEmpresa = escapar(o.optString("NOMBRE_EMPRESA"))
                db.execSQL("INSERT OR IGNORE INTO EMPRESA (NIT, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID, NOMBRE_EMPRESA) VALUES ('$nit', 1, 1, 1, '$nombreEmpresa')")
                db.execSQL("UPDATE EMPRESA SET NOMBRE_EMPRESA = '$nombreEmpresa' WHERE NIT = '$nit'")

                if (idGrado != "NULL") {
                    db.execSQL("INSERT OR IGNORE INTO GRADO_ACADEMICO (ID_GRADO_ACADEMICO, NOMBRE_GRADO) VALUES ($idGrado, 'Grado $idGrado')")
                }

                db.execSQL("INSERT OR REPLACE INTO OFERTA_TRABAJO (NIT, ID_OFERTA, ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, FECHA_CADUCIDAD, EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, DESCRIPCION_OFERTA_TRABAJO) VALUES ('$nit', '$idOferta', $idGrado, '$titulo', $fechaPub, $fechaCad, $expAnios, $edadMin, $edadMax, '$descripcion')")
                
                db.execSQL("DELETE FROM DETALLE_REQUISITO WHERE NIT = '$nit' AND ID_OFERTA = '$idOferta'")
                val requisitos = o.optJSONArray("requisitos")
                if (requisitos != null) {
                    for (j in 0 until requisitos.length()) {
                        val r = requisitos.getJSONObject(j)
                        db.execSQL("INSERT INTO DETALLE_REQUISITO (NIT, ID_OFERTA, ID_DETALLE, DESCRIPCION_REQUISITO) VALUES ('$nit', '$idOferta', '${escapar(r.optString("ID_DETALLE"))}', '${escapar(r.optString("DESCRIPCION_REQUISITO"))}')")
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }
}
