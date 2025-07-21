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
import nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER
import nl.basjes.modbus.schema.expression.DOUBLE_BYTES
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.generic.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions

class IEEE754Float64(
    private val byteArray: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(
        DOUBLE_BYTES / BYTES_PER_REGISTER,
        notImplemented,
    ),
    NumericalExpression {

    override fun toString(): String = "ieee754_64($byteArray" + super<NotImplemented>.toString() + ")"

    override val subExpressions: List<Expression>
        get() = listOf(byteArray)

    override var isImmutable: Boolean = false

    override val returnType: ReturnType
        get() = ReturnType.DOUBLE

    override val problems: List<Problem>
        get() =
            combine(
                "ieee754_64",
                super<NumericalExpression>.problems,
                super<NotImplemented>.problems,
                checkFatal(
                    byteArray.returnedAddresses ==
                        DOUBLE_BYTES / BYTES_PER_REGISTER,
                    "Wrong number of registers: Got ${byteArray.returnedAddresses}, need ${DOUBLE_BYTES / BYTES_PER_REGISTER}",
                ),
            )

    override fun getModbusValues(schemaDevice: SchemaDevice) = byteArray.getModbusValues(schemaDevice)

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val bytes = byteArray.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return byteArray.getByteArray(schemaDevice)?.let { ByteConversions.bytesToDouble(it) }
    }
}
