package com.galaicatech.sshbuttons.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaicatech.sshbuttons.data.AppRepository
import com.galaicatech.sshbuttons.data.CommandButton
import com.galaicatech.sshbuttons.data.HostConfig
import com.galaicatech.sshbuttons.ssh.SshExecutor
import com.galaicatech.sshbuttons.ssh.TofuHostKeyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ExecutionState(
    val id: String,
    val label: String,
    val hostName: String,
    val target: String,
    val command: String,
    val output: String = "",
    val running: Boolean = true,
    val exitCode: Int? = null,
    val error: String? = null,
    val timedOut: Boolean = false,
    val cancelled: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    val hosts: StateFlow<List<HostConfig>> = repository.hosts
    val buttons: StateFlow<List<CommandButton>> = repository.buttons

    private val _execution = MutableStateFlow<ExecutionState?>(null)
    val execution: StateFlow<ExecutionState?> = _execution.asStateFlow()

    private var executionJob: Job? = null

    fun saveHost(host: HostConfig) = repository.saveHost(host)

    fun deleteHost(hostId: String) = repository.deleteHost(hostId)

    fun saveButton(button: CommandButton) = repository.saveButton(button)

    fun deleteButton(buttonId: String) = repository.deleteButton(buttonId)

    fun forgetHostKey(host: HostConfig) =
        TofuHostKeyRepository.forget(getApplication(), host.host, host.port)

    fun runCommand(host: HostConfig, button: CommandButton) {
        executionJob?.cancel()
        executionJob = null

        val executionId = UUID.randomUUID().toString()
        _execution.value = ExecutionState(
            id = executionId,
            label = button.label.ifBlank { "Command" },
            hostName = host.displayName,
            target = host.target,
            command = button.command
        )

        executionJob = viewModelScope.launch {
            try {
                val result = SshExecutor.execute(
                    context = getApplication(),
                    host = host,
                    command = button.command,
                    onOutput = { chunk -> appendOutput(executionId, chunk) }
                )
                _execution.update { current ->
                    if (current?.id != executionId) current
                    else current.copy(
                        running = false,
                        exitCode = result.exitCode,
                        timedOut = result.timedOut,
                        error = if (result.timedOut) {
                            "Timed out after ${host.commandTimeoutSeconds}s"
                        } else {
                            null
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _execution.update { current ->
                    if (current?.id != executionId) current
                    else current.copy(
                        running = false,
                        error = t.message?.takeIf { it.isNotBlank() }
                            ?: t.javaClass.simpleName
                    )
                }
            }
        }
    }

    fun cancelCurrentExecution() {
        val job = executionJob
        executionJob = null
        if (job?.isActive == true) {
            _execution.update { current ->
                current?.copy(running = false, cancelled = true)
            }
            job.cancel()
        }
    }

    fun dismissExecution() {
        cancelCurrentExecution()
        _execution.value = null
    }

    private fun appendOutput(executionId: String, chunk: String) {
        if (chunk.isEmpty()) return
        _execution.update { current ->
            if (current?.id != executionId) return@update current

            val cleaned = ANSI_REGEX.replace(chunk, "")
            var combined = current.output + cleaned
            if (combined.length > MAX_OUTPUT_CHARS) {
                combined = TRUNCATED_MARKER + combined.takeLast(MAX_OUTPUT_CHARS)
            }
            current.copy(output = combined)
        }
    }

    private companion object {
        const val MAX_OUTPUT_CHARS = 250_000
        const val TRUNCATED_MARKER = "… earlier output truncated …\n"
        val ANSI_REGEX = Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]")
    }
}
