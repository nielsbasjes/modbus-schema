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

import kotlin.math.max

class StringTable {
    private var headers: List<String> = ArrayList()
    private val lines: MutableList<List<String>> = ArrayList()

    override fun toString(): String {
        val sb = StringBuilder(512)
        val columnWidths: MutableList<Int> = ArrayList()

        for (column in headers.indices) {
            var maxWidth = headers[column].length
            for (line in lines) {
                if (line.isNotEmpty() && line.size > column) {
                    val columnValue = line[column]
                    maxWidth = max(maxWidth.toDouble(), columnValue.length.toDouble()).toInt()
                }
            }
            columnWidths.add(maxWidth)
        }
        writeSeparator(sb, columnWidths)
        writeLine(sb, columnWidths, headers)
        writeSeparator(sb, columnWidths)
        for (line in lines) {
            writeLine(sb, columnWidths, line)
        }
        writeSeparator(sb, columnWidths)
        return sb.toString()
    }

    private fun writeSeparator(sb: StringBuilder, columnWidths: List<Int>) {
        var first = true
        for (columnWidth in columnWidths) {
            if (first) {
                sb.append('|')
                first = false
            } else {
                sb.append('+')
            }
            sb.append("-".repeat(columnWidth + 2))
        }
        sb.append('|')
        sb.append('\n')
    }

    private fun writeLine(sb: StringBuilder, columnWidths: List<Int>, fields: List<String>) {
        if (fields.isEmpty()) {
            writeSeparator(sb, columnWidths)
            return
        }

        val columns = max(columnWidths.size.toDouble(), fields.size.toDouble()).toInt()

        for (columnNr in 0 until columns) {
            var columnWidth = 1
            if (columnNr < columnWidths.size) {
                columnWidth = columnWidths[columnNr]
            }
            if (columnNr <= columnWidths.size) {
                sb.append('|')
            }
            var field = ""
            if (columnNr < fields.size) {
                field = fields[columnNr]
            }
            sb.append(String.format(" %-" + columnWidth + "s ", field)) // NOSONAR java:S3457 This is creative, I know.
        }
        if (columns <= columnWidths.size) {
            sb.append('|')
        }
        sb.append('\n')
    }

    fun withHeaders(vararg fields: String): StringTable {
        this.headers = listOf(*fields)
        return this
    }

    fun withHeaders(fields: List<String>): StringTable {
        this.headers = ArrayList(fields)
        return this
    }

    fun addRow(vararg fields: String): StringTable {
        lines.add(listOf(*fields))
        return this
    }

    fun addRow(fields: List<String>): StringTable {
        lines.add(ArrayList(fields))
        return this
    }

    fun addRowSeparator(): StringTable {
        lines.add(ArrayList()) // Empty array
        return this
    }
}
