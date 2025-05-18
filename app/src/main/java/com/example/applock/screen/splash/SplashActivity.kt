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
import com.example.applock.constant.EXTRA_FROM_SPLASH
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.databinding.ActivitySplashBinding
import com.example.applock.model.AppInfo
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.language.LanguageActivity
import com.example.applock.screen.validate_lock_pattern.LockPatternActivity
import com.example.applock.util.AppInfoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private lateinit var splashAnimation: Animation
    private lateinit var handler: Handler
    private lateinit var db: AppInfoDatabase

    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun initData() {
        splashAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_scaling)
        handler = Handler(Looper.getMainLooper())
        db = AppInfoDatabase.getInstance(this)
    }

    override fun setupView() {
        binding.imgSplashIcon.startAnimation(splashAnimation)
        lifecycleScope.launch {
            if (MyPreferences.read(MyPreferences.PREF_LOCK_PATTERN, null) == null)
                processAppDataAndNavigate(false)
            else
                processAppDataAndNavigate(true)
        }
    }

    private suspend fun processAppDataAndNavigate(hasLockPattern: Boolean) {
        val startTime = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val appInfoDao = db.appInfoDAO()
            if (!hasLockPattern) {
                AppInfoUtil.initInstalledApps(this@SplashActivity)
                // Tạo bản sao để tránh ConcurrentModificationException
                val appListCopy = ArrayList(AppInfoUtil.listAppInfo)
                appListCopy.forEach { appInfo ->
                    appInfoDao.insertAppInfo(appInfo)
                }
            } else {
                // Lấy danh sách từ database
                val allApps = appInfoDao.getAllApp() as ArrayList<AppInfo>
                val lockedApps = appInfoDao.getLockedApp() as ArrayList<AppInfo>

                // Cập nhật trạng thái isLocked cho allApps
                allApps.forEach { app ->
                    app.isLocked = lockedApps.any { it.packageName == app.packageName }
                }

                // Gán vào danh sách toàn cục
                synchronized(AppInfoUtil) {
                    AppInfoUtil.listAppInfo = allApps
                    AppInfoUtil.listLockedAppInfo = lockedApps
                }
            }
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < 3000) delay(3000 - elapsedTime)

        navigateTo(hasLockPattern)
    }

    private fun navigateTo(hasLockPattern: Boolean) {
        if (!hasLockPattern) {
            startActivity(Intent(this, LanguageActivity::class.java).apply {
                putExtra(EXTRA_FROM_SPLASH, true)
            })
        } else {
            startActivity(Intent(this, LockPatternActivity::class.java))
        }
        finish()
    }

    override fun handleEvent() {}
}