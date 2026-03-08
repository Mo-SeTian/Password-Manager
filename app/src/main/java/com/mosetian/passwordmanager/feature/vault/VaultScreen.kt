package com.mosetian.passwordmanager.feature.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mosetian.passwordmanager.feature.vault.model.CustomFieldUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryEditorForm
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.VaultMockData
import kotlinx.coroutines.launch

@Composable
fun VaultScreen() {
    var selectedGroup by remember { mutableStateOf<GroupId>(GroupId.All) }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }
    var editorForm by remember { mutableStateOf<EntryEditorForm?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(false) }

    val entries = remember { mutableStateListOf(*VaultMockData.initialEntries.toTypedArray()) }
    val entryDetails = remember { mutableStateListOf(*VaultMockData.initialEntryDetails.toTypedArray()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val groups = remember(entries.size) {
        VaultMockData.groups.map { group ->
            val count = when (group.id) {
                GroupId.All -> entries.size
                else -> entries.count { it.groupId == group.id }
            }
            group.copy(count = count)
        }
    }

    val groupFilteredEntries = remember(selectedGroup, entries.toList()) {
        when (selectedGroup) {
            GroupId.All -> entries.toList()
            else -> entries.filter { it.groupId == selectedGroup }
        }
    }
    val visibleEntries = remember(groupFilteredEntries, searchQuery) {
        if (searchQuery.isBlank()) groupFilteredEntries
        else groupFilteredEntries.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val selectedEntry = remember(selectedEntryId, entryDetails.toList()) {
        selectedEntryId?.let { id -> entryDetails.firstOrNull { it.id == id } }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editorForm = EntryEditorForm()
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "新增凭据")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LeftGroupsPane(
                    groups = groups,
                    selectedGroup = selectedGroup,
                    onGroupClick = {
                        selectedGroup = it
                        selectedEntryId = null
                    }
                )
                RightEntriesList(
                    entries = visibleEntries,
                    searchQuery = searchQuery,
                    searchMode = searchMode,
                    onSearchQueryChange = { searchQuery = it },
                    onToggleSearch = {
                        searchMode = !searchMode
                        if (!searchMode) searchQuery = ""
                    },
                    onEntryClick = { selectedEntryId = it },
                    onAddClick = { editorForm = EntryEditorForm() },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = selectedEntry != null,
                enter = fadeIn() + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + scaleOut(targetScale = 0.96f)
            ) {
                if (selectedEntry != null) {
                    EntryDetailOverlay(
                        entry = selectedEntry,
                        onDismiss = { selectedEntryId = null },
                        onEdit = {
                            editorForm = VaultMockData.createForm(selectedEntry)
                        },
                        onCopy = { label, value ->
                            clipboardManager.setText(AnnotatedString(value))
                            scope.launch {
                                snackbarHostState.showSnackbar("已复制$label")
                            }
                        }
                    )
                }
            }

            if (editorForm != null) {
                EntryEditorDialog(
                    form = editorForm!!,
                    onDismiss = { editorForm = null },
                    onFormChange = { editorForm = it },
                    onSave = { form ->
                        val targetGroup = when (selectedGroup) {
                            GroupId.All -> GroupId.All
                            else -> selectedGroup
                        }
                        val entryId = form.id ?: (entries.size + 1).toString()
                        val entry = EntryUiModel(
                            id = entryId,
                            name = form.name.ifBlank { "未命名凭据" },
                            iconEmoji = form.iconEmoji.ifBlank { "🔐" },
                            groupId = targetGroup
                        )
                        val detail = EntryDetailUiModel(
                            id = entryId,
                            name = entry.name,
                            iconEmoji = entry.iconEmoji,
                            username = form.username,
                            password = form.password,
                            website = form.website.ifBlank { null },
                            note = form.note.ifBlank { null },
                            customFields = buildList {
                                if (form.customFieldLabel.isNotBlank() || form.customFieldValue.isNotBlank()) {
                                    add(
                                        CustomFieldUiModel(
                                            label = form.customFieldLabel.ifBlank { "自定义字段" },
                                            value = form.customFieldValue
                                        )
                                    )
                                }
                            }
                        )

                        val existingEntryIndex = entries.indexOfFirst { it.id == entryId }
                        if (existingEntryIndex >= 0) {
                            entries[existingEntryIndex] = entry
                            val existingDetailIndex = entryDetails.indexOfFirst { it.id == entryId }
                            if (existingDetailIndex >= 0) {
                                entryDetails[existingDetailIndex] = detail
                            }
                            if (selectedEntryId == entryId) selectedEntryId = entryId
                            scope.launch {
                                snackbarHostState.showSnackbar("已更新凭据")
                            }
                        } else {
                            entries.add(0, entry)
                            entryDetails.add(0, detail)
                            selectedEntryId = entryId
                            scope.launch {
                                snackbarHostState.showSnackbar("已新增凭据")
                            }
                        }
                        editorForm = null
                    }
                )
            }
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
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
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
    searchQuery: String,
    searchMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onEntryClick: (String) -> Unit,
    onAddClick: () -> Unit,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "我的凭据",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "极简、安全、专注的名称列表" else "当前搜索到 ${entries.size} 条结果",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Rounded.Add, contentDescription = "新增")
                    }
                }

                AnimatedVisibility(visible = searchMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text("搜索凭据名称") },
                        singleLine = true
                    )
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
                            text = if (searchQuery.isBlank()) "此分组暂无凭据" else "没有找到匹配的凭据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "你可以点击右上角的新增按钮创建第一条凭据"
                            } else {
                                "试试更短的关键词，或者换一个分组看看"
                            },
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
                        EntryNameCard(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryNameCard(
    entry: EntryUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
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

@Composable
private fun EntryDetailOverlay(
    entry: EntryDetailUiModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.60f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 10.dp,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = entry.iconEmoji)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击字段值即可复制",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CopyableField(label = "账号", value = entry.username, onCopy = onCopy)
                    SecretCopyableField(label = "密码", value = entry.password, onCopy = onCopy)

                    entry.website?.let {
                        CopyableField(label = "网址", value = it, onCopy = onCopy)
                    }

                    entry.note?.let {
                        StaticField(label = "备注", value = it)
                    }

                    if (entry.customFields.isNotEmpty()) {
                        Text(
                            text = "自定义信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        entry.customFields.forEach { field ->
                            CustomFieldRow(field = field, onCopy = onCopy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryEditorDialog(
    form: EntryEditorForm,
    onDismiss: () -> Unit,
    onFormChange: (EntryEditorForm) -> Unit,
    onSave: (EntryEditorForm) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(form) }) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = {
            Text(if (form.id == null) "新增凭据" else "编辑凭据")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = { Text("名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.iconEmoji,
                    onValueChange = { onFormChange(form.copy(iconEmoji = it)) },
                    label = { Text("图标 / Emoji") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.username,
                    onValueChange = { onFormChange(form.copy(username = it)) },
                    label = { Text("账号") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.password,
                    onValueChange = { onFormChange(form.copy(password = it)) },
                    label = { Text("密码") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.website,
                    onValueChange = { onFormChange(form.copy(website = it)) },
                    label = { Text("网址") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.note,
                    onValueChange = { onFormChange(form.copy(note = it)) },
                    label = { Text("备注") },
                    minLines = 3
                )
                OutlinedTextField(
                    value = form.customFieldLabel,
                    onValueChange = { onFormChange(form.copy(customFieldLabel = it)) },
                    label = { Text("自定义字段名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.customFieldValue,
                    onValueChange = { onFormChange(form.copy(customFieldValue = it)) },
                    label = { Text("自定义字段值") },
                    singleLine = true
                )
            }
        }
    )
}

@Composable
private fun CopyableField(
    label: String,
    value: String,
    onCopy: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy(label, value) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "复制$label",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SecretCopyableField(
    label: String,
    value: String,
    onCopy: (String, String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val displayValue = if (visible) value else "•".repeat(maxOf(8, value.length.coerceAtMost(16)))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCopy(label, value) }
                )
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "隐藏密码" else "显示密码"
                    )
                }
                IconButton(onClick = { onCopy(label, value) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "复制$label"
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticField(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun CustomFieldRow(
    field: CustomFieldUiModel,
    onCopy: (String, String) -> Unit
) {
    if (field.isSecret) {
        SecretCopyableField(
            label = field.label,
            value = field.value,
            onCopy = onCopy
        )
    } else if (field.copyable) {
        CopyableField(
            label = field.label,
            value = field.value,
            onCopy = onCopy
        )
    } else {
        StaticField(
            label = field.label,
            value = field.value
        )
    }
}
