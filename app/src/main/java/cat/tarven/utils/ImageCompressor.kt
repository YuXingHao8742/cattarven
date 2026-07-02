package cat.tarven.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 图片压缩工具 — 将图片文件压缩编码为 Base64 字符串
 * 用于多模态 API 请求中的图片附件发送
 */
object ImageCompressor {

    /**
     * 压缩并缩放图片，返回 JPEG 的 Base64 编码，防止图片过大导致 HTTP 413
     *
     * 使用 try-finally 确保 Bitmap 在异常情况下也能正确回收，防止内存泄漏。
     */
    fun getCompressedImageBase64(filePath: String): String? {
        var bitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        try {
            // 第一步：仅读取图片尺寸
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, boundsOptions)

            // 第二步：计算采样率
            var scale = 1
            while (boundsOptions.outWidth / scale / 2 >= 1024 && boundsOptions.outHeight / scale / 2 >= 1024) {
                scale *= 2
            }

            // 第三步：按采样率解码
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            bitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null

            // 第四步：如果仍然超过 1024px，再缩放一次
            val maxDim = 1024f
            val width = bitmap.width
            val height = bitmap.height
            scaledBitmap = if (width > maxDim || height > maxDim) {
                val ratio = minOf(maxDim / width, maxDim / height)
                Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
            } else {
                null // 不需要缩放，直接使用 bitmap
            }

            // 第五步：压缩为 JPEG 并编码为 Base64
            val finalBitmap = scaledBitmap ?: bitmap
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()

            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            // 确保 Bitmap 始终被回收，即使中间发生异常
            scaledBitmap?.recycle()
            bitmap?.recycle()
        }
    }
}
