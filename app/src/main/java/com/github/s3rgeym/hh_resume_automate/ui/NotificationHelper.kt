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

        // Настройка канала уведомлений для Android 8.0+
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

        // ЯВНЫЙ ИНТЕНТ: Открываем внутреннее окно CaptchaActivity из вашего проекта.
        // Android разрешает фоновым сервисам открывать экраны своего же приложения.
        val intent = Intent(context, CaptchaActivity::class.java).apply {
            putExtra("captcha_url", captchaUrl) // Передаем ссылку на капчу для отображения
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // Генерируем уникальный requestCode на основе текущего времени, 
        // чтобы система не кэшировала старые ссылки
        val requestCode = System.currentTimeMillis().toInt()

        // Используем FLAG_IMMUTABLE для корректной работы на Android 14+ (U+)
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
            .setContentText("Нажмите здесь, чтобы открыть окно ввода капчи")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true) // Уведомление останется в шторке, пока вы не нажмете
            .setAutoCancel(true) // Уведомление закроется сразу после клика
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
