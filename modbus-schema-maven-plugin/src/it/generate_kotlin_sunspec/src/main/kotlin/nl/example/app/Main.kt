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
package nl.example.app

import nl.basjes.modbus.device.api.MODBUS_STANDARD_TCP_PORT
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.plc4j.ModbusDevicePlc4j
import nl.example.sunspec.SunnyBoy36

const val SUNSPEC_STANDARD_UNITID: Int = 126

fun main() {
    val hostname = "sunspec.iot.basjes.nl"

    val connectionString =
        "modbus-tcp:tcp://$hostname:$MODBUS_STANDARD_TCP_PORT?unit-identifier=$SUNSPEC_STANDARD_UNITID"

    try {
        val sunnyBoy36 = SunnyBoy36()

        ModbusDevicePlc4j(connectionString).use {
            sunnyBoy36.connect(it)

            sunnyBoy36.model1.need()
            sunnyBoy36.update(1000L)
            println("MN=${sunnyBoy36.model1.mn}")
            println("MD=${sunnyBoy36.model1.md}")
        }
    } catch (e: ModbusException) {
        throw RuntimeException(e)
    }
}
