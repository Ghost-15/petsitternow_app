package www.com.petsitternow_app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 80

    /**
     * Compress an image URI and return the resulting bytes.
     * @param context Android context for content resolver
     * @param uri The image URI to compress
     * @return Compressed image as ByteArray
     */
    fun compress(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI")

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        var sampleSize = 1

        while (originalWidth / sampleSize > MAX_DIMENSION * 2 ||
            originalHeight / sampleSize > MAX_DIMENSION * 2
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val secondStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot reopen image URI")
        val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            ?: throw IllegalArgumentException("Cannot decode image")
        secondStream.close()

        val scaledBitmap = scaleBitmap(bitmap, MAX_DIMENSION)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        scaledBitmap.recycle()

        return outputStream.toByteArray()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
