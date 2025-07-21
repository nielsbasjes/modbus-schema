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
package nl.basjes.modbus.schema.expression.modbus

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.ModbusValue
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER
import nl.basjes.modbus.schema.expression.registers.RegistersExpression

private const val MOST_SIG_BYTE = 0xFF00.toShort()
private const val LEAST_SIG_BYTE = 0x00FF.toShort()

/*
 * An Expression for which the result is a byte array of raw register values
 */
class RegistersModbusExpression(
    addresses: List<Address>,
) : RegistersExpression, ModbusExpression(addresses) {
    /**
     * @return The list of bytes value or null in case of problems
     */
    override fun getByteArray(schemaDevice: SchemaDevice): ByteArray? {
        val registerBlock = schemaDevice.getModbusBlock(addressClass)
        require(registerBlock is RegisterBlock) {
            "This should occur: doing getByteArray() on address $addresses (not registers)."
        }
        val bytes = ByteArray(addresses.size * BYTES_PER_REGISTER)
        var nextByteIndex = 0
        for (address in addresses) {
            val value = registerBlock.getValue(address) ?: return null
            val msb = (0xFF and ((value.toInt() and MOST_SIG_BYTE.toInt()) shr 8)).toByte()
            val lsb = (0xFF and (value.toInt() and LEAST_SIG_BYTE.toInt())).toByte()
            bytes[nextByteIndex++] = msb
            bytes[nextByteIndex++] = lsb
        }
        return bytes
    }

    override fun getModbusValues(schemaDevice: SchemaDevice): List<ModbusValue<*,*>> {
        val registerValues = ArrayList<RegisterValue>()
        val registerBlock = schemaDevice.getModbusBlock(addressClass)
        require(registerBlock is RegisterBlock) {
            "This should occur: doing getRegisterValues() on address $addresses (not registers)."
        }
        for (register in addresses) {
            registerValues.add(registerBlock[register])
        }
        return registerValues
    }

    override val returnedAddresses: Int
        get() = addresses.size
}
