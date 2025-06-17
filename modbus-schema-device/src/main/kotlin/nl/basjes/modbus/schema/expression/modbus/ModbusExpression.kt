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
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.DiscreteBlock
import nl.basjes.modbus.device.api.DiscreteValue
import nl.basjes.modbus.device.api.ModbusValue
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.booleans.BooleanExpression
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import java.util.TreeSet

private const val MOST_SIG_BYTE = 0xFF00.toShort()
private const val LEAST_SIG_BYTE = 0x00FF.toShort()

/*
 * An Expression for which the result is a raw boolean values
 */
class DiscreteModbusExpression(
    val address: Address,
) : BooleanExpression, ModbusExpression(listOf(address)) {

    override fun getBoolean(schemaDevice: SchemaDevice): Boolean? {
        val discreteBlock = schemaDevice.getModbusBlock(addressClass)
        require(discreteBlock is DiscreteBlock) {
            "This should occur: doing getBoolean() on address $addresses (not discrete)."
        }
        return discreteBlock[address].value
    }

    override fun getModbusValues(schemaDevice: SchemaDevice): List<ModbusValue<*, *>> {
        val discreteValues = ArrayList<DiscreteValue>()
        val discreteBlock = schemaDevice.getModbusBlock(addressClass)
        require(discreteBlock is DiscreteBlock) {
            "This should occur: doing getModbusValues() on address $addresses (not discrete)."
        }
        discreteValues.add(discreteBlock[address])
        return discreteValues
    }

}
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

/*
 * An Expression for which the result is a byte array of raw register values
 */
sealed class ModbusExpression(
    protected val addresses: List<Address>,
) : Expression {

    override val requiredAddresses: List<Address>
        get() = addresses

    override var isImmutable: Boolean = false

    override val requiredMutableAddresses: List<Address>
        get() = addresses

    override val problems: List<Problem>
        get() {
            if (addresses.isEmpty()) {
                return listOf(Problem("No addresses"))
            }
            // All registers must be a single incremental unique list without any gaps !
            // But they MAY be used out of order (so we sort them first for this check) !!!!
            val sortedSet = TreeSet(addresses)
            if (sortedSet.size != addresses.size) {
                return listOf(Problem("Duplicate addresses: $addresses")) // Apparently duplicates
            }

            var expectedAddress: Address? = null
            for (address in sortedSet) {
                if (expectedAddress == null) {
                    expectedAddress = address
                }
                if (expectedAddress!! != address) {
                    return listOf(Problem("Illegal Address range specified: $addresses")) // Apparently duplicates
                }
                expectedAddress = expectedAddress.increment()
            }

            return listOf()
        }

    private val isSortedList
        get() =
            if (problems.isEmpty()) {
                addresses == ArrayList(TreeSet(addresses))
            } else {
                false
            }

    protected val addressClass: AddressClass
        get() = addresses[0].addressClass

    override fun toString(): String {
        if (isSortedList) {
            if (addresses.size > 1) {
                return addresses[0].toString() + " # " + addresses.size
            }
            return addresses[0].toString()
        }
        return addresses.joinToString(separator = ", ") { it.toString() }
    }
}
