package com.example.applock.screen.home

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Shader
import android.os.Build
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.applock.R
import com.example.applock.base.BaseActivity
import com.example.applock.databinding.ActivityHomeBinding
import com.example.applock.receiver.AppLockDeviceAdminReceiver
import com.example.applock.receiver.LocaleChangeReceiver
import com.example.applock.receiver.PackageChangeReceiver
import com.example.applock.screen.dialog.PermissionDialog
import com.example.applock.screen.setting.SettingActivity
import com.example.applock.service.LockService
import com.example.applock.util.AppInfoUtil
import com.example.applock.util.PermissionUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// Activity chính chứa ViewPager2 với AllAppFragment và LockedAppFragment
class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private val viewModel: AppLockViewModel by viewModels() // ViewModel quản lý dữ liệu
    private lateinit var packageChangeReceiver: PackageChangeReceiver
    private lateinit var localeChangeReceiver: LocaleChangeReceiver

    override fun getViewBinding(layoutInflater: LayoutInflater): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun initData() {
        try {
            AppInfoUtil.initInstalledApps(this)
            viewModel.loadInitialData(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.error_loading_apps, Toast.LENGTH_SHORT).show()
        }
        checkAndRequestNotificationPermission()
        ContextCompat.startForegroundService(this, Intent(this, LockService::class.java))
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        packageChangeReceiver = PackageChangeReceiver(viewModel)
        registerReceiver(packageChangeReceiver, packageFilter)
        localeChangeReceiver = LocaleChangeReceiver.register(this)
    }

    override fun setupView() {
        binding.apply {
            viewPager2.adapter = FragmentPageAdapter(supportFragmentManager, lifecycle, viewModel)
            TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
                tab.text = when(position) {
                    0 -> getString(R.string.all_apps) // Tab danh sách ứng dụng chưa khóa
                    1 -> getString(R.string.locked_apps) // Tab danh sách ứng dụng đã khóa
                    else -> ""
                }
            }.attach()
            viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tabLayout.getTabAt(position)?.select()
                    updateTabLayoutTextColor(position)
                }
            })
        }
    }

    override fun handleEvent() {
        binding.apply {
            tabLayout.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.position?.let { viewPager2.currentItem = it }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            btnSetting.setOnClickListener {
                startActivity(Intent(this@HomeActivity, SettingActivity::class.java))
            }
        }
    }

    // Cập nhật màu chữ và kiểu chữ cho TabLayout
    fun updateTabLayoutTextColor(selectedPosition: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            val colors = if (i == selectedPosition) {
                intArrayOf(
                    ContextCompat.getColor(this, R.color.gradient_start),
                    ContextCompat.getColor(this, R.color.gradient_end)
                )
            } else {
                intArrayOf(
                    ContextCompat.getColor(this, R.color.tab_unselected),
                    ContextCompat.getColor(this, R.color.tab_unselected)
                )
            }
            val height = tab?.view?.height ?: 0
            val spannable = SpannableString(tab?.text)
            spannable.setSpan(GradientTextSpan(colors, height.toFloat()), 0, spannable.length, 0)
            tab?.text = spannable
            if (i == selectedPosition) setTabTypeface(tab)
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
        tab?.view?.let { view ->
            for (i in 0 until view.childCount) {
                val typeface = ResourcesCompat.getFont(view.context, R.font.exo_bold)
                (view.getChildAt(i) as? TextView)?.typeface = typeface
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageChangeReceiver)
        try {
            unregisterReceiver(localeChangeReceiver)
        } catch (e: IllegalArgumentException) {
            // Bỏ qua nếu receiver chưa được đăng ký
        }
    }

    // Kiểm tra và yêu cầu quyền thông báo
    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showDialogRequestPermission()
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.notification_permission_title)
                        .setMessage(R.string.notification_permission_message)
                        .setPositiveButton(R.string.agree) { _, _ ->
                            requestNotificationPermissionLauncher.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        } else {
            showDialogRequestPermission()
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) showDialogRequestPermission()
            else Toast.makeText(this, R.string.notifications_disabled, Toast.LENGTH_SHORT).show()
        }

    private var permissionDialog: PermissionDialog? = null

    private fun showDialogRequestPermission() {
        if (PermissionUtil.isAllPermissionRequested()) {
            ContextCompat.startForegroundService(this, Intent(this, LockService::class.java))
        } else {
            permissionDialog = PermissionDialog().apply {
                show(supportFragmentManager, "permission_dialog")
                onToggleUsageClick = { PermissionUtil.requestUsageStatsPermission() }
                onToggleOverlayClick = { PermissionUtil.requestOverlayPermission() }
                onToggleDeviceAdminClick = { requestDeviceAdminPermission() }
                onGotoSettingClick = {
                    when {
                        !PermissionUtil.checkUsageStatsPermission() -> PermissionUtil.requestUsageStatsPermission()
                        !PermissionUtil.checkOverlayPermission() -> PermissionUtil.requestOverlayPermission()
                        !PermissionUtil.checkDeviceAdminPermission() -> requestDeviceAdminPermission()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionDialog?.updateToggle()
    }

    private fun requestDeviceAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this@HomeActivity, AppLockDeviceAdminReceiver::class.java))
        }
        requestDeviceAdminLauncher.launch(intent)
    }

    private val requestDeviceAdminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
}