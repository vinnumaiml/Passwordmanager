package com.novavault.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SecureClipboard {
    /**
     * Copy [text] to clipboard and clear after [clearAfterSeconds].
     * On Android 13+, the OS limits background clipboard access; we clear on a best-effort basis.
     * Sensitive: never log or expose [text] in crash reports.
     */
    fun copyAndScheduleClear(
        context: Context,
        text: String,
        label: String = "Nova Vault",
        clearAfterSeconds: Int = 30,
        scope: CoroutineScope,
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        if (clearAfterSeconds > 0) {
            scope.launch {
                delay(clearAfterSeconds * 1000L)
                try {
                    clipboard.clearPrimaryClip()
                } catch (ignored: Exception) {
                    // Ignored on newer Android when app is in background
                }
            }
        }
    }

    fun clear(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        try {
            clipboard.clearPrimaryClip()
        } catch (ignored: Exception) {}
    }
}
