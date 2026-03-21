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
    private val lastFillContexts = mutableMapOf<String, List<String>>()
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
            val matchResult = if (!domainKey.isNullOrBlank()) {
                val normalizedTarget = normalizeDomain(domainKey)
                // 三层匹配：精确匹配 > 子域名匹配 > 模糊关键词匹配
                val exactMatches = details.filter { detail ->
                    val host = extractHost(detail.website)
                    host != null && normalizeDomain(host) == normalizedTarget
                }
                if (exactMatches.isNotEmpty()) exactMatches
                else {
                    val subdomainMatches = details.filter { detail ->
                        val host = extractHost(detail.website)
                        host != null && matchesDomain(host, domainKey)
                    }
                    if (subdomainMatches.isNotEmpty()) subdomainMatches
                    else {
                        // 模糊匹配：域名包含关键词
                        val fuzzyMatches = details.filter { detail ->
                            val host = extractHost(detail.website)
                            host != null && normalizeDomain(host).contains(normalizedTarget.split(".").first())
                        }
                        if (fuzzyMatches.isNotEmpty()) fuzzyMatches else details
                    }
                }
            } else details
            val manualSelectionMode = !domainKey.isNullOrBlank() && matchResult.size == details.size
            val grouped = matchResult.groupBy { it.name.lowercase() }
            val hasDuplicates = grouped.values.any { it.size > 1 }
            if (candidateKeys.isNotEmpty()) {
                lastFillContexts[candidateKeys.first()] = candidateKeys
            }
            val now = System.currentTimeMillis()
            val sevenDays = 7 * 24 * 60 * 60 * 1000L
            val sorted = matchResult.sortedWith(compareByDescending<EntryDetailUiModel> { it.id == (lastEntryId ?: "") }
                .thenByDescending {
                    val lastUsed = usageMap[it.id] ?: 0L
                    // 最近7天使用过的优先
                    if (now - lastUsed < sevenDays) 2L else if (lastUsed > 0) 1L else 0L
                }
                .thenByDescending { usageMap[it.id] ?: 0L }
                .thenBy { it.name })
            if (sorted.isEmpty()) {
                callback.onSuccess(null)
                return@runBlocking
            }
            val clientState = android.os.Bundle().apply {
                putString("packageName", fields.packageName)
                putStringArrayList("candidateKeys", ArrayList(candidateKeys))
            }
            val responseBuilder = FillResponse.Builder().setClientState(clientState)
            sorted.forEach { detail ->
                val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2)
                val label = if (manualSelectionMode) "手动 · ${detail.name}（记住）" else detail.name
                presentation.setTextViewText(android.R.id.text1, label)
                val lastUsed = usageMap[detail.id] ?: 0L
                val isRecent = lastUsed > 0 && now - lastUsed < sevenDays
                val subtitle = buildAutofillSubtitle(detail, domainKey, manualSelectionMode, detail.id == lastEntryId, hasDuplicates, isRecent)
                presentation.setTextViewText(android.R.id.text2, subtitle)
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
                        val keys = buildAutofillKeys(fields).toMutableList()
                        lastFillContexts[fields.packageName]?.let { cached ->
                            cached.forEach { key -> if (!keys.contains(key)) keys.add(key) }
                        }
                        keys.forEach { key -> prefs.setLastAutofillSelection(key, matched.id) }
                        lastFillContexts.remove(fields.packageName)
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

    private fun buildAutofillSubtitle(detail: EntryDetailUiModel, domainKey: String?, manualMode: Boolean, isDefault: Boolean, showUsername: Boolean, isRecent: Boolean): String {
        val host = extractHost(detail.website)
        val tag = when {
            manualMode -> "手动选择"
            isDefault -> "默认项"
            isRecent -> "最近使用"
            else -> ""
        }
        val hostText = if (!host.isNullOrBlank()) "@${normalizeDomain(host)}" else ""
        val matchHint = if (!domainKey.isNullOrBlank() && !host.isNullOrBlank() && matchesDomain(host, domainKey)) "匹配域名" else ""
        val usernameHint = if (showUsername && detail.username.isNotBlank()) "账号:${detail.username}" else ""
        return listOf(tag, hostText, matchHint, usernameHint).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun traverse(node: AssistStructure.ViewNode, onVisit: (AssistStructure.ViewNode) -> Unit) {
        onVisit(node)
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { traverse(it, onVisit) }
        }
    }
}
