package com.example.appforguap

import android.content.Context
import android.graphics.*
import android.widget.ImageView
import android.media.ExifInterface
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import java.io.IOException
import java.net.URL
import kotlinx.coroutines.*
import java.io.InputStream



fun loadImageWithRotation(context: Context, imageUrl: String, imageView: ImageView) {
    CoroutineScope(Dispatchers.Main).launch {
        val rotationAngle = withContext(Dispatchers.IO) {
            getExifRotationAngle(imageUrl)
        }

        Picasso.get()
            .load(imageUrl)
            .transform(RotationTransformation(rotationAngle))
            .transform(RoundedCornersTransformation(75))
            .into(imageView)
    }
}

private suspend fun getExifRotationAngle(imageUrl: String): Float {
    return try {
        withContext(Dispatchers.IO) {
            val inputStream: InputStream = URL(imageUrl).openStream()
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        0f
    }
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

class RotationTransformation(private val rotationAngle: Float) : Transformation {

    override fun key(): String {
        return "rotate_$rotationAngle"
    }

    override fun transform(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationAngle)

        val rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        if (source != rotatedBitmap) {
            source.recycle()
        }

        return rotatedBitmap
    }
}
