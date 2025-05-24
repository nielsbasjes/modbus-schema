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

import nl.basjes.modbus.schema.utils.CodeGeneration.convertToCodeCompliantName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.junit5.JUnit5Asserter.assertEquals

internal class TestCodeGeneration {
    private fun checkConvert(
        input: String,
        expectedLower: String,
        expectedUpper: String,
    ) {
        assertEquals("Wrong lowercase initial", expectedLower, convertToCodeCompliantName(input, false))
        assertEquals("Wrong uppercase initial", expectedUpper, convertToCodeCompliantName(input, true))
    }

    @Test
    fun testCodeConverter() {
        // Edge cases
        checkConvert("", "", "")
        checkConvert("        ", "", "")
        checkConvert("  € - $ ", "", "")

        // Normal testcases
        checkConvert("   aap   ", "aap", "Aap")
        checkConvert("   aap nootMies  ", "aapNootMies", "AapNootMies")
        checkConvert("  € aap noot$€Mies  ", "aapNootMies", "AapNootMies")
        checkConvert("  aaP_noOt_MieS  ", "aaP_noOt_MieS", "AaP_noOt_MieS")
        checkConvert("  -__aaP-_noOt_-MieS__  ", "aaP_noOt_MieS", "AaP_noOt_MieS")

        // Real examples
        checkConvert("NOT_CONFIGURED", "nOT_CONFIGURED", "NOT_CONFIGURED")
        checkConvert("VRefOfs", "vRefOfs", "VRefOfs")
        checkConvert("VMax", "vMax", "VMax")
        checkConvert("VArMaxQ1", "vArMaxQ1", "VArMaxQ1")
        checkConvert("Active power (over-Excited$) rating", "activePowerOverExcitedRating", "ActivePowerOverExcitedRating")
        checkConvert("Today's Minimum Battery Voltage", "todaySMinimumBatteryVoltage", "TodaySMinimumBatteryVoltage")
        checkConvert("Data Log Daily (kWh)", "dataLogDailyKWh", "DataLogDailyKWh")
    }


    @Test
    fun testIdentifierValidation() {
        // Clean
        validIdentifier("A")
        validIdentifier("Aap")
        validIdentifier("noot42")

        // With spaces
        validIdentifier("A a p")
        validIdentifier("Mies 42")
        validIdentifier("Mies 42 Wim")

        // Bad
        inValidIdentifier("42Wim")
        inValidIdentifier("42 Wim")
    }

    private fun validIdentifier(str: String) = assertTrue(isValidIdentifier(str), "Identifier \"$str\" should be good")
    private fun inValidIdentifier(str: String) = assertFalse(isValidIdentifier(str), "Identifier \"$str\" should be bad")

}
