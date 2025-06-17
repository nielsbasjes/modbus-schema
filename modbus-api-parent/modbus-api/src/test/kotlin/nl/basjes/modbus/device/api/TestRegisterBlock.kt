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
package nl.basjes.modbus.device.api

import nl.basjes.modbus.device.exception.ModbusIllegalAddressClassException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestRegisterBlock {
    val logger: Logger = LogManager.getLogger()

    @Test
    fun testToHexString() {
        val registerBlock = RegisterBlock(AddressClass.HOLDING_REGISTER)
        registerBlock[Address.of("4x00005")].setValue(5.toShort(), 5L) // 0005
        registerBlock.setValue(Address.of("4x00006"), 6.toShort(), 6L) // 0006
        // 7 is absent
        registerBlock.setValue(Address.of("4x00008"), 8.toShort(), 8L) // 0008
        // 9 is absent
        // 10 is a read error
        registerBlock[Address.of("4x00010")].setSoftReadError() // 000A
        registerBlock.setValue(Address.of("4x00011"), 11.toShort(), 11L) // 000B
        // 12 is absent
        registerBlock.setValue(Address.of("4x00013"), 13.toShort(), 13L) // 000D

        val firstAddress = registerBlock.firstAddress.toString()
        val registersString = "0005 0006 ---- 0008 ---- xxxx 000B ---- 000D"

        assertEquals("hr:00004", firstAddress)
        assertEquals("0005 0006 ---- 0008 ---- xxxx 000B ---- 000D", registersString)

        // Because a RegisterBlock is an instance of Map: Log4j will handle it in a special way.
        // So the toString() here will actually give a different result !
        logger.info("As String: {}", registerBlock.toString())
    }

    @Test
    fun testInvalidCombinationsIntoRegisterBlock() {
        for (blockAddressClass in AddressClass.entries) {
            if (blockAddressClass.bitsPerValue != 16) {
                assertThrows<ModbusIllegalAddressClassException> {
                    RegisterBlock(blockAddressClass)
                }
                continue
            }
            val registerBlock = RegisterBlock(blockAddressClass)
            for (valueAddressClass in AddressClass.entries) {
                if (valueAddressClass.bitsPerValue == 1) {
                    assertThrows<ModbusIllegalAddressClassException> {
                        registerBlock.setValue(Address.of(valueAddressClass, 1), 2.toShort(), 2L)
                    }
                } else {
                    if (blockAddressClass == valueAddressClass) {
                        assertDoesNotThrow {
                            registerBlock.setValue(
                                Address.of(valueAddressClass, 1),
                                1.toShort(),
                                1L,
                            )
                        }
                    } else {
                        assertThrows<ModbusIllegalAddressClassException> {
                            registerBlock.setValue(Address.of(valueAddressClass, 1), 2.toShort(), 2L)
                        }
                    }
                }
            }
            assertEquals(1, registerBlock.size)
            assertEquals(1.toShort(), registerBlock[Address.of(blockAddressClass, 1)].value)
        }
    }

    @Test
    fun testInvalidCombinationsIntoDiscreteBlock() {
        for (blockAddressClass in AddressClass.entries) {
            if (blockAddressClass.bitsPerValue != 1) {
                assertThrows<ModbusIllegalAddressClassException> {
                    DiscreteBlock(blockAddressClass)
                }
                continue
            }
            val discreteBlock = DiscreteBlock(blockAddressClass)
            for (valueAddressClass in AddressClass.entries) {
                if (valueAddressClass.bitsPerValue == 16) {
                    assertThrows<ModbusIllegalAddressClassException> {
                        discreteBlock.setValue(Address.of(valueAddressClass, 1), true, 2L)
                    }
                } else {
                    if (blockAddressClass == valueAddressClass) {
                        assertDoesNotThrow {
                            discreteBlock.setValue(
                                Address.of(valueAddressClass, 1),
                                true,
                                1L,
                            )
                        }
                    } else {
                        assertThrows<ModbusIllegalAddressClassException> {
                            discreteBlock.setValue(Address.of(valueAddressClass, 1), true, 2L)
                        }
                    }
                }
            }
            assertEquals(1, discreteBlock.size)
            assertEquals(true, discreteBlock[Address.of(blockAddressClass, 1)].value)
        }
    }

}
