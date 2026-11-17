package com.slidebolt.client.feature.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideBoltRoot(controller: ClientController, television: Boolean = false) {
    val state by controller.state.collectAsState()
    var url by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf(if (television) "SlideBolt TV" else "SlideBolt Phone") }
    var section by remember { mutableStateOf("devices") }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text(state.active?.name ?: "SlideBolt") }) },
        ) { padding ->
            Column(Modifier.padding(padding).padding(if (television) 32.dp else 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.active == null) {
                    Text("Connect", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(url, { url = it }, label = { Text("SlideBolt API URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(clientName, { clientName = it }, label = { Text("Client name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = controller::discover) { Text("Discover") }
                        Button(onClick = { if (url.isNotBlank()) controller.connect(url, clientName) }, enabled = !state.busy) { Text("Connect") }
                    }
                    state.pairingCode?.let { code -> Text(code, style = MaterialTheme.typography.displayMedium) }
                    state.candidates.forEach { candidate ->
                        ListItem(
                            headlineContent = { Text(candidate.name) },
                            supportingContent = { Text(candidate.apiUrl) },
                            modifier = Modifier.clickable { controller.connect(candidate.apiUrl, clientName) },
                        )
                    }
                    if (state.installations.isNotEmpty()) {
                        Text("Paired installations", style = MaterialTheme.typography.titleMedium)
                        state.installations.forEach { installation ->
                            ListItem(headlineContent = { Text(installation.name) }, supportingContent = { Text(installation.apiUrl) }, modifier = Modifier.clickable { controller.select(installation) })
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(section == "devices", { section = "devices" }, label = { Text("Devices") })
                        FilterChip(section == "entities", { section = "entities" }, label = { Text("Entities") })
                        FilterChip(section == "diagnostics", { section = "diagnostics" }, label = { Text("Diagnostics") })
                    }
                    when (section) {
                        "devices" -> LazyColumn { items(state.devices, key = { it.plugin + ":" + it.id }) { device ->
                            ListItem(headlineContent = { Text(device.name) }, supportingContent = { Text(listOfNotNull(device.deviceClass, device.plugin).joinToString(" · ")) })
                            HorizontalDivider()
                        }}
                        "entities" -> LazyColumn { items(state.entities, key = { it.key }) { entity ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(entity.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${entity.type} · ${entity.key}")
                                    Text(entity.state.toString(), style = MaterialTheme.typography.bodySmall)
                                    entity.commands.chunked(3).forEach { commands -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        commands.forEach { action -> OutlinedButton(onClick = { controller.command(entity, action) }) { Text(action) } }
                                    }}
                                }
                            }
                        }}
                        else -> LazyColumn {
                            item { Text("Access: ${state.active?.access}\nGroups: ${state.active?.securityGroups?.joinToString()}\nDevices: ${state.devices.size}\nEntities: ${state.entities.size}") }
                            item { Text(state.definitions?.raw?.toString().orEmpty(), style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                Text(state.status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

