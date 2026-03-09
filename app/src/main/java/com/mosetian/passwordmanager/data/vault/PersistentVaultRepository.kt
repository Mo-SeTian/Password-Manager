package com.mosetian.passwordmanager.data.vault

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import com.mosetian.passwordmanager.data.local.dao.CustomGroupDao
import com.mosetian.passwordmanager.data.local.dao.EntryDao
import com.mosetian.passwordmanager.data.local.dao.EntryDetailDao
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity
import com.mosetian.passwordmanager.data.local.entity.EntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.EntryEntity
import com.mosetian.passwordmanager.data.security.VaultCryptoManager
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
    private val customGroupDao: CustomGroupDao,
    private val cryptoManager: VaultCryptoManager
) : VaultRepository {
    override fun getEntries(): List<EntryUiModel> = runBlocking {
        entryDao.getAll().map {
            EntryUiModel(
                id = it.id,
                name = cryptoManager.decrypt(it.name),
                iconEmoji = cryptoManager.decrypt(it.iconEmoji),
                groupId = when (it.groupKey) {
                    "all" -> GroupId.All
                    "favorites" -> GroupId.Favorites
                    "recent" -> GroupId.Recent
                    "weak" -> GroupId.Weak
                    else -> GroupId.Custom(cryptoManager.decrypt(it.groupKey))
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
                name = cryptoManager.decrypt(it.name),
                iconEmoji = cryptoManager.decrypt(it.iconEmoji),
                username = cryptoManager.decrypt(it.username),
                password = cryptoManager.decrypt(it.password),
                website = it.website?.let(cryptoManager::decrypt),
                note = it.note?.let(cryptoManager::decrypt),
                customFields = parseCustomFields(it.customFieldsJson)
            )
        }
    }

    override fun getCustomGroups(): List<GroupUiModel> = runBlocking {
        customGroupDao.getAll().map {
            GroupUiModel(
                id = GroupId.Custom(it.key),
                name = cryptoManager.decrypt(it.name),
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
                name = cryptoManager.encrypt(entry.name),
                iconEmoji = cryptoManager.encrypt(entry.iconEmoji),
                groupKey = when (val group = entry.groupId) {
                    GroupId.All -> "all"
                    GroupId.Favorites -> "favorites"
                    GroupId.Recent -> "recent"
                    GroupId.Weak -> "weak"
                    is GroupId.Custom -> cryptoManager.encrypt(group.value)
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
                name = cryptoManager.encrypt(detail.name),
                iconEmoji = cryptoManager.encrypt(detail.iconEmoji),
                username = cryptoManager.encrypt(detail.username),
                password = cryptoManager.encrypt(detail.password),
                website = detail.website?.let(cryptoManager::encrypt),
                note = detail.note?.let(cryptoManager::encrypt),
                customFieldsJson = stringifyCustomFields(detail.customFields)
            )
        )
    }

    override fun addGroup(group: GroupUiModel) = runBlocking {
        val key = (group.id as? GroupId.Custom)?.value ?: return@runBlocking
        customGroupDao.insert(
            CustomGroupEntity(
                key = key,
                name = cryptoManager.encrypt(group.name)
            )
        )
    }

    override fun migratePlaintextDataIfNeeded() = runBlocking {
        entryDao.getAll().forEach { entry ->
            val encryptedGroupKey = when (entry.groupKey) {
                "all", "favorites", "recent", "weak" -> entry.groupKey
                else -> cryptoManager.encrypt(cryptoManager.decrypt(entry.groupKey))
            }
            entryDao.upsert(
                entry.copy(
                    name = cryptoManager.encrypt(cryptoManager.decrypt(entry.name)),
                    iconEmoji = cryptoManager.encrypt(cryptoManager.decrypt(entry.iconEmoji)),
                    groupKey = encryptedGroupKey
                )
            )
        }

        entryDetailDao.getAll().forEach { detail ->
            entryDetailDao.upsert(
                detail.copy(
                    name = cryptoManager.encrypt(cryptoManager.decrypt(detail.name)),
                    iconEmoji = cryptoManager.encrypt(cryptoManager.decrypt(detail.iconEmoji)),
                    username = cryptoManager.encrypt(cryptoManager.decrypt(detail.username)),
                    password = cryptoManager.encrypt(cryptoManager.decrypt(detail.password)),
                    website = detail.website?.let { cryptoManager.encrypt(cryptoManager.decrypt(it)) },
                    note = detail.note?.let { cryptoManager.encrypt(cryptoManager.decrypt(it)) },
                    customFieldsJson = stringifyCustomFields(parseCustomFields(detail.customFieldsJson))
                )
            )
        }

        customGroupDao.getAll().forEach { group ->
            customGroupDao.insert(
                group.copy(name = cryptoManager.encrypt(cryptoManager.decrypt(group.name)))
            )
        }
    }

    private fun stringifyCustomFields(fields: List<CustomFieldUiModel>): String {
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

    private fun parseCustomFields(json: String): List<CustomFieldUiModel> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        CustomFieldUiModel(
                            label = cryptoManager.decrypt(item.optString("label")),
                            value = cryptoManager.decrypt(item.optString("value")),
                            isSecret = item.optBoolean("isSecret", false),
                            copyable = item.optBoolean("copyable", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
