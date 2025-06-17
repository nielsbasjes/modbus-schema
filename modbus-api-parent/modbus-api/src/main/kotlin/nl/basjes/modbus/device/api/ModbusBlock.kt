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
import java.util.TreeMap

class RegisterBlock(
    /** The AddressClass of ALL addresses in this RegisterBlock */
    addressClass: AddressClass,
): ModbusBlock<RegisterBlock, RegisterValue, Short>(
    addressClass,
    { addressClass: AddressClass    -> RegisterBlock(addressClass)  },
    { address:      Address         -> RegisterValue(address)       },
    16,
) {

    /**
     * Creating a list of hex values for all available values.
     * Any MISSING registers will be filled with "----"
     * So all values are 4 characters: ([0-9A-F][0-9A-F][0-9A-F][0-9A-F]|----)
     * @return List of hex values
     */
    fun toHexList(): List<String> = noGapsValuesList().map { it.asString }.toList()

    /**
     * Creating a single string of hex values for all available values.
     * Any MISSING registers will be filled with "----".
     * So all values are 4 characters: ([0-9A-F][0-9A-F][0-9A-F][0-9A-F]|----)
     * @return A String of mostly 4 char hex values, sometimes "----".
     */
    fun toHexString(): String = toHexList().joinToString(separator = " ")

    override fun asString() = toHexString()

    override fun toString(): String =
        firstAddress?.let {
            "Starting at " + it.toCleanFormat() + ": [ " + toHexString() + " ]"
        } ?: "Empty RegisterBlock"

}

// ------------------------------------------

class DiscreteBlock(
    /** The AddressClass of ALL addresses in this DiscreteBlock */
    addressClass: AddressClass,
): ModbusBlock<DiscreteBlock, DiscreteValue, Boolean>(
    addressClass,
    { addressClass: AddressClass    -> DiscreteBlock(addressClass)  },
    { address:      Address         -> DiscreteValue(address)       },
    1,
) {
    /**
     * Creating a list of hex values for all available values.
     * Any MISSING registers will be filled with "----"
     * So all values are 4 characters: ([0-9A-F][0-9A-F][0-9A-F][0-9A-F]|----)
     * @return List of hex values
     */
    fun toBitList(): List<String> = noGapsValuesList().map { it.asString }.toList()

    /**
     * Creating a single string of hex values for all available values.
     * Any MISSING registers will be filled with "----".
     * So all values are 4 characters: ([0-9A-F][0-9A-F][0-9A-F][0-9A-F]|----)
     * @return A String of mostly 4 char hex values, sometimes "----".
     */
    fun toBitString(): String = toBitList().joinToString(separator = " ")

    override fun asString() = toBitString()

    override fun toString(): String =
        firstAddress?.let {
            "Starting at " + it.toCleanFormat() + ": [ " + toBitString() + " ]"
        } ?: "Empty DiscreteBlock"

}

// ------------------------------------------

/**
 * The collection of all requested Registers that are in the same address space of a single specific device.
 * The map from address to register in a SORTED way.
 * This means we can iterate over the keys sequentially (yet there can be Address gaps!).
 */
sealed class ModbusBlock<BLOCK: ModbusBlock<BLOCK, VALUE, TYPE>, VALUE: ModbusValue<VALUE, TYPE>, TYPE>(
    /** The AddressClass of ALL addresses in this RegisterBlock */
    val addressClass: AddressClass,
    private val newBlock: (AddressClass) -> BLOCK,
    private val newValue: (Address) -> VALUE,
    requiredBitsPerValue: Int,
) {
    private val modbusValues = TreeMap<Address, VALUE>()

    init {
        require(addressClass.bitsPerValue == requiredBitsPerValue ) {
            throw ModbusIllegalAddressClassException("The used AddressClass must be $requiredBitsPerValue bits per value (${addressClass.longLabel} has ${addressClass.bitsPerValue} bits per value)")
        }
    }

    fun clear() {
        // We want to keep the comments.
        modbusValues.values.forEach { it.clear() }
    }

    operator fun get(address: Address): VALUE {
        assertAddressClass(address.addressClass)
        return modbusValues.computeIfAbsent(address) { newValue(address) }
    }

    operator fun set(
        address: Address,
        modbusValue: VALUE,
    ) {
        if (modbusValues[address] != null && modbusValue.hasValue()) {
            setValue(modbusValue.address, modbusValue.value!!, modbusValue.fetchTimestamp)
        } else {
            modbusValues[address] = modbusValue
        }
        modbusValues.keys
    }

    val firstAddress: Address?
        get() =
            if (modbusValues.isEmpty()) {
                null
            } else {
                modbusValues.firstKey()
            }

    val keys
        get() = modbusValues.keys
    val values
        get() = modbusValues.values
    val size
        get() = modbusValues.size

    fun getOrCreateIfAbsent(
        requiredRegister: Address,
    ) = modbusValues.computeIfAbsent(requiredRegister, newValue)

    fun put(value: VALUE) {
        this[value.address].setValue(value)
    }

    /**
     * Get the values for the provided addresses.
     * @param addresses The register addresses we need the values for.
     * @return The list of values which may be empty!
     */
    fun get(addresses: List<Address>): List<VALUE> = addresses.mapNotNull { key: Address -> modbusValues[key] }

    fun put(
        key: Address,
        value: VALUE,
    ): VALUE? {
        assertAddressClass(key.addressClass)
        require(key == value.address) { "The address MUST be the same as the address in the register value" }
        return modbusValues.put(key, value)
    }

    /**
     * Set a not-yet-loaded register value IFF absent
     * @param address The address of the new value
     */
    fun setValue(address: Address) {
        this[address]
    }

    /**
     * Mark the provided address as a soft read error
     */
    fun setReadError(address: Address) {
        this[address].setSoftReadError()
    }

    fun setValue(
        address: Address,
        value: TYPE,
        timestamp: Long,
    ) {
        this[address].setValue(value, timestamp)
    }

    fun merge(modbusBlock: BLOCK) {
        assertAddressClass(modbusBlock.addressClass)
        for (registerValue in modbusBlock.modbusValues.values) {
            put(registerValue)
        }
    }

    fun getValue(address: Address): TYPE? {
        val modbusValue = modbusValues.get(address) ?: return null
        return modbusValue.value
    }

    fun noGapsValuesList(): List<VALUE> {
        val result: MutableList<VALUE> = mutableListOf()
        if (!modbusValues.isEmpty()) {
            var expectedNextAddress: Address = modbusValues.firstKey() // The address we expect of the next entry
            for (value in modbusValues.values) {
                while (value.address != expectedNextAddress) {
                    result.add(newValue(expectedNextAddress)) // Put a null dummy in
                    expectedNextAddress = expectedNextAddress.increment()
                }
                result.add(value)
                expectedNextAddress = expectedNextAddress.increment()
            }
        }
        return result
    }

    fun toMultiLineString(): String {
        val sb = StringBuilder()
        val valuesList = noGapsValuesList()
        // The number of elements on THIS line
        var lineCount = 0
        for (value in valuesList) {
            var comment = value.comment
            if (!comment.isNullOrEmpty()) {
                comment = comment.replace("{address}", value.address.toCleanFormat())
                if (lineCount > 0) {
                    sb.append("\n")
                }
                sb.append("\n# " + comment.replace("\n", "\n# ") + "\n")
                lineCount = 0
            }
            if (lineCount > 0) {
                sb.append(" ")
            }
            sb.append(value.asString)
            lineCount++
            if (lineCount >= 10) {
                sb.append("\n")
                lineCount = 0
            }
        }
        return sb.toString()
    }

    abstract fun asString(): String

    fun asString(multiLine: Boolean): String {
        return if (multiLine) {
            toMultiLineString()
        } else {
            asString()
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RegisterBlock) {
            return false
        }
        if (addressClass != other.addressClass) {
            return false
        }
        return super.equals(other)
    }

    private fun assertAddressClass(addressClass: AddressClass) {
        if (this.addressClass == addressClass) {
            return  // Ok.
        }
        throw ModbusIllegalAddressClassException(
            "Trying to use the AddressClass \"$addressClass\" on a ModbusBlock which only allows AddressClass \"${this.addressClass}\"",
        )
    }

    fun clone(): BLOCK {
        val result = newBlock(addressClass)
        for (value in modbusValues.values) {
            result.modbusValues[value.address] = value.clone()
        }
        return result
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + addressClass.hashCode()
        return result
    }
}


private fun String.toCleanedArray(): Array<String> =
    this
        .replace(Regex("#.*\n"), "\n")  // Drop '#' comments
        .replace(Regex("//.*\n"), "\n") // Drop '//' comments
        .replace(Regex("^ +"), "")      // Drop leading spaced
        .replace("\n", " ")              // Make it all 1 line
        .replace(Regex(" +"), " ")       // Make it all 1 space separators
        .lowercase()
        .split(" ".toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()


// Timestamp is fixed value because loaded from file: 2001-02-03T04:05:06:789Z (i.e. 123456789)
private const val standardFixedYamlTimeStamp = 981173106789L
fun String.toDiscreteBlock(
    /** The Address of the first discrete in the list */
    firstAddress: Address,
): DiscreteBlock {
    val addressClass = firstAddress.addressClass
    val discreteBlock = DiscreteBlock(addressClass)
    var currentPhysicalAddress = firstAddress.physicalAddress

    for (word in toCleanedArray()) {
        val toParse = word.trim { it <= ' ' }
        if (toParse.isEmpty()) {
            continue  // Skip completely empty values
        }
        toParse.toCharArray().map { it.toString() }.forEach {
            val currentAddress = Address.of(addressClass, currentPhysicalAddress)
            when (it) {
                "-" -> discreteBlock.setValue(currentAddress)
                "x" -> discreteBlock.setReadError(currentAddress)
                "0" -> discreteBlock.setValue(currentAddress, false, standardFixedYamlTimeStamp)
                "1" -> discreteBlock.setValue(currentAddress, true, standardFixedYamlTimeStamp)
                else -> throw ModbusApiException("The unable to parse \"$toParse\" into a valid discrete value")
            }
            currentPhysicalAddress++
        }
    }
    return discreteBlock
}


fun String.toRegisterBlock(
    /** The Address of the first register in the list */
    firstAddress: Address,
): RegisterBlock {
    val addressClass = firstAddress.addressClass
    val registerBlock = RegisterBlock(addressClass)
    var currentPhysicalAddress = firstAddress.physicalAddress

    for (word in toCleanedArray()) {
        var toParse = word.trim { it <= ' ' }
        if (toParse.isEmpty()) {
            continue  // Skip completely empty values
        }
        val currentAddress = Address.of(addressClass, currentPhysicalAddress)
        toParse = toParse.lowercase()
        when (toParse) {
            "null", "----" -> {
                registerBlock.setValue(currentAddress)
            }

            "xxxx" -> {
                registerBlock.setReadError(currentAddress)
            }

            else -> {
                val parsedInt = toParse.toInt(16)
                val value = (parsedInt and 0xFFFF).toShort()

                // Timestamp is fixed value because loaded from file: 2001-02-03T04:05:06:789Z (i.e. 123456789)
                registerBlock.setValue(currentAddress, value, 981173106789L)
            }
        }

        currentPhysicalAddress++
    }
    return registerBlock
}
