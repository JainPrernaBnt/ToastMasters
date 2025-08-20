package com.bntsoft.toastmasters.presentation.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.databinding.FragmentSignUpBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.bntsoft.toastmasters.utils.UiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val viewModel: AuthViewModel by viewModels()

    private val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    private var selectedGender: String? = null
    private var selectedMentorId: String? = null
    private var mentorsList: List<Pair<String, String>> = emptyList() // Pair of (id, name)

    private var formSubmitted: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupForm()
        setupClickListeners()
        observeViewModel()

    }

    private fun setupForm() {
        // Setup gender dropdown
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        (binding.genderAutoCompleteTextView as? AutoCompleteTextView)?.setAdapter(adapter)

        // Setup mentor dropdown (will be populated later)
        val mentorAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        (binding.mentorInputLayout.editText as? AutoCompleteTextView)?.setAdapter(mentorAdapter)

        // Setup text change listeners for form validation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }

        listOf(
            binding.nameEditText,
            binding.emailEditText,
            binding.phoneEditText,
            binding.addressEditText,
            binding.toastmastersIdEditText,
            binding.clubIdEditText,
            binding.passwordEditText,
            binding.confirmPasswordEditText
        ).forEach { editText ->
            editText.addTextChangedListener(textWatcher)
        }

        // Setup gender selection
        (binding.genderAutoCompleteTextView as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            selectedGender = genders[position].toString()
            validateForm()
        }

        // Setup mentor selection
        (binding.mentorInputLayout.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            if (position < mentorsList.size) {
                selectedMentorId = mentorsList[position].first
            }
        }

        // Setup joined date picker
        binding.joinedDateEditText.setOnClickListener {
            showDatePicker()
        }

        // Setup mentor assignment toggle
        binding.hasMentorSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.mentorInputLayout.isVisible = isChecked
            if (!isChecked) {
                selectedMentorId = null
            }
            validateForm()
        }

        // Setup role selection - no navigation needed here, just handle the selection
    }

    private fun setupClickListeners() {
        binding.signUpButton.setOnClickListener {
            hideKeyboard()
            attemptSignUp()
        }

        binding.loginTextView.setOnClickListener {
            findNavController().navigateUp()
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

                        is AuthViewModel.AuthUiState.SignUpSuccess -> {
                            showLoading(false)
                            showSuccessMessage(state.userRole, state.requiresApproval)
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

        // Observe mentors list when implemented
        /*
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mentors.collect { mentors ->
                    updateMentorsList(mentors)
                }
            }
        }
        */
    }

    private fun updateMentorsList(mentors: List<Pair<String, String>>) {
        mentorsList = mentors
        val mentorNames = mentors.map { it.second }
        val mentorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mentorNames
        )
        (binding.mentorInputLayout.editText as? AutoCompleteTextView)?.setAdapter(mentorAdapter)
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                binding.joinedDateEditText.setText(dateFormatter.format(calendar.time))
                validateForm()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set max date to today
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun attemptSignUp() {
        formSubmitted = true
        if (!validateForm()) return

        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val address = binding.addressEditText.text.toString().trim()
        val gender = selectedGender ?: ""
        val joinedDate = binding.joinedDateEditText.text.toString()
        val toastmastersId = binding.toastmastersIdEditText.text.toString().trim()
        val clubId = binding.clubIdEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()
        val mentorName = if (binding.mentorInputLayout.visibility == View.VISIBLE) {
            binding.mentorEditText.text.toString().trim()
        } else {
            ""
        }

        // Get selected role
        val role = if (binding.roleVpEducation.isChecked) {
            UserRole.VP_EDUCATION
        } else {
            UserRole.MEMBER
        }

        val user = User(
            name = name,
            email = email,
            phoneNumber = phone,
            address = address,
            gender = gender,
            joinedDate = dateFormatter.parse(joinedDate) ?: Date(),
            toastmastersId = toastmastersId,
            clubId = clubId,
            password = password,
            role = role, // Use selected role
            isApproved = role == UserRole.VP_EDUCATION, // Auto-approve VP Education
            mentorNames = if (mentorName.isNotBlank()) listOf(mentorName) else emptyList()
        )

        viewModel.signUp(user, binding.passwordEditText.text.toString())
    }

    private fun validateForm(): Boolean {
        if (!formSubmitted) {
            // Don't show validation errors until form is submitted
            return false
        }

        var isValid = true

        // Name validation
        if (binding.nameEditText.text.isNullOrEmpty()) {
            binding.nameInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.nameInputLayout.error = null
        }

        // Email validation
        if (binding.emailEditText.text.isNullOrEmpty()) {
            binding.emailInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.emailEditText.text.toString()).matches()) {
            binding.emailInputLayout.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }

        // Password validation
        if (binding.passwordEditText.text.isNullOrEmpty()) {
            binding.passwordInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else if (binding.passwordEditText.text.toString().length < 6) {
            binding.passwordInputLayout.error = getString(R.string.error_password_too_short)
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }

        // Confirm Password validation
        if (binding.confirmPasswordEditText.text.toString() != binding.passwordEditText.text.toString()) {
            binding.confirmPasswordInputLayout.error = getString(R.string.error_passwords_dont_match)
            isValid = false
        } else {
            binding.confirmPasswordInputLayout.error = null
        }

        // Phone validation
        if (binding.phoneEditText.text.isNullOrEmpty()) {
            binding.phoneInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else if (binding.phoneEditText.text.toString().length < 10) {
            binding.phoneInputLayout.error = getString(R.string.error_invalid_phone)
            isValid = false
        } else {
            binding.phoneInputLayout.error = null
        }
        // Toastmasters ID validation
        if (binding.toastmastersIdEditText.text.isNullOrEmpty()) {
            binding.toastmastersIdInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.toastmastersIdInputLayout.error = null
        }

        // Club ID validation
        if (binding.clubIdEditText.text.isNullOrEmpty()) {
            binding.clubIdInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.clubIdInputLayout.error = null
        }

        // Mentor validation (if has mentor is selected)
        if (binding.hasMentorSwitch.isChecked && selectedMentorId.isNullOrEmpty()) {
            binding.mentorInputLayout.error = getString(R.string.error_select_mentor)
            isValid = false
        } else {
            binding.mentorInputLayout.error = null
        }

        // Role validation
        if (!binding.roleVpEducation.isChecked && !binding.roleMember.isChecked) {
            binding.roleInputLayout.error = getString(R.string.error_select_role)
            isValid = false
        } else {
            binding.roleInputLayout.error = null
        }

        binding.signUpButton.isEnabled = isValid
        return isValid
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.signUpButton.isEnabled = !show
        binding.loginTextView.isEnabled = !show
    }

    private fun showSuccessMessage(userRole: UserRole, requiresApproval: Boolean) {
        // Save user role
        preferenceManager.saveUserRole(userRole)

        if (requiresApproval) {
            // For members requiring approval
            UiUtils.showSuccessMessage(
                binding.root,
                getString(R.string.signup_success_pending_approval)
            )

            // Navigate back to login after showing the message
            findNavController().popBackStack()
        } else {
            // For VP Education (auto-approved)
            // Show success message with email verification info
            UiUtils.showSuccessMessage(
                binding.root,
                getString(R.string.vp_education_signup_success)
            )

            // Navigate back to login after a short delay
            binding.root.postDelayed({
                findNavController().popBackStack()
            }, 2000) // 2 seconds delay to show the message
        }
    }

    private fun showErrorSnackbar(message: String) {
        UiUtils.showErrorWithRetry(binding.root, message)
    }

    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        view?.let { v ->
            val imm =
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
