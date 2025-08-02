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

import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import nl.basjes.modbus.schema.expression.parser.ExpressionParser.Companion.parse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TestScenarioExpressionParser {
    private fun assertEqualExpression(
        expression: String,
        expected: String,
    ) {
        val parsed = parse(expression)
        assertEquals(expected, parsed.toString(), "Mismatch in resulting expression")
        // Now parse the output and do it again.
        assertEquals(expected, parse(parsed.toString()).toString(), "Mismatch in re-parsed expression")
    }

    private fun assertInvalidAddressInExpression(expression: String) {
        val parsed: Expression
        try {
            parsed = parse(expression)
        } catch (exception: Exception) {
            assertTrue(exception is IllegalArgumentException || exception is ModbusSchemaParseException)
            return
        }
        assertFalse(parsed.problems.isEmpty())
    }

    @Test
    fun testNumbers() {
        assertEqualExpression("123", "123")
    }

    @Test
    fun testMathOperations() {
        assertEqualExpression("123+456", "123+456")
        assertEqualExpression("123-456", "123-456")
        assertEqualExpression("123*456", "123*456")
        assertEqualExpression("123/456", "123/456") // Division becomes a floating point number
        assertEqualExpression("123^456", "123^456")
    }

    @Test
    fun testOperationsOrdering() {
        assertEqualExpression("123+456-789", "(123+456)-789")
        assertEqualExpression("123-456+789", "(123-456)+789")

        // https://youtu.be/GnE9b__GX1E
        // Multiplication and division are equal and must be calculated in the order they appear.
        assertEqualExpression("  12 / 3 * 4 ", "(12/3)*4")
        assertEqualExpression("  4 * 3 / 12 ", "(4*3)/12")

        // Verify if the parse tree is built correctly (i.e. right operator prioritization)
        assertEqualExpression("12+34^2/56-78*90^3", "(12+((34^2)/56))-(78*(90^3))")
    }

    @Test
    fun testRegisterFormats() {
        // Clean format
        assertEqualExpression("int16(coil:123)", "int16(c:00123)")
        assertEqualExpression("int16(c:123)", "int16(c:00123)")

        // Format as used by plc4j
        assertEqualExpression("int16(0x123)", "int16(c:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(0x0123)", "int16(c:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(0x00123)", "int16(c:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(0x000123)", "int16(c:00122)") // NOTE: The old formats are 1 off !

        // Traditional modicon (max 9999 registers)
        assertEqualExpression("int16(00123)", "int16(c:00122)") // NOTE: The old formats are 1 off !
        // Better modicon (max 65536 registers)
        assertEqualExpression("int16(000123)", "int16(c:00122)") // NOTE: The old formats are 1 off !

        // Same for all others:
        // discrete-input
        assertEqualExpression("int16(discrete-input:123)", "int16(di:00123)")
        assertEqualExpression("int16(di:123)", "int16(di:00123)")
        assertEqualExpression("int16(1x123)", "int16(di:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(1x0123)", "int16(di:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(1x00123)", "int16(di:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(1x000123)", "int16(di:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(10123)", "int16(di:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(100123)", "int16(di:00122)") // NOTE: The old formats are 1 off !

        // input-register
        assertEqualExpression("int16(input-register:123)", "int16(ir:00123)")
        assertEqualExpression("int16(ir:123)", "int16(ir:00123)")
        assertEqualExpression("int16(3x123)", "int16(ir:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(3x0123)", "int16(ir:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(3x00123)", "int16(ir:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(3x000123)", "int16(ir:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(30123)", "int16(ir:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(300123)", "int16(ir:00122)") // NOTE: The old formats are 1 off !

        // holding-register
        assertEqualExpression("int16(holding-register:123)", "int16(hr:00123)")
        assertEqualExpression("int16(hr:123)", "int16(hr:00123)")
        assertEqualExpression("int16(4x123)", "int16(hr:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(4x0123)", "int16(hr:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(4x00123)", "int16(hr:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(4x000123)", "int16(hr:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(40123)", "int16(hr:00122)") // NOTE: The old formats are 1 off !
        assertEqualExpression("int16(400123)", "int16(hr:00122)") // NOTE: The old formats are 1 off !
    }

    @Test
    fun testRegisterFormatsInvalid() {
        // Coil
        assertInvalidAddressInExpression("int16(coil:66666)")
        assertInvalidAddressInExpression("int16(c:66666)")
        assertInvalidAddressInExpression("int16(0x66666)")
        assertInvalidAddressInExpression("int16(0x066666)")
        assertInvalidAddressInExpression("int16(00000)")
        assertInvalidAddressInExpression("int16(000000)")
        assertInvalidAddressInExpression("int16(066666)")

        // discrete-input
        assertInvalidAddressInExpression("int16(discrete-input:66666)")
        assertInvalidAddressInExpression("int16(di:66666)")
        assertInvalidAddressInExpression("int16(1x66666)")
        assertInvalidAddressInExpression("int16(1x066666)")
        assertInvalidAddressInExpression("int16(10000)")
        assertInvalidAddressInExpression("int16(100000)")
        assertInvalidAddressInExpression("int16(166666)")

        // input-register
        assertInvalidAddressInExpression("int16(input-register:66666)")
        assertInvalidAddressInExpression("int16(ir:66666)")
        assertInvalidAddressInExpression("int16(3x66666)")
        assertInvalidAddressInExpression("int16(3x066666)")
        assertInvalidAddressInExpression("int16(30000)")
        assertInvalidAddressInExpression("int16(300000)")
        assertInvalidAddressInExpression("int16(366666)")

        // holding-register
        assertInvalidAddressInExpression("int16(holding-register:66666)")
        assertInvalidAddressInExpression("int16(hr:66666)")
        assertInvalidAddressInExpression("int16(4x66666)")
        assertInvalidAddressInExpression("int16(4x066666)")
        assertInvalidAddressInExpression("int16(40000)")
        assertInvalidAddressInExpression("int16(400000)")
        assertInvalidAddressInExpression("int16(466666)")
    }

    @Test
    fun testRegisterLists() {
        // Valid range definitions
        assertEqualExpression("int64(coil:123 , c:124  , c:125 , c:126)", "int64(c:00123 # 4)")
        assertEqualExpression("int64(coil:123 .. c:126)", "int64(c:00123 # 4)")
        assertEqualExpression("int64(coil:123 # 4 )", "int64(c:00123 # 4)")

        // Explicit range mixing formats
        // Note the seemingly missing "125" --> The old formats at the end are 1 off !
        assertEqualExpression("int64(coil:123             , c:124  , 0x00126 , 00127)", "int64(c:00123 # 4)")
        assertEqualExpression("int64(discrete-input:123   , di:124 , 1x00126 , 10127)", "int64(di:00123 # 4)")
        assertEqualExpression("int64(input-register:123   , ir:124 , 3x00126 , 30127)", "int64(ir:00123 # 4)")
        assertEqualExpression("int64(holding-register:123 , hr:124 , 4x00126 , 40127)", "int64(hr:00123 # 4)")

        // Note the seemingly missing "125" --> The old formats at the end are 1 off !
        assertEqualExpression("int64(coil:123             .. 0x000127)", "int64(c:00123 # 4)")
        assertEqualExpression("int64(discrete-input:123   .. 1x000127)", "int64(di:00123 # 4)")
        assertEqualExpression("int64(input-register:123   .. 3x000127)", "int64(ir:00123 # 4)")
        assertEqualExpression("int64(holding-register:123 .. 4x000127)", "int64(hr:00123 # 4)")
    }

    @Test
    fun testRegisterListsInvalid() {
        // Reversed range ?!?!
        assertInvalidAddressInExpression("int64(coil:126 .. c:123)")

        // Zero/Negative number of registers
        assertInvalidAddressInExpression("int64(coil:123 # 0 )")
        assertInvalidAddressInExpression("int64(coil:123 # -1 )")
    }

    @Test
    fun testRegisterExpressions() {
        assertEqualExpression("int16(hr:123)", "int16(hr:00123)")
        assertEqualExpression("int16(hr:123 # 1)", "int16(hr:00123)")
        assertEqualExpression("int16(hr:123 # 3)", "int16(hr:00123 # 3)")
        assertEqualExpression("int16(4x00124)", "int16(hr:00123)")
        assertEqualExpression("int16(4x00124 # 1)", "int16(hr:00123)")
        assertEqualExpression("int16(4x00124 # 3)", "int16(hr:00123 # 3)")

        assertEqualExpression("int16(hr:1 , hr:2 , hr:3)", "int16(hr:00001 # 3)")
        assertEqualExpression("int16(hr:1 .. hr:3)", "int16(hr:00001 # 3)")
        assertEqualExpression("int16(hr:1 .. 4x00004)", "int16(hr:00001 # 3)")

        assertEqualExpression("int16(hr:1, holding-register:000002, 4x00004)", "int16(hr:00001 # 3)")

        // Out of order !!
        assertEqualExpression("int16(hr:3 , hr:2 , hr:1)", "int16(hr:00003, hr:00002, hr:00001)")
    }

    @Test
    fun testRealisticCase() {
        // This is the kind of expression we see in SunSpec
        assertEqualExpression(
            "ieee754_32(ir:3 # 2) * 10 ^ scalingFactor",
            "ieee754_32(ir:00003 # 2)*(10^scalingFactor)",
        )
        assertEqualExpression(
            "ieee754_32(3x00004 # 2) * 10 ^ scalingFactor",
            "ieee754_32(ir:00003 # 2)*(10^scalingFactor)",
        )
    }
}
