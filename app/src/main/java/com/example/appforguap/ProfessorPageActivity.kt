package com.example.appforguap

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ProfessorPageActivity : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var subjectsTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var buttonSubjects: Button
    private var subjectsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professor_page)

        nameTextView = findViewById(R.id.nameTextView)
        subjectsTextView = findViewById(R.id.subjectsTextView)
        imageView = findViewById(R.id.imageView)
        buttonSubjects = findViewById(R.id.buttonSubjects)

        val professorName = intent.getStringExtra("professor_name") ?: "Unknown"
        val subjects = intent.getStringArrayListExtra("professor_subjects") ?: ArrayList()

        nameTextView.text = professorName
        subjectsTextView.text = subjects.joinToString("\n")

        val imageUrl = intent.getStringExtra("professor_image_url") ?: ""
        loadImageWithRotation(this, imageUrl, imageView)

        buttonSubjects.setOnClickListener {
            toggleSubjectsVisibility()
        }
    }

    private fun toggleSubjectsVisibility() {
        subjectsVisible = !subjectsVisible
        if (subjectsVisible) {
            subjectsTextView.visibility = TextView.VISIBLE
        } else {
            subjectsTextView.visibility = TextView.GONE
        }
    }
}
