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
package nl.example;

import nl.basjes.modbus.device.api.AddressClass;
import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.memory.MockedModbusDevice;

public class FakeModbusDevice {
    public static final ModbusDevice modbusDevice = MockedModbusDevice
            .builder()
//            .withLogging()
            .withRegisters(
                AddressClass.HOLDING_REGISTER,
                0,
                """
                449A 5225
                4093 4A45 84FC D47C

                100F
                100F 100F
                100F 100F 100F
                100F 100F 100F 100F
                0001

                0102 0304 0506 0708

                CFC7
                3039
                B669 FD2E
                4996 02d2
                EEDD EF0B 8216 7EEB
                1122 10F4 7DE9 8115
                0102 0304
                0001 0203 0405 0607 0809 1011 1213 1415
                4E69 656C 7320 4261 736A 6573
                """
            ).build();
}
