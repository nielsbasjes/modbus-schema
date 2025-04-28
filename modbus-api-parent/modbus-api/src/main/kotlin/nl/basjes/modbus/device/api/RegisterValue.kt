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
package nl.basjes.modbus.device.api

import java.util.UUID

/**
 * Holds the single value of a single modbus register
 */
class RegisterValue(
    /**
     * The modbus register address.
     */
    val address: Address
) : Comparable<RegisterValue> {
    /**
     * An identifier to that can be used to ensure some registers are retrieved together.
     * By default, filled with a random unique value because we assume they are all independent.
     */
    var fetchGroup = UUID.randomUUID().toString()

    /**
     * Some registers will NEVER change and thus do not need to be retrieved a second time
     */
    var immutable = false

    /*
     * The last known value of the register (always 16 bits!).
     */
    var value: Short? = null
        private set

    /**
     * If a register has a comment this can be used when converting it to a String (in yaml for example)
     */
    var comment: String? = null

    /*
     * The timestamp (epoch in milliseconds) of the last known value of the register.
     */
    var timestamp: Long = Long.MIN_VALUE

    fun setValue(value: Short): RegisterValue {
        return setValue(value, System.currentTimeMillis())
    }

    fun setValue(value: Short, timestamp: Long): RegisterValue {
        this.value = value
        this.timestamp = timestamp
        return this
    }

    fun hasValue(): Boolean {
        return value != null
    }

    fun needsToBeUpdated(now: Long, maxAge: Long): Boolean {
        if (value == null) {
            return true
        }
        if (immutable) {
            return false
        }
        require(timestamp > -2208988800000) { "Any register with a valid value MUST be after 1900-01-01T00:00:00Z" }
        return now - timestamp > maxAge
    }

    fun clear() {
        this.value = null
        timestamp = Long.MIN_VALUE
    }

    val hexValue: String
        /**
         * @return The current register value as a 4 digit HEX string in uppercase. Or "----" in case of null.
         */
        get() {
            if (hasValue()) {
                return String.format("%04X", value)
            }
            return "----"
        }

    override fun toString(): String {
        var stringComment = ""
        if (!comment.isNullOrBlank()) {
            stringComment = " /* ${comment} */"
        }
        if (hasValue()) {
            return "{$address=0x$hexValue}${stringComment}"
        }
        return "{$address= ----}${stringComment}"
    }

    override fun compareTo(other: RegisterValue): Int {
        val addressCompared = address.compareTo(other.address)
        if (addressCompared != 0) {
            return addressCompared
        }
        return timestamp.compareTo(other.timestamp)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RegisterValue) {
            return false
        }
        return  immutable   == other.immutable &&
                value       == other.value &&
                timestamp   == other.timestamp &&
                address     == other.address &&
                fetchGroup  == other.fetchGroup
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + fetchGroup.hashCode()
        result = 31 * result + immutable.hashCode()
        return result
    }
}
