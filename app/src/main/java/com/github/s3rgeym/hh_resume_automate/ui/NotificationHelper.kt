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

        // Создаем канал уведомлений с максимальным приоритетом (для Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Проверка капчи",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Требуется ввод капчи на hh.ru"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Прямой Intent операционной системе на запуск веб-ссылки
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(captchaUrl)).apply {
            // Флаг NEW_TASK обязателен, чтобы открыть стороннее приложение (браузер) из фонового контекста
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Задаем уникальный requestCode на основе текущего времени, 
        // чтобы Android не переиспользовал старые ссылки из системного кэша
        val requestCode = System.currentTimeMillis().toInt()

        // Используем FLAG_IMMUTABLE, чтобы избежать крашей на Android 14+ (U+)
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

        // Собираем стандартную плашку уведомления
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Требуется ввести капчу!")
            .setContentText("Нажмите здесь, чтобы открыть страницу проверки hh.ru")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true) // Уведомление исчезнет из шторки сразу после успешного клика
            .setContentIntent(pendingIntent) // Привязываем команду к клику

        // Отправляем уведомление в систему
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
