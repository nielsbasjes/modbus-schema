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
package nl.basjes.modbus.schema

import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.fetcher.TestScenarioRegisterBlockFetcher.AssertingMockedModbusDevice
import nl.basjes.modbus.schema.utils.DoubleCompare
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestImmutableFieldOptimizer {

    private val log = LoggerFactory.getLogger(TestImmutableFieldOptimizer::class.java)

    private fun createTestModbusDevice(): AssertingMockedModbusDevice {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addRegisters(
            AddressClass.HOLDING_REGISTER, 101,
            """
            # abcdefghijklmnopqrstuvwxyz
            6162 6364 6566 6768 696a 6b6c 6d6e 6f70 7172 7374 7576 7778 797a
            # 0123456789
            3031 3233 3435 3637 3839
            # A deliberate gap without data
            ---- ----
            # ABCDEFGHIJKLMNOPQRSTUVWXYZ
            4142 4344 4546 4748 494a 4b4c 4d4e 4f50 5152 5354 5556 5758 595a
            # 0123456789
            3031 3233 3435 3637 3839
            # A deliberate gap with bad data (666) at deadAddress
            029A 029A
            3039    # The number 12345
            000A    # The number 10

            2B67    // The number 11111
            """
        )
        return modbusDevice
    }

    private fun createTestSchemaDevice(): SchemaDevice {
        val schemaDevice = SchemaDevice("First test device")

        val block = Block(schemaDevice,"Block", "Block")

        // Field names are chosen to have a different logical (by required registers) from a sorted by name ordering.
        // A field registers itself with the mentioned Block
        Field(block, "Some",    expression = "utf8( hr:101 # 13)")
        Field(block, "Field",   expression = "utf8( hr:114 # 5)",           immutable = true)
        Field(block, "Value1",  expression = "int16( hr:141 ) / Scale1")
        Field(block, "Scale1",  expression = "int16( hr:142 ) ",            immutable = true)
        Field(block, "Value2",  expression = "int16( hr:143 ) / Scale2",    immutable = true)
        Field(block, "Scale2",  expression = "100")

        schemaDevice.initialize()
        return schemaDevice
    }

    private fun assertCloseEnough(value1: Double?, value2: Double?) {
        assertNotNull(value1)
        assertNotNull(value2)
        assertTrue(
            DoubleCompare.closeEnough(value1, value2),
            "Values $value1 and $value2  are not close enough together."
        )
    }


    @Test
    fun buildImmutableFields() {
        val schemaDevice = createTestSchemaDevice()
        val modbusDevice = createTestModbusDevice()
        schemaDevice.connect(modbusDevice)
        schemaDevice.needAll()

        val block = schemaDevice.getBlock("Block")
        require(block != null) { "Oops" }

        val fieldSome   = block.getField("Some")   ?: throw AssertionError()
        val fieldField  = block.getField("Field")  ?: throw AssertionError()
        val fieldValue1 = block.getField("Value1") ?: throw AssertionError()
        val fieldScale1 = block.getField("Scale1") ?: throw AssertionError()
        val fieldValue2 = block.getField("Value2") ?: throw AssertionError()
        val fieldScale2 = block.getField("Scale2") ?: throw AssertionError()

        schemaDevice.updateAll()

        assertEquals("abcdefghijklmnopqrstuvwxyz", fieldSome.stringValue)
        assertEquals("0123456789",                 fieldField.stringValue)
        assertCloseEnough(1234.5,                  fieldValue1.doubleValue)
        assertEquals(10,                           fieldScale1.longValue)
        assertCloseEnough(111.11,                  fieldValue2.doubleValue)
        assertEquals(100,                          fieldScale2.longValue)

        fieldValue1.parsedExpression?.let { printRawExpression(it) }
        fieldValue2.parsedExpression?.let { printRawExpression(it) }

        require(fieldValue2.requiredFields.isNotEmpty()) { "Missing required fields" }
    }

    private fun printRawExpression(expression: Expression, depth:Int = 0) {
        log.info("  ".repeat(depth) + "- name: " + expression.javaClass.simpleName)
        log.info("  ".repeat(depth) + "  immu: " + expression.isImmutable)
        expression.subExpressions.forEach {
            printRawExpression(it, depth+1)
        }
    }


}
