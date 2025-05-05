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

package nl.basjes.modbus.device.exception

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue

fun createReadErrorResponse(firstRegister: Address, count: Int): RegisterBlock {
    val registerBlock = RegisterBlock(firstRegister.addressClass)
    var address = firstRegister
    for (i in 0 until count) {
        val readErrorValue = RegisterValue(address)
        readErrorValue.setSoftReadError()
        registerBlock[address] = readErrorValue
        address = firstRegister.increment(i)
    }
    return registerBlock
}
