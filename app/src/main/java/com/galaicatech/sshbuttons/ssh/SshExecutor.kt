package com.galaicatech.sshbuttons.ssh

import android.content.Context
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.galaicatech.sshbuttons.data.AuthType
import com.galaicatech.sshbuttons.data.HostConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.OutputStream
import java.util.Properties

data class SshResult(
    val exitCode: Int?,
    val timedOut: Boolean
)

object SshExecutor {

    suspend fun execute(
        context: Context,
        host: HostConfig,
        command: String,
        onOutput: (String) -> Unit
    ): SshResult = withContext(Dispatchers.IO) {
        require(host.host.isNotBlank()) { "Host address is empty" }
        require(host.username.isNotBlank()) { "Username is empty" }
        require(command.isNotBlank()) { "Command is empty" }

        var session: Session? = null
        var channel: ChannelExec? = null
        var reader: Job? = null

        try {
            val jsch = JSch()
            val sshSession = jsch.getSession(host.username, host.host, host.port).apply {
                setConfig(buildConfig(host))
                hostKeyRepository = TofuHostKeyRepository(context, host.hostKeyPolicy)
                setDaemonThread(true)
                if (host.keepAliveSeconds > 0) {
                    setServerAliveInterval(host.keepAliveSeconds * 1000)
                    setServerAliveCountMax(3)
                }
            }
            session = sshSession

            when (host.authType) {
                AuthType.PASSWORD -> sshSession.setPassword(host.password)
                AuthType.PRIVATE_KEY -> {
                    require(host.privateKey.isNotBlank()) { "Private key is empty" }
                    jsch.addIdentity(
                        "ssh-buttons-${host.id}",
                        host.privateKey.toByteArray(Charsets.UTF_8),
                        null,
                        host.passphrase.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8)
                    )
                }
            }

            sshSession.connect(host.connectTimeoutSeconds * 1000)

            val exec = (sshSession.openChannel("exec") as ChannelExec).also { channel = it }
            exec.setCommand(command)
            exec.setInputStream(null)
            exec.setErrStream(object : OutputStream() {
                override fun write(b: Int) {
                    onOutput(String(byteArrayOf(b.toByte()), Charsets.UTF_8))
                }

                override fun write(b: ByteArray, off: Int, len: Int) {
                    if (len > 0) onOutput(String(b, off, len, Charsets.UTF_8))
                }
            })

            val stdout = exec.inputStream
            exec.connect(host.connectTimeoutSeconds * 1000)

            reader = launch {
                val buffer = ByteArray(8192)
                try {
                    while (isActive) {
                        val read = stdout.read(buffer)
                        if (read < 0) break
                        if (read > 0) onOutput(String(buffer, 0, read, Charsets.UTF_8))
                    }
                } catch (_: IOException) {
                }
            }

            var timedOut = false
            val timeoutMs = host.commandTimeoutSeconds * 1000L
            try {
                if (timeoutMs > 0) {
                    withTimeout(timeoutMs) {
                        while (!exec.isClosed) delay(100)
                    }
                } else {
                    while (!exec.isClosed) delay(100)
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                timedOut = true
            }

            if (!timedOut) {
                withTimeoutOrNull(3000L) { reader.join() }
            }

            SshResult(
                exitCode = if (timedOut) null else exec.exitStatus.takeIf { it >= 0 },
                timedOut = timedOut
            )
        } finally {
            runCatching { channel?.disconnect() }
            runCatching { session?.disconnect() }
            reader?.cancel()
        }
    }

    private fun buildConfig(host: HostConfig): Properties = Properties().apply {
        put(
            "StrictHostKeyChecking",
            if (host.hostKeyPolicy == com.galaicatech.sshbuttons.data.HostKeyPolicy.ACCEPT_ALL) "no" else "yes"
        )
        put(
            "PreferredAuthentications",
            if (host.authType == AuthType.PASSWORD) "password,keyboard-interactive" else "publickey"
        )
        put("ConnectTimeout", (host.connectTimeoutSeconds * 1000).toString())
        put("HashKnownHosts", "no")
    }
}
