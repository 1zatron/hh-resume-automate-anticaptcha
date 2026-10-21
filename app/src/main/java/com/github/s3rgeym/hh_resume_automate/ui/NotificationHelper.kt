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

        // Чистый интент для открытия ссылки. Android сам поймет, что нужно запустить браузер.
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(captchaUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        // Уникальный requestCode на основе времени, чтобы система гарантированно создавала новое действие
        val requestCode = System.currentTimeMillis().toInt()

        // FLAG_IMMUTABLE обязателен для Android 14+ при передаче управления операционной системе
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
            .setContentText("Нажмите здесь для перехода в браузер и прохождения проверки")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true) // ВАЖНО: Уведомление нельзя смахнуть случайно, оно держится на экране
            .setAutoCancel(false) // ВАЖНО: Уведомление не исчезает само по себе при клике
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
