# Cambios necesarios en Android Studio

## Resumen

Para integrar el **Servicio 2 (Recomendador de Formación)** y el **Servicio 1 mejorado** en la app Android, hay que tocar **6 archivos**:

| # | Archivo | Cambio |
|---|---------|--------|
| 1 | `ApiService.kt` | Agregar método `recomendarFormacion()` |
| 2 | `Servicio2Fragment.kt` | **NUEVO** — Pantalla del recomendador |
| 3 | `fragment_servicio2.xml` | **NUEVO** — Layout de la pantalla |
| 4 | `nav_graph.xml` | Agregar destino `servicio2Fragment` y acción |
| 5 | `DashboardFragment.kt` | Wirear clic de Servicio 2 |
| 6 | `strings.xml` | Agregar strings para la UI |

---

## 1. `ApiService.kt` — Agregar método

**Archivo:** `app/src/main/java/sv/ues/fia/eisi/bt/service/ApiService.kt`

Agregar este método dentro del `object ApiService` (después de `sincronizarUnPostulante`):

```kotlin
suspend fun recomendarFormacion(idPostulante: String): JSONObject = withContext(Dispatchers.Main) {
    val body = JSONObject().apply { put("id_postulante", idPostulante) }
    fetch("recomendar_formacion", "POST", body.toString())
}
```

---

## 2. `Servicio2Fragment.kt` — NUEVO archivo

**Crear en:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/servicios/Servicio2Fragment.kt`

```kotlin
package sv.ues.fia.eisi.bt.ui.servicios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONObject
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.service.ApiService

class Servicio2Fragment : Fragment() {

    private lateinit var etIdPostulante: TextInputEditText
    private lateinit var btnAnalizar: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGrados: TextView
    private lateinit var tvInstituciones: TextView
    private lateinit var tvSkills: TextView
    private lateinit var tvSkillsFaltan: TextView
    private lateinit var tvMercado: TextView
    private lateinit var containerResultado: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_servicio2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etIdPostulante = view.findViewById(R.id.etIdPostulante)
        btnAnalizar = view.findViewById(R.id.btnAnalizar)
        progressBar = view.findViewById(R.id.progressBar)
        tvGrados = view.findViewById(R.id.tvGrados)
        tvInstituciones = view.findViewById(R.id.tvInstituciones)
        tvSkills = view.findViewById(R.id.tvSkills)
        tvSkillsFaltan = view.findViewById(R.id.tvSkillsFaltan)
        tvMercado = view.findViewById(R.id.tvMercado)
        containerResultado = view.findViewById(R.id.containerResultado)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }

        btnAnalizar.setOnClickListener { analizar() }
    }

    private fun analizar() {
        val id = etIdPostulante.text?.toString()?.trim()
        if (id.isNullOrEmpty()) {
            Snackbar.make(requireView(), "Ingresá un ID de postulante", Snackbar.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnAnalizar.isEnabled = false
        containerResultado.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = ApiService.recomendarFormacion(id)
                if (result.optBoolean("exito", false)) {
                    mostrarResultado(result.getJSONObject("data"))
                } else {
                    Snackbar.make(requireView(), result.optString("error", "Error"), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(requireView(), e.message?.take(300) ?: "Error de conexión", Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            btnAnalizar.isEnabled = true
        }
    }

    private fun mostrarResultado(data: JSONObject) {
        val postulante = data.getJSONObject("postulante")
        val nombre = postulante.getString("nombre")

        // Grados demandados
        val arrGrados = data.getJSONArray("grados_mas_demandados")
        val gradosText = StringBuilder("📊 Grados más demandados:\n")
        if (arrGrados.length() == 0) {
            gradosText.append("  (no hay ofertas activas aún)\n")
        } else {
            for (i in 0 until minOf(arrGrados.length(), 5)) {
                val g = arrGrados.getJSONObject(i)
                val marca = if (g.optBoolean("es_tu_grado", false)) " ← tu grado" else ""
                gradosText.append("  ${i+1}. ${g.getString("NOMBRE_GRADO")} — ${g.getInt("total_ofertas")} ofertas$marca\n")
            }
        }

        // Instituciones top
        val arrInst = data.getJSONArray("instituciones_top")
        val instText = StringBuilder("🏫 Instituciones con más egresados:\n")
        if (arrInst.length() == 0) {
            instText.append("  (sin datos aún)\n")
        } else {
            for (i in 0 until minOf(arrInst.length(), 5)) {
                val inst = arrInst.getJSONObject(i)
                instText.append("  ${i+1}. ${inst.getString("NOMBRE_INSTITUCION")} — ${inst.getInt("total_egresados")} egresados\n")
            }
        }

        // Skills que te faltan
        val arrFaltan = data.getJSONArray("skills_que_te_faltan")
        val faltanText = StringBuilder("🎯 Skills que te faltan (${arrFaltan.length()}):\n")
        if (arrFaltan.length() == 0) {
            faltanText.append("  ¡Tenés todas las skills del sistema!\n")
        } else {
            for (i in 0 until minOf(arrFaltan.length(), 10)) {
                val s = arrFaltan.getJSONObject(i)
                faltanText.append("  • ${s.getString("NOMBRE_HABILIDAD")} (${s.getString("NOMBRE_CATEGORIA")})\n")
            }
            if (arrFaltan.length() > 10) faltanText.append("  ... y ${arrFaltan.length() - 10} más\n")
        }

        // Skills más comunes
        val arrComunes = data.getJSONArray("skills_mas_comunes")
        val comunesText = StringBuilder("📈 Skills más comunes entre postulantes:\n")
        if (arrComunes.length() == 0) {
            comunesText.append("  (sin datos)\n")
        } else {
            for (i in 0 until minOf(arrComunes.length(), 10)) {
                val s = arrComunes.getJSONObject(i)
                comunesText.append("  • ${s.getString("NOMBRE_HABILIDAD")} — ${s.getInt("total_postulantes")} postulantes\n")
            }
        }

        // Mercado
        val mercado = data.getJSONObject("estadisticas_mercado")
        val mercadoText = "📈 Mercado: ${mercado.getInt("ofertas_activas")} ofertas activas | " +
                "${mercado.getInt("total_postulantes")} postulantes | " +
                "${mercado.getInt("total_empresas")} empresas\n" +
                "Competencia: ${mercado.getString("competencia_promedio")}"

        tvGrados.text = gradosText.trimEnd()
        tvInstituciones.text = instText.trimEnd()
        tvSkillsFaltan.text = faltanText.trimEnd()
        tvSkills.text = comunesText.trimEnd()
        tvMercado.text = mercadoText

        containerResultado.visibility = View.VISIBLE
    }
}
```

---

## 3. `fragment_servicio2.xml` — NUEVO layout

**Crear en:** `app/src/main/res/layout/fragment_servicio2.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimary"
        android:theme="@style/ThemeOverlay.MaterialComponents.Dark.ActionBar"
        app:navigationIcon="@drawable/ic_arrow_back"
        app:title="Recomendador de Formación"
        app:titleTextColor="@android:color/white" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:gravity="center_horizontal">

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="ID del Postulante"
                app:startIconDrawable="@drawable/ic_person"
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etIdPostulante"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="text"
                    android:text="HG23008" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnAnalizar"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="Analizar mi perfil"
                style="@style/Widget.Material3.Button" />

            <ProgressBar
                android:id="@+id/progressBar"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:visibility="gone" />

            <LinearLayout
                android:id="@+id/containerResultado"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone"
                android:layout_marginTop="16dp">

                <TextView
                    android:id="@+id/tvGrados"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:textSize="13sp"
                    android:lineSpacingExtra="2dp"
                    android:padding="12dp"
                    android:background="?attr/colorSurfaceVariant"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:id="@+id/tvInstituciones"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:textSize="13sp"
                    android:lineSpacingExtra="2dp"
                    android:padding="12dp"
                    android:background="?attr/colorSurfaceVariant"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:id="@+id/tvSkills"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:textSize="13sp"
                    android:lineSpacingExtra="2dp"
                    android:padding="12dp"
                    android:background="?attr/colorSurfaceVariant"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:id="@+id/tvSkillsFaltan"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:textSize="13sp"
                    android:lineSpacingExtra="2dp"
                    android:padding="12dp"
                    android:background="?attr/colorSurfaceVariant"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:id="@+id/tvMercado"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:textSize="12sp"
                    android:lineSpacingExtra="2dp"
                    android:padding="12dp"
                    android:background="?attr/colorSurfaceVariant"
                    android:textColor="?attr/colorPrimary" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

---

## 4. `nav_graph.xml` — Agregar destino y acción

**Archivo:** `app/src/main/res/navigation/nav_graph.xml`

Agregar el fragment destino (después de `servicio1Fragment`):

```xml
<fragment
    android:id="@+id/servicio2Fragment"
    android:name="sv.ues.fia.eisi.bt.ui.servicios.Servicio2Fragment"
    android:label="Recomendador de Formación"
    tools:layout="@layout/fragment_servicio2" />
```

Agregar la acción de navegación (dentro de `dashboardFragment`):

```xml
<action
    android:id="@+id/action_dashboard_to_servicio2"
    app:destination="@id/servicio2Fragment" />
```

---

## 5. `DashboardFragment.kt` — Wirear clic de Servicio 2

**Archivo:** `app/src/main/java/sv/ues/fia/eisi/bt/ui/dashboard/DashboardFragment.kt`

En la función `setupRecyclerView()`, en `onServiceClick`, agregar el caso 2:

```kotlin
onServiceClick = { service ->
    when (service.id) {
        1 -> findNavController().navigate(R.id.action_dashboard_to_bulkOferta)
        2 -> findNavController().navigate(R.id.action_dashboard_to_servicio2)  // ← AGREGAR ESTA LÍNEA
        3 -> findNavController().navigate(R.id.action_dashboard_to_servicio3)
    }
}
```

---

## 6. `strings.xml` — Opcional (etiquetas en español)

**Archivo:** `app/src/main/res/values/strings.xml`

Agregar si querés personalizar textos:

```xml
<string name="servicio2_titulo">Recomendador de Formación</string>
<string name="servicio2_desc">Analizá el mercado para decidir qué estudiar</string>
```

---

## Resumen de archivos a crear/modificar

```
CREAR:
  app/src/main/java/sv/ues/fia/eisi/bt/ui/servicios/Servicio2Fragment.kt
  app/src/main/res/layout/fragment_servicio2.xml

MODIFICAR:
  app/src/main/java/sv/ues/fia/eisi/bt/service/ApiService.kt       (+1 método)
  app/src/main/res/navigation/nav_graph.xml                        (+1 fragment +1 acción)
  app/src/main/java/sv/ues/fia/eisi/bt/ui/dashboard/DashboardFragment.kt  (+1 línea)

(OPCIONAL):
  app/src/main/res/values/strings.xml                              (+2 strings)
```
