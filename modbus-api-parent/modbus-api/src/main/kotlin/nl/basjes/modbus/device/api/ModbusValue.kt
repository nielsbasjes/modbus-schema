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

import nl.basjes.modbus.device.exception.ModbusApiException
import nl.basjes.modbus.device.exception.ModbusIllegalAddressClassException

// ------------------------------------------

/**
 * Holds the single value of a single modbus register
 */
class RegisterValue(
    /**
     * The modbus register address.
     */
    address: Address,
) : ModbusValue<RegisterValue, Short>(address, { RegisterValue(address) }, 16 ) {
    /**
     * @return The current register value as a 4 digit HEX string in uppercase. Or "----" in case of null.
     */
    override fun toSingleValueString(): String {
        val localValue = value
        if (localValue != null) {
            return String.format("%04X", localValue)
        }
        if (isReadError()) {
            return if (hardReadError) "XXXX" else "xxxx"
        }
        return "----"
    }

    override fun toString(): String {
        var stringComment = ""
        if (!comment.isNullOrBlank()) {
            stringComment = " /* $comment */"
        }
        if (hasValue()) {
            return "{$address=0x${toSingleValueString()}}$stringComment"
        }
        return "{$address=${toSingleValueString()}}$stringComment"
    }

}

// ------------------------------------------

/**
 * Holds the single value of a single modbus register
 */
class DiscreteValue(
    /**
     * The modbus register address.
     */
    address: Address,
) : ModbusValue<DiscreteValue, Boolean>(address, { DiscreteValue(address) }, 1 ) {
    /**
     * @return The current value as a 1 character String ("0"=false, "1"=true, "x"=Soft read error, "X"=Hard read error, "-" in case of no value available yet).
     */
    override fun toSingleValueString(): String {
        val localValue = value
        if (localValue != null) {
            return if (localValue) "1" else "0"
        }
        if (isReadError()) {
            return if (hardReadError) "X" else "x"
        }
        return "-"
    }

    override fun toString(): String {
        var stringComment = ""
        if (!comment.isNullOrBlank()) {
            stringComment = " /* $comment */"
        }
        return "{$address=$asString}$stringComment"
    }

}

// ------------------------------------------

/**
 * Holds the single value of a single modbus coil/discrete input or register
 */
sealed class ModbusValue<VALUE : ModbusValue<VALUE, TYPE>, TYPE>(
    /**
     * The modbus coil/discrete/register input address.
     */
    val address: Address,
    internal val makeANewInstance: (address: Address) -> VALUE,
    requiredBitsPerValue: Int,
) : Comparable<VALUE> {

    init {
        require(address.addressClass.bitsPerValue == requiredBitsPerValue ) {
            throw ModbusIllegalAddressClassException("The used AddressClass must be $requiredBitsPerValue bits per value " +
                    "(${address.addressClass.longLabel} has ${address.addressClass.bitsPerValue} bits per value)")
        }
    }

    /**
     * An identifier to that can be used to ensure some registers are retrieved together.
     * By default, filled with a random unique value because we assume they are all independent.
     */
    var fetchGroup = "FG_" + address.toCleanFormat()

    /**
     * Some registers will NEVER change and thus do not need to be retrieved a second time
     */
    var immutable = false

    /*
     * The last known value.
     */
    var value: TYPE? = null
        private set

    /**
     * If a value has a comment this can be used when converting it to a String (in yaml for example)
     */
    var comment: String? = null

    /*
     * The timestamp (epoch in milliseconds) of the last known value of the register.
     */
    internal var fetchTimestamp: Long = Long.MIN_VALUE

    val timestamp: Long?
        get() = if (fetchTimestamp <= NEVER_VALID_BEFORE || immutable) null else fetchTimestamp

    fun clone(): VALUE {
        val modbusValue = makeANewInstance(address)
        modbusValue.fetchGroup      = fetchGroup
        modbusValue.immutable       = immutable
        modbusValue.value           = value
        modbusValue.comment         = comment
        modbusValue.fetchTimestamp  = fetchTimestamp
        modbusValue.hardReadError   = hardReadError
        return modbusValue
    }

    fun setValue(value: TYPE): VALUE = setValue(value, System.currentTimeMillis())

    fun setValue(modbusValue: VALUE): VALUE {
        this.value           = modbusValue.value
        this.fetchTimestamp  = modbusValue.fetchTimestamp
        this.hardReadError   = modbusValue.hardReadError
        @Suppress("UNCHECKED_CAST")
        return this as VALUE
    }

    fun setValue(
        value: TYPE,
        timestamp: Long,
    ): VALUE {
        this.value          = value
        this.fetchTimestamp = timestamp
        this.hardReadError  = false
        @Suppress("UNCHECKED_CAST")
        return this as VALUE
    }

    fun hasValue(): Boolean = value           != null

    // If a read error is NOT hard it can be reset
    // If a read error IS hard it cannot be reset
    var hardReadError = false

    fun setSoftReadError() {
        this.value           = null
        this.fetchTimestamp = READERROR_TIMESTAMP
        this.hardReadError = false
    }

    fun setHardReadError() {
        this.value           = null
        this.fetchTimestamp = READERROR_TIMESTAMP
        this.hardReadError = true
    }

    fun isReadError(): Boolean = (value           == null && fetchTimestamp == READERROR_TIMESTAMP)

    fun clearSoftReadError() {
        if (isReadError() && !hardReadError) {
            value           = null
            fetchTimestamp = Long.MIN_VALUE
        }
    }

    fun needsToBeUpdated(
        now: Long,
        maxAge: Long,
    ): Boolean {
        if (isReadError()) {
            return false
        }
        if (value           == null) {
            return true
        }
        if (immutable) {
            return false
        }
        // Any register with a valid value MUST be after 1900-01-01T00:00:00Z
        if (fetchTimestamp < NEVER_VALID_BEFORE) {
            return true
        }
        return now - fetchTimestamp > maxAge
    }

    fun clear() {
        value           = null
        fetchTimestamp = Long.MIN_VALUE
    }

    val asString: String
        get() = toSingleValueString()

    abstract fun toSingleValueString(): String

    override fun compareTo(other: VALUE): Int {
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
        if (other !is ModbusValue<VALUE, TYPE>) {
            return false
        }
        @Suppress("ktlint:standard:indent")
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

