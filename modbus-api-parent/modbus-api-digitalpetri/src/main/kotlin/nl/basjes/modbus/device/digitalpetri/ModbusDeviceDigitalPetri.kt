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
package nl.basjes.modbus.device.digitalpetri

import com.digitalpetri.modbus.client.ModbusClient
import com.digitalpetri.modbus.exceptions.ModbusException as DPModbusException
import com.digitalpetri.modbus.exceptions.ModbusResponseException as DPModbusResponseException
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest
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
import nl.basjes.modbus.device.exception.createReadErrorResponse

/**
 * An instance of a ModbusDevice that uses the DigitalPetri library for the Modbus connection
 */
class ModbusDeviceDigitalPetri(
    /**
     * The connected instance of the ModbusClient that is to be used for the connection.
     */
    private val client: ModbusClient,
    /**
     * The Modbus unit id of the device that we want to connect to.
     */
    private val unitId: Int,
) : ModbusDevice() {

    init {
        require(client.isConnected) { "The provided client must be connected" }

        // https://ipc2u.com/articles/knowledge-base/modbus-rtu-made-simple-with-detailed-descriptions-and-examples
        // SlaveID is the address of the device, it can take a value from 0 to 247, addresses from 248 to 255 are reserved.
        require(!(unitId < 0 || unitId > 247)) { "The unitId is outside the allowed range [0-247]: $unitId" }
    }

    override fun close() {
        client.disconnect()
    }

    @Throws(ModbusException::class)
    override fun getRegisters(firstRegister: Address, count: Int): RegisterBlock {
        when (val functionCode = forReading(firstRegister.addressClass)) {
            READ_COIL,
            READ_DISCRETE_INPUT ->
                throw NotYetImplementedException("Reading a ${firstRegister.addressClass} has not yet been implemented")

            READ_HOLDING_REGISTERS -> {
                    try {
                        val response = client.readHoldingRegisters(
                            unitId,
                            ReadHoldingRegistersRequest(firstRegister.physicalAddress, count)
                        )
                        return buildRegisterBlock(firstRegister, response.registers)
                    } catch (_: DPModbusResponseException) {
                        return createReadErrorResponse(firstRegister, count)
                    } catch (e: DPModbusException) {
                        throw ModbusException(
                            "For " + functionCode + " & " + firstRegister.physicalAddress + ":" + e.message,
                            e,
                        )
                    }
                }

            READ_INPUT_REGISTERS -> {
                    try {
                        val response = client.readInputRegisters(
                            unitId,
                            ReadInputRegistersRequest(firstRegister.physicalAddress, count)
                        )
                        return buildRegisterBlock(firstRegister, response.registers)
                    } catch (_: DPModbusResponseException) {
                        return createReadErrorResponse(firstRegister, count)
                    } catch (e: com.digitalpetri.modbus.exceptions.ModbusException) {
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

    private fun buildRegisterBlock(firstAddress: Address, bytes: ByteArray?): RegisterBlock {
        // Record all received values under the current timestamp.
        // Many devices have a bad clock.
        val now = System.currentTimeMillis()

        var loopRegisterAddress = firstAddress
        val result = RegisterBlock(firstAddress.addressClass)

        if (bytes == null) {
            return result
        }
        if (bytes.size % 2 != 0) {
            throw ModbusException("Received an odd number of bytes (${bytes.size}) for the registers")
        }

        for (registerNr in 0 until bytes.size/2) {
            val byte0: Int = bytes[registerNr*2].toInt()
            val byte1: Int = bytes[(registerNr*2)+1].toInt()
            val register:Short =
                (
                    (
                        ((byte0 shl 8) and 0xFF00)
                            or
                        (byte1 and 0x00FF)
                    ) and 0xFFFF
                ).toShort()

            result[loopRegisterAddress] =
                RegisterValue(loopRegisterAddress).setValue(register, now)
            loopRegisterAddress = loopRegisterAddress.increment(1)
        }
        return result
    }
}
