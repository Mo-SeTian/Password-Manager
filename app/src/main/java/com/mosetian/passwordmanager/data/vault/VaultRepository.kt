package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel

interface VaultRepository {
    suspend fun getEntries(): List<EntryUiModel>
    suspend fun getEntryDetail(id: String): EntryDetailUiModel?
    suspend fun getEntryDetails(): List<EntryDetailUiModel>
    suspend fun getDeletedEntries(): List<DeletedEntryUiModel>
    suspend fun getDeletedEntryDetail(id: String): DeletedEntryDetailUiModel?
    suspend fun getCustomGroups(): List<GroupUiModel>

    suspend fun upsertEntry(entry: EntryUiModel)
    suspend fun upsertEntryDetail(detail: EntryDetailUiModel)
    suspend fun deleteEntry(id: String)
    suspend fun deleteEntries(ids: List<String>)
    suspend fun restoreEntry(id: String)
    suspend fun clearRecycleBin()
    suspend fun purgeDeletedEntries(olderThanMillis: Long)
    suspend fun addGroup(group: GroupUiModel)
    suspend fun updateGroup(group: GroupUiModel)
    suspend fun deleteGroup(groupId: GroupId)
    suspend fun migratePlaintextDataIfNeeded()
}
