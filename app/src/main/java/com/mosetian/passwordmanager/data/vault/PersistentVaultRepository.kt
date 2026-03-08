package com.mosetian.passwordmanager.data.vault

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import com.mosetian.passwordmanager.data.local.dao.CustomGroupDao
import com.mosetian.passwordmanager.data.local.dao.EntryDao
import com.mosetian.passwordmanager.data.local.dao.EntryDetailDao
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity
import com.mosetian.passwordmanager.data.local.entity.EntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.EntryEntity
import com.mosetian.passwordmanager.feature.vault.model.CustomFieldUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class PersistentVaultRepository(
    private val entryDao: EntryDao,
    private val entryDetailDao: EntryDetailDao,
    private val customGroupDao: CustomGroupDao
) : VaultRepository {
    override fun getEntries(): List<EntryUiModel> = runBlocking {
        entryDao.getAll().map {
            EntryUiModel(
                id = it.id,
                name = it.name,
                iconEmoji = it.iconEmoji,
                groupId = when (it.groupKey) {
                    "all" -> GroupId.All
                    "favorites" -> GroupId.Favorites
                    "recent" -> GroupId.Recent
                    "weak" -> GroupId.Weak
                    else -> GroupId.Custom(it.groupKey)
                },
                isFavorite = it.isFavorite,
                isWeak = it.isWeak,
                isRecent = it.isRecent
            )
        }
    }

    override fun getEntryDetails(): List<EntryDetailUiModel> = runBlocking {
        entryDetailDao.getAll().map {
            EntryDetailUiModel(
                id = it.id,
                name = it.name,
                iconEmoji = it.iconEmoji,
                username = it.username,
                password = it.password,
                website = it.website,
                note = it.note,
                customFields = parseCustomFields(it.customFieldsJson)
            )
        }
    }

    override fun getCustomGroups(): List<GroupUiModel> = runBlocking {
        customGroupDao.getAll().map {
            GroupUiModel(
                id = GroupId.Custom(it.key),
                name = it.name,
                count = 0,
                icon = Icons.Rounded.Folder,
                isBuiltIn = false
            )
        }
    }

    override fun upsertEntry(entry: EntryUiModel) = runBlocking {
        entryDao.upsert(
            EntryEntity(
                id = entry.id,
                name = entry.name,
                iconEmoji = entry.iconEmoji,
                groupKey = when (val group = entry.groupId) {
                    GroupId.All -> "all"
                    GroupId.Favorites -> "favorites"
                    GroupId.Recent -> "recent"
                    GroupId.Weak -> "weak"
                    is GroupId.Custom -> group.value
                },
                isFavorite = entry.isFavorite,
                isWeak = entry.isWeak,
                isRecent = entry.isRecent
            )
        )
    }

    override fun upsertEntryDetail(detail: EntryDetailUiModel) = runBlocking {
        entryDetailDao.upsert(
            EntryDetailEntity(
                id = detail.id,
                name = detail.name,
                iconEmoji = detail.iconEmoji,
                username = detail.username,
                password = detail.password,
                website = detail.website,
                note = detail.note,
                customFieldsJson = stringifyCustomFields(detail.customFields)
            )
        )
    }

    override fun addGroup(group: GroupUiModel) = runBlocking {
        val key = (group.id as? GroupId.Custom)?.value ?: return@runBlocking
        customGroupDao.insert(CustomGroupEntity(key = key, name = group.name))
    }

    private fun stringifyCustomFields(fields: List<CustomFieldUiModel>): String {
        return JSONArray().apply {
            fields.filter { it.label.isNotBlank() || it.value.isNotBlank() }.forEach { field ->
                put(
                    JSONObject().apply {
                        put("label", field.label)
                        put("value", field.value)
                        put("isSecret", field.isSecret)
                        put("copyable", field.copyable)
                    }
                )
            }
        }.toString()
    }

    private fun parseCustomFields(json: String): List<CustomFieldUiModel> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        CustomFieldUiModel(
                            label = item.optString("label"),
                            value = item.optString("value"),
                            isSecret = item.optBoolean("isSecret", false),
                            copyable = item.optBoolean("copyable", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
