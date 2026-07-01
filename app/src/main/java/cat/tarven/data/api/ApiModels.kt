package cat.tarven.data.api

import com.google.gson.annotations.SerializedName

/**
 * OpenAI Chat Completion 请求
 */
@androidx.annotation.Keep
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val temperature: Double = 1.0,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1000,
    val stream: Boolean = true,
    @SerializedName("top_p")
    val topP: Double = 1.0,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double = 0.0,
    @SerializedName("presence_penalty")
    val presencePenalty: Double = 0.0,
    val n: Int? = 1
)

@androidx.annotation.Keep
data class ApiMessage(
    val role: String,
    val content: String,
    val name: String? = null
)

/**
 * OpenAI Chat Completion 非流式响应
 */
@androidx.annotation.Keep
data class ChatCompletionResponse(
    val id: String?,
    val choices: List<Choice>?,
    val usage: Usage?,
    val error: ApiError?
)

@androidx.annotation.Keep
data class Choice(
    val index: Int?,
    val message: ResponseMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

@androidx.annotation.Keep
data class ResponseMessage(
    val role: String?,
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

@androidx.annotation.Keep
data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)

@androidx.annotation.Keep
data class ApiError(
    val message: String?,
    val type: String?,
    val code: String?
)

/**
 * SSE 流式响应的 chunk
 */
@androidx.annotation.Keep
data class StreamChunk(
    val id: String?,
    val choices: List<StreamChoice>?
)

@androidx.annotation.Keep
data class StreamChoice(
    val index: Int?,
    val delta: DeltaMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

@androidx.annotation.Keep
data class DeltaMessage(
    val role: String?,
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

/**
 * 模型列表响应
 */
@androidx.annotation.Keep
data class ModelsResponse(
    val data: List<ModelInfo>?
)

@androidx.annotation.Keep
data class ModelInfo(
    val id: String,
    @SerializedName("owned_by")
    val ownedBy: String?
)
