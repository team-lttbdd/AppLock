package com.example.applock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.applock.R
import com.example.applock.custom.lock_pattern.PatternLockView
import com.example.applock.custom.lock_pattern.listener.PatternLockViewListener
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.home.HomeActivity
import com.example.applock.util.AnimationUtil
import com.example.applock.util.AppInfoUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LockService : Service() {
    private val TAG = "LockService"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager
    private var lastForegroundPackageName = ""
    private val checkInterval = 500L // Kiểm tra mỗi 0.5 giây để phản ứng nhanh hơn

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isOverlayShown = false

    // Pattern cho xác thực
    private lateinit var correctPattern: List<PatternLockView.Dot>

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        // Tải pattern từ SharedPreferences
        loadSavedPattern()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AppLock Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kênh thông báo cho AppLock foreground service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun loadSavedPattern() {
        val gson = Gson()
        val json = MyPreferences.read(MyPreferences.PREF_LOCK_PATTERN, null)
        if (json != null) {
            val type = object : TypeToken<List<PatternLockView.Dot>>() {}.type
            correctPattern = gson.fromJson(json, type)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AppLock đang chạy")
            .setContentText("Bảo vệ ứng dụng của bạn")
            .setSmallIcon(R.drawable.applock_icon)      // icon bạn chuẩn bị trong drawable
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        // Chạy service ở chế độ foreground
        startForeground(NOTIFICATION_ID, notification)
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                checkCurrentApp()
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun checkCurrentApp() {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - TimeUnit.MINUTES.toMillis(1) // Lấy dữ liệu từ 1 phút trước

        val usageEvents = usageStatsManager.queryEvents(startTime, currentTime)
        val event = UsageEvents.Event()
        var foregroundPackageName = ""

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackageName = event.packageName
            }
        }

        if (foregroundPackageName.isNotEmpty() && foregroundPackageName != lastForegroundPackageName) {
            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())
            val logMessage = "[$timestamp] Ứng dụng được mở: $foregroundPackageName"
            val isLocked = AppInfoUtil.listLockedAppInfo.stream()
                .anyMatch({ appInfo -> appInfo.packageName.equals(foregroundPackageName) })

            // Ghi log
            Log.i(TAG, logMessage)
            writeToLogFile(logMessage)

            if (isLocked) {
                showPatternLockOverlay(foregroundPackageName)
            } else if (isOverlayShown) {
                hideOverlay()
            }

            lastForegroundPackageName = foregroundPackageName
        }
    }

    private fun showPatternLockOverlay(packageName: String) {
        if (isOverlayShown) return

        try {
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            // Sử dụng layout lock_pattern_overlay.xml (bạn cần tạo file layout này)
            overlayView = inflater.inflate(R.layout.lock_pattern_overlay, null)

            // Lấy các view từ layout
            val patternLockView = overlayView?.findViewById<PatternLockView>(R.id.pattern_lock_view)
            val tvAppName = overlayView?.findViewById<TextView>(R.id.tv_app_name)
            val tvDrawPattern = overlayView?.findViewById<TextView>(R.id.tv_draw_an_unlock_pattern)

            // Cập nhật tên ứng dụng
            tvAppName?.text = getAppNameFromPackage(packageName)

            // Xử lý sự kiện pattern lock
            patternLockView?.addPatternLockListener(object : PatternLockViewListener {
                override fun onStarted() {
                    // Không cần xử lý
                }

                override fun onProgress(progressPattern: MutableList<PatternLockView.Dot>?) {
                    // Không cần xử lý
                }

                override fun onComplete(pattern: MutableList<PatternLockView.Dot>?) {
                    val tempPattern = pattern?.let { ArrayList(it) } ?: arrayListOf()

                    if (!::correctPattern.isInitialized || pattern != correctPattern) {
                        // Pattern không đúng, hiển thị thông báo lỗi
                        AnimationUtil.setTextWrong(patternLockView, tvDrawPattern, tempPattern)
                    } else {
                        // Pattern đúng, ẩn overlay
                        patternLockView.setPattern(PatternLockView.PatternViewMode.CORRECT, tempPattern)

                        // Thêm độ trễ ngắn để người dùng thấy mẫu hình đúng đã được vẽ
                        Handler(Looper.getMainLooper()).postDelayed({
                            hideOverlay()
                        }, 300)
                    }
                }

                override fun onCleared() {
                    // Không cần xử lý
                }
            })

            // Thiết lập tùy chọn cho overlay
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.CENTER

            // Hiển thị overlay
            windowManager.addView(overlayView, params)
            isOverlayShown = true

            // Cập nhật flag để có thể tương tác với overlay
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager.updateViewLayout(overlayView, params)

            Log.i(TAG, "Hiển thị màn hình khóa cho ứng dụng: $packageName")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị màn hình khóa: ${e.message}")
        }
    }

    private fun hideOverlay() {
        if (!isOverlayShown || overlayView == null) return

        try {
            windowManager.removeView(overlayView)
            overlayView = null
            isOverlayShown = false

            Log.i(TAG, "Đã ẩn màn hình khóa")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ẩn màn hình khóa: ${e.message}")
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            return packageName
        }
    }

    private fun writeToLogFile(message: String) {
        try {
            val logFile = applicationContext.getFileStreamPath("app_access_log.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            applicationContext.openFileOutput("app_access_log.txt", MODE_APPEND).use { output ->
                output.write("$message\n".toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Không thể ghi log ra file: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        // Đảm bảo ẩn overlay khi service bị hủy
        if (isOverlayShown) {
            hideOverlay()
        }

        Log.i(TAG, "LockService đã bị hủy")
    }

    companion object {
        private const val CHANNEL_ID = "applock_service_channel"
        private const val NOTIFICATION_ID = 1
    }
}