package com.mosetian.passwordmanager.feature.vault.state

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryEditorForm
import com.mosetian.passwordmanager.feature.vault.model.DeletedEntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupEditorForm
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData

object VaultStateFactory {
    private fun normalizeQuery(query: String): String = query.trim().lowercase()

    fun buildGroups(
        customGroups: List<GroupUiModel>,
        entries: List<EntryUiModel>,
        deletedEntries: List<DeletedEntryUiModel>
    ): List<GroupUiModel> {
        val builtIns = VaultMockData.builtInGroups.map { group ->
            val count = when (group.id) {
                GroupId.All -> entries.size
                GroupId.Favorites -> entries.count { it.isFavorite }
                GroupId.Recent -> entries.count { it.isRecent }
                GroupId.Weak -> entries.count { it.isWeak }
                GroupId.RecycleBin -> deletedEntries.size
                is GroupId.Custom -> entries.count { it.groupId == group.id }
            }
            group.copy(count = count)
        }
        val customs = customGroups.map { group ->
            group.copy(count = entries.count { it.groupId == group.id })
        }
        return builtIns + customs
    }

    fun filterDeletedEntries(
        searchQuery: String,
        deletedEntries: List<DeletedEntryUiModel>
    ): List<EntryUiModel> {
        val normalizedQuery = normalizeQuery(searchQuery)
        val mapped = deletedEntries.map { deleted ->
            EntryUiModel(
                id = deleted.id,
                name = deleted.name,
                iconEmoji = deleted.iconEmoji,
                groupId = deleted.groupId,
                isFavorite = deleted.isFavorite,
                isWeak = deleted.isWeak,
                isRecent = deleted.isRecent
            )
        }
        return if (normalizedQuery.isBlank()) mapped
        else mapped.filter { it.name.lowercase().contains(normalizedQuery) }
    }

    fun filterEntries(
        selectedGroup: GroupId,
        searchQuery: String,
        entries: List<EntryUiModel>
    ): List<EntryUiModel> {
        val normalizedQuery = normalizeQuery(searchQuery)
        val groupFiltered = when (selectedGroup) {
            GroupId.All -> entries
            GroupId.Favorites -> entries.filter { it.isFavorite }
            GroupId.Recent -> entries.filter { it.isRecent }
            GroupId.Weak -> entries.filter { it.isWeak }
            GroupId.RecycleBin -> entries
            is GroupId.Custom -> entries.filter { it.groupId == selectedGroup }
        }
        return if (normalizedQuery.isBlank()) groupFiltered
        else groupFiltered.filter { it.name.lowercase().contains(normalizedQuery) }
    }

    fun buildState(
        selectedGroup: GroupId,
        selectedEntry: EntryDetailUiModel?,
        searchQuery: String,
        searchMode: Boolean,
        editorForm: EntryEditorForm?,
        groupEditorForm: GroupEditorForm?,
        entries: List<EntryUiModel>,
        deletedEntries: List<DeletedEntryUiModel>,
        customGroups: List<GroupUiModel>
    ): VaultUiState {
        val groups = buildGroups(customGroups, entries, deletedEntries)
        val editableGroups = groups.filter { it.id !is GroupId.Favorites && it.id !is GroupId.Recent && it.id !is GroupId.Weak }
        val visibleEntries = if (selectedGroup == GroupId.RecycleBin) {
            filterDeletedEntries(searchQuery, deletedEntries)
        } else {
            filterEntries(selectedGroup, searchQuery, entries)
        }
        return VaultUiState(
            groups = groups,
            editableGroups = editableGroups,
            visibleEntries = visibleEntries,
            deletedEntries = deletedEntries,
            selectedGroup = selectedGroup,
            selectedEntry = selectedEntry,
            searchQuery = searchQuery,
            searchMode = searchMode,
            editorForm = editorForm,
            groupEditorForm = groupEditorForm
        )
    }
}
