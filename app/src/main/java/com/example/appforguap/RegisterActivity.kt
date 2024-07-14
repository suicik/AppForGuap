package com.example.appforguap

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.appforguap.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var sharedPreferences: SharedPreferences
    private var email_ = ""
    private var password_ = ""
    private var group_ = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Setup SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Пожалуйста ожидайте")
        progressDialog.setCanceledOnTouchOutside(false)

        // Load saved credentials
        loadSavedCredentials()

        // Button visibility and behavior
        binding.buttonForRegister.isVisible = false
        binding.buttonForRegister.isEnabled = false

        binding.clickToRegister.setOnClickListener {
            toggleRegistrationView(true)
        }
        binding.backToSignIn.setOnClickListener {
            toggleRegistrationView(false)
        }

        // Registration button
        binding.buttonForRegister.setOnClickListener {
            validateData()
        }

        // Sign In button
        binding.buttonForSignIn.setOnClickListener {
            validateData()
        }

        if (intent.getBooleanExtra("LOGGED_OUT", false)) {
            clearFields()
        }
    }

    private fun validateData() {
        email_ = binding.etEmail.text.toString().trim()
        password_ = binding.etPassword.text.toString().trim()
        group_ = binding.etGroup.text.toString().trim()

        if (email_.isEmpty()) {
            Toast.makeText(this, "Введите email, поле пусто", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email_).matches()) {
            Toast.makeText(this, "Введите email, неправильный формат", Toast.LENGTH_SHORT).show()
        } else if (password_.isEmpty()) {
            Toast.makeText(this, "Введите пароль, поле пусто", Toast.LENGTH_SHORT).show()
        } else if (group_.isEmpty()) {
            Toast.makeText(this, "Введите группу, поле пусто", Toast.LENGTH_SHORT).show()
        } else {
            if (binding.buttonForRegister.isEnabled) {
                createUserAcc()
            } else {
                loginUser()
            }
        }
    }

    private fun loginUser() {
        progressDialog.setMessage("Logging in...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email_, password_)
            .addOnSuccessListener {
                checkUser()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Не удалось войти в аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }

    private fun createUserAcc() {
        progressDialog.setMessage("Создаем аккаунт...")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email_, password_)
            .addOnSuccessListener {
                updateUserInfo()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Не удалось создать аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUser() {
        progressDialog.setMessage("Checking User...")

        val firebaseUser = firebaseAuth.currentUser!!

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()
                    val userType = snapshot.child("userType").value
                    if (userType == "user") {
                        saveCredentialsIfChecked()
                        startActivity(Intent(this@RegisterActivity, ProfessorsActivity::class.java))
                        finish()
                    } else if (userType == "admin") {
                        saveCredentialsIfChecked()
                        startActivity(Intent(this@RegisterActivity, AdminActivity::class.java))
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateUserInfo() {
        progressDialog.setMessage("Заносим в базу Ваш аккаунт")
        val timestamp = System.currentTimeMillis()
        val uid_ = firebaseAuth.uid

        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid_
        hashMap["email"] = email_
        hashMap["password"] = password_
        hashMap["group"] = group_
        hashMap["userType"] = "user"
        hashMap["timestamp"] = timestamp

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid_!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                saveCredentialsIfChecked()
                Toast.makeText(this, "Аккаунт создан", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, ProfessorsActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Не удалось сохранить ваш аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCredentialsIfChecked() {
        if (binding.rememberEnter.isChecked) {
            val editor = sharedPreferences.edit()
            editor.putString("email", email_)
            editor.putString("password", password_)
            editor.putString("group", group_)
            editor.apply()
        }
    }

    private fun loadSavedCredentials() {
        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")
        val group = sharedPreferences.getString("group", "")
        binding.etEmail.setText(email)
        binding.etPassword.setText(password)
        binding.etGroup.setText(group)
        binding.rememberEnter.isChecked = email!!.isNotEmpty() && password!!.isNotEmpty() && group!!.isNotEmpty()
    }

    private fun toggleRegistrationView(isRegister: Boolean) {
        binding.buttonForSignIn.isVisible = !isRegister
        binding.buttonForSignIn.isEnabled = !isRegister
        binding.doNotHaveAccText.isVisible = !isRegister
        binding.clickToRegister.isVisible = !isRegister
        binding.clickToRegister.isEnabled = !isRegister
        binding.buttonForRegister.isVisible = isRegister
        binding.buttonForRegister.isEnabled = isRegister
    }

    private fun clearFields() {
        binding.etEmail.text.clear()
        binding.etPassword.text.clear()
        binding.etGroup.text.clear()
        binding.rememberEnter.isChecked = false // Uncheck the remember me checkbox
    }
}
