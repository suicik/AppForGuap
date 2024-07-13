package com.example.appforguap

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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
    private lateinit var binding :ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private  lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        startActivity(Intent(this, ProfessorsActivity::class.java))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding = ActivityRegisterBinding.inflate((layoutInflater))
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        //Ожидание когда входим в аккаунт, чтобы был виден прогресс
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Пожалуйста ожидайте")
        progressDialog.setCanceledOnTouchOutside(false)

        //Кноки вход/ригстрация и их поведение
        binding.buttonForRegister.isVisible = false
        binding.buttonForRegister.isEnabled = false
        binding.clickToRegister.setOnClickListener{
            binding.buttonForSignIn.isVisible = false
            binding.buttonForSignIn.isEnabled = false
            binding.doNotHaveAccText.isVisible = false
            binding.clickToRegister.isVisible = false
            binding.clickToRegister.isEnabled = false
            binding.buttonForRegister.isVisible = true
            binding.buttonForRegister.isEnabled = true
        }
        binding.backToSignIn.setOnClickListener{
            binding.buttonForRegister.isVisible = false
            binding.buttonForRegister.isEnabled = false
            binding.buttonForSignIn.isVisible = true
            binding.buttonForSignIn.isEnabled = true
            binding.doNotHaveAccText.isVisible = true
            binding.clickToRegister.isVisible = true
            binding.clickToRegister.isEnabled = true
        }

        //Регистрация
        binding.buttonForRegister.setOnClickListener{
            /*Шаги
            1) Ввод данных
            2) Правильность введенного
            3) Создание аккаунта Firebase Auth
            4) Добавление в базу данных Firebase Database
            */
            validateData()
        }
        binding.buttonForSignIn.setOnClickListener{
            /*
            1. Ввод данных
            2) Правильность введенного
            3) Вход - Firebase Auth
            4) Проверка типа пользователя
             Если пользователь - то к просмотру преподов
             Если админ - то к управлению
             */
            validateData()
        }


    }



    private var email_ =""
    private var password_ =""
    private var group_ =""

    private fun validateData(){
        // Ввод данных
        email_ = binding.etEmail.text.toString().trim()
        password_ = binding.etPassword.text.toString().trim()
        group_ = binding.etGroup.text.toString().trim()

        //Валидность !!Потом добавить проверку по номеру группы
        if (email_.isEmpty()) {
            //Всплывающее окно Toast
            Toast.makeText(this, "Введите email, поле пусто", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email_).matches()){
            Toast.makeText(this, "Введите email, неправильный формат", Toast.LENGTH_SHORT).show()
        } else if (password_.isEmpty()){
            Toast.makeText(this, "Введите пароль, поле пусто", Toast.LENGTH_SHORT).show()
        } else if (group_.isEmpty()){
            Toast.makeText(this, "Введите группу, поле пусто", Toast.LENGTH_SHORT).show()
        } else {
            if (binding.buttonForRegister.isEnabled) {
                createUserAcc()
            } else{
                loginUser()
            }
        }
    }

    private fun loginUser() {
        progressDialog.setMessage("Logging in...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email_,password_)
            .addOnSuccessListener {
                checkUser()
            }
            .addOnFailureListener{e->
                Toast.makeText(this, "Не удалось войти в аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }

    private fun createUserAcc(){
        // Cоздание аккаунта Firebase Auth
        progressDialog.setMessage("Создаем аккаунт...")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email_,password_)
            .addOnSuccessListener {
                updateUserInfo()
            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this, "Не удалось создать аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUser(){
        progressDialog.setMessage("Checking User...")

        val firebaseUser = firebaseAuth.currentUser!!

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object  : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()
                    val userType = snapshot.child("userType").value
                    if (userType == "user"){
                        startActivity((Intent(this@RegisterActivity, ProfessorsActivity::class.java)))
                        finish()
                    }
                    else if (userType == "admin") {
                        startActivity((Intent(this@RegisterActivity, AdminActivity::class.java)))
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    private fun updateUserInfo() {
        //Добавление в базу данных Firebase Database
        progressDialog.setMessage("Заносим в базу Ваш аккаунт")
        val timestamp = System.currentTimeMillis()
        val uid_ = firebaseAuth.uid

        val hashMap: HashMap<String, Any?> =HashMap()
        hashMap["uid"] = uid_
        hashMap["email"] = email_
        hashMap["password"] = password_
        hashMap["group"] = group_
        hashMap["userType"] = "user"
        hashMap["timestamp"] = timestamp
        //Без понятия что ещё добавить

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid_!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Аккаунт создан", Toast.LENGTH_SHORT)
                startActivity(Intent(this@RegisterActivity, ProfessorsActivity::class.java))
                finish()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this, "Не удалось сохранить ваш аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}