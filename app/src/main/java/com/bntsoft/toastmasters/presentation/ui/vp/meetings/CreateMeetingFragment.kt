package com.bntsoft.toastmasters.presentation.ui.vp.meetings

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.Meeting
import com.bntsoft.toastmasters.databinding.FragmentCreateMeetingBinding
import com.bntsoft.toastmasters.databinding.ItemMeetingFormBinding
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.CreateMeetingState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel.MeetingsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateMeetingFragment : Fragment() {

    private val viewModel: MeetingsViewModel by viewModels()

    private var _binding: FragmentCreateMeetingBinding? = null
    private val binding get() = _binding!!

    private val meetingForms = mutableListOf<MeetingFormData>()

    private val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMeetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the first meeting form
        addNewMeetingForm()

        // Set click listener for "Add Another Meeting" button with smooth scroll
        binding.addAnotherMeetingButton.setOnClickListener {
            if (meetingForms.isNotEmpty()) {
                // Get the last form's roles
                val lastForm = meetingForms.last()
                val roles = getRolesFromForm(lastForm.binding)

                if (roles.isNotEmpty()) {
                    // Show confirmation dialog with Material 3 style
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                    )
                        .setTitle("Copy Preferred Roles?")
                        .setMessage("Do you want to use the same preferred roles as the above meeting?")
                        .setPositiveButton("Yes") { _, _ ->
                            addNewMeetingForm(roles)
                            scrollToBottom()
                        }
                        .setNegativeButton("No") { _, _ ->
                            addNewMeetingForm()
                            scrollToBottom()
                        }
                        .show()
                } else {
                    addNewMeetingForm()
                    scrollToBottom()
                }
            } else {
                addNewMeetingForm()
                scrollToBottom()
            }
        }

        // Set click listener for "Create Meeting(s)" button
        binding.createMeetingsButton.setOnClickListener {
            if (validateForms()) {
                createMeetings()
            } else {
                showError("Please fill all fields")
            }
        }

        lifecycleScope.launch {
            viewModel.createMeetingState.collect { state ->
                when (state) {
                    is CreateMeetingState.Success -> {
                        showError("Meeting '${state.meeting.theme}' created successfully")
                        // Clear the form for new entry
                        binding.meetingFormsContainer.removeAllViews()
                        meetingForms.clear()
                        addNewMeetingForm()
                        viewModel.resetCreateMeetingState()
                    }

                    is CreateMeetingState.Duplicate -> {
                        showDuplicateMeetingDialog(state.meeting)
                        viewModel.resetCreateMeetingState()
                    }

                    is CreateMeetingState.Error -> {
                        showError(state.message)
                        viewModel.resetCreateMeetingState()
                    }

                    is CreateMeetingState.Loading -> {
                        // Show a loading indicator if you have one
                    }

                    is CreateMeetingState.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun getRolesFromForm(formBinding: ItemMeetingFormBinding): List<Pair<String, Int>> {
        val roles = mutableListOf<Pair<String, Int>>()
        val roleCount = mutableMapOf<String, Int>()

        // Count occurrences of each role
        for (i in 0 until formBinding.roleChipGroup.childCount) {
            val chip = formBinding.roleChipGroup.getChildAt(i) as? Chip ?: continue
            chip?.let {
                val roleText = it.text.toString()
                val roleName = roleText.substringBeforeLast(' ').trim()
                roleCount[roleName] = (roleCount[roleName] ?: 0) + 1
            }
        }

        // Convert to list of pairs
        roleCount.forEach { (role, count) ->
            roles.add(Pair(role, count))
        }

        return roles
    }

    private fun addNewMeetingForm(rolesToCopy: List<Pair<String, Int>> = emptyList()) {
        val formBinding =
            ItemMeetingFormBinding.inflate(layoutInflater, binding.meetingFormsContainer, false)

        // Initialize with default values
        val calendar = Calendar.getInstance()
        val startTime = Calendar.getInstance()
        val endTime = Calendar.getInstance()

        // Calculate the base date (next Saturday after last meeting or today)
        viewLifecycleOwner.lifecycleScope.launch {
            val lastMeetingDate = viewModel.getLastMeetingDate()
            val baseCalendar = lastMeetingDate?.let { date ->
                // Start from the last meeting date
                Calendar.getInstance().apply { time = date }
            } ?: Calendar.getInstance() // No meetings exist, start from today

            // Move to the next Saturday from the base date
            val dayOfWeek = baseCalendar.get(Calendar.DAY_OF_WEEK)
            val daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
            baseCalendar.add(
                Calendar.DAY_OF_MONTH,
                if (daysUntilSaturday == 0) 7 else daysUntilSaturday
            )

            // For subsequent forms, add 7 days for each form after the first one
            val formIndex = meetingForms.size
            if (formIndex > 0) {
                baseCalendar.add(Calendar.DAY_OF_MONTH, 7 * formIndex)
            }

            // Update the working calendar
            calendar.time = baseCalendar.time

            // Set the times
            startTime.time = calendar.time
            startTime.set(Calendar.HOUR_OF_DAY, 17)
            startTime.set(Calendar.MINUTE, 30)
            startTime.set(Calendar.SECOND, 0)
            startTime.set(Calendar.MILLISECOND, 0)

            endTime.time = calendar.time
            endTime.set(Calendar.HOUR_OF_DAY, 19)
            endTime.set(Calendar.MINUTE, 30)
            endTime.set(Calendar.SECOND, 0)
            endTime.set(Calendar.MILLISECOND, 0)

            // Update the UI with the calculated date and times
            formBinding.apply {
                meetingDateInput.setText(dateFormat.format(calendar.time))
                startTimeInput.setText(timeFormat.format(startTime.time))
                endTimeInput.setText(timeFormat.format(endTime.time))
            }

            // Store the calendar instances in the form data
            val meetingFormData = MeetingFormData(formBinding, startTime, endTime)
            meetingForms.add(meetingFormData)

            binding.meetingFormsContainer.addView(formBinding.root)
            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }

        formBinding.apply {

            // Setup date picker
            meetingDateInput.setOnClickListener {
                // Find the meeting form data for this binding
                val formData = meetingForms.find { it.binding === formBinding }
                formData?.let {
                    showDatePicker(it.startCalendar, meetingDateInput)
                }
            }

            // Setup time pickers
            startTimeInput.setOnClickListener {
                val formData = meetingForms.find { it.binding === formBinding }
                formData?.let {
                    showTimePicker(it.startCalendar, startTimeInput)
                }
            }

            endTimeInput.setOnClickListener {
                val formData = meetingForms.find { it.binding === formBinding }
                formData?.let {
                    showTimePicker(it.endCalendar, endTimeInput)
                }
            }

            // Remove button visibility and action
            if (meetingForms.isEmpty()) {
                removeMeetingButton.visibility = View.GONE
            } else {
                removeMeetingButton.visibility = View.VISIBLE
                removeMeetingButton.setOnClickListener {
                    val formData = meetingForms.find { it.binding === formBinding }
                    formData?.let {
                        binding.meetingFormsContainer.removeView(root)
                        meetingForms.remove(it)
                    }
                }
            }

            // Add roles if copying from previous form
            if (rolesToCopy.isNotEmpty()) {
                rolesToCopy.forEach { (role, count) ->
                    for (i in 1..count) {
                        val chip = Chip(context).apply {
                            text = "$role $i"
                            isCloseIconVisible = true
                            setOnCloseIconClickListener {
                                formBinding.roleChipGroup.removeView(this)
                            }
                            tag = role
                            setChipBackgroundColorResource(R.color.chip_background)
                            setTextAppearance(R.style.ChipTextAppearance)
                        }
                        formBinding.roleChipGroup.addView(chip)
                    }
                }
            }

            setupFormListeners(formBinding)
        }

        // Scroll to the bottom
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun setupFormListeners(binding: ItemMeetingFormBinding) {
        // Set up date picker with ripple effect
        binding.meetingDateInput.apply {
            setOnClickListener {
                val calendar = Calendar.getInstance()
                showDatePicker(calendar, binding.meetingDateInput)
            }
            background = null
        }

        // Set up time pickers with ripple effect
        binding.startTimeInput.apply {
            setOnClickListener {
                val calendar = Calendar.getInstance()
                showTimePicker(calendar, binding.startTimeInput)
            }
            background = null
        }

        binding.endTimeInput.apply {
            setOnClickListener {
                val calendar = Calendar.getInstance()
                showTimePicker(calendar, binding.endTimeInput)
            }
            background = null
        }

        // Set up role dropdown with Material 3 style
        setupRoleDropdown(binding)

        // Set up remove button with confirmation dialog
        binding.removeMeetingButton.setOnClickListener {
            if (meetingForms.size > 1) {
                val position = meetingForms.indexOfFirst { it.binding === binding }
                if (position != -1) {
                    showRemoveConfirmationDialog(binding, position)
                }
            } else {
                // Show a more user-friendly message
                Snackbar.make(
                    requireView(),
                    "You must have at least one meeting form",
                    Snackbar.LENGTH_SHORT
                ).setBackgroundTint(
                    requireContext().getColor(com.google.android.material.R.color.design_default_color_error)
                ).show()
            }
        }
    }

    private fun setupRoleDropdown(binding: ItemMeetingFormBinding) {
        val roles = resources.getStringArray(R.array.Preferred_roles).toMutableList()
        roles.add(0, "+ Add new role") // Add at top

        // Custom adapter to style "+ Add new role"
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            com.google.android.material.R.layout.mtrl_auto_complete_simple_item,
            roles
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                if (position == 0) {
                    // Style for "+ Add new role"
                    view.setTypeface(view.typeface, Typeface.BOLD)
                    view.setTextColor(ContextCompat.getColor(context, R.color.primary))
                } else {
                    // Normal style for other items
                    view.setTypeface(view.typeface, Typeface.NORMAL)
                    view.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) {
                    view.setTypeface(view.typeface, Typeface.BOLD)
                    view.setTextColor(ContextCompat.getColor(context, R.color.primary))
                } else {
                    view.setTypeface(view.typeface, Typeface.NORMAL)
                    view.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                }
                return view
            }
        }

        binding.apply {
            val dropdown = roleDropdown as MaterialAutoCompleteTextView
            dropdown.setAdapter(adapter)
            dropdown.keyListener = null
            roleDropdownLayout.isFocusable = false
            roleDropdownLayout.isFocusableInTouchMode = false

            dropdown.setOnClickListener {
                if (adapter.count > 0) dropdown.showDropDown()
            }

            dropdown.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && adapter.count > 0) dropdown.showDropDown()
            }

            dropdown.setOnItemClickListener { _, _, position, _ ->
                val selectedRole = adapter.getItem(position) ?: return@setOnItemClickListener

                if (position == 0) { // "+ Add new role"
                    roleInputLayout.visibility = View.VISIBLE
                    saveRoleButton.visibility = View.VISIBLE
                    roleInput.requestFocus()

                    // Show keyboard
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(roleInput, InputMethodManager.SHOW_IMPLICIT)

                    dropdown.setText("", false)
                } else {
                    if (selectedRole.equals("Toastmaster of the Day", ignoreCase = true)) {
                        showTmodConfirmationDialog(binding, selectedRole)
                    } else {
                        showRoleCountDialog(binding, selectedRole)
                    }
                    dropdown.setText("", false)
                }
                dropdown.clearFocus()
            }

            saveRoleButton.setOnClickListener {
                val role = roleInput.text.toString().trim()
                if (role.isNotEmpty()) {
                    showRoleCountDialog(binding, role)
                    roleInput.text?.clear()
                    roleInputLayout.visibility = View.GONE
                    saveRoleButton.visibility = View.GONE

                    // Hide keyboard
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(roleInput.windowToken, 0)
                }
            }

            roleInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    val role = roleInput.text.toString().trim()
                    if (role.isNotEmpty()) {
                        showRoleCountDialog(binding, role)
                        roleInput.text?.clear()
                        roleInputLayout.visibility = View.GONE
                        saveRoleButton.visibility = View.GONE

                        // Hide keyboard
                        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(roleInput.windowToken, 0)
                    }
                    true
                } else false
            }
        }
    }

    private fun showRoleCountDialog(binding: ItemMeetingFormBinding, role: String) {
        if (role.equals("Toastmaster of the Day", ignoreCase = true)) {
            addRoleChip(binding, role, isTmod = true)
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_role_count, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.roleCountInput).apply {
            setText("1")
            setSelectAllOnFocus(true)
            requestFocus()
        }

        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.roleCountLayout)
        textInputLayout.hint = "Number of $role"

        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_ToastMasters_Dialog)
            .setTitle("Add $role")
            .setView(dialogView)
            .setPositiveButton("Add") { d, _ ->
                val count = input.text.toString().toIntOrNull() ?: 0
                if (count > 0) {
                    addRoleChip(binding, role, isTmod = false, count = count)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        // Show keyboard
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()

        // Input filter 1-99
        input.filters = arrayOf(InputFilter.LengthFilter(2), InputFilter { source, _, _, _, _, _ ->
            if (source.isEmpty()) return@InputFilter null
            if (source.matches("[0-9]+".toRegex())) {
                val value = source.toString().toInt()
                if (value in 1..99) null else ""
            } else ""
        })
    }


    private fun addRoleChip(
        binding: ItemMeetingFormBinding,
        role: String,
        isTmod: Boolean = false,
        count: Int = 1
    ) {
        if (isTmod || role.equals("Toastmaster of the Day", ignoreCase = true)) {
            // TMOD / Toastmaster of the Day
            val chip = Chip(requireContext()).apply {
                text = role
                isCloseIconVisible = true
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                chipBackgroundColor = requireContext().getColorStateList(R.color.chip_background)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                tag = role // TMOD uses exact role name
                setOnCloseIconClickListener {
                    binding.roleChipGroup.removeView(this)
                    if (isTmod) {
                        val formData = meetingForms.find { it.binding === binding }
                        formData?.let { data ->
                            data.selectedTmodId = null
                            data.selectedTmodName = null
                        }
                    }
                }
            }
            binding.roleChipGroup.addView(chip)
            return
        }

        // Non-TMOD roles, possibly multiple chips
        val startIndex = binding.roleChipGroup.children.count {
            val chip = it as? Chip
            chip != null && chip.tag == role
        } + 1

        for (i in 0 until count) {
            val chipText = "$role ${startIndex + i}"
            val chip = Chip(requireContext()).apply {
                text = chipText
                isCloseIconVisible = true
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                chipBackgroundColor = requireContext().getColorStateList(R.color.chip_background)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                tag = role // keep tag as base role for counting
                setOnCloseIconClickListener {
                    binding.roleChipGroup.removeView(this)
                }
            }
            binding.roleChipGroup.addView(chip)
        }
    }

    fun Float.dpToPx(): Float = this * Resources.getSystem().displayMetrics.density

    private fun showTmodConfirmationDialog(
        formBinding: ItemMeetingFormBinding,
        selectedRole: String
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm TMOD")
            .setMessage("Is the Toastmaster of the Day already assigned?")
            .setPositiveButton("Yes") { _, _ ->
                fetchMembersAndShowDialog(formBinding, selectedRole)
            }
            .setNegativeButton("No") { dialog, _ ->
                // If No → still add chip, but you could also trigger a flow to assign later
                addRoleChip(formBinding, selectedRole)
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun fetchMembersAndShowDialog(
        formBinding: ItemMeetingFormBinding,
        selectedRole: String
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("role", "MEMBER")
            .get()
            .addOnSuccessListener { snapshot ->
                val members = snapshot.documents.mapNotNull { it.getString("name") }
                if (members.isNotEmpty()) {
                    showMemberSelectionDialog(formBinding, selectedRole, members)
                } else {
                    Toast.makeText(requireContext(), "No members found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    data class MeetingFormData(
        val binding: ItemMeetingFormBinding,
        val startCalendar: Calendar,
        val endCalendar: Calendar,
        var selectedTmodId: String? = null,
        var selectedTmodName: String? = null,
        var meetingId: String? = null
    )

    private fun showMemberSelectionDialog(
        formBinding: ItemMeetingFormBinding,
        selectedRole: String,
        members: List<String>
    ) {
        val formData = meetingForms.find { it.binding === formBinding } ?: return
        var selectedMember: String? = null
        var selectedMemberId: String? = null

        val db = FirebaseFirestore.getInstance()

        AlertDialog.Builder(requireContext())
            .setTitle("Select TMOD")
            .setSingleChoiceItems(members.toTypedArray(), -1) { _, which ->
                selectedMember = members[which]
                // Get the user ID for the selected member
                db.collection("users")
                    .whereEqualTo("name", selectedMember)
                    .whereEqualTo("role", "MEMBER")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            selectedMemberId = snapshot.documents[0].id
                        }
                    }
            }
            .setPositiveButton("OK") { dialog, _ ->
                selectedMember?.let { name ->
                    // Update the form data with selected TMOD
                    formData.selectedTmodId = selectedMemberId
                    formData.selectedTmodName = name

                    // Only save if meeting is already created
                    formData.meetingId?.let { meetingId ->
                        saveTmodAssignment(
                            meetingId,
                            formData.selectedTmodId!!,
                            formData.selectedTmodName!!
                        )
                    }
                    // Update UI to show TMOD assignment
                    val roleWithName = "TMOD: $name"
                    addRoleChip(formBinding, roleWithName)

                    // Remove any existing TMOD chip
                    formBinding.roleChipGroup.children.forEach { view ->
                        if (view is Chip && view.text.startsWith("TMOD:")) {
                            formBinding.roleChipGroup.removeView(view)
                        }
                    }

                    // Add the new TMOD chip
                    val chip = Chip(requireContext()).apply {
                        text = roleWithName
                        isCloseIconVisible = true
                        setOnCloseIconClickListener {
                            formBinding.roleChipGroup.removeView(this)
                            formData.selectedTmodId = null
                            formData.selectedTmodName = null
                        }
                        setChipBackgroundColorResource(R.color.chip_background)
                        setTextAppearance(R.style.ChipTextAppearance)
                    }
                    formBinding.roleChipGroup.addView(chip)

                } ?: Toast.makeText(requireContext(), "No member selected!", Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scrollToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showRemoveConfirmationDialog(binding: ItemMeetingFormBinding, position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Meeting?")
            .setMessage("Are you sure you want to remove this meeting?")
            .setPositiveButton("Remove") { _, _ ->
                removeMeetingForm(binding, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeMeetingForm(formBinding: ItemMeetingFormBinding, position: Int) {
        if (meetingForms.size > 1 && position in meetingForms.indices) {
            // Remove the form from the container
            binding.meetingFormsContainer.removeView(formBinding.root)
            meetingForms.removeAt(position)
        } else {
            showError("At least one meeting form is required")
        }
    }


    private fun showError(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(
                    requireContext().getColor(com.google.android.material.R.color.design_default_color_error)
                )
                .show()
        }
    }

    private fun validateForms(): Boolean {
        if (meetingForms.isEmpty()) return false

        return meetingForms.all { formData ->
            formData.binding.meetingDateInput.text?.isNotBlank() == true &&
                    formData.binding.startTimeInput.text?.isNotBlank() == true &&
                    formData.binding.endTimeInput.text?.isNotBlank() == true &&
                    formData.binding.venueInput.text?.isNotBlank() == true
        }
    }

    private fun showDatePicker(calendar: Calendar, dateView: TextInputEditText) {
        val datePicker =
            com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(calendar.timeInMillis)
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            calendar.timeInMillis = selection
            dateView.setText(dateFormat.format(calendar.time))
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(calendar: Calendar, timeView: TextInputEditText) {
        val isSystem24Hour = android.text.format.DateFormat.is24HourFormat(requireContext())
        val clockFormat =
            if (isSystem24Hour) com.google.android.material.timepicker.TimeFormat.CLOCK_24H else com.google.android.material.timepicker.TimeFormat.CLOCK_12H

        val timePicker =
            com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Select time")
                .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            timeView.setText(timeFormat.format(calendar.time))
        }

        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun createMeetings(forceCreate: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Start collecting the state flow before creating any meetings
            viewModel.createMeetingState.collect { state ->
                Log.d("CreateMeeting", "Received state: ${state.javaClass.simpleName}")
                when (state) {
                    is CreateMeetingState.Success -> {
                        state.formData.meetingId = state.meeting.id
                        Log.d("CreateMeeting", "Meeting created with ID: ${state.meeting.id}")

                        val tmodId = state.formData.selectedTmodId
                        val tmodName = state.formData.selectedTmodName

                        if (!tmodId.isNullOrBlank() && !tmodName.isNullOrBlank()) {
                            Log.d(
                                "CreateMeeting",
                                "Attempting to assign TMOD: $tmodName ($tmodId) to meeting: ${state.meeting.id}"
                            )

                            // Save TMOD assignment and handle the result
                            saveTmodAssignment(state.meeting.id, tmodId, tmodName)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // TMOD assignment successful
                                        Log.d(
                                            "CreateMeeting",
                                            "TMOD assignment successful for meeting: ${state.meeting.id}"
                                        )
                                        activity?.runOnUiThread {
                                            Toast.makeText(
                                                context,
                                                "Meeting created and TMOD assigned successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        // Show error to user and provide retry option
                                        val error = task.exception?.message ?: "Unknown error"
                                        Log.e(
                                            "CreateMeeting",
                                            "TMOD assignment failed: $error",
                                            task.exception
                                        )

                                        activity?.runOnUiThread {
                                            android.app.AlertDialog.Builder(requireContext())
                                                .setTitle("TMOD Assignment Failed")
                                                .setMessage("Meeting was created, but failed to assign Toastmaster of the Day. Would you like to retry?")
                                                .setPositiveButton("Retry") { _, _ ->
                                                    // Retry TMOD assignment
                                                    saveTmodAssignment(
                                                        state.meeting.id,
                                                        tmodId,
                                                        tmodName
                                                    )
                                                        .addOnCompleteListener { retryTask ->
                                                            if (retryTask.isSuccessful) {
                                                                activity?.runOnUiThread {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "TMOD assigned successfully!",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            } else {
                                                                activity?.runOnUiThread {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Failed to assign TMOD. Please try again later.",
                                                                        Toast.LENGTH_LONG
                                                                    ).show()
                                                                }
                                                            }
                                                        }
                                                }
                                                .setNegativeButton("Cancel") { dialog, _ ->
                                                    dialog.dismiss()
                                                    Toast.makeText(
                                                        context,
                                                        "TMOD not assigned. You can assign it later.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                .show()
                                        }
                                    }
                                }
                        } else {
                            Log.d(
                                "CreateMeeting",
                                "No TMOD assigned for meeting: ${state.meeting.id}"
                            )
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "Meeting created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    is CreateMeetingState.Error -> {
                        activity?.runOnUiThread {
                            showError(state.message)
                        }
                    }

                    is CreateMeetingState.Duplicate -> {
                        activity?.runOnUiThread {
                            showDuplicateMeetingDialog(state.meeting)
                        }
                    }

                    is CreateMeetingState.Loading -> {
                        // Show loading state if needed
                        Log.d("CreateMeeting", "Loading...")
                    }

                    is CreateMeetingState.Idle -> {
                        // Handle idle state if needed
                        Log.d("CreateMeeting", "Idle state")
                    }
                }
            }
        }

        // Now create each meeting
        meetingForms.forEach { formData ->
            val theme = formData.binding.themeInput.text.toString()
            val venue = formData.binding.venueInput.text.toString()

            // Combine date from startCalendar with time from start and end calendars
            val startCal = formData.startCalendar
            val endCal = formData.endCalendar

            val finalStartCalendar = Calendar.getInstance().apply {
                time = startCal.time // This sets the date and time
            }

            val finalEndCalendar = Calendar.getInstance().apply {
                time = startCal.time // Start with the same date
                set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)) // Set hour from endCal
                set(Calendar.MINUTE, endCal.get(Calendar.MINUTE)) // Set minute from endCal
            }

            if (finalStartCalendar.after(finalEndCalendar)) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        context,
                        "Error: End time must be after start time.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@forEach
            }

            val startLocalDateTime =
                LocalDateTime.ofInstant(finalStartCalendar.toInstant(), ZoneId.systemDefault())
            val endLocalDateTime =
                LocalDateTime.ofInstant(finalEndCalendar.toInstant(), ZoneId.systemDefault())

            // Extract roles and their counts from chips
            val roleCounts = mutableMapOf<String, Int>()
            formData.binding.roleChipGroup.children.forEach { view ->
                val chip = view as? Chip ?: return@forEach
                val role = chip.tag?.toString() ?: return@forEach
                roleCounts[role] = roleCounts.getOrDefault(role, 0) + 1
            }

            // Convert LocalDateTime to Date for the data model
            val startDate = Date.from(startLocalDateTime.atZone(ZoneId.systemDefault()).toInstant())

            // Create meeting using data model with role information
            val meeting = Meeting(
                theme = theme,
                date = startDate,
                startTime = startLocalDateTime.toLocalTime().toString(),
                endTime = endLocalDateTime.toLocalTime().toString(),
                venue = venue,
                officers = emptyMap(), // Empty officers as they will be set by the ViewModel
                roleCounts = roleCounts, // Add role counts
                assignedCounts = roleCounts.mapValues { 0 } // Initialize assigned counts to 0
            )

            Log.d("CreateMeeting", "Calling viewModel.createMeeting()")
            viewModel.createMeeting(meeting, formData, forceCreate)
        }
    }

    private fun showDuplicateMeetingDialog(meeting: com.bntsoft.toastmasters.domain.model.Meeting) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Meeting already exists")
            .setMessage("A meeting is already scheduled on this date and time. Do you want to create it again?")
            .setPositiveButton("Yes") { _, _ ->
                createMeetings(forceCreate = true)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(
            "CreateMeeting",
            "onResume - Current createMeetingState: ${viewModel.createMeetingState}"
        )
    }

    private fun saveTmodAssignment(
        meetingId: String,
        userId: String,
        memberName: String
    ): com.google.android.gms.tasks.Task<Void> {
        Log.d(
            "CreateMeeting",
            "saveTmodAssignment called with meetingId: $meetingId, userId: $userId, memberName: $memberName"
        )

        if (meetingId.isBlank() || userId.isBlank()) {
            val errorMsg = "Invalid meetingId or userId - meetingId: $meetingId, userId: $userId"
            Log.e("CreateMeeting", errorMsg)
            return com.google.android.gms.tasks.Tasks.forException(IllegalArgumentException(errorMsg))
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val assignedRoleRef = firestore
                .collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .document(userId)

            val tmodAssignment = hashMapOf(
                "userId" to userId,
                "memberName" to memberName,
                "roles" to listOf("Toastmaster of the Day"),
                "assignedRole" to "Toastmaster of the Day",
                "assignedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            Log.d("CreateMeeting", "TMOD assignment data: $tmodAssignment")

            assignedRoleRef.set(tmodAssignment)
                .addOnSuccessListener {
                    Log.d(
                        "CreateMeeting",
                        "TMOD assignment saved successfully at meetings/$meetingId/assignedRole/$userId"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("CreateMeeting", "Failed to save TMOD assignment", e)
                    if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
                        Log.e(
                            "CreateMeeting",
                            "Firestore error - Code: ${e.code}, Message: ${e.message}"
                        )
                    }
                }

        } catch (e: Exception) {
            Log.e("CreateMeeting", "Error in saveTmodAssignment", e)
            com.google.android.gms.tasks.Tasks.forException(e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
