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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    private val jsInterface = object {
        @JavascriptInterface
        fun onResult(json: String) {
            Log.d(TAG, "WV raw: ${json.take(300)}")
            Handler(Looper.getMainLooper()).post {
                try {
                    val obj = JSONObject(json)
                    pendingCallback?.invoke(obj)
                } catch (e: Exception) {
                    pendingCallback?.invoke(JSONObject().apply { put("_error", json.take(200)) })
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

    private suspend fun fetch(action: String, method: String = "GET", body: String? = null): JSONObject {
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
                        let r = await fetch('${BASE_URL}$action');
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
                            cont.resumeWithException(Exception("Respuesta inválida: ${json.optString("_error")}"))
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
}
