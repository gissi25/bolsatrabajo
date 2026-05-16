package sv.ues.fia.eisi.bt.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import sv.ues.fia.eisi.bt.R
import sv.ues.fia.eisi.bt.utils.Constants
import sv.ues.fia.eisi.bt.utils.LocaleHelper
import sv.ues.fia.eisi.bt.utils.StyledToast
import sv.ues.fia.eisi.bt.utils.ThemeToggleHelper
import sv.ues.fia.eisi.bt.utils.TriggerErrorTranslator
import sv.ues.fia.eisi.bt.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnLanguage: ImageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilUsername = view.findViewById(R.id.tilUsername)
        tilPassword = view.findViewById(R.id.tilPassword)
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnRegister = view.findViewById(R.id.btnRegister)
        btnThemeToggle = view.findViewById(R.id.btnThemeToggle)
        btnLanguage = view.findViewById(R.id.btnLanguage)

        btnThemeToggle.setImageResource(ThemeToggleHelper.getIconRes(requireContext()))
        btnThemeToggle.setOnClickListener {
            ThemeToggleHelper.toggle(requireActivity())
        }

        btnLanguage.setImageResource(ThemeToggleHelper.getWorldIconRes(requireContext()))
        btnLanguage.setOnClickListener { showLanguageMenu() }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(username, password)) {
                viewModel.login(username, password)
            }
        }

        btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { usuario ->
                if (usuario != null) {
                    saveSession(usuario.idUsuario, usuario.username, usuario.rol)
                    findNavController().navigate(R.id.action_login_to_dashboard)
                } else {
                    StyledToast.show(requireContext(), getString(R.string.usuario_o_contrasena_incorrectos))
                }
            }
            result.onFailure { exception ->
                StyledToast.show(requireContext(), TriggerErrorTranslator.translate(exception.message, requireContext()))
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            btnLogin.isEnabled = !isLoading
            btnRegister.isEnabled = !isLoading
        }
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

    private fun validateInput(username: String, password: String): Boolean {
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
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun saveSession(userId: Int, username: String, rol: String) {
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(Constants.KEY_IS_LOGGED_IN, true)
            putInt(Constants.KEY_USER_ID, userId)
            putString(Constants.KEY_USERNAME, username)
            putString(Constants.KEY_USER_ROLE, rol)
            apply()
        }
    }
}
