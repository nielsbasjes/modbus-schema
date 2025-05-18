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

import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression

interface NumericalExpression : Expression {
    /**
     * @return The Double value or null if it was not implemented
     */
    fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val valueAsLong = getValueAsLong(schemaDevice) ?: return null
        return valueAsLong * 1.0
    }

    /**
     * @return The Long value or null if it was not implemented
     */
    fun getValueAsLong(schemaDevice: SchemaDevice): Long? = null

    /**
     * @return Can we guarantee anything about the return value?
     */
    fun getGuarantee(): ValueGuarantee = ValueGuarantee.NONE

    enum class ValueGuarantee {
        NONE, // There are no guarantees given
        POSITIVE, // Value is always 0 or higher
        NEGATIVE, // Value is always negative
    }
}
