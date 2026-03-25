package com.mosetian.passwordmanager.data.vault

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import com.mosetian.passwordmanager.data.local.dao.CustomGroupDao
import com.mosetian.passwordmanager.data.local.dao.DeletedEntryDao
import com.mosetian.passwordmanager.data.local.dao.DeletedEntryDetailDao
import com.mosetian.passwordmanager.data.local.dao.EntryDao
import com.mosetian.passwordmanager.data.local.dao.EntryDetailDao
import com.mosetian.passwordmanager.data.local.entity.CustomGroupEntity
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.DeletedEntryEntity
import com.mosetian.passwordmanager.data.local.entity.EntryDetailEntity
import com.mosetian.passwordmanager.data.local.entity.EntryEntity
import com.mosetian.passwordmanager.data.security.VaultCryptoManager
import com.mosetian.passwordmanager.feature.vault.model.CustomFieldUiModel
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import org.json.JSONArray
import org.json.JSONObject

class PersistentVaultRepository(
    private val entryDao: EntryDao,
    private val entryDetailDao: EntryDetailDao,
    private val customGroupDao: CustomGroupDao,
    private val deletedEntryDao: DeletedEntryDao,
    private val deletedEntryDetailDao: DeletedEntryDetailDao,
    private val cryptoManager: VaultCryptoManager
) : VaultRepository {
    override suspend fun getEntries(): List<EntryUiModel> {
        return entryDao.getAll().map {
            EntryUiModel(
                id = it.id,
                name = cryptoManager.decrypt(it.name),
                iconEmoji = cryptoManager.decrypt(it.iconEmoji),
                groupId = when (it.groupKey) {
                    "all" -> GroupId.All
                    "favorites" -> GroupId.Favorites
                    "recent" -> GroupId.Recent
                    "weak" -> GroupId.Weak
                    "recycle_bin" -> GroupId.RecycleBin
                    else -> GroupId.Custom(cryptoManager.decrypt(it.groupKey))
                },
                isFavorite = it.isFavorite,
                isWeak = it.isWeak,
                isRecent = it.isRecent
            )
        }
    }

    override suspend fun getEntryDetail(id: String): EntryDetailUiModel? {
        return entryDetailDao.getById(id)?.toUiModel()
    }

    override suspend fun getEntryDetails(): List<EntryDetailUiModel> {
        return entryDetailDao.getAll().map { it.toUiModel() }
    }

    override suspend fun getDeletedEntries(): List<DeletedEntryUiModel> {
        return deletedEntryDao.getAll().map { it.toUiModel(cryptoManager) }
    }

    override suspend fun getDeletedEntryDetail(id: String): DeletedEntryDetailUiModel? {
        return deletedEntryDetailDao.getById(id)?.toUiModel(cryptoManager)
    }

    override suspend fun getCustomGroups(): List<GroupUiModel> {
        return customGroupDao.getAll().map {
            GroupUiModel(
                id = GroupId.Custom(it.key),
                name = cryptoManager.decrypt(it.name),
                count = 0,
                icon = Icons.Rounded.Folder,
                isBuiltIn = false,
                iconEmoji = it.iconEmoji
            )
        }
    }

    override suspend fun upsertEntry(entry: EntryUiModel) {
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
                    GroupId.RecycleBin -> "recycle_bin"
                    is GroupId.Custom -> cryptoManager.encrypt(group.value)
                },
                isFavorite = entry.isFavorite,
                isWeak = entry.isWeak,
                isRecent = entry.isRecent
            )
        )
    }

    override suspend fun upsertEntryDetail(detail: EntryDetailUiModel) {
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

    override suspend fun deleteEntry(id: String) {
        val now = System.currentTimeMillis()
        val entry = entryDao.getAll().firstOrNull { it.id == id }
        val detail = entryDetailDao.getById(id)
        if (entry != null) {
            deletedEntryDao.upsert(
                DeletedEntryEntity(
                    id = entry.id,
                    name = entry.name,
                    iconEmoji = entry.iconEmoji,
                    groupKey = entry.groupKey,
                    isFavorite = entry.isFavorite,
                    isWeak = entry.isWeak,
                    isRecent = entry.isRecent,
                    deletedAt = now
                )
            )
        }
        if (detail != null) {
            deletedEntryDetailDao.upsert(
                DeletedEntryDetailEntity(
                    id = detail.id,
                    name = detail.name,
                    iconEmoji = detail.iconEmoji,
                    username = detail.username,
                    password = detail.password,
                    website = detail.website,
                    note = detail.note,
                    customFieldsJson = detail.customFieldsJson,
                    deletedAt = now
                )
            )
        }
        entryDao.deleteById(id)
        entryDetailDao.deleteById(id)
    }

    override suspend fun deleteEntries(ids: List<String>) {
        ids.forEach { deleteEntry(it) }
    }

    override suspend fun restoreEntry(id: String) {
        val entry = deletedEntryDao.getById(id)
        val detail = deletedEntryDetailDao.getById(id)
        if (entry != null) {
            entryDao.upsert(
                EntryEntity(
                    id = entry.id,
                    name = entry.name,
                    iconEmoji = entry.iconEmoji,
                    groupKey = entry.groupKey,
                    isFavorite = entry.isFavorite,
                    isWeak = entry.isWeak,
                    isRecent = entry.isRecent
                )
            )
            deletedEntryDao.deleteById(id)
        }
        if (detail != null) {
            entryDetailDao.upsert(
                EntryDetailEntity(
                    id = detail.id,
                    name = detail.name,
                    iconEmoji = detail.iconEmoji,
                    username = detail.username,
                    password = detail.password,
                    website = detail.website,
                    note = detail.note,
                    customFieldsJson = detail.customFieldsJson
                )
            )
            deletedEntryDetailDao.deleteById(id)
        }
    }

    override suspend fun clearRecycleBin() {
        deletedEntryDao.deleteAll()
        deletedEntryDetailDao.deleteAll()
    }

    override suspend fun purgeDeletedEntries(olderThanMillis: Long) {
        deletedEntryDao.deleteOlderThan(olderThanMillis)
        deletedEntryDetailDao.deleteOlderThan(olderThanMillis)
    }

    override suspend fun addGroup(group: GroupUiModel) {
        val key = (group.id as? GroupId.Custom)?.value ?: return
        val existing = customGroupDao.getAll()
        val maxOrder = existing.maxOfOrNull { it.sortOrder } ?: -1
        customGroupDao.insert(
            CustomGroupEntity(
                key = key,
                name = cryptoManager.encrypt(group.name),
                iconEmoji = group.iconEmoji.ifBlank { "📁" },
                sortOrder = maxOrder + 1
            )
        )
    }

    override suspend fun updateGroup(group: GroupUiModel) {
        val key = (group.id as? GroupId.Custom)?.value ?: return
        val existing = customGroupDao.getByKey(key) ?: return
        customGroupDao.update(
            key = key,
            name = cryptoManager.encrypt(group.name),
            iconEmoji = group.iconEmoji.ifBlank { existing.iconEmoji },
            sortOrder = existing.sortOrder
        )
    }

    override suspend fun deleteGroup(groupId: GroupId) {
        val key = (groupId as? GroupId.Custom)?.value ?: return
        customGroupDao.deleteByKey(key)
    }

    override suspend fun migratePlaintextDataIfNeeded() {
        val entries = entryDao.getAll()
        val details = entryDetailDao.getAll()
        val groups = customGroupDao.getAll()

        val entriesNeedMigration = entries.any {
            !cryptoManager.isEncrypted(it.name) ||
                !cryptoManager.isEncrypted(it.iconEmoji) ||
                (it.groupKey !in setOf("all", "favorites", "recent", "weak") && !cryptoManager.isEncrypted(it.groupKey))
        }
        val detailsNeedMigration = details.any {
            !cryptoManager.isEncrypted(it.name) ||
                !cryptoManager.isEncrypted(it.iconEmoji) ||
                !cryptoManager.isEncrypted(it.username) ||
                !cryptoManager.isEncrypted(it.password) ||
                (it.website != null && !cryptoManager.isEncrypted(it.website)) ||
                (it.note != null && !cryptoManager.isEncrypted(it.note)) ||
                parseCustomFields(it.customFieldsJson).any { field ->
                    !cryptoManager.isEncrypted(field.label) || !cryptoManager.isEncrypted(field.value)
                }
        }
        val groupsNeedMigration = groups.any { !cryptoManager.isEncrypted(it.name) }

        if (!entriesNeedMigration && !detailsNeedMigration && !groupsNeedMigration) return

        entries.forEach { entry ->
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

        details.forEach { detail ->
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

        groups.forEach { group ->
            customGroupDao.insert(
                group.copy(name = cryptoManager.encrypt(cryptoManager.decrypt(group.name)))
            )
        }
    }

    private fun EntryDetailEntity.toUiModel(): EntryDetailUiModel {
        return EntryDetailUiModel(
            id = id,
            name = cryptoManager.decrypt(name),
            iconEmoji = cryptoManager.decrypt(iconEmoji),
            username = cryptoManager.decrypt(username),
            password = cryptoManager.decrypt(password),
            website = website?.let(cryptoManager::decrypt),
            note = note?.let(cryptoManager::decrypt),
            customFields = parseCustomFields(customFieldsJson)
        )
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
}
