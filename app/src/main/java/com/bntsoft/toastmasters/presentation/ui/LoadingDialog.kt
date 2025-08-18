package com.bntsoft.toastmasters.presentation.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.bntsoft.toastmasters.databinding.DialogLoadingBinding

class LoadingDialog(context: Context) : Dialog(context) {

    private var _binding: DialogLoadingBinding? = null
    private val binding get() = _binding!!

    private var message: String = ""
    private var isCancelableDialog: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        _binding = DialogLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set dialog properties
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        setCancelable(isCancelableDialog)
        setCanceledOnTouchOutside(isCancelableDialog)

        // Set message if provided
        if (message.isNotEmpty()) {
            binding.tvMessage.text = message
            binding.tvMessage.visibility = View.VISIBLE
        } else {
            binding.tvMessage.visibility = View.GONE
        }
    }

    fun setMessage(message: String): LoadingDialog {
        this.message = message
        binding.tvMessage.text = message
        binding.tvMessage.visibility =
            if (message.isNotEmpty()) View.VISIBLE else View.GONE
        return this
    }

    override fun setCancelable(cancelable: Boolean) {
        this.isCancelableDialog = cancelable
        super.setCancelable(cancelable)
    }

    companion object {

        fun create(
            context: Context,
            message: String = "",
            cancelable: Boolean = false
        ): LoadingDialog {
            return LoadingDialog(context).apply {
                this.message = message
                this.isCancelableDialog = cancelable
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _binding = null
    }
}