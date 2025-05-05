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

import java.util.TreeMap

/**
 * The collection of all requested Registers that are in the same address space of a single specific device.
 * The map from address to register in a SORTED way.
 * This means we can iterate over the keys sequentially (yet there can be Address gaps!).
 */
class RegisterBlock (
    /** The AddressClass of ALL addresses in this RegisterBlock */
    val addressClass: AddressClass
) {
    private val registerValues = TreeMap<Address, RegisterValue>()

    fun clear() {
        // We want to keep the comments.
        registerValues.values.forEach { it.clear() }
    }

    operator fun get(address: Address): RegisterValue {
        assertAddressClass(address.addressClass)
        return registerValues.computeIfAbsent(address) { RegisterValue(address) }
    }

    operator fun set(address: Address, registerValue: RegisterValue) {
        if (registerValues[address] != null && registerValue.hasValue()) {
            setValue(registerValue.address, registerValue.value!!, registerValue.fetchTimestamp)
        } else {
            registerValues[address] = registerValue
        }
        registerValues.keys
    }
    val firstAddress: Address
        get() = registerValues.firstKey()
    val keys
        get() = registerValues.keys
    val values
        get() = registerValues.values
    val size
        get() = registerValues.size
    fun computeIfAbsent(requiredRegister: Address, function: (Address) -> RegisterValue) =
        registerValues.computeIfAbsent(requiredRegister, function)

    fun put(value: RegisterValue) {
        this[value.address].setValue(value)
    }

    /**
     * Get the values for the provided addresses.
     * @param addresses The register addresses we need the values for.
     * @return The list of values which may be empty!
     */
    fun get(addresses: List<Address>): List<RegisterValue> {
        return addresses.mapNotNull { key: Address -> registerValues[key] }
    }

    fun put(key: Address, value: RegisterValue): RegisterValue? {
        assertAddressClass(key.addressClass)
        require(key == value.address) { "The address MUST be the same as the address in the register value" }
        return registerValues.put(key, value)
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

    fun setValue(address: Address, value: Short, timestamp: Long) {
        this[address].setValue(value, timestamp)
    }

    fun merge(registerBlock: RegisterBlock) {
        assertAddressClass(registerBlock.addressClass)
        for (registerValue in registerBlock.registerValues.values) {
            put(registerValue)
        }
    }

    fun getValue(address: Address): Short? {
        val registerValue = registerValues.get(address) ?: return null
        return registerValue.value
    }

    fun noGapsValuesList(): List<RegisterValue> {
        val result: MutableList<RegisterValue> = mutableListOf()
        if (!registerValues.isEmpty()) {
            var expectedNextAddress: Address = registerValues.firstKey() // The address we expect of the next entry
            for (value in registerValues.values) {
                while (value.address != expectedNextAddress) {
                    result.add(RegisterValue(expectedNextAddress)) // Put a null dummy in
                    expectedNextAddress = expectedNextAddress.increment()
                }
                result.add(value)
                expectedNextAddress = expectedNextAddress.increment()
            }
        }
        return result
    }

    /**
     * Creating a list of hex values for all available values.
     * Any MISSING registers will be filled with "----"
     * So all values are 4 characters: ([0-9A-F][0-9A-F][0-9A-F][0-9A-F]|----)
     * @return List of hex values
     */
    fun toHexList(): List<String> {
        return noGapsValuesList().map { it.hexValue }.toList()
    }

    /**
     * Creating a single string of hex values for all available values.
     * Any MISSING registers will be filled with "----".
     * So all values are 4 characters: ([0-9A-F][0-9A-F][0-9A-F][0-9A-F]|----)
     * @return A String of mostly 4 char hex values, sometimes "----".
     */
    fun toHexString(): String {
        return toHexList().joinToString(separator = " ")
    }

    override fun toString(): String {
        return "Starting at " + registerValues.firstKey().toCleanFormat() + ": [ " + toHexString() + " ]"
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
        throw IllegalArgumentException("Trying to use the AddressClass \"$addressClass\" on a RegisterBlock which only allows AddressClass \"$addressClass\"")
    }

    fun clone(): RegisterBlock {
        val result = RegisterBlock(addressClass)
        for (value in registerValues.values) {
            result.registerValues[value.address] = value.clone()
        }
        return result
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + addressClass.hashCode()
        return result
    }
}

fun String.toRegisterBlock(
    /** The Address of the first register in the list */
    firstRegisterAddress: Address,
): RegisterBlock {
    val addressClass = firstRegisterAddress.addressClass
    val registerBlock = RegisterBlock(addressClass)
    var currentPhysicalAddress = firstRegisterAddress.physicalAddress

    val cleaned = this
        .replace(Regex("#.*\n"), "\n")  // Drop '#' comments
        .replace(Regex("//.*\n"), "\n") // Drop '//' comments
        .replace(Regex("^ +"), "")      // Drop leading spaced
        .replace("\n"," ")              // Make it all 1 line
        .replace(Regex(" +")," ")       // Make it all 1 space separators


    for (word in cleaned.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
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
            "xxxx" ->
                registerBlock.setReadError(currentAddress)
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

