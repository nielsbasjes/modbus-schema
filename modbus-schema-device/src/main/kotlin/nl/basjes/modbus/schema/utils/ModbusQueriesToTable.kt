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

import nl.basjes.modbus.schema.fetcher.HoleModbusQuery
import nl.basjes.modbus.schema.fetcher.MergedModbusQuery
import nl.basjes.modbus.schema.fetcher.ModbusQuery
import kotlin.time.DurationUnit

fun List<ModbusQuery>.toTable(): String {
    val table = StringTable()
    table.withHeaders("Class name", "Type", "Start Address", "Count", "Status", "Duration (ms)", "Block", "Fields")

    /**
     * In addition to the requested field ids also return the requested holes.
     */
    fun ModbusQuery.tableFields(): List<String> {
        if (this is HoleModbusQuery) {
            return listOf("<Hole ${start}#${count}>")
        }
        if (this is MergedModbusQuery) {
            return modbusQueries.map { it.tableFields() }.flatten()
        }
        return fields.map { it.id }
    }

    this.forEach { modbusQuery ->
        table.addRow(
            modbusQuery.javaClass.simpleName,
            modbusQuery.type.name,
            modbusQuery.start.toCleanFormat(),
            modbusQuery.count.toString(),
            modbusQuery.status.toString(),
            (modbusQuery.duration?.toLong(DurationUnit.MILLISECONDS)?: "NOT retrieved").toString() ,
            modbusQuery.fields.firstOrNull()?.block?.id ?: "<No Fields>",
            modbusQuery.tableFields().joinToString(", ")
        )
    }
    return table.toString()
}

fun String.println(prefix: String = "", postfix: String = "") {
    println(message = "$prefix$this$postfix")
}
