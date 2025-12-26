package com.idcard.ocr.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Utility class for Base64 encoding/decoding
 */
object Base64Helper {

    /**
     * Convert Bitmap to Base64 string
     *
     * @param bitmap Source bitmap
     * @param quality JPEG quality (0-100)
     * @return Base64 encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Convert byte array to Base64 string
     *
     * @param byteArray Source byte array
     * @return Base64 encoded string
     */
    fun byteArrayToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Convert Base64 string to Bitmap
     *
     * @param base64String Base64 encoded string
     * @return Decoded Bitmap or null if conversion fails
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val cleanString = base64String.replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
                .replace("\n", "")

            val decodedBytes = Base64.decode(cleanString, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert Base64 string to byte array
     *
     * @param base64String Base64 encoded string
     * @return Decoded byte array or empty array if conversion fails
     */
    fun base64ToByteArray(base64String: String): ByteArray {
        return try {
            val cleanString = base64String.replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
                .replace("\n", "")

            Base64.decode(cleanString, Base64.NO_WRAP)
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    /**
     * Compress and resize bitmap for optimal upload size
     *
     * @param bitmap Source bitmap
     * @param maxDimension Maximum dimension (width or height)
     * @param quality JPEG quality
     * @return Resized and compressed bitmap
     */
    fun compressBitmap(bitmap: Bitmap, maxDimension: Int = 1920, quality: Int = 80): Bitmap {
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
