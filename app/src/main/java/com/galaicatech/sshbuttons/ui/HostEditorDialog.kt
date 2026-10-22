package com.galaicatech.sshbuttons.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.galaicatech.sshbuttons.data.AuthType
import com.galaicatech.sshbuttons.data.HostConfig
import com.galaicatech.sshbuttons.data.HostKeyPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HostEditorDialog(
    host: HostConfig,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (HostConfig) -> Unit,
    onForgetHostKey: (HostConfig) -> Unit
) {
    var name by remember(host.id) { mutableStateOf(host.name) }
    var hostAddress by remember(host.id) { mutableStateOf(host.host) }
    var portText by remember(host.id) { mutableStateOf(host.port.toString()) }
    var username by remember(host.id) { mutableStateOf(host.username) }
    var authType by remember(host.id) { mutableStateOf(host.authType) }
    var password by remember(host.id) { mutableStateOf(host.password) }
    var privateKey by remember(host.id) { mutableStateOf(host.privateKey) }
    var passphrase by remember(host.id) { mutableStateOf(host.passphrase) }
    var hostKeyPolicy by remember(host.id) { mutableStateOf(host.hostKeyPolicy) }
    var keepAliveText by remember(host.id) { mutableStateOf(host.keepAliveSeconds.toString()) }
    var connectTimeoutText by remember(host.id) {
        mutableStateOf(host.connectTimeoutSeconds.toString())
    }
    var commandTimeoutText by remember(host.id) {
        mutableStateOf(host.commandTimeoutSeconds.toString())
    }
    var passwordVisible by remember { mutableStateOf(false) }
    var passphraseVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                }.getOrNull()
            }
            if (text != null) {
                privateKey = text
                authType = AuthType.PRIVATE_KEY
            } else {
                Toast.makeText(context, "Could not read the key file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val portValue = portText.toIntOrNull()
    val keepAliveValue = keepAliveText.toIntOrNull()
    val connectTimeoutValue = connectTimeoutText.toIntOrNull()
    val commandTimeoutValue = commandTimeoutText.toIntOrNull()

    val hostMissing = hostAddress.isBlank()
    val usernameMissing = username.isBlank()
    val portInvalid = portValue == null || portValue !in 1..65535
    val keepAliveInvalid = keepAliveValue == null || keepAliveValue !in 0..3600
    val connectTimeoutInvalid = connectTimeoutValue == null || connectTimeoutValue !in 1..300
    val commandTimeoutInvalid =
        commandTimeoutValue == null || commandTimeoutValue !in 0..86_400
    val passwordMissing = authType == AuthType.PASSWORD && password.isEmpty()
    val keyMissing = authType == AuthType.PRIVATE_KEY && privateKey.isBlank()

    val formValid = !hostMissing && !usernameMissing && !portInvalid &&
        !keepAliveInvalid && !connectTimeoutInvalid && !commandTimeoutInvalid &&
        !passwordMissing && !keyMissing

    fun save() {
        showErrors = true
        if (!formValid) return
        onSave(
            host.copy(
                name = name,
                host = hostAddress,
                port = portValue ?: 22,
                username = username,
                authType = authType,
                password = if (authType == AuthType.PASSWORD) password else "",
                privateKey = if (authType == AuthType.PRIVATE_KEY) privateKey else "",
                passphrase = if (authType == AuthType.PRIVATE_KEY) passphrase else "",
                hostKeyPolicy = hostKeyPolicy,
                keepAliveSeconds = keepAliveValue ?: 0,
                connectTimeoutSeconds = connectTimeoutValue ?: 15,
                commandTimeoutSeconds = commandTimeoutValue ?: 30
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
                        text = if (isNew) "New host" else "Edit host",
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
                    SectionLabel("Connection")

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display name") },
                        placeholder = { Text("Optional, defaults to the address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hostAddress,
                        onValueChange = { hostAddress = it },
                        label = { Text("Host or IP address") },
                        placeholder = { Text("example.com or 192.168.1.10") },
                        singleLine = true,
                        isError = showErrors && hostMissing,
                        supportingText = if (showErrors && hostMissing) {
                            { Text("Host address is required") }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = "Port",
                            isError = showErrors && portInvalid,
                            maxLength = 5,
                            modifier = Modifier.width(130.dp)
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            isError = showErrors && usernameMissing,
                            supportingText = if (showErrors && usernameMissing) {
                                { Text("Required") }
                            } else {
                                null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    SectionLabel("Authentication")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuthType.entries.forEach { type ->
                            FilterChip(
                                selected = authType == type,
                                onClick = { authType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }

                    if (authType == AuthType.PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            },
                            isError = showErrors && passwordMissing,
                            supportingText = if (showErrors && passwordMissing) {
                                { Text("Password is required for password authentication") }
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = privateKey,
                            onValueChange = { privateKey = it },
                            label = { Text("Private key (PEM / OpenSSH)") },
                            placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----") },
                            minLines = 5,
                            maxLines = 11,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            isError = showErrors && keyMissing,
                            supportingText = if (showErrors && keyMissing) {
                                { Text("A private key is required") }
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { keyPicker.launch("*/*") }) {
                                Icon(
                                    Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Import key file")
                            }
                        }

                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("Key passphrase") },
                            placeholder = { Text("Optional") },
                            singleLine = true,
                            visualTransformation = if (passphraseVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                                    Icon(
                                        if (passphraseVisible) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = if (passphraseVisible) {
                                            "Hide passphrase"
                                        } else {
                                            "Show passphrase"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "Credentials are encrypted with the Android Keystore and never leave this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))
                    SectionLabel("Connection options")

                    Text("Host key verification", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HostKeyPolicy.entries.forEach { policy ->
                            FilterChip(
                                selected = hostKeyPolicy == policy,
                                onClick = { hostKeyPolicy = policy },
                                label = { Text(policy.label) }
                            )
                        }
                    }
                    Text(
                        text = if (hostKeyPolicy == HostKeyPolicy.TRUST_ON_FIRST_USE) {
                            "The first key presented by a host is remembered. A changed key blocks the connection."
                        } else {
                            "Any host key is accepted. This weakens protection against man-in-the-middle attacks."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hostKeyPolicy == HostKeyPolicy.ACCEPT_ALL) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            value = keepAliveText,
                            onValueChange = { keepAliveText = it },
                            label = "Keep-alive (s)",
                            isError = showErrors && keepAliveInvalid,
                            supportingText = "0 disables",
                            maxLength = 4,
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = connectTimeoutText,
                            onValueChange = { connectTimeoutText = it },
                            label = "Connect timeout (s)",
                            isError = showErrors && connectTimeoutInvalid,
                            maxLength = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    NumberField(
                        value = commandTimeoutText,
                        onValueChange = { commandTimeoutText = it },
                        label = "Command timeout (s)",
                        isError = showErrors && commandTimeoutInvalid,
                        supportingText = "0 means no timeout",
                        maxLength = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isNew && hostAddress.isNotBlank() && !portInvalid) {
                        OutlinedButton(
                            onClick = {
                                onForgetHostKey(
                                    host.copy(
                                        host = hostAddress.trim(),
                                        port = portValue ?: host.port
                                    )
                                )
                                Toast.makeText(
                                    context,
                                    "Saved host key cleared",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text("Forget saved host key")
                        }
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
                        Text(if (isNew) "Add host" else "Save host")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    maxLength: Int = 5,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.length <= maxLength && newValue.all(Char::isDigit)) {
                onValueChange(newValue)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        supportingText = when {
            isError -> {
                { Text("Invalid value") }
            }
            supportingText != null -> {
                { Text(supportingText) }
            }
            else -> null
        },
        modifier = modifier
    )
}
