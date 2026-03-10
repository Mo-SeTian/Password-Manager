package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData

class InMemoryVaultRepository : VaultRepository {
    private val entries = VaultMockData.initialEntries.toMutableList()
    private val entryDetails = VaultMockData.initialEntryDetails.toMutableList()
    private val customGroups = VaultMockData.initialCustomGroups.toMutableList()

    override suspend fun getEntries(): List<EntryUiModel> = entries.toList()

    override suspend fun getEntryDetail(id: String): EntryDetailUiModel? = entryDetails.firstOrNull { it.id == id }

    override suspend fun getEntryDetails(): List<EntryDetailUiModel> = entryDetails.toList()

    override suspend fun getCustomGroups(): List<GroupUiModel> = customGroups.toList()

    override suspend fun upsertEntry(entry: EntryUiModel) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries.add(0, entry)
    }

    override suspend fun upsertEntryDetail(detail: EntryDetailUiModel) {
        val index = entryDetails.indexOfFirst { it.id == detail.id }
        if (index >= 0) entryDetails[index] = detail else entryDetails.add(0, detail)
    }

    override suspend fun addGroup(group: GroupUiModel) {
        customGroups.add(group)
    }

    override suspend fun migratePlaintextDataIfNeeded() = Unit
}
