/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.modbus.schema.utils

import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException

private const val idExpression = "^[a-zA-Z]([a-zA-Z0-9_ ]*[a-zA-Z0-9_]+)?$"

fun isValidIdentifier(id: String) = id.matches(Regex(idExpression))

fun requireValidIdentifier(
    id: String,
    fieldName: String,
) {
    if (!isValidIdentifier(id)) {
        throw ModbusSchemaParseException("Illegal $fieldName: \"$id\"")
    }
}

@JvmInline
value class NameUsableAsClassName(
    val value: String,
) {
    init {
        require(value.matches(Regex(idExpression)))
    }
}
