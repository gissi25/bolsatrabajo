package sv.ues.fia.eisi.bt.service

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ApiService {

    private const val BASE_URL = "https://bolsadetrabajopdm.gt.tc/go.php?action="
    private const val TAG = "ApiService"

    private var webView: WebView? = null
    private var challengeDone = false
    private var pendingCallback: ((JSONObject) -> Unit)? = null
    private val fetchMutex = Mutex()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsInterface = object {
        @JavascriptInterface
        fun onResult(json: String) {
            Log.d(TAG, "WV raw: ${json.take(300)}")
            Handler(Looper.getMainLooper()).post {
                try {
                    val obj = JSONObject(json)
                    pendingCallback?.invoke(obj)
                } catch (e: Exception) {
                    val detalle = json.trim().ifBlank { "respuesta vacía del servidor" }
                    pendingCallback?.invoke(JSONObject().apply { put("_error", detalle.take(200)) })
                }
                pendingCallback = null
            }
        }
    }

    fun initChallenge(wv: WebView) {
        if (challengeDone) return
        webView = wv

        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.addJavascriptInterface(jsInterface, "BTBridge")

        var loads = 0
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                loads++
                Log.d(TAG, "WV load #$loads: $url")
                if (loads >= 2) {
                    challengeDone = true
                    Log.d(TAG, "Challenge listo!")
                }
            }
        }
        wv.loadUrl("${BASE_URL}empresas")
        Log.d(TAG, "Challenge iniciado...")
    }

    private fun parseJsonResponse(jsonStr: String): JSONObject {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty()) throw Exception("Respuesta vacía del servidor")
        if (trimmed.startsWith("<")) throw Exception("El servidor devolvió HTML en lugar de JSON")
        val obj = JSONObject(trimmed)
        if (obj.has("error")) throw Exception(obj.getString("error"))
        return obj
    }

    private fun buildHttpRequest(url: String, body: String? = null): Request {
        val builder = Request.Builder().url(url)
        CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let {
            builder.addHeader("Cookie", it)
        }
        return if (body != null) {
            builder.post(body.toRequestBody("application/json".toMediaType())).build()
        } else {
            builder.build()
        }
    }

    private suspend fun fetch(action: String, method: String = "GET", body: String? = null): JSONObject = fetchMutex.withLock {
        val wv = webView ?: throw Exception("initChallenge() no llamado aún")

        val jsCode = when (method) {
            "POST" -> {
                val b64 = Base64.encodeToString((body ?: "{}").toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                """
                (async function() {
                    try {
                        let bodyStr = atob('$b64');
                        let r = await fetch('${BASE_URL}$action', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            credentials: 'include',
                            body: bodyStr
                        });
                        let t = await r.text();
                        BTBridge.onResult(t);
                    } catch(e) {
                        BTBridge.onResult(JSON.stringify({error: e.message}));
                    }
                })();
            """
            }
            else -> """
                (async function() {
                    try {
                        let r = await fetch('${BASE_URL}$action', {credentials: 'include'});
                        let t = await r.text();
                        BTBridge.onResult(t);
                    } catch(e) {
                        BTBridge.onResult(JSON.stringify({error: e.message}));
                    }
                })();
            """
        }

        return suspendCancellableCoroutine { cont ->
            var done = false
            pendingCallback = { json ->
                if (!done) {
                    done = true
                    try {
                        if (json.has("_error")) {
                            val detalle = json.optString("_error").ifBlank { "formato JSON no reconocido" }
                            cont.resumeWithException(Exception("Respuesta inválida: $detalle"))
                        } else if (json.has("error")) {
                            cont.resumeWithException(Exception(json.getString("error")))
                        } else {
                            cont.resume(json)
                        }
                    } catch (_: Exception) {}
                }
            }

            Handler(Looper.getMainLooper()).post {
                wv.evaluateJavascript(jsCode, null)
                Log.d(TAG, "JS injected: $action")
            }

            cont.invokeOnCancellation { done = true }

            Handler(Looper.getMainLooper()).postDelayed({
                if (!done) {
                    done = true
                    pendingCallback = null
                    cont.resumeWithException(Exception("Timeout - JS no respondió"))
                }
            }, 60000)
        }
    }

    private suspend fun fetchDirect(action: String, body: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val url = "${BASE_URL}${action}"
        Log.d(TAG, "OkHttp $url")
        val response = okHttpClient.newCall(buildHttpRequest(url, body)).execute()
        val jsonStr = response.body?.string() ?: throw Exception("Respuesta vacía")
        Log.d(TAG, "OkHttp response: ${jsonStr.take(300)}")
        parseJsonResponse(jsonStr)
    }

    private suspend fun fetchWithFallback(action: String, body: String? = null): JSONObject {
        return try {
            fetchDirect(action, body)
        } catch (e: Exception) {
            Log.w(TAG, "OkHttp falló, usando WebView: ${e.message}")
            withContext(Dispatchers.Main) {
                if (body != null) fetch(action, "POST", body) else fetch(action)
            }
        }
    }

    suspend fun getEmpresas(): List<JSONObject> = withContext(Dispatchers.Main) {
        val json = fetch("empresas")
        val arr = json.getJSONArray("data")
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun getGrados(): List<JSONObject> = withContext(Dispatchers.Main) {
        val json = fetch("grados")
        val arr = json.getJSONArray("data")
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun insertarOfertas(ofertas: List<JSONObject>): JSONObject = withContext(Dispatchers.Main) {
        val arr = JSONArray()
        for (of in ofertas) arr.put(of)
        val body = JSONObject().apply { put("ofertas", arr) }
        fetch("insertar_ofertas", "POST", body.toString())
    }

    suspend fun sincronizarUnPostulante(postulante: JSONObject): JSONObject = withContext(Dispatchers.Main) {
        fetch("sincronizar_postulantes", "POST", postulante.toString())
    }

    suspend fun buscarOfertasPorEdad(edad: Int, idPostulante: String = ""): List<JSONObject> {
        val action = buildString {
            append("ofertas_por_edad&edad=$edad")
            if (idPostulante.isNotBlank()) {
                append("&id_postulante=${URLEncoder.encode(idPostulante, "UTF-8")}")
            }
        }
        val json = fetchWithFallback(action)
        val arr = json.getJSONArray("data")
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun postularOferta(idPostulante: String, nit: String, idOferta: String): JSONObject = withContext(Dispatchers.Main) {
        // InfinityFree (openresty) rechaza POST con 400; GET via WebView funciona.
        val action = buildString {
            append("postular")
            append("&id_postulante=${URLEncoder.encode(idPostulante, "UTF-8")}")
            append("&nit=${URLEncoder.encode(nit, "UTF-8")}")
            append("&id_oferta=${URLEncoder.encode(idOferta, "UTF-8")}")
        }
        fetch(action)
    }

    suspend fun getDashboardEmpresa(nit: String): JSONObject = withContext(Dispatchers.Main) {
        fetch("dashboard_empresa&nit=$nit")
    }

    suspend fun recomendarFormacion(idPostulante: String): JSONObject = withContext(Dispatchers.Main) {
        val body = JSONObject().apply { put("id_postulante", idPostulante) }
        fetch("recomendar_formacion", "POST", body.toString())
    }

    suspend fun sincronizarCertificaciones(body: JSONObject): JSONObject = withContext(Dispatchers.Main) {
        fetch("sincronizar_certificaciones", "POST", body.toString())
    }

    suspend fun getTiposCertificacion(): List<JSONObject> = withContext(Dispatchers.Main) {
        val json = fetch("tipos_certificacion")
        val arr = json.getJSONArray("data")
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun buscarCertificaciones(tipo: Int, nombre: String?, anio: Int?): JSONObject = withContext(Dispatchers.Main) {
        val params = mutableListOf<String>()
        params.add("tipo=$tipo")
        if (!nombre.isNullOrBlank()) params.add("nombre=${java.net.URLEncoder.encode(nombre, "UTF-8")}")
        if (anio != null && anio > 0) params.add("anio=$anio")
        fetch("buscar_certificaciones&${params.joinToString("&")}")
    }

    // Servicio 5: descargar datos del servidor
    suspend fun getCatalogos(): JSONObject = withContext(Dispatchers.Main) {
        fetch("catalogos")
    }

    suspend fun getEmpresasFull(): JSONObject = withContext(Dispatchers.Main) {
        fetch("empresas_full")
    }

    suspend fun getOfertasFull(): JSONObject = withContext(Dispatchers.Main) {
        fetch("ofertas_full")
    }

    suspend fun getPostulantesFull(): JSONObject = withContext(Dispatchers.Main) {
        fetch("postulantes_full")
    }

    suspend fun getPostulacionesFull(idPostulante: String? = null): JSONObject = withContext(Dispatchers.Main) {
        val action = if (!idPostulante.isNullOrBlank()) {
            "postulaciones_full&id_postulante=${java.net.URLEncoder.encode(idPostulante, "UTF-8")}"
        } else {
            "postulaciones_full"
        }
        fetch(action)
    }

    suspend fun insertarPostulacion(postulacion: JSONObject): JSONObject = withContext(Dispatchers.Main) {
        fetch("insertar_postulacion", "POST", postulacion.toString())
    }

    // Servicio 6: filtros geográficos y de postulantes
    suspend fun getDepartamentos(): JSONObject = withContext(Dispatchers.Main) {
        fetch("departamentos")
    }

    suspend fun getMunicipiosPorDepto(idDepartamento: String): JSONObject = withContext(Dispatchers.Main) {
        fetch("municipios_por_depto&id_departamento=${java.net.URLEncoder.encode(idDepartamento, "UTF-8")}")
    }

    suspend fun filtrarOfertasPorUbicacion(idDepartamento: String, idMunicipio: String? = null): JSONObject = withContext(Dispatchers.Main) {
        var action = "filtrar_ofertas_ubicacion&id_departamento=${java.net.URLEncoder.encode(idDepartamento, "UTF-8")}"
        if (!idMunicipio.isNullOrBlank()) {
            action += "&id_municipio=${java.net.URLEncoder.encode(idMunicipio, "UTF-8")}"
        }
        fetch(action)
    }

    suspend fun filtrarPostulantesPorEmpresa(nit: String, estado: String? = null): JSONObject = withContext(Dispatchers.Main) {
        if (!estado.isNullOrBlank()) {
            fetch("filtrar_postulantes_empresa_estado&nit=${java.net.URLEncoder.encode(nit, "UTF-8")}&estado=${java.net.URLEncoder.encode(estado, "UTF-8")}")
        } else {
            fetch("filtrar_postulantes_empresa&nit=${java.net.URLEncoder.encode(nit, "UTF-8")}")
        }
    }

    suspend fun getPostulantes(): List<JSONObject> = withContext(Dispatchers.Main) {
        val json = fetch("postulantes")
        val arr = json.getJSONArray("data")
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun getOfertasVigentes(nit: String): List<JSONObject> = withContext(Dispatchers.Main) {
        val json = fetch("ofertas_vigentes&nit=${java.net.URLEncoder.encode(nit, "UTF-8")}")
        val arr = json.getJSONArray("data")
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun matchingPostulante(idPostulante: String): JSONObject = withContext(Dispatchers.Main) {
        val body = JSONObject().apply { put("id_postulante", idPostulante) }
        fetch("matching_postulante", "POST", body.toString())
    }

    suspend fun matchingOferta(nit: String, idOferta: String): JSONObject = withContext(Dispatchers.Main) {
        val body = JSONObject().apply { put("nit", nit); put("id_oferta", idOferta) }
        fetch("matching_oferta", "POST", body.toString())
    }

    suspend fun getMisPostulaciones(idPostulante: String): JSONObject {
        val body = JSONObject().apply { put("id_postulante", idPostulante) }
        return fetchWithFallback("mis_postulaciones", body.toString())
    }
}
