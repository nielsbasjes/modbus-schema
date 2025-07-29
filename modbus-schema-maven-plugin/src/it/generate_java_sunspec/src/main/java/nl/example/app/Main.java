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
package nl.example.app;

import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.device.plc4j.ModbusDevicePlc4j;
import nl.example.sunspec.SunnyBoy36;

import static nl.basjes.modbus.device.api.ModbusDeviceKt.MODBUS_STANDARD_TCP_PORT;

public class Main {

    private static final Integer SUNSPEC_STANDARD_UNITID = 126;

    public static void main(String[] args) {

        String hostname = "sunspec.iot.basjes.nl";

        String connectionString =
            "modbus-tcp:tcp://"+hostname+":"+MODBUS_STANDARD_TCP_PORT+"?unit-identifier="+SUNSPEC_STANDARD_UNITID;

        try(ModbusDevice modbusDevice = new ModbusDevicePlc4j(connectionString)) {
            SunnyBoy36 sunnyBoy36 = new SunnyBoy36();
            sunnyBoy36.connect(modbusDevice);

            sunnyBoy36.model1.need();
            sunnyBoy36.update(1000L);
            System.out.println("MN="+sunnyBoy36.model1.mn);
            System.out.println("MD="+sunnyBoy36.model1.md);

        } catch (ModbusException e) {
            throw new RuntimeException(e);
        }
    }

}
