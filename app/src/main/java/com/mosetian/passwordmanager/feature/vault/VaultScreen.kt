package com.mosetian.passwordmanager.feature.vault

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData

@Composable
fun VaultScreen() {
    var selectedGroup by remember { mutableStateOf<GroupId>(GroupId.All) }

    val visibleEntries = remember(selectedGroup) {
        when (selectedGroup) {
            GroupId.All -> VaultMockData.entries
            else -> VaultMockData.entries.filter { it.groupId == selectedGroup }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LeftGroupsPane(
                groups = VaultMockData.groups,
                selectedGroup = selectedGroup,
                onGroupClick = { selectedGroup = it }
            )
            RightEntriesList(
                entries = visibleEntries,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LeftGroupsPane(
    groups: List<GroupUiModel>,
    selectedGroup: GroupId,
    onGroupClick: (GroupId) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(108.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(groups) { group ->
                val selected = group.id == selectedGroup
                val containerColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    label = "group_container"
                )
                val scale by animateFloatAsState(
                    if (selected) 1.03f else 1f,
                    label = "group_scale"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onGroupClick(group.id) },
                    color = containerColor,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = group.icon,
                            contentDescription = group.name,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Badge {
                            Text(text = group.count.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RightEntriesList(
    entries: List<EntryUiModel>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "我的凭据",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "极简、安全、专注的名称列表",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Rounded.Search, contentDescription = "搜索")
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "此分组暂无凭据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "后续这里会接入优雅插图和添加入口",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries) { entry ->
                        EntryNameCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryNameCard(entry: EntryUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = entry.iconEmoji)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
