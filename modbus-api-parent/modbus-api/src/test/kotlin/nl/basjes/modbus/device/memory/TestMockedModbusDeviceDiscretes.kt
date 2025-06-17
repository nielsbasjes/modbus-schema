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
package nl.basjes.modbus.device.memory

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.DiscreteBlock
import nl.basjes.modbus.device.api.DiscreteValue
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TestMockedModbusDeviceDiscretes {
    @Test
    fun checkMockDeviceValuesNoSpace() {
        checkMockDeviceDiscreteValues("0001100101000101010010-xx001---1")
    }

    @Test
    fun checkMockDeviceValuesNormal() {
        checkMockDeviceDiscreteValues("0001 1001 0100 0101 0100 10-x x001 ---1")
    }

    @Test
    fun checkMockDeviceValuesSpaces() {
        checkMockDeviceDiscreteValues("00011001    0100  0 1 0 1    010010-x   x0 01 -- -1")
    }

    @Test
    fun testLoadingWithRegisterGapsAndErrors() {
        checkMockDeviceDiscreteValues("""
        0
        00
        110
        01    01
        00  0 1 0
         1    010010-x   x0 01 -

         - -1""")
    }

    private fun assertAddressValue(
        discreteBlock: DiscreteBlock,
        addressClass: AddressClass,
        addressInBlock: Int,
        expectedValue: String?,
    ) {
        if (expectedValue == null) {
            assertNull(
                discreteBlock[Address.of(addressClass, addressInBlock)].value,
                "Invalid value for discrete $addressInBlock",
            )
        } else {
            val discreteValue: DiscreteValue = discreteBlock[Address.of(addressClass, addressInBlock)]
            val actualValue = discreteValue.asString
            assertEquals(expectedValue, actualValue, "Invalid value for discrete $addressInBlock")
        }
    }

    fun checkMockDeviceDiscreteValues(discretesString: String?) {
        val addressClass = AddressClass.DISCRETE_INPUT

        val modbusDevice = builder()
            .withDiscretes(addressClass, 0, discretesString!!)
            .build()

        val discretes = modbusDevice
            .getDiscretes(Address.of(addressClass, 0), 40)

        // A single Read Error should make the entire block return a read error
        discretes
            .values
            .forEach { assertTrue { it.isReadError() } }

        assertEquals(
            40,
            discretes.size,
            "Should have only 32 registers as that is all there is in the device",
        )

        discretes.clear()
        // Now re-read them one by one
        (0 until 40).forEach {
            discretes.merge(modbusDevice
                .getDiscretes(Address.of(addressClass, it), 1))
        }

        assertAddressValue(discretes, addressClass,  0, "0")
        assertAddressValue(discretes, addressClass,  1, "0")
        assertAddressValue(discretes, addressClass,  2, "0")
        assertAddressValue(discretes, addressClass,  3, "1")
        assertAddressValue(discretes, addressClass,  4, "1")
        assertAddressValue(discretes, addressClass,  5, "0")
        assertAddressValue(discretes, addressClass,  6, "0")
        assertAddressValue(discretes, addressClass,  7, "1")
        assertAddressValue(discretes, addressClass,  8, "0")
        assertAddressValue(discretes, addressClass,  9, "1")
        assertAddressValue(discretes, addressClass, 10, "0")
        assertAddressValue(discretes, addressClass, 11, "0")
        assertAddressValue(discretes, addressClass, 12, "0")
        assertAddressValue(discretes, addressClass, 13, "1")
        assertAddressValue(discretes, addressClass, 14, "0")
        assertAddressValue(discretes, addressClass, 15, "1")
        assertAddressValue(discretes, addressClass, 16, "0")
        assertAddressValue(discretes, addressClass, 17, "1")
        assertAddressValue(discretes, addressClass, 18, "0")
        assertAddressValue(discretes, addressClass, 19, "0")
        assertAddressValue(discretes, addressClass, 20, "1")
        assertAddressValue(discretes, addressClass, 21, "0")
        assertAddressValue(discretes, addressClass, 22, "-")
        assertAddressValue(discretes, addressClass, 23, "x")
        assertAddressValue(discretes, addressClass, 24, "x")
        assertAddressValue(discretes, addressClass, 25, "0")
        assertAddressValue(discretes, addressClass, 26, "0")
        assertAddressValue(discretes, addressClass, 27, "1")
        assertAddressValue(discretes, addressClass, 28, "-")
        assertAddressValue(discretes, addressClass, 29, "-")
        assertAddressValue(discretes, addressClass, 30, "-")
        assertAddressValue(discretes, addressClass, 31, "1")

    }

}
