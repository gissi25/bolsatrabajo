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
    data class EstadoItem(val nombre: String) {
        override fun toString() = nombre
    }
    data class ResultadoOferta(
        val nit: String, val idOferta: String, val titulo: String,
        val empresa: String, val departamento: String, val municipio: String,
        val grado: String, val fechaPub: String, val fechaCad: String,
        val expAnios: String, val edadMin: String, val edadMax: String,
        val descripcion: String, val contacto: String
    )
    data class ResultadoPostulante(
        val idPostulante: String, val nombre: String, val apellido: String,
        val email: String, val telefono: String, val grado: String,
        val idPostulacion: String, val idOferta: String, val ofertaTitulo: String,
        val fechaAplicacion: String, val estadoProceso: String
    )

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var scrollView: ScrollView
    private lateinit var cardFiltros: MaterialCardView
    private lateinit var spDepartamento: MaterialAutoCompleteTextView
    private lateinit var tilDepartamento: TextInputLayout
    private lateinit var spMunicipio: MaterialAutoCompleteTextView
    private lateinit var tilMunicipio: TextInputLayout
    private lateinit var btnBuscar: MaterialButton
    private lateinit var cardEstado: MaterialCardView
    private lateinit var spEstado: MaterialAutoCompleteTextView
    private lateinit var tilEstado: TextInputLayout
    private lateinit var cardResultados: MaterialCardView
    private lateinit var tvTituloResultados: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var tvSinResultados: TextView

    private var role = Constants.ROLE_POSTULANTE
    private var idPostulanteSeleccionado: String? = null
    private var nitEmpresaSeleccionado: String? = null

    private val departamentos = mutableListOf<DeptoItem>()
    private val municipios = mutableListOf<MuniItem>()
    private val ofertasResultados = mutableListOf<ResultadoOferta>()
    private val postulantesResultados = mutableListOf<ResultadoPostulante>()

    private val estados = listOf(
        EstadoItem(""), EstadoItem("Pendiente"), EstadoItem("Activo"),
        EstadoItem("En Proceso"), EstadoItem("Contratado"), EstadoItem("Rechazado")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        role = Constants.ROLE_ADMIN
        idPostulanteSeleccionado = prefs.getString(Constants.KEY_ID_POSTULANTE, null)
        nitEmpresaSeleccionado = prefs.getString(Constants.KEY_NIT_EMPRESA, null)

        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progressBar)
        scrollView = view.findViewById(R.id.scrollView)
        cardFiltros = view.findViewById(R.id.cardFiltros)
        spDepartamento = view.findViewById(R.id.spDepartamento)
        tilDepartamento = view.findViewById(R.id.tilDepartamento)
        spMunicipio = view.findViewById(R.id.spMunicipio)
        tilMunicipio = view.findViewById(R.id.tilMunicipio)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        cardEstado = view.findViewById(R.id.cardEstado)
        spEstado = view.findViewById(R.id.spEstado)
        tilEstado = view.findViewById(R.id.tilEstado)
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
        cardEstado.visibility = View.GONE
        btnBuscar.setOnClickListener { buscarOfertasLocal() }
        cargarDepartamentos()
    }

    private fun configurarVistaEmpresa() {
        toolbar.title = getString(R.string.s6_titulo_empresa)
        cardFiltros.visibility = View.GONE
        cardEstado.visibility = View.VISIBLE

        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estados)
        spEstado.setAdapter(estadoAdapter)
        spEstado.setThreshold(0)
        spEstado.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= 0 && pos < estados.size) {
                spEstado.setTag(estados[pos].nombre)
                buscarPostulantesLocal()
            }
        }
        tilEstado.setOnClickListener { spEstado.showDropDown() }

        if (nitEmpresaSeleccionado.isNullOrBlank()) {
            val lista = queryEmpresasParaSeleccion()
            if (lista.isEmpty()) {
                Snackbar.make(requireView(), getString(R.string.s5_sin_perfil), Snackbar.LENGTH_LONG).show()
            } else {
                mostrarDialogoSeleccionEmpresa(lista)
            }
        } else {
            buscarPostulantesLocal()
        }
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
                    }
                }
                tilMunicipio.setOnClickListener { spMunicipio.showDropDown() }
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
                                    
                                    buscarOfertasInicialesPostulante(deptoId, muniId, distId)
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
                val resultados = withContext(Dispatchers.IO) {
                    val lista = mutableListOf<ResultadoOferta>()
                    val db = ConnectionHelper(requireContext()).writableDb
                    val muniId = spMunicipio.tag?.toString()
                    val sql = buildString {
                        append("SELECT o.NIT, o.ID_OFERTA, o.TITULO_PUESTO, o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, ")
                        append("o.EXPERIENCIA_ANIOS, o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO, o.ID_GRADO_ACADEMICO, ")
                        append("e.NOMBRE_EMPRESA, e.CONTACTO_DIRECTO, ")
                        append("IFNULL(d.NOMBRE_DISTRITO,''), IFNULL(m.NOMBRE_MUNICIPIO,''), IFNULL(dep.NOMBRE_DEPARTAMENTO,''), ")
                        append("IFNULL(g.NOMBRE_GRADO,'') ")
                        append("FROM OFERTA_TRABAJO o ")
                        append("INNER JOIN EMPRESA e ON o.NIT = e.NIT ")
                        append("LEFT JOIN DISTRITO d ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO AND e.ID_DISTRITO_ID = d.ID_DISTRITO ")
                        append("LEFT JOIN MUNICIPIO m ON e.ID_DISTRITO_DEPTO = m.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = m.ID_MUNICIPIO ")
                        append("LEFT JOIN DEPARTAMENTO dep ON e.ID_DISTRITO_DEPTO = dep.ID_DEPARTAMENTO ")
                        append("LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO ")
                        append("WHERE e.ID_DISTRITO_DEPTO = ? ")
                        if (!muniId.isNullOrBlank()) append("AND e.ID_DISTRITO_MUNICIPIO = ? ")
                        append("ORDER BY o.FECHA_PUBLICACION DESC")
                    }
                    val args = if (!muniId.isNullOrBlank()) arrayOf(deptoId, muniId) else arrayOf(deptoId)
                    val c = db.rawQuery(sql, args)
                    while (c.moveToNext()) {
                        lista.add(ResultadoOferta(
                            nit = c.getString(0), idOferta = c.getString(1), titulo = c.getString(2),
                            empresa = c.getString(10) ?: "", departamento = c.getString(14) ?: "",
                            municipio = c.getString(13) ?: "", grado = c.getString(15) ?: "",
                            fechaPub = c.getString(3) ?: "", fechaCad = c.getString(4) ?: "",
                            expAnios = if (!c.isNull(5)) "${c.getInt(5)}" else "",
                            edadMin = if (!c.isNull(6)) "${c.getInt(6)}" else "",
                            edadMax = if (!c.isNull(7)) "${c.getInt(7)}" else "",
                            descripcion = c.getString(8) ?: "", contacto = c.getString(11) ?: ""
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
    // BUSCAR POSTULANTES POR EMPRESA (LOCAL SQLITE)
    // =========================================================================

    private fun buscarPostulantesLocal() {
        val nit = nitEmpresaSeleccionado
        if (nit.isNullOrBlank()) {
            Snackbar.make(requireView(), "No se encontró NIT de empresa", Snackbar.LENGTH_LONG).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        cardResultados.visibility = View.GONE
        tvSinResultados.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val estado = spEstado.tag?.toString()?.takeIf { it.isNotBlank() }
                val resultados = withContext(Dispatchers.IO) {
                    val lista = mutableListOf<ResultadoPostulante>()
                    val db = ConnectionHelper(requireContext()).writableDb
                    val sql = buildString {
                        append("SELECT po.ID_POSTULANTE, po.NOMBRE, po.APELLIDO, po.EMAIL, po.TELEFONO_CELULAR, ")
                        append("IFNULL(g.NOMBRE_GRADO,''), p.ID_POSTULACION, p.ID_OFERTA, IFNULL(o.TITULO_PUESTO,''), ")
                        append("p.FECHA_APLICACION, p.ESTADO_PROCESO ")
                        append("FROM POSTULACION p ")
                        append("INNER JOIN POSTULANTE po ON p.ID_POSTULANTE = po.ID_POSTULANTE ")
                        append("LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA ")
                        append("LEFT JOIN GRADO_ACADEMICO g ON po.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO ")
                        append("WHERE p.NIT = ? ")
                        if (estado != null) append("AND p.ESTADO_PROCESO = ? ")
                        append("ORDER BY p.FECHA_APLICACION DESC")
                    }
                    val args = if (estado != null) arrayOf(nit, estado) else arrayOf(nit)
                    val c = db.rawQuery(sql, args)
                    while (c.moveToNext()) {
                        lista.add(ResultadoPostulante(
                            idPostulante = c.getString(0), nombre = c.getString(1) ?: "",
                            apellido = c.getString(2) ?: "", email = c.getString(3) ?: "",
                            telefono = c.getString(4) ?: "", grado = c.getString(5) ?: "",
                            idPostulacion = c.getString(6), idOferta = c.getString(7) ?: "",
                            ofertaTitulo = c.getString(8) ?: "", fechaAplicacion = c.getString(9) ?: "",
                            estadoProceso = c.getString(10) ?: ""
                        ))
                    }
                    c.close()
                    db.close()
                    lista
                }

                postulantesResultados.clear()
                postulantesResultados.addAll(resultados)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (postulantesResultados.isEmpty()) {
                        cardResultados.visibility = View.GONE
                        tvSinResultados.visibility = View.VISIBLE
                    } else {
                        tvSinResultados.visibility = View.GONE
                        cardResultados.visibility = View.VISIBLE
                        tvTituloResultados.text = getString(R.string.s6_n_postulantes, postulantesResultados.size)
                        rvResultados.adapter = PostulanteAdapter(postulantesResultados) { item -> mostrarDetallePostulante(item) }
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
    // DIALOGO SELECCION EMPRESA
    // =========================================================================

    private fun queryEmpresasParaSeleccion(): List<Pair<String, String>> {
        val lista = mutableListOf<Pair<String, String>>()
        try {
            val db = ConnectionHelper(requireContext()).writableDb
            val c = db.rawQuery("SELECT NIT, NOMBRE_EMPRESA FROM EMPRESA ORDER BY NOMBRE_EMPRESA", null)
            while (c.moveToNext()) lista.add(Pair(c.getString(0), c.getString(1)))
            c.close(); db.close()
        } catch (_: Exception) {}
        return lista
    }

    private fun mostrarDialogoSeleccionEmpresa(lista: List<Pair<String, String>>) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 8) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val tvTitulo = TextView(context).apply {
            text = getString(R.string.s5_seleccionar_empresa)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
            setTextColor(0xFF0D1A4A.toInt())
        }
        container.addView(tvTitulo)
        val radioGroup = android.widget.RadioGroup(context)
        val radioButtons = lista.mapIndexed { idx, (nit, nombre) ->
            android.widget.RadioButton(context).apply {
                text = "$nit — $nombre"
                tag = idx
                id = View.generateViewId()
                setPadding(4, 8, 4, 8)
                setTextColor(0xDD000000.toInt())
            }
        }
        radioButtons.forEach { radioGroup.addView(it) }
        if (radioButtons.isNotEmpty()) radioGroup.check(radioButtons.first().id)
        container.addView(radioGroup)
        sv.addView(container)
        AlertDialog.Builder(context)
            .setView(sv)
            .setPositiveButton(getString(R.string.s5_confirmar)) { _, _ ->
                val checked = radioGroup.findViewById<android.widget.RadioButton>(radioGroup.checkedRadioButtonId)
                val idx = checked?.tag as? Int
                if (idx != null && idx < lista.size) {
                    nitEmpresaSeleccionado = lista[idx].first
                    requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit { putString(Constants.KEY_NIT_EMPRESA, nitEmpresaSeleccionado) }
                    buscarPostulantesLocal()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                Snackbar.make(requireView(), getString(R.string.s5_sin_perfil), Snackbar.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            }
            .create().apply { setCancelable(false); show() }
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
            text = getString(R.string.s5_postularse)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF3366FF.toInt())
            cornerRadius = 24.toDp(context)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56.toDp(context))
        }.also { container.addView(it) }

        sv.addView(container)
        val dialog = AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).create()
        btnPostular.setOnClickListener {
            dialog.dismiss()
            postularse(item.nit, item.idOferta)
        }
        dialog.show()
    }

    // =========================================================================
    // DIALOGO DETALLE POSTULANTE
    // =========================================================================

    private fun mostrarDetallePostulante(item: ResultadoPostulante) {
        val context = requireContext()
        val sv = ScrollView(context).apply { setPadding(24, 16, 24, 16) }
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val tvNombre = TextView(context).apply {
            text = "${item.nombre} ${item.apellido}"
            textSize = 18f
            setTextColor(0xFF0D1A4A.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        container.addView(tvNombre)
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(48, 3.toDp(context)).apply { topMargin = 6; bottomMargin = 12 }
            setBackgroundColor(0xFF3366FF.toInt())
        })
        addSectionHeader(context, container, getString(R.string.s6_datos_postulante))
        addDetailRow(context, container, "ID Postulante", item.idPostulante)
        addDetailRow(context, container, getString(R.string.hint_correo_electronico), item.email.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.hint_telefono_celular), item.telefono.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.s5_grado), item.grado.ifBlank { "—" })

        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 4 }
            setBackgroundColor(0x22000000)
        })
        addSectionHeader(context, container, getString(R.string.s6_datos_postulacion))
        addDetailRow(context, container, getString(R.string.s6_oferta_aplicada), item.ofertaTitulo.ifBlank { item.idOferta })
        addDetailRow(context, container, getString(R.string.s5_estado), item.estadoProceso.ifBlank { "—" })
        addDetailRow(context, container, getString(R.string.s5_fecha), item.fechaAplicacion.ifBlank { "—" })
        addDetailRow(context, container, "ID Postulación", item.idPostulacion)

        if (role == Constants.ROLE_EMPRESA) {
            container.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 16; bottomMargin = 12 }
                setBackgroundColor(0x22000000)
            })
            MaterialButton(context).apply {
                text = getString(R.string.s6_cambiar_estado)
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF3366FF.toInt())
                cornerRadius = 24.toDp(context)
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56.toDp(context))
                setOnClickListener { mostrarDialogoCambiarEstado(item) }
            }.also { container.addView(it) }
        }

        sv.addView(container)
        AlertDialog.Builder(context).setView(sv).setPositiveButton(getString(R.string.cerrar), null).show()
    }

    private fun mostrarDialogoCambiarEstado(item: ResultadoPostulante) {
        val estadosList = listOf("Pendiente", "Activo", "En Proceso", "Contratado", "Rechazado")
        val labels = estadosList.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.s6_cambiar_estado_para, "${item.nombre} ${item.apellido}"))
            .setItems(labels) { _, which ->
                val nuevoEstado = estadosList[which]
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val db = ConnectionHelper(requireContext()).writableDb
                            db.execSQL("UPDATE POSTULACION SET ESTADO_PROCESO = ? WHERE ID_POSTULACION = ?", arrayOf(nuevoEstado, item.idPostulacion))
                            db.close()
                        }
                        Snackbar.make(requireView(), getString(R.string.s6_estado_actualizado), Snackbar.LENGTH_SHORT).show()
                        buscarPostulantesLocal()
                    } catch (e: Exception) {
                        Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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

        val lista = queryPostulantesParaSeleccion()
        if (lista.isEmpty()) {
            configurarVistaPostulante()
        } else {
            mostrarDialogoSeleccionPostulanteAlInicio(lista)
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
                        append("IFNULL(g.NOMBRE_GRADO,'') ")
                        append("FROM OFERTA_TRABAJO o ")
                        append("INNER JOIN EMPRESA e ON o.NIT = e.NIT ")
                        append("LEFT JOIN DISTRITO d ON e.ID_DISTRITO_DEPTO = d.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = d.ID_MUNICIPIO AND e.ID_DISTRITO_ID = d.ID_DISTRITO ")
                        append("LEFT JOIN MUNICIPIO m ON e.ID_DISTRITO_DEPTO = m.ID_DEPARTAMENTO AND e.ID_DISTRITO_MUNICIPIO = m.ID_MUNICIPIO ")
                        append("LEFT JOIN DEPARTAMENTO dep ON e.ID_DISTRITO_DEPTO = dep.ID_DEPARTAMENTO ")
                        append("LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO ")
                        append("WHERE e.ID_DISTRITO_DEPTO = ? ")
                        if (!muniId.isNullOrBlank()) append("AND e.ID_DISTRITO_MUNICIPIO = ? ")
                        if (!distId.isNullOrBlank()) append("AND e.ID_DISTRITO_ID = ? ")
                        append("ORDER BY o.FECHA_PUBLICACION DESC")
                    }
                    val argsList = mutableListOf<String>()
                    argsList.add(deptoId)
                    if (!muniId.isNullOrBlank()) argsList.add(muniId)
                    if (!distId.isNullOrBlank()) argsList.add(distId)
                    val c = db.rawQuery(sql, argsList.toTypedArray())
                    while (c.moveToNext()) {
                        lista.add(ResultadoOferta(
                            nit = c.getString(0), idOferta = c.getString(1), titulo = c.getString(2),
                            empresa = c.getString(10) ?: "", departamento = c.getString(14) ?: "",
                            municipio = c.getString(13) ?: "", grado = c.getString(15) ?: "",
                            fechaPub = c.getString(3) ?: "", fechaCad = c.getString(4) ?: "",
                            expAnios = if (!c.isNull(5)) "${c.getInt(5)}" else "",
                            edadMin = if (!c.isNull(6)) "${c.getInt(6)}" else "",
                            edadMax = if (!c.isNull(7)) "${c.getInt(7)}" else "",
                            descripcion = c.getString(8) ?: "", contacto = c.getString(11) ?: ""
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
            h.tv1.text = "${item.titulo} — ${item.empresa}"
            h.tv2.text = "${item.municipio}, ${item.departamento} · ${item.fechaPub}"
            h.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = items.size
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tv1: TextView = view.findViewById(android.R.id.text1)
            val tv2: TextView = view.findViewById(android.R.id.text2)
        }
    }

    inner class PostulanteAdapter(
        private val items: List<ResultadoPostulante>,
        private val onClick: (ResultadoPostulante) -> Unit
    ) : RecyclerView.Adapter<PostulanteAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.tv1.text = "${item.nombre} ${item.apellido} · ${item.estadoProceso}"
            h.tv2.text = "${item.ofertaTitulo.ifBlank { item.idOferta }} · ${item.fechaAplicacion}"
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
}
