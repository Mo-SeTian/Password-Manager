package com.mosetian.passwordmanager.feature.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mosetian.passwordmanager.data.vault.InMemoryVaultRepository
import com.mosetian.passwordmanager.data.vault.VaultRepository
import com.mosetian.passwordmanager.feature.security.SecuritySettings
import com.mosetian.passwordmanager.feature.vault.model.CustomFieldUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryDetailUiModel
import com.mosetian.passwordmanager.feature.vault.model.EntryEditorForm
import com.mosetian.passwordmanager.feature.vault.model.EntryUiModel
import com.mosetian.passwordmanager.feature.vault.model.GroupEditorForm
import com.mosetian.passwordmanager.feature.vault.model.GroupId
import com.mosetian.passwordmanager.feature.vault.model.GroupUiModel
import com.mosetian.passwordmanager.feature.vault.model.toEditorForm
import com.mosetian.passwordmanager.feature.vault.state.VaultStateFactory
import com.mosetian.passwordmanager.feature.vault.state.VaultUiState
import kotlinx.coroutines.launch

private data class VaultLayoutDensity(
    val groupsPaneWidth: Dp,
    val screenPadding: Dp,
    val paneGap: Dp,
    val paneCorner: Dp,
    val topBarHorizontal: Dp,
    val topBarVertical: Dp,
    val listItemHorizontal: Dp,
    val listItemVertical: Dp,
    val listItemSpacing: Dp,
    val groupItemVertical: Dp,
    val groupItemSpacing: Dp
)

private fun layoutDensityOf(value: Float): VaultLayoutDensity {
    val safeScale = value.coerceAtLeast(0.35f)
    val factor = 1f + (safeScale - 1f) * 0.85f
    return VaultLayoutDensity(
        groupsPaneWidth = 112.dp * factor,
        screenPadding = 16.dp * factor,
        paneGap = 16.dp * factor,
        paneCorner = 30.dp * factor,
        topBarHorizontal = 22.dp * factor,
        topBarVertical = 18.dp * factor,
        listItemHorizontal = 18.dp * factor,
        listItemVertical = 18.dp * factor,
        listItemSpacing = 14.dp * factor,
        groupItemVertical = 15.dp * factor,
        groupItemSpacing = 12.dp * factor
    )
}

@Composable
fun VaultScreen(
    repository: VaultRepository = remember { InMemoryVaultRepository() },
    initialSecuritySettings: SecuritySettings = SecuritySettings(),
    initialUiScale: Float = 0.48f,
    onSecuritySettingsChange: (SecuritySettings) -> Unit = {},
    onUiScaleChange: (Float) -> Unit = {},
    onRequestLockSetup: () -> Unit = {},
    onRequestLockNow: () -> Unit = {},
    onRequestChangePassword: () -> Unit = {},
    onRequestDisableAppLock: () -> Unit = {}
) {
    var selectedGroup by remember { mutableStateOf<GroupId>(GroupId.All) }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }
    var editorForm by remember { mutableStateOf<EntryEditorForm?>(null) }
    var groupEditorForm by remember { mutableStateOf<GroupEditorForm?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(false) }
    var securitySettings by remember(initialSecuritySettings) { mutableStateOf(initialSecuritySettings) }
    var securityPanelVisible by remember { mutableStateOf(false) }
    var uiScale by remember(initialUiScale) { mutableStateOf(initialUiScale) }
    val configuration = LocalConfiguration.current
    val autoLayoutScale = remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp <= 360 -> 0.92f
            configuration.screenWidthDp >= 480 -> 1.08f
            else -> 1f
        }
    }
    val layoutDensity = remember(uiScale, autoLayoutScale) { layoutDensityOf((uiScale * autoLayoutScale).coerceIn(0.35f, 1.2f)) }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<EntryUiModel>>(emptyList()) }
    var selectedEntryDetail by remember { mutableStateOf<EntryDetailUiModel?>(null) }
    var detailCache by remember { mutableStateOf<Map<String, EntryDetailUiModel>>(emptyMap()) }
    var detailLoading by remember { mutableStateOf(false) }
    var customGroups by remember { mutableStateOf<List<GroupUiModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reloadVaultData() {
        loading = true
        entries = repository.getEntries()
        customGroups = repository.getCustomGroups()
        loading = false
    }

    fun upsertLocalEntry(entry: EntryUiModel) {
        entries = buildList {
            var replaced = false
            entries.forEach { current ->
                if (current.id == entry.id) {
                    add(entry)
                    replaced = true
                } else {
                    add(current)
                }
            }
            if (!replaced) add(0, entry)
        }
    }

    fun addLocalGroup(group: GroupUiModel) {
        if (customGroups.none { it.id == group.id }) {
            customGroups = customGroups + group
        }
    }

    fun cacheEntryDetail(detail: EntryDetailUiModel) {
        detailCache = detailCache + (detail.id to detail)
    }

    suspend fun reloadSelectedEntryDetail() {
        val entryId = selectedEntryId
        if (entryId == null) {
            selectedEntryDetail = null
            detailLoading = false
            return
        }
        detailCache[entryId]?.let { cached ->
            selectedEntryDetail = cached
            detailLoading = false
            return
        }
        detailLoading = true
        val detail = repository.getEntryDetail(entryId)
        selectedEntryDetail = detail
        if (detail != null) cacheEntryDetail(detail)
        detailLoading = false
    }

    LaunchedEffect(repository) {
        reloadVaultData()
    }

    LaunchedEffect(repository, selectedEntryId) {
        reloadSelectedEntryDetail()
    }

    val uiState = remember(
        selectedGroup,
        selectedEntryId,
        searchQuery,
        searchMode,
        editorForm,
        groupEditorForm,
        entries,
        selectedEntryDetail,
        customGroups
    ) {
        VaultStateFactory.buildState(
            selectedGroup = selectedGroup,
            selectedEntry = selectedEntryDetail,
            searchQuery = searchQuery,
            searchMode = searchMode,
            editorForm = editorForm,
            groupEditorForm = groupEditorForm,
            entries = entries,
            customGroups = customGroups
        )
    }

    LaunchedEffect(uiState.visibleEntries, selectedEntryId) {
        val currentId = selectedEntryId ?: return@LaunchedEffect
        if (uiState.visibleEntries.none { it.id == currentId }) {
            selectedEntryId = null
            selectedEntryDetail = null
            detailLoading = false
        }
    }

    BackHandler(enabled = selectedEntryId != null) {
        selectedEntryId = null
    }
    BackHandler(enabled = editorForm != null) {
        editorForm = null
    }
    BackHandler(enabled = groupEditorForm != null) {
        groupEditorForm = null
    }
    BackHandler(enabled = securityPanelVisible) {
        securityPanelVisible = false
    }

    VaultScreenContent(
        uiState = uiState,
        securitySettings = securitySettings,
        securityPanelVisible = securityPanelVisible,
        uiScale = uiScale,
        loading = loading,
        detailLoading = detailLoading,
        layoutDensity = layoutDensity,
        snackbarHostState = snackbarHostState,
        onSelectGroup = {
            selectedGroup = it
        },
        onToggleSearch = {
            searchMode = !searchMode
            if (!searchMode) searchQuery = ""
        },
        onSearchQueryChange = { searchQuery = it },
        onEntryClick = {
            if (selectedEntryId == it && selectedEntryDetail != null) return@VaultScreenContent
            detailCache[it]?.let { cached ->
                selectedEntryDetail = cached
                detailLoading = false
            }
            selectedEntryId = it
        },
        onAddEntry = {
            val defaultGroup = if (selectedGroup is GroupId.Custom || selectedGroup == GroupId.All) selectedGroup else GroupId.All
            editorForm = EntryEditorForm(groupId = defaultGroup)
        },
        onEditEntry = {
            uiState.selectedEntry?.let { detail ->
                val entry = entries.firstOrNull { it.id == detail.id }
                editorForm = detail.toEditorForm(entry?.groupId ?: GroupId.All)
            }
        },
        onOpenGroupEditor = { groupEditorForm = GroupEditorForm() },
        onOpenSecurityPanel = { securityPanelVisible = true },
        onDismissSecurityPanel = { securityPanelVisible = false },
        onSecuritySettingsChange = {
            securitySettings = it
            onSecuritySettingsChange(it)
        },
        onUiScaleChange = {
            uiScale = it
            onUiScaleChange(it)
        },
        onDismissDetail = { selectedEntryId = null },
        onDismissEditor = { editorForm = null },
        onRequestLockSetup = onRequestLockSetup,
        onRequestLockNow = onRequestLockNow,
        onRequestChangePassword = onRequestChangePassword,
        onRequestDisableAppLock = onRequestDisableAppLock,
        onDismissGroupEditor = { groupEditorForm = null },
        onEditorFormChange = { editorForm = it },
        onGroupEditorFormChange = { groupEditorForm = it },
        onCopyField = { label, value ->
            clipboardManager.setText(AnnotatedString(value))
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (securitySettings.autoClearClipboardEnabled) "已复制$label（后续将支持自动清空）" else "已复制$label"
                )
            }
        },
        onSaveEntry = { form ->
            scope.launch {
                val targetGroup = form.groupId
                val entryId = form.id?.takeIf { it.isNotBlank() } ?: ((entries.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1).toString()
                val entry = EntryUiModel(
                    id = entryId,
                    name = form.name.ifBlank { "未命名凭据" },
                    iconEmoji = form.iconEmoji.ifBlank { "🔐" },
                    groupId = targetGroup,
                    isFavorite = targetGroup == GroupId.Favorites,
                    isRecent = true,
                    isWeak = form.password.length in 1..6
                )
                val detail = EntryDetailUiModel(
                    id = entryId,
                    name = entry.name,
                    iconEmoji = entry.iconEmoji,
                    username = form.username,
                    password = form.password,
                    website = form.website.ifBlank { null },
                    note = form.note.ifBlank { null },
                    customFields = form.customFields.filter { it.label.isNotBlank() || it.value.isNotBlank() }.map {
                        it.copy(label = it.label.ifBlank { "自定义字段" })
                    }
                )
                repository.upsertEntry(entry)
                repository.upsertEntryDetail(detail)
                upsertLocalEntry(entry)
                cacheEntryDetail(detail)
                selectedEntryDetail = detail
                detailLoading = false
                selectedEntryId = entryId
                editorForm = null
                snackbarHostState.showSnackbar(if (form.id.isNullOrBlank()) "已新增凭据" else "已更新凭据")
            }
        },
        onSaveGroup = { form ->
            scope.launch {
                val key = form.key.ifBlank { form.name.lowercase().replace(" ", "-") }
                val newGroup = GroupUiModel(
                    id = GroupId.Custom(key),
                    name = form.name.ifBlank { "新分组" },
                    count = 0,
                    icon = Icons.Rounded.FolderOpen,
                    isBuiltIn = false
                )
                repository.addGroup(newGroup)
                addLocalGroup(newGroup)
                selectedGroup = newGroup.id
                groupEditorForm = null
                snackbarHostState.showSnackbar("已创建分组：${newGroup.name}")
            }
        }
    )
}

@Composable
private fun VaultScreenContent(
    uiState: VaultUiState,
    securitySettings: SecuritySettings,
    securityPanelVisible: Boolean,
    uiScale: Float,
    loading: Boolean,
    detailLoading: Boolean,
    layoutDensity: VaultLayoutDensity,
    snackbarHostState: SnackbarHostState,
    onRequestLockSetup: () -> Unit,
    onRequestLockNow: () -> Unit,
    onRequestChangePassword: () -> Unit,
    onRequestDisableAppLock: () -> Unit,
    onSelectGroup: (GroupId) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEntryClick: (String) -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: () -> Unit,
    onOpenGroupEditor: () -> Unit,
    onOpenSecurityPanel: () -> Unit,
    onDismissSecurityPanel: () -> Unit,
    onSecuritySettingsChange: (SecuritySettings) -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onDismissDetail: () -> Unit,
    onDismissEditor: () -> Unit,
    onDismissGroupEditor: () -> Unit,
    onEditorFormChange: (EntryEditorForm) -> Unit,
    onGroupEditorFormChange: (GroupEditorForm) -> Unit,
    onCopyField: (String, String) -> Unit,
    onSaveEntry: (EntryEditorForm) -> Unit,
    onSaveGroup: (GroupEditorForm) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "新增凭据")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(layoutDensity.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(layoutDensity.paneGap)
            ) {
                LeftGroupsPane(
                    groups = uiState.groups,
                    selectedGroup = uiState.selectedGroup,
                    onGroupClick = onSelectGroup,
                    onManageGroups = onOpenGroupEditor,
                    onOpenSecurityPanel = onOpenSecurityPanel,
                    layoutDensity = layoutDensity
                )
                RightEntriesList(
                    entries = uiState.visibleEntries,
                    searchQuery = uiState.searchQuery,
                    searchMode = uiState.searchMode,
                    selectedGroup = uiState.groups.firstOrNull { it.id == uiState.selectedGroup },
                    loading = loading,
                    onSearchQueryChange = onSearchQueryChange,
                    onToggleSearch = onToggleSearch,
                    onEntryClick = onEntryClick,
                    onAddClick = onAddEntry,
                    layoutDensity = layoutDensity,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = detailLoading || uiState.selectedEntry != null,
                enter = fadeIn() + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + scaleOut(targetScale = 0.96f)
            ) {
                when {
                    detailLoading -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.18f)),
                            color = Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 24.dp
                                ) {
                                    Text(
                                        text = "正在读取详情…",
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    uiState.selectedEntry != null -> {
                        EntryDetailOverlay(
                            entry = uiState.selectedEntry,
                            onDismiss = onDismissDetail,
                            onEdit = onEditEntry,
                            onCopy = onCopyField,
                            obscureSensitiveContent = securitySettings.obscureSensitiveContentEnabled
                        )
                    }
                }
            }

            uiState.editorForm?.let { form ->
                EntryEditorDialog(
                    form = form,
                    groups = uiState.editableGroups,
                    onDismiss = onDismissEditor,
                    onFormChange = onEditorFormChange,
                    onSave = onSaveEntry
                )
            }

            uiState.groupEditorForm?.let { form ->
                GroupEditorDialog(
                    form = form,
                    onDismiss = onDismissGroupEditor,
                    onFormChange = onGroupEditorFormChange,
                    onSave = onSaveGroup
                )
            }

            if (securityPanelVisible) {
                SecuritySettingsDialog(
                    settings = securitySettings,
                    uiScale = uiScale,
                    onDismiss = onDismissSecurityPanel,
                    onSettingsChange = onSecuritySettingsChange,
                    onUiScaleChange = onUiScaleChange,
                    onRequestLockSetup = onRequestLockSetup,
                    onRequestLockNow = onRequestLockNow,
                    onRequestChangePassword = onRequestChangePassword,
                    onRequestDisableAppLock = onRequestDisableAppLock
                )
            }
        }
    }
}

@Composable
private fun LeftGroupsPane(
    groups: List<GroupUiModel>,
    selectedGroup: GroupId,
    onGroupClick: (GroupId) -> Unit,
    onManageGroups: () -> Unit,
    onOpenSecurityPanel: () -> Unit,
    layoutDensity: VaultLayoutDensity
) {
    val pinnedGroups = groups.filter { it.id == GroupId.Favorites || it.id == GroupId.Recent || it.id == GroupId.Weak }
    val primaryGroups = groups.filter { it.id == GroupId.All || it.id is GroupId.Custom }
    Surface(
        modifier = Modifier.fillMaxHeight().width(layoutDensity.groupsPaneWidth),
        shape = RoundedCornerShape(layoutDensity.paneCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                pinnedGroups.forEach { group ->
                    val selected = group.id == selectedGroup
                    Text(
                        text = group.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onGroupClick(group.id) }
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ) {}
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(primaryGroups) { group ->
                    val selected = group.id == selectedGroup
                    Text(
                        text = group.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onGroupClick(group.id) }
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onManageGroups,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "添加分组", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onOpenSecurityPanel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "安全设置", tint = MaterialTheme.colorScheme.primary)
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
    selectedGroup: GroupUiModel?,
    loading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onEntryClick: (String) -> Unit,
    onAddClick: () -> Unit,
    layoutDensity: VaultLayoutDensity,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(layoutDensity.paneCorner),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VaultTopBar(
                selectedGroup = selectedGroup,
                entriesCount = entries.size,
                searchQuery = searchQuery,
                searchMode = searchMode,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onAddClick = onAddClick,
                layoutDensity = layoutDensity
            )
            if (loading) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Text("正在加载密码库…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 30.dp)
                        ) {
                            Text("🫧", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "此分组暂无凭据" else "没有找到匹配的凭据",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isBlank()) "你可以点击右上角的新增按钮创建第一条凭据" else "试试更短的关键词，或者换一个分组看看",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = layoutDensity.topBarHorizontal - 2.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(layoutDensity.listItemSpacing)) {
                    items(entries) { entry ->
                        EntryNameCard(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                            layoutDensity = layoutDensity
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultTopBar(
    selectedGroup: GroupUiModel?,
    entriesCount: Int,
    searchQuery: String,
    searchMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddClick: () -> Unit,
    layoutDensity: VaultLayoutDensity
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = layoutDensity.topBarHorizontal, vertical = layoutDensity.topBarVertical)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(selectedGroup?.name ?: "我的凭据", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (searchQuery.isBlank()) "当前分组共 ${entriesCount} 条凭据" else "当前搜索到 ${entriesCount} 条结果",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Rounded.Add, contentDescription = "新增", tint = MaterialTheme.colorScheme.primary)
            }
        }
        AnimatedVisibility(visible = searchMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("搜索凭据名称") },
                singleLine = true,
                shape = RoundedCornerShape(22.dp)
            )
        }
    }
}

@Composable
private fun EntryNameCard(entry: EntryUiModel, onClick: () -> Unit, layoutDensity: VaultLayoutDensity) {
    val elevation by animateDpAsState(targetValue = 4.dp, label = "entry_elevation")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(layoutDensity.paneCorner - 4.dp)),
        shape = RoundedCornerShape(layoutDensity.paneCorner - 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = elevation,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = layoutDensity.listItemHorizontal, vertical = layoutDensity.listItemVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) { Text(entry.iconEmoji) }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntryDetailOverlay(
    entry: EntryDetailUiModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopy: (String, String) -> Unit,
    obscureSensitiveContent: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.84f).fillMaxHeight(0.78f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 10.dp,
            shadowElevation = 18.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(46.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(entry.iconEmoji) }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("点击字段值即可复制", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "关闭") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (obscureSensitiveContent) {
                    SecurityHintCard()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CopyableField("账号", entry.username, onCopy)
                    SecretCopyableField("密码", entry.password, onCopy, obscureSensitiveContent)
                    entry.website?.let { CopyableField("网址", it, onCopy) }
                    entry.note?.let { StaticField("备注", it) }
                    if (entry.customFields.isNotEmpty()) {
                        Text("自定义信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        entry.customFields.forEach { field -> CustomFieldRow(field, onCopy, obscureSensitiveContent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecuritySettingsDialog(
    settings: SecuritySettings,
    uiScale: Float,
    onDismiss: () -> Unit,
    onSettingsChange: (SecuritySettings) -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onRequestLockSetup: () -> Unit,
    onRequestLockNow: () -> Unit,
    onRequestChangePassword: () -> Unit,
    onRequestDisableAppLock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("完成")
            }
        },
        title = { Text("安全设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "这是第一版安全能力结构，后续会继续接入应用锁、生物识别、截图保护和剪贴板自动清理。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecuritySettingRow("启用应用锁", settings.appLockEnabled) { enabled ->
                    if (enabled) {
                        onSettingsChange(settings.copy(appLockEnabled = true))
                        onRequestLockSetup()
                    } else {
                        onRequestDisableAppLock()
                    }
                }
                if (settings.appLockEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onRequestChangePassword) {
                            Text("修改主密码")
                        }
                        TextButton(onClick = onRequestLockNow) {
                            Text("立即锁定")
                        }
                    }
                }
                SecuritySettingRow("启用生物解锁", settings.biometricUnlockEnabled) {
                    onSettingsChange(settings.copy(biometricUnlockEnabled = it))
                }
                SecuritySettingRow("自动清理剪贴板", settings.autoClearClipboardEnabled) {
                    onSettingsChange(settings.copy(autoClearClipboardEnabled = it))
                }
                SecuritySettingRow("阻止截图（预留）", settings.blockScreenshotsEnabled) {
                    onSettingsChange(settings.copy(blockScreenshotsEnabled = it))
                }
                SecuritySettingRow("隐藏敏感信息", settings.obscureSensitiveContentEnabled) {
                    onSettingsChange(settings.copy(obscureSensitiveContentEnabled = it))
                }
                SecuritySettingRow("深色模式", settings.darkModeEnabled) {
                    onSettingsChange(settings.copy(darkModeEnabled = it))
                }
                SecuritySettingRow("切到后台自动锁定", settings.autoLockOnBackgroundEnabled) {
                    onSettingsChange(settings.copy(autoLockOnBackgroundEnabled = it))
                }
                Text(
                    "页面密度（拖动条）: ${String.format("%.2f", uiScale)}x",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = uiScale,
                    onValueChange = { onUiScaleChange(it) },
                    valueRange = 0.35f..2.2f
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "左侧更紧凑",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onUiScaleChange(1.0f) }) {
                        Text("恢复标准")
                    }
                    Text(
                        "右侧更舒展",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
private fun SecuritySettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecurityHintCard() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "已启用敏感信息保护模式",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EntryEditorDialog(
    form: EntryEditorForm,
    groups: List<GroupUiModel>,
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (form.id == null) "新增凭据" else "编辑凭据") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "当前已支持调整分组与多个自定义字段。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(form.name, { onFormChange(form.copy(name = it)) }, label = { Text("名称") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(form.iconEmoji, { onFormChange(form.copy(iconEmoji = it)) }, label = { Text("图标 / Emoji") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                var groupMenuExpanded by remember { mutableStateOf(false) }
                val selectedGroupName = groups.firstOrNull { it.id == form.groupId }?.name ?: "请选择分组"
                Text("分组", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { groupMenuExpanded = true },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedGroupName, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "展开分组")
                        }
                    }
                    DropdownMenu(
                        expanded = groupMenuExpanded,
                        onDismissRequest = { groupMenuExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    onFormChange(form.copy(groupId = group.id))
                                    groupMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(form.username, { onFormChange(form.copy(username = it)) }, label = { Text("账号") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(form.password, { onFormChange(form.copy(password = it)) }, label = { Text("密码") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(form.website, { onFormChange(form.copy(website = it)) }, label = { Text("网址") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(form.note, { onFormChange(form.copy(note = it)) }, label = { Text("备注") }, minLines = 3, shape = RoundedCornerShape(20.dp))
                Text("自定义字段", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                form.customFields.forEachIndexed { index, field ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("字段 ${index + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        if (form.customFields.size > 1) {
                            TextButton(
                                onClick = {
                                    onFormChange(form.copy(customFields = form.customFields.filterIndexed { currentIndex, _ -> currentIndex != index }))
                                }
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("删除")
                            }
                        }
                    }
                    OutlinedTextField(
                        field.label,
                        { value ->
                            onFormChange(form.copy(customFields = form.customFields.toMutableList().also { it[index] = field.copy(label = value) }))
                        },
                        label = { Text("字段名称 ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    OutlinedTextField(
                        field.value,
                        { value ->
                            onFormChange(form.copy(customFields = form.customFields.toMutableList().also { it[index] = field.copy(value = value) }))
                        },
                        label = { Text("字段值 ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                TextButton(onClick = { onFormChange(form.copy(customFields = form.customFields + CustomFieldUiModel(label = "", value = ""))) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增自定义字段")
                }
            }
        }
    )
}

@Composable
private fun GroupEditorDialog(form: GroupEditorForm, onDismiss: () -> Unit, onFormChange: (GroupEditorForm) -> Unit, onSave: (GroupEditorForm) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(form) }) { Text("创建分组") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("新建分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(form.name, { onFormChange(form.copy(name = it)) }, label = { Text("分组名称") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(form.key, { onFormChange(form.copy(key = it)) }, label = { Text("分组键值（可选）") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                OutlinedTextField(form.iconEmoji, { onFormChange(form.copy(iconEmoji = it)) }, label = { Text("图标 / Emoji（预留）") }, singleLine = true, shape = RoundedCornerShape(20.dp))
            }
        }
    )
}

@Composable
private fun CopyableField(label: String, value: String, onCopy: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
        ) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onCopy(label, value) }.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制$label", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SecretCopyableField(label: String, value: String, onCopy: (String, String) -> Unit, obscureSensitiveContent: Boolean) {
    var visible by remember { mutableStateOf(false) }
    val shouldHide = obscureSensitiveContent || !visible
    val displayValue = if (shouldHide) "•".repeat(maxOf(8, value.length.coerceAtMost(16))) else value
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(displayValue, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).clickable { onCopy(label, value) })
                IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (visible) "隐藏密码" else "显示密码") }
                IconButton(onClick = { onCopy(label, value) }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "复制$label") }
            }
        }
    }
}

@Composable
private fun StaticField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
        ) {
            Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp))
        }
    }
}

@Composable
private fun CustomFieldRow(field: CustomFieldUiModel, onCopy: (String, String) -> Unit, obscureSensitiveContent: Boolean) {
    if (field.isSecret) SecretCopyableField(field.label, field.value, onCopy, obscureSensitiveContent)
    else if (field.copyable) CopyableField(field.label, field.value, onCopy)
    else StaticField(field.label, field.value)
}
