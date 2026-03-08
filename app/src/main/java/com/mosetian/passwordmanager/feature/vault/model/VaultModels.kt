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
    val icon: ImageVector
)

data class EntryUiModel(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val groupId: GroupId
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

object VaultMockData {
    val groups = listOf(
        GroupUiModel(GroupId.All, "全部", 12, Icons.Rounded.Folder),
        GroupUiModel(GroupId.Favorites, "常用", 4, Icons.Rounded.Favorite),
        GroupUiModel(GroupId.Recent, "最近", 5, Icons.Rounded.History),
        GroupUiModel(GroupId.Weak, "弱密码", 2, Icons.Rounded.Shield),
        GroupUiModel(GroupId.Custom("work"), "工作", 3, Icons.Rounded.Key)
    )

    val entries = listOf(
        EntryUiModel("1", "GitHub 工作", "💻", GroupId.Custom("work")),
        EntryUiModel("2", "QQ", "🐧", GroupId.All),
        EntryUiModel("3", "公司邮箱", "📨", GroupId.Custom("work")),
        EntryUiModel("4", "Netflix家庭", "🎬", GroupId.Favorites),
        EntryUiModel("5", "云服务器 Root", "☁️", GroupId.Recent),
        EntryUiModel("6", "银行短信服务", "🏦", GroupId.Weak)
    )

    val entryDetails = listOf(
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

    fun detailById(id: String): EntryDetailUiModel? = entryDetails.firstOrNull { it.id == id }
}
