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

import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import java.util.Locale

open class BooleanConstant(
    val value: String,
) : BooleanExpression {

    private val theBit = when (value.lowercase(Locale.ROOT).trim()) {
        "0", "false"    -> false
        "1", "true"     -> true
        else            -> null
    }

    override fun toString(): String = when(theBit) {
        true  -> "true"
        false -> "false"
        null -> ""
    }

    override var isImmutable: Boolean = true
        set(unused) {
            field = true // Refusing to change the value
        }

    override val returnType: ReturnType
        get() = ReturnType.BOOLEAN

    override fun getBoolean(schemaDevice: SchemaDevice): Boolean? = theBit
}
