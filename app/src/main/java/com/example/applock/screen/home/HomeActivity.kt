package com.example.applock.screen.home

import android.content.Intent
import android.content.IntentFilter
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
import com.example.applock.receiver.PackageChangeReceiver
import com.example.applock.screen.setting.SettingActivity
import com.example.applock.util.PermissionUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.jvm.java

// Activity chính chứa ViewPager2 với AllAppFragment và LockedAppFragment
class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private val viewModel: AppLockViewModel by viewModels() // ViewModel để quản lý dữ liệu
    private lateinit var permissionUtils: PermissionUtils // Utility để kiểm tra quyền
    private lateinit var packageChangeReceiver: PackageChangeReceiver

    // Khởi tạo giao diện Activity
    override fun getViewBinding(layoutInflater: LayoutInflater): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }

    // Khởi tạo dữ liệu và quyền
    override fun initData() {
        try {
            viewModel.loadInitialData(this) // Tải dữ liệu ứng dụng
        } catch (e: Exception) {
            e.printStackTrace()
        }

        permissionUtils = PermissionUtils(this)
        checkAndRequestPermissions() // Kiểm tra và yêu cầu quyền
        // Khởi động dịch vụ khóa ứng dụng
        ContextCompat.startForegroundService(this, Intent(this, LockService::class.java))

        // Đăng ký BroadcastReceiver để lắng nghe sự kiện gỡ cài đặt ứng dụng
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        packageChangeReceiver = PackageChangeReceiver(viewModel)
        registerReceiver(packageChangeReceiver, packageFilter)
    }

    // Kiểm tra và yêu cầu các quyền cần thiết
    private fun checkAndRequestPermissions() {
        if (!permissionUtils.checkUsageStatsPermission()) {
            permissionUtils.requestUsageStatsPermission() // Yêu cầu quyền Usage Stats
        }
        if (!permissionUtils.checkOverlayPermission()) {
            permissionUtils.requestOverlayPermission() // Yêu cầu quyền Overlay
        }
    }

    // Thiết lập giao diện và ViewPager2
    override fun setupView() {
        try {
            binding.apply {
                // Gán adapter cho ViewPager2
                viewPager2.adapter = FragmentPageAdapter(supportFragmentManager, lifecycle, viewModel)

                // Liên kết TabLayout với ViewPager2
                TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
                    tab.text = when(position) {
                        0 -> getString(R.string.all_apps) // Tab danh sách ứng dụng chưa khóa
                        1 -> getString(R.string.locked_apps) // Tab danh sách ứng dụng đã khóa
                        else -> ""
                    }
                }.attach()

                // Cập nhật màu chữ khi chuyển tab
                viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        tabLayout.getTabAt(position)?.select()
                        updateTabLayoutTextColor(position) // Cập nhật màu chữ tab
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Xử lý sự kiện click
    override fun handleEvent() {
        binding.apply {
            // Xử lý sự kiện chọn tab
            tabLayout.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (tab != null) viewPager2.currentItem = tab.position
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            // Chuyển đến SettingActivity
            btnSetting.setOnClickListener {
                startActivity(Intent(this@HomeActivity, SettingActivity::class.java))
            }
        }
    }

    // Cập nhật màu chữ và kiểu chữ cho TabLayout
    fun updateTabLayoutTextColor(selectedPosition: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val colors = if (i == selectedPosition) intArrayOf(
                resources.getColor(R.color.gradient_start, null),
                resources.getColor(R.color.gradient_end, null)
            ) else intArrayOf(Color.parseColor("#ACACAC"), Color.parseColor("#ACACAC"))
            val tab = binding.tabLayout.getTabAt(i)
            val height = tab?.view?.height ?: 0
            val spannable = SpannableString(tab?.text)
            // Áp dụng gradient cho chữ tab được chọn
            spannable.setSpan(GradientTextSpan(colors, height.toFloat()), 0, spannable.length, 0)
            tab?.text = spannable
            if (i == selectedPosition) setTabTypeface(binding.tabLayout.getTabAt(i))
        }
    }

    // Class để áp dụng gradient cho chữ tab
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

    // Đặt kiểu chữ đậm cho tab được chọn
    private fun setTabTypeface(tab: TabLayout.Tab?) {
        tab?.let {
            for (i in 0 until tab.view.childCount) {
                val typeface = ResourcesCompat.getFont(tab.view.context, R.font.exo_bold)
                val tabViewChild = tab.view.getChildAt(i)
                if (tabViewChild is TextView) tabViewChild.typeface = typeface
            }
        }
    }

    // Xử lý sự kiện khi Activity bị hủy
    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký receiver khi Activity bị hủy
        unregisterReceiver(packageChangeReceiver)
    }
}