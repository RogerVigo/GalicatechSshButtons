package com.galaicatech.sshbuttons.data

import java.util.UUID

enum class AuthType(val label: String) {
    PASSWORD("Password"),
    PRIVATE_KEY("Private key");

    companion object {
        fun fromName(value: String?): AuthType =
            entries.firstOrNull { it.name == value } ?: PASSWORD
    }
}

enum class HostKeyPolicy(val label: String) {
    TRUST_ON_FIRST_USE("Trust on first use"),
    ACCEPT_ALL("Accept any host key");

    companion object {
        fun fromName(value: String?): HostKeyPolicy =
            entries.firstOrNull { it.name == value } ?: TRUST_ON_FIRST_USE
    }
}

data class HostConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authType: AuthType = AuthType.PASSWORD,
    val password: String = "",
    val privateKey: String = "",
    val passphrase: String = "",
    val hostKeyPolicy: HostKeyPolicy = HostKeyPolicy.TRUST_ON_FIRST_USE,
    val keepAliveSeconds: Int = 30,
    val connectTimeoutSeconds: Int = 15,
    val commandTimeoutSeconds: Int = 30
) {
    val displayName: String
        get() = name.ifBlank { host.ifBlank { "Unnamed host" } }

    val target: String
        get() = "$username@$host:$port"

    fun sanitized(): HostConfig = copy(
        name = name.trim(),
        host = host.trim(),
        port = port.coerceIn(1, 65535),
        username = username.trim(),
        keepAliveSeconds = keepAliveSeconds.coerceIn(0, 3600),
        connectTimeoutSeconds = connectTimeoutSeconds.coerceIn(1, 300),
        commandTimeoutSeconds = commandTimeoutSeconds.coerceIn(0, 86_400)
    )
}

data class CommandButton(
    val id: String = UUID.randomUUID().toString(),
    val hostId: String,
    val label: String,
    val command: String,
    val requireConfirmation: Boolean = false
) {
    fun sanitized(): CommandButton = copy(
        label = label.trim().ifBlank { command.lineSequence().firstOrNull()?.take(40).orEmpty() },
        command = command.trim()
    )
}

data class HostSection(
    val host: HostConfig,
    val buttons: List<CommandButton>
)
