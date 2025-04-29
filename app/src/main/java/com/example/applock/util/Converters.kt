package com.example.applock.util

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class Converters {
    @TypeConverter
    fun fromDrawable(drawable: Drawable): ByteArray {
        // Convert the Drawable to a Bitmap.
        val bitmap: Bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            // Ensure the drawable has a valid size.
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        // Convert the Bitmap to a ByteArray.
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @TypeConverter
    fun toDrawable(byteArray: ByteArray): Drawable {
        // Convert the ByteArray back to a Bitmap...
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        // ...and then wrap it in a Drawable.
        return BitmapDrawable(Resources.getSystem(), bitmap)
    }
}