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

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.strings.MissingField

class NumericalField(
    val fieldName: String,
) : NumericalExpression {

    private lateinit var field: Field
    private var fieldExpression: NumericalExpression = MissingField(fieldName)

    override fun toString(): String = fieldName

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
            is NumericalExpression -> {
                fieldExpression = expression
                return true
            }
        }
        return false
    }

    override val subExpressions: List<Expression>
        get() = listOf(fieldExpression)

    override val requiredRegisters: List<Address>
        get() = listOf() // Fields are fetched separately !!

    override val requiredFields: List<String>
        get() = listOf(fieldName)

    override var isImmutable: Boolean
        get() = fieldExpression.isImmutable
        set(value) {
            fieldExpression.isImmutable = value
        }

    override val returnType: ReturnType
        get() = fieldExpression.returnType

    override val problems: List<Problem>
        get() =
            combine(
                "NumericalField",
                super.problems,
            )

    @Throws(ModbusException::class)
    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? = fieldExpression.getValueAsDouble(schemaDevice)

    @Throws(ModbusException::class)
    override fun getValueAsLong(schemaDevice: SchemaDevice): Long? = fieldExpression.getValueAsLong(schemaDevice)
}
