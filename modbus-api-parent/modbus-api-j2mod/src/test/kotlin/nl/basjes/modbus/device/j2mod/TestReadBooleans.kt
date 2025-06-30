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
import com.ghgande.j2mod.modbus.procimg.Register
import com.ghgande.j2mod.modbus.util.BitVector
import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.MODBUS_STANDARD_TCP_PORT
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.testcases.sunspec.SUNSPEC_STANDARD_UNITID
import nl.basjes.modbus.device.testcases.sunspec.SunSpecBasicsPrinter
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("Requires real device")
internal class TestReadBooleans {

    fun Boolean.p() = if(this) "1" else "0"

    @Test
    fun readBooleans() {
        val master: AbstractModbusMaster = ModbusTCPMaster("localhost", 1502)
        try {
            master.connect()
            require(master.isConnected) { "The provided master must be connected" }

            for (i in 0 ..8) {
                val coils: BitVector = master.readCoils(1, 3+i, 40-i)
                val coil27 = 27 - (3+i)
                println("$i " +
                    coils.getBit(coil27).p() +
                    coils.getBit(coil27+1).p() +
                    coils.getBit(coil27+2).p() +
                    coils.getBit(coil27+3).p() +
                    coils.getBit(coil27+4).p() +
                    coils.getBit(coil27+5).p() +
                    coils.getBit(coil27+6).p() +
                    coils.getBit(coil27+7).p() +
                    coils.getBit(coil27+8).p() +
                    "    : $coils")
            }
//            val modbusDevice: ModbusDevice = ModbusDeviceJ2Mod(master, SUNSPEC_STANDARD_UNITID)

        } catch (e: Exception) {
            throw ModbusException("Unable to connect to the master", e)
        } finally {
            master.disconnect()
        }
    }
}
