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
package nl.basjes.modbus.device.j2mod

import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster
import com.ghgande.j2mod.modbus.procimg.InputRegister
import com.ghgande.j2mod.modbus.procimg.Register
import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.FunctionCode.Companion.forReading
import nl.basjes.modbus.device.api.FunctionCode.READ_COIL
import nl.basjes.modbus.device.api.FunctionCode.READ_DISCRETE_INPUT
import nl.basjes.modbus.device.api.FunctionCode.READ_HOLDING_REGISTERS
import nl.basjes.modbus.device.api.FunctionCode.READ_INPUT_REGISTERS
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.exception.NotYetImplementedException
import com.ghgande.j2mod.modbus.ModbusException as J2ModModbusException

/**
 * An instance of a ModbusDevice that uses the J2Mod library for the Modbus connection
 */
class ModbusDeviceJ2Mod(
    /**
     * The connected instance of the ModbusMaster that is to be used for the connection.
     */
    private val master: AbstractModbusMaster,
    /**
     * The Modbus unit id of the device that we want to connect to.
     */
    private val unitId: Int,
) : ModbusDevice() {

    init {
        require(master.isConnected) { "The provided master must be connected" }

        // https://ipc2u.com/articles/knowledge-base/modbus-rtu-made-simple-with-detailed-descriptions-and-examples
        // SlaveID is the address of the device, it can take a value from 0 to 247, addresses from 248 to 255 are reserved.
        require(!(unitId < 0 || unitId > 247)) { "The unitId is outside the allowed range [0-247]: $unitId" }
    }

    override fun close() {
        master.disconnect()
    }

    @Throws(ModbusException::class)
    override fun getRegisters(firstRegister: Address, count: Int): RegisterBlock {
        when (val functionCode = forReading(firstRegister.addressClass)) {
            READ_COIL,
            READ_DISCRETE_INPUT ->
                throw NotYetImplementedException("Reading a ${firstRegister.addressClass} has not yet been implemented")

            READ_HOLDING_REGISTERS -> {
                    try {
                        val registers: Array<Register> = master
                            .readMultipleRegisters(unitId, firstRegister.physicalAddress, count)
                        return buildRegisterBlock(firstRegister, registers)
                    } catch (e: J2ModModbusException) {
                        throw ModbusException(
                            "For " + functionCode + " & " + firstRegister.physicalAddress + ":" + e.message,
                            e,
                        )
                    }
                }

            READ_INPUT_REGISTERS -> {
                    try {
                        val registers: Array<InputRegister> = master
                            .readInputRegisters(unitId, firstRegister.physicalAddress, count)
                        return buildRegisterBlock(firstRegister, registers)
                    } catch (e: J2ModModbusException) {
                        throw ModbusException(
                            "For " + functionCode + " & " + firstRegister.physicalAddress + ":" + e.message,
                            e,
                        )
                    }
                }

            else ->
                throw NotYetImplementedException("The function code $functionCode for ${firstRegister.addressClass} has not yet been implemented")
        }
    }

    private fun buildRegisterBlock(firstAddress: Address, registers: Array<out InputRegister>): RegisterBlock {
        // Record all received values under the current timestamp.
        // Many devices have a bad clock.
        val now = System.currentTimeMillis()

        var loopRegisterAddress = firstAddress
        val result = RegisterBlock(firstAddress.addressClass)
        for (register in registers) {
            result[loopRegisterAddress] =
                RegisterValue(loopRegisterAddress).setValue((register.value and 0xFFFF).toShort(), now)
            loopRegisterAddress = loopRegisterAddress.increment(1)
        }
        return result
    }
}
