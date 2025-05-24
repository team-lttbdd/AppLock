package com.example.applock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import com.example.applock.custom.lock_pattern.PatternLockView.PatternViewMode
import com.example.applock.custom.lock_pattern.listener.PatternLockViewListener
import com.example.applock.preference.MyPreferences
import com.example.applock.screen.home.HomeActivity
import com.example.applock.util.AnimationUtil
import com.example.applock.util.AppInfoUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class LockService : Service() {
    private val TAG = "LockService"
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringJob: Job? = null
    private var lastForegroundPackageName = ""
    private val checkInterval = 200L
    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager
    private var overlayView: View? = null
    private var isOverlayShown = false

    private lateinit var correctPattern: List<PatternLockView.Dot>

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WindowManager::class.java)
        usageStatsManager = getSystemService(UsageStatsManager::class.java)
        // Tải pattern từ SharedPreferences
        loadSavedPattern()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Kiểm tra nếu là hành động cập nhật mẫu khóa
        if (intent?.action == "UPDATE_PATTERN") {
            loadSavedPattern()
            return START_STICKY
        }

        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AppLock đang chạy")
            .setContentText("Bảo vệ ứng dụng của bạn")
            .setSmallIcon(R.drawable.applock_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true) //sự kiện đang diễn ra
            .build()

        // Chạy service ở chế độ foreground
        startForeground(NOTIFICATION_ID, notification)
        startMonitoring()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        // Đảm bảo ẩn overlay khi service bị hủy
        if (isOverlayShown) hideOverlay()

        // Khởi động lại service nếu bị hủy
        startService(applicationContext)
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
        try {
            val gson = Gson()
            val json = MyPreferences.read(MyPreferences.PREF_LOCK_PATTERN, null)
            if (json != null) {
                val type = object : TypeToken<List<PatternLockView.Dot>>() {}.type
                correctPattern = gson.fromJson(json, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tải mẫu khóa: ${e.message}")
        }
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    checkCurrentApp()
                    delay(checkInterval)
                } catch (_: CancellationException) {
                    break // Thoát khi bị cancel
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi: ${e.message}")
                    delay(checkInterval) // Vẫn delay trước khi thử lại
                }
            }
        }
    }

    private suspend fun checkCurrentApp() {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - 3000L
        val usageEvents = usageStatsManager.queryEvents(startTime, currentTime)
        val event = UsageEvents.Event()
        var foregroundPackageName = ""

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
                foregroundPackageName = event.packageName
        }

        if (foregroundPackageName.isNotEmpty() && foregroundPackageName != lastForegroundPackageName) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = "[$timestamp] Ứng dụng được mở: $foregroundPackageName (trước đó: $lastForegroundPackageName)"
            Log.d(TAG, logMessage)
            // Kiểm tra nếu ứng dụng bị khóa
            val isLocked = AppInfoUtil.listLockedAppInfo
                // Chuyển List<AppInfo> thành Sequence<AppInfo> để xử lý lazy (chỉ thực thi khi cần)
                .asSequence()
                // any { … } là phép kiểm tra, trả về true ngay khi tìm thấy phần tử đầu tiên thỏa mãn
                .any { it.packageName == foregroundPackageName }

            // Chuyển về Main thread để cập nhật UI
            withContext(Dispatchers.Main) {
                if (isOverlayShown) hideOverlay()
                else if (isLocked) showPatternLockOverlay()
            }

            lastForegroundPackageName = foregroundPackageName
        }
    }

    private fun showPatternLockOverlay() {
        try {
            // Tải lại mẫu khóa mỗi khi hiển thị overlay để đảm bảo luôn sử dụng mẫu mới nhất
            loadSavedPattern()
            
            // Lấy ngôn ngữ đã lưu
            val savedLanguage = MyPreferences.read(MyPreferences.PREF_LANGUAGE, "en") ?: "en"
            val locale = Locale(savedLanguage)
            val configuration = resources.configuration
            configuration.setLocale(locale)
            
            // Tạo Context mới với ngôn ngữ đã cập nhật
            val localizedContext = createConfigurationContext(configuration)

            val inflater = LayoutInflater.from(localizedContext)
            overlayView = inflater.inflate(R.layout.lock_pattern_overlay, null)


            // Lấy các view từ layout bằng findViewById
            val patternLockView = overlayView?.findViewById<PatternLockView>(R.id.pattern_lock_view)
            val tvAppName = overlayView?.findViewById<TextView>(R.id.tv_app_name)
            val tvDrawPattern = overlayView?.findViewById<TextView>(R.id.tv_draw_an_unlock_pattern)

            if (patternLockView == null || tvAppName == null || tvDrawPattern == null) {
                Log.e(TAG, "Không thể tìm thấy các view trong layout")
                return
            }

            // Cập nhật văn bản với ngôn ngữ hiện tại
            tvAppName.text = localizedContext.getString(R.string.app_name)
            tvDrawPattern.text = localizedContext.getString(R.string.draw_an_unlock_pattern)

            // Xử lý sự kiện pattern lock
            patternLockView.addPatternLockListener(object : PatternLockViewListener {
                override fun onStarted() {}

                override fun onProgress(progressPattern: MutableList<PatternLockView.Dot>?) {}

                override fun onComplete(pattern: MutableList<PatternLockView.Dot>?) {
                    val tempPattern = pattern?.let { ArrayList(it) } ?: arrayListOf()
                    if (pattern != correctPattern) AnimationUtil.setTextWrong(patternLockView, tvDrawPattern, tempPattern)
                    else {
                        patternLockView.setPattern(PatternViewMode.CORRECT, tempPattern)
                        hideOverlay()
                    }
                }

                override fun onCleared() {}
            })

            // Thiết lập tùy chọn cho overlay với mức ưu tiên cao nhất
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                //Hiển thị view toàn bộ màn hình
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                //View hoàn toàn không trong suốt, alpha bị bỏ qua
                PixelFormat.OPAQUE
            )
            params.gravity = Gravity.CENTER

            // Hiển thị overlay
            try {
                windowManager.addView(overlayView, params)
                isOverlayShown = true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi thêm overlay vào WindowManager: ${e.message}")
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị màn hình khóa: ${e.message}")
        }
    }

    private fun hideOverlay() {
        if (!isOverlayShown || overlayView == null) { return }
        try {
            windowManager.removeView(overlayView)
            overlayView = null
            isOverlayShown = false
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ẩn màn hình khóa: ${e.message}")
        }
    }

    companion object {
        private const val CHANNEL_ID = "applock_service_channel"
        private const val NOTIFICATION_ID = 1

        // Hàm tiện ích để khởi động service từ bên ngoài
        fun startService(context: Context) {
            try {
                val intent = Intent(context, LockService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (e: Exception) {
                Log.e("LockService", "Lỗi khi khởi động service: ${e.message}")
            }
        }
        
        // Thêm phương thức mới để cập nhật mẫu khóa
        fun updatePattern(context: Context) {
            try {
                val intent = Intent(context, LockService::class.java)
                intent.action = "UPDATE_PATTERN"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (e: Exception) {
                Log.e("LockService", "Lỗi khi cập nhật mẫu khóa: ${e.message}")
            }
        }
    }
}
