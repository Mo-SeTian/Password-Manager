package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData

class InMemoryVaultRepository : VaultRepository {
    private val entries = VaultMockData.initialEntries.toMutableList()
    private val entryDetails = VaultMockData.initialEntryDetails.toMutableList()
    private val customGroups = VaultMockData.initialCustomGroups.toMutableList()
    private var deletedEntries = mutableListOf<DeletedEntryUiModel>()
    private var deletedDetails = mutableListOf<DeletedEntryDetailUiModel>()

    override suspend fun getEntries(): List<EntryUiModel> = entries.toList()

    override suspend fun getEntryDetail(id: String): EntryDetailUiModel? = entryDetails.firstOrNull { it.id == id }

    override suspend fun getEntryDetails(): List<EntryDetailUiModel> = entryDetails.toList()

    override suspend fun getDeletedEntries(): List<DeletedEntryUiModel> = deletedEntries.toList()

    override suspend fun getDeletedEntryDetail(id: String): DeletedEntryDetailUiModel? = deletedDetails.firstOrNull { it.id == id }

    override suspend fun getCustomGroups(): List<GroupUiModel> = customGroups.toList()

    override suspend fun upsertEntry(entry: EntryUiModel) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries.add(0, entry)
    }

    override suspend fun upsertEntryDetail(detail: EntryDetailUiModel) {
        val index = entryDetails.indexOfFirst { it.id == detail.id }
        if (index >= 0) entryDetails[index] = detail else entryDetails.add(0, detail)
    }

    override suspend fun deleteEntry(id: String) {
        val now = System.currentTimeMillis()
        entries.firstOrNull { it.id == id }?.let { entry ->
            deletedEntries.add(
                DeletedEntryUiModel(
                    id = entry.id,
                    name = entry.name,
                    iconEmoji = entry.iconEmoji,
                    groupId = entry.groupId,
                    isFavorite = entry.isFavorite,
                    isWeak = entry.isWeak,
                    isRecent = entry.isRecent,
                    deletedAt = now
                )
            )
        }
        entryDetails.firstOrNull { it.id == id }?.let { detail ->
            deletedDetails.add(
                DeletedEntryDetailUiModel(
                    id = detail.id,
                    name = detail.name,
                    iconEmoji = detail.iconEmoji,
                    username = detail.username,
                    password = detail.password,
                    website = detail.website,
                    note = detail.note,
                    customFields = detail.customFields,
                    deletedAt = now
                )
            )
        }
        entries.removeAll { it.id == id }
        entryDetails.removeAll { it.id == id }
    }

    override suspend fun deleteEntries(ids: List<String>) {
        ids.forEach { deleteEntry(it) }
    }

    override suspend fun restoreEntry(id: String) {
        val entry = deletedEntries.firstOrNull { it.id == id }
        val detail = deletedDetails.firstOrNull { it.id == id }
        if (entry != null) {
            entries.add(0, EntryUiModel(entry.id, entry.name, entry.iconEmoji, entry.groupId, entry.isFavorite, entry.isWeak, entry.isRecent))
            deletedEntries = deletedEntries.filterNot { it.id == id }.toMutableList()
        }
        if (detail != null) {
            entryDetails.add(0, EntryDetailUiModel(detail.id, detail.name, detail.iconEmoji, detail.username, detail.password, detail.website, detail.note, detail.customFields))
            deletedDetails = deletedDetails.filterNot { it.id == id }.toMutableList()
        }
    }

    override suspend fun clearRecycleBin() {
        deletedEntries.clear()
        deletedDetails.clear()
    }

    override suspend fun purgeDeletedEntries(olderThanMillis: Long) {
        deletedEntries = deletedEntries.filter { it.deletedAt >= olderThanMillis }.toMutableList()
        deletedDetails = deletedDetails.filter { it.deletedAt >= olderThanMillis }.toMutableList()
    }

    override suspend fun addGroup(group: GroupUiModel) {
        customGroups.add(group)
    }

    override suspend fun updateGroup(group: GroupUiModel) {
        val index = customGroups.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            customGroups[index] = group
        }
    }

    override suspend fun deleteGroup(groupId: GroupId) {
        val key = (groupId as? GroupId.Custom)?.value ?: return
        customGroups.removeAll { (it.id as? GroupId.Custom)?.value == key }
        entries.removeAll { it.groupId == groupId }
        entryDetails.removeAll { detail ->
            entries.none { it.id == detail.id }
        }
    }

    override suspend fun migratePlaintextDataIfNeeded() = Unit
}
