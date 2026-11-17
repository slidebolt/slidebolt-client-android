package com.slidebolt.client.acceptance

import com.slidebolt.client.core.SlideBoltClient
import com.slidebolt.client.model.*
import io.cucumber.java.After
import io.cucumber.java.en.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionSteps {
    private lateinit var server: MockWebServer
    private val client = SlideBoltClient()
    private lateinit var known: WellKnown
    private lateinit var request: PairingRequest
    private var exchange: PairingExchange? = null
    private var approve = false
    private var installation: Installation? = null
    private var entities: List<Entity> = emptyList()

    @Given("a SlideBolt installation named {string}")
    fun installation(name: String) {
        server = MockWebServer()
        server.dispatcher = dispatcher(name)
        server.start()
    }

    @Given("the administrator will approve pairing")
    fun approvePairing() { approve = true }

    @Given("an approved client for a SlideBolt installation")
    fun approvedClient() {
        installation("Test Home")
        installation = Installation("system-test", "Test Home", baseUrl(), "token", "control", listOf("*"))
    }

    @When("the client loads its well-known contract")
    fun loadKnown() = runBlocking { known = client.wellKnown(baseUrl()) }

    @When("the client requests and exchanges a pairing code")
    fun pair() = runBlocking {
        known = client.wellKnown(baseUrl())
        request = client.requestPairing(baseUrl(), "Acceptance", "test")
        exchange = client.exchange(baseUrl(), known, request)
    }

    @When("the client requests a pairing code without approval")
    fun pending() { approve = false; pair() }

    @When("the client loads devices and entities")
    fun inventory() = runBlocking {
        client.devices(installation!!)
        entities = client.entities(installation!!)
    }

    @Then("the stable system id is {string}")
    fun stableId(id: String) { assertEquals(id, known.id) }
    @Then("pairing is advertised")
    fun pairingAdvertised() { assertTrue(known.pairing) }
    @Then("the paired access is {string}")
    fun pairedAccess(access: String) { assertEquals(access, (exchange as PairingExchange.Approved).installation.access) }
    @Then("the paired security groups include {string}")
    fun pairedGroup(group: String) { assertTrue(group in (exchange as PairingExchange.Approved).installation.securityGroups) }
    @Then("the exchange status is pending")
    fun exchangePending() { assertTrue(exchange is PairingExchange.Pending) }
    @Then("it receives a {string} entity with command {string}")
    fun receivesEntity(type: String, command: String) { assertTrue(entities.any { it.type == type && command in it.commands }) }
    @Then("it does not need plugin-specific decoding")
    fun genericDecoding() { assertEquals("light", entities.single().type) }

    @After
    fun stop() { if (::server.isInitialized) server.shutdown() }

    private fun baseUrl() = server.url("/").toString().trimEnd('/')

    private fun dispatcher(name: String) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val body = when {
                request.path == "/.well-known/slidebolt" -> """{"id":"system-test","name":"$name","apiVersion":"1","apiUrl":"${baseUrl()}","openapiUrl":"${baseUrl()}/openapi.json","pairing":true}"""
                request.path == "/pairing/requests" -> """{"id":"request-1","code":"ABCD-EFGH","verifier":"verify","expiresAt":"2099-01-01T00:00:00Z","pollAfterSeconds":1}"""
                request.path == "/pairing/requests/request-1/exchange" && approve -> """{"status":"approved","token":"token","access":"control","securityGroups":["*"],"apiUrl":"${baseUrl()}"}"""
                request.path == "/pairing/requests/request-1/exchange" -> """{"status":"pending"}"""
                request.path == "/devices" -> """[{"id":"lamp","plugin":"plugin-any","name":"Lamp","class":"light"}]"""
                request.path == "/entities" -> """[{"id":"power","plugin":"plugin-any","deviceID":"lamp","type":"light","name":"Lamp","state":{"power":false},"commands":["light_turn_on"]}]"""
                else -> return MockResponse().setResponseCode(404)
            }
            return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
        }
    }
}
