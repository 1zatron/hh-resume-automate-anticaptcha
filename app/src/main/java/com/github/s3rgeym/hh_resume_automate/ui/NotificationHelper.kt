package com.github.s3rgeym.hh_resume_automate.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Вызываем класс главного экрана приложения напрямую.
        // Используем имя пакета и класс программно, чтобы избежать ошибок импорта.
        val mainActivityClass = Class.forName("com.github.s3rgeym.hh_resume_automate.ui.MainActivity")
        val intent = Intent(context, mainActivityClass).apply {
            // Передаем ссылку на капчу внутрь интента
            putExtra("OPEN_CAPTCHA_URL", captchaUrl)
            action = Intent.ACTION_RUN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            intent,
            flags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Требуется ввести капчу hh.ru!")
            .setContentText("Нажмите здесь для прохождения проверки")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true) 
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
