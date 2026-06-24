package cat.tarven.utils

import android.util.Base64
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

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
}
