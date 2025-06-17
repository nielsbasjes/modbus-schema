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
package nl.basjes.modbus.device.plc4j

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
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
import nl.basjes.modbus.device.exception.createReadErrorRegisterBlock
import org.apache.plc4x.java.api.PlcConnection
import org.apache.plc4x.java.api.PlcDriverManager
import org.apache.plc4x.java.api.exceptions.PlcConnectionException
import org.apache.plc4x.java.api.exceptions.PlcRuntimeException
import org.apache.plc4x.java.api.messages.PlcReadResponse
import org.apache.plc4x.java.api.types.PlcResponseCode
import org.apache.plc4x.java.modbus.base.tag.ModbusTag
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * An instance of a ModbusDevice that uses the Apache Plc4J library for the Modbus connection
 */
class ModbusDevicePlc4j(
    /**
     * The Plc4J specific connect string for the desired modbus device
     */
    connectionString: String,
) : ModbusDevice() {
    private val connection: PlcConnection

    @Throws(ModbusException::class)
    override fun close() {
        try {
            connection.close()
        } catch (e: Exception) {
            throw ModbusException(e.message ?: "No message in exception", e)
        }
    }

    init {
        try {
            connection = PlcDriverManager.getDefault().connectionManager.getConnection(connectionString)
        } catch (e: PlcConnectionException) {
            throw ModbusException("Unable to connect to the master", e)
        }

        if (!connection.isConnected) {
            throw ModbusException("Unable to connect")
        }

        // Check if this connection support reading of data.
        if (!connection.metadata.isReadSupported) {
            throw ModbusException("This connection doesn't support reading.")
        }
    }

    private fun getAddressClassTag(addressClass: AddressClass): String = addressClass.longLabel

    @Throws(ModbusException::class)
    override fun getRegisters(
        firstRegister: Address,
        count: Int,
    ): RegisterBlock {
        when (val functionCode = forReading(firstRegister.addressClass)) {
            READ_COIL,
            READ_DISCRETE_INPUT,
            -> {
                throw NotYetImplementedException("Reading a " + firstRegister.addressClass + " has not yet been implemented")
            }

            READ_HOLDING_REGISTERS,
            READ_INPUT_REGISTERS,
            -> {
                val builder = connection.readRequestBuilder()

                // This is REALLY fragile ! If you add the correct type (like WORD) you only get a single value.
                val fieldTag =
                    String.format(
                        "%s:%05d[%d]",
                        getAddressClassTag(firstRegister.addressClass),
                        firstRegister.registerNumber,
                        count,
                    )
                builder.addTag("F", ModbusTag.of(fieldTag))

                val asyncResponse = builder.build().execute()

                // Wait for completion
                val response: PlcReadResponse
                try {
                    response = asyncResponse[2, TimeUnit.SECONDS]
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                } catch (e: ExecutionException) {
                    throw RuntimeException(e)
                } catch (e: TimeoutException) {
                    throw RuntimeException(e)
                }

                // Record all received values under the current timestamp.
                // Many devices have a bad clock.
                val now = System.currentTimeMillis()

                when (response.getResponseCode("F")) {
                    PlcResponseCode.OK,
                    -> {
                        // We're cool
                    }

                    PlcResponseCode.NOT_FOUND,
                    PlcResponseCode.ACCESS_DENIED,
                    PlcResponseCode.INVALID_ADDRESS,
                    PlcResponseCode.INVALID_DATATYPE,
                    PlcResponseCode.INVALID_DATA,
                    PlcResponseCode.INTERNAL_ERROR,
                    PlcResponseCode.REMOTE_BUSY,
                    PlcResponseCode.REMOTE_ERROR,
                    PlcResponseCode.UNSUPPORTED,
                    PlcResponseCode.RESPONSE_PENDING,
                    -> {
                        return createReadErrorRegisterBlock(firstRegister, count)
                    }
                }

                var address = firstRegister
                val result = RegisterBlock(address.addressClass)
                try {
                    val allShorts = response.getAllIntegers("F")
                    for (value in allShorts) {
                        result[address] = RegisterValue(address).setValue(value.toShort(), now)
                        address = address.increment(1)
                    }
                } catch (e: PlcRuntimeException) {
                    throw ModbusException("Got a PlcRuntimeException (" + e.message + ") on " + fieldTag, e)
                }
                return result
            }

            else -> {
                throw NotYetImplementedException(
                    "The function code $functionCode for ${firstRegister.addressClass} has not yet been implemented",
                )
            }
        }
    }
}
