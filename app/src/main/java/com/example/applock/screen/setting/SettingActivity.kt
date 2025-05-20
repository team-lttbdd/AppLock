package com.example.applock.screen.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.example.applock.base.BaseActivity
import com.example.applock.databinding.ActivitySettingBinding
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.home.HomeActivity
import com.example.applock.screen.validate_lock_pattern.LockPatternActivity
import com.example.applock.R


class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySettingBinding {
        return ActivitySettingBinding.inflate(layoutInflater)
    }

    override fun initData() {

    }

    override fun setupView() {
        updateSwitchHidePatternUI()
    }

    override fun handleEvent() {
        binding.apply {
            imgToggle.setOnClickListener {
                MyPreferences.write(MyPreferences.IS_HIDE_DRAW_PATTERN, !MyPreferences.read(MyPreferences.IS_HIDE_DRAW_PATTERN, false))
                updateSwitchHidePatternUI()
            }

            binding.itemChangePassword.setOnClickListener {
                val intent = Intent(this@SettingActivity, LockPatternActivity::class.java)
                intent.putExtra("CHANGE_PASSWORD", true)
                startActivity(intent)
            }


            imgBack.setOnClickListener {
                finish()
            }
        }
    }
    private fun updateSwitchHidePatternUI() {
        binding.imgToggle.setImageResource(
            if (MyPreferences.read(MyPreferences.IS_HIDE_DRAW_PATTERN, false)) {
                com.example.applock.R.drawable.ic_toggle_inactive
            } else {
                com.example.applock.R.drawable.ic_toggle_active
            }
        )
    }
}