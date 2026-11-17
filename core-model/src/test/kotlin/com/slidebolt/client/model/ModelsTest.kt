package com.slidebolt.client.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlinx.serialization.json.buildJsonObject

class ModelsTest {
    @Test
    fun `entity key is provider neutral address`() {
        val entity = Entity("power", "plugin-example", "lamp", "light", "Lamp", buildJsonObject {}, emptyList(), buildJsonObject {})
        assertEquals("plugin-example.lamp.power", entity.key)
    }

    @Test
    fun `core discovery has its own service type`() {
        assertEquals("_slidebolt-api._tcp.", SLIDEBOLT_API_SERVICE)
    }
}

