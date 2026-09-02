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

import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.utils.BITS64_IN_REGISTERS
import nl.basjes.modbus.device.utils.BYTES_PER_REGISTER
import nl.basjes.modbus.device.utils.INTEGER_BYTES
import nl.basjes.modbus.device.utils.LONG_BYTES
import nl.basjes.modbus.device.utils.SHORT_BYTES
import nl.basjes.modbus.device.utils.toHexString0x
import nl.basjes.modbus.device.utils.toInteger
import nl.basjes.modbus.device.utils.toLong
import nl.basjes.modbus.device.utils.toShort
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.generic.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression

class EnumString(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
    val mappings: Map<Long, String>,
) : NotImplemented(registers.returnedAddresses, notImplemented),
    StringExpression {

    override fun toString(): String =
        "enum(" + registers + super<NotImplemented>.toString() + " ; " +
            mappings.entries.joinToString(" ; ") { "${it.key}->'${it.value}'" } +
            ")"

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override var isImmutable: Boolean = false

    override val problems: List<Problem>
        get() =
            combine(
                "enum",
                checkFatal(registers.returnedAddresses > 0, "No registers"),
                checkFatal(registers.returnedAddresses <= BITS64_IN_REGISTERS, "Too many registers"),
                super<StringExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getModbusValues(schemaDevice: SchemaDevice) = registers.getModbusValues(schemaDevice)

    @Throws(ModbusException::class)
    override fun getValue(schemaDevice: SchemaDevice): String? {
        val bytes = registers.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        val value =
            when (bytes.size) {
                SHORT_BYTES ->      bytes.toShort().toLong()
                INTEGER_BYTES ->    bytes.toInteger().toLong()
                LONG_BYTES ->       bytes.toLong()
                else -> null
            }
        var mappedValue = mappings[value]
        if (mappedValue == null) {
            mappedValue = "No mapping for value " + bytes.toHexString0x()
        }
        return mappedValue
    }
}
