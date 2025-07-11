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

package nl.basjes.modbus.schema.fetcher

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.schema.Field
import java.util.Objects
import kotlin.time.Duration

open class ModbusQuery(
    val start: Address,
    var count: Int,
) : Comparable<ModbusQuery> {
    /**
     * The number of milliseconds the actual fetch took.
     * NULL if not fetched yet.
     */
    var duration: Duration? = null
    var status: Status = Status.NOT_FETCHED

    enum class Status {
        NOT_FETCHED,
        ERROR,
        SUCCESS
    }

    /** The affected list of fields */
    val fields: MutableList<Field> = mutableListOf()

    val type = start.addressClass.type

    override fun compareTo(other: ModbusQuery): Int {
        val addressCompare = start.compareTo(other.start)
        if (addressCompare != 0) {
            return addressCompare
        }
        return count.compareTo(other.count)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ModbusQuery) {
            return false
        }
        return count == other.count && start == other.start && status == other.status && fields == other.fields
    }

    override fun hashCode(): Int = Objects.hash(start, count, status, fields)

    override fun toString(): String =
        "FetchBatch { $start # $count } (Fields: ${fields.joinToString(", ") { it.block.id + "[" + it.id + "]" }})"
}

/**
 * When doing fetch optimization we are sometimes combining the FetchBatches.
 * This is the class to hold such a combination.
 * This is needed to be able to handle the retry in case of a read error
 */
class MergedModbusQuery(
    start: Address,
    count: Int,
) : ModbusQuery(start, count) {
    val modbusQueries: MutableList<ModbusQuery> = ArrayList()

    fun add(modbusQuery: ModbusQuery) {
        modbusQueries.add(modbusQuery)
        fields.addAll(modbusQuery.fields)
    }
}
