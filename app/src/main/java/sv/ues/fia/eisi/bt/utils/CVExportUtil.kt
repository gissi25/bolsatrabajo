package sv.ues.fia.eisi.bt.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
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
    val requisitos: List<String>
)

object CVExportUtil {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val LINE_HEIGHT = 24f
    private const val SECTION_SPACE = 55f
    private const val PAGE_MAX_Y = PAGE_HEIGHT - MARGIN - 20f

    private val ACCENT = Color.rgb(51, 102, 255)
    private val ACCENT_DARK = Color.rgb(26, 79, 255)
    private val TEXT_DARK = Color.rgb(30, 30, 40)
    private val TEXT_MEDIUM = Color.rgb(90, 90, 100)

    fun generateCVPdf(context: Context, data: PostulantFullData, fileName: String): File {
        val document = PdfDocument()
        val mgr = PageManager(document)
        drawCVDocument(mgr, data)
        mgr.finish()
        return savePdf(document, context, fileName)
    }

    fun generateOfertaPdf(context: Context, data: OfertaFullData, fileName: String): File {
        val document = PdfDocument()
        val mgr = PageManager(document)
        drawOfertaDocument(mgr, data)
        mgr.finish()
        return savePdf(document, context, fileName)
    }

    private class PageManager(private val document: PdfDocument) {
        var canvas: Canvas
        var y: Float = MARGIN + 80f
        private var currentPage: PdfDocument.Page? = null

        init {
            canvas = newPage()
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

    private fun drawCVDocument(mgr: PageManager, data: PostulantFullData) {
        with(mgr) {
            drawPageTitle(canvas, "CURRICULUM VITAE")
            y += 20
            y = drawSectionHeader(canvas, "Datos Personales", y)
            y = drawField(canvas, "Nombre completo", "${data.nombre} ${data.apellido}", y)
            y = drawField(canvas, "Fecha de nacimiento", data.fechaNacimiento, y)
            y = drawField(canvas, "Correo electronico", data.email, y)
            y = drawField(canvas, "Telefono celular", data.telefonoCelular, y)
            if (data.telefonoCasa.isNotBlank()) y = drawField(canvas, "Telefono casa", data.telefonoCasa, y)
            if (data.direccion.isNotBlank()) y = drawField(canvas, "Direccion", data.direccion, y)
            if (data.nup.isNotBlank()) y = drawField(canvas, "NUP", data.nup, y)
            y = drawField(canvas, "Grado academico", data.gradoAcademico, y)

            if (data.formaciones.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Formacion Academica", y)
                for (f in data.formaciones) {
                    val titulo = f.getOrElse(0) { "" }
                    val institucion = f.getOrElse(1) { "" }
                    val inicio = f.getOrElse(2) { "" }
                    val fin = f.getOrElse(3) { "" }
                    val grado = f.getOrElse(4) { "" }
                    val periodo = if (inicio.isNotBlank() || fin.isNotBlank()) "$inicio — $fin" else ""
                    y = drawBullet(canvas, titulo, y); checkPage()
                    val sub = buildString {
                        if (institucion.isNotBlank()) append(institucion)
                        if (grado.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(grado) }
                        if (periodo.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(periodo) }
                    }
                    if (sub.isNotBlank()) { y = drawSubtext(canvas, sub, y); checkPage() }
                }
            }

            if (data.certificaciones.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Certificaciones", y)
                for (c in data.certificaciones) {
                    val nombre = c.getOrElse(0) { "" }
                    val institucion = c.getOrElse(1) { "" }
                    val fecha = c.getOrElse(2) { "" }
                    val tipo = c.getOrElse(3) { "" }
                    y = drawBullet(canvas, nombre, y); checkPage()
                    val sub = buildString {
                        if (tipo.isNotBlank()) append(tipo)
                        if (institucion.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(institucion) }
                        if (fecha.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(fecha) }
                    }
                    if (sub.isNotBlank()) { y = drawSubtext(canvas, sub, y); checkPage() }
                }
            }

            if (data.habilidades.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Habilidades", y)
                val sb = StringBuilder()
                for (h in data.habilidades) {
                    val nombre = h.getOrElse(0) { "" }
                    val nivel = h.getOrElse(1) { "" }
                    if (sb.isNotEmpty()) sb.append("  •  ")
                    sb.append(nombre)
                    if (nivel.isNotBlank()) sb.append(" ($nivel)")
                }
                y = drawBodyText(canvas, sb.toString(), y); checkPage()
            }

            if (data.experiencias.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Experiencia Laboral", y)
                for (e in data.experiencias) {
                    val puesto = e.getOrElse(0) { "" }
                    val empresa = e.getOrElse(1) { "" }
                    val periodo = e.getOrElse(2) { "" }
                    val desc = e.getOrElse(3) { "" }
                    y = drawBullet(canvas, puesto, y); checkPage()
                    val sub = buildString {
                        if (empresa.isNotBlank()) append(empresa)
                        if (periodo.isNotBlank()) { if (isNotEmpty()) append("  |  "); append(periodo) }
                    }
                    if (sub.isNotBlank()) { y = drawSubtext(canvas, sub, y); checkPage() }
                    if (desc.isNotBlank()) { y = drawSmallText(canvas, desc, y); checkPage() }
                }
            }

            if (data.redesSociales.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Redes Sociales", y)
                for (r in data.redesSociales) {
                    val nombre = r.getOrElse(0) { "" }
                    val url = r.getOrElse(1) { "" }
                    y = drawBullet(canvas, if (url.isNotBlank()) "$nombre: $url" else nombre, y); checkPage()
                }
            }
        }
    }

    private fun drawOfertaDocument(mgr: PageManager, data: OfertaFullData) {
        with(mgr) {
            drawPageTitle(canvas, "DETALLE DE LA VACANTE")
            y += 20

            y = drawSectionHeader(canvas, "Empresa", y)
            y = drawField(canvas, "Nombre", data.nombreEmpresa, y)
            y = drawField(canvas, "NIT", data.nit, y)
            if (data.contactoEmpresa.isNotBlank()) y = drawField(canvas, "Contacto", data.contactoEmpresa, y)

            y += SECTION_SPACE; checkPage()
            y = drawSectionHeader(canvas, "Puesto", y)
            y = drawField(canvas, "Titulo del puesto", data.tituloPuesto, y)
            y = drawField(canvas, "Grado academico requerido", data.nombreGrado, y)
            if (data.experienciaAnios.isNotBlank()) y = drawField(canvas, "Experiencia requerida", "${data.experienciaAnios} años", y)
            if (data.edadMinima.isNotBlank()) y = drawField(canvas, "Edad requerida", "${data.edadMinima} — ${data.edadMaxima} años", y)
            y = drawField(canvas, "Fecha de publicacion", data.fechaPublicacion, y)
            y = drawField(canvas, "Fecha de caducidad", data.fechaCaducidad, y)

            if (data.requisitos.isNotEmpty()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Requisitos", y)
                for (r in data.requisitos) {
                    y = drawBullet(canvas, r, y); checkPage()
                }
            }

            if (data.descripcion.isNotBlank()) {
                y += SECTION_SPACE; checkPage()
                y = drawSectionHeader(canvas, "Descripcion", y)
                y = drawSmallText(canvas, data.descripcion, y)
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
        val linePaint = Paint().apply {
            color = ACCENT
            strokeWidth = 3f
        }
        canvas.drawLine(MARGIN, y, MARGIN, y + 24, linePaint)

        val textPaint = Paint().apply {
            color = ACCENT_DARK
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText(text.uppercase(), MARGIN + 14, y + 17, textPaint)

        val dividerPaint = Paint().apply {
            color = Color.argb(60, 51, 102, 255)
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, y + 24, PAGE_WIDTH - MARGIN, y + 24, dividerPaint)
        return y + 48
    }

    private fun drawField(canvas: Canvas, label: String, value: String, y: Float): Float {
        val labelPaint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 10f
        }
        canvas.drawText(label, MARGIN + 10, y + 10, labelPaint)

        val valuePaint = Paint().apply {
            color = TEXT_DARK
            textSize = 11.5f
        }
        canvas.drawText(value, MARGIN + 10, y + 26, valuePaint)
        return y + LINE_HEIGHT + 6
    }

    private fun drawBullet(canvas: Canvas, text: String, y: Float): Float {
        val bulletPaint = Paint().apply {
            color = ACCENT
            textSize = 14f
        }
        val textPaint = Paint().apply {
            color = TEXT_DARK
            textSize = 11.5f
        }
        val bx = MARGIN + 10
        canvas.drawText("●", bx, y + 4, bulletPaint)
        val wrapped = wrapText(text, textPaint, CONTENT_WIDTH - 40)
        var cy = y
        for (line in wrapped) {
            canvas.drawText(line, bx + 18, cy + 4, textPaint)
            cy += LINE_HEIGHT
        }
        return cy
    }

    private fun drawSubtext(canvas: Canvas, text: String, y: Float): Float {
        val paint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 9.5f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
        }
        val wrapped = wrapText(text, paint, CONTENT_WIDTH - 40)
        var cy = y
        for (line in wrapped) {
            canvas.drawText(line, MARGIN + 30, cy + 4, paint)
            cy += LINE_HEIGHT - 4
        }
        return cy
    }

    private fun drawBodyText(canvas: Canvas, text: String, y: Float): Float {
        val paint = Paint().apply {
            color = TEXT_DARK
            textSize = 11f
        }
        val wrapped = wrapText(text, paint, CONTENT_WIDTH - 20)
        var cy = y
        for (line in wrapped) {
            canvas.drawText(line, MARGIN + 10, cy + 4, paint)
            cy += LINE_HEIGHT - 2
        }
        return cy
    }

    private fun drawSmallText(canvas: Canvas, text: String, y: Float): Float {
        val paint = Paint().apply {
            color = TEXT_MEDIUM
            textSize = 9.5f
        }
        val wrapped = wrapText(text, paint, CONTENT_WIDTH - 20)
        var cy = y
        for (line in wrapped) {
            canvas.drawText(line, MARGIN + 10, cy + 4, paint)
            cy += LINE_HEIGHT - 4
        }
        return cy
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
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
