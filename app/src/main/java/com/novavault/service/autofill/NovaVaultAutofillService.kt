package com.novavault.service.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.repository.VaultRepository
import com.novavault.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaVaultAutofillService : AutofillService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutofillEntryPoint {
        fun vaultRepository(): VaultRepository
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val vaultRepository: VaultRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AutofillEntryPoint::class.java,
        ).vaultRepository()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val fields = parseAssistStructure(structure)
        val usernameField = fields.firstOrNull { it.type == FieldType.USERNAME }
        val passwordField = fields.firstOrNull { it.type == FieldType.PASSWORD }

        if (usernameField == null && passwordField == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                if (!vaultRepository.isUnlocked()) {
                    // Vault locked: gate with Auth Intent
                    val authIntent = Intent(this@NovaVaultAutofillService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("AUTOFILL_UNLOCK", true)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this@NovaVaultAutofillService,
                        1001,
                        authIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
                    )

                    val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                        setTextViewText(android.R.id.text1, "🔒 Unlock Nova Vault to Autofill")
                    }

                    val requiredAuthIds = listOfNotNull(usernameField?.id, passwordField?.id).toTypedArray()
                    val response = FillResponse.Builder()
                        .setAuthentication(
                            requiredAuthIds,
                            pendingIntent.intentSender,
                            presentation,
                        )
                        .build()

                    callback.onSuccess(response)
                    return@launch
                }

                // Vault is unlocked: fetch credentials
                val items = vaultRepository.getItems()
                    .filter { it.type == VaultItemType.LOGIN && !it.isDeleted }

                if (items.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                for (item in items.take(5)) {
                    val login = item.login ?: continue
                    val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                        setTextViewText(android.R.id.text1, item.name)
                        setTextViewText(android.R.id.text2, login.username)
                    }

                    val datasetBuilder = Dataset.Builder()

                    if (usernameField != null && login.username.isNotEmpty()) {
                        datasetBuilder.setValue(
                            usernameField.id,
                            AutofillValue.forText(login.username),
                            presentation,
                        )
                    }

                    if (passwordField != null && login.password.isNotEmpty()) {
                        datasetBuilder.setValue(
                            passwordField.id,
                            AutofillValue.forText(login.password),
                            presentation,
                        )
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                // Add SaveInfo so user can save updated credentials
                val saveTypes = SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
                val requiredIds = listOfNotNull(usernameField?.id, passwordField?.id).toTypedArray()
                if (requiredIds.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(saveTypes, requiredIds).build()
                    responseBuilder.setSaveInfo(saveInfo)
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private data class ParsedField(
        val id: AutofillId,
        val type: FieldType,
    )

    private enum class FieldType {
        USERNAME,
        PASSWORD,
    }

    private fun parseAssistStructure(structure: AssistStructure): List<ParsedField> {
        val fields = mutableListOf<ParsedField>()
        for (i in 0 until structure.windowNodeCount) {
            val rootNode = structure.getWindowNodeAt(i).rootViewNode
            traverseNode(rootNode, fields)
        }
        return fields
    }

    private fun traverseNode(node: AssistStructure.ViewNode, fields: MutableList<ParsedField>) {
        val autofillId = node.autofillId
        val hints = node.autofillHints ?: emptyArray()
        val className = node.className.orEmpty()
        val hint = node.hint.orEmpty().lowercase()
        val idEntry = node.idEntry.orEmpty().lowercase()

        val isPassword = hints.any { it.contains("password", ignoreCase = true) } ||
                hint.contains("password") || hint.contains("pass") ||
                idEntry.contains("password") || idEntry.contains("pass") ||
                (node.inputType and 0x80) != 0 || (node.inputType and 0x10) != 0

        val isUsername = hints.any {
            it.contains("username", ignoreCase = true) ||
                    it.contains("email", ignoreCase = true)
        } || hint.contains("username") || hint.contains("email") || hint.contains("user") ||
                idEntry.contains("username") || idEntry.contains("email") || idEntry.contains("user")

        if (autofillId != null && node.visibility == View.VISIBLE) {
            if (isPassword) {
                fields.add(ParsedField(autofillId, FieldType.PASSWORD))
            } else if (isUsername) {
                fields.add(ParsedField(autofillId, FieldType.USERNAME))
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), fields)
        }
    }
}
