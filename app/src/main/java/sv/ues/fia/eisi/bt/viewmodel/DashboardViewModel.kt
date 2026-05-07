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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository(application)

    private val _tables = MutableLiveData<List<MainRepository.TableInfo>>()
    val tables: LiveData<List<MainRepository.TableInfo>> = _tables

    private var allTablesOriginal: List<MainRepository.TableInfo> = emptyList()
    private var currentRole: String = Constants.ROLE_ADMIN

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _seedResult = MutableLiveData<Resource?>()
    val seedResult: LiveData<Resource?> get() = _seedResult

    fun loadTables(role: String? = null) {
        if (role != null) currentRole = role
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val all = withContext(Dispatchers.IO) {
                    repository.getAllTablesWithCount()
                }
                val roleTables = Constants.getRoleTables(currentRole)
                val filtered = all.filter { roleTables.containsKey(it.name) }
                allTablesOriginal = filtered
                _tables.postValue(filtered)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Error al cargar las tablas")
            } finally {
                _isLoading.postValue(false)
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
                allTablesOriginal = filtered
                _tables.postValue(filtered)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Error al actualizar contadores")
            }
        }
    }

    fun filterTables(query: String) {
        if (query.isBlank()) {
            _tables.value = allTablesOriginal
        } else {
            val filtered = allTablesOriginal.filter { 
                it.displayName.contains(query, ignoreCase = true) || 
                it.name.contains(query, ignoreCase = true) 
            }
            _tables.value = filtered
        }
    }
    
    fun loadOriginalTables() {
        _tables.value = allTablesOriginal
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