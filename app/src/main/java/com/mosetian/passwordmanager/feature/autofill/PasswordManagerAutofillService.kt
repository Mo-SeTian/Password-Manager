package com.mosetian.passwordmanager.feature.autofill

import android.app.assist.AssistStructure
import android.content.ComponentName
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.service.autofill.SaveInfo
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.mosetian.passwordmanager.data.local.PreferencesStore
import com.mosetian.passwordmanager.data.vault.VaultRepositoryProvider
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PasswordManagerAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val context = this
        runBlocking {
            val structure = request.fillContexts.lastOrNull()?.structure
            if (structure == null) {
                callback.onSuccess(null)
                return@runBlocking
            }
            val fields = parseFields(structure)
            if (fields.usernameId == null && fields.passwordId == null) {
                callback.onSuccess(null)
                return@runBlocking
            }
            val repository = VaultRepositoryProvider.createPersistent(context)
            val entries = withContext(Dispatchers.IO) { repository.getEntries() }
            val details = mutableListOf<EntryDetailUiModel>()
            withContext(Dispatchers.IO) {
                entries.forEach { entry ->
                    repository.getEntryDetail(entry.id)?.let { details.add(it) }
                }
            }
            val preferencesStore = PreferencesStore(context)
            val usageMap = preferencesStore.getAutofillUsageMap()
            val domainKey = fields.webDomain
            val packageKey = fields.packageName
            val candidateKeys = listOfNotNull(domainKey, packageKey).distinct()
            var lastEntryId: String? = null
            for (key in candidateKeys) {
                lastEntryId = preferencesStore.getLastAutofillSelection(key)
                if (!lastEntryId.isNullOrBlank()) break
            }
            val filtered = if (!domainKey.isNullOrBlank()) {
                val matched = details.filter { detail ->
                    val host = extractHost(detail.website)
                    host != null && matchesDomain(host, domainKey)
                }
                if (matched.isNotEmpty()) matched else details
            } else details
            val sorted = filtered.sortedWith(compareByDescending<EntryDetailUiModel> { it.id == (lastEntryId ?: "") }
                .thenByDescending { usageMap[it.id] ?: 0L }
                .thenBy { it.name })
            if (sorted.isEmpty()) {
                callback.onSuccess(null)
                return@runBlocking
            }
            val responseBuilder = FillResponse.Builder()
            sorted.forEach { detail ->
                val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
                presentation.setTextViewText(android.R.id.text1, detail.name)
                val dataset = Dataset.Builder(presentation).apply {
                    fields.usernameId?.let { setValue(it, AutofillValue.forText(detail.username), presentation) }
                    fields.passwordId?.let { setValue(it, AutofillValue.forText(detail.password), presentation) }
                }.build()
                responseBuilder.addDataset(dataset)
            }
            val requiredIds = listOfNotNull(fields.usernameId, fields.passwordId).toTypedArray()
            if (requiredIds.isNotEmpty()) {
                responseBuilder.setSaveInfo(
                    SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD, requiredIds).build()
                )
            }
            callback.onSuccess(responseBuilder.build())
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        runBlocking {
            val structure = request.fillContexts.lastOrNull()?.structure
            if (structure != null) {
                val fields = parseFields(structure)
                val values = extractValues(structure, fields)
                if (!values.username.isNullOrBlank() || !values.password.isNullOrBlank()) {
                    val repository = VaultRepositoryProvider.createPersistent(this@PasswordManagerAutofillService)
                    val entries = withContext(Dispatchers.IO) { repository.getEntries() }
                    val matched = withContext(Dispatchers.IO) {
                        entries.firstOrNull { entry ->
                            repository.getEntryDetail(entry.id)?.let { detail ->
                                (values.username == null || values.username == detail.username) &&
                                    (values.password == null || values.password == detail.password)
                            } ?: false
                        }
                    }
                    if (matched != null) {
                        val prefs = PreferencesStore(this@PasswordManagerAutofillService)
                        val keys = buildAutofillKeys(fields)
                        keys.forEach { key -> prefs.setLastAutofillSelection(key, matched.id) }
                        prefs.setAutofillUsage(matched.id, System.currentTimeMillis())
                    }
                }
            }
            callback.onSuccess()
        }
    }

    private data class ParsedFields(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val packageName: String,
        val webDomain: String?
    )

    private data class ParsedValues(val username: String?, val password: String?)

    private fun parseFields(structure: AssistStructure): ParsedFields {
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()
        val packageName = structure.activityComponent?.packageName ?: "unknown"
        var webDomain: String? = null
        val windowCount = structure.windowNodeCount
        for (i in 0 until windowCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val root = windowNode.rootViewNode
            traverse(root) { node ->
                val hints = node.autofillHints?.toList().orEmpty()
                if (node.webDomain != null && !node.webDomain.isNullOrBlank()) {
                    webDomain = node.webDomain.toString()
                }
                if (hints.any { it.equals(View.AUTOFILL_HINT_USERNAME, true) || it.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, true) }) {
                    node.autofillId?.let { usernameIds.add(it) }
                }
                if (hints.any { it.equals(View.AUTOFILL_HINT_PASSWORD, true) }) {
                    node.autofillId?.let { passwordIds.add(it) }
                }
            }
        }
        return ParsedFields(usernameIds.firstOrNull(), passwordIds.firstOrNull(), packageName, webDomain)
    }

    private fun extractValues(structure: AssistStructure, fields: ParsedFields): ParsedValues {
        var username: String? = null
        var password: String? = null
        val windowCount = structure.windowNodeCount
        for (i in 0 until windowCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val root = windowNode.rootViewNode
            traverse(root) { node ->
                if (fields.usernameId != null && node.autofillId == fields.usernameId) {
                    username = node.autofillValue?.textValue?.toString()
                }
                if (fields.passwordId != null && node.autofillId == fields.passwordId) {
                    password = node.autofillValue?.textValue?.toString()
                }
            }
        }
        return ParsedValues(username, password)
    }


    private fun buildAutofillKeys(fields: ParsedFields): List<String> {
        return listOfNotNull(fields.webDomain, fields.packageName).distinct()
    }

    private fun extractHost(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return try {
            val host = java.net.URI(value).host
            if (host.isNullOrBlank()) value else host
        } catch (e: Exception) {
            value
        }
    }

    private fun normalizeDomain(value: String): String {
        return value.lowercase().removePrefix("www.")
    }

    private fun matchesDomain(host: String, domain: String): Boolean {
        val normalizedHost = normalizeDomain(host)
        val normalizedDomain = normalizeDomain(domain)
        if (normalizedHost == normalizedDomain) return true
        return normalizedHost.endsWith(".$normalizedDomain")
    }

    private fun traverse(node: AssistStructure.ViewNode, onVisit: (AssistStructure.ViewNode) -> Unit) {
        onVisit(node)
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { traverse(it, onVisit) }
        }
    }
}
