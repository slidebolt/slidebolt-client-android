package com.slidebolt.client.core

import com.slidebolt.client.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SlideBoltHttpException(val status: Int, message: String) : IOException(message)

class SlideBoltClient(
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun wellKnown(apiUrl: String): WellKnown {
        val body = get(apiUrl, "/.well-known/slidebolt")
        return WellKnown(
            id = body.string("id"),
            name = body.string("name"),
            apiVersion = body.string("apiVersion"),
            apiUrl = body.string("apiUrl"),
            openapiUrl = body.string("openapiUrl"),
            pairing = body.boolean("pairing"),
        )
    }

    suspend fun requestPairing(
        apiUrl: String,
        clientName: String,
        clientType: String,
        access: String = "control",
        securityGroups: List<String> = listOf("*"),
    ): PairingRequest {
        val body = post(apiUrl, "/pairing/requests", null, buildJsonObject {
            put("clientName", clientName)
            put("clientType", clientType)
            put("requestedAccess", access)
            put("requestedSecurityGroups", JsonArray(securityGroups.map(::JsonPrimitive)))
        })
        return PairingRequest(body.string("id"), body.string("code"), body.string("verifier"), body.string("expiresAt"), body.int("pollAfterSeconds"))
    }

    suspend fun exchange(apiUrl: String, system: WellKnown, request: PairingRequest): PairingExchange = try {
        val body = post(apiUrl, "/pairing/requests/${request.id}/exchange", null, buildJsonObject { put("verifier", request.verifier) })
        when (body.string("status")) {
            "pending" -> PairingExchange.Pending
            "approved" -> PairingExchange.Approved(
                Installation(
                    id = system.id,
                    name = system.name,
                    apiUrl = body.optionalString("apiUrl") ?: system.apiUrl,
                    token = body.string("token"),
                    access = body.string("access"),
                    securityGroups = body.array("securityGroups").map { it.jsonPrimitive.content },
                )
            )
            "rejected" -> PairingExchange.Rejected
            else -> PairingExchange.Pending
        }
    } catch (error: SlideBoltHttpException) {
        when (error.status) {
            403 -> PairingExchange.Rejected
            410 -> PairingExchange.Expired
            else -> throw error
        }
    }

    suspend fun devices(installation: Installation): List<Device> =
        list(installation, "/devices").mapNotNull { element -> element.jsonObject.toDeviceOrNull() }

    suspend fun entities(installation: Installation): List<Entity> =
        list(installation, "/entities").mapNotNull { element -> element.jsonObject.toEntityOrNull() }

    suspend fun definitions(installation: Installation): DefinitionCatalog =
        DefinitionCatalog(get(installation.apiUrl, "/definitions", installation.token))

    suspend fun command(installation: Installation, entity: Entity, action: String, payload: JsonObject = buildJsonObject {}): JsonObject =
        post(installation.apiUrl, "/entities/${entity.plugin}/${entity.deviceId}/${entity.id}/command/$action", installation.token, payload)

    fun events(installation: Installation): Flow<SlideBoltEvent> = callbackFlow {
        val wsUrl = installation.apiUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/ws/events?pattern=%3E"
        val request = Request.Builder().url(wsUrl).header("Authorization", "Bearer ${installation.token}").build()
        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val raw = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return
                val obj = raw.jsonObject
                val type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val payload = obj["payload"] ?: raw
                trySend(when (type) {
                    "snapshot" -> SlideBoltEvent.Snapshot(payload)
                    "upsert" -> SlideBoltEvent.Upsert(payload)
                    "remove" -> SlideBoltEvent.Remove(payload)
                    else -> SlideBoltEvent.Unknown(type, payload)
                })
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { close(t) }
        }
        val socket = http.newWebSocket(request, listener)
        awaitClose { socket.close(1000, "client closed") }
    }

    private suspend fun list(installation: Installation, path: String): JsonArray {
        val element = request(installation.apiUrl, path, "GET", installation.token, null)
        return when (element) {
            is JsonArray -> element
            is JsonObject -> element["items"]?.jsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
    }

    private suspend fun get(apiUrl: String, path: String, token: String? = null): JsonObject =
        request(apiUrl, path, "GET", token, null).jsonObject

    private suspend fun post(apiUrl: String, path: String, token: String?, body: JsonObject): JsonObject =
        request(apiUrl, path, "POST", token, body).jsonObject

    private suspend fun request(apiUrl: String, path: String, method: String, token: String?, body: JsonObject?): JsonElement {
        val builder = Request.Builder().url(apiUrl.trimEnd('/') + path).header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (method == "POST") builder.post((body ?: buildJsonObject {}).toString().toRequestBody("application/json".toMediaType()))
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(builder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val text = it.body.string()
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(SlideBoltHttpException(it.code, text))
                        } else {
                            continuation.resume(if (text.isBlank()) buildJsonObject {} else json.parseToJsonElement(text))
                        }
                    }
                }
            })
        }
    }
}

private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.content ?: error("missing $key")
private fun JsonObject.optionalString(key: String) = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.boolean(key: String) = this[key]?.jsonPrimitive?.boolean ?: false
private fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.int ?: 0
private fun JsonObject.array(key: String) = this[key]?.jsonArray ?: JsonArray(emptyList())

private fun JsonObject.toDeviceOrNull(): Device? {
    val id = optionalString("id") ?: optionalString("deviceID") ?: return null
    return Device(id, optionalString("plugin").orEmpty(), optionalString("name") ?: id, optionalString("class") ?: optionalString("deviceClass"), this)
}

private fun JsonObject.toEntityOrNull(): Entity? {
    val id = optionalString("id") ?: return null
    val plugin = optionalString("plugin") ?: return null
    val device = optionalString("deviceID") ?: return null
    val commands = array("commands").mapNotNull { it.jsonPrimitive.contentOrNull }
    return Entity(id, plugin, device, optionalString("type").orEmpty(), optionalString("name") ?: id, this["state"]?.jsonObject ?: buildJsonObject {}, commands, this)
}

