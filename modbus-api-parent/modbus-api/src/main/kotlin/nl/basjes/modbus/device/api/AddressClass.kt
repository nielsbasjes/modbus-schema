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

enum class AddressClass(
    /** The "offset" used in the modicon notation. (I.e. the address is shifted by this much (* 10000, or * 100000) when specified).  */
    val baseOffset: Int,
    /** Sometimes 1 bit per value (booleans really), sometimes 16 bits (normal registers)  */
    val bitsPerValue: Int,
    /** Offset from the "Physical Modbus Address" to the "Register Number"  */
    val registerNumberOffset: Int,
    /** A human-readable name  */
    val readableId: String,
    /** The java code name of this enum which can be used for code generation  */
    val enumName: String,
    /** The additional labels on which the instance must also be returned.  */
    val shortLabel: String,
    val longLabel: String,
    vararg labels: String
) {
    /** A Coil is a read/write single bit value */
    COIL(             0,  1, 1, "Coil",             "COIL",             "c",  "coil",             "coil",             "coils"),
    /** A Discrete Input is a readonly single bit value */
    DISCRETE_INPUT(   1,  1, 1, "Discrete Input",   "DISCRETE_INPUT",   "di", "discrete-input",   "discrete input",   "discrete inputs"),
    /** An Input Register is a readonly 16 bit register */
    INPUT_REGISTER(   3, 16, 1, "Input Register",   "INPUT_REGISTER",   "ir", "input-register",   "input register",   "input registers" ),
    /** A Holding Register is a read/write 16 bit register */
    HOLDING_REGISTER( 4, 16, 1, "Holding Register", "HOLDING_REGISTER", "hr", "holding-register", "holding register", "holding registers" );

    val labels: MutableList<String> = ArrayList()

    init {
        this.labels.add(shortLabel)
        this.labels.add(longLabel)
        this.labels.addAll(listOf(*labels))
    }

    companion object {
        private val labelLookup: MutableMap<String, AddressClass> = HashMap()

        // Put all the enums also in a map for fast lookups
        init {
            for (addressClass in entries) {
                labelLookup[addressClass.baseOffset.toString()] = addressClass
                for (label in addressClass.labels) {
                    labelLookup[label] = addressClass
                    if (label.indexOf(' ') >= 0) {
                        labelLookup[label.replace(' ', '-')] = addressClass
                        labelLookup[label.replace(' ', '_')] = addressClass
                    }
                }
            }
        }

        /**
         * Retrieve the AddressClass instance by its textual name/code
         * @param label The label on which to retrieve the AddressClass.
         * @return The requested AddressClass or null if not found
         */
        @JvmStatic
        fun of(label: String): AddressClass {
            val addressClass = labelLookup[label.trim { it <= ' ' }.lowercase()]
            requireNotNull(addressClass) { "The provided address class label \"$label\" is not supported." }
            return addressClass
        }

        /**
         * Retrieve the address class by its register baseOffset
         * @param baseOffset The register baseOffset for which to retrieve the AddressClass.
         * @return The requested AddressClass or null if not found
         */
        @JvmStatic
        fun of(baseOffset: Int): AddressClass {
            return of(baseOffset.toString())
        }
    }
}
