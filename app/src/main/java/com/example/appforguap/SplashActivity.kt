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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

fun checkUserLoggedIn(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userEmail = sharedPreferences.getString("email", null)
    val userPassword = sharedPreferences.getString("password", null)
    val userGroup = sharedPreferences.getString("group", null)
    return userEmail != null && userPassword != null && userGroup != null
}

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
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
                checkUserType { userType ->
                    if (userType == "admin") {
                        startActivity(Intent(this@SplashActivity, AdminActivity::class.java))
                    } else if (userType == "user") {
                        startActivity(Intent(this@SplashActivity, ProfessorsActivity::class.java))
                    } else {
                        // При необходимости можно обработать неопределенный тип пользователя
                        startActivity(Intent(this@SplashActivity, RegisterActivity::class.java))
                    }
                    finish()
                }
            } else {
                // Перенаправьте пользователя на экран входа в систему
                startActivity(Intent(this@SplashActivity, RegisterActivity::class.java))
                finish()
            }
        }, 1000)
    }
    private fun checkUserLoggedIn(): Boolean {
        val user = auth.currentUser
        return user != null
    }

    private fun checkUserType(callback: (String?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = databaseReference.child("Users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userType = snapshot.child("userType").getValue(String::class.java)
                    callback(userType)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                    println("Ошибка при работе с базой данных: ${error.message}")
                }
            })
        } else {
            callback(null)
        }
    }
}