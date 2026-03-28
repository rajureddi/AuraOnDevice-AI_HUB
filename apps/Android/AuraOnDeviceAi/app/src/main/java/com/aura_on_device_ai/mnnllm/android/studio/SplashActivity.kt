package com.aura_on_device_ai.mnnllm.android.studio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aura_on_device_ai.mnnllm.android.R
import com.aura_on_device_ai.mnnllm.android.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.iv_splash_logo)
        val title = findViewById<TextView>(R.id.tv_splash_title)

        // Triple-Animation: Scale + Fade + Pulse
        val scaleAnim = ScaleAnimation(
            0.8f, 1.05f, 0.8f, 1.05f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1500
            fillAfter = true
        }

        val fadeAnim = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 1200
        }

        val animSet = AnimationSet(true).apply {
            addAnimation(scaleAnim)
            addAnimation(fadeAnim)
        }

        logo.startAnimation(animSet)
        title.startAnimation(fadeAnim)

        // 2-second transition to Main Dashboard
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // Sleek cross-fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2000)
    }
}

