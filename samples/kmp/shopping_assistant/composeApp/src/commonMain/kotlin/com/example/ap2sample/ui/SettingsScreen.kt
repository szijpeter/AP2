/**
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     https://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.ap2sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val MERCHANT_AGENT_URL = "http://localhost:8001/a2a/merchant_agent"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        agentCardUrl: String,
        useAndroidCredentialManager: Boolean,
        useMockedCredentials: Boolean,
        onDoneClicked: (String, Boolean, Boolean) -> Unit
) {
    val agentOptions = listOf("Generic Merchant Agent" to MERCHANT_AGENT_URL, "Custom" to "")

    var editedUrl by remember { mutableStateOf(agentCardUrl) }
    var editedUseAndroidCredentialManager by remember {
        mutableStateOf(useAndroidCredentialManager)
    }
    var editedUseMockedCredentials by remember { mutableStateOf(useMockedCredentials) }

    var selectedOption by remember {
        mutableStateOf(
                agentOptions.find { it.second == agentCardUrl && it.first != "Custom" }?.first
                        ?: "Custom"
        )
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Settings") },
                        actions = {
                            IconButton(
                                    onClick = {
                                        onDoneClicked(
                                                editedUrl,
                                                editedUseAndroidCredentialManager,
                                                editedUseMockedCredentials
                                        )
                                    }
                            ) {
                                Icon(imageVector = Icons.Default.Done, contentDescription = "Done")
                            }
                        },
                )
            }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            agentOptions.forEach { (name, url) ->
                Row(
                        Modifier.fillMaxWidth()
                                .selectable(
                                        selected = (selectedOption == name),
                                        onClick = {
                                            selectedOption = name
                                            if (name != "Custom") {
                                                editedUrl = url
                                            }
                                        },
                                )
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                            selected = (selectedOption == name),
                            onClick = null,
                    )
                    Text(text = name, modifier = Modifier.padding(start = 16.dp))
                }
            }

            OutlinedTextField(
                    value = editedUrl,
                    onValueChange = { newUrl ->
                        editedUrl = newUrl
                        selectedOption =
                                agentOptions
                                        .find { it.second == newUrl && it.first != "Custom" }
                                        ?.first
                                        ?: "Custom"
                    },
                    label = { Text("Agent Card URL") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = true,
                    trailingIcon = {
                        if (editedUrl.isNotEmpty()) {
                            IconButton(
                                    onClick = {
                                        editedUrl = ""
                                        selectedOption = "Custom"
                                    }
                            ) {
                                Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear URL"
                                )
                            }
                        }
                    },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                    "Debug Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Android Native Auth Manager")
                    Text(
                            "If disabled, uses shared KMP flow on Android",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                        checked = editedUseAndroidCredentialManager,
                        onCheckedChange = { editedUseAndroidCredentialManager = it }
                )
            }

            Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use Mock Credentials")
                    Text(
                            "Seeds and selects demo card for KMP flow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                        checked = editedUseMockedCredentials,
                        onCheckedChange = { editedUseMockedCredentials = it }
                )
            }
        }
    }
}
