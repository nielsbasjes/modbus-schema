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
package nl.basjes.modbus.schema.expression.strings

import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.INTEGER_BYTES
import nl.basjes.modbus.schema.expression.LONG_BYTES
import nl.basjes.modbus.schema.expression.NotImplemented
import nl.basjes.modbus.schema.expression.SHORT_BYTES
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToInteger
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToLong
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToShort

class EnumString(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
    val mappings: Map<Long, String>,
) : NotImplemented(registers.returnedRegisters, notImplemented),
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
                checkFatal(registers.returnedRegisters > 0, "No registers"),
                checkFatal(registers.returnedRegisters <= LONG_BYTES / BYTES_PER_REGISTER, "Too many registers"),
                super<StringExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> = registers.getRegisterValues(schemaDevice)

    @Throws(ModbusException::class)
    override fun getValue(schemaDevice: SchemaDevice): String? {
        val bytes = registers.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        val value =
            when (bytes.size) {
                SHORT_BYTES -> bytesToShort(bytes).toLong()
                INTEGER_BYTES -> bytesToInteger(bytes).toLong()
                LONG_BYTES -> bytesToLong(bytes)
                else -> null
            }
        var mappedValue = mappings[value]
        if (mappedValue == null) {
            mappedValue = "No mapping for value " + ByteConversions.bytesToHexString(bytes)
        }
        return mappedValue
    }
}
