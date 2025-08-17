package com.bntsoft.toastmasters.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

object UiUtils {

    fun showSnackbar(
        view: View,
        message: String,
        @StringRes actionText: Int = 0,
        action: (() -> Unit)? = null,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        val snackbar = Snackbar.make(view, message, duration)

        if (actionText != 0 && action != null) {
            snackbar.setAction(actionText) { action.invoke() }
        }

        snackbar.show()
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun setLoadingState(
        loadingView: View,
        contentView: View,
        isLoading: Boolean,
        message: String? = null
    ) {
        loadingView.isVisible = isLoading
        contentView.isEnabled = !isLoading
        contentView.alpha = if (isLoading) 0.5f else 1f

        // If the loading view has a TextView for the message, update it
        val messageView = loadingView.rootView.findViewById<View>(R.id.progressBar)
        if (messageView is android.widget.TextView && message != null) {
            messageView.text = message
        }
    }

    fun showErrorWithRetry(
        view: View,
        message: String,
        retryAction: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(
            view,
            message,
            if (retryAction != null) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
        )

        if (retryAction != null) {
            snackbar.setAction(R.string.retry) { retryAction.invoke() }
            snackbar.duration = Snackbar.LENGTH_INDEFINITE
        }

        snackbar.setBackgroundTint(
            ContextCompat.getColor(view.context, R.color.error)
        )

        snackbar.show()
    }

    fun showSuccessMessage(view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(
            ContextCompat.getColor(view.context, R.color.success)
        )
        snackbar.show()
    }

    fun showWarningMessage(view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(
            ContextCompat.getColor(
                view.context,
                R.color.teal_700
            ) // Using teal_700 as warning color
        )
        snackbar.show()
    }

    fun showDialog(
        context: Context,
        title: String? = null,
        message: String? = null,
        @StringRes positiveButtonText: Int = android.R.string.ok,
        @StringRes negativeButtonText: Int? = null,
        @StringRes neutralButtonText: Int? = null,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null,
        onNeutralClick: (() -> Unit)? = null,
        cancelable: Boolean = true,
        @StyleRes style: Int = R.style.Theme_ToastMasters
    ): androidx.appcompat.app.AlertDialog {
        val builder = MaterialAlertDialogBuilder(context, style)
            .setCancelable(cancelable)

        title?.let { builder.setTitle(it) }
        message?.let { builder.setMessage(it) }

        builder.setPositiveButton(positiveButtonText) { dialog, _ ->
            onPositiveClick?.invoke()
            dialog.dismiss()
        }

        negativeButtonText?.let { textRes ->
            builder.setNegativeButton(textRes) { dialog, _ ->
                onNegativeClick?.invoke()
                dialog.dismiss()
            }
        }

        neutralButtonText?.let { textRes ->
            builder.setNeutralButton(textRes) { dialog, _ ->
                onNeutralClick?.invoke()
                dialog.dismiss()
            }
        }

        return builder.show()
    }

    fun Fragment.showDialog(
        title: String? = null,
        message: String? = null,
        @StringRes positiveButtonText: Int = android.R.string.ok,
        @StringRes negativeButtonText: Int? = null,
        @StringRes neutralButtonText: Int? = null,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null,
        onNeutralClick: (() -> Unit)? = null,
        cancelable: Boolean = true,
        @StyleRes style: Int = R.style.Theme_ToastMasters
    ): androidx.appcompat.app.AlertDialog? {
        return context?.let { ctx ->
            showDialog(
                context = ctx,
                title = title,
                message = message,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                neutralButtonText = neutralButtonText,
                onPositiveClick = onPositiveClick,
                onNegativeClick = onNegativeClick,
                onNeutralClick = onNeutralClick,
                cancelable = cancelable,
                style = style
            )
        }
    }
}
