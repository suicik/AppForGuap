package com.example.appforguap

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appforguap.ProfessorsHelper.setupSpinner
import com.example.appforguap.databinding.ActivityProfessorPageBinding
import com.example.appforguap.databinding.DialogAddCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfessorPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfessorPageBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var reviewsAdapter: ReviewsAdapter
    private val reviewsList = mutableListOf<Review>()
    private var review = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfessorPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Инициализация ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setCancelable(false)
        }

        // Загрузка данных профессора
        val professorName = intent.getStringExtra("professor_name") ?: "Unknown"
        binding.nameTextView.text = professorName

        val imageUrl = intent.getStringExtra("professor_image_url") ?: ""
        loadImageWithRotation(this, imageUrl, binding.imageView)

        // Инициализация RecyclerView
        reviewsAdapter = ReviewsAdapter(reviewsList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = reviewsAdapter

        // Загрузка отзывов
        loadReviews()

        binding.buttonSubjects.setOnClickListener {
            addReviewDialog()
        }
    }

    private fun loadReviews() {
        val id = intent.getStringExtra("professor_id") ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("Books").child(id).child("Reviews")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()
                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(Review::class.java)
                    review?.let { reviewsList.add(it) }
                }
                reviewsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfessorPageActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addReviewDialog() {
        val reviewAddBinding = DialogAddCommentBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(reviewAddBinding.root)

        val alertDialog = builder.create()
        alertDialog.show()
        reviewAddBinding.imageView2.setOnClickListener { alertDialog.dismiss() }

        val subjects = intent.getStringArrayListExtra("professor_subjects") ?: ArrayList()
        setupSpinner(this, reviewAddBinding.spinner, subjects)

        reviewAddBinding.button2.setOnClickListener {
            review = reviewAddBinding.commentEditText.text.toString().trim()
            if (review.isEmpty()) {
                Toast.makeText(this, "Введите текст", Toast.LENGTH_SHORT).show()
            } else {
                alertDialog.dismiss()
                addReviewToFirebase()
            }
        }
    }

    private fun addReviewToFirebase() {
        progressDialog.setMessage("Добавляем отзыв")
        progressDialog.show()

        val id = intent.getStringExtra("professor_id") ?: ""
        val timestamp = "${System.currentTimeMillis()}"
        val hashMap = HashMap<String, Any>().apply {
            put("id", timestamp)
            put("professorId", id)
            put("timestamp", timestamp)
            put("review", review)
            put("uid", "${firebaseAuth.uid}")
        }

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(id).child("Reviews").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Отзыв добавлен", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
