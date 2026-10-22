package com.galaicatech.sshbuttons.data

import android.content.Context
import com.galaicatech.sshbuttons.security.SecretCipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cipher = SecretCipher()

    private val _hosts = MutableStateFlow(readHosts())
    private val _buttons = MutableStateFlow(readButtons())

    val hosts: StateFlow<List<HostConfig>> = _hosts.asStateFlow()
    val buttons: StateFlow<List<CommandButton>> = _buttons.asStateFlow()

    @Synchronized
    fun saveHost(host: HostConfig) {
        val clean = host.sanitized()
        val updated = _hosts.value.toMutableList()
        val index = updated.indexOfFirst { it.id == clean.id }
        if (index >= 0) updated[index] = clean else updated.add(clean)
        persistHosts(updated)
        _hosts.value = updated
    }

    @Synchronized
    fun deleteHost(hostId: String) {
        val updatedHosts = _hosts.value.filterNot { it.id == hostId }
        val updatedButtons = _buttons.value.filterNot { it.hostId == hostId }
        persistHosts(updatedHosts)
        persistButtons(updatedButtons)
        _hosts.value = updatedHosts
        _buttons.value = updatedButtons
    }

    @Synchronized
    fun saveButton(button: CommandButton) {
        val clean = button.sanitized()
        val updated = _buttons.value.toMutableList()
        val index = updated.indexOfFirst { it.id == clean.id }
        if (index >= 0) updated[index] = clean else updated.add(clean)
        persistButtons(updated)
        _buttons.value = updated
    }

    @Synchronized
    fun deleteButton(buttonId: String) {
        val updated = _buttons.value.filterNot { it.id == buttonId }
        persistButtons(updated)
        _buttons.value = updated
    }

    private fun readHosts(): List<HostConfig> = runCatching {
        val raw = prefs.getString(KEY_HOSTS, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    HostConfig(
                        id = json.optString("id"),
                        name = json.optString("name"),
                        host = json.optString("host"),
                        port = json.intOr("port", 22),
                        username = json.optString("username"),
                        authType = AuthType.fromName(json.optString("authType")),
                        password = cipher.decrypt(json.optString("password")),
                        privateKey = cipher.decrypt(json.optString("privateKey")),
                        passphrase = cipher.decrypt(json.optString("passphrase")),
                        hostKeyPolicy = HostKeyPolicy.fromName(json.optString("hostKeyPolicy")),
                        keepAliveSeconds = json.intOr("keepAliveSeconds", 30),
                        connectTimeoutSeconds = json.intOr("connectTimeoutSeconds", 15),
                        commandTimeoutSeconds = json.intOr("commandTimeoutSeconds", 30)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun readButtons(): List<CommandButton> = runCatching {
        val raw = prefs.getString(KEY_BUTTONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    CommandButton(
                        id = json.optString("id"),
                        hostId = json.optString("hostId"),
                        label = json.optString("label"),
                        command = json.optString("command"),
                        requireConfirmation = json.optBoolean("requireConfirmation", false)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun persistHosts(hosts: List<HostConfig>) {
        val array = JSONArray()
        hosts.forEach { host ->
            array.put(
                JSONObject()
                    .put("id", host.id)
                    .put("name", host.name)
                    .put("host", host.host)
                    .put("port", host.port)
                    .put("username", host.username)
                    .put("authType", host.authType.name)
                    .put("password", cipher.encrypt(host.password))
                    .put("privateKey", cipher.encrypt(host.privateKey))
                    .put("passphrase", cipher.encrypt(host.passphrase))
                    .put("hostKeyPolicy", host.hostKeyPolicy.name)
                    .put("keepAliveSeconds", host.keepAliveSeconds)
                    .put("connectTimeoutSeconds", host.connectTimeoutSeconds)
                    .put("commandTimeoutSeconds", host.commandTimeoutSeconds)
            )
        }
        prefs.edit().putString(KEY_HOSTS, array.toString()).apply()
    }

    private fun persistButtons(buttons: List<CommandButton>) {
        val array = JSONArray()
        buttons.forEach { button ->
            array.put(
                JSONObject()
                    .put("id", button.id)
                    .put("hostId", button.hostId)
                    .put("label", button.label)
                    .put("command", button.command)
                    .put("requireConfirmation", button.requireConfirmation)
            )
        }
        prefs.edit().putString(KEY_BUTTONS, array.toString()).apply()
    }

    private fun JSONObject.intOr(key: String, fallback: Int): Int =
        if (has(key)) optInt(key, fallback) else fallback

    private companion object {
        const val PREFS_NAME = "ssh_buttons_data"
        const val KEY_HOSTS = "hosts"
        const val KEY_BUTTONS = "buttons"
    }
}
