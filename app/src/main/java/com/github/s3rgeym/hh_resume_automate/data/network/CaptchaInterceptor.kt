package ru.s3rgeym.hhresumeautomate.data.network

import android.app.Application
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import com.github.s3rgeym.hh_resume_automate.ui.NotificationHelper

class CaptchaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 429 || response.code == 403) {
            val bodyString = response.peekBody(Long.MAX_VALUE).string()
            if (bodyString.contains("captcha_required")) {
                val json = JSONObject(bodyString)
                val captchaUrl = json.optString("captcha_url")
                val requestId = captchaUrl.substringAfter("request_id=").substringBefore("&")

                // Получаем контекст приложения напрямую через рефлексию (безопасно для Android)
                val activityThread = Class.forName("android.app.ActivityThread")
                val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
                val context = activityThread.getMethod("getApplication").invoke(currentActivityThread) as Application

                NotificationHelper.showCaptchaNotification(context, captchaUrl)

                val answer = CaptchaRepository.waitForUserResponse(captchaUrl, requestId)

                if (!answer.isNullOrBlank()) {
                    val isSuccess = submitCaptchaToHH(chain, requestId, answer)
                    if (isSuccess) {
                        return chain.proceed(request)
                    }
                }
            }
        }
        return response
    }

    private fun submitCaptchaToHH(chain: Interceptor.Chain, requestId: String, answer: String): Boolean {
        val formBody = FormBody.Builder()
            .add("request_id", requestId)
            .add("captcha_answer", answer)
            .build()

        val captchaRequest = Request.Builder()
            .url("https://hh.ru")
            .post(formBody)
            .build()

        return try {
            val res = chain.proceed(captchaRequest)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
