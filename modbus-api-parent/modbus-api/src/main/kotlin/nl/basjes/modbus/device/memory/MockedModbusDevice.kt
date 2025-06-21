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
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.api.toRegisterBlock
import nl.basjes.modbus.device.exception.createReadErrorResponse
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.TreeMap

open class MockedModbusDevice : ModbusDevice() {
    // Map AddressClass to block of registers
    private val registerBlocks: MutableMap<AddressClass, RegisterBlock> = TreeMap()

    private val logger: Logger = LogManager.getLogger()

    var logRequests = false

    override fun close() {
        // Nothing to do here
    }

    fun addRegister(
        registerValue: RegisterValue,
    ) {
        val addressClass = registerValue.address.addressClass
        registerBlocks
            .computeIfAbsent(addressClass) { RegisterBlock(addressClass) }
            .put(registerValue)
    }

    fun addRegisters(
        registerBlock: RegisterBlock,
    ) {
        val addressClass = registerBlock.addressClass
        registerBlocks
            .computeIfAbsent(addressClass) { RegisterBlock(addressClass) }
            .merge(registerBlock)
    }

    fun addRegisters(
        firstRegisterAddress: Address,
        hexRegisterValues: String,
    ): MockedModbusDevice =
        addRegisters(
            firstRegisterAddress.addressClass,
            firstRegisterAddress.physicalAddress,
            hexRegisterValues,
        )

    fun addRegisters(
        addressClass: AddressClass,
        firstPhysicalAddress: Int,
        hexRegisterValues: String,
    ): MockedModbusDevice {
        val registerBlock = hexRegisterValues.toRegisterBlock(Address(addressClass, firstPhysicalAddress))
        addRegisters(registerBlock)
        return this
    }

    override fun getRegisters(
        firstRegister: Address,
        count: Int,
    ): RegisterBlock {
//        if (logRequests) {
//            logger.info("MODBUS GET: {} # {}",firstRegister, count );
//        }
        val addressClass = firstRegister.addressClass
        val registerBlock = registerBlocks[addressClass] ?: return RegisterBlock(addressClass)
        val registers = RegisterBlock(addressClass)

        val firstPhysicalAddress = firstRegister.physicalAddress
        for (registerNumber in firstPhysicalAddress until (firstPhysicalAddress + count)) {
            val address = Address.of(addressClass, registerNumber)
            val registerValue = registerBlock[address]
            if (registerValue.isReadError()) {
                // If ANY of the requested registers is invalid then the entire block of values is returned as readerror
                // This is to match the behaviour of real devices
                logRequestResult(firstRegister, count, "READ ERROR ON ${address.toCleanFormat()}")
                return createReadErrorResponse(firstRegister, count)
            }
            if (registerValue.hasValue()) {
                registerValue.fetchTimestamp = System.currentTimeMillis()
                registers[address] = registerValue
            }
        }
        logRequestResult(firstRegister, count, registers)
        return registers
    }

    private fun logRequestResult(
        firstRegister: Address,
        count: Int,
        result: Any,
    ) {
        if (logRequests) {
            logger.info(
                "Getting {} registers starting at \"{}\" (last = \"{}\"): {}",
                count,
                firstRegister,
                // The -1 is because the count is including both the first and last registers in the list
                firstRegister.increment(count - 1),
                result,
            )
        }
    }

    class MockedModbusDeviceBuilder {
        private val mockedModbusDevice = MockedModbusDevice()

        fun withRegisters(
            addressClass: AddressClass,
            firstRegisterAddress: Int,
            hexRegisterValues: String,
        ): MockedModbusDeviceBuilder {
            mockedModbusDevice.addRegisters(addressClass, firstRegisterAddress, hexRegisterValues)
            return this
        }

        fun withRegisters(
            firstRegisterAddress: Address,
            hexRegisterValues: String,
        ): MockedModbusDeviceBuilder {
            mockedModbusDevice.addRegisters(firstRegisterAddress, hexRegisterValues)
            return this
        }

        fun withRegisters(registerBlock: RegisterBlock): MockedModbusDeviceBuilder {
            val firstAddress = registerBlock.firstAddress
            if (firstAddress != null) {
                mockedModbusDevice.addRegisters(firstAddress, registerBlock.toHexString())
            }
            return this
        }

        fun withLogging(): MockedModbusDeviceBuilder {
            mockedModbusDevice.logRequests = true
            return this
        }

        fun build(): MockedModbusDevice = mockedModbusDevice
    }

    companion object {
        /**
         * Create a new MockedModbusDevice instance
         * @param firstAddress The modbus address for the first value in the list
         * @param hexRegisterValues A space separated string of 4 hex digits for each register (like "0123 4567 89AB CDEF")
         * @return The created MockedModbusDevice instance
         */
        @JvmStatic
        fun of(
            firstAddress: Address,
            hexRegisterValues: String,
        ): MockedModbusDevice = of(firstAddress.addressClass, firstAddress.physicalAddress, hexRegisterValues)

        /**
         * Create a new MockedModbusDevice instance
         * @param addressClass The modbus function code for the provided register values
         * @param firstPhysicalAddress The modbus register number for the first value in the list
         * @param hexRegisterValues A space separated string of 4 hex digits for each register (like "0123 4567 89AB CDEF")
         * @return The created MockedModbusDevice instance
         */
        @JvmStatic
        fun of(
            addressClass: AddressClass,
            firstPhysicalAddress: Int,
            hexRegisterValues: String,
        ): MockedModbusDevice {
            val mockedDevice = MockedModbusDevice()
            mockedDevice.addRegisters(addressClass, firstPhysicalAddress, hexRegisterValues)
            return mockedDevice
        }

        @JvmStatic
        fun builder(): MockedModbusDeviceBuilder = MockedModbusDeviceBuilder()
    }
}
