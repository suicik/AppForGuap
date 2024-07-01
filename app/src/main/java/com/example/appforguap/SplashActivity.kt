package com.example.appforguap

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        /*!1.Это нужно, чтобы было красивое приложение и статус бар с навигацией не выглядили
        страшно и встраивались в приложение*/
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //!1.Вот до этого момента

        Handler().postDelayed({
            val intent = Intent(this@SplashActivity, RegisterActivity ::class.java )
            startActivity(intent)
            finish()
        }, 2000)
    }
}