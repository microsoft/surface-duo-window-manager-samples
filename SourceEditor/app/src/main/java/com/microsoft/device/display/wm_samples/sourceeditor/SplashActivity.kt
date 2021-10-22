package com.microsoft.device.display.wm_samples.sourceeditor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        // move to the app straight away
        startActivity(Intent(this, MainActivity::class.java))
    }
}
