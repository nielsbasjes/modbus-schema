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

import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.generic.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions

const val IPV4ADDR_REGISTERS = 2

class IPv4AddrString(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(IPV4ADDR_REGISTERS, notImplemented),
    StringExpression {

    override fun toString(): String = "ipv4addr(" + registers + super<NotImplemented>.toString() + ")"

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override var isImmutable: Boolean = false

    override val problems: List<Problem>
        get() =
            combine(
                "ipv4addr",
                checkFatal(
                    registers.returnedAddresses == IPV4ADDR_REGISTERS,
                    "Must have $IPV4ADDR_REGISTERS registers (got ${registers.returnedAddresses})",
                ),
                super<StringExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getModbusValues(schemaDevice: SchemaDevice) = registers.getModbusValues(schemaDevice)

    override fun getValue(schemaDevice: SchemaDevice): String? {
        val bytes = registers.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return ByteConversions.bytesToSeparatedUnsignedIntegerString(bytes, ".")
    }
}
