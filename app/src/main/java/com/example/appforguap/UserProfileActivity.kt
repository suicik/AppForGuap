package com.example.appforguap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import androidx.core.view.isVisible


class UserProfileActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvGroup: TextView
    private lateinit var tvUserType: TextView
    private lateinit var btnLogout: Button
    private lateinit var reviewsLayout: LinearLayout
    private lateinit var btnToAdmin: Button
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
        reviewsLayout = findViewById(R.id.reviewsLayout)
        btnToAdmin = findViewById(R.id.to_admin_panel)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("Users")

        btnToAdmin.isVisible =false
        btnToAdmin.isEnabled = false

        loadUserProfile()

        btnToAdmin.setOnClickListener{
            startActivity(Intent(this@UserProfileActivity, AdminActivity::class.java))
        }
        btnLogout.setOnClickListener {
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


    }
    fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()

        // Очистка SharedPreferences
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
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
                            if (userType == "admin"){
                                btnToAdmin.isVisible =true
                                btnToAdmin.isEnabled = true
                            }
                            tvEmail.text = "Email: $email"
                            tvGroup.text = "Группа: $group"
                            tvUserType.text = "Права: ${userType.replaceFirstChar { it.uppercase() }}"

                            loadUserReviews(uid)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@UserProfileActivity, "Невозможно отобразить профиль", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun loadUserReviews(uid: String) {
        val booksReference = firebaseDatabase.getReference("Books")
        booksReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsLayout.removeAllViews() // Clear existing views

                if (snapshot.exists()) {
                    val reviewsMap = mutableMapOf<String, MutableList<Triple<String, String, String>>>()
                    val professorIds = mutableSetOf<String>()

                    for (bookSnapshot in snapshot.children) {
                        val reviewsNode = bookSnapshot.child("Reviews")
                        for (reviewSnapshot in reviewsNode.children) {
                            if (reviewSnapshot.child("uid").value == uid) {
                                val professorId = reviewSnapshot.child("professorId").value.toString()
                                val subject = reviewSnapshot.child("subject").value.toString()
                                val reviewText = reviewSnapshot.child("review").value.toString()
                                val reviewId = reviewSnapshot.key.toString()
                                reviewsMap.getOrPut(professorId) { mutableListOf() }
                                    .add(Triple(subject, reviewText, reviewId))
                                professorIds.add(professorId)
                            }
                        }
                    }

                    if (reviewsMap.isEmpty()) {
                        val noReviewsTextView = TextView(this@UserProfileActivity)
                        noReviewsTextView.text = "Отзывы не найдены"
                        reviewsLayout.addView(noReviewsTextView)
                    } else {
                        loadProfessorNames(professorIds, reviewsMap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfileActivity, "Ошибка при загрузке отзывов", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProfessorNames(
        professorIds: Set<String>,
        reviewsMap: Map<String, List<Triple<String, String, String>>>
    ) {
        val professorsReference = firebaseDatabase.getReference("Proffesors")
        professorsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsLayout.removeAllViews()
                for (professorId in professorIds) {
                    val professorSnapshot = snapshot.child(professorId)
                    if (professorSnapshot.exists()) {
                        val professorName = professorSnapshot.child("name").value.toString()
                        reviewsMap[professorId]?.forEach { (subject, reviewText, reviewId) ->
                            val reviewLayout = LinearLayout(this@UserProfileActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                gravity = Gravity.CENTER_VERTICAL // Center the buttons vertically
                            }

                            val reviewTextView = TextView(this@UserProfileActivity).apply {
                                text = "Преподаватель: $professorName\nДисциплина: $subject\nОтзыв: $reviewText"
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                            }

                            val editButton = ImageView(this@UserProfileActivity).apply {
                                setImageResource(R.drawable.button_edit)
                                layoutParams = LinearLayout.LayoutParams(
                                    70,
                                    70
                                )
                                setOnClickListener {
                                    // Handle edit action
                                    editReview(professorId, reviewId, subject, reviewText)
                                }
                            }

                            val deleteButton = ImageView(this@UserProfileActivity).apply {
                                setImageResource(R.drawable.button_delete)
                                layoutParams = LinearLayout.LayoutParams(
                                    70,
                                    70
                                ).apply {
                                    setMargins(16, 0, 0, 0) // Add left margin for spacing
                                }
                                setOnClickListener {
                                    // Handle delete action
                                    deleteReview(professorId, reviewId)
                                }
                            }

                            reviewLayout.addView(reviewTextView)
                            reviewLayout.addView(editButton)
                            reviewLayout.addView(deleteButton)

                            reviewsLayout.addView(reviewLayout)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfileActivity, "Failed to load professor names", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun editReview(professorId: String, reviewId: String, subject: String, reviewText: String) {
        // Create an EditText for input
        val editText = EditText(this).apply {
            setText(reviewText) // Pre-fill with the current review text
        }

        // Create an AlertDialog to get the new review text
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Редактировать отзыв")
            setMessage("Введите новый текст отзыва:")
            setView(editText) // Set the EditText as the dialog's view
            setPositiveButton("Сохранить") { dialog, _ ->
                val newReviewText = editText.text.toString()
                val reviewsReference = firebaseDatabase.getReference("Books").child(professorId).child("Reviews").child(reviewId)
                reviewsReference.child("review").setValue(newReviewText).addOnSuccessListener {
                    Toast.makeText(this@UserProfileActivity, "Отзыв успешно обновлен", Toast.LENGTH_SHORT).show()
                    loadUserProfile() // Refresh the user profile to show updated reviews
                }.addOnFailureListener { error ->
                    Toast.makeText(this@UserProfileActivity, "Не удалось обновить отзыв: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss() // Just close the dialog
            }
        }

        alertDialog.show() // Show the dialog
    }


    private fun deleteReview(professorId: String, reviewId: String) {
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Подтверждение")
            setMessage("Вы уверены, что хотите удалить отзыв?")
            setPositiveButton("Да") { dialog, _ ->
                val reviewsReference = firebaseDatabase.getReference("Books").child(professorId).child("Reviews").child(reviewId)
                reviewsReference.removeValue().addOnSuccessListener {
                    Toast.makeText(this@UserProfileActivity, "Отзыв успешно удален", Toast.LENGTH_SHORT).show()
                    loadUserProfile() // Refresh the user profile to show updated reviews
                }.addOnFailureListener { error ->
                    Toast.makeText(this@UserProfileActivity, "Не удалось удалить отзыв: ${error.message}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss() // Just close the dialog
            }
        }

        alertDialog.show() // Show the dialog
    }

}
