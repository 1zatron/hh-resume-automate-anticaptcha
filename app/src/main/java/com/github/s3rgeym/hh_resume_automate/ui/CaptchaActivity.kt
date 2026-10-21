package com.github.s3rgeym.hh_resume_automate.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.github.s3rgeym.hh_resume_automate.R
import ru.s3rgeym.hhresumeautomate.data.network.CaptchaRepository

class CaptchaActivity : ComponentActivity() { // Изменено на ComponentActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captcha)

        val imageView = findViewById<ImageView>(R.id.captchaImageView)
        val editText = findViewById<EditText>(R.id.captchaEditText)
        val button = findViewById<Button>(R.id.submitButton)

        lifecycleScope.launch {
            CaptchaRepository.currentTask.collectLatest { task ->
                if (task != null) {
                    Glide.with(this@CaptchaActivity).load(task.imageUrl).into(imageView)
                } else {
                    finish()
                }
            }
        }

        button.setOnClickListener {
            val answer = editText.text.toString().trim()
            if (answer.isNotEmpty()) {
                CaptchaRepository.submitAnswer(answer)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CaptchaRepository.cancel()
    }
}
