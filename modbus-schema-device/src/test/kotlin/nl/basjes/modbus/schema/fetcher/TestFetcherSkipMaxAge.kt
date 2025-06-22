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

import nl.basjes.modbus.schema.assertCorrectFieldValues
import nl.basjes.modbus.schema.createTestModbusDevice
import nl.basjes.modbus.schema.createTestSchemaDevice
import nl.basjes.modbus.schema.utils.println
import nl.basjes.modbus.schema.utils.toTable
import kotlin.test.Test

class TestFetcherSkipMaxAge {

    @Test
    fun ensureNoSecondFetch() {
        val modbusDevice = createTestModbusDevice()
        val schemaDevice = createTestSchemaDevice()
        schemaDevice.connect(modbusDevice)

        schemaDevice.needAll()

        println("First fetch should get everything")
        schemaDevice.update()
            .toTable().println("\n") // After the fetch print what was fetched
        assertCorrectFieldValues(schemaDevice)

        modbusDevice.failOnFetch=true

        // With a high max age there should be no fetch done because everything is
        // still up-to-date enough
        // Really large maxAge (1 day in milliseconds) to allow stepping through with debugger
        println("Second fetch should get nothing")
        schemaDevice.update(86400000)
            .toTable().println("\n") // After the fetch print what was fetched

        assertCorrectFieldValues(schemaDevice)
    }

}
