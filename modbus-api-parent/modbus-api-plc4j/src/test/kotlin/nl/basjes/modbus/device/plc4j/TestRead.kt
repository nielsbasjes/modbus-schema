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
package nl.basjes.modbus.device.plc4j

import nl.basjes.modbus.device.api.ModbusDeviceTcpConfig
import nl.basjes.modbus.device.test.ModbusDeviceTestReadingHoldingRegisters
import nl.basjes.modbus.device.test.ModbusDeviceTestReadingInputRegisters
import nl.basjes.modbus.device.test.ModbusDeviceTester

internal class TestRead :
//    ModbusDeviceTestReadingDeviceReadCoils,   TODO: Implement Discretes
//    ModbusDeviceTestReadingDiscreteInputs,   TODO: Implement Discretes
    ModbusDeviceTestReadingInputRegisters,
    ModbusDeviceTestReadingHoldingRegisters {
    val deviceTester = ModbusDeviceTester { modbusHost, modbusPort, modbusUnit ->
        ModbusDeviceTcpConfig(modbusHost, modbusPort, modbusUnit).toModbusDevicePlc4j()
    }

    override fun deviceTester(): ModbusDeviceTester = deviceTester
}
