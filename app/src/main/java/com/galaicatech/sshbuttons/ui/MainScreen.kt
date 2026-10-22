package com.galaicatech.sshbuttons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galaicatech.sshbuttons.data.CommandButton
import com.galaicatech.sshbuttons.data.HostConfig
import com.galaicatech.sshbuttons.data.HostSection

private data class CommandEditorTarget(
    val button: CommandButton,
    val initialHostId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    adsAllowed: Boolean = false,
    viewModel: MainViewModel = viewModel()
) {
    val hosts by viewModel.hosts.collectAsStateWithLifecycle()
    val buttons by viewModel.buttons.collectAsStateWithLifecycle()
    val execution by viewModel.execution.collectAsStateWithLifecycle()

    var hostEditorTarget by remember { mutableStateOf<HostConfig?>(null) }
    var commandEditorTarget by remember { mutableStateOf<CommandEditorTarget?>(null) }
    var hostToDelete by remember { mutableStateOf<HostConfig?>(null) }
    var buttonToDelete by remember { mutableStateOf<CommandButton?>(null) }
    var commandToConfirm by remember { mutableStateOf<Pair<HostConfig, CommandButton>?>(null) }

    val sections = remember(hosts, buttons) {
        hosts.map { host -> HostSection(host, buttons.filter { it.hostId == host.id }) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SSH Buttons",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    FilledTonalButton(
                        onClick = { hostEditorTarget = HostConfig() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Host")
                    }
                }
            )
        },
        bottomBar = {
            if (adsAllowed) {
                AdBanner()
            } else {
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
            }
        }
    ) { innerPadding ->
        if (sections.isEmpty()) {
            EmptyState(
                onAddHost = { hostEditorTarget = HostConfig() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sections, key = { it.host.id }) { section ->
                    HostSectionCard(
                        section = section,
                        onAddCommand = {
                            commandEditorTarget = CommandEditorTarget(
                                button = CommandButton(
                                    hostId = section.host.id,
                                    label = "",
                                    command = ""
                                ),
                                initialHostId = section.host.id
                            )
                        },
                        onEditHost = { hostEditorTarget = section.host },
                        onDeleteHost = { hostToDelete = section.host },
                        onRunCommand = { button ->
                            if (button.requireConfirmation) {
                                commandToConfirm = section.host to button
                            } else {
                                viewModel.runCommand(section.host, button)
                            }
                        },
                        onEditCommand = { button ->
                            commandEditorTarget = CommandEditorTarget(button, button.hostId)
                        },
                        onDeleteCommand = { button -> buttonToDelete = button }
                    )
                }
            }
        }
    }

    hostEditorTarget?.let { target ->
        HostEditorDialog(
            host = target,
            isNew = hosts.none { it.id == target.id },
            onDismiss = { hostEditorTarget = null },
            onSave = { edited ->
                viewModel.saveHost(edited)
                hostEditorTarget = null
            },
            onForgetHostKey = viewModel::forgetHostKey
        )
    }

    commandEditorTarget?.let { target ->
        CommandEditorDialog(
            button = target.button,
            hosts = hosts,
            isNew = buttons.none { it.id == target.button.id },
            initialHostId = target.initialHostId,
            onDismiss = { commandEditorTarget = null },
            onSave = { edited ->
                viewModel.saveButton(edited)
                commandEditorTarget = null
            }
        )
    }

    hostToDelete?.let { host ->
        AlertDialog(
            onDismissRequest = { hostToDelete = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Delete host?") },
            text = {
                Text(
                    "“${host.displayName}” and all commands assigned to it will be removed. " +
                        "The saved host key will also be forgotten."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forgetHostKey(host)
                        viewModel.deleteHost(host.id)
                        hostToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { hostToDelete = null }) { Text("Cancel") }
            }
        )
    }

    buttonToDelete?.let { button ->
        AlertDialog(
            onDismissRequest = { buttonToDelete = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Delete command?") },
            text = { Text("“${button.label.ifBlank { button.command }}” will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteButton(button.id)
                        buttonToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { buttonToDelete = null }) { Text("Cancel") }
            }
        )
    }

    commandToConfirm?.let { (host, button) ->
        AlertDialog(
            onDismissRequest = { commandToConfirm = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text("Run this command?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${host.displayName} · ${host.target}")
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = button.command,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        commandToConfirm = null
                        viewModel.runCommand(host, button)
                    }
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { commandToConfirm = null }) { Text("Cancel") }
            }
        )
    }

    execution?.let { state ->
        OutputDialog(
            state = state,
            onCancel = viewModel::cancelCurrentExecution,
            onDismiss = viewModel::dismissExecution
        )
    }
}

@Composable
private fun HostSectionCard(
    section: HostSection,
    onAddCommand: () -> Unit,
    onEditHost: () -> Unit,
    onDeleteHost: () -> Unit,
    onRunCommand: (CommandButton) -> Unit,
    onEditCommand: (CommandButton) -> Unit,
    onDeleteCommand: (CommandButton) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.host.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = section.host.target,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onAddCommand) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add command to ${section.host.displayName}"
                )
            }
            HostMenu(onEditHost = onEditHost, onDeleteHost = onDeleteHost)
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )

        if (section.buttons.isEmpty()) {
            Text(
                text = "No commands yet. Tap + to add one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                section.buttons.forEach { button ->
                    CommandRow(
                        button = button,
                        onRun = { onRunCommand(button) },
                        onEdit = { onEditCommand(button) },
                        onDelete = { onDeleteCommand(button) }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HostMenu(
    onEditHost: () -> Unit,
    onDeleteHost: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Host options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit host") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEditHost()
                }
            )
            DropdownMenuItem(
                text = {
                    Text("Delete host", color = MaterialTheme.colorScheme.error)
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onDeleteHost()
                }
            )
        }
    }
}

@Composable
private fun CommandRow(
    button: CommandButton,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .clickable(onClick = onRun)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = button.label.ifBlank { button.command },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (button.requireConfirmation) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = "Asks for confirmation",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = button.command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Command options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit command") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Delete command", color = MaterialTheme.colorScheme.error)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddHost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(42.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No hosts yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add an SSH host, then create command buttons for the tasks you run most often.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAddHost) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add your first host")
        }
    }
}
