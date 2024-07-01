package com.example.appforguap

import android.content.Context
import android.graphics.*
import android.widget.ImageView
import com.squareup.picasso.*


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
