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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialConfirmationSheet(request: ConfirmationRequest, onDismissRequest: () -> Unit) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
                onDismissRequest = {
                        request.onResult(false)
                        onDismissRequest()
                },
                sheetState = sheetState
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                text = "Confirm transaction",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Credential Card Display
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Future enhancement: could load cardArtUrl here if an image loader
                                // library is
                                // added.
                                // For now, using a fallback vector icon.
                                Box(
                                        modifier =
                                                Modifier.size(48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = "Card Icon",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                        Text(
                                                text = request.credentialInfo.cardName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                text =
                                                        "•••• ${request.credentialInfo.cardNumberLast4}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Merchant and Amount
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(
                                        text = "Requesting App",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = request.merchantName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(text = "Amount", style = MaterialTheme.typography.titleMedium)
                                Text(
                                        text = request.amount,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Action Buttons
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                OutlinedButton(
                                        onClick = {
                                                request.onResult(false)
                                                onDismissRequest()
                                        },
                                        modifier = Modifier.weight(1f)
                                ) { Text("Cancel") }

                                Button(
                                        onClick = {
                                                request.onResult(true)
                                                onDismissRequest()
                                        },
                                        modifier = Modifier.weight(1f)
                                ) { Text("Continue") }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                }
        }
}
