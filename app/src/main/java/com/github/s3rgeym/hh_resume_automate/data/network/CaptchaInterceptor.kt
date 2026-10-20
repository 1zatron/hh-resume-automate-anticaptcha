package ru.s3rgeym.hhresumeautomate.data.network

import android.content.Context
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import ru.s3rgeym.hhresumeautomate.utils.NotificationHelper

class CaptchaInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 429 || response.code == 403) {
            val bodyString = response.peekBody(Long.MAX_VALUE).string()
            
            if (bodyString.contains("captcha_required")) {
                val json = JSONObject(bodyString)
                val captchaUrl = json.optString("captcha_url")
                val requestId = captchaUrl.substringAfter("request_id=").substringBefore("&")

                NotificationHelper.showCaptchaNotification(context)

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
