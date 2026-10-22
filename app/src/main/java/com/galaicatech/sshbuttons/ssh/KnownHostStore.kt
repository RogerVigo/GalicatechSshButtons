package com.galaicatech.sshbuttons.ssh

import android.content.Context
import android.util.Base64
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.UserInfo
import com.galaicatech.sshbuttons.data.HostKeyPolicy

class TofuHostKeyRepository(
    context: Context,
    private val policy: HostKeyPolicy
) : HostKeyRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun check(host: String, key: ByteArray): Int {
        if (policy == HostKeyPolicy.ACCEPT_ALL) return HostKeyRepository.OK

        val stored = prefs.getString(host, null)
        val incoming = encode(key)
        return when {
            stored == null -> {
                prefs.edit().putString(host, incoming).apply()
                HostKeyRepository.OK
            }
            stored == incoming -> HostKeyRepository.OK
            else -> HostKeyRepository.CHANGED
        }
    }

    override fun add(hostkey: HostKey, userinfo: UserInfo?) {
        if (policy == HostKeyPolicy.ACCEPT_ALL) return
        prefs.edit().putString(hostkey.host, hostkey.key).apply()
    }

    override fun remove(host: String, type: String) {
        prefs.edit().remove(host).apply()
    }

    override fun remove(host: String, type: String, key: ByteArray) {
        prefs.edit().remove(host).apply()
    }

    override fun getKnownHostsRepositoryID(): String = PREFS_NAME

    override fun getHostKey(): Array<HostKey> {
        val result = mutableListOf<HostKey>()
        prefs.all.forEach { (host, value) ->
            val bytes = (value as? String)?.let(::decode) ?: return@forEach
            runCatching { result.add(HostKey(host, bytes)) }
        }
        return result.toTypedArray()
    }

    override fun getHostKey(host: String?, type: String?): Array<HostKey> {
        if (policy == HostKeyPolicy.ACCEPT_ALL) return emptyArray()
        val target = host ?: return emptyArray()
        val stored = prefs.getString(target, null) ?: return emptyArray()
        val bytes = decode(stored) ?: return emptyArray()
        return runCatching { arrayOf(HostKey(target, bytes)) }.getOrDefault(emptyArray())
    }

    private fun encode(key: ByteArray): String = Base64.encodeToString(key, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray? =
        runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()

    companion object {
        private const val PREFS_NAME = "ssh_buttons_known_hosts"

        fun forget(context: Context, host: String, port: Int) {
            val id = if (port == 22) host else "[$host]:$port"
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(id)
                .apply()
        }
    }
}
