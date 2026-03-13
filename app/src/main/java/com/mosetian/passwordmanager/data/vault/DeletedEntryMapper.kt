package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.data.local.entity.DeletedEntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryEntity
import com.mosetian.passwordmanager.feature.vault.model.CustomFieldUiModel
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.data.security.VaultCryptoManager
import org.json.JSONArray
import org.json.JSONObject

internal fun DeletedEntryEntity.toUiModel(cryptoManager: VaultCryptoManager): DeletedEntryUiModel {
    return DeletedEntryUiModel(
        id = id,
        name = cryptoManager.decrypt(name),
        iconEmoji = cryptoManager.decrypt(iconEmoji),
        groupId = when (groupKey) {
            "all" -> GroupId.All
            "favorites" -> GroupId.Favorites
            "recent" -> GroupId.Recent
            "weak" -> GroupId.Weak
            else -> GroupId.Custom(cryptoManager.decrypt(groupKey))
        },
        isFavorite = isFavorite,
        isWeak = isWeak,
        isRecent = isRecent,
        deletedAt = deletedAt
    )
}

internal fun DeletedEntryDetailEntity.toUiModel(cryptoManager: VaultCryptoManager): DeletedEntryDetailUiModel {
    return DeletedEntryDetailUiModel(
        id = id,
        name = cryptoManager.decrypt(name),
        iconEmoji = cryptoManager.decrypt(iconEmoji),
        username = cryptoManager.decrypt(username),
        password = cryptoManager.decrypt(password),
        website = website?.let(cryptoManager::decrypt),
        note = note?.let(cryptoManager::decrypt),
        customFields = parseCustomFields(cryptoManager, customFieldsJson),
        deletedAt = deletedAt
    )
}

internal fun stringifyDeletedCustomFields(cryptoManager: VaultCryptoManager, fields: List<CustomFieldUiModel>): String {
    return JSONArray().apply {
        fields.filter { it.label.isNotBlank() || it.value.isNotBlank() }.forEach { field ->
            put(
                JSONObject().apply {
                    put("label", cryptoManager.encrypt(field.label))
                    put("value", cryptoManager.encrypt(field.value))
                    put("isSecret", field.isSecret)
                    put("copyable", field.copyable)
                }
            )
        }
    }.toString()
}

internal fun parseCustomFields(cryptoManager: VaultCryptoManager, json: String): List<CustomFieldUiModel> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val rawLabel = item.optString("label")
                val rawValue = item.optString("value")
                add(
                    CustomFieldUiModel(
                        label = cryptoManager.decrypt(rawLabel),
                        value = cryptoManager.decrypt(rawValue),
                        isSecret = item.optBoolean("isSecret", false),
                        copyable = item.optBoolean("copyable", true)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
