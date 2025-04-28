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
package nl.basjes.modbus.schema.expression.numbers

import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression

abstract class IntegerSigned (
    private val name: String,
    private val bytesPerValue: Int,
    private val byteArray: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(bytesPerValue / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER, notImplemented), NumericalExpression {

    override fun toString(): String {
        return "$name(" + byteArray + super<NotImplemented>.toString() + ")"
    }

    override val subExpressions: List<Expression>
        get() = listOf(byteArray)

    override var isImmutable: Boolean = byteArray.isImmutable

    override val returnType: ReturnType
        get() = ReturnType.LONG

    override val problems: List<Problem>
        get() = combine(
            name,
            super<NumericalExpression>.problems,
            super<NotImplemented>.problems,
            checkFatal(byteArray.returnedRegisters == bytesPerValue / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER,
                "Wrong number of registers: Got ${byteArray.returnedRegisters}, need ${bytesPerValue / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER}"),
        )

    override fun getRegisterValues(schemaDevice: SchemaDevice) =
        byteArray.getRegisterValues(schemaDevice)

    abstract override fun getValueAsLong(schemaDevice: SchemaDevice): Long?
}
