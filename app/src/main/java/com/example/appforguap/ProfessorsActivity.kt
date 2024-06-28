package com.example.appforguap

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.*
import android.graphics.*
import android.content.Context


import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

class ProfessorsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professors)

        enableEdgeToEdge()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        GlobalScope.launch(Dispatchers.IO) {
            val allProfessors = parseAllProfessors()

            launch(Dispatchers.Main) {
                val adapter = ProfessorsAdapter(allProfessors)
                recyclerView.adapter = adapter
            }
        }
    }
}

// Адаптер для RecyclerView
class ProfessorsAdapter(private val professors: List<Professor>) :
    RecyclerView.Adapter<ProfessorsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_professor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val professor = professors[position]

        val formattedPositions = professor.positions.joinToString("\n") {
            "${it.title}, ${it.department}"
        }

        holder.nameTextView.text = professor.name
        holder.positionsTextView.text = formattedPositions
        loadImageWithRotation(holder.imageView.context, professor.imageUrl, holder.imageView)
    }

    override fun getItemCount(): Int {
        return professors.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val positionsTextView: TextView = itemView.findViewById(R.id.positionsTextView)
    }
}
// Трансформация изображения (Можно вынести в отдельный файл)
fun loadImageWithRotation(context: Context, imageUrl: String, imageView: ImageView) {
    Picasso.get()
        .load(imageUrl)
        .transform(RoundedCornersTransformation(75))
        .into(imageView)
}
// Функция для округления изображения преподавателей (Можно вынести в отдельный файл)
class RoundedCornersTransformation(private val radius: Int) : Transformation {

    override fun key(): String {
        return "rounded_corners(radius=$radius)"
    }

    override fun transform(source: Bitmap): Bitmap {
        val minSize = minOf(source.width, source.height)

        val scale = radius.toFloat() * 2 / minSize.toFloat()

        val scaledBitmap = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt(),
            (source.height * scale).toInt(),
            true
        )

        val output = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)

        val paint = Paint()
        paint.isAntiAlias = true


        val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        canvas.drawRoundRect(rect, radius.toFloat(), radius.toFloat(), paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(
            scaledBitmap,
            (output.width - scaledBitmap.width) / 2.toFloat(),
            (output.height - scaledBitmap.height) / 2.toFloat(),
            paint
        )

        scaledBitmap.recycle()
        if (source != output) {
            source.recycle()
        }

        return output
    }
}