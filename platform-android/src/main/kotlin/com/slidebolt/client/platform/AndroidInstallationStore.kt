package com.slidebolt.client.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.slidebolt.client.model.Installation
import com.slidebolt.client.session.InstallationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidInstallationStore(context: Context) : InstallationStore {
    private val preferences = context.getSharedPreferences("slidebolt-installations", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    override val installations: Flow<List<Installation>> = state

    override suspend fun save(installation: Installation) {
        val next = state.value.filterNot { it.id == installation.id } + installation
        persist(next)
    }

    override suspend fun remove(systemId: String) { persist(state.value.filterNot { it.id == systemId }) }
    override suspend fun find(systemId: String): Installation? = state.value.firstOrNull { it.id == systemId }

    private fun persist(items: List<Installation>) {
        val raw = JsonArray(items.map { item -> buildJsonObject {
            put("id", item.id); put("name", item.name); put("apiUrl", item.apiUrl); put("token", item.token); put("access", item.access)
            put("securityGroups", JsonArray(item.securityGroups.map(::JsonPrimitive)))
        }}).toString().toByteArray()
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(raw), Base64.NO_WRAP)
        preferences.edit().putString(DATA_KEY, encoded).apply()
        state.value = items
    }

    private fun load(): List<Installation> = runCatching {
        val encoded = preferences.getString(DATA_KEY, null) ?: return emptyList()
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        Json.parseToJsonElement(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString()).jsonArray.map { value ->
            val item = value.jsonObject
            Installation(
                item.getValue("id").jsonPrimitive.content,
                item.getValue("name").jsonPrimitive.content,
                item.getValue("apiUrl").jsonPrimitive.content,
                item.getValue("token").jsonPrimitive.content,
                item.getValue("access").jsonPrimitive.content,
                item.getValue("securityGroups").jsonArray.map { it.jsonPrimitive.content },
            )
        }
    }.getOrElse { emptyList() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "slidebolt-client-installations-v1"
        const val DATA_KEY = "encrypted-installations"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

