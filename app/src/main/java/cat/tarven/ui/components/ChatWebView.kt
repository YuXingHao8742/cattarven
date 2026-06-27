package cat.tarven.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import cat.tarven.data.model.ChatMessage
import cat.tarven.data.model.MessageRole
import cat.tarven.data.model.RegexRule
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 单核 WebView 聊天流引擎
 *
 * 在一个全屏 WebView 中渲染所有聊天消息。
 * WebView 加载 assets/chat_page.html 模板，通过 JavaScript 动态管理消息 DOM。
 * Kotlin 通过 evaluateJavascript() 向 WebView 推送数据，
 * WebView 通过 @JavascriptInterface (ChatWebViewBridge) 向 Kotlin 回传用户操作。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatWebView(
    messages: List<ChatMessage>,
    isDarkMode: Boolean,
    chatFontSize: Float,
    enableHtmlRendering: Boolean,
    regexRules: List<RegexRule>,
    characterName: String?,
    characterAvatarUri: String?,
    isGenerating: Boolean,
    onDeleteMessage: (String) -> Unit,
    onRequestEditDialog: (String, String) -> Unit,
    onRegenerateMessage: (String) -> Unit,
    onReincarnate: (String) -> Unit,
    onSwitchSwipe: (String, Int) -> Unit,
    onCopyText: (String) -> Unit,
    onFormSubmit: (String) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    // 标记 WebView 页面是否加载完毕
    var pageReady by remember { mutableStateOf(false) }

    // 记录上一次同步到 WebView 的消息快照（用于增量更新）
    var lastSyncedSize by remember { mutableIntStateOf(0) }
    var lastSyncedContentHash by remember { mutableLongStateOf(0L) }

    // 流式节流相关
    var lastStreamUpdateTime by remember { mutableLongStateOf(0L) }

    // 创建 Bridge
    val bridge = remember {
        ChatWebViewBridge(
            onDeleteMessage = onDeleteMessage,
            onRequestEditDialog = onRequestEditDialog,
            onRegenerateMessage = onRegenerateMessage,
            onReincarnate = onReincarnate,
            onSwitchSwipe = onSwitchSwipe,
            onCopyText = onCopyText,
            onFormSubmitCallback = onFormSubmit,
            onScrollStateChangedCallback = onScrollStateChanged,
            onPageReadyCallback = { pageReady = true }
        )
    }

    // 创建并持有 WebView 实例
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.allowFileAccess = true

            setBackgroundColor(0) // 透明背景
            isVerticalScrollBarEnabled = false

            addJavascriptInterface(bridge, "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 页面加载完成后 JS 会调用 Android.onPageReady()
                }
            }

            loadUrl("file:///android_asset/chat_page.html")
        }
    }

    // 生命周期管理
    DisposableEffect(Unit) {
        onDispose {
            webView.removeJavascriptInterface("Android")
            webView.destroy()
        }
    }

    // ===== 将消息列表转换为 JS 友好的 JSON 对象 =====
    fun buildMessageJsonArray(msgs: List<ChatMessage>): String {
        val lastAssistantId = msgs.lastOrNull { it.role == MessageRole.ASSISTANT }?.id
        val list = msgs.map { msg ->
            buildMap {
                put("id", msg.id)
                put("role", msg.role.value)
                put("displayContent", msg.displayContent)
                put("timestamp", msg.timestamp)
                put("name", msg.name)
                put("propName", msg.propName)
                put("avatarUri", if (msg.role == MessageRole.ASSISTANT) characterAvatarUri else null)
                put("isLastAssistant", msg.id == lastAssistantId)
                put("swipeCount", if (msg.swipes.isNotEmpty()) msg.swipes.size else 0)
                put("currentSwipeIndex", msg.currentSwipeIndex)
                put("greetingCount", if (msg.alternateGreetings.isNotEmpty()) msg.alternateGreetings.size else 0)
                put("currentGreetingIndex", msg.currentGreetingIndex)
            }
        }
        return gson.toJson(list)
    }

    fun buildSingleMessageJson(msg: ChatMessage, isLastAssistant: Boolean): String {
        val map = buildMap {
            put("id", msg.id)
            put("role", msg.role.value)
            put("displayContent", msg.displayContent)
            put("timestamp", msg.timestamp)
            put("name", msg.name)
            put("propName", msg.propName)
            put("avatarUri", if (msg.role == MessageRole.ASSISTANT) characterAvatarUri else null)
            put("isLastAssistant", isLastAssistant)
            put("swipeCount", if (msg.swipes.isNotEmpty()) msg.swipes.size else 0)
            put("currentSwipeIndex", msg.currentSwipeIndex)
            put("greetingCount", if (msg.alternateGreetings.isNotEmpty()) msg.alternateGreetings.size else 0)
            put("currentGreetingIndex", msg.currentGreetingIndex)
        }
        return gson.toJson(map)
    }

    fun escapeForJs(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("<", "\\x3c")
            .replace(">", "\\x3e")
    }

    // ===== 页面加载完毕后的初始化 =====
    LaunchedEffect(pageReady) {
        if (!pageReady) return@LaunchedEffect

        // 设置主题
        webView.evaluateJavascript("setTheme(${isDarkMode})", null)
        // 设置字号
        webView.evaluateJavascript("setFontSize($chatFontSize)", null)
        // 设置 HTML 渲染开关
        webView.evaluateJavascript("setHtmlRendering($enableHtmlRendering)", null)

        // 设置正则规则
        val rulesJson = gson.toJson(regexRules.filter { it.isEnabled }.map {
            mapOf("pattern" to it.pattern, "replacement" to it.replacement, "enabled" to it.isEnabled)
        })
        webView.evaluateJavascript("setRegexRules('${escapeForJs(rulesJson)}')", null)

        // 全量推送当前消息
        if (messages.isEmpty()) {
            webView.evaluateJavascript("setEmptyState('${escapeForJs(characterName ?: "角色")}')", null)
        } else {
            val json = buildMessageJsonArray(messages)
            webView.evaluateJavascript("replaceAllMessages('${escapeForJs(json)}')", null)
        }
        lastSyncedSize = messages.size
        lastSyncedContentHash = messages.lastOrNull()?.displayContent?.hashCode()?.toLong() ?: 0L
    }

    // ===== 主题切换 =====
    LaunchedEffect(isDarkMode) {
        if (!pageReady) return@LaunchedEffect
        webView.evaluateJavascript("setTheme($isDarkMode)", null)
    }

    // ===== 字号更新 =====
    LaunchedEffect(chatFontSize) {
        if (!pageReady) return@LaunchedEffect
        webView.evaluateJavascript("setFontSize($chatFontSize)", null)
    }

    // ===== HTML 渲染开关 =====
    LaunchedEffect(enableHtmlRendering) {
        if (!pageReady) return@LaunchedEffect
        webView.evaluateJavascript("setHtmlRendering($enableHtmlRendering)", null)
        // 切换后需要全量刷新
        val json = buildMessageJsonArray(messages)
        webView.evaluateJavascript("replaceAllMessages('${escapeForJs(json)}')", null)
    }

    // ===== 正则规则更新 =====
    LaunchedEffect(regexRules) {
        if (!pageReady) return@LaunchedEffect
        val rulesJson = gson.toJson(regexRules.filter { it.isEnabled }.map {
            mapOf("pattern" to it.pattern, "replacement" to it.replacement, "enabled" to it.isEnabled)
        })
        webView.evaluateJavascript("setRegexRules('${escapeForJs(rulesJson)}')", null)
        // 需要全量刷新以重新应用正则
        val json = buildMessageJsonArray(messages)
        webView.evaluateJavascript("replaceAllMessages('${escapeForJs(json)}')", null)
    }

    // ===== 消息列表变化同步 =====
    LaunchedEffect(messages.size, messages.firstOrNull()?.id) {
        if (!pageReady) return@LaunchedEffect

        if (messages.isEmpty()) {
            webView.evaluateJavascript("replaceAllMessages('[]')", null)
            webView.evaluateJavascript("setEmptyState('${escapeForJs(characterName ?: "角色")}')", null)
            lastSyncedSize = 0
            lastSyncedContentHash = 0L
            return@LaunchedEffect
        }

        when {
            // 新消息追加（最常见的情况）
            messages.size > lastSyncedSize && lastSyncedSize > 0 -> {
                // 追加新消息
                for (i in lastSyncedSize until messages.size) {
                    val msg = messages[i]
                    val lastAssistantId = messages.lastOrNull { it.role == MessageRole.ASSISTANT }?.id
                    val json = buildSingleMessageJson(msg, msg.id == lastAssistantId)
                    webView.evaluateJavascript("appendMessage('${escapeForJs(json)}')", null)
                }
            }

            // 消息减少（删除、新建对话等）或大幅变化 → 全量同步
            else -> {
                val json = buildMessageJsonArray(messages)
                webView.evaluateJavascript("replaceAllMessages('${escapeForJs(json)}')", null)
            }
        }

        lastSyncedSize = messages.size
        lastSyncedContentHash = messages.lastOrNull()?.displayContent?.hashCode()?.toLong() ?: 0L
    }

    // ===== 流式内容更新（监听最后一条消息的内容变化）=====
    LaunchedEffect(messages.lastOrNull()?.displayContent, messages.lastOrNull()?.currentSwipeIndex) {
        if (!pageReady || messages.isEmpty()) return@LaunchedEffect

        val lastMsg = messages.last()
        val contentHash = lastMsg.displayContent.hashCode().toLong()

        // 如果内容没变化，跳过
        if (contentHash == lastSyncedContentHash && messages.size == lastSyncedSize) return@LaunchedEffect

        // 节流：流式生成时不要太频繁更新
        val now = System.currentTimeMillis()
        if (lastMsg.isStreaming && now - lastStreamUpdateTime < 80) {
            // 延迟 80ms 后再检查
            delay(80)
        }
        lastStreamUpdateTime = System.currentTimeMillis()

        // 更新 WebView 中该消息的内容
        val safeContent = escapeForJs(lastMsg.displayContent)
        val safeRaw = escapeForJs(lastMsg.displayContent)

        // 处理 Swipe 更新
        val totalVersions = if (lastMsg.swipes.isNotEmpty()) lastMsg.swipes.size
            else if (lastMsg.alternateGreetings.size > 1) lastMsg.alternateGreetings.size
            else 0
        val currentIdx = if (lastMsg.swipes.isNotEmpty()) lastMsg.currentSwipeIndex
            else lastMsg.currentGreetingIndex

        if (totalVersions > 1) {
            webView.evaluateJavascript(
                "updateSwipe('${escapeForJs(lastMsg.id)}', $currentIdx, $totalVersions, '$safeContent', '$safeRaw')",
                null
            )
        } else {
            webView.evaluateJavascript(
                "updateMessage('${escapeForJs(lastMsg.id)}', '$safeContent', '$safeRaw')",
                null
            )
        }

        lastSyncedContentHash = contentHash
    }

    // ===== 渲染 WebView =====
    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
