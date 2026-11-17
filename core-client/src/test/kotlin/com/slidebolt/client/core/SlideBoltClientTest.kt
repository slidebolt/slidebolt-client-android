package com.slidebolt.client.core

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlideBoltClientTest {
    @Test
    fun `loads well known contract`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"id":"system-1","name":"Test","apiVersion":"1","apiUrl":"${server.url("/").toString().trimEnd('/')}","openapiUrl":"x","pairing":true}""").setHeader("Content-Type", "application/json"))
            val known = SlideBoltClient().wellKnown(server.url("/").toString())
            assertEquals("system-1", known.id)
            assertEquals(true, known.pairing)
        }
    }
}

