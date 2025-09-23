package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView

class SynchronizedHorizontalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private var scrollView: HorizontalScrollView? = null
    private var isSyncing = false

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        
        if (!isSyncing && scrollView != null) {
            isSyncing = true
            scrollView?.scrollTo(l, t)
            isSyncing = false
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        if (!isSyncing) {
            super.scrollTo(x, y)
        }
    }
}
