package com.example.vktesttask

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.vktesttask.work.FolderFileChangedWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CustomApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    lateinit var foregroundInfo: ForegroundInfo
    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "123",
                "FolderHash",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Hash"
            channel.enableLights(true)
            channel.setSound(null, null)
            channel.enableVibration(false)
            notificationManager!!.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "123")
            .setSmallIcon(com.example.vktesttask.R.mipmap.ic_launcher)
            .setContentTitle("Hash")
            .build()

        foregroundInfo = ForegroundInfo(0, notification)
    }
}
