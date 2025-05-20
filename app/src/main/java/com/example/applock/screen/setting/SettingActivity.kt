package com.example.applock.screen.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.example.applock.base.BaseActivity
import com.example.applock.databinding.ActivitySettingBinding
import com.example.applock.screen.home.HomeActivity
import com.example.applock.screen.validate_lock_pattern.LockPatternActivity

class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySettingBinding {
        return ActivitySettingBinding.inflate(layoutInflater)
    }

    override fun initData() {

    }

    override fun setupView() {

    }

    override fun handleEvent() {
        binding.apply {

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
}