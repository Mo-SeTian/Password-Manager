package com.mosetian.passwordmanager.feature.vault.state

import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryEditorForm
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupEditorForm
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel

data class VaultUiState(
    val groups: List<GroupUiModel> = emptyList(),
    val editableGroups: List<GroupUiModel> = emptyList(),
    val visibleEntries: List<EntryUiModel> = emptyList(),
    val selectedGroup: GroupId = GroupId.All,
    val selectedEntry: EntryDetailUiModel? = null,
    val searchQuery: String = "",
    val searchMode: Boolean = false,
    val editorForm: EntryEditorForm? = null,
    val groupEditorForm: GroupEditorForm? = null
)
