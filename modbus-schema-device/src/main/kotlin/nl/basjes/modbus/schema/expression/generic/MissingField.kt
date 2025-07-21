/*
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
package nl.basjes.modbus.schema.expression.generic

import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.ReturnType.UNKNOWN
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.booleans.BooleanExpression
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression
import nl.basjes.modbus.schema.expression.strings.StringExpression

class MissingField(
    private val fieldName: String,
) : StringExpression,
    NumericalExpression,
    BooleanExpression {

    override fun toString(): String = "<<MISSING FIELD: $fieldName>>"

    override var isImmutable: Boolean = true

    override val problems: List<Expression.Problem>
        get() = listOf(Expression.Warning("Field $fieldName is missing"))

    override fun getValue(schemaDevice: SchemaDevice) = null
    override fun getBoolean(schemaDevice: SchemaDevice) = null
    override fun getValueAsDouble(schemaDevice: SchemaDevice) = null
    override fun getValueAsLong(schemaDevice: SchemaDevice) = null

    override val returnType: ReturnType
        get() = UNKNOWN
}
