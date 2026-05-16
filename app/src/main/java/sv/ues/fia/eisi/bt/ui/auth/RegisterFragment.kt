package sv.ues.fia.eisi.bt.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.LocaleHelper
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.utils.ThemeToggleHelper
import sv.ues.fia.eisi.bt.utils.TriggerErrorTranslator
import sv.ues.fia.eisi.bt.viewmodel.AuthViewModel

class   RegisterFragment : Fragment() {

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
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnLanguage: ImageButton

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
        btnThemeToggle = view.findViewById(R.id.btnThemeToggle)
        btnLanguage = view.findViewById(R.id.btnLanguage)

        btnThemeToggle.setImageResource(ThemeToggleHelper.getIconRes(requireContext()))
        btnThemeToggle.setOnClickListener {
            ThemeToggleHelper.toggle(requireActivity())
        }

        btnLanguage.setImageResource(ThemeToggleHelper.getWorldIconRes(requireContext()))
        btnLanguage.setOnClickListener { showLanguageMenu() }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val rol = if (actvRol.text?.isNotBlank() == true) {
                val selected = actvRol.text.toString().trim()
                when (selected) {
                    getString(R.string.rol_postulante) -> "postulante"
                    getString(R.string.rol_empresa) -> "gerente de empresa"
                    getString(R.string.rol_admin) -> "administrador"
                    else -> "postulante"
                }
            } else "postulante"

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
                    StyledToast.show(requireContext(), getString(R.string.registro_exitoso))
                    findNavController().navigate(R.id.action_register_to_login)
                } else if (userId == -2L) {
                    tilUsername.error = getString(R.string.username_already_exists)
                    StyledToast.show(requireContext(), getString(R.string.username_already_exists))
                } else {
                    StyledToast.show(requireContext(), getString(R.string.error_registro))
                }
            }
            result.onFailure { exception ->
                StyledToast.show(requireContext(), TriggerErrorTranslator.translate(exception.message, requireContext()))
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            btnRegister.isEnabled = !isLoading
            btnLogin.isEnabled = !isLoading
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val roles = arrayOf(
            getString(R.string.rol_postulante),
            getString(R.string.rol_empresa),
            getString(R.string.rol_admin)
        )
        actvRol.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles))
        actvRol.setText(getString(R.string.rol_postulante), false)
    }

    private fun showLanguageMenu() {
        val languages = arrayOf(
            getString(R.string.espanol) to "es",
            getString(R.string.ingles) to "en",
            getString(R.string.portugues) to "pt"
        )
        val labels = languages.map { it.first }.toTypedArray()
        val currentLang = LocaleHelper.getLanguage(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.idiomas))
            .setItems(labels) { _, which ->
                val lang = languages[which].second
                if (lang != currentLang) {
                    LocaleHelper.setLocale(requireContext(), lang)
                    requireActivity().recreate()
                }
            }
            .show()
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
        } else if (password.length < 8) {
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
