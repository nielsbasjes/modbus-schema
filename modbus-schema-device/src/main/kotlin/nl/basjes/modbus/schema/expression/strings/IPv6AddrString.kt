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
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions

const val IPv6Addr_REGISTERS = 8

class IPv6AddrString(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(IPv6Addr_REGISTERS, notImplemented), StringExpression {

    override fun toString(): String {
        return "ipv6addr(" + registers + super<NotImplemented>.toString() + ")"
    }

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override var isImmutable: Boolean = false

    override val problems: List<Problem>
        get() =
            combine(
                "ipv6addr",
                checkFatal(registers.returnedRegisters == IPv6Addr_REGISTERS, "Must have $IPv6Addr_REGISTERS registers (got ${registers.returnedRegisters})"),
                registers.problems,
                super<StringExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> {
        return registers.getRegisterValues(schemaDevice)
    }

    override fun getValue(schemaDevice: SchemaDevice): String? {
        val bytes = registers.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return ByteConversions.bytesToSeparatedTwoByteHexString(bytes, ":")
    }
}
