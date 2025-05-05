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
    var fetchGroup = "FG_"+ address.toCleanFormat()

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
    var fetchTimestamp: Long = Long.MIN_VALUE

    val timestamp: Long?
        get() = if (fetchTimestamp <= NEVER_VALID_BEFORE || immutable) null else fetchTimestamp

    fun clone(): RegisterValue {
        val registerValue = RegisterValue(address)
        registerValue.fetchGroup      = fetchGroup
        registerValue.immutable       = immutable
        registerValue.value           = value
        registerValue.comment         = comment
        registerValue.fetchTimestamp  = fetchTimestamp
        registerValue.hardReadError   = hardReadError
        return registerValue
    }

    fun setValue(value: Short): RegisterValue {
        return setValue(value, System.currentTimeMillis())
    }

    fun setValue(registerValue: RegisterValue): RegisterValue {
        this.value           = registerValue.value
        this.fetchTimestamp  = registerValue.fetchTimestamp
        this.hardReadError   = registerValue.hardReadError
        return this
    }

    fun setValue(value: Short, timestamp: Long): RegisterValue {
        this.value = value
        this.fetchTimestamp = timestamp
        this.hardReadError = false
        return this
    }

    fun hasValue(): Boolean {
        return value != null
    }

    /** 1900-01-01T00:00:00.000Z */
    val EPOCH_1900 = -2208988800000
    /** 1888-08-08T08:08:08.888Z */
    val EPOCH_1888 = -2568642711112

    val NEVER_VALID_BEFORE  = EPOCH_1900
    val READERROR_TIMESTAMP = EPOCH_1888

    // If a read error is NOT hard it can be reset
    // If a read error IS hard it cannot be reset
    var hardReadError = false

    fun setSoftReadError() {
        this.value = null
        this.fetchTimestamp = READERROR_TIMESTAMP
        this.hardReadError = false
    }

    fun setHardReadError() {
        this.value = null
        this.fetchTimestamp = READERROR_TIMESTAMP
        this.hardReadError = true
    }

    fun isReadError(): Boolean {
        return value == null && fetchTimestamp == READERROR_TIMESTAMP
    }

    fun clearSoftReadError() {
        if (!hardReadError) {
            value = null
            fetchTimestamp = Long.MIN_VALUE
        }
    }

    fun needsToBeUpdated(now: Long, maxAge: Long): Boolean {
        if (isReadError()) {
            return false
        }
        if (value == null) {
            return true
        }
        if (immutable) {
            return false
        }
        // Any register with a valid value MUST be after 1900-01-01T00:00:00Z
        if (fetchTimestamp > NEVER_VALID_BEFORE) {
            return true
        }
        return now - fetchTimestamp > maxAge
    }

    fun clear() {
        value = null
        fetchTimestamp = Long.MIN_VALUE
    }

    val hexValue: String
        /**
         * @return The current register value as a 4 digit HEX string in uppercase. Or "----" in case of null.
         */
        get() {
            if (hasValue()) {
                return String.format("%04X", value)
            }
            if (isReadError()) {
                if (hardReadError) {
                    return "XXXX"
                }
                return "xxxx"
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
        return "{$address= $hexValue${stringComment}"
    }

    override fun compareTo(other: RegisterValue): Int {
        val addressCompared = address.compareTo(other.address)
        if (addressCompared != 0) {
            return addressCompared
        }
        return fetchTimestamp.compareTo(other.fetchTimestamp)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RegisterValue) {
            return false
        }
        return  immutable       == other.immutable &&
                value           == other.value &&
                fetchTimestamp  == other.fetchTimestamp &&
                address         == other.address &&
                fetchGroup      == other.fetchGroup
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + fetchGroup.hashCode()
        result = 31 * result + immutable.hashCode()
        return result
    }
}
