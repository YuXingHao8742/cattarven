package cat.tarven.data.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * OpenAI 兼容 API 服务
 * 支持自定义端点和密钥，SSE 流式传输
 */
class OpenAIService {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 流式聊天补全 — 返回 Flow<String>，逐 token 发射
     */
    fun streamChatCompletion(
        apiUrl: String,
        apiKey: String,
        request: ChatCompletionRequest
    ): Flow<String> = callbackFlow {
        val url = apiUrl.trimEnd('/') + "/v1/chat/completions"
        val streamRequest = request.copy(stream = true)
        val jsonBody = gson.toJson(streamRequest)
        
        // 打印大模型请求的真实 JSON 数据，方便在 Logcat 查看！
        android.util.Log.d("ChatJSON", "================ 准备发送给 AI 的数据 ================")
        android.util.Log.d("ChatJSON", jsonBody)
        android.util.Log.d("ChatJSON", "=====================================================")

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "text/event-stream")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(httpRequest)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    // 尝试解析 API 错误
                    val errorMsg = try {
                        val errorResponse = gson.fromJson(errorBody, ChatCompletionResponse::class.java)
                        errorResponse.error?.message ?: "HTTP ${response.code}: $errorBody"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $errorBody"
                    }
                    close(IOException(errorMsg))
                    return
                }

                try {
                    val reader = BufferedReader(
                        InputStreamReader(response.body!!.byteStream())
                    )
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: continue
                        if (currentLine.startsWith("data: ")) {
                            val data = currentLine.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                break
                            }
                            try {
                                val chunk = gson.fromJson(data, StreamChunk::class.java)
                                val content = chunk.choices?.firstOrNull()?.delta?.content
                                if (!content.isNullOrEmpty()) {
                                    trySend(content)
                                }
                            } catch (e: Exception) {
                                // 忽略无法解析的行
                            }
                        }
                    }
                    reader.close()
                    close()
                } catch (e: Exception) {
                    close(e)
                }
            }
        })

        awaitClose { call.cancel() }
    }

    /**
     * 非流式聊天补全
     */
    suspend fun chatCompletion(
        apiUrl: String,
        apiKey: String,
        request: ChatCompletionRequest
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = apiUrl.trimEnd('/') + "/v1/chat/completions"
            val nonStreamRequest = request.copy(stream = false)
            val jsonBody = gson.toJson(nonStreamRequest)

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorResponse = try {
                    gson.fromJson(responseBody, ChatCompletionResponse::class.java)
                } catch (e: Exception) { null }
                return@withContext Result.failure(
                    IOException(errorResponse?.error?.message ?: "HTTP ${response.code}: $responseBody")
                )
            }

            val completionResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            val content = completionResponse.choices?.firstOrNull()?.message?.content ?: ""
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 测试连接 — 尝试获取模型列表
     */
    suspend fun testConnection(
        apiUrl: String,
        apiKey: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = apiUrl.trimEnd('/') + "/v1/models"
            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: $responseBody")
                )
            }

            val modelsResponse = gson.fromJson(responseBody, ModelsResponse::class.java)
            val modelIds = modelsResponse.data?.map { it.id }?.sorted() ?: emptyList()
            Result.success(modelIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
