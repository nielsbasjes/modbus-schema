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
package nl.basjes.modbus.device.testcases.sunspec

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.MODBUS_MAX_REGISTERS_PER_REQUEST
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

private const val SUNSPEC_MODEL_HEADER_SIZE = 2

/** The list of starting addresses where the SunSpec Model chain can begin. */
private val sunSpecStandardStartPhysicalAddress =
    arrayOf(
        Address(HOLDING_REGISTER, 0),
        Address(HOLDING_REGISTER, 40000),
        Address(HOLDING_REGISTER, 50000),
    )

const val SUNSPEC_STANDARD_UNITID: Int = 126

/**
 * This class is for TESTING PURPOSES ONLY.
 * It simply walks the SunSpec model chain and prints some information.
 * This is useful when testing a modbus api implementation.
 */
class SunSpecBasicsPrinter(
    private val modbusDevice: ModbusDevice,
) {
    private val sunSpecChainHeaderSize = 2

    fun print(onlyModelHeaders: Boolean = true) {
        val sunSpecModels: MutableList<SunSpecModelHeader> = ArrayList()
        var sunSpecFirstModelAddress: Address? = null

        val fullDump = RegisterBlock(HOLDING_REGISTER)

        for (sunSpecChainStartAddress in sunSpecStandardStartPhysicalAddress) {
            LOG.info("Looking for SunSpec header at {}", sunSpecChainStartAddress)

            // Read and verify the 'SunS' header.
            val registers = modbusDevice.getRegisters(sunSpecChainStartAddress, 2)
            val registerValues = registers.values.toTypedArray<RegisterValue>()

            fullDump.merge(registers)
            if (registerValues.size >= 2 &&
                // 'S' 'u'
                0x5375.toShort() == registerValues[0].value &&
                // 'n' 'S'
                0x6E53.toShort() == registerValues[1].value
            ) {
                sunSpecFirstModelAddress = sunSpecChainStartAddress.increment(sunSpecChainHeaderSize)
                LOG.info("Found the SunSpec header at {}", sunSpecChainStartAddress)
                break // Found it.
            }
        }

        require(sunSpecFirstModelAddress != null) { "Unable to locate the SunSpec address" }

        var modelAddress: Address = sunSpecFirstModelAddress
        var modelId: Int
        var sunSpecModelHeader: SunSpecModelHeader
        do {
            sunSpecModelHeader = SunSpecModelHeader(modbusDevice, modelAddress)
            sunSpecModels.add(sunSpecModelHeader)
            LOG.info("Model: {}", sunSpecModelHeader)
            modelAddress = modelAddress.increment(SUNSPEC_MODEL_HEADER_SIZE + sunSpecModelHeader.modelSize)
            modelId = sunSpecModelHeader.modelId
        } while (modelId != -1 && modelId != 0) // Apparently some devices do this incorrectly https://github.com/sunspec/models/issues/44

        if (onlyModelHeaders) {
            return
        }

        // The last one is the terminating list element which is not a model
        sunSpecModels.remove(sunSpecModelHeader)

        for (sunSpecModel in sunSpecModels) {
            LOG.info("--- LOADING: {}", sunSpecModel)
            sunSpecModel.loadRegisters(modbusDevice)
            fullDump.merge(sunSpecModel.header)
            fullDump.merge(sunSpecModel.registers)
            LOG.info("+++ SUCCESS: {}", sunSpecModel)
        }

        LOG.info("FullDump: {}", fullDump)

        val values: Collection<RegisterValue> = fullDump.values
        require(values.isNotEmpty())
        var dumpAddress = values.first().address
        for (value in values) {
            while (dumpAddress != value.address) {
                val unknownValue = RegisterValue(dumpAddress).setValue(0xFFFF.toShort(), 0L)
                LOG.info("{} -------", unknownValue)
                dumpAddress = dumpAddress.increment()
            }
            LOG.info("{}", value)
            dumpAddress = dumpAddress.increment()
        }
    }

    private class SunSpecModelHeader(
        device: ModbusDevice,
        address: Address,
    ) {
        // The first useful data address (i.e. the next one AFTER the modelId and modelSize)
        val modelAddress: Address

        // The SunSpec model ID
        val modelId: Int

        // The number of 16 bit modbus registers of useful data in this model.
        val modelSize: Int

        // The actual registers values of the model
        val header: RegisterBlock

        // The actual registers values of the model
        val registers: RegisterBlock

        init {
            val modelHeader = device.getRegisters(address, SUNSPEC_MODEL_HEADER_SIZE)
            val modelHeaderValues = modelHeader.values.toTypedArray<RegisterValue>()
            modelId = modelHeaderValues[0].value!!.toInt()
            modelSize = modelHeaderValues[1].value!!.toInt()
            header = RegisterBlock(address.addressClass)
            registers = RegisterBlock(address.addressClass)
            header.merge(modelHeader)
            modelAddress = address.increment(SUNSPEC_MODEL_HEADER_SIZE)
        }

        @Throws(ModbusException::class)
        fun loadRegisters(device: ModbusDevice) {
            var readAddress = modelAddress
            var toRead = modelSize
            val readSize = MODBUS_MAX_REGISTERS_PER_REQUEST
            while (toRead > MODBUS_MAX_REGISTERS_PER_REQUEST) {
                registers.merge(device.getRegisters(readAddress, readSize))
                toRead -= readSize
                readAddress = readAddress.increment(readSize)
            }
            registers.merge(device.getRegisters(readAddress, toRead))
        }

        override fun toString(): String =
            "SunSpecModel{" + modelAddress +
                ", Id=" + modelId +
                ", Size=" + modelSize +
                ", registers= [" + registers.values.joinToString(", ") { "0x" + it.asString } + "]" +
                '}'
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger("SunSpec TEST Printer")
    }
}
