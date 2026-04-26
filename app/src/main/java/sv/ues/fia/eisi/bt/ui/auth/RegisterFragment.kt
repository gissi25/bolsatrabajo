package sv.ues.fia.eisi.bt.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tilRol: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var actvRol: MaterialAutoCompleteTextView
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnLogin: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilUsername = view.findViewById(R.id.tilUsername)
        tilPassword = view.findViewById(R.id.tilPassword)
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)
        tilRol = view.findViewById(R.id.tilRol)
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        actvRol = view.findViewById(R.id.actvRol)
        btnRegister = view.findViewById(R.id.btnRegister)
        btnLogin = view.findViewById(R.id.btnLogin)

        // Setup rol dropdown
        val roles = arrayOf("postulante", "empresa", "admin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        actvRol.setAdapter(adapter)
        actvRol.setText("postulante", false)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val rol = actvRol.text.toString().trim().ifBlank { "postulante" }

            if (validateInput(username, password, confirmPassword)) {
                viewModel.register(username, password, rol)
            }
        }

        btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { userId ->
                if (userId > 0) {
                    StyledToast.show(requireContext(), "Registro exitoso. Ahora puedes iniciar sesión.")
                    findNavController().navigate(R.id.action_register_to_login)
                } else if (userId == -2L) {
                    tilUsername.error = getString(R.string.username_already_exists)
                    StyledToast.show(requireContext(), "El nombre de usuario ya existe")
                } else {
                    StyledToast.show(requireContext(), "Error en el registro. Intenta de nuevo.")
                }
            }
            result.onFailure { exception ->
                StyledToast.show(requireContext(), exception.message ?: "Error en el registro")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            btnRegister.isEnabled = !isLoading
            btnLogin.isEnabled = !isLoading
        }
    }

    private fun validateInput(username: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (username.isBlank()) {
            tilUsername.error = getString(R.string.field_required)
            isValid = false
        } else {
            tilUsername.error = null
        }

        if (password.isBlank()) {
            tilPassword.error = getString(R.string.field_required)
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = getString(R.string.password_too_short)
            isValid = false
        } else {
            tilPassword.error = null
        }

        if (confirmPassword.isBlank()) {
            tilConfirmPassword.error = getString(R.string.field_required)
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = getString(R.string.password_mismatch)
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        return isValid
    }
}