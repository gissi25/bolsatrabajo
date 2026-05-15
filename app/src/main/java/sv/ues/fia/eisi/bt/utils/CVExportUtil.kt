package sv.ues.fia.eisi.bt.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import sv.ues.fia.eisi.bt.R
import java.io.File
import java.io.FileOutputStream

data class PostulantFullData(
    val idPostulante: String,
    val nombre: String,
    val apellido: String,
    val fechaNacimiento: String,
    val email: String,
    val telefonoCasa: String,
    val telefonoCelular: String,
    val direccion: String,
    val nup: String,
    val gradoAcademico: String,
    val genero: String,
    val tipoDocumento: String,
    val numDocumento: String,
    val departamento: String,
    val municipio: String,
    val distrito: String,
    val formaciones: List<List<String>>,
    val certificaciones: List<List<String>>,
    val habilidades: List<List<String>>,
    val experiencias: List<List<String>>,
    val redesSociales: List<List<String>>
)

data class OfertaFullData(
    val nit: String,
    val idOferta: String,
    val tituloPuesto: String,
    val fechaPublicacion: String,
    val fechaCaducidad: String,
    val experienciaAnios: String,
    val edadMinima: String,
    val edadMaxima: String,
    val descripcion: String,
    val nombreGrado: String,
    val nombreEmpresa: String,
    val contactoEmpresa: String,
    val empresaDepartamento: String,
    val empresaMunicipio: String,
    val empresaDistrito: String,
    val requisitos: List<String>
)

object CVExportUtil {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val COL_WIDTH = (CONTENT_WIDTH - 30) / 2
    private const val LINE_HEIGHT = 26f
    private const val SECTION_SPACE = 60f
    private const val PAGE_MAX_Y = PAGE_HEIGHT - MARGIN - 20f

    private val ACCENT = Color.rgb(51, 102, 255)
    private val ACCENT_DARK = Color.rgb(26, 79, 255)
    private val TEXT_DARK = Color.rgb(30, 30, 40)
    private val TEXT_MEDIUM = Color.rgb(90, 90, 100)

    fun generateCVPdf(context: Context, data: PostulantFullData, fileName: String): File {
        val document = PdfDocument()
        val mgr = PageManager(document)
        drawCVDocument(mgr, data, context)
        mgr.finish()
        return savePdf(document, context, fileName)
    }

    fun generateOfertaPdf(context: Context, data: OfertaFullData, fileName: String): File {
        val document = PdfDocument()
        val mgr = PageManager(document)
        drawOfertaDocument(mgr, data, context)
        mgr.finish()
        return savePdf(document, context, fileName)
    }

    private class PageManager(private val document: PdfDocument) {
        var canvas: Canvas
        var y: Float = MARGIN + 80f
        private var currentPage: PdfDocument.Page? = null

        init {
            canvas = newPage()
            y = MARGIN + 55f
        }

        private fun newPage(): Canvas {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
            )
            currentPage = page
            drawHeader(page.canvas, document.pages.size == 1)
            return page.canvas
        }

        fun checkPage(): Boolean {
            if (y > PAGE_MAX_Y) {
                currentPage?.let { document.finishPage(it) }
                canvas = newPage()
                y = MARGIN + 10f
                return true
            }
            return false
        }

        fun finish() {
            try { currentPage?.let { document.finishPage(it) } } catch (_: Exception) {}
        }
    }

    private fun drawHeader(canvas: Canvas, isFirstPage: Boolean) {
        val bgPaint = Paint().apply {
            color = ACCENT
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 60f, bgPaint)
    }

    private fun drawCVDocument(mgr: PageManager, data: PostulantFullData, context: Context) {
        with(mgr) {
            drawPageTitle(canvas, context.getString(R.string.pdf_curriculum_vitae))
            y = drawSectionHeader(canvas, "Datos Personales", y)

            val fullDir = buildString {
                if (data.direccion.isNotBlank()) append(data.direccion)
                if (data.distrito.isNotBlank()) { append(", "); append(data.distrito) }
                if (data.municipio.isNotBlank()) { append(", "); append(data.municipio) }
                if (data.departamento.isNotBlank()) { append(", "); append(data.departamento) }
            }

            val docInfo = buildString {
                if (data.tipoDocumento.isNotBlank()) append(data.tipoDocumento)
                if (data.numDocumento.isNotBlank()) { if (isNotEmpty()) append(" "); append(data.numDocumento) }
            }

            y = drawField2Col(canvas, context.getString(R.string.pdf_codigo), data.idPostulante, context.getString(R.string.pdf_grado), data.gradoAcademico, y)
            y = drawField2Col(canvas, context.getString(R.string.hint_nombre), "${data.nombre} ${data.apellido}", "Genero", data.genero, y)
            y = drawField2Col(canvas, "Nacimiento", data.fechaNacimiento, "Documento", docInfo, y)
            y = drawField2Col(canvas, "NUP", data.nup, "Email", data.email, y)
            y = drawField2Col(canvas, "Telefono", data.telefonoCelular, "Tel. Casa", data.telefonoCasa, y)
            if (fullDir.isNotBlank()) {
                checkPage()
                y = drawSectionField(canvas, "Direccion", fullDir, y, this)
            }

            if (data.formaciones.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Formacion Academica", y)
                for (f in data.formaciones) {
                    val titulo = f.getOrElse(0) { "" }
                    val institucion = f.getOrElse(1) { "" }
                    val inicio = f.getOrElse(2) { "" }
                    val fin = f.getOrElse(3) { "" }
                    val grado = f.getOrElse(4) { "" }
                    val fechaObtencion = f.getOrElse(5) { "" }
                    val periodo = if (inicio.isNotBlank() || fin.isNotBlank()) "$inicio → $fin" else ""

                    y = drawBullet(canvas, titulo.ifBlank { "(sin titulo)" }, y, this)
                    val sub = buildString {
                        if (institucion.isNotBlank()) append(institucion)
                        if (grado.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(grado) }
                        if (periodo.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(periodo) }
                    }
                    if (sub.isNotBlank()) { y = drawSubtext(canvas, sub, y, this) }
                    if (fechaObtencion.isNotBlank()) { y = drawSmallText(canvas, "Obtencion: $fechaObtencion", y, this) }
                }
            }

            if (data.certificaciones.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Certificaciones", y)
                for (c in data.certificaciones) {
                    val nombre = c.getOrElse(0) { "" }
                    val institucion = c.getOrElse(1) { "" }
                    val fechaCert = c.getOrElse(2) { "" }
                    val tipo = c.getOrElse(3) { "" }
                    val periodoInicio = c.getOrElse(4) { "" }
                    val periodoFin = c.getOrElse(5) { "" }
                    val periodo = if (periodoInicio.isNotBlank() || periodoFin.isNotBlank()) "$periodoInicio → $periodoFin" else ""

                    y = drawBullet(canvas, nombre.ifBlank { "(sin nombre)" }, y, this)
                    val sub = buildString {
                        if (tipo.isNotBlank()) append(tipo)
                        if (institucion.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(institucion) }
                        if (periodo.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(periodo) }
                    }
                    if (sub.isNotBlank()) { y = drawSubtext(canvas, sub, y, this) }
                    if (fechaCert.isNotBlank()) { y = drawSmallText(canvas, "Certificado: $fechaCert", y, this) }
                }
            }

            if (data.habilidades.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Habilidades", y)
                for (h in data.habilidades) {
                    val nombre = h.getOrElse(0) { "" }
                    val nivel = h.getOrElse(1) { "" }
                    val label = if (nivel.isNotBlank()) "$nombre ($nivel)" else nombre
                    y = drawBullet(canvas, label, y, this)
                }
            }

            if (data.experiencias.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Experiencia Laboral", y)
                for (e in data.experiencias) {
                    val puesto = e.getOrElse(0) { "" }
                    val empresa = e.getOrElse(1) { "" }
                    val inicio = e.getOrElse(2) { "" }
                    val fin = e.getOrElse(3) { "" }
                    val desc = e.getOrElse(4) { "" }
                    val contacto = e.getOrElse(5) { "" }
                    val periodo = if (inicio.isNotBlank() || fin.isNotBlank()) "$inicio → $fin" else ""

                    y = drawBullet(canvas, puesto.ifBlank { "(sin puesto)" }, y, this)
                    val sub = buildString {
                        if (empresa.isNotBlank()) append(empresa)
                        if (periodo.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(periodo) }
                    }
                    if (sub.isNotBlank()) { y = drawSubtext(canvas, sub, y, this) }
                    if (desc.isNotBlank()) { y = drawSmallText(canvas, desc, y, this) }
                    if (contacto.isNotBlank()) { y = drawSmallText(canvas, "Contacto: $contacto", y, this) }
                }
            }

            if (data.redesSociales.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Redes Sociales", y)
                for (r in data.redesSociales) {
                    val nombre = r.getOrElse(0) { "" }
                    val url = r.getOrElse(1) { "" }
                    val label = if (url.isNotBlank()) "$nombre: $url" else nombre
                    y = drawBullet(canvas, label, y, this)
                }
            }
        }
    }

    private fun drawOfertaDocument(mgr: PageManager, data: OfertaFullData, context: Context) {
        with(mgr) {
            drawPageTitle(canvas, context.getString(R.string.pdf_detalle_vacante))

            val dirEmpresa = buildString {
                if (data.empresaDistrito.isNotBlank()) append(data.empresaDistrito)
                if (data.empresaMunicipio.isNotBlank()) { if (isNotEmpty()) append(", "); append(data.empresaMunicipio) }
                if (data.empresaDepartamento.isNotBlank()) { if (isNotEmpty()) append(", "); append(data.empresaDepartamento) }
            }

            y = drawSectionHeader(canvas, "Empresa", y)
            y = drawField2Col(canvas, context.getString(R.string.hint_nombre), data.nombreEmpresa, context.getString(R.string.pdf_nit), data.nit, y)
            y = drawField2Col(canvas, context.getString(R.string.pdf_contacto), data.contactoEmpresa, context.getString(R.string.pdf_ubicacion), dirEmpresa, y)

            y += SECTION_SPACE; checkPage()
            y = drawSectionHeader(canvas, "Puesto", y)
            y = drawField2Col(canvas, "Titulo", data.tituloPuesto, "Grado requerido", data.nombreGrado, y)
            y = drawField2Col(canvas, "Experiencia", if (data.experienciaAnios.isNotBlank()) "${data.experienciaAnios} años" else "", "Edad", if (data.edadMinima.isNotBlank()) "${data.edadMinima} — ${data.edadMaxima} años" else "", y)
            y = drawField2Col(canvas, "Publicacion", data.fechaPublicacion, "Caducidad", data.fechaCaducidad, y)

            if (data.requisitos.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Requisitos", y)
                for (r in data.requisitos) {
                    y = drawBullet(canvas, r, y, this)
                }
            }

            if (data.descripcion.isNotBlank()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Descripcion", y)
                y = drawSmallText(canvas, data.descripcion, y, this)
            }
        }
    }

    private fun drawPageTitle(canvas: Canvas, title: String) {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, PAGE_WIDTH / 2f, 38f, textPaint)
    }

    private fun drawSectionHeader(canvas: Canvas, text: String, y: Float): Float {
        val textPaint = Paint().apply {
            color = ACCENT_DARK
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText(text.uppercase(), MARGIN, y + 17, textPaint)
        val dividerPaint = Paint().apply {
            color = Color.argb(60, 51, 102, 255)
            strokeWidth = 1.5f
        }
        canvas.drawLine(MARGIN, y + 28, PAGE_WIDTH - MARGIN, y + 28, dividerPaint)
        return y + 44
    }

    private fun drawField2Col(canvas: Canvas, label1: String, value1: String, label2: String, value2: String, y: Float): Float {
        val labelPaint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 9f
        }
        val valuePaint = Paint().apply {
            color = TEXT_DARK
            textSize = 10.5f
        }
        val x1 = MARGIN
        val x2 = MARGIN + COL_WIDTH + 30
        canvas.drawText(label1, x1, y + 10, labelPaint)
        canvas.drawText(value1, x1, y + 24, valuePaint)
        canvas.drawText(label2, x2, y + 10, labelPaint)
        canvas.drawText(value2, x2, y + 24, valuePaint)
        return y + LINE_HEIGHT
    }

    private fun drawSectionField(canvas: Canvas, label: String, value: String, y: Float, mgr: PageManager): Float {
        val labelPaint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 9f
        }
        canvas.drawText(label, MARGIN, y + 10, labelPaint)
        val valuePaint = Paint().apply {
            color = TEXT_DARK
            textSize = 10.5f
        }
        val wrapped = wrapText(value, valuePaint, CONTENT_WIDTH)
        var cy = y + 20
        for (line in wrapped) {
            mgr.checkPage()
            canvas.drawText(line, MARGIN, cy, valuePaint)
            cy += LINE_HEIGHT - 4
        }
        return cy + 4
    }

    private fun drawBullet(canvas: Canvas, text: String, y: Float, mgr: PageManager): Float {
        val textPaint = Paint().apply {
            color = TEXT_DARK
            textSize = 10.5f
        }
        val wrapped = wrapText(text, textPaint, CONTENT_WIDTH - 40)
        var cy = y
        for ((i, line) in wrapped.withIndex()) {
            mgr.checkPage()
            if (i == 0) {
                canvas.drawText("●", MARGIN, cy + 4, Paint().apply { color = ACCENT; textSize = 12f })
                canvas.drawText(line, MARGIN + 16, cy + 4, textPaint)
            } else {
                canvas.drawText(line, MARGIN + 16, cy + 4, textPaint)
            }
            cy += LINE_HEIGHT
        }
        return cy
    }

    private fun drawSubtext(canvas: Canvas, text: String, y: Float, mgr: PageManager): Float {
        val paint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 9f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
        }
        val wrapped = wrapText(text, paint, CONTENT_WIDTH - 30)
        var cy = y
        for (line in wrapped) {
            mgr.checkPage()
            canvas.drawText(line, MARGIN + 30, cy + 4, paint)
            cy += LINE_HEIGHT - 6
        }
        return cy
    }

    private fun drawSmallText(canvas: Canvas, text: String, y: Float, mgr: PageManager): Float {
        val paint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 9f
        }
        val wrapped = wrapText(text, paint, CONTENT_WIDTH - 30)
        var cy = y
        for (line in wrapped) {
            mgr.checkPage()
            canvas.drawText(line, MARGIN + 30, cy + 4, paint)
            cy += LINE_HEIGHT - 6
        }
        return cy
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val result = mutableListOf<String>()
        val words = text.split(" ")
        val current = StringBuilder()
        for (word in words) {
            val testLine = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(testLine) <= maxWidth) {
                if (current.isNotEmpty()) current.append(" ")
                current.append(word)
            } else {
                if (current.isNotEmpty()) result.add(current.toString())
                current.clear()
                current.append(word)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result.ifEmpty { listOf(text) }
    }

    private fun savePdf(document: PdfDocument, context: Context, fileName: String): File {
        val dir = File(context.filesDir, "pdfs").also { it.mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun getPdfUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
