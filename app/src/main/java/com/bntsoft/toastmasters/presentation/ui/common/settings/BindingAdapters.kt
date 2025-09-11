package com.bntsoft.toastmasters.presentation.ui.common.settings

import android.view.View
import androidx.databinding.BindingAdapter
import com.bntsoft.toastmasters.R
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("visibleIf")
fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("helperText")
fun MaterialTextView.setHelperText(text: String?) {
    this.text = text
}

@BindingAdapter("helperTextOrDefault")
fun MaterialTextView.setHelperTextWithDefault(text: String?) {
    this.text = if (text.isNullOrEmpty()) {
        context.getString(android.R.string.unknownName)
    } else {
        text
    }
}

@BindingAdapter("dateHelperText")
fun MaterialTextView.setDateHelperText(date: Date?) {
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    this.text = try {
        date?.let { dateFormat.format(it) } ?: context.getString(android.R.string.unknownName)
    } catch (e: Exception) {
        date?.toString() ?: context.getString(android.R.string.unknownName)
    }
}

@BindingAdapter("helperTextForRole")
fun MaterialTextView.setRoleHelperText(roleName: String?) {
    text = when (roleName) {
        "VP_EDUCATION" -> "VP Education"
        "MEMBER" -> "Member"
        null, "" -> context.getString(android.R.string.unknownName)
        else -> roleName.replace("_", " ") // fallback: make ENUM readable
    }
}

@BindingAdapter("helperTextForToastmastersId")
fun MaterialTextView.setToastmastersIdHelperText(tmId: String?) {
    val value = tmId?.ifEmpty { context.getString(android.R.string.unknownName) }
    text = "TM ID: $value"
}

@BindingAdapter("helperTextForClubId")
fun MaterialTextView.setClubIdHelperText(clubId: String?) {
    val value = clubId?.ifEmpty { context.getString(android.R.string.unknownName) }
    text = "Club ID: $value"
}

@BindingAdapter("textOrDefault")
fun MaterialTextView.setTextWithDefault(text: String?) {
    this.text = if (text.isNullOrEmpty()) {
        context.getString(android.R.string.unknownName)
    } else {
        text
    }
}

@BindingAdapter("hideIfLoading")
fun View.hideIfLoading(isLoading: Boolean) {
    visibility = if (isLoading) View.GONE else View.VISIBLE
}

@BindingAdapter("textOrLoading")
fun MaterialTextView.setTextOrLoading(isLoading: Boolean) {
    text = if (isLoading) {
        context.getString(R.string.loading)
    } else {
        text // Keep existing text
    }
}
