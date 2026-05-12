package sv.ues.fia.eisi.bt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sv.ues.fia.eisi.bt.data.repository.MainRepository
import sv.ues.fia.eisi.bt.utils.Constants

sealed class DashboardItem {
    data class Section(val title: String, val isExpanded: Boolean = false) : DashboardItem()
    data class Table(val info: MainRepository.TableInfo, val isReadOnly: Boolean = false, val sectionTitle: String = "") : DashboardItem()
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
                expandedSections.clear()
                allItemsOriginal = buildSectionedList(filtered)
                _items.postValue(allItemsOriginal.filter { item ->
                    item is DashboardItem.Section || (item as? DashboardItem.Table)?.sectionTitle in expandedSections
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
            result.add(DashboardItem.Section("CATÁLOGOS", "CATÁLOGOS" in expandedSections))
            result.addAll(sortByOrder(catalog, catalogOrder).map { DashboardItem.Table(it, accessLevel(it.name), "CATÁLOGOS") })
        }
        if (empresa.isNotEmpty()) {
            result.add(DashboardItem.Section("EMPRESA", "EMPRESA" in expandedSections))
            result.addAll(sortByOrder(empresa, empresaOrder).map { DashboardItem.Table(it, accessLevel(it.name), "EMPRESA") })
        }
        if (postulante.isNotEmpty()) {
            result.add(DashboardItem.Section("POSTULANTE", "POSTULANTE" in expandedSections))
            result.addAll(sortByOrder(postulante, postulanteOrder).map { DashboardItem.Table(it, accessLevel(it.name), "POSTULANTE") })
        }
        if (otros.isNotEmpty()) {
            result.add(DashboardItem.Section("OTRAS", "OTRAS" in expandedSections))
            result.addAll(otros.sortedBy { it.displayName }.map { DashboardItem.Table(it, accessLevel(it.name), "OTRAS") })
        }
        return result
    }

    fun toggleSection(title: String) {
        if (title in expandedSections) expandedSections.remove(title) else expandedSections.add(title)
        allItemsOriginal = allItemsOriginal.map { item ->
            if (item is DashboardItem.Section && item.title == title) item.copy(isExpanded = title in expandedSections) else item
        }
        _items.value = allItemsOriginal.filter { item ->
            when (item) {
                is DashboardItem.Section -> true
                is DashboardItem.Table -> item.sectionTitle in expandedSections
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
                allItemsOriginal = buildSectionedList(filtered)
                _items.postValue(allItemsOriginal.filter { item ->
                    item is DashboardItem.Section || (item as? DashboardItem.Table)?.sectionTitle in expandedSections
                })
            } catch (e: Exception) {
            }
        }
    }

    fun filterTables(query: String) {
        if (query.isBlank()) {
            _items.value = allItemsOriginal.filter { item ->
                item is DashboardItem.Section || (item as? DashboardItem.Table)?.sectionTitle in expandedSections
            }
        } else {
            val matchingNames = allItemsOriginal.filterIsInstance<DashboardItem.Table>()
                .filter { it.info.displayName.contains(query, ignoreCase = true) || it.info.name.contains(query, ignoreCase = true) }
                .map { it.sectionTitle }.toSet()
            val filtered = allItemsOriginal.filter { item ->
                when (item) {
                    is DashboardItem.Section -> item.title in matchingNames
                    is DashboardItem.Table ->
                        (item.info.displayName.contains(query, ignoreCase = true) || item.info.name.contains(query, ignoreCase = true))
                }
            }
            _items.value = filtered
        }
    }

    fun loadOriginalTables() {
        _items.value = allItemsOriginal.filter { item ->
            item is DashboardItem.Section || (item as? DashboardItem.Table)?.sectionTitle in expandedSections
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
                    _seedResult.postValue(Resource.Success("Datos insertados correctamente"))
                    refreshCounts()
                } else {
                    _seedResult.postValue(Resource.Error(error, error))
                }
            } catch (e: Exception) {
                _seedResult.postValue(Resource.Error(e.message ?: "Error desconocido", "Error al insertar datos"))
            }
        }
    }
}
