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
package nl.basjes.modbus.schema.fetcher

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.schema.AssertingMockedModbusDevice
import nl.basjes.modbus.schema.SchemaDevice
import kotlin.test.Test
import kotlin.test.assertEquals

class TestRequestRemerging {

    @Test
    fun verifyRemerge() {
        val modbusQuery = MergedModbusQuery(Address.of("di:00000"), 6)
        fun addQuery(address: String, size: Int) = modbusQuery.add(ModbusQuery(Address.of(address), size))

        addQuery("di:00000", 1)
        addQuery("di:00001", 1)
        addQuery("di:00002", 1)
        addQuery("di:00003", 1)
        addQuery("di:00004", 1)
        addQuery("di:00005", 1)


        val fetcher = OptimizingModbusBlockFetcher(SchemaDevice(), AssertingMockedModbusDevice())
        val mergeQueries = fetcher
            .splitMergedModbusRequest(modbusQuery)
                { it.modbusQueries.size >= 3 }

        println("-------")
        modbusQuery.modbusQueries.forEach { println("IN:  $it --> ${it.addresses.sorted()}") }
        println("-------")
        mergeQueries.forEach { println("OUT: $it --> ${it.addresses.sorted()}") }
        println("-------")

        val inAddresses  = modbusQuery.modbusQueries.map { it.addresses }.flatten().sorted()
        val outAddresses = mergeQueries.map { it.addresses }.flatten().sorted()
        println(inAddresses)
        println(outAddresses)

        assertEquals(inAddresses, outAddresses)
    }

}
