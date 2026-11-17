package com.slidebolt.client.feature.browser

import com.slidebolt.client.core.SlideBoltClient
import com.slidebolt.client.model.*
import com.slidebolt.client.platform.AndroidNsdDiscovery
import com.slidebolt.client.session.InstallationStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject

data class ClientState(
    val candidates: List<InstallationCandidate> = emptyList(),
    val installations: List<Installation> = emptyList(),
    val active: Installation? = null,
    val pairingCode: String? = null,
    val devices: List<Device> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val definitions: DefinitionCatalog? = null,
    val status: String = "Discover or enter a SlideBolt URL",
    val busy: Boolean = false,
)

class ClientController(
    private val api: SlideBoltClient,
    private val discovery: AndroidNsdDiscovery,
    private val store: InstallationStore,
    private val clientType: String,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(ClientState())
    val state: StateFlow<ClientState> = mutable.asStateFlow()

    init {
        scope.launch { store.installations.collect { mutable.update { state -> state.copy(installations = it) } } }
    }

    fun discover() {
        scope.launch {
            mutable.update { it.copy(status = "Searching the LAN…") }
            discovery.discover().catch { error -> mutable.update { it.copy(status = error.message ?: "Discovery failed") } }
                .collect { candidate ->
                    mutable.update { current ->
                        current.copy(candidates = (current.candidates.filterNot { it.apiUrl == candidate.apiUrl } + candidate), status = "Found ${candidate.name}")
                    }
                }
        }
    }

    fun connect(apiUrl: String, clientName: String) {
        scope.launch {
            mutable.update { it.copy(busy = true, status = "Checking $apiUrl") }
            runCatching {
                val known = api.wellKnown(apiUrl.trimEnd('/'))
                store.find(known.id)?.let { existing ->
                    select(existing)
                    return@runCatching
                }
                val pairing = api.requestPairing(known.apiUrl, clientName, clientType)
                mutable.update { it.copy(pairingCode = pairing.code, busy = false, status = "Enter ${pairing.code} on the SlideBolt Clients page") }
                while (currentCoroutineContext().isActive) {
                    delay(pairing.pollAfterSeconds.coerceAtLeast(1) * 1000L)
                    when (val result = api.exchange(known.apiUrl, known, pairing)) {
                        PairingExchange.Pending -> Unit
                        is PairingExchange.Approved -> {
                            store.save(result.installation)
                            mutable.update { it.copy(pairingCode = null, status = "Paired with ${known.name}") }
                            select(result.installation)
                            return@runCatching
                        }
                        PairingExchange.Rejected -> error("Pairing was rejected")
                        PairingExchange.Expired -> error("Pairing request expired")
                    }
                }
            }.onFailure { error -> mutable.update { it.copy(busy = false, pairingCode = null, status = error.message ?: "Connection failed") } }
        }
    }

    fun select(installation: Installation) {
        scope.launch {
            mutable.update { it.copy(active = installation, busy = true, status = "Loading ${installation.name}") }
            runCatching {
                val devices = api.devices(installation)
                val entities = api.entities(installation)
                val definitions = api.definitions(installation)
                mutable.update { it.copy(devices = devices, entities = entities, definitions = definitions, busy = false, status = "Connected") }
            }.onFailure { error -> mutable.update { it.copy(busy = false, status = error.message ?: "Load failed") } }
        }
    }

    fun command(entity: Entity, action: String) {
        val installation = state.value.active ?: return
        scope.launch {
            runCatching { api.command(installation, entity, action, buildJsonObject {}) }
                .onSuccess { select(installation) }
                .onFailure { error -> mutable.update { it.copy(status = error.message ?: "Command failed") } }
        }
    }
}

