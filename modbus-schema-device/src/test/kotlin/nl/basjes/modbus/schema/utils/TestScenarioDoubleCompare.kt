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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


internal class TestScenarioDoubleCompare {
    @Test
    fun testDoubleCompareSame() {
        var value1 = 0.0000000000000000000000000001234567890123456
        var value2 = 0.0000000000000000000000000001234567896

        for (i in 0..99) {
            assertTrue(DoubleCompare.closeEnough(value1, value2), "Comparing $value1 and $value2")
            value1 *= 10.0
            value2 *= 10.0
        }
    }

    @Test
    fun testDoubleCompareDifferent() {
        var value1 = 0.0000000000000000000000000001234567890123456
        var value2 = 0.000000000000000000000000000123456

        for (i in 0..99) {
            assertFalse(DoubleCompare.closeEnough(value1, value2), "Comparing $value1 and $value2")
            value1 *= 10.0
            value2 *= 10.0
        }
    }

    @Test
    fun testDoubleCompareDifferentChecks() {
        assertFalse(DoubleCompare.closeEnough(0.0000000001234567890123456, 0.001234567890123456))
        assertFalse(DoubleCompare.closeEnough(1.0, -1.0))
        assertFalse(DoubleCompare.closeEnough(-0.000000000000001, 0.000000000000001))
        assertTrue(DoubleCompare.closeEnough(239.002, 239.001, 0.00001))
    }

    @Test
    fun testDoubleCompareSpecials() {
        assertTrue(DoubleCompare.closeEnough(Double.NaN, Double.NaN))
        assertTrue(DoubleCompare.closeEnough(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY))
        assertTrue(DoubleCompare.closeEnough(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY))
    }
}
