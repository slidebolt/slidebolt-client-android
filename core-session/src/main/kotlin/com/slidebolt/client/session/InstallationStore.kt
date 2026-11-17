package com.slidebolt.client.session

import com.slidebolt.client.model.Installation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface InstallationStore {
    val installations: Flow<List<Installation>>
    suspend fun save(installation: Installation)
    suspend fun remove(systemId: String)
    suspend fun find(systemId: String): Installation?
}

class InMemoryInstallationStore(initial: List<Installation> = emptyList()) : InstallationStore {
    private val state = MutableStateFlow(initial)
    override val installations: Flow<List<Installation>> = state
    override suspend fun save(installation: Installation) {
        state.value = state.value.filterNot { it.id == installation.id } + installation
    }
    override suspend fun remove(systemId: String) { state.value = state.value.filterNot { it.id == systemId } }
    override suspend fun find(systemId: String): Installation? = state.value.firstOrNull { it.id == systemId }
}

