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
import sv.ues.fia.eisi.bt.utils.TriggerErrorTranslator

sealed class Resource {
    data class Success(val message: String) : Resource()
    data class Error(val message: String, val translatedMessage: String) : Resource()
}

class CrudViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository(application)

    private val _items = MutableLiveData<List<List<Any>>>()
    val items: LiveData<List<List<Any>>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentTable: String = ""

    private val _operationResult = MutableLiveData<Resource?>()
    val operationResult: LiveData<Resource?> get() = _operationResult

    private val _deleteDependencies = MutableLiveData<List<MainRepository.DependencyInfo>>()
    val deleteDependencies: LiveData<List<MainRepository.DependencyInfo>> get() = _deleteDependencies

    fun clearResult() {
        _operationResult.value = null
    }

    fun setTable(tableName: String) {
        currentTable = tableName
        loadItems()
    }

    fun loadItems() {
        if (currentTable.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.searchTable(currentTable, "")
                }
                _items.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteItem(id: String) {
        if (currentTable.isBlank()) return
        _operationResult.value = null

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteRecord(currentTable, id)
                }
                loadItems()
                _operationResult.value = Resource.Success(getApplication<Application>().getString(R.string.eliminado_correctamente))
            } catch (e: Exception) {
                val translated = TriggerErrorTranslator.translate(e.message)
                _operationResult.value = Resource.Error(e.message ?: getApplication<Application>().getString(R.string.error_al_eliminar), translated)
                e.printStackTrace()
            }
        }
    }

    fun deleteItemByRow(rowData: List<String>) {
        if (currentTable.isBlank()) return
        _operationResult.value = null

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteRecordByRow(currentTable, rowData)
                }
                loadItems()
                _operationResult.value = Resource.Success(getApplication<Application>().getString(R.string.eliminado_correctamente))
            } catch (e: Exception) {
                val translated = TriggerErrorTranslator.translate(e.message)
                _operationResult.value = Resource.Error(e.message ?: getApplication<Application>().getString(R.string.error_al_eliminar), translated)
                e.printStackTrace()
            }
        }
    }

    fun checkDeleteDependencies(id: String) {
        if (currentTable.isBlank()) return
        viewModelScope.launch {
            val deps = withContext(Dispatchers.IO) {
                repository.getDeleteDependencies(currentTable, id)
            }
            _deleteDependencies.postValue(deps)
        }
    }


    fun insertRecord(tableName: String, values: List<String>) {
        _operationResult.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.insertRecord(tableName, values)
                }
                currentTable = tableName
                loadItems()
                _operationResult.value = Resource.Success(getApplication<Application>().getString(R.string.guardado_correctamente))
            } catch (e: Exception) {
                val translated = TriggerErrorTranslator.translate(e.message)
                _operationResult.value = Resource.Error(e.message ?: getApplication<Application>().getString(R.string.error_al_guardar), translated)
                e.printStackTrace()
            }
        }
    }

    fun updateRecord(tableName: String, id: String, values: List<String>) {
        _operationResult.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateRecord(tableName, id, values)
                }
                currentTable = tableName
                loadItems()
                _operationResult.value = Resource.Success(getApplication<Application>().getString(R.string.actualizado_correctamente))
            } catch (e: Exception) {
                val translated = TriggerErrorTranslator.translate(e.message)
                _operationResult.value = Resource.Error(e.message ?: getApplication<Application>().getString(R.string.error_al_actualizar), translated)
                e.printStackTrace()
            }
        }
    }

    fun getFkReferences(tableName: String): Map<String, MainRepository.FkReference> {
        return repository.getFkReferences(tableName)
    }

    fun getDropdownOptions(refTable: String, displayColumn: String): List<Pair<String, String>> {
        return repository.getDropdownOptions(refTable, displayColumn)
    }

    fun getFilteredOptions(childTable: String, childFkColumn: String, parentId: String, displayColumn: String? = null, includeExpired: Boolean = false): List<Pair<String, String>> {
        return repository.getFilteredOptions(childTable, childFkColumn, parentId, displayColumn, includeExpired)
    }

    fun getDepartamentoByMunicipio(deptoId: String, munId: String): String? {
        return repository.getDepartamentoByMunicipio(deptoId, munId)
    }

    fun getMunicipioByDistrito(deptoId: String, munId: String, distritoId: String): String? {
        return repository.getMunicipioByDistrito(deptoId, munId, distritoId)
    }
}