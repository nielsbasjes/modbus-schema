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
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import nl.basjes.modbus.device.api.MODBUS_STANDARD_TCP_PORT
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.sunspec.SUNSPEC_STANDARD_UNITID
import nl.basjes.modbus.device.sunspec.SunSpecBasicsPrinter
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("Requires real device")
internal class TestRead {
    @Test
    @Throws(ModbusException::class)
    fun readSunSpecHeader() {
        val master: AbstractModbusMaster = ModbusTCPMaster("sunspec.iot.basjes.nl", MODBUS_STANDARD_TCP_PORT)
        try {
            master.connect()
            val modbusDevice: ModbusDevice = ModbusDeviceJ2Mod(master, SUNSPEC_STANDARD_UNITID)
            SunSpecBasicsPrinter(modbusDevice).print()
        } catch (e: Exception) {
            throw ModbusException("Unable to connect to the master", e)
        } finally {
            master.disconnect()
        }
    }
}
