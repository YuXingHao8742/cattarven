package cat.tarven.ui.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Kotlin-JS 双向通信桥
 * 通过 @JavascriptInterface 暴露给 WebView 中的 JavaScript 调用。
 * JS 端通过 `Android.xxx()` 调用这些方法。
 *
 * 所有回调都在主线程上执行（通过 Handler 转发），因为 WebView 的
 * JavascriptInterface 可能在 WebView 的内部线程上被调用。
 */
class ChatWebViewBridge(
    private val onDeleteMessage: (String) -> Unit,
    private val onRequestEditDialog: (String, String) -> Unit,
    private val onRegenerateMessage: (String) -> Unit,
    private val onReincarnate: (String) -> Unit,
    private val onSwitchSwipe: (String, Int) -> Unit,
    private val onCopyText: (String) -> Unit,
    private val onFormSubmitCallback: (String) -> Unit,
    private val onScrollStateChangedCallback: (Boolean) -> Unit,
    private val onPageReadyCallback: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    @JavascriptInterface
    fun deleteMessage(messageId: String) {
        runOnMain { onDeleteMessage(messageId) }
    }

    @JavascriptInterface
    fun requestEditDialog(messageId: String, currentContent: String) {
        runOnMain { onRequestEditDialog(messageId, currentContent) }
    }

    @JavascriptInterface
    fun regenerateMessage(messageId: String) {
        runOnMain { onRegenerateMessage(messageId) }
    }

    @JavascriptInterface
    fun reincarnate(content: String) {
        runOnMain { onReincarnate(content) }
    }

    @JavascriptInterface
    fun switchSwipe(messageId: String, newIndex: Int) {
        runOnMain { onSwitchSwipe(messageId, newIndex) }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        runOnMain { onCopyText(text) }
    }

    @JavascriptInterface
    fun onFormSubmit(formDataJson: String) {
        runOnMain { onFormSubmitCallback(formDataJson) }
    }

    @JavascriptInterface
    fun onScrollStateChanged(showScrollToBottom: Boolean) {
        runOnMain { onScrollStateChangedCallback(showScrollToBottom) }
    }

    @JavascriptInterface
    fun onPageReady() {
        runOnMain { onPageReadyCallback() }
    }
}
