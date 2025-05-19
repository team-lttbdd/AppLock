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

    private suspend fun processAppDataAndNavigate(hasLockPattern : Boolean) {
        val startTime = System.currentTimeMillis()
        //Initial 2 app lists
        withContext(Dispatchers.IO) {
            val appInfoDao = db.appInfoDAO()
            if(!hasLockPattern) {
                AppInfoUtil.initInstalledApps(this@SplashActivity)
                AppInfoUtil.listAppInfo.forEach{ appInfo ->
                    appInfoDao.insertAppInfo(appInfo)
                }
            }
            else {
                AppInfoUtil.listAppInfo = appInfoDao.getAllApp() as ArrayList<AppInfo>
                AppInfoUtil.listLockedAppInfo = appInfoDao.getLockedApp() as ArrayList<AppInfo>
            }
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < 3000) delay(3000 - elapsedTime)

        navigateTo(if(!hasLockPattern) LanguageActivity::class.java else LockPatternActivity::class.java)
    }

    private fun navigateTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish()
    }

    override fun handleEvent() {}
}
