package com.mosetian.passwordmanager.feature.vault.state

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryEditorForm
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupEditorForm
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData

object VaultStateFactory {
    fun buildGroups(
        customGroups: List<GroupUiModel>,
        entries: List<EntryUiModel>
    ): List<GroupUiModel> {
        val builtIns = VaultMockData.builtInGroups.map { group ->
            val count = when (group.id) {
                GroupId.All -> entries.size
                GroupId.Favorites -> entries.count { it.isFavorite }
                GroupId.Recent -> entries.count { it.isRecent }
                GroupId.Weak -> entries.count { it.isWeak }
                is GroupId.Custom -> entries.count { it.groupId == group.id }
            }
            group.copy(count = count)
        }
        val customs = customGroups.map { group ->
            group.copy(count = entries.count { it.groupId == group.id })
        }
        return builtIns + customs
    }

    fun filterEntries(
        selectedGroup: GroupId,
        searchQuery: String,
        entries: List<EntryUiModel>
    ): List<EntryUiModel> {
        val groupFiltered = when (selectedGroup) {
            GroupId.All -> entries
            GroupId.Favorites -> entries.filter { it.isFavorite }
            GroupId.Recent -> entries.filter { it.isRecent }
            GroupId.Weak -> entries.filter { it.isWeak }
            is GroupId.Custom -> entries.filter { it.groupId == selectedGroup }
        }
        return if (searchQuery.isBlank()) groupFiltered
        else groupFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    fun buildState(
        selectedGroup: GroupId,
        selectedEntry: EntryDetailUiModel?,
        searchQuery: String,
        searchMode: Boolean,
        editorForm: EntryEditorForm?,
        groupEditorForm: GroupEditorForm?,
        entries: List<EntryUiModel>,
        customGroups: List<GroupUiModel>
    ): VaultUiState {
        return VaultUiState(
            groups = buildGroups(customGroups, entries),
            editableGroups = buildGroups(customGroups, entries).filter { it.id !is GroupId.Favorites && it.id !is GroupId.Recent && it.id !is GroupId.Weak },
            visibleEntries = filterEntries(selectedGroup, searchQuery, entries),
            selectedGroup = selectedGroup,
            selectedEntry = selectedEntry,
            searchQuery = searchQuery,
            searchMode = searchMode,
            editorForm = editorForm,
            groupEditorForm = groupEditorForm
        )
    }
}
