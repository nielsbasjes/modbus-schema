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

import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression

class StringFromNumber(val numericalExpression: NumericalExpression): StringExpression {
    override fun toString(): String {
        return "$numericalExpression"
    }

    override val subExpressions: List<Expression>
        get() = listOf(numericalExpression)

    override fun getValue(schemaDevice: SchemaDevice): String =
        when(numericalExpression.returnType) {
            ReturnType.DOUBLE ->     (numericalExpression.getValueAsDouble(schemaDevice) ?: "").toString()
            ReturnType.LONG ->       (numericalExpression.getValueAsLong(schemaDevice)   ?: "").toString()
            ReturnType.UNKNOWN ->     error("Should never have a NumericalExpression that returns a UNKNOWN")
            ReturnType.BOOLEAN ->     error("Should never have a NumericalExpression that returns a BOOLEAN")
            ReturnType.STRING ->      error("Should never have a NumericalExpression that returns a STRING")
            ReturnType.STRINGLIST ->  error("Should never have a NumericalExpression that returns a STRINGLIST")
        }

    override val problems: List<Problem>
        get() = combine(
            "StringFromNumber",
            super.problems,
        )

}
