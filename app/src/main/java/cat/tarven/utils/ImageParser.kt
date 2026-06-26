package cat.tarven.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

object ImageParser {
    /**
     * 从 PNG 图片输入流中提取 `tEXt` 块里隐藏的角色 JSON 数据 (SillyTavern 标准)
     */
    fun extractCharacterJsonFromPng(inputStream: InputStream): String? {
        try {
            val signature = ByteArray(8)
            if (inputStream.read(signature) != 8) return null
            
            // 验证 PNG 签名
            val pngSignature = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            )
            if (!signature.contentEquals(pngSignature)) return null

            val lengthBuffer = ByteArray(4)
            val typeBuffer = ByteArray(4)

            while (true) {
                if (inputStream.read(lengthBuffer) != 4) break
                if (inputStream.read(typeBuffer) != 4) break

                val length = ByteBuffer.wrap(lengthBuffer).int
                if (length < 0) break // 防止溢出或异常
                
                val type = String(typeBuffer, StandardCharsets.US_ASCII)

                if (type == "tEXt" || type == "iTXt") {
                    val data = ByteArray(length)
                    var readSoFar = 0
                    while (readSoFar < length) {
                        val read = inputStream.read(data, readSoFar, length - readSoFar)
                        if (read == -1) break
                        readSoFar += read
                    }
                    
                    if (readSoFar == length) {
                        // 寻找 null byte (0x00) 作为关键字和内容的分隔符
                        var nullIndex = -1
                        for (i in data.indices) {
                            if (data[i] == 0x00.toByte()) {
                                nullIndex = i
                                break
                            }
                        }

                        if (nullIndex != -1) {
                            val keyword = String(data, 0, nullIndex, StandardCharsets.ISO_8859_1)
                            if (keyword == "chara" || keyword == "ccv3") {
                                // 提取内容部分
                                // 对于 tEXt 块，后面直接是内容
                                // 对于 iTXt 块，结构更复杂一些（这里为了兼容，如果内容是合法的 base64 则直接尝试解析）
                                val contentStart = if (type == "iTXt") {
                                    // 简化版 iTXt 解析：跳过压缩标记和语言标签等，直接找连续的文本
                                    // 这里我们为了稳妥，假定通常情况还是用 tEXt，如果是 iTXt，可能需要更严格的解析
                                    nullIndex + 1
                                } else {
                                    nullIndex + 1
                                }
                                
                                val contentStr = String(data, contentStart, data.size - contentStart, StandardCharsets.UTF_8)
                                
                                // 酒馆标准：通常里面是一串 Base64
                                return decodeBase64(contentStr)
                            }
                        }
                    }
                } else {
                    // 跳过这个块的 data
                    var skipped = 0L
                    while (skipped < length) {
                        val s = inputStream.skip(length - skipped)
                        if (s <= 0) break
                        skipped += s
                    }
                }
                
                // 跳过 CRC (4 bytes)
                val crcSkipped = inputStream.skip(4)
                if (crcSkipped < 4) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.close()
        }
        return null
    }

    private fun decodeBase64(base64Str: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将角色卡 JSON 数据嵌入到 PNG 图片中（SillyTavern 标准 tEXt 块，keyword = "chara"）
     * @param bitmap 原始图片（如果为 null 则需要先调用 generateDefaultAvatar 生成）
     * @param jsonString 角色卡 JSON 字符串
     * @param outputStream 输出流
     */
    fun embedCharacterJsonToPng(bitmap: Bitmap, jsonString: String, outputStream: OutputStream) {
        val pngBytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngBytes)
        val originalPng = pngBytes.toByteArray()

        val keyword = "chara"
        val base64Data = Base64.encodeToString(
            jsonString.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        val keywordBytes = keyword.toByteArray(StandardCharsets.ISO_8859_1)
        val nullSeparator = byteArrayOf(0x00)
        val textBytes = base64Data.toByteArray(StandardCharsets.ISO_8859_1)
        val chunkData = keywordBytes + nullSeparator + textBytes

        val chunkType = "tEXt".toByteArray(StandardCharsets.US_ASCII)
        val crc32 = CRC32()
        crc32.update(chunkType)
        crc32.update(chunkData)
        val crcValue = crc32.value.toInt()

        val chunkLength = chunkData.size
        val textChunk = ByteBuffer.allocate(4 + 4 + chunkLength + 4)
            .putInt(chunkLength)
            .put(chunkType)
            .put(chunkData)
            .putInt(crcValue)
            .array()

        val iendMarker = "IEND".toByteArray(StandardCharsets.US_ASCII)
        var iendPosition = -1

        for (i in (originalPng.size - 12) downTo 8) {
            if (originalPng[i] == 0x00.toByte() &&
                originalPng[i + 1] == 0x00.toByte() &&
                originalPng[i + 2] == 0x00.toByte() &&
                originalPng[i + 3] == 0x00.toByte() &&
                originalPng[i + 4] == iendMarker[0] &&
                originalPng[i + 5] == iendMarker[1] &&
                originalPng[i + 6] == iendMarker[2] &&
                originalPng[i + 7] == iendMarker[3]
            ) {
                iendPosition = i
                break
            }
        }

        if (iendPosition == -1) {
            outputStream.write(originalPng)
            outputStream.write(textChunk)
        } else {
            outputStream.write(originalPng, 0, iendPosition)
            outputStream.write(textChunk)
            outputStream.write(originalPng, iendPosition, originalPng.size - iendPosition)
        }

        outputStream.flush()
    }

    /**
     * 生成默认的角色头像 — 渐变背景 + 角色名首字
     */
    fun generateDefaultAvatar(characterName: String, size: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                0xFF9C6ADE.toInt(), 0xFFE8B84B.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), gradientPaint)

        val initial = (characterName.trim().firstOrNull() ?: '?').uppercase()
        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = size * 0.4f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val textX = size / 2f
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial, textX, textY, textPaint)

        return bitmap
    }
}
