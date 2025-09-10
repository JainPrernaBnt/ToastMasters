package com.bntsoft.toastmasters.custom

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.bntsoft.toastmasters.R

class StepIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        // Apply default styles
        gravity = android.view.Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, android.R.color.black))
        textSize = 14f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        
        // Set default background
        setBackgroundResource(R.drawable.step_indicator_inactive)
        
        // Handle custom attributes
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StepIndicatorView,
            0, 0
        ).apply {
            try {
                val state = getInt(R.styleable.StepIndicatorView_stepState, 1) // Default to inactive
                setStepState(state)
            } finally {
                recycle()
            }
        }
    }
    
    fun setStepState(state: Int) {
        val backgroundRes = when (state) {
            0 -> R.drawable.step_indicator_active
            1 -> R.drawable.step_indicator_inactive
            2 -> R.drawable.step_indicator_completed
            else -> R.drawable.step_indicator_inactive
        }
        setBackgroundResource(backgroundRes)
    }
}
