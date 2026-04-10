package com.screenrecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

internal class ScreenRecorderService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "screen_recorder_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_STOP = "com.screenrecorder.STOP"

        fun start(context: Context) {
            val intent = Intent(context, ScreenRecorderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenRecorderService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification("Screen Recorder ready")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ScreenRecorderServiceBridge.onStopRequested?.invoke()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        ScreenRecorderServiceBridge.onServiceDestroyed?.invoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screen Recorder",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows when screen recording is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Screen Recorder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(Notification.Action.Builder(
                null, "Stop", stopPendingIntent
            ).build())
            .setOngoing(true)
            .build()
    }

    fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}

/** Bridge for communication between service and ScreenRecorder singleton. */
internal object ScreenRecorderServiceBridge {
    var onStopRequested: (() -> Unit)? = null
    var onServiceDestroyed: (() -> Unit)? = null
}
