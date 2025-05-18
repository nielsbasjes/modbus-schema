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
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem

class SwapBytes(
    val registers: RegistersExpression,
) : RegistersExpression {

    override fun toString(): String = "swapbytes($registers)"

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override val requiredRegisters: List<Address>
        get() = registers.requiredRegisters

    override val returnedRegisters: Int
        get() = registers.returnedRegisters

    override var isImmutable: Boolean
        get() = registers.isImmutable
        set(value) {
            registers.isImmutable = value
        }

    override val problems: List<Problem>
        get() =
            combine(
                "swapbytes",
                checkFatal(registers.returnedRegisters == 1, "Need exactly 1 register)"),
            )

    override fun getByteArray(schemaDevice: SchemaDevice): ByteArray? {
        val input = registers.getByteArray(schemaDevice) ?: return null
        val output = ByteArray(2)
        output[0] = input[1]
        output[1] = input[0]
        return output
    }
}
