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
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TestMockedModbusDevice {
    @Test
    @Throws(ModbusException::class)
    fun checkMockDeviceValuesNormal() {
        checkMockDeviceValues("F001 E002 D003 C004 B005 A006 9007 8008 7009 600A 500B 400C 300D 200E 100F 0000 FFFF")
    }

    @Test
    @Throws(ModbusException::class)
    fun checkMockDeviceValuesSpaces() {
        checkMockDeviceValues(" F001   E002  D003   C004   B005 A006 9007 8008 7009 600A 500B 400C 300D 200E 100F 0000  FFFF  ")
    }

    private fun assertAddressValue(
        registerBlock: RegisterBlock,
        addressClass: AddressClass,
        registerNumber: Int,
        expectedHexValue: String?
    ) {
        if (expectedHexValue == null) {
            assertNull(
                registerBlock[Address.of(addressClass, registerNumber)].value,
                "Invalid value for register $registerNumber"
            )
        } else {
            val registerValue: RegisterValue = registerBlock[Address.of(addressClass, registerNumber)]
            val actualValue = registerValue.hexValue
            assertEquals(expectedHexValue, actualValue, "Invalid value for register $registerNumber")
        }
    }

    @Throws(ModbusException::class)
    fun checkMockDeviceValues(registerString: String?) {
        val addressClass = AddressClass.INPUT_REGISTER

        builder()
            .withRegisters(addressClass, 0, registerString!!)
            .build().use { device ->
                val registers = device.getRegisters(Address.of(addressClass, 0), 20)
                assertEquals(
                    17,
                    registers.size,
                    "Should have only 17 registers as that is all there is in the device"
                )

                assertAddressValue(registers, addressClass, 0, "F001")
                assertAddressValue(registers, addressClass, 1, "E002")
                assertAddressValue(registers, addressClass, 2, "D003")
                assertAddressValue(registers, addressClass, 3, "C004")
                assertAddressValue(registers, addressClass, 4, "B005")
                assertAddressValue(registers, addressClass, 5, "A006")
                assertAddressValue(registers, addressClass, 6, "9007")
                assertAddressValue(registers, addressClass, 7, "8008")
                assertAddressValue(registers, addressClass, 8, "7009")
                assertAddressValue(registers, addressClass, 9, "600A")
                assertAddressValue(registers, addressClass, 10, "500B")
                assertAddressValue(registers, addressClass, 11, "400C")
                assertAddressValue(registers, addressClass, 12, "300D")
                assertAddressValue(registers, addressClass, 13, "200E")
                assertAddressValue(registers, addressClass, 14, "100F")
                assertAddressValue(registers, addressClass, 15, "0000")
                assertAddressValue(registers, addressClass, 16, "FFFF")
            }
    }

    @Test
    fun testLoadingWithRegisterGaps() {
        val registerString =
            """
                0001   ----   0003
                null    0005
                0006 ----   0008  null
                null   000B  null   000D
            """

        val addressClass = AddressClass.INPUT_REGISTER

        try {
            builder()
                .withRegisters(addressClass, 0, registerString)
                .build().use { device ->
                    val registers = device.getRegisters(Address.of(addressClass, 0), 20)
                    assertEquals(
                        7,
                        registers.size,
                        "Should have 7 registers as that is all there is in the device"
                    )

                    assertAddressValue(registers, addressClass, 0, "0001")
                    assertAddressValue(registers, addressClass, 1, null)
                    assertAddressValue(registers, addressClass, 2, "0003")
                    assertAddressValue(registers, addressClass, 3, null)
                    assertAddressValue(registers, addressClass, 4, "0005")
                    assertAddressValue(registers, addressClass, 5, "0006")
                    assertAddressValue(registers, addressClass, 6, null)
                    assertAddressValue(registers, addressClass, 7, "0008")
                    assertAddressValue(registers, addressClass, 8, null)
                    assertAddressValue(registers, addressClass, 9, null)
                    assertAddressValue(registers, addressClass, 10, "000B")
                    assertAddressValue(registers, addressClass, 11, null)
                    assertAddressValue(registers, addressClass, 12, "000D")
                    assertAddressValue(registers, addressClass, 13, null)
                    assertAddressValue(registers, addressClass, 14, null)
                }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
