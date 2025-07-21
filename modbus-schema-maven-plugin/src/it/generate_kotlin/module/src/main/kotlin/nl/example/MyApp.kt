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
package nl.example

import nl.basjes.modbus.device.api.ModbusDevice
import nl.example.modbus.MyNewDevice

fun main(args : Array<String>) {
    MyApp(FakeModbusDevice.modbusDevice).doSomething()
}

class MyApp(val modbusDevice: ModbusDevice) {
    fun doSomething() {
        val device = MyNewDevice().connect(modbusDevice)
        device.main.myIPv4.need()
        device.update(10000) // Need everything to be at most 10 seconds old.
        println("${device.main.myIPv4.value}")
    }

}
