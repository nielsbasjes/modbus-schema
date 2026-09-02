/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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
package nl.basjes.modbus.schema.expression.strings

import nl.basjes.modbus.device.utils.BITS48_IN_BYTES
import nl.basjes.modbus.device.utils.BITS48_IN_REGISTERS
import nl.basjes.modbus.device.utils.BITS64_IN_REGISTERS
import nl.basjes.modbus.device.utils.BYTES_PER_REGISTER
import nl.basjes.modbus.device.utils.toSeparatedHexString
import nl.basjes.modbus.schema.SchemaDevice

import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.generic.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression

class Eui48String(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(registers.returnedAddresses, notImplemented),
    StringExpression {

    override fun toString(): String = "eui48(" + registers + super<NotImplemented>.toString() + ")"

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override var isImmutable: Boolean = false

    override val problems: List<Problem>
        get() =
            combine(
                "enum",
                // Only sizes 3 and 4 are allowed
                checkFatal(
                    listOf(BITS48_IN_REGISTERS, BITS64_IN_REGISTERS).contains(registers.returnedAddresses),
                    "Must have 3 or 4 registers (got ${registers.returnedAddresses})",
                ),
                super<StringExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getModbusValues(schemaDevice: SchemaDevice) = registers.getModbusValues(schemaDevice)

    override fun getValue(schemaDevice: SchemaDevice): String? {
        var bytes = registers.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        // If it is more than 48 bits then we only examine the last 48 bits in the ByteArray
        if (bytes.size > BITS48_IN_BYTES) {
            bytes = bytes.copyOfRange(bytes.size - BITS48_IN_BYTES, bytes.size)
        }
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return bytes.toSeparatedHexString(":")
    }
}
