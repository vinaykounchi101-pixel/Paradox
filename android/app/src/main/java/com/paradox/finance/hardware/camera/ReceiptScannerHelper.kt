package com.paradox.finance.hardware.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.paradox.finance.core.Resource
import com.paradox.finance.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ReceiptScannerHelper {

    private val api = ApiClient.getApi()

    fun downscaleBitmap(context: Context, imageUri: Uri, maxDimension: Int = 1280): ByteArray {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val width = originalBitmap.width
        val height = originalBitmap.height
        val maxDim = max(width, height)

        val scaledBitmap = if (maxDim > maxDimension) {
            val scale = maxDimension.toFloat() / maxDim
            Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    suspend fun scanReceipt(context: Context, imageUri: Uri): Resource<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val bytes = downscaleBitmap(context, imageUri)
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "receipt.jpg", requestBody)

            val response = api.scanReceipt(part)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Receipt OCR processing failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "OCR scan network error")
        }
    }
}
