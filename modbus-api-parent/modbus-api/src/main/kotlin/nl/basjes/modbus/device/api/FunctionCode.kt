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

import nl.basjes.modbus.device.api.AddressClass.COIL
import nl.basjes.modbus.device.api.AddressClass.DISCRETE_INPUT
import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.AddressClass.INPUT_REGISTER

enum class FunctionCode(
    /** The underlying modbus function code  */
    private val code: Int,
    // The AddressClass on which this function code MAY be used */
    private val addressClass: AddressClass,
    /** If readonly then false  */
    private val forWriting: Boolean,
    /**
     * If readonly then false,
     * If writable then true if this one is for writing multiple.
     */
    private val forWritingMultiple: Boolean,
    /** The java code name of this enum which can be used for code generation  */
    val enumName: String,
    /** A human-readable name for the FunctionCode  */
    val readableName: String,
) {
    READ_COIL(                        0x01, COIL,             false, false, "READ_COIL",                        "Read Coil"),
    READ_DISCRETE_INPUT(              0x02, DISCRETE_INPUT,   false, false, "READ_DISCRETE_INPUT",              "Read Discrete Input"),
    READ_HOLDING_REGISTERS(           0x03, HOLDING_REGISTER, false, false, "READ_HOLDING_REGISTERS",           "Read Holding Registers" ),
    READ_INPUT_REGISTERS(             0x04, INPUT_REGISTER,   false, false, "READ_INPUT_REGISTERS",             "Read Input Registers" ),

    WRITE_SINGLE_COIL(                0x05, COIL,             true,  false, "WRITE_SINGLE_COIL",                "Write Single Coil"),
    WRITE_SINGLE_HOLDING_REGISTER(    0x06, HOLDING_REGISTER, true,  false, "WRITE_SINGLE_HOLDING_REGISTER",    "Write Single Holding Register"),
    WRITE_MULTIPLE_COILS(             0x0F, COIL,             true,  true,  "WRITE_MULTIPLE_COILS",             "Write Multiple Coils"),
    WRITE_MULTIPLE_HOLDING_REGISTERS( 0x10, HOLDING_REGISTER, true,  true,  "WRITE_MULTIPLE_HOLDING_REGISTERS", "Write Multiple Holding Registers" );

    // Sometimes 1 bit per value (booleans really), sometimes 16 bits (normal registers) */
    val bitsPerValue = addressClass.bitsPerValue

    val isForReading: Boolean
        get() = !forWriting

    val isForWritableSingle: Boolean
        get() = forWriting && !forWritingMultiple

    override fun toString(): String {
        return super.toString() + "($code)"
    }

    companion object {
        /**
         * Retrieve the FunctionCode instance by its numerical code
         * @param code The numerical modbus function code.
         * @return The requested FunctionCode or null if not found
         */
        @JvmStatic
        fun of(code: Int): FunctionCode {
            return entries
                .firstOrNull { it.code == code } ?:
                    throw IllegalArgumentException("The provided function code value $code is not supported.")
        }

        /**
         * Retrieve the read FunctionCode instance which can handle the provided AddressClass
         * @param addressClass The class of the address for which to retrieve the FunctionCode.
         * @return The FunctionCode(s) that can be used for reading
         */
        @JvmStatic
        fun forReading(addressClass: AddressClass): FunctionCode {
            return entries
                .filter { it.addressClass == addressClass }
                .filter { it.isForReading }
                .firstOrNull()
                ?: throw IllegalArgumentException("Unable to find the read function code for $addressClass.")
        }

        /**
         * Retrieve the writing FunctionCode instance which can handle the provided AddressClass
         * @param addressClass The class of the address for which to retrieve the FunctionCode.
         * @return The FunctionCode that can be used for writing a single register
         */
        @JvmStatic
        fun forWritingSingle(addressClass: AddressClass): FunctionCode {
            return entries
                .filter { it.addressClass == addressClass }
                .firstOrNull { it.isForWritableSingle }
                    ?: throw IllegalArgumentException("Unable to find the write single function code for $addressClass.")
        }

        /**
         * Retrieve the writing FunctionCode instance which can handle the provided AddressClass
         * @param addressClass The class of the address for which to retrieve the FunctionCode.
         * @return The FunctionCode that can be used for writing a single register
         */
        @JvmStatic
        fun forWritingMultiple(addressClass: AddressClass): FunctionCode {
            return entries
                .filter { it.addressClass == addressClass }
                .firstOrNull { it.forWritingMultiple }
                    ?: throw IllegalArgumentException("Unable to find the write multiple function code for $addressClass.")
        }
    }
}
