//package com.example.sleep_garden.sleep
//
//import android.app.Service
//import android.content.Intent
//import android.graphics.PixelFormat
//import android.os.Build
//import android.os.IBinder
//import android.util.TypedValue
//import android.view.Gravity
//import android.view.View
//import android.view.WindowManager
//import android.widget.FrameLayout
//import android.widget.ImageView
//import com.example.sleep_garden.R
//
//class SleepOverlayService : Service() {
//
//    private lateinit var wm: WindowManager
//    private var overlayView: View? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        wm = getSystemService(WINDOW_SERVICE) as WindowManager
//        showOverlay()
//    }
//
//    private fun showOverlay() {
//        if (overlayView != null) return
//
//        // ルート（全画面）
//        val root = FrameLayout(this).apply {
//            layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            isClickable = true
//            isFocusable = false
//        }
//
//        // 背景（フェード画像）— 画面いっぱい
//        val bg = ImageView(this).apply {
//            setImageResource(R.drawable.sleep_overlay)
//            scaleType = ImageView.ScaleType.CENTER_CROP
//            layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//        }
//        root.addView(bg)
//
//        // 起きるボタン（比率を保ちつつ下寄せ・横90%）
//        val wake = ImageView(this).apply {
//            setImageResource(R.drawable.btn_wake)
//            scaleType = ImageView.ScaleType.FIT_CENTER
//            adjustViewBounds = true
//
//            val widthPercent = 0.90f
//            val lp = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
//            )
//            lp.leftMargin = 0
//            lp.rightMargin = 0
//            lp.bottomMargin = dp(48)
//            layoutParams = lp
//
//            // 横 90% で収まるように左右パディング
//            val sidePad = ((resources.displayMetrics.widthPixels * (1f - widthPercent)) / 2f).toInt()
//            setPadding(sidePad, dp(6), sidePad, dp(6))
//
//            setOnClickListener {
//                removeOverlayAndStop()
//            }
//        }
//        root.addView(wake)
//
//        overlayView = root
//
//        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//        else
//            WindowManager.LayoutParams.TYPE_PHONE
//
//        val lp = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            type,
//            // タッチ可能・フォーカスは奪わない（ホーム・通知の操作は基本妨げない）
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//            PixelFormat.TRANSLUCENT
//        ).apply {
//            gravity = Gravity.CENTER
//        }
//
//        wm.addView(overlayView, lp)
//    }
//
//    private fun removeOverlayAndStop() {
//        overlayView?.let {
//            try {
//                wm.removeView(it)
//            } catch (_: Throwable) { /* ignore */ }
//            overlayView = null
//        }
//        stopSelf()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // 何度呼ばれても確実に表示されるように
//        showOverlay()
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        overlayView?.let {
//            try {
//                wm.removeViewImmediate(it)
//            } catch (_: Throwable) { /* ignore */ }
//            overlayView = null
//        }
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    private fun dp(v: Int): Int =
//        TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            v.toFloat(),
//            resources.displayMetrics
//        ).toInt()
//}
