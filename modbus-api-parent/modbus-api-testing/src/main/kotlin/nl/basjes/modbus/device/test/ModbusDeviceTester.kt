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
        (1..100).forEach {
            val address = Address.of(addressClass, it)
            val value = RegisterValue(address)
            value.setValue(it.toShort())
            input.put(value)
        }
        return input
    }

    private fun createDiscretesBlock(addressClass: AddressClass): DiscreteBlock {
        val input = DiscreteBlock(addressClass)
        (1..100).forEach {
            val address = Address.of(addressClass, it)
            val value = DiscreteValue(address)
            value.setValue(it % 2 == 0)
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
            log.info("Modbus Test server: Starting")
            ModbusTestServer(42).use { testServer ->
                log.info("Modbus Test server: Running on localhost port ${testServer.port} with unitId ${testServer.unitId}")
                try {
                    log.info("Modbus Client: Creating")
                    modbusDeviceCreator("localhost", testServer.port, testServer.unitId)
                        .use { modbusDevice ->
                            requireNotNull(modbusDevice)
                            log.info("Modbus Client: Created")
                            testServer.loadModbusBlocks(listOf(inputBlock))

                            val firstAddress =
                                inputBlock.firstAddress ?: throw IllegalStateException("First address not set")
                            val size = inputBlock.size
                            log.info("Modbus Client: Retrieving block {}#{}", firstAddress, size)
                            try {
                                val retrievedBlock = blockRetriever(modbusDevice, firstAddress, size)
                                log.info("Modbus Client: Retrieved block: {}", retrievedBlock)
                                assertEquals(inputBlock, retrievedBlock)
                            } catch (e: ModbusException) {
                                log.fatal("Modbus Client: Failed with: " + e.message, e)
                                throw e
                            }
                        }
                } finally {
                    log.info("Modbus Client: Stopped")
                }
            }
        }
        finally {
            log.info("Modbus Test server: Stopped")
        }
    }

}
