package sv.ues.fia.eisi.bt.ui.servicios

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.service.ApiService
import sv.ues.fia.eisi.bt.utils.setupMarqueeTitle
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class Servicio1Fragment : Fragment() {

    private lateinit var spEmpresa: MaterialAutoCompleteTextView
    private lateinit var tilEmpresa: TextInputLayout
    private lateinit var etIdOferta: TextInputEditText
    private lateinit var etTitulo: TextInputEditText
    private lateinit var spGrado: MaterialAutoCompleteTextView
    private lateinit var tilGrado: TextInputLayout
    private lateinit var etFechaPub: TextInputEditText
    private lateinit var etFechaCad: TextInputEditText
    private lateinit var etExpAnios: TextInputEditText
    private lateinit var etEdadMin: TextInputEditText
    private lateinit var etEdadMax: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etReqId: TextInputEditText
    private lateinit var etReqDesc: TextInputEditText
    private lateinit var btnAgregarOtra: MaterialButton
    private lateinit var btnDescargarFormato: MaterialButton
    private lateinit var btnCargarCSV: MaterialButton
    private lateinit var btnInsertarTodas: MaterialButton
    private lateinit var btnSubirLocales: MaterialButton
    private lateinit var rvPendientes: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var repository: MainRepository

    private val pendingOffers = mutableListOf<JSONObject>()
    private lateinit var pendingAdapter: PendingAdapter
    private var reqCounter = 0

    private data class EmpresaItem(val nit: String, val nombre: String) {
        override fun toString() = nombre
    }
    private data class GradoItem(val id: Int, val nombre: String) {
        override fun toString() = nombre
    }
    private var empresas = listOf<EmpresaItem>()
    private var grados = listOf<GradoItem>()

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { cargarArchivo(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spEmpresa = view.findViewById(R.id.spEmpresa); tilEmpresa = view.findViewById(R.id.tilEmpresa)
        etIdOferta = view.findViewById(R.id.etIdOferta); etTitulo = view.findViewById(R.id.etTitulo)
        spGrado = view.findViewById(R.id.spGrado); tilGrado = view.findViewById(R.id.tilGrado)
        etFechaPub = view.findViewById(R.id.etFechaPub); etFechaCad = view.findViewById(R.id.etFechaCad)
        etExpAnios = view.findViewById(R.id.etExpAnios)
        etEdadMin = view.findViewById(R.id.etEdadMin); etEdadMax = view.findViewById(R.id.etEdadMax)
        etDescripcion = view.findViewById(R.id.etDescripcion)
        etReqId = view.findViewById(R.id.etReqId); etReqDesc = view.findViewById(R.id.etReqDesc)
        btnAgregarOtra = view.findViewById(R.id.btnAgregarOtra)
        btnDescargarFormato = view.findViewById(R.id.btnDescargarFormato)
        btnCargarCSV = view.findViewById(R.id.btnCargarCSV)
        btnInsertarTodas = view.findViewById(R.id.btnInsertarTodas)
        btnSubirLocales = view.findViewById(R.id.btnSubirLocales)
        rvPendientes = view.findViewById(R.id.rvPendientes)
        progressBar = view.findViewById(R.id.progressBar)
        repository = MainRepository(requireContext())

        pendingAdapter = PendingAdapter(pendingOffers) { pos ->
            pendingOffers.removeAt(pos); pendingAdapter.notifyDataSetChanged(); updateInsertButton()
        }
        rvPendientes.layoutManager = LinearLayoutManager(requireContext())
        rvPendientes.adapter = pendingAdapter

        etFechaPub.setOnClickListener { showDatePicker(etFechaPub) }
        etFechaCad.setOnClickListener { showDatePicker(etFechaCad) }

        spEmpresa.setOnItemClickListener { _, _, pos, _ -> if (pos >= 0 && pos < empresas.size) spEmpresa.setTag(empresas[pos].nit) }
        spGrado.setOnItemClickListener { _, _, pos, _ -> if (pos >= 0 && pos < grados.size) spGrado.setTag(grados[pos].id.toString()) }

        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setupMarqueeTitle()
        }
        view.findViewById<TextInputLayout>(R.id.tilFechaPub).setEndIconOnClickListener { showDatePicker(etFechaPub) }
        view.findViewById<TextInputLayout>(R.id.tilFechaCad).setEndIconOnClickListener { showDatePicker(etFechaCad) }

        btnAgregarOtra.setOnClickListener { agregarOferta() }
        btnDescargarFormato.setOnClickListener {
            val url = "https://bolsadetrabajopdm.gt.tc/go.php?action=descargar_formato"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        btnCargarCSV.setOnClickListener { filePicker.launch("*/*") }
        btnInsertarTodas.setOnClickListener { mostrarPreview() }
        btnSubirLocales.setOnClickListener { subirDatosLocales() }

        lifecycleScope.launch {
            for (attempt in 1..3) {
                progressBar.visibility = View.VISIBLE
                if (cargarDatos()) break
                progressBar.visibility = View.GONE
                if (attempt < 3) delay(3000)
            }
            progressBar.visibility = View.GONE
        }
    }

    private suspend fun cargarDatos(): Boolean {
        return try {
            val empJson = ApiService.getEmpresas()
            empresas = empJson.map { EmpresaItem(it.getString("NIT"), it.getString("NOMBRE_EMPRESA")) }
            spEmpresa.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, empresas))
            spEmpresa.setThreshold(0); tilEmpresa.setOnClickListener { spEmpresa.showDropDown() }

            val gradJson = ApiService.getGrados()
            grados = gradJson.map { GradoItem(it.getInt("ID_GRADO_ACADEMICO"), it.getString("NOMBRE_GRADO")) }
            spGrado.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, grados))
            spGrado.setThreshold(0); tilGrado.setOnClickListener { spGrado.showDropDown() }
            true
        } catch (e: Exception) {
            Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            false
        }
    }

    private fun showDatePicker(et: TextInputEditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d -> et.setText(String.format("%04d-%02d-%02d", y, m + 1, d)) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validarOferta(): String? {
        val emp = spEmpresa.tag?.toString()
        val id = etIdOferta.text?.toString()?.trim()
        val tit = etTitulo.text?.toString()?.trim()
        val edadMin = etEdadMin.text?.toString()?.toIntOrNull()
        val edadMax = etEdadMax.text?.toString()?.toIntOrNull()
        val fechaPub = etFechaPub.text?.toString()?.trim()
        val fechaCad = etFechaCad.text?.toString()?.trim()
        val expAnios = etExpAnios.text?.toString()?.toIntOrNull()

        if (emp.isNullOrEmpty()) return "Debe seleccionar una empresa"
        if (id.isNullOrEmpty()) return "El código de oferta es obligatorio"
        if (tit.isNullOrEmpty()) return "El título del puesto es obligatorio"
        if (edadMin != null && edadMin < 18) return "Edad mínima debe ser ≥ 18"
        if (edadMin != null && edadMax != null && edadMin > edadMax) return "Edad mínima no puede ser mayor a la máxima"
        if (expAnios != null && expAnios < 0) return "Experiencia no puede ser negativa"
        if (fechaPub != null && fechaCad != null && fechaCad <= fechaPub) return "Fecha de caducidad debe ser posterior a la de publicación"
        return null
    }

    private fun getOfertaActual(): JSONObject? {
        val error = validarOferta()
        if (error != null) {
            Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
            return null
        }

        val reqId = etReqId.text?.toString()?.trim()
        val reqDesc = etReqDesc.text?.toString()?.trim()
        val requisitos = JSONArray()
        if (!reqId.isNullOrEmpty() && !reqDesc.isNullOrEmpty()) {
            requisitos.put(JSONObject().apply { put("id_detalle", reqId); put("descripcion", reqDesc) })
        }

        return JSONObject().apply {
            put("nit", spEmpresa.tag!!)
            put("id_oferta", etIdOferta.text.toString().trim())
            put("titulo", etTitulo.text.toString().trim())
            spGrado.tag?.toString()?.toIntOrNull()?.let { put("id_grado", it) }
            etFechaPub.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { put("fecha_publicacion", it) }
            etFechaCad.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { put("fecha_caducidad", it) }
            etExpAnios.text?.toString()?.toIntOrNull()?.let { put("experiencia_anios", it) }
            etEdadMin.text?.toString()?.toIntOrNull()?.let { put("edad_minima", it) }
            etEdadMax.text?.toString()?.toIntOrNull()?.let { put("edad_maxima", it) }
            etDescripcion.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { put("descripcion", it) }
            put("requisitos", requisitos)
        }
    }

    private fun agregarOferta() {
        val oferta = getOfertaActual() ?: return
        pendingOffers.add(oferta)
        pendingAdapter.notifyItemInserted(pendingOffers.size - 1)
        updateInsertButton()
        reqCounter++
        etIdOferta.text?.clear(); etTitulo.text?.clear()
        etReqId.setText("D${reqCounter + 1}"); etReqDesc.text?.clear()
        spGrado.setText(""); spGrado.tag = null
        etFechaPub.text?.clear(); etFechaCad.text?.clear()
        etExpAnios.text?.clear(); etEdadMin.text?.clear(); etEdadMax.text?.clear()
        etDescripcion.text?.clear()
        etIdOferta.requestFocus()
    }

    private fun updateInsertButton() {
        btnInsertarTodas.text = if (pendingOffers.isEmpty()) getString(R.string.oferta_btn_insertar)
        else getString(R.string.oferta_btn_insertar_n, pendingOffers.size)
        btnInsertarTodas.isEnabled = pendingOffers.isNotEmpty()
    }

    private fun mostrarPreview() {
        val sb = StringBuilder()
        for (of in pendingOffers) {
            sb.append("• ${of.optString("id_oferta")} - ${of.optString("titulo")}")
            val reqs = of.optJSONArray("requisitos")
            val n = reqs?.length() ?: 0
            if (n > 0) sb.append(" ($n requisito${if (n != 1) "s" else ""})")
            sb.append("\n")
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar inserción")
            .setMessage(sb.toString().trimEnd())
            .setPositiveButton("Insertar") { _, _ -> insertarTodas() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun insertarTodas() {
        btnInsertarTodas.isEnabled = false; btnInsertarTodas.text = getString(R.string.oferta_insertando)
        lifecycleScope.launch {
            try {
                val result = ApiService.insertarOfertas(pendingOffers.toList())
                if (result.optBoolean("exito", false)) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Éxito")
                        .setMessage(result.optString("mensaje", "Insertado correctamente"))
                        .setPositiveButton("OK") { _, _ ->
                            pendingOffers.clear(); pendingAdapter.notifyDataSetChanged(); updateInsertButton()
                            parentFragmentManager.popBackStack()
                        }.show()
                } else throw Exception(result.optString("error", "Error"))
            } catch (e: Exception) {
                btnInsertarTodas.isEnabled = true; updateInsertButton()
                Snackbar.make(requireView(), e.message ?: "Error", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun subirDatosLocales() {
        btnSubirLocales.isEnabled = false
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val ofertas = repository.getOfertasLocales()
                if (ofertas.isEmpty()) {
                    Snackbar.make(requireView(), "No hay ofertas locales para subir", Snackbar.LENGTH_LONG).show()
                    btnSubirLocales.isEnabled = true; progressBar.visibility = View.GONE; return@launch
                }

                val result = ApiService.insertarOfertas(ofertas)
                if (result.optBoolean("exito", false)) {
                    val msg = result.optString("mensaje", "Subidas correctamente")
                    val insertadas = result.optInt("insertadas", 0)
                    val fallidas = result.optInt("fallidas", 0)

                    val detalle = result.optJSONArray("detalle")
                    val sb = StringBuilder(msg)
                    if (detalle != null) {
                        for (i in 0 until detalle.length()) {
                            val d = detalle.getJSONObject(i)
                            if (d.optString("estado") == "fallida") {
                                val errArr = d.optJSONArray("errores")
                                val errStr = if (errArr != null) {
                                    (0 until errArr.length()).joinToString(", ") { errArr.optString(it) }
                                } else "error"
                                sb.append("\n• ${d.optString("oferta_id")}: $errStr")
                            }
                        }
                    }

                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Subidas: $insertadas ok, $fallidas fallos")
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    throw Exception(result.optString("error", "Error del servidor"))
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            }
            btnSubirLocales.isEnabled = true
            progressBar.visibility = View.GONE
        }
    }

    private fun cargarArchivo(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                    ?: run { Snackbar.make(requireView(), "No se pudo abrir el archivo", Snackbar.LENGTH_LONG).show(); return@launch }

                val bytes = inputStream.readBytes()
                inputStream.close()

                val lines = if (bytes.size > 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                    parseXlsx(bytes)
                } else {
                    parseCsv(bytes)
                }

                if (lines.size < 2) {
                    Snackbar.make(requireView(), "El archivo no tiene datos (solo cabecera)", Snackbar.LENGTH_LONG).show(); return@launch
                }

                procesarLineas(lines)
            } catch (e: Exception) {
                Log.e("ARCHIVO", "Error: ${e.message}", e)
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun parseCsv(bytes: ByteArray): List<List<String>> {
        val text = bytes.toString(Charsets.UTF_8).trimStart().removePrefix("\uFEFF")
        return text.split("\n").map { it.trim() }.filter { it.isNotBlank() }.map { line ->
            line.split(",").map { it.trim() }
        }
    }

    private fun parseXlsx(bytes: ByteArray): List<List<String>> {
        val zip = ZipInputStream(bytes.inputStream())
        var sharedStringsXml: String? = null
        var sheetXml: String? = null
        var entry = zip.nextEntry

        while (entry != null) {
            val data = zip.readBytes()
            val name = entry.name
            if (name == "xl/sharedStrings.xml") sharedStringsXml = data.toString(Charsets.UTF_8)
            else if (name == "xl/worksheets/sheet1.xml") sheetXml = data.toString(Charsets.UTF_8)
            zip.closeEntry()
            entry = zip.nextEntry
        }
        zip.close()

        if (sheetXml == null) throw Exception("No se encontró hoja de cálculo en el Excel")

        val docFactory = DocumentBuilderFactory.newInstance()
        val sharedStrings = mutableListOf<String>()

        if (sharedStringsXml != null) {
            val ssDoc = docFactory.newDocumentBuilder().parse(sharedStringsXml.byteInputStream())
            val sis = ssDoc.getElementsByTagName("si")
            for (i in 0 until sis.length) {
                val sb = StringBuilder()
                val item = sis.item(i)
                if (item is org.w3c.dom.Element) {
                    val texts = item.getElementsByTagName("t")
                    for (j in 0 until texts.length) sb.append(texts.item(j).textContent)
                }
                sharedStrings.add(sb.toString())
            }
        }

        val doc = docFactory.newDocumentBuilder().parse(sheetXml.byteInputStream())
        val rows = doc.getElementsByTagName("row")
        val result = mutableListOf<List<String>>()

        for (r in 0 until rows.length) {
            val cells = rows.item(r).childNodes
            val rowData = mutableListOf<String>()
            for (c in 0 until cells.length) {
                val cell = cells.item(c)
                if (cell.nodeName != "c") continue
                var value = ""
                val cellChildren = cell.childNodes
                for (v in 0 until cellChildren.length) {
                    if (cellChildren.item(v).nodeName == "v") {
                        val raw = cellChildren.item(v).textContent
                        val type = cell.attributes.getNamedItem("t")?.textContent
                        value = if (type == "s") {
                            val idx = raw.toIntOrNull()
                            if (idx != null && idx < sharedStrings.size) sharedStrings[idx] else raw
                        } else {
                            raw
                        }
                    }
                }
                rowData.add(value)
            }
            result.add(rowData)
        }
        return result
    }

    private fun procesarLineas(dataLines: List<List<String>>) {
        if (dataLines.size < 2) throw Exception("El archivo no tiene datos (solo cabecera)")
        Log.d("CSV", "Filas: ${dataLines.size}, primera fila celdas: ${dataLines.getOrNull(0)?.joinToString("|")?.take(200)}")
        val header = dataLines[0].map { it.trim().lowercase() }
        Log.d("CSV", "Cabeceras: $header")

        val idxCodigo = header.indexOfFirst { it == "codigo" || it == "id_oferta" || it == "cod" }
        val idxTitulo = header.indexOfFirst { it == "titulo" || it == "titulo_puesto" || it == "puesto" }
        val idxEmpresa = header.indexOfFirst { it == "nit" || it == "empresa" }
        val idxGrado = header.indexOfFirst { it == "grado" || it == "id_grado" }
        val idxExp = header.indexOfFirst { it == "experiencia_anios" || it == "exp" || it == "anios" || it == "experiencia" }
        val idxEdadMin = header.indexOfFirst { it == "edad_minima" || it == "edad_min" || it == "edadmin" }
        val idxEdadMax = header.indexOfFirst { it == "edad_maxima" || it == "edad_max" || it == "edadmax" }
        val idxFechaPub = header.indexOfFirst { it == "fecha_publicacion" || it == "fecha_pub" || it == "publicacion" }
        val idxFechaCad = header.indexOfFirst { it == "fecha_caducidad" || it == "fecha_cad" || it == "caducidad" }
        val idxDesc = header.indexOfFirst { it == "descripcion" || it == "descripcion_oferta" || it == "desc" }
        val idxReqCod = header.indexOfFirst { it == "req_codigo" || it == "req_cod" || it == "codigo_requisito" || it == "id_detalle" }
        val idxReqDesc = header.indexOfFirst { it == "req_descripcion" || it == "req_desc" || it == "descripcion_requisito" }

        Log.d("CSV", "Índices: cod=$idxCodigo tit=$idxTitulo emp=$idxEmpresa gra=$idxGrado")
        Log.d("CSV", "Primera línea datos: ${dataLines.getOrNull(1)?.joinToString("|")?.take(100)}")

        if (idxCodigo < 0) throw Exception("No encontré columna 'codigo'. Cabeceras: ${header.joinToString(", ")}")
        if (idxTitulo < 0) throw Exception("No encontré columna 'titulo'")
        if (idxEmpresa < 0) throw Exception("No encontré columna 'nit'")

        var count = 0
        var errorCount = 0
        val errores = StringBuilder()
        for (i in 1 until dataLines.size) {
            val cols = dataLines[i]
            if (cols.size <= maxOf(idxCodigo, idxTitulo, idxEmpresa)) {
                errorCount++; errores.append("Línea ${i+1}: faltan columnas básicas\n"); continue
            }

            val idOferta = cols[idxCodigo]
            val titulo = cols[idxTitulo]
            val nit = cols[idxEmpresa]
            val edadMin = if (idxEdadMin >= 0 && idxEdadMin < cols.size) cols[idxEdadMin].toIntOrNull() else null
            val edadMax = if (idxEdadMax >= 0 && idxEdadMax < cols.size) cols[idxEdadMax].toIntOrNull() else null
            val fechaPub = if (idxFechaPub >= 0 && idxFechaPub < cols.size) cols[idxFechaPub].trim() else ""
            val fechaCad = if (idxFechaCad >= 0 && idxFechaCad < cols.size) cols[idxFechaCad].trim() else ""
            val exp = if (idxExp >= 0 && idxExp < cols.size) cols[idxExp].toIntOrNull() else null

            val rowErrors = mutableListOf<String>()
            if (idOferta.isBlank()) rowErrors.add("código vacío")
            if (titulo.isBlank()) rowErrors.add("título vacío")
            if (nit.isBlank()) rowErrors.add("NIT vacío")
            if (edadMin != null && edadMin < 18) rowErrors.add("edad mínima $edadMin < 18")
            if (edadMin != null && edadMax != null && edadMin > edadMax) rowErrors.add("edad min > max ($edadMin > $edadMax)")
            if (exp != null && exp < 0) rowErrors.add("experiencia negativa ($exp)")
            if (fechaPub.isNotBlank() && fechaCad.isNotBlank() && fechaCad <= fechaPub) rowErrors.add("caducidad ≤ publicación")

            if (rowErrors.isNotEmpty()) {
                errorCount++
                val ref = idOferta.takeIf { it.isNotBlank() } ?: "línea ${i+1}"
                errores.append("$ref: ${rowErrors.joinToString(", ")}\n"); continue
            }

            val oferta = JSONObject().apply {
                put("id_oferta", idOferta); put("titulo", titulo); put("nit", nit)
                if (idxGrado >= 0 && idxGrado < cols.size) cols[idxGrado].toIntOrNull()?.let { put("id_grado", it) }
                if (idxExp >= 0 && idxExp < cols.size && exp != null) put("experiencia_anios", exp)
                if (idxEdadMin >= 0 && idxEdadMin < cols.size && edadMin != null) put("edad_minima", edadMin)
                if (idxEdadMax >= 0 && idxEdadMax < cols.size && edadMax != null) put("edad_maxima", edadMax)
                if (fechaPub.isNotEmpty()) put("fecha_publicacion", fechaPub)
                if (fechaCad.isNotEmpty()) put("fecha_caducidad", fechaCad)
                if (idxDesc >= 0 && idxDesc < cols.size && cols[idxDesc].isNotEmpty()) put("descripcion", cols[idxDesc])
                val reqArr = JSONArray()
                if (idxReqCod >= 0 && idxReqCod < cols.size && idxReqDesc >= 0 && idxReqDesc < cols.size
                    && cols[idxReqCod].isNotEmpty() && cols[idxReqDesc].isNotEmpty()) {
                    reqArr.put(JSONObject().apply { put("id_detalle", cols[idxReqCod]); put("descripcion", cols[idxReqDesc]) })
                }
                put("requisitos", reqArr)
            }
            pendingOffers.add(oferta); count++
        }
        pendingAdapter.notifyDataSetChanged(); updateInsertButton()
        val msg = if (count > 0) "$count ofertas cargadas" else "0 ofertas cargadas"
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
        if (errorCount > 0) {
            Snackbar.make(requireView(), "$errorCount filas rechazadas:\n${errores.trimEnd()}", Snackbar.LENGTH_LONG).show()
        }
    }

    private class PendingAdapter(
        private val items: MutableList<JSONObject>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<PendingAdapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_pending_offer, p, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            val id = item.optString("id_oferta", "")
            val tit = item.optString("titulo", "")
            val reqs = item.optJSONArray("requisitos")

            h.tvId.text = id
            var titleText = tit
            if (reqs != null && reqs.length() > 0) {
                val bullets = StringBuilder()
                for (j in 0 until reqs.length()) {
                    val r = reqs.getJSONObject(j)
                    val cod = r.optString("id_detalle", "")
                    val desc = r.optString("descripcion", "")
                    bullets.append("\n  • $cod: $desc")
                }
                titleText += bullets.toString()
            }
            h.tvTitle.text = titleText
            h.itemView.setOnClickListener { onDelete(pos) }
        }
        override fun getItemCount() = items.size
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvId: TextView = itemView.findViewById(R.id.tvOfferId)
            val tvTitle: TextView = itemView.findViewById(R.id.tvOfferTitle)
        }
    }
}
