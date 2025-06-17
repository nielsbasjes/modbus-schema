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
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions
import java.util.BitSet
import kotlin.collections.isEmpty

class BooleanBit(
    private val addresses: BooleanExpression,
) : BooleanExpression {

    override fun toString(): String =
        "boolean($addresses)"

    override val subExpressions: List<Expression>
        get() = listOf(addresses)

    override val returnType: ReturnType
        get() = ReturnType.BOOLEAN

    override var isImmutable: Boolean
        get() = addresses.isImmutable
        set(value) {
            addresses.isImmutable = value
        }

    override val problems: List<Problem>
        get() =
            combine(
                "boolean",
                super<BooleanExpression>.problems,
            )

    override fun getModbusValues(schemaDevice: SchemaDevice) = addresses.getModbusValues(schemaDevice)

    override fun getBoolean(schemaDevice: SchemaDevice) = addresses.getBoolean(schemaDevice)

}
