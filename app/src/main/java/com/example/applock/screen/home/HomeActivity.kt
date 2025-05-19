package com.example.applock.screen.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Shader
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.applock.service.LockService
import com.example.applock.R
import com.example.applock.base.BaseActivity
import com.example.applock.databinding.ActivityHomeBinding
import com.example.applock.screen.setting.SettingActivity
import com.example.applock.util.PermissionUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.jvm.java

class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private val viewModel: AppLockViewModel by viewModels()
    private lateinit var permissionUtils: PermissionUtils

    override fun getViewBinding(layoutInflater: LayoutInflater): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun initData() {
        try {
            // Khởi tạo database
            viewModel.loadInitialData(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        permissionUtils = PermissionUtils(this)
        // Yêu cầu các quyền cần thiết
        checkAndRequestPermissions()
        ContextCompat.startForegroundService(this, Intent(this, LockService::class.java))
    }

    private fun checkAndRequestPermissions() {
        if (!permissionUtils.checkUsageStatsPermission()) {
            permissionUtils.requestUsageStatsPermission()
        }

        // Kiểm tra quyền Overlay
        if (!permissionUtils.checkOverlayPermission()) {
            permissionUtils.requestOverlayPermission()
        }
    }
    override fun setupView() {
        try {
            binding.apply {
                // Setup ViewPager2
                viewPager2.adapter = FragmentPageAdapter(supportFragmentManager, lifecycle, viewModel)
                
                // Setup TabLayout
                TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
                    tab.text = when(position) {
                        0 -> getString(R.string.all_apps)
                        1 -> getString(R.string.locked_apps)
                        else -> ""
                    }
                }.attach()

                viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        tabLayout.getTabAt(position)?.select()
                        updateTabLayoutTextColor(position)
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun handleEvent() {
        binding.apply {
            tabLayout.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (tab != null) viewPager2.currentItem = tab.position
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            btnSetting.setOnClickListener({
                startActivity(Intent(this@HomeActivity, SettingActivity::class.java))

            })
        }
    }

    fun updateTabLayoutTextColor(selectedPosition: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val colors = if (i == selectedPosition) intArrayOf(
                resources.getColor(R.color.gradient_start, null),
                resources.getColor(R.color.gradient_end, null)
            ) else
                intArrayOf(Color.parseColor("#ACACAC"), Color.parseColor("#ACACAC"))
            val tab = binding.tabLayout.getTabAt(i)
            val height = tab?.view?.height ?: 0
            val spannable = SpannableString(tab?.text)

            spannable.setSpan(GradientTextSpan(colors, height.toFloat()), 0, spannable.length, 0)
            tab?.text = spannable

            if (i == selectedPosition) setTabTypeface(binding.tabLayout.getTabAt(i))
        }
    }

    inner class GradientTextSpan(private val colors: IntArray, private val height: Float) :
        CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) {
            val shader = android.graphics.LinearGradient(
                0f, 0f, tp.textSize, height,
                colors, null, Shader.TileMode.CLAMP
            )
            tp.shader = shader
        }
    }

    private fun setTabTypeface(tab: TabLayout.Tab?) {
        tab?.let {
            for (i in 0 until tab.view.childCount) {
                val typeface = ResourcesCompat.getFont(tab.view.context, R.font.exo_bold)
                val tabViewChild = tab.view.getChildAt(i)
                if (tabViewChild is TextView) tabViewChild.typeface = typeface
            }
        }
    }
}