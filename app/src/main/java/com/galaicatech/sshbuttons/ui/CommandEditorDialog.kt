package com.galaicatech.sshbuttons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.galaicatech.sshbuttons.data.CommandButton
import com.galaicatech.sshbuttons.data.HostConfig

private val commandHints = listOf(
    "uptime",
    "whoami",
    "hostname -I",
    "df -h",
    "free -m",
    "ls -lah",
    "top -bn1 | head -20",
    "docker ps",
    "systemctl status ssh",
    "journalctl -n 50 --no-pager",
    "ss -tulpn",
    "uname -a",
    "reboot"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandEditorDialog(
    button: CommandButton,
    hosts: List<HostConfig>,
    isNew: Boolean,
    initialHostId: String,
    onDismiss: () -> Unit,
    onSave: (CommandButton) -> Unit
) {
    val fallbackHostId = hosts.firstOrNull { it.id == button.hostId }?.id
        ?: hosts.firstOrNull { it.id == initialHostId }?.id
        ?: hosts.firstOrNull()?.id.orEmpty()

    var label by remember(button.id) { mutableStateOf(button.label) }
    var command by remember(button.id) { mutableStateOf(button.command) }
    var selectedHostId by remember(button.id) { mutableStateOf(fallbackHostId) }
    var requireConfirmation by remember(button.id) {
        mutableStateOf(button.requireConfirmation)
    }
    var hostMenuExpanded by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val selectedHost = hosts.firstOrNull { it.id == selectedHostId }
    val commandMissing = command.isBlank()
    val hostMissing = selectedHostId.isBlank()
    val formValid = !commandMissing && !hostMissing

    fun save() {
        showErrors = true
        if (!formValid) return
        onSave(
            button.copy(
                hostId = selectedHostId,
                label = label,
                command = command,
                requireConfirmation = requireConfirmation
            ).sanitized()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                    Text(
                        text = if (isNew) "New command" else "Edit command",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    TextButton(onClick = { save() }) { Text("Save") }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Assigned host",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box {
                        OutlinedButton(
                            onClick = { hostMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = selectedHost?.displayName ?: "Select a host",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (selectedHost != null) {
                                    Text(
                                        text = selectedHost.target,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = hostMenuExpanded,
                            onDismissRequest = { hostMenuExpanded = false }
                        ) {
                            hosts.forEach { host ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(host.displayName)
                                            Text(
                                                host.target,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Dns, contentDescription = null)
                                    },
                                    onClick = {
                                        selectedHostId = host.id
                                        hostMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (showErrors && hostMissing) {
                        Text(
                            text = "Select a host",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Button label") },
                        placeholder = { Text("Optional, defaults to the command") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command") },
                        placeholder = { Text("df -h") },
                        minLines = 4,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        isError = showErrors && commandMissing,
                        supportingText = if (showErrors && commandMissing) {
                            { Text("A command is required") }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Useful commands",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap a hint to use it as the command.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commandHints.forEach { hint ->
                            AssistChip(
                                onClick = { command = hint },
                                label = {
                                    Text(
                                        text = hint,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask before running",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Show a confirmation prompt for this command.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requireConfirmation,
                            onCheckedChange = { requireConfirmation = it }
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = { save() }) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isNew) "Add command" else "Save command")
                    }
                }
            }
        }
    }
}
