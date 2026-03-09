package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel

interface VaultRepository {
    suspend fun getEntries(): List<EntryUiModel>
    suspend fun getEntryDetails(): List<EntryDetailUiModel>
    suspend fun getCustomGroups(): List<GroupUiModel>

    suspend fun upsertEntry(entry: EntryUiModel)
    suspend fun upsertEntryDetail(detail: EntryDetailUiModel)
    suspend fun addGroup(group: GroupUiModel)
    suspend fun migratePlaintextDataIfNeeded()
}
