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

import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem

class StringConcat(val expressions: List<StringExpression>) : StringExpression {

    override fun toString(): String {
        return "concat("+ expressions.joinToString(", ") + ")"
    }

    override val subExpressions: List<Expression>
        get() = expressions

    override val problems: List<Problem>
        get() =
            combine(
                "concat",
                super.problems
            )

    override fun getValue(schemaDevice: SchemaDevice): String {
        var result = ""
        for (expression in expressions) {
            result += expression.getValue(schemaDevice) ?: ""
        }
        return result
    }
}
