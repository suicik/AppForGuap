package com.example.appforguap

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appforguap.databinding.ActivityAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AdminActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityAdminBinding
    private lateinit var  firebaseAuth: FirebaseAuth
    private  lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Ожидайте")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.addFiltersToBd.setOnClickListener{
            parseFilters()
        }
    }

    private fun parseFilters() {
        extractFilters()
    }

    private fun extractFilters() {
        progressDialog.show()
        Thread {
            val doc: Document = Jsoup.connect("https://pro.guap.ru/professors").get()
            extractFilterOptions(doc, "#position")
            extractFilterOptions(doc, "#facultyWithChairs")
            extractFilterOptions(doc, "#subunit")
        }.start()
        progressDialog.dismiss()
    }
    // Парсинг фильтров
    private fun extractFilterOptions(doc: Document, selector: String){

        val elements = doc.select("$selector option")

        for (element in elements) {
            val timestamp = System.currentTimeMillis()
            val hashMap = HashMap<String, Any>()
            hashMap["id"] = "$timestamp"
            hashMap["value"] = element.attr("value").trim()
            hashMap["text"] = element.text().trim()
            hashMap["timestamp"] = timestamp
            hashMap["uid"] = "${firebaseAuth.uid}"

            val ref = FirebaseDatabase.getInstance().getReference(selector.drop(1))
            ref.child("$timestamp")
                .setValue(hashMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Успешный вход", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {e->
                    Toast.makeText(this, "Не удалось сохранить ваш аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}