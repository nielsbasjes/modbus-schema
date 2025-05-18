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
package nl.basjes.modbus.schema.expression

import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee.NEGATIVE
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee.NONE
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression.ValueGuarantee.POSITIVE
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestExpressionReturnTypes {

    private fun getExpression(expression: String): NumericalExpression {
        val schemaDevice = SchemaDevice("Device")
        val block = Block(schemaDevice, "Block")
        val field = Field(block = block, id = "Field", expression = expression)
        assertTrue(field.initialize())
        val parsedExpression = field.parsedExpression
        require(parsedExpression != null)
        require(parsedExpression is NumericalExpression)
        return parsedExpression
    }

    private fun assertIsDouble(expression: String): NumericalExpression {
        val parsedExpression = getExpression(expression)
        assertNotNull(parsedExpression)
        require(parsedExpression.returnType == ReturnType.DOUBLE) {
            "The expression \"$expression\" should return a \"Double\" but returns \"${parsedExpression.javaClass.simpleName}\" instead."
        }
        return parsedExpression
    }

    private fun assertIsLong(expression: String): NumericalExpression {
        val parsedExpression = getExpression(expression)
        assertNotNull(parsedExpression)
        require(parsedExpression.returnType == ReturnType.LONG) {
            "The expression \"$expression\" should return a \"Long\" but returns \"${parsedExpression.javaClass.simpleName}\" instead."
        }
        return parsedExpression
    }

    private fun NumericalExpression.assertPositive() {
        require(this.getGuarantee() == POSITIVE) {
            "The expression \"$this\" should guarantee a positive value."
        }
    }

    private fun NumericalExpression.assertNegative() {
        require(this.getGuarantee() == NEGATIVE) {
            "The expression \"$this\" should guarantee a negative value."
        }
    }

    private fun NumericalExpression.assertNoGuarantees() {
        require(this.getGuarantee() == NONE) {
            "The expression \"$this\" should NOT guarantee a positive or negative value."
        }
    }

    @Test
    fun verifyConstants() {
        // Constant
        assertIsLong("10").assertPositive()
        assertIsLong("-10").assertNegative()
        assertIsDouble("10.0").assertPositive()
        assertIsDouble("-10.0").assertNegative()
        // Braces
        assertIsLong("(10)").assertPositive()
        assertIsLong("(-10)").assertNegative()
        assertIsDouble("(10.0)").assertPositive()
        assertIsDouble("(-10.0)").assertNegative()
    }

    @Test
    fun verifyAdd() {
        assertIsLong(" 10+ 10").assertPositive()
        assertIsLong(" 10+-10").assertNoGuarantees()
        assertIsLong("-10+ 10").assertNoGuarantees()
        assertIsLong("-10+-10").assertNegative()

        assertIsDouble(" 10.0+ 10").assertPositive()
        assertIsDouble(" 10.0+-10").assertNoGuarantees()
        assertIsDouble("-10.0+ 10").assertNoGuarantees()
        assertIsDouble("-10.0+-10").assertNegative()

        assertIsDouble(" 10+ 10.0").assertPositive()
        assertIsDouble(" 10+-10.0").assertNoGuarantees()
        assertIsDouble("-10+ 10.0").assertNoGuarantees()
        assertIsDouble("-10+-10.0").assertNegative()

        assertIsDouble(" 10.0+ 10.0").assertPositive()
        assertIsDouble(" 10.0+-10.0").assertNoGuarantees()
        assertIsDouble("-10.0+ 10.0").assertNoGuarantees()
        assertIsDouble("-10.0+-10.0").assertNegative()
    }

    @Test
    fun verifySubtract() {
        assertIsLong(" 10- 10").assertNoGuarantees()
        assertIsLong(" 10--10").assertPositive()
        assertIsLong("-10- 10").assertNegative()
        assertIsLong("-10--10").assertNoGuarantees()

        assertIsDouble(" 10.0- 10").assertNoGuarantees()
        assertIsDouble(" 10.0--10").assertPositive()
        assertIsDouble("-10.0- 10").assertNegative()
        assertIsDouble("-10.0--10").assertNoGuarantees()

        assertIsDouble(" 10- 10.0").assertNoGuarantees()
        assertIsDouble(" 10--10.0").assertPositive()
        assertIsDouble("-10- 10.0").assertNegative()
        assertIsDouble("-10--10.0").assertNoGuarantees()

        assertIsDouble(" 10.0- 10.0").assertNoGuarantees()
        assertIsDouble(" 10.0--10.0").assertPositive()
        assertIsDouble("-10.0- 10.0").assertNegative()
        assertIsDouble("-10.0--10.0").assertNoGuarantees()
    }

    @Test
    fun verifyMultiply() {
        assertIsLong(" 10* 10").assertPositive()
        assertIsLong(" 10*-10").assertNegative()
        assertIsLong("-10* 10").assertNegative()
        assertIsLong("-10*-10").assertPositive()

        assertIsDouble(" 10.0* 10").assertPositive()
        assertIsDouble(" 10.0*-10").assertNegative()
        assertIsDouble("-10.0* 10").assertNegative()
        assertIsDouble("-10.0*-10").assertPositive()

        assertIsDouble(" 10* 10.0").assertPositive()
        assertIsDouble(" 10*-10.0").assertNegative()
        assertIsDouble("-10* 10.0").assertNegative()
        assertIsDouble("-10*-10.0").assertPositive()

        assertIsDouble(" 10.0* 10.0").assertPositive()
        assertIsDouble(" 10.0*-10.0").assertNegative()
        assertIsDouble("-10.0* 10.0").assertNegative()
        assertIsDouble("-10.0*-10.0").assertPositive()
    }

    @Test
    fun verifyDivide() {
        assertIsDouble(" 10/ 10").assertPositive()
        assertIsDouble(" 10/-10").assertNegative()
        assertIsDouble("-10/ 10").assertNegative()
        assertIsDouble("-10/-10").assertPositive()

        assertIsDouble(" 10.0/ 10").assertPositive()
        assertIsDouble(" 10.0/-10").assertNegative()
        assertIsDouble("-10.0/ 10").assertNegative()
        assertIsDouble("-10.0/-10").assertPositive()

        assertIsDouble(" 10/ 10.0").assertPositive()
        assertIsDouble(" 10/-10.0").assertNegative()
        assertIsDouble("-10/ 10.0").assertNegative()
        assertIsDouble("-10/-10.0").assertPositive()

        assertIsDouble(" 10.0/ 10.0").assertPositive()
        assertIsDouble(" 10.0/-10.0").assertNegative()
        assertIsDouble("-10.0/ 10.0").assertNegative()
        assertIsDouble("-10.0/-10.0").assertPositive()
    }

    @Test
    fun verifyPower() {
        assertIsLong(" 10^ 10").assertPositive()
        assertIsDouble(" 10^-10").assertPositive()
        assertIsLong("-10^ 10").assertNoGuarantees()
        assertIsDouble("-10^-10").assertNoGuarantees()

        assertIsDouble(" 10.0^ 10").assertPositive()
        assertIsDouble(" 10.0^-10").assertPositive()
        assertIsDouble("-10.0^ 10").assertNoGuarantees()
        assertIsDouble("-10.0^-10").assertNoGuarantees()

        assertIsDouble(" 10^ 10.0").assertPositive()
        assertIsDouble(" 10^-10.0").assertPositive()
        assertIsDouble("-10^ 10.0").assertNoGuarantees()
        assertIsDouble("-10^-10.0").assertNoGuarantees()

        assertIsDouble(" 10.0^ 10.0").assertPositive()
        assertIsDouble(" 10.0^-10.0").assertPositive()
        assertIsDouble("-10.0^ 10.0").assertNoGuarantees()
        assertIsDouble("-10.0^-10.0").assertNoGuarantees()
    }

    @Test
    fun verifyNumberRetrieval() {
        assertIsLong("int16(hr:0)").assertNoGuarantees()
        assertIsLong("int32(hr:0#2)").assertNoGuarantees()
        assertIsLong("int64(hr:0#4)").assertNoGuarantees()
        assertIsLong("uint16(hr:0)").assertPositive()
        assertIsLong("uint32(hr:0#2)").assertPositive()
        assertIsLong("uint64(hr:0#4)").assertPositive()

        assertIsDouble("ieee754_32(hr:0#2)").assertNoGuarantees()
        assertIsDouble("ieee754_64(hr:0#4)").assertNoGuarantees()
    }
}
