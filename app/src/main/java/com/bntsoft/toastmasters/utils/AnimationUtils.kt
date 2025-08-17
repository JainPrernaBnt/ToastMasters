package com.bntsoft.toastmasters.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.bntsoft.toastmasters.R

/**
 * Utility class for handling common animations.
 */
object AnimationUtils {

    /**
     * Fades in a view with the specified duration.
     * @param view The view to fade in
     * @param duration The duration of the animation in milliseconds (default: 300ms)
     * @param startDelay The delay before starting the animation in milliseconds (default: 0)
     * @param onEnd Optional callback when the animation ends
     */
    fun fadeIn(
        view: View,
        duration: Long = 300,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        if (view.visibility == View.VISIBLE && view.alpha == 1f) {
            onEnd?.invoke()
            return
        }

        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(startDelay)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })
        }
    }

    /**
     * Fades out a view with the specified duration.
     * @param view The view to fade out
     * @param duration The duration of the animation in milliseconds (default: 300ms)
     * @param startDelay The delay before starting the animation in milliseconds (default: 0)
     * @param onEnd Optional callback when the animation ends
     */
    fun fadeOut(
        view: View,
        duration: Long = 300,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        if (view.visibility == View.INVISIBLE || view.alpha == 0f) {
            onEnd?.invoke()
            return
        }

        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setStartDelay(startDelay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.INVISIBLE
                    onEnd?.invoke()
                }
            })
    }

    /**
     * Slides a view up from the bottom with a fade in effect.
     * @param view The view to slide up
     * @param duration The duration of the animation in milliseconds (default: 400ms)
     * @param startDelay The delay before starting the animation in milliseconds (default: 0)
     */
    fun slideUp(
        view: View,
        duration: Long = 400,
        startDelay: Long = 0
    ) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = height.toFloat()
            animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(startDelay)
                .setInterpolator(AccelerateDecelerateInterpolator())
        }
    }

    /**
     * Slides a view down and fades it out.
     * @param view The view to slide down
     * @param duration The duration of the animation in milliseconds (default: 300ms)
     * @param startDelay The delay before starting the animation in milliseconds (default: 0)
     * @param onEnd Optional callback when the animation ends
     */
    fun slideDown(
        view: View,
        duration: Long = 300,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setStartDelay(startDelay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
    }

    /**
     * Performs a scale and fade in animation on a view.
     * @param view The view to animate
     * @param duration The duration of the animation in milliseconds (default: 300ms)
     * @param startDelay The delay before starting the animation in milliseconds (default: 0)
     */
    fun scaleIn(
        view: View,
        duration: Long = 300,
        startDelay: Long = 0
    ) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setStartDelay(startDelay)
                .setInterpolator(AccelerateDecelerateInterpolator())
        }
    }

    /**
     * Performs a scale and fade out animation on a view.
     * @param view The view to animate
     * @param duration The duration of the animation in milliseconds (default: 200ms)
     * @param startDelay The delay before starting the animation in milliseconds (default: 0)
     * @param onEnd Optional callback when the animation ends
     */
    fun scaleOut(
        view: View,
        duration: Long = 200,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        view.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(duration)
            .setStartDelay(startDelay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
    }

    /**
     * Shakes a view horizontally to indicate an error.
     * @param view The view to shake
     * @param duration The duration of the animation in milliseconds (default: 400ms)
     */
    fun shake(view: View, duration: Long = 400) {
        val anim = android.view.animation.AnimationUtils.loadAnimation(
            view.context,
            R.anim.shake_horizontal
        )
        anim.duration = duration
        view.startAnimation(anim)
    }

    /**
     * Applies a bounce animation to a view.
     * @param view The view to animate
     * @param duration The duration of the animation in milliseconds (default: 500ms)
     */
    fun bounce(view: View, duration: Long = 500) {
        val anim = android.view.animation.AnimationUtils.loadAnimation(
            view.context,
            R.anim.bounce
        )
        anim.duration = duration
        view.startAnimation(anim)
    }

    /**
     * Applies a press animation to a view.
     * @param view The view to animate
     * @param onEnd Optional callback when the animation ends
     */
    fun press(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction { onEnd?.invoke() }
            }
    }
}
