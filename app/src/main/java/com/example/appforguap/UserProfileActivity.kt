package com.example.appforguap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class UserProfileActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvGroup: TextView
    private lateinit var tvUserType: TextView
    private lateinit var btnLogout: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        tvEmail = findViewById(R.id.tvEmail)
        tvGroup = findViewById(R.id.tvGroup)
        tvUserType = findViewById(R.id.tvUserType)
        btnLogout = findViewById(R.id.btnLogout)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("Users")

        loadUserProfile()

        btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun loadUserProfile() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            databaseReference.child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val email = snapshot.child("email").value.toString()
                            val group = snapshot.child("group").value.toString()
                            val userType = snapshot.child("userType").value.toString()

                            tvEmail.text = "Email: $email"
                            tvGroup.text = "Группа: $group"
                            tvUserType.text = "Права: ${userType.replaceFirstChar { it.uppercase() }}"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@UserProfileActivity, "Невозможно отобразить профиль", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}
