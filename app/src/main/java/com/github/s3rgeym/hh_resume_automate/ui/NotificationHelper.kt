package com.github.s3rgeym.hh_resume_automate.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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

        // 1. ПРИНУДИТЕЛЬНЫЙ ВЫЗОВ БРАУЗЕРА ИЗ ГЛАВНОГО ПОТОКА (ОБХОД БЛОКИРОВКИ СЕТИ)
        // Мы отправляем команду на открытие страницы капчи прямо в Looper операционной системы.
        // Браузер откроется автоматически, даже если фоновый поток OkHttp сейчас жестко заблокирован.
        Handler(Looper.getMainLooper()).post {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(captchaUrl)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. СОЗДАНИЕ УВЕДОМЛЕНИЯ КАК ПОДСТРАХОВКИ
        // Если пользователь случайно свернет браузер, он сможет вернуться туда через уведомление
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(captchaUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val requestCode = System.currentTimeMillis().toInt()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, requestCode, intent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Требуется ввести капчу!")
            .setContentText("Браузер открыт автоматически. Нажмите, если страница закрылась.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
