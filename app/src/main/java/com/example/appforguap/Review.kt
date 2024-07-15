package com.example.appforguap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.auth.User

data class UserForReviw(
    val email: String = "",
    val group: String = ""
)
data class Review(
    val id: String = "",
    val professorId: String = "",
    val timestamp: String = "",
    val subject: String = "",
    val review: String = "",
    val uid: String = ""
) {

}

class ReviewsAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {



    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private fun getUserData(uid: String, callback: (String, String) -> Unit) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users/$uid")

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserForReviw::class.java)
                    if (user != null) {
                        callback(user.email, user.group)
                    } else {
                        callback("Unknown", "Unknown")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Error", "Error")
                }
            })
        }
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)
        private val reviewNumberTextView: TextView = itemView.findViewById(R.id.reviewNumberTextView)
        private val reviewSubjectTextView: TextView = itemView.findViewById(R.id.reviewSubjectTextView)
        fun bind(position: Int, review: Review) {
            reviewTextView.text = review.review
            reviewSubjectTextView.text = review.subject
            getUserData(review.uid) { userEmail, userGroup ->
                reviewNumberTextView.text = "Email: $userEmail, Группа: $userGroup"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(position, reviews[position])
    }

    override fun getItemCount(): Int = reviews.size
}

