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

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression

class StringField(
    val fieldName: String,
) : StringExpression {

    private lateinit var field: Field
    private var fieldExpression: StringExpression = MissingField(fieldName)

    override fun toString() = toString(true)

    override fun toString(isTop: Boolean) = fieldName

    override fun initialize(containingField: Field): Boolean {
        val block = containingField.block
        val retrievedField = block.getField(fieldName)
        if (retrievedField != null) {
            field = retrievedField
        } else {
            return false
        }

        val expression = field.parsedExpression

        when (expression) {
            is StringExpression -> {
                fieldExpression = expression
                return true
            }

            is NumericalExpression -> {
                fieldExpression = StringFromNumber(expression)
                return true
            }
        }
        return false
    }

    override val subExpressions: List<Expression>
        get() = listOf(fieldExpression)

    override val requiredFields: List<String>
        get() = listOf(fieldName)

    override val requiredRegisters: List<Address>
        get() = listOf() // Fields are fetched separately !!

    override var isImmutable: Boolean
        get() = fieldExpression.isImmutable
        set(value) {
            fieldExpression.isImmutable = value
        }

    override val problems: List<Problem>
        get() =
            combine(
                "StringField",
                super.problems,
            )

    override fun getValue(schemaDevice: SchemaDevice): String? = fieldExpression.getValue(schemaDevice)
}
