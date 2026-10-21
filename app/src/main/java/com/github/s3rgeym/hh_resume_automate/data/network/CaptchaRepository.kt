package ru.s3rgeym.hhresumeautomate.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object CaptchaRepository {
    private val _currentTask = MutableStateFlow<CaptchaTask?>(null)
    val currentTask = _currentTask.asStateFlow()

    private var latch: CountDownLatch? = null
    @Volatile var captchaAnswer: String? = null
        private set

    data class CaptchaTask(val imageUrl: String, val requestId: String)

    fun waitForUserResponse(imageUrl: String, requestId: String): String? {
        captchaAnswer = null
        latch = CountDownLatch(1)
        _currentTask.value = CaptchaTask(imageUrl, requestId)
        
        // Ждем ввода от пользователя максимум 2 минуты, затем сбрасываем
        latch?.await(2, TimeUnit.MINUTES)
        _currentTask.value = null
        return captchaAnswer
    }

    fun submitAnswer(answer: String) {
        captchaAnswer = answer
        latch?.countDown()
    }

    fun cancel() {
        captchaAnswer = null
        latch?.countDown()
    }
}
