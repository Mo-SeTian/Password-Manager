package com.mosetian.passwordmanager.feature.vault.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class GroupId {
    data object All : GroupId()
    data object Favorites : GroupId()
    data object Recent : GroupId()
    data object Weak : GroupId()
    data class Custom(val value: String) : GroupId()
}

data class GroupUiModel(
    val id: GroupId,
    val name: String,
    val count: Int,
    val icon: ImageVector,
    val isBuiltIn: Boolean = true
)

data class EntryUiModel(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val groupId: GroupId,
    val isFavorite: Boolean = false,
    val isWeak: Boolean = false,
    val isRecent: Boolean = false
)

data class CustomFieldUiModel(
    val label: String,
    val value: String,
    val isSecret: Boolean = false,
    val copyable: Boolean = true
)

data class EntryDetailUiModel(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val username: String,
    val password: String,
    val website: String? = null,
    val note: String? = null,
    val customFields: List<CustomFieldUiModel> = emptyList()
)

data class EntryEditorForm(
    val id: String? = null,
    val name: String = "",
    val iconEmoji: String = "🔐",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val note: String = "",
    val customFieldLabel: String = "",
    val customFieldValue: String = ""
)

data class GroupEditorForm(
    val name: String = "",
    val key: String = "",
    val iconEmoji: String = "📁"
)

object VaultMockData {
    val builtInGroups = listOf(
        GroupUiModel(GroupId.All, "全部", 0, Icons.Rounded.Folder, true),
        GroupUiModel(GroupId.Favorites, "常用", 0, Icons.Rounded.Favorite, true),
        GroupUiModel(GroupId.Recent, "最近", 0, Icons.Rounded.History, true),
        GroupUiModel(GroupId.Weak, "弱密码", 0, Icons.Rounded.Shield, true)
    )

    val initialCustomGroups = listOf(
        GroupUiModel(GroupId.Custom("work"), "工作", 0, Icons.Rounded.Key, false)
    )

    val initialEntries = listOf(
        EntryUiModel("1", "GitHub 工作", "💻", GroupId.Custom("work"), isFavorite = true, isRecent = true),
        EntryUiModel("2", "QQ", "🐧", GroupId.All, isRecent = true),
        EntryUiModel("3", "公司邮箱", "📨", GroupId.Custom("work"), isFavorite = true),
        EntryUiModel("4", "Netflix家庭", "🎬", GroupId.All, isFavorite = true),
        EntryUiModel("5", "云服务器 Root", "☁️", GroupId.Custom("work"), isRecent = true),
        EntryUiModel("6", "银行短信服务", "🏦", GroupId.All, isWeak = true)
    )

    val initialEntryDetails = listOf(
        EntryDetailUiModel(
            id = "1",
            name = "GitHub 工作",
            iconEmoji = "💻",
            username = "dev@company.com",
            password = "Gh_2026_Strong!",
            website = "https://github.com",
            note = "工作仓库与 CI 使用，建议每 90 天轮换。",
            customFields = listOf(
                CustomFieldUiModel("2FA 备用码", "GH-2FA-7781-9921", isSecret = true),
                CustomFieldUiModel("标签", "work, git, ci", copyable = true)
            )
        ),
        EntryDetailUiModel(
            id = "2",
            name = "QQ",
            iconEmoji = "🐧",
            username = "3478xxxx12",
            password = "qqPass!2088",
            website = "https://im.qq.com",
            note = "私人联络账号。"
        ),
        EntryDetailUiModel(
            id = "3",
            name = "公司邮箱",
            iconEmoji = "📨",
            username = "name@company.com",
            password = "Mail#Secure2026",
            website = "https://outlook.office.com",
            note = "对外联络主邮箱。",
            customFields = listOf(
                CustomFieldUiModel("恢复邮箱", "backup@proton.me")
            )
        ),
        EntryDetailUiModel(
            id = "4",
            name = "Netflix家庭",
            iconEmoji = "🎬",
            username = "family@stream.tv",
            password = "NetflixHome!",
            website = "https://netflix.com",
            note = "家庭订阅，注意不要公开分享。"
        ),
        EntryDetailUiModel(
            id = "5",
            name = "云服务器 Root",
            iconEmoji = "☁️",
            username = "root",
            password = "Srv#Root#2026",
            website = "https://example-cloud.com",
            note = "生产环境，必须配合 SSH key 使用。",
            customFields = listOf(
                CustomFieldUiModel("端口", "22"),
                CustomFieldUiModel("Region", "ap-east-1")
            )
        ),
        EntryDetailUiModel(
            id = "6",
            name = "银行短信服务",
            iconEmoji = "🏦",
            username = "bank-notify",
            password = "123456",
            website = null,
            note = "该密码强度偏弱，后续应提示更换。"
        )
    )

    fun createForm(detail: EntryDetailUiModel?): EntryEditorForm {
        if (detail == null) return EntryEditorForm()
        val firstCustom = detail.customFields.firstOrNull()
        return EntryEditorForm(
            id = detail.id,
            name = detail.name,
            iconEmoji = detail.iconEmoji,
            username = detail.username,
            password = detail.password,
            website = detail.website.orEmpty(),
            note = detail.note.orEmpty(),
            customFieldLabel = firstCustom?.label.orEmpty(),
            customFieldValue = firstCustom?.value.orEmpty()
        )
    }
}
