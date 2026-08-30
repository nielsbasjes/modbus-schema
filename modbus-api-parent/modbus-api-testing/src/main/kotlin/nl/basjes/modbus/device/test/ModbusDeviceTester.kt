/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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
package nl.basjes.modbus.device.test

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.AddressClass.COIL
import nl.basjes.modbus.device.api.AddressClass.DISCRETE_INPUT
import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.AddressClass.INPUT_REGISTER
import nl.basjes.modbus.device.api.DiscreteBlock
import nl.basjes.modbus.device.api.DiscreteValue
import nl.basjes.modbus.device.api.ModbusBlock
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import org.apache.logging.log4j.LogManager
import kotlin.test.assertEquals

private val log = LogManager.getLogger(ModbusDeviceTester::class.java)

class ModbusDeviceTester(
    val modbusDeviceCreator: (hostname: String, port: Int, unitId: Int) -> ModbusDevice?,
) {

    private fun createRegisterBlock(addressClass: AddressClass): RegisterBlock {
        val input = RegisterBlock(addressClass)
        (0..99).forEach {
            val address = Address.of(addressClass, it)
            val value = RegisterValue(address)
            value.setValue(it.toShort())
            input.put(value)
        }
        return input
    }


    // This bit pattern makes several types of alignment issues visible (off by 1, off by 2, etc.).
    val bitPattern = listOf(
        true,
        false,
        true,  true,
        false, false,
        true,  true,  true,
        false, false, false,
        true,  true,  true,  true,
        false, false, false, false,
    )
    private fun createDiscretesBlock(addressClass: AddressClass): DiscreteBlock {
        val input = DiscreteBlock(addressClass)
        (0..99).forEach {
            val address = Address.of(addressClass, it)
            val value = DiscreteValue(address)
            value.setValue(bitPattern[it % bitPattern.size])
            input.put(value)
        }
        return input
    }

    fun testInputRegisters() {
        testRegisters(createRegisterBlock(INPUT_REGISTER))
    }

    fun testHoldingRegisters() {
        testRegisters(createRegisterBlock(HOLDING_REGISTER))
    }

    fun testCoils() {
        testDiscretes(createDiscretesBlock(COIL))
    }
    fun testDiscreteInputs() {
        testDiscretes(createDiscretesBlock(DISCRETE_INPUT))
    }

    private fun testRegisters(inputRegisterBlock: RegisterBlock) {
        testModbusRetrieval(inputRegisterBlock) {
                modbusDevice, firstAddress, size -> modbusDevice.getRegisters(firstAddress, size)
        }
    }

    private fun testDiscretes(inputDiscretesBlock: DiscreteBlock) {
        testModbusRetrieval(inputDiscretesBlock) {
            modbusDevice, firstAddress, size -> modbusDevice.getDiscretes(firstAddress, size)
        }
    }

    private fun testModbusRetrieval(inputBlock: ModbusBlock<*,*,*>, blockRetriever: (modbusDevice: ModbusDevice, firstAddress: Address, size: Int) -> ModbusBlock<*,*,*>,
    ) {
        try {
            log.info("Modbus Test slave: Starting")
            ModbusTestServer(42).use { testSlave ->
                log.info("Modbus Test slave: Running on localhost port ${testSlave.port} with unitId ${testSlave.unitId}")
                try {
                    log.info("ModbusDevice master: Creating")
                    modbusDeviceCreator("localhost", testSlave.port, testSlave.unitId)
                        .use { modbusDevice ->
                            requireNotNull(modbusDevice)
                            log.info("ModbusDevice master: Created ({})", modbusDevice.javaClass.simpleName)

                            log.info("Modbus Test slave: Loading data")
                            testSlave.loadModbusBlocks(listOf(inputBlock))

                            val firstAddress =
                                inputBlock.firstAddress ?: throw IllegalStateException("First address not set")
                            val size = inputBlock.size
                            try {
                                log.info("ModbusDevice master: Trying block: {}#{}", firstAddress, size)
                                var retrievedBlock = blockRetriever(modbusDevice, firstAddress, size)
                                assertBlock(inputBlock, retrievedBlock, firstAddress, size)

                                (1..10).forEach { size ->
                                    log.info("ModbusDevice master: Trying blocks of {} registers.", size)
                                    (0 until inputBlock.size-size).forEach {
                                    val address = firstAddress + it
                                        retrievedBlock = blockRetriever(modbusDevice, address, size)
                                        assertBlock(inputBlock, retrievedBlock, address, size)
                                    }
                                }

                            } catch (e: ModbusException) {
                                log.fatal("ModbusDevice master: Failed with: " + e.message, e)
                                throw e
                            }
                        }
                } finally {
                    log.info("ModbusDevice master: Stopped")
                }
            }
        }
        finally {
            log.info("Modbus Test slave: Stopped")
        }
    }


    private fun assertBlock(expectedBlock: ModbusBlock<*,*,*>, actualBlock: ModbusBlock<*,*,*>, address: Address, size: Int) {
        assertEquals(size, actualBlock.size, "Actual block has the wrong size")
        if (expectedBlock.size == size) {
            assertEquals(
                expectedBlock, actualBlock,
                """
                    Retrieved $address#$size and got mismatch:
                    - expectedBlock  = $expectedBlock
                    - retrievedBlock = $actualBlock
                """.trimIndent()
            )
            return
        }
        (0 until size).forEach { offset ->
            val checkAddress = address + offset
            assertEquals(
                expectedBlock[checkAddress], actualBlock[checkAddress],
                """
                    Retrieved $address#$size and got mismatch:
                    - expectedBlock [$checkAddress] = ${expectedBlock[checkAddress]}
                    - retrievedBlock[$checkAddress] = ${actualBlock[checkAddress]}

                    => expectedBlock  = $expectedBlock
                    => retrievedBlock = $actualBlock
                """.trimIndent()
            )
        }
    }
}
