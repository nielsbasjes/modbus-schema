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

import nl.basjes.modbus.device.exception.ModbusException

const val MODBUS_MAX_REGISTERS_PER_REQUEST: Int = 125
const val MODBUS_STANDARD_TCP_PORT: Int = 502

abstract class ModbusDevice : AutoCloseable {
    /**
     * The maximum number of modbus registers that can be requested PER call.
     * Some devices do not allow the normal max of 125.
     */
    var maxRegistersPerModbusRequest: Int = MODBUS_MAX_REGISTERS_PER_REQUEST
        set(value) {
            if (value < 1 || value > MODBUS_MAX_REGISTERS_PER_REQUEST) {
                throw ModbusException(
                    "The maxRegistersPerModbusRequest must be between 1 and $MODBUS_MAX_REGISTERS_PER_REQUEST" +
                        " (was set to $value).",
                )
            }
            field = value
        }

    /**
     * Retrieve a block of registers.
     *
     * @param firstRegister The first modbus register that is desired in the output.
     * @param count The maximum number of registers to retrieve ( >= 1 ).
     * @return A RegisterBlock with of all the retrieved registers
     */
    @Throws(ModbusException::class)
    abstract fun getRegisters(
        firstRegister: Address,
        count: Int,
    ): RegisterBlock

    // Explicitly override with a more restricted kind of exception because of
    // https://bugs.openjdk.org/browse/JDK-8155591
    @Throws(ModbusException::class)
    abstract override fun close()
}
