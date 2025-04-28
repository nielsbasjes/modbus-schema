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
package nl.basjes.modbus.schema.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestScenarioDoubleToString {
    @Test
    fun testOfFloat() {
        // Note the rounding errors!
        assertEquals("1.234568", DoubleToString.of(1.23456789f))
        assertEquals("12.34568", DoubleToString.of(12.3456789f))
        assertEquals("123.4568", DoubleToString.of(123.456789f))
        assertEquals("1234.568", DoubleToString.of(1234.56789f))
        assertEquals("12345.68", DoubleToString.of(12345.6789f))
        assertEquals("123456.8", DoubleToString.of(123456.789f))
        assertEquals("1234567.9", DoubleToString.of(1234567.89f))
        assertEquals("12345679", DoubleToString.of(12345678.9f))
        assertEquals("123456792", DoubleToString.of(123456789f))

        LOG.info("239.30 --> {}", DoubleToString.of(239.300000002f))

        var foo = 0.0000123450000001f
        for (i in 0..9) {
            LOG.info("{}", String.format("FLOAT: %2d : %30f --> %s", i, foo, DoubleToString.of(foo)))
            foo *= 10.0f
        }
    }

    @Test
    fun testOfDouble() {
        assertEquals("1.23456789", DoubleToString.of(1.23456789))
        assertEquals("12.3456789", DoubleToString.of(12.3456789))
        assertEquals("123.456789", DoubleToString.of(123.456789))
        assertEquals("1234.56789", DoubleToString.of(1234.56789))
        assertEquals("12345.6789", DoubleToString.of(12345.6789))
        assertEquals("123456.789", DoubleToString.of(123456.789))
        assertEquals("1234567.89", DoubleToString.of(1234567.89))
        assertEquals("12345678.9", DoubleToString.of(12345678.9))
        assertEquals("123456789", DoubleToString.of(123456789.0))

        LOG.info("239.30 --> {}", DoubleToString.of(239.300000002))

        var foo = 1.234500000000001
        for (i in 0..19) {
            foo *= 10.0
            LOG.info("{}", String.format("DOUBLE: %2d : %30f --> %s", i, foo, DoubleToString.of(foo)))
        }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()
    }
}
