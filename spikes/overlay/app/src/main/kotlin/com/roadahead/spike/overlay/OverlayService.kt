package com.roadahead.spike.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class OverlayService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel("overlay", "오버레이 시험", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, "overlay")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("오버레이 시험 중")
            .setContentText("시험 앱에서 카드를 숨길 수 있습니다")
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(21, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(21, notification)
        }
        try {
            OverlayCard.show(this, 0.8f)
            Log.i("OverlaySpike", "foreground service started")
        } catch (error: Exception) {
            Log.e("OverlaySpike", "foreground overlay failed", error)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { OverlayCard.hide(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
