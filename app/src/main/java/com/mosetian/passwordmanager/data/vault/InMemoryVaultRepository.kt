package com.mosetian.passwordmanager.data.vault

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData

class InMemoryVaultRepository : VaultRepository {
    private val entries = VaultMockData.initialEntries.toMutableList()
    private val entryDetails = VaultMockData.initialEntryDetails.toMutableList()
    private val customGroups = VaultMockData.initialCustomGroups.toMutableList()

    override fun getEntries(): List<EntryUiModel> = entries.toList()

    override fun getEntryDetails(): List<EntryDetailUiModel> = entryDetails.toList()

    override fun getCustomGroups(): List<GroupUiModel> = customGroups.toList()

    override fun upsertEntry(entry: EntryUiModel) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries.add(0, entry)
    }

    override fun upsertEntryDetail(detail: EntryDetailUiModel) {
        val index = entryDetails.indexOfFirst { it.id == detail.id }
        if (index >= 0) entryDetails[index] = detail else entryDetails.add(0, detail)
    }

    override fun addGroup(group: GroupUiModel) {
        customGroups.add(group)
    }

    override fun migratePlaintextDataIfNeeded() = Unit
}
