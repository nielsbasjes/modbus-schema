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
package nl.basjes.modbus.schema.expression.booleans

import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.LONG_BYTES
import nl.basjes.modbus.schema.expression.generic.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions
import java.util.BitSet

class BitsetBoolean(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
    val bitNr: Int,
) : NotImplemented(registers.returnedRegisters, notImplemented),
    BooleanExpression {

    override fun toString(): String =
        "bitsetbit(" + registers + super<NotImplemented>.toString() + " ; " + bitNr + ")"

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override val returnType: ReturnType
        get() = ReturnType.BOOLEAN

    override var isImmutable: Boolean
        get() = registers.isImmutable
        set(value) {
            registers.isImmutable = value
        }

    override val problems: List<Problem>
        get() =
            combine(
                "bitsetbit",
                checkFatal(registers.returnedRegisters > 0, "No registers"),
                checkFatal(
                    registers.returnedRegisters <=
                        LONG_BYTES / BYTES_PER_REGISTER,
                    "Too many registers",
                ),
                checkFatal(
                    bitNr >= 0,
                    "Negative bitNr requested",
                ),
                checkFatal(
                    bitNr < registers.returnedRegisters * BYTES_PER_REGISTER * 8,
                    "The requested bitNr $bitNr is larger than the maximum of ${(registers.returnedRegisters * BYTES_PER_REGISTER * 8) - 1 }",
                ),
                super<BooleanExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> = registers.getRegisterValues(schemaDevice)

    override fun getValueAsBoolean(schemaDevice: SchemaDevice): Boolean? {
        val bytes = registers.getByteArray(schemaDevice)
        if (bytes == null || bytes.isEmpty() || isNotImplemented(bytes)) {
            return null // Not implemented
        }
        ByteConversions.reverse(bytes)
        val bitSet = BitSet.valueOf(bytes)
        return bitSet[bitNr]
    }

}
