package sv.ues.fia.eisi.bt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sv.ues.fia.eisi.bt.data.repository.MainRepository

class CrudViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository(application)

    private val _items = MutableLiveData<List<List<Any>>>()
    val items: LiveData<List<List<Any>>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentTable: String = ""

    fun setTable(tableName: String) {
        currentTable = tableName
        loadItems()
    }

    fun loadItems() {
        if (currentTable.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                delay(300) // Pequeña pausa para asegurar persistencia
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

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteRecord(currentTable, id)
                }
                delay(300)
                loadItems()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage
    
    fun clearError() {
        _errorMessage.value = null
    }

    fun insertRecord(tableName: String, values: List<String>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.insertRecord(tableName, values)
                }
                delay(300)
                currentTable = tableName
                loadItems()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error al guardar"
                e.printStackTrace()
            }
        }
    }

    fun updateRecord(tableName: String, id: String, values: List<String>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateRecord(tableName, id, values)
                }
                delay(300)
                currentTable = tableName
                loadItems()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error al actualizar"
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

    fun getFilteredOptions(childTable: String, childFkColumn: String, parentId: String): List<Pair<String, String>> {
        return repository.getFilteredOptions(childTable, childFkColumn, parentId)
    }
}