package com.example.appforguap

import android.app.ProgressDialog
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        binding.addPrepods.setOnClickListener{
            Thread{
                parseAllProfessors()
            }.start()
        }

        binding.addGroupsToBd.setOnClickListener {
            extractGroupOptions()
        }

        binding.logOutAdmin.setOnClickListener {
            signOut(this)
            Toast.makeText(this, "Выход прошёл успешно", Toast.LENGTH_SHORT).show()

            // Start RegisterActivity with a flag indicating logout
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("LOGGED_OUT", true) // Pass the flag
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
            }

            startActivity(intent)
            finish()
        }
        binding.toProfessorsAdmin.setOnClickListener{
            startActivity(Intent(this@AdminActivity, ProfessorsActivity::class.java))
        }


    }
    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()

        // Очистка SharedPreferences
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
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
            if (element.attr("value").trim() == "0")
                continue
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

    // Информация для фильтров
    data class Position(
        val department: String,
        val title: String,
        val institute: String
    )

    // Парсинг страниц
    private fun parseAllProfessors() {
        val baseUrl = "https://pro.guap.ru/professors?page="
        var page = 1
        var isMorePages = true

        while (isMorePages) {
            val url = "$baseUrl$page"
            println("Parsing page: $page")

            try {
                val doc: Document = Jsoup.connect(url).get()
                val professorElements: Elements = doc.select("div.card.shadow-sm.my-sm-2")

                val jobs = professorElements.map { element ->
                    val timestamp = System.currentTimeMillis()
                    val hashMap = HashMap<String, Any>()
                    hashMap["id"] = "$timestamp"

                    val nameElement = element.selectFirst("h5 a")
                    hashMap["name"] = nameElement?.text()?.trim() ?: "Unknown"

                    val profileUrl = nameElement?.attr("href")?.let { "https://pro.guap.ru$it" } ?: "Unknown"
                    hashMap["profileUrl"] = profileUrl

                    val imageElement = element.selectFirst("img.profile_image")
                    hashMap["imageUrl"] = imageElement?.attr("src")?.let { "https://pro.guap.ru$it" } ?: "Unknown"

                    val positions =parseProfessorPage(profileUrl)
                    hashMap["positions"] = positions.first
                    hashMap["subjects"] = positions.second
                    hashMap["timestamp"] = timestamp
                    hashMap["uid"] = "${firebaseAuth.uid}"
                    val ref = FirebaseDatabase.getInstance().getReference("Proffesors")
                    ref.child("$timestamp")
                        .setValue(hashMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Успешный вход", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {e->
                            Toast.makeText(this, "Не удалось сохранить ваш аккаунт из за ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                if (jobs.isEmpty()) {
                    isMorePages = false
                } else {
                    page++
                }
            } catch (e: IOException) {
                println("Error parsing page $url: ${e.message}")
            }
        }

    }
    // Парсинг каждого преподавателя
    private fun parseProfessorPage(url: String): Pair<List<Position>, List<String>> {
        return try {
            val doc: Document = Jsoup.connect(url).get()

            // Selecting position elements
            val positionElements: Elements =
                doc.select("div.card.shadow-sm div.card-body div.list-group-item")
            val positions = mutableListOf<Position>()

            // Parsing position elements
            for (element in positionElements) {
                val department =
                    element.selectFirst("div.small.text-end.text-muted.mb-1")?.text()?.trim() ?: ""
                val title = element.selectFirst("h5.fw-semibold.my-1")?.text()?.trim() ?: ""
                val institute =
                    element.selectFirst("div.small:not(.text-end)")?.text()?.trim() ?: ""

                if (department.isNotEmpty() && title.isNotEmpty() && institute.isNotEmpty()) {
                    val newPosition = Position(department, title, institute)
                    positions.add(newPosition)
                }
            }

            // Selecting subject elements
            val subjectElements: Elements = doc.select("div#subjects div.list-group-item")
            val subjects = subjectElements.map { it.text().trim() }

            Pair(positions.toList(), subjects)
        } catch (e: IOException) {
            println("Ошибка при парсинге страницы преподавателя $url: ${e.message}")
            Pair(emptyList(), emptyList())
        }
    }

    private fun extractGroupOptions() {
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://raspsess.guap.ru"
                val selector = "select[name='ctl00\$cphMain\$ctl03'] option"
                val document = Jsoup.connect(url).get()
                val groups = document.select(selector)

                for (group in groups) {
                    val timestamp = System.currentTimeMillis()
                    val hashMap = HashMap<String, Any>()
                    if (group.attr("value").trim() == "-1")
                        continue
                    hashMap["id"] = "$timestamp"
                    hashMap["value"] = group.attr("value").trim()
                    hashMap["text"] = group.text().trim()
                    hashMap["timestamp"] = timestamp
                    hashMap["uid"] = "${firebaseAuth.uid}"

                    val ref = FirebaseDatabase.getInstance().getReference("Groups")
                    ref.child("$timestamp").setValue(hashMap).addOnSuccessListener {
                        // Optionally show success message for each group
                    }.addOnFailureListener { e ->
                        // Handle failure
                        println("Failed to add group: ${e.message}")
                    }
                }
            } catch (e: IOException) {
                println("Error fetching groups: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }

}