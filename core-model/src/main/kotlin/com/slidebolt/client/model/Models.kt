package com.slidebolt.client.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

const val SLIDEBOLT_API_SERVICE = "_slidebolt-api._tcp."

enum class DiscoverySource { NSD, MANUAL }

data class InstallationCandidate(
    val name: String,
    val apiUrl: String,
    val source: DiscoverySource,
)

data class Installation(
    val id: String,
    val name: String,
    val apiUrl: String,
    val token: String,
    val access: String,
    val securityGroups: List<String>,
)

data class WellKnown(
    val id: String,
    val name: String,
    val apiVersion: String,
    val apiUrl: String,
    val openapiUrl: String,
    val pairing: Boolean,
)

data class PairingRequest(
    val id: String,
    val code: String,
    val verifier: String,
    val expiresAt: String,
    val pollAfterSeconds: Int,
)

sealed interface PairingExchange {
    data object Pending : PairingExchange
    data class Approved(val installation: Installation) : PairingExchange
    data object Rejected : PairingExchange
    data object Expired : PairingExchange
}

data class Device(
    val id: String,
    val plugin: String,
    val name: String,
    val deviceClass: String?,
    val raw: JsonObject,
)

data class Entity(
    val id: String,
    val plugin: String,
    val deviceId: String,
    val type: String,
    val name: String,
    val state: JsonObject,
    val commands: List<String>,
    val raw: JsonObject,
) {
    val key: String get() = "$plugin.$deviceId.$id"
}

data class DefinitionCatalog(val raw: JsonObject)

sealed interface SlideBoltEvent {
    data class Snapshot(val payload: JsonElement) : SlideBoltEvent
    data class Upsert(val payload: JsonElement) : SlideBoltEvent
    data class Remove(val payload: JsonElement) : SlideBoltEvent
    data class Unknown(val type: String, val payload: JsonElement) : SlideBoltEvent
}

