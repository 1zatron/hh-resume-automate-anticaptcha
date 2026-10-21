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

        // Создаем канал уведомлений для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Проверка капчи",
                NotificationManager.IMPORTANCE_HIGH // HIGH обязателен, чтобы уведомление всплывало сверху
            ).apply {
                description = "Уведомления о необходимости ввода капчи HeadHunter"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Интент, который сработает при нажатии на уведомление.
        // Он откроет системный браузер (или WebView) прямо со страницей капчи от HH.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(captchaUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Флаг FLAG_IMMUTABLE обязателен для Android 12+
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Строим само уведомление
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Можете заменить на свою иконку приложения
            .setContentTitle("Требуется ввести капчу!")
            .setContentText("HeadHunter заблокировал запрос. Нажмите сюда, чтобы пройти проверку.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true) // Чтобы уведомление исчезало после клика по нему
            .setContentIntent(pendingIntent) // Привязываем наше действие клика

        // Показываем уведомление
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
