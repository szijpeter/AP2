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
package com.example.ap2sample.ap2.a2a

import com.example.ap2sample.platform.randomUuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/** Utility class for building A2A messages. Supports multiple parts. */
class A2aMessageBuilder {
    companion object {
        val SHOPPING_AGENT_ID = "trusted_shopping_agent"
    }

    val parts = mutableListOf<Part>()
    private var contextId: String? = null

    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
        encodeDefaults = true
    }

    /** Adds a text part to the message. */
    fun addText(text: String): A2aMessageBuilder {
        parts.add(TextPart(text))
        return this
    }

    /**
     * Adds a data part to the message.
     *
     * @param key The key to wrap the data in. If empty, the data is added directly.
     * @param data The data to be added, which will be serialized to a JsonElement.
     */
    inline fun <reified T> addData(key: String = "", data: T): A2aMessageBuilder {
        val jsonData = json.encodeToJsonElement(serializer(), data)

        val finalData =
                if (key.isNotBlank()) {
                    JsonObject(mapOf(key to jsonData))
                } else {
                    jsonData
                }
        parts.add(DataPart(finalData))
        return this
    }

    /** Sets the context id on the message. */
    fun setContextId(contextId: String): A2aMessageBuilder {
        this.contextId = contextId
        return this
    }

    /** Builds the final Message object. */
    fun build(): Message {
        this.addData("shopping_agent_id", SHOPPING_AGENT_ID)
        return Message(
                messageId = randomUuid().replace("-", ""),
                contextId = this.contextId,
                parts = parts,
                role = Role.AGENT,
        )
    }
}
