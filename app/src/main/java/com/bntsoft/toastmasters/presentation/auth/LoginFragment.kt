package com.bntsoft.toastmasters.presentation.auth

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.MemberActivity
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.VpMainActivity
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.databinding.FragmentLoginBinding
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var firestoreService: FirestoreService

    private val viewModel: AuthViewModel by viewModels()
    private val auth: FirebaseAuth by lazy { Firebase.auth }

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

        // Hide action bar and status bar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        // Disable back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing to disable back button
                }
            })

        // Hide menu items
        setHasOptionsMenu(false)

        setupForm()
        setupClickListeners()
        observeViewModel()

        // Check if user is already logged in and monitor session
        preferenceManager.userId?.let { userId ->
            monitorSessionChanges(userId)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if we were redirected from a session termination
        if (preferenceManager.userId != null && Firebase.auth.currentUser == null) {
            // Clear any stale data
            preferenceManager.clearUserData()
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
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthUiState.Loading -> {
                            showLoading(true)
                        }

                        is AuthViewModel.AuthUiState.Success -> {
                            showLoading(false)
                            // Handle successful login
                            val user = state.user
                            preferenceManager.userId = user.id
                            preferenceManager.userEmail = user.email
                            preferenceManager.userName = user.name
                            preferenceManager.saveUserRole(state.userRole)
                            preferenceManager.isLoggedIn = true // Set login flag

                            // Start monitoring session changes
                            monitorSessionChanges(user.id)

                            // Start notification listener service
                            val notificationServiceIntent = android.content.Intent(requireContext(), com.bntsoft.toastmasters.service.NotificationListenerService::class.java)
                            val serviceResult = requireContext().startService(notificationServiceIntent)
                            android.util.Log.d("LoginFragment", "Started NotificationListenerService: $serviceResult")

                            // Navigate to the appropriate activity based on role
                            val intent = if (state.userRole == UserRole.VP_EDUCATION) {
                                android.content.Intent(requireContext(), VpMainActivity::class.java)
                            } else {
                                android.content.Intent(requireContext(), MemberActivity::class.java)
                            }
                            // Clear the back stack so user can't go back to login with back button
                            intent.flags =
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            activity?.finish()
                        }

                        is AuthViewModel.AuthUiState.DeviceConflict -> {
                            showLoading(false)
                            showDeviceConflictDialog(state)
                        }

                        is AuthViewModel.AuthUiState.Error -> {
                            showLoading(false)
                            showErrorSnackbar(state.message)
                        }

                        is AuthViewModel.AuthUiState.SignUpSuccess -> {
                            // Handle successful sign up if needed
                            showLoading(false)
                        }

                        is AuthViewModel.AuthUiState.Initial,
                        is AuthViewModel.AuthUiState.Loading -> {
                            // Initial state or loading, no action needed
                            showLoading(state is AuthViewModel.AuthUiState.Loading)
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
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun monitorSessionChanges(userId: String) {
        // Cancel any existing session monitoring job
        sessionMonitoringJob?.cancel()

        sessionMonitoringJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                firestoreService.listenForSessionChanges(userId)
                    .catch { e ->
                        Log.e("LoginFragment", "Error listening for session changes", e)
                    }
                    .collect { sessionData ->
                        val isSessionActive = sessionData?.get("isActive") as? Boolean ?: false
                        
                        if (!isSessionActive && isAdded && isActivityActive()) {
                            // Check if this device is still in the allowed devices list
                            val deviceIds = sessionData?.get("deviceIds") as? List<String>
                            val singleDeviceId = sessionData?.get("deviceId")?.toString() // For backward compatibility
                            
                            val currentDeviceId = try {
                                android.provider.Settings.Secure.getString(
                                    requireContext().contentResolver,
                                    android.provider.Settings.Secure.ANDROID_ID
                                )
                            } catch (e: Exception) {
                                null
                            }
                            
                            val allowedDevices = deviceIds ?: (singleDeviceId?.let { listOf(it) } ?: emptyList())
                            
                            // Only show session terminated dialog if this device is not in the allowed list
                            // or if the session is explicitly deactivated (not just due to multi-device login)
                            if (currentDeviceId != null && !allowedDevices.contains(currentDeviceId)) {
                                showSessionTerminatedDialog()
                            } else if (sessionData?.get("forceLogout") == true) {
                                // Only force logout if explicitly requested
                                showSessionTerminatedDialog()
                            }
                        }
                    }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // This is expected when the coroutine is cancelled
                    Log.d("LoginFragment", "Session monitoring coroutine was cancelled")
                } else {
                    Log.e("LoginFragment", "Error in session monitoring", e)
                }
            }
        }
    }

    private fun isActivityActive(): Boolean {
        val activity = activity
        return activity != null && !activity.isFinishing && !activity.isDestroyed
    }

    private var sessionTerminatedDialog: androidx.appcompat.app.AlertDialog? = null
    private var sessionMonitoringJob: Job? = null

    private fun showSessionTerminatedDialog() {
        // Dismiss any existing dialog to prevent multiple dialogs
        sessionTerminatedDialog?.dismiss()

        if (!isAdded || !isActivityActive()) {
            return
        }

        try {
            sessionTerminatedDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Session Terminated")
                .setMessage("Your session has been terminated by another device. Please log in again.")
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    navigateToLogin()
                }
                .setOnDismissListener {
                    sessionTerminatedDialog = null
                }
                .show()
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error showing session terminated dialog", e)
        }
    }

    private fun navigateToLogin() {
        try {
            // Clear user data and Firebase auth
            preferenceManager.clearUserData()
            Firebase.auth.signOut()

            // Only navigate if we're still attached to an activity
            if (isAdded) {
                // Clear the back stack and navigate to login
                findNavController().popBackStack(R.id.loginFragment, false)

                // If we're not already on the login fragment, navigate to it
                if (findNavController().currentDestination?.id != R.id.loginFragment) {
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error navigating to login", e)
        }
    }
    private fun showDeviceConflictDialog(state: AuthViewModel.AuthUiState.DeviceConflict) {
        if (!isAdded || isDetached) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Device Conflict")
            .setMessage(state.message)
            .setPositiveButton("Continue") { _, _ ->
                state.onContinue()
            }
            .setNegativeButton("Cancel") { _, _ ->
                state.onCancel()
            }
            .setOnDismissListener {
                // Ensure we reset the state if dialog is dismissed
                state.onCancel()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        // Cancel any ongoing session monitoring
        sessionMonitoringJob?.cancel()
        sessionMonitoringJob = null

        // Dismiss any showing dialogs
        sessionTerminatedDialog?.dismiss()
        sessionTerminatedDialog = null

        super.onDestroyView()
        _binding = null
    }
}
