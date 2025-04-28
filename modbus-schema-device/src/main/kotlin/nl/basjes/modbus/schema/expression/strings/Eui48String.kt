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
import java.util.Arrays

class Eui48String(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(registers.returnedRegisters, notImplemented), StringExpression {

    override fun toString(): String {
        return "eui48(" + registers + super<NotImplemented>.toString() + ")"
    }

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override var isImmutable: Boolean = false

    override val problems: List<Problem>
        get() =
            combine(
                "enum",
                // Only sizes 3 and 4 are allowed
                checkFatal(listOf(3,4 ).contains(registers.returnedRegisters), "Must have 3 or 4 registers (got ${registers.returnedRegisters})"),
                super<StringExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> {
        return registers.getRegisterValues(schemaDevice)
    }

    override fun getValue(schemaDevice: SchemaDevice): String? {
        var bytes = registers.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        if (bytes.size > 3 * nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER) {
            bytes = Arrays.copyOfRange(bytes, bytes.size - (3 * nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER), bytes.size)
        }
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return ByteConversions.bytesToSeparatedHexString(bytes, ":")
    }
}
