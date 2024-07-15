package com.example.appforguap

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun checkUserLoggedIn(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userEmail = sharedPreferences.getString("email", null)
    val userPassword = sharedPreferences.getString("password", null)
    val userGroup = sharedPreferences.getString("group", null)
    return userEmail != null && userPassword != null && userGroup != null
}

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
            if (checkUserLoggedIn(this)) {
                // Пользователь уже вошел в систему
                // Перенаправьте пользователя на главный экран или выполнение другой логики
                startActivity(Intent(this@SplashActivity, ProfessorsActivity::class.java))
                finish()
            } else {
                // Перенаправьте пользователя на экран входа в систему
                startActivity(Intent(this@SplashActivity, RegisterActivity::class.java))
                finish()
            }
        }, 1000)
    }
}