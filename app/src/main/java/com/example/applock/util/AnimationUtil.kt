package com.example.applock.util

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.example.applock.custom.lock_pattern.PatternLockView
import com.example.applock.custom.lock_pattern.PatternLockView.PatternViewMode

object AnimationUtil {
    fun setTextWrong(patternLockView: PatternLockView, textView: TextView?, pattern: MutableList<PatternLockView.Dot>?) {
        patternLockView.setPattern(PatternViewMode.WRONG, pattern)
        val animator = ObjectAnimator.ofFloat(
            textView,
            "translationX",
            0f, 20f, -20f, 15f, -15f, 5f, -5f, 0f
        )
        animator.duration = 1000

        textView?.setTextColor(Color.RED)
        animator.start()

        Handler(Looper.getMainLooper()).postDelayed({
            textView?.setTextColor(Color.BLACK)
            patternLockView.clearPattern()
        }, 1000)
    }


}