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

import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions

class IEEE754Float64(
    private val byteArray: RegistersExpression,
    notImplemented: List<String>,
) : NotImplemented(nl.basjes.modbus.schema.expression.DOUBLE_BYTES / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER, notImplemented), NumericalExpression {

    override fun toString(): String {
        return "ieee754_64($byteArray)"
    }

    override val subExpressions: List<Expression>
        get() = listOf(byteArray)

    override var isImmutable: Boolean = false

    override val returnType: ReturnType
        get() = ReturnType.DOUBLE

    override val problems: List<Problem>
        get() = combine(
            "ieee754_64",
            super<NumericalExpression>.problems,
            super<NotImplemented>.problems,
            checkFatal(byteArray.returnedRegisters == nl.basjes.modbus.schema.expression.DOUBLE_BYTES / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER,
                "Wrong number of registers: Got ${byteArray.returnedRegisters}, need ${nl.basjes.modbus.schema.expression.DOUBLE_BYTES / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER}"),
        )

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> {
        return byteArray.getRegisterValues(schemaDevice)
    }

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val bytes = byteArray.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return byteArray.getByteArray(schemaDevice)?.let { ByteConversions.bytesToDouble(it) }
    }
}
