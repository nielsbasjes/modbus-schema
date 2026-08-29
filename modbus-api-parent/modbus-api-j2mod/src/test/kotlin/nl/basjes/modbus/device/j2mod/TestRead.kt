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
package nl.basjes.modbus.device.j2mod

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.test.ModbusDeviceTester
import kotlin.test.Test

internal class TestRead {
    val deviceTester = ModbusDeviceTester { modbusHost, modbusPort, modbusUnit ->
        val modbusMaster = ModbusTCPMaster(modbusHost, modbusPort)
        modbusMaster.connect()
        ModbusDeviceJ2Mod(modbusMaster, modbusUnit)
    }

    @Test
    @Throws(ModbusException::class)
    fun readCoils() {
        deviceTester.testCoils()
    }

    @Test
    @Throws(ModbusException::class)
    fun readDiscreteInputs() {
        deviceTester.testDiscreteInputs()
    }

    @Test
    @Throws(ModbusException::class)
    fun readInputRegisters() {
        deviceTester.testInputRegisters()
    }

    @Test
    @Throws(ModbusException::class)
    fun readHoldingRegisters() {
        deviceTester.testHoldingRegisters()
    }

}
