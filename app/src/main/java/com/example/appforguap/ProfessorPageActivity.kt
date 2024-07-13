package com.example.appforguap

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import com.example.appforguap.ProfessorsHelper.setupSpinner
import com.example.appforguap.databinding.ActivityAdminBinding
import com.example.appforguap.databinding.ActivityProfessorPageBinding
import com.example.appforguap.databinding.DialogAddCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfessorPageActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityProfessorPageBinding
    private lateinit var  firebaseAuth: FirebaseAuth
    private  lateinit var progressDialog: ProgressDialog
    private var subjectsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =ActivityProfessorPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val professorName = intent.getStringExtra("professor_name") ?: "Unknown"
        //val subjects = intent.getStringArrayListExtra("professor_subjects") ?: ArrayList()

        binding.nameTextView.text = professorName
        //binding.subjectsTextView.text = subjects.joinToString("\n")

        val imageUrl = intent.getStringExtra("professor_image_url") ?: ""
        loadImageWithRotation(this, imageUrl, binding.imageView)

        binding.buttonSubjects.setOnClickListener{
            addCommentDialog()
        }

//        buttonSubjects.setOnClickListener {
//            toggleSubjectsVisibility()
//        }
    }

    private var comment = ""

    private fun addCommentDialog() {
        val commentaddBinding = DialogAddCommentBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentaddBinding.root)

        val alertDialog =builder.create()
        alertDialog.show()
        commentaddBinding.imageView2.setOnClickListener{alertDialog.dismiss()}
        val subjects = intent.getStringArrayListExtra("professor_subjects") ?: ArrayList()
        setupSpinner(this, commentaddBinding.spinner, subjects)
        commentaddBinding.button2.setOnClickListener{
            comment = commentaddBinding.commentEditText.text.toString().trim()
            if (comment.isEmpty()){
                Toast.makeText(this,"Введите текст", Toast.LENGTH_SHORT).show()
            } else {
                alertDialog.dismiss()
                addCommentToFirebase()
            }
        }
        
    }

    private fun addCommentToFirebase() {
        progressDialog.setMessage("Добавляем комментарий")
        progressDialog.show()
        val id = intent.getStringExtra("professor_id") ?: ""
        val timestamp = "${System.currentTimeMillis()}"
        val hashMap =HashMap<String,Any>()
        hashMap["id"] ="$timestamp"
        hashMap["proffessorId"] = id
        hashMap["timestamp"] ="$timestamp"
        hashMap["comment"] = comment
        hashMap["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(id).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"OKKKK To ADD COmment", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this,"Failed to add comment из за ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

//    private fun toggleSubjectsVisibility() {
//        subjectsVisible = !subjectsVisible
//        if (subjectsVisible) {
//            subjectsTextView.visibility = TextView.VISIBLE
//        } else {
//            subjectsTextView.visibility = TextView.GONE
//        }
//    }
}
