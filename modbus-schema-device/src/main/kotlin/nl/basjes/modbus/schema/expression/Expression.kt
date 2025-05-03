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
package nl.basjes.modbus.schema.expression

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException

interface Expression {

    fun toString(isTop: Boolean): String {
        return toString()
    }

    /**
     * Initialize the expression.
     * @param containingField The field of which this expression is a part
     * @return true if success, false means it must be retried.
     */
    fun initialize(containingField: Field): Boolean {
        if (subExpressions.isEmpty()) {
            return true
        }
        return subExpressions.all { it.initialize(containingField) }
    }

    val subExpressions: List<Expression>
        get() = emptyList()

    val requiredRegisters: List<Address>
        get() = subExpressions.flatMap { it.requiredRegisters } .toList()

    val requiredMutableRegisters: List<Address>
        get() = if (isImmutable) { emptyList() } else { subExpressions.flatMap { it.requiredMutableRegisters }.toList() }

    val requiredFields: List<String>
        get() = subExpressions.flatMap { it.requiredFields }.toList()

    var isImmutable: Boolean
        get() = subExpressions.all { it.isImmutable }
        set(value) { subExpressions.forEach { it.isImmutable = value } }

    val returnType: ReturnType
        get () = throw ModbusSchemaParseException("No return type specified for expression class: ${this.javaClass.name}")

    val problems: List<Problem>
        get() = subExpressions.flatMap { it.problems }

    /**
     * @return The list of Register values that are used to calculate this value.
     */
    fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> {
        return emptyList()
    }

    open class Problem(val explain:String) {
        override fun toString(): String = explain
    }

    class Warning(reason: String): Problem(reason)
    class Fatal(reason: String): Problem(reason)

    /**
     * If the condition is false the explain value is used to report a Problem
     */
    fun check(condition:Boolean, explain:String): List<Problem> =
        if (condition) {
            listOf()
        } else {
            listOf(Warning(explain))
        }

    fun checkFatal(condition:Boolean, explain:String): List<Problem> =
        if (condition) {
            listOf()
        } else {
            listOf(Fatal(explain))
        }

    fun combine(function:String, vararg problems: List<Problem>) =
        problems.flatMap { it }.map {
            when (it) {
                is Fatal -> Fatal("${function}(${it.explain})")
                is Warning -> Warning("${function}(${it.explain})")
                else -> Problem("${function}(${it.explain})")
            }
        }
}


