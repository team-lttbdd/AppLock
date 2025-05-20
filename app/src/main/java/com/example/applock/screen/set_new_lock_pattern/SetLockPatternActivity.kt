package com.example.applock.screen.set_new_lock_pattern

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.applock.R
import com.example.applock.base.BaseActivity
import com.example.applock.custom.lock_pattern.PatternLockView
import com.example.applock.custom.lock_pattern.PatternLockView.PatternViewMode
import com.example.applock.custom.lock_pattern.listener.PatternLockViewListener
import com.example.applock.databinding.ActivitySetLockPatternBinding
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.home.HomeActivity
import com.example.applock.screen.validate_lock_pattern.LockPatternActivity
import com.example.applock.util.*
import com.google.gson.Gson

class SetLockPatternActivity : BaseActivity<ActivitySetLockPatternBinding>() {

    private lateinit var tempPattern : ArrayList<PatternLockView.Dot>
    private var correctPattern = ArrayList<PatternLockView.Dot>()

    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySetLockPatternBinding {
        return ActivitySetLockPatternBinding.inflate(layoutInflater)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun initData() {
    }

    override fun setupView() {
    }

    override fun handleEvent() {
        binding.patternLockView.addPatternLockListener(object :
            PatternLockViewListener {
            override fun onStarted() {

            }

            override fun onProgress(progressPattern: MutableList<PatternLockView.Dot>?) {

            }

            override fun onComplete(pattern: MutableList<PatternLockView.Dot>?) {
                val length = pattern?.size ?: 0
                tempPattern = pattern?.let { ArrayList(it) } ?: arrayListOf()
                if(length < 4) {
                    AnimationUtil.setTextWrong(binding.patternLockView, binding.tvDrawAnUnlockPattern, tempPattern)
                } else {
                    if(correctPattern.isEmpty()) {
                        correctPattern = pattern?.let { ArrayList(it) } ?: arrayListOf()
                        Handler(Looper.getMainLooper()).post {
                            binding.patternLockView.clearPattern()
                            changeToConfirmPatternUI()
                        }
                    } else {
                        confirmPattern(correctPattern, pattern)
                    }
                }
                binding.btnReset.setOnClickListener {
                    correctPattern.clear()
                    changeToSetPatternUI()
                }
            }


            override fun onCleared() {
            }
        })
    }

    private fun changeToConfirmPatternUI() {
        binding.apply {
            tvNumberStep2.setTextColor(ContextCompat.getColor(this@SetLockPatternActivity, R.color.white))
            imgStepTwo.setImageDrawable(ContextCompat.getDrawable(this@SetLockPatternActivity, R.drawable.iv_step_1))
            ivProgressBar.setBackgroundColor(ContextCompat.getColor(this@SetLockPatternActivity, R.color.gradient_end))
            tvDrawAnUnlockPattern.setText(R.string.draw_pattern_again_to_confirm)
            btnReset.visibility = View.VISIBLE
            tvReset.visibility = View.VISIBLE

        }
    }

    private fun changeToSetPatternUI() {
        binding.apply {
            tvNumberStep2.setTextColor(ContextCompat.getColor(this@SetLockPatternActivity, R.color.gradient_end))
            imgStepTwo.setImageDrawable(ContextCompat.getDrawable(this@SetLockPatternActivity, R.drawable.iv_step_2))
            ivProgressBar.setBackgroundColor(ContextCompat.getColor(this@SetLockPatternActivity, R.color.grey))
            tvDrawAnUnlockPattern.setText(R.string.draw_an_unlock_pattern)
            btnReset.visibility = View.INVISIBLE
            tvReset.visibility = View.INVISIBLE
        }
    }

    private fun confirmPattern(correctPattern: ArrayList<PatternLockView.Dot>?, drawPattern: MutableList<PatternLockView.Dot>?) {
        if (tempPattern == correctPattern) {
            binding.patternLockView.setPattern(PatternViewMode.CORRECT, correctPattern)
            val gson = Gson()
            val json = gson.toJson(drawPattern)
            MyPreferences.write(MyPreferences.PREF_LOCK_PATTERN, json)
            startActivity(Intent(this@SetLockPatternActivity, LockPatternActivity::class.java))
            finish()
        } else {
            AnimationUtil.setTextWrong(binding.patternLockView, binding.tvDrawAnUnlockPattern, tempPattern)
            binding.patternLockView.clearPattern()
        }
    }
}