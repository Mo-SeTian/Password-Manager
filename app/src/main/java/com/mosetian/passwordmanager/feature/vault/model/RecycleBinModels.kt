package com.mosetian.passwordmanager.feature.vault.model

data class DeletedEntryUiModel(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val groupId: GroupId,
    val isFavorite: Boolean,
    val isWeak: Boolean,
    val isRecent: Boolean,
    val deletedAt: Long
)

data class DeletedEntryDetailUiModel(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val username: String,
    val password: String,
    val website: String?,
    val note: String?,
    val customFields: List<CustomFieldUiModel>,
    val deletedAt: Long
)
