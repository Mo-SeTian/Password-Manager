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
}
