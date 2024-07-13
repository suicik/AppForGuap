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
    fun getUserReview(userUid: String, callback: (Review?) -> Unit) {
        val id = intent.getStringExtra("professor_id") ?: ""
        val reviewsRef = FirebaseDatabase.getInstance().getReference("Books").child(id).child("Reviews")
        reviewsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userReview: Review? = null
                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(Review::class.java)
                    if (review?.uid == userUid) {
                        userReview = review
                        break
                    }
                }
                callback(userReview)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }
    private fun addReviewDialog() {
        val id = intent.getStringExtra("professor_id") ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("Books").child(id).child("Reviews")
        val reviewAddBinding = DialogAddCommentBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(reviewAddBinding.root)

        val alertDialog = builder.create()
        alertDialog.show()
        reviewAddBinding.imageView2.setOnClickListener { alertDialog.dismiss() }

        val subjects = intent.getStringArrayListExtra("professor_subjects") ?: ArrayList()
        setupSpinner(this, reviewAddBinding.spinner, subjects)
        reviewAddBinding.button2.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val selectedSubject = reviewAddBinding.spinner.selectedItem.toString()
                getUserReview(user.uid) { userReview ->
                    if (userReview == null || userReview.subject != selectedSubject) {
                        // Текущий пользователь не оставлял отзыв или отзыв с другим предметом, можно продолжить
                        val review = reviewAddBinding.commentEditText.text.toString().trim()

                        if (review.isEmpty()) {
                            Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
                        } else {
                            alertDialog.dismiss()
                            addReviewToFirebase(review, selectedSubject)
                        }
                    } else {
                        // Текущий пользователь уже оставлял отзыв для данного предмета, уведомляем об этом
                        Toast.makeText(this, "Вы уже оставляли отзыв для этого предмета", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                // Если пользователь не авторизован, можно показать сообщение или выполнить другую логику
                Toast.makeText(this, "Пожалуйста, авторизуйтесь", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addReviewToFirebase(review: String, selectedSubject: String) {
        progressDialog.setMessage("Добавляем отзыв")
        progressDialog.show()

        val id = intent.getStringExtra("professor_id") ?: ""
        val timestamp = "${System.currentTimeMillis()}"
        val hashMap = HashMap<String, Any>().apply {
            put("id", timestamp)
            put("professorId", id)
            put("subject", selectedSubject)
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
