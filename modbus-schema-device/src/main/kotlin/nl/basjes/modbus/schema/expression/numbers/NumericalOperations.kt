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

import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee.NEGATIVE
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee.NONE
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee.POSITIVE
import kotlin.math.pow

abstract class SubExpression(
    protected val name: String,
    val left: NumericalExpression,
    val right: NumericalExpression,
) : NumericalExpression {

    abstract val operatorSymbol: String

    override fun toString() = toString(true)

    override fun toString(isTop: Boolean): String {
        return if (isTop) {
            left.toString(false) + operatorSymbol + right.toString(false)
        } else {
            "(" + toString(true) + ")"
        }
    }

    override val subExpressions: List<Expression>
        get() = listOf(left, right)

    override val problems: List<Problem>
        get() = combine(name, left.problems, right.problems)

    override fun getRegisterValues(schemaDevice: SchemaDevice): List<RegisterValue> {
        val registerValues = ArrayList<RegisterValue>()
        registerValues.addAll(left.getRegisterValues(schemaDevice))
        registerValues.addAll(right.getRegisterValues(schemaDevice))
        return registerValues
    }
}

// ----------------------------------------------------------------------

class Add(left: NumericalExpression, right: NumericalExpression) : SubExpression("Add", left, right) {
    override val operatorSymbol: String
        get() = "+"

    override fun getGuarantee(): ValueGuarantee {
        val leftGuarantee = left.getGuarantee()
        val rightGuarantee = right.getGuarantee()
        return when {
            leftGuarantee == POSITIVE && rightGuarantee == POSITIVE -> POSITIVE
            leftGuarantee == NEGATIVE && rightGuarantee == NEGATIVE -> NEGATIVE
            else -> NONE
        }
    }

    override fun getValueAsLong(schemaDevice: SchemaDevice): Long? {
        val left  = left.getValueAsLong(schemaDevice)  ?: return null
        val right = right.getValueAsLong(schemaDevice) ?: return null
        return left + right
    }

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val left  = getValidatedDouble(schemaDevice, left)  ?: return null
        val right = getValidatedDouble(schemaDevice, right) ?: return null
        return left + right
    }

    override val returnType: ReturnType
        get() =
            if (left.returnType == ReturnType.LONG && right.returnType == ReturnType.LONG) {
                ReturnType.LONG
            } else {
                ReturnType.DOUBLE
            }

}

// ----------------------------------------------------------------------

class Subtract(left: NumericalExpression, right: NumericalExpression) : SubExpression("Subtract", left, right) {
    override val operatorSymbol: String
        get() = "-"

    override fun getGuarantee(): ValueGuarantee {
        val leftGuarantee = left.getGuarantee()
        val rightGuarantee = right.getGuarantee()
        return when {
            leftGuarantee == POSITIVE && rightGuarantee == NEGATIVE -> POSITIVE
            leftGuarantee == NEGATIVE && rightGuarantee == POSITIVE -> NEGATIVE
            else -> NONE
        }
    }

    override fun getValueAsLong(schemaDevice: SchemaDevice): Long? {
        val left  = left.getValueAsLong(schemaDevice)  ?: return null
        val right = right.getValueAsLong(schemaDevice) ?: return null
        return left - right
    }

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val left  = getValidatedDouble(schemaDevice, left)  ?: return null
        val right = getValidatedDouble(schemaDevice, right) ?: return null
        return left - right
    }

    override val returnType: ReturnType
        get() =
            if (left.returnType == ReturnType.LONG && right.returnType == ReturnType.LONG) {
                ReturnType.LONG
            } else {
                ReturnType.DOUBLE
            }
}

// ----------------------------------------------------------------------

class Multiply(left: NumericalExpression, right: NumericalExpression) : SubExpression("Multiply", left, right) {
    override val operatorSymbol: String
        get() = "*"

    override fun getGuarantee(): ValueGuarantee {
        val leftGuarantee = left.getGuarantee()
        val rightGuarantee = right.getGuarantee()
        return when {
            leftGuarantee == POSITIVE && rightGuarantee == POSITIVE -> POSITIVE
            leftGuarantee == NEGATIVE && rightGuarantee == POSITIVE -> NEGATIVE
            leftGuarantee == POSITIVE && rightGuarantee == NEGATIVE -> NEGATIVE
            leftGuarantee == NEGATIVE && rightGuarantee == NEGATIVE -> POSITIVE
            else -> NONE
        }
    }

    override fun getValueAsLong(schemaDevice: SchemaDevice): Long? {
        val left  = left.getValueAsLong(schemaDevice)  ?: return null
        val right = right.getValueAsLong(schemaDevice) ?: return null
        return left * right
    }

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val left  = getValidatedDouble(schemaDevice, left)  ?: return null
        val right = getValidatedDouble(schemaDevice, right) ?: return null
        return left * right
    }

    override val returnType: ReturnType
        get() =
            if (left.returnType == ReturnType.LONG && right.returnType == ReturnType.LONG) {
                ReturnType.LONG
            } else {
                ReturnType.DOUBLE
            }
}

// ----------------------------------------------------------------------

class Divide(left: NumericalExpression, right: NumericalExpression) : SubExpression("Divide", left, right) {
    val dividend: NumericalExpression
        get() = left

    val divisor: NumericalExpression
        get() = right

    override val operatorSymbol: String
        get() = "/"

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val dividend = getValidatedDouble(schemaDevice, dividend) ?: return null
        val divisor  = getValidatedDouble(schemaDevice, divisor)  ?: return null
        return dividend / divisor
    }

    override fun getGuarantee(): ValueGuarantee {
        val leftGuarantee = left.getGuarantee()
        val rightGuarantee = right.getGuarantee()
        return when {
            leftGuarantee == POSITIVE && rightGuarantee == POSITIVE -> POSITIVE
            leftGuarantee == POSITIVE && rightGuarantee == NEGATIVE -> NEGATIVE
            leftGuarantee == NEGATIVE && rightGuarantee == POSITIVE -> NEGATIVE
            leftGuarantee == NEGATIVE && rightGuarantee == NEGATIVE -> POSITIVE
            else -> NONE
        }
    }

    override val returnType: ReturnType
        get() = ReturnType.DOUBLE
}

// ----------------------------------------------------------------------

class Power(left: NumericalExpression, right: NumericalExpression) : SubExpression("Power", left, right) {
    val base: NumericalExpression
        get() = left

    val exponent: NumericalExpression
        get() = right

    override val operatorSymbol: String
        get() = "^"

    override fun getValueAsLong(schemaDevice: SchemaDevice): Long? {
        val base     = base.getValueAsLong(schemaDevice)     ?: return null
        val exponent = exponent.getValueAsLong(schemaDevice) ?: return null

        // There is no library function for doing exponent on longs in Kotlin
        return (base.toDouble()).pow(exponent.toDouble()).toLong()
    }

    override fun getValueAsDouble(schemaDevice: SchemaDevice): Double? {
        val base     = getValidatedDouble(schemaDevice, base)     ?: return null
        val exponent = getValidatedDouble(schemaDevice, exponent) ?: return null
        return base.pow(exponent)
    }

    override fun getGuarantee(): ValueGuarantee {
        val leftGuarantee = left.getGuarantee()
        val rightGuarantee = right.getGuarantee()
        return when {
            leftGuarantee == POSITIVE && rightGuarantee == POSITIVE -> POSITIVE
            leftGuarantee == POSITIVE && rightGuarantee == NEGATIVE -> POSITIVE
            else -> NONE
        }
    }

    override val returnType: ReturnType
        get() =
            if (left.returnType == ReturnType.LONG && right.returnType == ReturnType.LONG && right.getGuarantee()==POSITIVE) {
                ReturnType.LONG
            } else {
                ReturnType.DOUBLE
            }
}

// ---------------------------------------------------------------------

private fun getValidatedDouble(schemaDevice: SchemaDevice, expression: NumericalExpression): Double? {
    val value = expression.getValueAsDouble(schemaDevice)
    if (value == null || value.isNaN() || value.isInfinite()) {
        return null
    }
    return value
}
