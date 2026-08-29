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

import com.ghgande.j2mod.modbus.procimg.SimpleDigitalIn
import com.ghgande.j2mod.modbus.procimg.SimpleDigitalOut
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import com.ghgande.j2mod.modbus.slave.ModbusSlave
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory
import nl.basjes.modbus.device.api.AddressClass.COIL
import nl.basjes.modbus.device.api.AddressClass.DISCRETE_INPUT
import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.AddressClass.INPUT_REGISTER
import nl.basjes.modbus.device.api.DiscreteBlock
import nl.basjes.modbus.device.api.DiscreteValue
import nl.basjes.modbus.device.api.ModbusBlock
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.memory.MockedModbusDevice
import java.net.ServerSocket

private const val DEFAULT_UNIT_ID = 1
private const val THREAD_POOL_SIZE = 4

/**
 * A small in-process Modbus TCP server for integration tests.
 *
 * The server exposes the supplied RegisterBlocks and DiscreteBlocks
 *
 * The server listens only on localhost and uses an automatically
 * allocated ephemeral TCP port.
 */
class ModbusTestServer(
    val unitId: Int = DEFAULT_UNIT_ID,
) : MockedModbusDevice() {

    /**
     * The TCP port on which the test server is listening.
     */
    val port: Int

    private fun findFreePort(): Int =
        ServerSocket(0).use { it.localPort }

    private val slave: ModbusSlave

    private val processImage: SimpleProcessImage

    init {
        require(unitId in 0..255) {
            "Invalid Modbus unit ID: $unitId"
        }
        port = findFreePort()

        processImage = SimpleProcessImage()
        slave = ModbusSlaveFactory.createTCPSlave(
            port,
            THREAD_POOL_SIZE,
        )

        slave.addProcessImage(unitId, processImage)
        slave.open()
    }

    fun loadModbusBlocks(modbusBlocks: List<ModbusBlock<*,*,*>>) {
        modbusBlocks.forEach {
            when(it) {
                is DiscreteBlock -> addDiscreteBlock(processImage, it)
                is RegisterBlock -> addRegisterBlock(processImage, it)
            }
        }
    }


    override fun addDiscretes(discreteValue: DiscreteValue) {
        super.addDiscretes(discreteValue)
        val discreteBlock = DiscreteBlock(discreteValue.address.addressClass)
        discreteBlock.put(discreteValue)
        addDiscreteBlock(processImage, discreteBlock)
    }

    override fun addDiscretes(discreteBlock: DiscreteBlock) {
        super.addDiscretes(discreteBlock)
        addDiscreteBlock(processImage, discreteBlock)
    }

    override fun addRegister(registerValue: RegisterValue) {
        super.addRegister(registerValue)
        val registerBlock = RegisterBlock(registerValue.address.addressClass)
        registerBlock.put(registerValue)
        addRegisterBlock(processImage, registerBlock)
    }

    override fun addRegisters(registerBlock: RegisterBlock) {
        super.addRegisters(registerBlock)
        addRegisterBlock(processImage, registerBlock)
    }

    private fun addRegisterBlock(
        processImage: SimpleProcessImage,
        block: RegisterBlock,
    ) {
        when (block.addressClass) {
            HOLDING_REGISTER -> {
                block.values.forEach { value ->
                    val registerValue = value.value
                    if (registerValue != null) {
                        processImage.addRegister(
                            value.address.physicalAddress,
                            SimpleRegister(registerValue.toInt()),
                        )
                    }
                }
            }

            INPUT_REGISTER -> {
                block.values.forEach { value ->
                    val registerValue = value.value
                    if (registerValue != null) {
                        processImage.addInputRegister(
                            value.address.physicalAddress,
                            SimpleInputRegister(registerValue.toInt()),
                        )
                    }
                }
            }

            else -> {
                error(
                    "RegisterBlock has unsupported address class: " +
                        block.addressClass
                )
            }
        }
    }

    private fun addDiscreteBlock(
        processImage: SimpleProcessImage,
        block: DiscreteBlock,
    ) {
        when (block.addressClass) {
            COIL -> {
                block.values.forEach { value ->
                    val discreteValue = value.value
                    if (discreteValue != null) {
                        processImage.addDigitalOut(
                            value.address.physicalAddress,
                            SimpleDigitalOut(discreteValue),
                        )
                    }
                }
            }

            DISCRETE_INPUT -> {
                block.values.forEach { value ->
                    val discreteValue = value.value
                    if (discreteValue != null) {
                        processImage.addDigitalIn(
                            value.address.physicalAddress,
                            SimpleDigitalIn(discreteValue),
                        )
                    }
                }
            }

            else -> error(
                "DiscreteBlock has unsupported address class: " +
                    block.addressClass
            )
        }
    }

    override fun close() {
        slave.close()
    }
}
