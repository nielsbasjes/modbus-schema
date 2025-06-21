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

import nl.basjes.modbus.schema.expression.Expression
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TestImmutableFieldOptimizer {

    private val log = LoggerFactory.getLogger(TestImmutableFieldOptimizer::class.java)

    @OptIn(ExperimentalTime::class)
    @Test
    fun buildImmutableFields() {
        val modbusDevice = createTestModbusDevice()
        val schemaDevice = createTestSchemaDevice()
        schemaDevice.connect(modbusDevice)
        schemaDevice.needAll()

        val block = schemaDevice.getBlock("Block1")
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

        val fieldValue1Time = fieldValue1.valueEpochMs ?.let { Instant.fromEpochMilliseconds(it).toString() } ?: "<Immutable>"
        log.info("Field 1: {} @ {}", fieldValue1.doubleValue, fieldValue1Time)
        fieldValue1.parsedExpression?.let { printRawExpression(it) }

        val fieldValue2Time = fieldValue1.valueEpochMs ?.let { Instant.fromEpochMilliseconds(it).toString() } ?: "<Immutable>"
        log.info("Field 2: {} @ {}", fieldValue2.doubleValue, fieldValue2Time)
        fieldValue2.parsedExpression?.let { printRawExpression(it) }

        require(fieldValue2.requiredFields.isNotEmpty()) { "Missing required fields" }
    }

    private fun printRawExpression(
        expression: Expression,
        depth: Int = 0,
    ) {
        log.info("  ".repeat(depth) + "- name: " + expression.javaClass.simpleName)
        log.info("  ".repeat(depth) + "  immu: " + expression.isImmutable)
        expression.subExpressions.forEach {
            printRawExpression(it, depth + 1)
        }
    }
}
