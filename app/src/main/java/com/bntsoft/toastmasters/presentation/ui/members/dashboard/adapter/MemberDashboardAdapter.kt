package com.bntsoft.toastmasters.presentation.ui.members.dashboard.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.SpeakerDetails
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.databinding.DialogSpeakerDetailsBinding
import com.bntsoft.toastmasters.databinding.DialogGrammarianDetailsBinding
import com.bntsoft.toastmasters.databinding.ItemMemberAssignedRoleBinding
import com.bntsoft.toastmasters.domain.model.MeetingWithRole
import com.bntsoft.toastmasters.presentation.ui.members.dashboard.MemberDashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MemberDashboardAdapter(
    private val viewModel: MemberDashboardViewModel,
    private val currentUserId: String,
    private val onMeetingClick: (String) -> Unit
) : ListAdapter<MeetingWithRole, MemberDashboardAdapter.MeetingWithRoleViewHolder>(
    MeetingWithRoleDiffCallback()
) {
    private val TAG = "MeetingRoleAdapter"

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingWithRoleViewHolder {
        Log.d(TAG, "Creating new view holder")
        return try {
            val binding = ItemMemberAssignedRoleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            MeetingWithRoleViewHolder(binding)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view holder", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: MeetingWithRoleViewHolder, position: Int) {
        Log.d(TAG, "Binding view at position $position")
        try {
            val item = getItem(position)
            Log.d(TAG, "Binding item: ${item.meeting.theme} (ID: ${item.meeting.id})")
            holder.bind(item)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding view at position $position", e)
        }
    }

    inner class MeetingWithRoleViewHolder(
        private val binding: ItemMemberAssignedRoleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MeetingWithRole) {
            with(binding) {
                // Set meeting theme/title
                tvMeetingTheme.text = item.meeting.theme.ifEmpty { "No Theme" }

                // Set venue/location
                tvMeetingVenue.text = item.meeting.location.ifEmpty { "Location not specified" }

                // Set day of week
                val dayOfWeek = item.meeting.dateTime.dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale.getDefault())
                tvMeetingDay.text = dayOfWeek

                // Set date
                tvMeetingDate.text = item.meeting.dateTime.format(dateFormatter)

                // Set time - handle both dateTime and endDateTime
                val startTime = item.meeting.dateTime.format(timeFormatter)
                val endTime = item.meeting.endDateTime?.format(timeFormatter)
                    ?: item.meeting.dateTime.plusHours(2).format(timeFormatter)
                tvMeetingTime.text = "$startTime - $endTime"

                // Set assigned roles with labels
                val rolesText = if (item.assignedRoles.isNotEmpty()) {
                    "Assigned roles: ${item.assignedRoles.joinToString(", ")}"
                } else {
                    "Assigned role: ${item.assignedRole}" // Fallback to single role if list is empty
                }
                tvMeetingRole.text = rolesText

                // Show Fill Details button only if user is assigned as a Speaker
                val isSpeaker = item.assignedRoles.any { role ->
                    role.contains("Speaker", ignoreCase = true)
                } || item.assignedRole.contains("Speaker", ignoreCase = true)

                // Show Fill Details button if user is assigned as a Grammarian
                val isGrammarian = item.assignedRoles.any { role ->
                    role.contains("Grammarian", ignoreCase = true)
                } || item.assignedRole.contains("Grammarian", ignoreCase = true)

                if (isSpeaker || isGrammarian) {
                    btnFillDetails.visibility = View.VISIBLE
                    btnFillDetails.setOnClickListener {
                        if (isSpeaker) {
                            showSpeakerDetailsDialog(binding.root.context, item.meeting.id)
                        } else if (isGrammarian) {
                            showGrammarianDetailsDialog(binding.root.context, item.meeting.id)
                        }
                    }

                    // Load existing details if any
                    if (isSpeaker) {
                        viewModel.loadSpeakerDetails(item.meeting.id, currentUserId)
                    }
                    if (isGrammarian) {
                        viewModel.loadGrammarianDetails(item.meeting.id, currentUserId)
                    }
                } else {
                    btnFillDetails.visibility = View.GONE
                }

                // Hide the assign role button as it's not needed in the dashboard
                btnAssignRole.visibility = View.GONE

                root.setOnClickListener {
                    onMeetingClick(item.meeting.id)
                }
            }
        }
    }

    private fun showSpeakerDetailsDialog(context: Context, meetingId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Inflate the dialog layout
        val dialogBinding = DialogSpeakerDetailsBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        // Pre-fill existing speaker details if available
        val existingDetails = viewModel.speakerDetailsState.value.speakerDetails[currentUser.uid]
        existingDetails?.let { details ->
            dialogBinding.apply {
                etName.setText(details.name)
                etPathwaysTrack.setText(details.pathwaysTrack)
                etLevel.setText(details.level.toString())
                etProjectNumber.setText(details.projectNumber.toString())
                etProjectTitle.setText(details.projectTitle)
                etSpeechTime.setText(details.speechTime)
                etSpeechTitle.setText(details.speechTitle)
                etSpeechObjectives.setText(details.speechObjectives)
            }
        }

        // Create AlertDialog
        val dialog = AlertDialog.Builder(context)
            .setTitle("Speaker Details")
            .setView(dialogBinding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        // Override Save button click to prevent auto-dismiss on validation failure
        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                // Validate required fields
                if (dialogBinding.etName.text.isNullOrBlank()) {
                    dialogBinding.etName.error = "Name is required"
                    return@setOnClickListener
                }

                // Safely parse values
                val level = dialogBinding.etLevel.text.toString().toIntOrNull() ?: 1
                val speechTime = dialogBinding.etSpeechTime.text.toString().ifEmpty { "5" }

                // Prepare SpeakerDetails object
                val speakerDetails = SpeakerDetails(
                    userId = currentUser.uid,
                    meetingId = meetingId,
                    name = dialogBinding.etName.text.toString().trim(),
                    pathwaysTrack = dialogBinding.etPathwaysTrack.text.toString().trim(),
                    level = level,
                    projectNumber = dialogBinding.etProjectNumber.text.toString().trim(),
                    projectTitle = dialogBinding.etProjectTitle.text.toString().trim(),
                    speechTime = speechTime,
                    speechTitle = dialogBinding.etSpeechTitle.text.toString().trim(),
                    speechObjectives = dialogBinding.etSpeechObjectives.text.toString().trim(),
                    timestamp = System.currentTimeMillis()
                )

                // Save details via ViewModel
                viewModel.saveSpeakerDetails(meetingId, currentUser.uid, speakerDetails)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showGrammarianDetailsDialog(context: Context, meetingId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val dialogBinding = DialogGrammarianDetailsBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        val existing = viewModel.grammarianDetailsState.value.grammarianDetails[currentUser.uid]
        existing?.let { details ->
            dialogBinding.apply {
                etWordOfTheDay.setText(details.wordOfTheDay)
                etWordMeaning.setText(details.wordMeaning.joinToString("\n"))
                etWordExamples.setText(details.wordExamples.joinToString("\n"))
                etIdiomOfTheDay.setText(details.idiomOfTheDay)
                etIdiomMeaning.setText(details.idiomMeaning)
                etIdiomExamples.setText(details.idiomExamples.joinToString("\n"))
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Grammarian Details")
            .setView(dialogBinding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                if (dialogBinding.etWordOfTheDay.text.isNullOrBlank()) {
                    dialogBinding.etWordOfTheDay.error = "WOD is required"
                    return@setOnClickListener
                }

                val wordMeaning = dialogBinding.etWordMeaning.text.toString()
                    .lines().map { it.trim() }.filter { it.isNotEmpty() }
                val wordExamples = dialogBinding.etWordExamples.text.toString()
                    .lines().map { it.trim() }.filter { it.isNotEmpty() }
                val idiomExamples = dialogBinding.etIdiomExamples.text.toString()
                    .lines().map { it.trim() }.filter { it.isNotEmpty() }

                val details = GrammarianDetails(
                    meetingID = meetingId,
                    userId = currentUser.uid,
                    wordOfTheDay = dialogBinding.etWordOfTheDay.text.toString().trim(),
                    wordMeaning = wordMeaning,
                    wordExamples = wordExamples,
                    idiomOfTheDay = dialogBinding.etIdiomOfTheDay.text.toString().trim(),
                    idiomMeaning = dialogBinding.etIdiomMeaning.text.toString().trim(),
                    idiomExamples = idiomExamples
                )

                viewModel.saveGrammarianDetails(meetingId, currentUser.uid, details)
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}

class MeetingWithRoleDiffCallback : DiffUtil.ItemCallback<MeetingWithRole>() {
    private val TAG = "DiffCallback"

    override fun areItemsTheSame(oldItem: MeetingWithRole, newItem: MeetingWithRole): Boolean {
        val isSame = oldItem.meeting.id == newItem.meeting.id
        Log.d(TAG, "areItemsTheSame: $isSame (${oldItem.meeting.id} vs ${newItem.meeting.id})")
        return isSame
    }

    override fun areContentsTheSame(
        oldItem: MeetingWithRole,
        newItem: MeetingWithRole
    ): Boolean {
        val isSame = oldItem.meeting.id == newItem.meeting.id &&
                oldItem.assignedRoles.containsAll(newItem.assignedRoles) &&
                newItem.assignedRoles.containsAll(oldItem.assignedRoles)

        if (!isSame) {
            Log.d(TAG, "Contents changed for item ${oldItem.meeting.id}")
        }
        return isSame
    }
}

