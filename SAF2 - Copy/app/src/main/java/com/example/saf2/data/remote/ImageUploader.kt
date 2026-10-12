package com.example.saf2.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Fill these in after creating a free Cloudinary account (see the setup steps):
 *  - CLOUD_NAME:    shown at the top of your Cloudinary dashboard
 *  - UPLOAD_PRESET: the name of an UNSIGNED upload preset (Settings > Upload > Upload presets)
 *
 * These two values are not secrets (an unsigned preset has no API secret in the app).
 * Restrict the preset in Cloudinary to images only and a max file size.
 */
object CloudinaryConfig {
    val CLOUD_NAME: String = "YOUR_CLOUD_NAME"
    val UPLOAD_PRESET: String = "YOUR_UNSIGNED_PRESET"
}

object ImageUploader {

    /** False until the two values above have been filled in. */
    val isConfigured: Boolean
        get() = CloudinaryConfig.CLOUD_NAME != "YOUR_CLOUD_NAME" &&
                CloudinaryConfig.UPLOAD_PRESET != "YOUR_UNSIGNED_PRESET"

    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 80

    /** Uploads one photo and returns its public https link. Throws with a readable message on failure. */
    suspend fun upload(context: Context, uri: Uri, folder: String): String =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                throw IllegalStateException("Photo hosting is not set up yet (see CloudinaryConfig).")
            }
            val jpeg = compress(context, uri)
            post(jpeg, folder)
        }

    /** Shrinks to at most 1280px on the long side, fixes phone-camera rotation, saves as JPEG. */
    private fun compress(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Could not read that image.")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IOException("Could not read that image.")

        val scale = min(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            val orientation = try {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val out = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return out.toByteArray()
    }

    private fun post(bytes: ByteArray, folder: String): String {
        val boundary = "----SAF" + System.currentTimeMillis()
        val url = URL("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            DataOutputStream(connection.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
                }
                field("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                field("folder", folder)
                out.writeBytes(
                    "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n" +
                            "Content-Type: image/jpeg\r\n\r\n"
                )
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val reason = try {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                } catch (e: Exception) {
                    null
                }
                throw IOException("Photo upload failed ($code): ${reason ?: body.take(120)}")
            }
            return JSONObject(body).getString("secure_url")
        } finally {
            connection.disconnect()
        }
    }
}