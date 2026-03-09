package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel

interface VaultRepository {
    fun getEntries(): List<EntryUiModel>
    fun getEntryDetails(): List<EntryDetailUiModel>
    fun getCustomGroups(): List<GroupUiModel>

    fun upsertEntry(entry: EntryUiModel)
    fun upsertEntryDetail(detail: EntryDetailUiModel)
    fun addGroup(group: GroupUiModel)
    fun migratePlaintextDataIfNeeded()
}
