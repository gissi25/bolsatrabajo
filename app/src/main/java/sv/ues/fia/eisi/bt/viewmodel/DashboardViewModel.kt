package sv.ues.fia.eisi.bt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.TriggerErrorTranslator
import sv.ues.fia.eisi.bt.utils.removeAccents

sealed class DashboardItem {
    data class Section(val sectionKey: String, val isExpanded: Boolean = false) : DashboardItem()
    data class Table(val info: MainRepository.TableInfo, val isReadOnly: Boolean = false, val sectionKey: String = "") : DashboardItem()
    data class WebService(val id: Int, val title: String) : DashboardItem()

    val contentSectionKey: String? get() = when (this) {
        is Section -> null
        is Table -> sectionKey
        is WebService -> DashboardItem.SECTION_SERVICIOS_WEB
    }

    companion object {
        const val SECTION_SERVICIOS_WEB = "servicios_web"
    }
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository(application)

    private val _items = MutableLiveData<List<DashboardItem>>()
    val items: LiveData<List<DashboardItem>> = _items

    private var allItemsOriginal: List<DashboardItem> = emptyList()
    private var currentRole: String = Constants.ROLE_ADMIN
    private val expandedSections = mutableSetOf<String>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _seedResult = MutableLiveData<Resource?>()
    val seedResult: LiveData<Resource?> get() = _seedResult

    private val webServices = (1..10).map { id ->
        val title = when (id) {
            1 -> getApplication<Application>().getString(R.string.servicio_1_titulo)
            2 -> getApplication<Application>().getString(R.string.servicio_2_titulo)
            7 -> "Filtrado de ofertas por edad"
            8 -> "Inteligencia empresarial"
            else -> "Servicio $id"
        }
        DashboardItem.WebService(id, title)
    }

    private val catalogTables = setOf(
        "CATEGORIA_HABILIDAD", "GENERO", "TIPO_DOCUMENTO", "DEPARTAMENTO",
        "MUNICIPIO", "DISTRITO", "INSTITUCION", "GRADO_ACADEMICO",
        "RED_SOCIAL", "TIPO_CERTIFICACION", "HABILIDAD", "OFERTA_ACADEMICA", "USUARIO"
    )
    private val empresaTables = setOf("EMPRESA", "OFERTA_TRABAJO", "DETALLE_REQUISITO")
    private val postulanteTables = setOf(
        "POSTULANTE", "FORMACION_ACADEMICA", "CERTIFICACION",
        "EXPERIENCIA_LABORAL", "HABILIDAD_POSTULANTE", "RED_SOCIAL_POSTULANTE", "POSTULACION"
    )

    fun loadTables(role: String? = null) {
        if (role != null) currentRole = role
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val all = withContext(Dispatchers.IO) {
                    repository.getAllTablesWithCount()
                }
                val roleTables = Constants.getRoleTables(currentRole)
                val filtered = all.filter { roleTables.containsKey(it.name) }
                allItemsOriginal = buildSectionedList(filtered) + buildWebServiceSection()
                _items.postValue(allItemsOriginal.filter { item ->
                    item is DashboardItem.Section || item.contentSectionKey in expandedSections
                })
            } catch (e: Exception) {
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private val catalogOrder = listOf(
        "DEPARTAMENTO", "MUNICIPIO", "DISTRITO", "GENERO", "TIPO_DOCUMENTO",
        "GRADO_ACADEMICO", "INSTITUCION", "OFERTA_ACADEMICA", "TIPO_CERTIFICACION",
        "RED_SOCIAL", "CATEGORIA_HABILIDAD", "HABILIDAD", "USUARIO"
    )
    private val empresaOrder = listOf("EMPRESA", "OFERTA_TRABAJO", "DETALLE_REQUISITO")
    private val postulanteOrder = listOf(
        "POSTULANTE", "FORMACION_ACADEMICA", "CERTIFICACION",
        "EXPERIENCIA_LABORAL", "HABILIDAD_POSTULANTE", "RED_SOCIAL_POSTULANTE", "POSTULACION"
    )

    companion object {
        const val SECTION_CATALOGOS = "catalogos"
        const val SECTION_EMPRESA = "empresa"
        const val SECTION_POSTULANTE = "postulante"
        const val SECTION_OTRAS = "otras"
    }

    private fun buildSectionedList(tables: List<MainRepository.TableInfo>): List<DashboardItem> {
        val roleTables = Constants.getRoleTables(currentRole)
        val catalog = tables.filter { it.name in catalogTables }
        val empresa = tables.filter { it.name in empresaTables }
        val postulante = tables.filter { it.name in postulanteTables }
        val otros = tables.filter { it.name !in catalogTables && it.name !in empresaTables && it.name !in postulanteTables }

        fun accessLevel(name: String) = roleTables[name] != Constants.AccessLevel.FULL
        fun sortByOrder(list: List<MainRepository.TableInfo>, order: List<String>) =
            list.sortedBy { order.indexOf(it.name).let { i -> if (i < 0) Int.MAX_VALUE else i } }

        val result = mutableListOf<DashboardItem>()
        if (catalog.isNotEmpty()) {
            result.add(DashboardItem.Section(SECTION_CATALOGOS, SECTION_CATALOGOS in expandedSections))
            result.addAll(sortByOrder(catalog, catalogOrder).map { DashboardItem.Table(it, accessLevel(it.name), SECTION_CATALOGOS) })
        }
        if (empresa.isNotEmpty()) {
            result.add(DashboardItem.Section(SECTION_EMPRESA, SECTION_EMPRESA in expandedSections))
            result.addAll(sortByOrder(empresa, empresaOrder).map { DashboardItem.Table(it, accessLevel(it.name), SECTION_EMPRESA) })
        }
        if (postulante.isNotEmpty()) {
            result.add(DashboardItem.Section(SECTION_POSTULANTE, SECTION_POSTULANTE in expandedSections))
            result.addAll(sortByOrder(postulante, postulanteOrder).map { DashboardItem.Table(it, accessLevel(it.name), SECTION_POSTULANTE) })
        }
        if (otros.isNotEmpty()) {
            result.add(DashboardItem.Section(SECTION_OTRAS, SECTION_OTRAS in expandedSections))
            result.addAll(otros.sortedBy { it.displayName }.map { DashboardItem.Table(it, accessLevel(it.name), SECTION_OTRAS) })
        }
        return result
    }

    private fun buildWebServiceSection(): List<DashboardItem> {
        val filtered = webServices.filter { sv ->
            when (sv.id) {
                1 -> currentRole == Constants.ROLE_ADMIN || currentRole == Constants.ROLE_EMPRESA
                5 -> currentRole == Constants.ROLE_ADMIN || currentRole == Constants.ROLE_POSTULANTE
                6 -> currentRole == Constants.ROLE_ADMIN || currentRole == Constants.ROLE_POSTULANTE
                7 -> currentRole == Constants.ROLE_ADMIN || currentRole == Constants.ROLE_POSTULANTE
                8 -> currentRole == Constants.ROLE_ADMIN || currentRole == Constants.ROLE_EMPRESA
                9 -> currentRole == Constants.ROLE_EMPRESA || currentRole == Constants.ROLE_ADMIN
                10 -> currentRole == Constants.ROLE_POSTULANTE || currentRole == Constants.ROLE_ADMIN
                else -> currentRole == Constants.ROLE_ADMIN
            }
        }
        val result = mutableListOf<DashboardItem>()
        if (filtered.isNotEmpty()) {
            result.add(DashboardItem.Section(DashboardItem.SECTION_SERVICIOS_WEB, DashboardItem.SECTION_SERVICIOS_WEB in expandedSections))
            result.addAll(filtered)
        }
        return result
    }

    fun toggleSection(key: String) {
        if (key in expandedSections) expandedSections.remove(key) else expandedSections.add(key)
        allItemsOriginal = allItemsOriginal.map { item ->
            if (item is DashboardItem.Section && item.sectionKey == key) item.copy(isExpanded = key in expandedSections) else item
        }
        _items.value = allItemsOriginal.filter { item ->
            when (item) {
                is DashboardItem.Section -> true
                is DashboardItem.Table -> item.sectionKey in expandedSections
                is DashboardItem.WebService -> item.contentSectionKey in expandedSections
            }
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            try {
                val all = withContext(Dispatchers.IO) {
                    repository.getAllTablesWithCount()
                }
                val roleTables = Constants.getRoleTables(currentRole)
                val filtered = all.filter { roleTables.containsKey(it.name) }
                allItemsOriginal = buildSectionedList(filtered) + buildWebServiceSection()
                _items.postValue(allItemsOriginal.filter { item ->
                    item is DashboardItem.Section || item.contentSectionKey in expandedSections
                })
            } catch (e: Exception) {
            }
        }
    }

    fun filterTables(query: String) {
        val q = query.removeAccents()
        if (q.isBlank()) {
            _items.value = allItemsOriginal.filter { item ->
                item is DashboardItem.Section || item.contentSectionKey in expandedSections
            }
        } else {
            val matchingKeys = allItemsOriginal.filterIsInstance<DashboardItem.Table>()
                .filter { it.info.displayName.removeAccents().contains(q, ignoreCase = true) || it.info.name.removeAccents().contains(q, ignoreCase = true) }
                .map { it.sectionKey }.toSet()
            val filtered = allItemsOriginal.filter { item ->
                when (item) {
                    is DashboardItem.Section -> item.sectionKey in matchingKeys
                    is DashboardItem.WebService -> false
                    is DashboardItem.Table ->
                        (item.info.displayName.removeAccents().contains(q, ignoreCase = true) || item.info.name.removeAccents().contains(q, ignoreCase = true))
                }
            }
            _items.value = filtered
        }
    }

    fun loadOriginalTables() {
        _items.value = allItemsOriginal.filter { item ->
            item is DashboardItem.Section || item.contentSectionKey in expandedSections
        }
    }

    fun clearSeedResult() {
        _seedResult.value = null
    }

    fun insertSeedData() {
        _seedResult.value = null
        viewModelScope.launch {
            try {
                val error = withContext(Dispatchers.IO) {
                    repository.insertSeedData()
                }
                if (error == null) {
                    _seedResult.postValue(Resource.Success(getApplication<Application>().getString(R.string.seed_inserted)))
                    refreshCounts()
                } else {
                    _seedResult.postValue(Resource.Error(error, TriggerErrorTranslator.translate(error, getApplication())))
                }
            } catch (e: Exception) {
                _seedResult.postValue(Resource.Error(e.message ?: getApplication<Application>().getString(R.string.error_desconocido), getApplication<Application>().getString(R.string.seed_error)))
            }
        }
    }
}
