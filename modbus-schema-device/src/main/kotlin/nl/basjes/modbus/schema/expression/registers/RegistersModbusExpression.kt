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
package nl.basjes.modbus.schema.expression.registers

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression.Problem
import java.util.TreeSet

private const val MOST_SIG_BYTE  = 0xFF00.toShort()
private const val LEAST_SIG_BYTE = 0x00FF.toShort()

/*
 * An Expression for which the result is a byte array of raw register values
 */
open class RegistersModbusExpression(
    private val registers: List<Address>
) : RegistersExpression {

    override val requiredRegisters: List<Address>
        get() = registers

    override val returnedRegisters: Int
        get() = registers.size

    override var isImmutable: Boolean = false

    override val problems: List<Problem>
        get() {
            if (registers.isEmpty()) {
                return listOf(Problem("No registers"))
            }
            // All registers must be a single incremental unique list without any gaps !
            // But they MAY be used out of order (so we sort them first for this check) !!!!
            val sortedSet = TreeSet(registers)
            if (sortedSet.size != registers.size) {
                return listOf(Problem("Duplicate registers: $registers")) // Apparently duplicates
            }

            var expectedAddress: Address? = null
            for (address in sortedSet) {
                if (expectedAddress == null) {
                    expectedAddress = address
                }
                if (expectedAddress!! != address) {
                    return listOf(Problem("Illegal Register range specified: $registers")) // Apparently duplicates
                }
                expectedAddress = expectedAddress.increment()
            }

            return listOf()
        }

    private val isSortedList
        get() = if(problems.isEmpty()) { registers == ArrayList(TreeSet(registers)) } else { false }

    /**
     * @return The list of bytes value or null in case of problems
     */
    override fun getByteArray(schemaDevice: SchemaDevice): ByteArray? {
        val registerBlock = schemaDevice.getRegisterBlock(addressClass)
        val bytes = ByteArray(registers.size * nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER)
        var nextByteIndex = 0
        for (register in registers) {
            val value = registerBlock.getValue(register) ?: return null
            val msb = (0xFF and ((value.toInt() and MOST_SIG_BYTE.toInt()) shr 8)).toByte()
            val lsb = (0xFF and (value.toInt() and LEAST_SIG_BYTE.toInt())).toByte()
            bytes[nextByteIndex++] = msb
            bytes[nextByteIndex++] = lsb
        }
        return bytes
    }

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> {
        val registerValues = ArrayList<RegisterValue>()
        val registerBlock = schemaDevice.getRegisterBlock(addressClass)
        for (register in registers) {
            registerValues.add(registerBlock[register])
        }
        return registerValues
    }

    private val addressClass: AddressClass
        get() = registers[0].addressClass

    override fun toString(): String {
        if (isSortedList) {
            if (registers.size > 1) {
                return registers[0].toString() + " # " + registers.size
            }
            return registers[0].toString()
        }
        return registers.joinToString(separator = ", ") { it.toString() }
    }
}
