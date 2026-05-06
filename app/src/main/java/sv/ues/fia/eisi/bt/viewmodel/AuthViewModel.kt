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

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository(application)

    private val _loginResult = MutableLiveData<Result<MainRepository.Usuario?>>()
    val loginResult: LiveData<Result<MainRepository.Usuario?>> = _loginResult

    private val _registerResult = MutableLiveData<Result<Long>>()
    val registerResult: LiveData<Result<Long>> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginResult.value = Result.failure(Exception("Por favor complete todos los campos"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.login(username, password)
                }
                _loginResult.postValue(Result.success(result))
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun register(username: String, password: String, rol: String = "postulante") {
        if (username.isBlank() || password.isBlank()) {
            _registerResult.value = Result.failure(Exception("Por favor complete todos los campos"))
            return
        }

        if (password.length < 8) {
            _registerResult.value = Result.failure(Exception("La contraseña debe tener al menos 8 caracteres"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.register(username, password, rol)
                }
                _registerResult.postValue(Result.success(result))
            } catch (e: Exception) {
                _registerResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}