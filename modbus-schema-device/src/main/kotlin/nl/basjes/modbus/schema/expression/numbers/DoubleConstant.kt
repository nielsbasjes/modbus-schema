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
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee
import nl.basjes.modbus.schema.utils.DoubleToString

class DoubleConstant(
    val value: Double,
) : NumericalExpression {

    override fun toString(): String = DoubleToString.of(value)

    override val problems: List<Problem>
        get() =
            combine(
                "DoubleConstant",
                super.problems,
                checkFatal(value.isFinite(), "Not a finite number"),
            )

    override var isImmutable: Boolean = true
        set(unused) {
            field = true // Refusing to change the value
        }

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double = value

    override fun getGuarantee(): ValueGuarantee =
        when {
            value >= 0 -> ValueGuarantee.POSITIVE
            value < 0  -> ValueGuarantee.NEGATIVE
            else       -> ValueGuarantee.NONE
        }

    override val returnType: ReturnType
        get() = ReturnType.DOUBLE
}
