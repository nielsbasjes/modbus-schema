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
 * The ModBus Address of a single register.
 * An Address instance is IMMUTABLE !!
 * @constructor Creates a new immutable Modbus Address instance.
 * @param addressClass The type of address (COIL/DISCRETE_INPUT/INPUT_REGISTER/HOLDING_REGISTER)
 * @param physicalAddress The physical wire address of the modbus register (0-65535). Usually 1 lower than what many modbus tools use.
 * @return A new immutable instance of Address with the given values
 */
class Address (
    /** The kind of the address (which limits the allowed function codes) */
    val addressClass: AddressClass,
    /** The technical wire address. Always in the 0-65535 range. */
    val physicalAddress: Int,
    ) : Comparable<Address> {

    /** The register number (as used in the original modicon notation) which is usually 1 higher than the physical address. */
    val registerNumber: Int
        get() = physicalAddress + addressClass.registerNumberOffset

    init {
        require(!(physicalAddress < 0 || physicalAddress > 0xFFFF)) {
            "Invalid Modbus register address specified for ${addressClass.longLabel}: $physicalAddress"
        }
    }

    companion object {
        /**
         * Creates a new Modbus Address instance.
         * @param addressClass The type of address (COIL/DISCRETE_INPUT/INPUT_REGISTER/HOLDING_REGISTER)
         * @param physicalAddress The physical wire address of the modbus register (0-65535). Usually 1 lower than what many modbus tools use.
         * @return A new immutable instance of Address with the given values
         */
        @JvmStatic
        fun of(
            /** The kind of the address (which limits the allowed function codes) */
            addressClass: AddressClass,
            /** The technical wire address. Always in the 0-65535 range. */
            physicalAddress: Int,
        ) = Address(addressClass, physicalAddress)

        /**
         * Create the address by parsing the provided numerical register format.
         * This ONLY allows the original Modicon 5 digit format.
         * @param modicon5RegisterTag The register's tag in the original Modicon 5 digit format.
         * @return A new immutable instance of Address with the given values
         */
        @JvmStatic
        fun ofModicon5(
            modicon5RegisterTag: Int,
        ):Address {
            val (addressClass, physicalAddress) = addressFromModicon5(modicon5RegisterTag)
            return Address(addressClass, physicalAddress)
        }

        /**
         * Create the address by parsing the provided numerical register format.
         * This ONLY allows the Modicon 6 digit format.
         * @param modicon6RegisterTag The register's tag in the Modicon 6 digit format.
         * @return a new immutable instance of Address with the given values
         */
        @JvmStatic
        fun ofModicon6(
            modicon6RegisterTag: Int,
        ):Address {
            val (addressClass, physicalAddress) = addressFromModicon6(modicon6RegisterTag)
            return Address(addressClass, physicalAddress)
        }

        /**
         * Create the address by parsing the provided register format.
         * @param registerTag The register's tag in one of the supported formats (for example: "40124", "400124", "4x124", "4x000124", "hr:123").
         * @return A new immutable instance of Address with the given values
         */
        @JvmStatic
        fun of(
            registerTag: String,
        ):Address {
            val (addressClass, physicalAddress) = addressFrom(registerTag)
            return Address(addressClass, physicalAddress)
        }
    }

    /**
     * Increment the address by `step`
     * @param step The desired address increase (step=1 if unspecified).
     * @return A NEW (incremented) immutable instance
     */
    @JvmOverloads
    fun increment(step: Int = 1): Address {
        return Address(addressClass, physicalAddress + step)
    }

    /**
     * @return The stored address as cleanly parsable format without the "off by one" problem.
     *          For example "hr:00123".
     */
    override fun toString(): String {
        return toCleanFormat()
    }

    /**
     * @return The stored address as cleanly parsable format without the "off by one" problem. For example "hr:00123".
     */
    fun toCleanFormat(): String {
        return String.format("%s:%05d", addressClass.shortLabel, physicalAddress)
    }

    /**
     * @return The stored address as an original modicon 5 digit value (or null if the register number is > 9999). For example "40124".
     */
    fun toModicon5(): String? {
        if (registerNumber > 9999) {
            return null
        }
        return String.format("%d%04d", addressClass.baseOffset, registerNumber);
    }

    /**
     * @return The stored address as a modicon 6 digit value. For example "400124".
     */
    fun toModicon6(): String {
        return String.format("%d%05d", addressClass.baseOffset, registerNumber);
    }

    /**
     * @return The stored address as a modicon variant with an 'x' separator. For example "4x00124".
     */
    fun toModiconX(): String {
        return String.format("%dx%05d", addressClass.baseOffset, registerNumber);
    }

    override fun compareTo(other: Address): Int {
        val addressClassCompared = addressClass.compareTo(other.addressClass)
        if (addressClassCompared != 0) {
            return addressClassCompared
        }
        return physicalAddress.compareTo(other.physicalAddress)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Address) {
            return false
        }
        return addressClass == other.addressClass &&
            physicalAddress == other.physicalAddress
    }

    override fun hashCode(): Int {
        var result = addressClass.hashCode()
        result = 31 * result + physicalAddress
        return result
    }

}

private val REGISTER_TAG_COLON_FORMAT       = Regex("^([a-zA-Z-]+):(\\d+)$")
private val REGISTER_TAG_X_FORMAT           = Regex("^(\\d)x(\\d+)$")
private val REGISTER_TAG_56DIGIT_FORMAT     = Regex("^(\\d)(\\d{4,5})$")

fun Int.asAddressAssumingModicon5() : Address {
    return Address.ofModicon5(this)
}

fun Int.asAddressAssumingModicon6() : Address {
    return Address.ofModicon6(this)
}

fun String.asAddress() : Address {
    return Address.of(this)
}

private fun addressFrom(registerTag: Int): Pair<AddressClass, Int> {
    // Modicon notation: MUST be positive and at most 5 digits (it is impossible to handle the 6 digit format)
    if (registerTag < 0 || registerTag > 99999) {
        throw IllegalArgumentException("Unable to parse the provided register tag \"$registerTag\".")
    }

    val modiconAddressClass   = registerTag / 10000
    val modiconRegisterNumber = registerTag - (modiconAddressClass * 10000)

    val addressClass = AddressClass.entries.find { it.baseOffset == modiconAddressClass }
    if (addressClass == null) {
        throw IllegalArgumentException("Unable to parse the provided register tag \"$registerTag\".")
    }
    return Pair(addressClass, modiconRegisterNumber - addressClass.registerNumberOffset)
}

private fun addressFromModicon5(registerTag: Int): Pair<AddressClass, Int> {
    return addressFrom(registerTag)
}

private fun addressFromModicon6(registerTag: Int): Pair<AddressClass, Int> {
    // Modicon notation: This MUST be the modicon 6 digit format!
    val modiconAddressClass   = registerTag / 100000
    val modiconRegisterNumber = registerTag - (modiconAddressClass * 100000)

    val addressClass = AddressClass.entries.find { it.baseOffset == modiconAddressClass }
    if (addressClass == null) {
        throw IllegalArgumentException("Unable to parse the provided register tag \"$registerTag\".")
    }
    return Pair(addressClass, modiconRegisterNumber - addressClass.registerNumberOffset)
}

private fun addressFrom(registerTag: String): Pair<AddressClass, Int> {
    val cleanedTag = registerTag.lowercase().trim { it <= ' ' }

    // Using the format which uses the physical address (like `hr:123`)
    // So `hr:123` becomes holding-register at wire address 123
    val tagColonResult = REGISTER_TAG_COLON_FORMAT.find(cleanedTag)
    if (tagColonResult != null) {
        val addressClassCode = tagColonResult.groupValues[1]
        val addressClass     = AddressClass.of(addressClassCode)
        val physicalAddress  = tagColonResult.groupValues[2].toInt()
        return Pair(addressClass, physicalAddress)
    }

    // Legacy format using the register number (i.e. off by 1)
    // So `4x124` becomes holding-register at wire address 123
    val tagXResult = REGISTER_TAG_X_FORMAT.find(cleanedTag)
    if (tagXResult != null) {
        val addressClassCode = tagXResult.groupValues[1]
        val addressClass     = AddressClass.of(addressClassCode)
        val registerNumber   = tagXResult.groupValues[2].toInt()
        val physicalAddress  = registerNumber - addressClass.registerNumberOffset
        return Pair(addressClass, physicalAddress)
    }

    // Legacy format (supports the 5 and 6 digit modicon variants) using the logical address
    // So `40124` and `400124` become holding-register at wire address 123
    val tag56DigitResult = REGISTER_TAG_56DIGIT_FORMAT.find(cleanedTag)
    if (tag56DigitResult != null) {
        val addressClassCode = tag56DigitResult.groupValues[1]
        val addressClass     = AddressClass.of(addressClassCode)
        val registerNumber   = tag56DigitResult.groupValues[2].toInt()
        val physicalAddress  = registerNumber - addressClass.registerNumberOffset
        return Pair(addressClass, physicalAddress)
    }

    throw IllegalArgumentException("Unable to parse the provided register tag \"$registerTag\".")
}
