package net.sourceforge.moonstone.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Utility for loading app icons from the file system.
 */
object AppIconLoader {
    /**
     * Load a bitmap from a file.
     *
     * @param file The image file to load
     * @return The loaded Bitmap, or null if loading fails
     */
    fun loadBitmap(file: File): Bitmap? {
        return try {
            if (!file.exists() || !file.isFile) return null

            // Read bytes first — avoids BitmapFactory.decodeFile silently failing
            // on scoped-storage paths (Android 10+).
            val bytes = file.readBytes()
            if (bytes.isEmpty()) return null

            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, 256, 256)
            options.inJustDecodeBounds = false

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate optimal sample size for downscaling images.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
