package com.bntsoft.toastmasters.presentation.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentNotificationsBinding
import com.bntsoft.toastmasters.utils.DateTimeUtils
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by viewModels()

    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load notifications when the view is created
        viewModel.loadNotifications()
    }
    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onNotificationClick = { notification ->
                // Handle notification click
                if (!notification.isRead) {
                    viewModel.markNotificationAsRead(notification.id)
                }
                // TODO: Handle navigation based on notification type
            },
            onDeleteClick = { notification ->
                showDeleteConfirmation(notification.id)
            }
        )
        
        binding.recyclerView.apply {
            adapter = notificationAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshNotifications()
        }
    }



    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect UI state
                viewModel.uiState.collectLatest { state ->
                    // Update loading state
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    // Update swipe refresh state
                    binding.swipeRefreshLayout.isRefreshing = state.isRefreshing

                    // Update notifications list
                    notificationAdapter.submitList(state.notifications)
                    
                    // Show empty state if no notifications
                    if (state.notifications.isEmpty() && !state.isLoading) {
                        binding.emptyState.root.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.root.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                    
                    // Show error message if any
                    state.error?.let { error ->
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry) { viewModel.loadNotifications() }
                            .show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation(notificationId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_notification_title)
            .setMessage(R.string.confirm_delete_notification_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> viewModel.deleteNotification(notificationId) }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    private fun showDeleteAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_all_title)
            .setMessage(R.string.confirm_delete_all_notifications_message)
            .setPositiveButton(android.R.string.yes) { _, _ -> viewModel.deleteAllNotifications() }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = NotificationFragment()
    }
}
