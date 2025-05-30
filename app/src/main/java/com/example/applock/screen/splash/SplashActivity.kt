package com.example.applock.screen.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.example.applock.R
import com.example.applock.base.BaseActivity
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.databinding.ActivitySplashBinding
import com.example.applock.model.AppInfo
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.home.HomeActivity
import com.example.applock.screen.language.LanguageActivity
import com.example.applock.screen.set_new_lock_pattern.SetLockPatternActivity
import com.example.applock.screen.validate_lock_pattern.LockPatternActivity
import com.example.applock.util.AppInfoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Activity hiển thị màn hình splash khi khởi động ứng dụng
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private lateinit var splashAnimation: Animation // Hiệu ứng phóng to cho biểu tượng splash
    private lateinit var handler: Handler // Handler để xử lý tác vụ trên main thread
    private lateinit var db: AppInfoDatabase // Database để quản lý thông tin ứng dụng

    // Khởi tạo giao diện Activity
    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    // Khởi tạo dữ liệu ban đầu
    override fun initData() {
        // Tải hiệu ứng phóng to từ resource
        splashAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_scaling)
        // Khởi tạo Handler để xử lý tác vụ trên main thread
        handler = Handler(Looper.getMainLooper())
        // Khởi tạo database
        db = AppInfoDatabase.getInstance(this)
    }

    // Thiết lập giao diện và bắt đầu xử lý dữ liệu
    override fun setupView() {
        // Áp dụng hiệu ứng phóng to cho biểu tượng splash
        binding.imgSplashIcon.startAnimation(splashAnimation)
        // Kiểm tra ngôn ngữ và mẫu khóa để điều hướng
        lifecycleScope.launch {
            val hasLanguage = MyPreferences.read(MyPreferences.PREF_LANGUAGE, null) != null
            val hasLockPattern = MyPreferences.read(MyPreferences.PREF_LOCK_PATTERN, null) != null

            processAppDataAndNavigate(
                when {
                    !hasLanguage -> ProcessDestination.LANGUAGE
                    hasLanguage && !hasLockPattern -> ProcessDestination.SET_LOCK_PATTERN
                    else -> ProcessDestination.LOCK_PATTERN
                }
            )
        }
    }

    // Enum để định nghĩa các đích đến sau khi xử lý dữ liệu
    private enum class ProcessDestination {
        LANGUAGE, // Màn hình chọn ngôn ngữ
        HOME, // Màn hình chính
        LOCK_PATTERN, // Màn hình xác thực mẫu khóa
        SET_LOCK_PATTERN // Màn hình tạo mẫu khóa mới
    }

    // Xử lý dữ liệu ứng dụng và điều hướng
    private suspend fun processAppDataAndNavigate(destination: ProcessDestination) {
        val startTime = System.currentTimeMillis() // Ghi lại thời gian bắt đầu
        // Tải danh sách ứng dụng và cập nhật database
        withContext(Dispatchers.IO) {
            val appInfoDao = db.appInfoDAO()
            val hasLanguage = MyPreferences.read(MyPreferences.PREF_LANGUAGE, null) != null
            
            if (!hasLanguage) {
                // Lần đầu chạy app, khởi tạo danh sách app
                AppInfoUtil.initInstalledApps(this@SplashActivity)
                appInfoDao.deleteAll()
                val appsToInsert = ArrayList(AppInfoUtil.listAppInfo)
                appsToInsert.forEach { appInfo ->
                    appInfoDao.insertAppInfo(appInfo)
                }
            } else {
                // Các lần sau, chỉ cập nhật danh sách từ database
                val allApps = appInfoDao.getAllApp()
                val lockedApps = appInfoDao.getLockedApp()
                AppInfoUtil.listAppInfo = ArrayList(allApps)
                AppInfoUtil.listLockedAppInfo = ArrayList(lockedApps)
            }
        }

        // Đảm bảo thời gian splash tối thiểu 3 giây
        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < 3000) delay(3000 - elapsedTime)

        // Điều hướng đến màn hình tương ứng
        when (destination) {
            ProcessDestination.LANGUAGE -> {
                val intent = Intent(this, LanguageActivity::class.java)
                intent.putExtra("IS_FIRST_LAUNCH", true)
                startActivity(intent)
            }
            ProcessDestination.HOME -> navigateTo(HomeActivity::class.java)
            ProcessDestination.LOCK_PATTERN -> navigateTo(LockPatternActivity::class.java)
            ProcessDestination.SET_LOCK_PATTERN -> navigateTo(SetLockPatternActivity::class.java)
        }
        finish()
    }

    // Điều hướng đến Activity đích và kết thúc SplashActivity
    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
    }

    // Không xử lý sự kiện trong SplashActivity
    override fun handleEvent() {}
}