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
package nl.basjes.modbus.schema

import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.utils.DoubleToString
import nl.basjes.modbus.schema.utils.StringTable

fun SchemaDevice.toTable(onlyUseFullFields: Boolean = false): String {
    val table = StringTable()
    table.withHeaders(
        "Block Id",
        "Field Id",
        "System",
        "Description",
        "Value",
        "Unit",
        "Expression",
        "Fetch Group",
        "Raw Bytes",
    )
    toTable(table, onlyUseFullFields)
    return table.toString()
}

internal fun SchemaDevice.toTable(
    table: StringTable,
    onlyUseFullFields: Boolean,
) {
    var first = true
    for (block in blocks) {
        if (!first) {
            table.addRowSeparator()
        }
        first = false
        block.toTable(table, onlyUseFullFields)
    }
}

// ------------------------------------------

fun Block.toTable(
    table: StringTable,
    onlyUseFullFields: Boolean,
) {
    table.addRow(
        id,
        "",
        "",
        description ?: "",
    )
    for (field in fields) {
        field.toTable(table, onlyUseFullFields)
    }
}

// ------------------------------------------

fun Field.toTable(
    table: StringTable,
    onlyUseFullFields: Boolean,
) {
    if (onlyUseFullFields && isSystem) {
        return
    }

    var value: Any?
    try {
//            System.out.println("Getting " + this);
        value = this.value
    } catch (_: ModbusException) {
        if (onlyUseFullFields) {
            return
        }
        value = "<<ERROR>>"
    }
    if (value == null) {
        if (onlyUseFullFields) {
            return
        }
        value = "<<Not Implemented>>"
    } else {
        if (value is Double) {
            value = DoubleToString.of((value as Double?)!!, 5)
        } else if (value is String) {
            if (onlyUseFullFields && value.isEmpty()) {
                return
            }
            value = "" + '"' + value + '"'
        }
        if (value is List<*>) {
            value = '['.toString() + value.joinToString(",") { it.toString() } + ']'
        }
    }

    var expressionString: String
    if (parsedExpression == null) {
        if (onlyUseFullFields) {
            return
        }
        expressionString = "-------"
    } else {
        expressionString = parsedExpression.toString()
        if (expressionString.length > 50) {
            expressionString = expressionString.substring(0, 45) + " ..."
        }
    }

    var bytes = ""
    if (parsedExpression != null) {
        bytes =
            parsedExpression!!
                .getRegisterValues(block.schemaDevice)
                .joinToString(" ") { it.hexValue }
    }
    var truncatedDescription = description
    if (truncatedDescription.length > 75) {
        truncatedDescription = truncatedDescription.substring(0, 70) + " ..."
    }
    table.addRow(
        block.id,
        id,
        if (isSystem) "*" else "",
        truncatedDescription,
        value.toString(),
        unit,
        expressionString,
        fetchGroup,
        bytes,
    )
}
