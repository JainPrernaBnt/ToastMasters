package com.bntsoft.toastmasters.presentation.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentLoginBinding
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupForm()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            // Handle back navigation or close
            if (!findNavController().navigateUp()) {
                requireActivity().finish()
            }
        }
    }

    private fun setupForm() {
        // Set up text change listeners for form validation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }

        binding.emailPhoneEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)

        // Handle keyboard done/next actions
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            hideKeyboard()
            attemptLogin()
        }

        binding.signUpButton.setOnClickListener {
            navigateToSignUp()
        }

        binding.forgotPasswordButton.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is AuthViewModel.AuthUiState.Loading -> {
                            showLoading(true)
                        }

                        is AuthViewModel.AuthUiState.Success -> {
                            showLoading(false)
                            // Navigate to main screen
                            findNavController().navigate(R.id.action_login_to_main_navigation)
                        }

                        is AuthViewModel.AuthUiState.SignUpSuccess -> {
                            showLoading(false)
                            // Show signup success message
                            val message = if (state.requiresApproval) {
                                getString(R.string.signup_success_pending_approval)
                            } else {
                                getString(R.string.signup_success_approved)
                            }
                            showSuccessSnackbar(message)
                        }

                        is AuthViewModel.AuthUiState.Error -> {
                            showLoading(false)
                            showErrorSnackbar(state.message)
                        }

                        else -> {
                            showLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun attemptLogin() {
        if (!validateForm()) return

        val identifier = binding.emailPhoneEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        viewModel.login(identifier, password)
    }

    private fun validateForm(): Boolean {
        val emailPhone = binding.emailPhoneEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        val isValid = emailPhone.isNotBlank() && password.length >= 6
        binding.loginButton.isEnabled = isValid

        return isValid
    }

    private fun showForgotPasswordDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.forgot_password_title)
            .setMessage(R.string.forgot_password_message)
            .setView(R.layout.dialog_forgot_password)
            .setPositiveButton(R.string.send_reset_link) { dialog, _ ->
                val emailEditText = (dialog as androidx.appcompat.app.AlertDialog)
                    .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.emailEditText)
                val email = emailEditText?.text?.toString()?.trim()

                if (!email.isNullOrBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                        .matches()
                ) {
                    sendPasswordResetEmail(email)
                } else {
                    showErrorSnackbar(getString(R.string.error_invalid_email))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            viewModel.sendPasswordResetEmail(email) { result ->
                showLoading(false)
                result.onSuccess {
                    showSuccessSnackbar(getString(R.string.reset_password_email_sent))
                }.onFailure { exception ->
                    showErrorSnackbar(
                        exception.message ?: getString(R.string.error_generic)
                    )
                }
            }
        }
    }

    private fun navigateToSignUp() {
        findNavController().navigate(R.id.action_login_to_signUpFragment)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.loginButton.isEnabled = !show
        binding.signUpButton.isEnabled = !show
        binding.forgotPasswordButton.isEnabled = !show
    }

    private fun showSuccessSnackbar(message: String) {
        UiUtils.showSuccessMessage(binding.root, message)
    }

    private fun showErrorSnackbar(message: String) {
        UiUtils.showErrorWithRetry(binding.root, message)
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(InputMethodManager::class.java)
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
