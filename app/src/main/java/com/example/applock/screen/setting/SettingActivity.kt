package com.example.applock.screen.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.applock.R
import com.example.applock.base.BaseActivity
import com.example.applock.databinding.ActivitySettingBinding
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.home.HomeActivity
import com.example.applock.screen.language.LanguageActivity
import com.example.applock.screen.validate_lock_pattern.LockPatternActivity

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

            itemChangeLanguages.setOnClickListener {
                startActivity(Intent(this@SettingActivity, LanguageActivity::class.java))
            }

            itemShareWithFriends.setOnClickListener {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    putExtra(Intent.EXTRA_TEXT, "Download ${getString(R.string.app_name)} from: https://play.google.com/store/apps/details?id=${packageName}")
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with_friends)))
            }

            itemFeedback.setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:") // Chỉ mở ứng dụng email
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("applock@gmail.com")) // Email nhận phản hồi
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for '${getString(R.string.app_name)}'")
                    putExtra(Intent.EXTRA_TEXT, "")
                }
                try {
                    startActivity(emailIntent)
                } catch (e: ActivityNotFoundException) {
                    // Nếu không có ứng dụng email nào được cài đặt
                    Toast.makeText(this@SettingActivity, "No email app found", Toast.LENGTH_SHORT).show()
                }
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