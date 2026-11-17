package com.slidebolt.client.session

import com.slidebolt.client.model.Installation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstallationStoreTest {
    @Test
    fun `saving same system replaces connection`() = runTest {
        val store = InMemoryInstallationStore()
        store.save(Installation("one", "First", "https://one", "old", "read", listOf("*")))
        store.save(Installation("one", "First", "https://one", "new", "control", listOf("*")))
        assertEquals("new", store.find("one")?.token)
    }
}

