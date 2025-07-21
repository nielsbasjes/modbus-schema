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
import nl.basjes.modbus.schema.expression.booleans.BooleanExpression

class StringFromBoolean(
    val booleanExpression: BooleanExpression,
    val zeroString: String,
    val oneString: String,
) : StringExpression {

    override fun toString(): String = "boolean( $booleanExpression ; '$zeroString' ; '$oneString' )"

    override val subExpressions: List<Expression>
        get() = listOf(booleanExpression)

    override fun getValue(schemaDevice: SchemaDevice): String =
        when (booleanExpression.returnType) {
            ReturnType.BOOLEAN ->
                when (booleanExpression.getBoolean(schemaDevice)) {
                    null -> ""
                    false -> zeroString
                    true  -> oneString
                }

            else ->
                error("Should never have a BooleanExpression that returns a ${booleanExpression.returnType}")
        }

    override val problems: List<Problem>
        get() =
            combine(
                "StringFromBoolean",
                super.problems,
            )
}
