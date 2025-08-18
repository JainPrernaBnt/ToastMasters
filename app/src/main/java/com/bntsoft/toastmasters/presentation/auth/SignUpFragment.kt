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
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.databinding.FragmentSignUpBinding
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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
        
        setupToolbar()
        setupForm()
        setupClickListeners()
        observeViewModel()
        
        // Load mentors if needed
        // viewModel.loadMentors() // Uncomment when implementing mentor loading
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().finish()
            }
        }
    }

    private fun setupForm() {
        // Setup gender dropdown
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
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
    }
    
    private fun setupClickListeners() {
        binding.signUpButton.setOnClickListener {
            hideKeyboard()
            attemptSignUp()
        }
        
        binding.loginButton.setOnClickListener {
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
        if (!validateForm()) return
        
        val user = User(
            name = binding.nameEditText.text.toString().trim(),
            email = binding.emailEditText.text.toString().trim(),
            phoneNumber = binding.phoneEditText.text.toString().trim(),
            address = binding.addressEditText.text.toString().trim(),
            gender = selectedGender ?: "",
            joinedDate = calendar.time,
            toastmastersId = binding.toastmastersIdEditText.text.toString().trim(),
            clubId = binding.clubIdEditText.text.toString().trim(),
            password = binding.passwordEditText.text.toString(),
            mentorIds = selectedMentorId?.let { listOf(it) } ?: emptyList(),
            isNewMember = true,
            isApproved = false // Will be set by admin
        )
        
        viewModel.signUp(user, binding.passwordEditText.text.toString())
    }
    
    private fun validateForm(): Boolean {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val address = binding.addressEditText.text.toString().trim()
        val toastmastersId = binding.toastmastersIdEditText.text.toString().trim()
        val clubId = binding.clubIdEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()
        
        var isValid = true
        
        // Name validation
        if (name.length < 2) {
            binding.nameInputLayout.error = getString(R.string.error_name_too_short)
            isValid = false
        } else {
            binding.nameInputLayout.error = null
        }
        
        // Email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }
        
        // Phone validation (basic check)
        if (phone.length < 6) {
            binding.phoneInputLayout.error = getString(R.string.error_invalid_phone)
            isValid = false
        } else {
            binding.phoneInputLayout.error = null
        }
        
        // Address validation
        if (address.length < 5) {
            binding.addressInputLayout.error = getString(R.string.error_address_too_short)
            isValid = false
        } else {
            binding.addressInputLayout.error = null
        }
        
        // Gender validation
        if (selectedGender.isNullOrEmpty()) {
            binding.genderAutoCompleteTextView.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.genderAutoCompleteTextView.error = null
        }
        
        // Joined date validation
        if (binding.joinedDateEditText.text.isNullOrEmpty()) {
            binding.joinedDateInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.joinedDateInputLayout.error = null
        }
        
        // Toastmasters ID validation
        if (toastmastersId.length < 4) {
            binding.toastmastersIdInputLayout.error = getString(R.string.error_invalid_toastmasters_id)
            isValid = false
        } else {
            binding.toastmastersIdInputLayout.error = null
        }
        
        // Club ID validation
        if (clubId.length < 2) {
            binding.clubIdInputLayout.error = getString(R.string.error_invalid_club_id)
            isValid = false
        } else {
            binding.clubIdInputLayout.error = null
        }
        
        // Password validation
        if (password.length < 6) {
            binding.passwordInputLayout.error = getString(R.string.error_password_too_short)
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }
        
        // Confirm password validation
        if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = getString(R.string.error_passwords_dont_match)
            isValid = false
        } else {
            binding.confirmPasswordInputLayout.error = null
        }
        
        // Mentor validation (if has mentor is selected)
        if (binding.hasMentorSwitch.isChecked && selectedMentorId.isNullOrEmpty()) {
            binding.mentorInputLayout.error = getString(R.string.error_select_mentor)
            isValid = false
        } else {
            binding.mentorInputLayout.error = null
        }
        
        binding.signUpButton.isEnabled = isValid
        return isValid
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.signUpButton.isEnabled = !show
        binding.loginButton.isEnabled = !show
    }
    
    private fun showSuccessMessage(userRole: UserRole, requiresApproval: Boolean) {
        if (requiresApproval) {
            // Don't navigate, just show message and stay on signup screen
            UiUtils.showSuccessMessage(binding.root, getString(R.string.signup_success_pending_approval))
            // Save user role
            preferenceManager.saveUserRole(userRole)
        } else {
            // Save user role
            preferenceManager.saveUserRole(userRole)
            
            // Navigate based on user role
            when (userRole) {
                UserRole.VP_EDUCATION -> {
                    findNavController().navigate(R.id.action_login_to_vp_nav_graph)
                }
                UserRole.MEMBER -> {
                    findNavController().navigate(R.id.action_login_to_member_nav_graph)
                }
                else -> {
                    // Default to member navigation for any other roles
                    findNavController().navigate(R.id.action_login_to_member_nav_graph)
                }
            }
            
            // Finish the activity to prevent going back to signup
            requireActivity().finish()
        }
    }
    
    private fun showErrorSnackbar(message: String) {
        UiUtils.showErrorWithRetry(binding.root, message)
    }
    
    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        view?.let { v ->
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
