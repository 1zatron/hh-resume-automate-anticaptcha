package com.github.s3rgeym.hh_resume_automate.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "captcha_channel"
    private const val NOTIFICATION_ID = 1001

    fun showCaptchaNotification(context: Context, captchaUrl: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Проверка капчи",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о необходимости ввода капчи HeadHunter"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Создаем чистый интент для веб-страниц
        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(captchaUrl)
            // Эти флаги заставляют Android вызывать браузер как независимый процесс, 
            // игнорируя то, что наше приложение сейчас заблокировано сетевым запросом
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }

        val requestCode = System.currentTimeMillis().toInt()

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            browserIntent,
            flags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Требуется ввести капчу hh.ru!")
            .setContentText("Нажмите здесь для перехода в браузер")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true) 
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
