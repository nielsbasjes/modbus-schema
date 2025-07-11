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
import nl.basjes.modbus.device.api.AddressClass.COIL
import nl.basjes.modbus.schema.AssertingMockedModbusDevice
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.utils.println
import nl.basjes.modbus.schema.utils.toTable
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestReadErrorWithHoleSituation {

    private fun createTestModbusDevice(): AssertingMockedModbusDevice {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addDiscretes(
            COIL,
            0,
            "0 1 1 1 x 0 x",
        )
        return modbusDevice
    }

    fun List<ModbusQuery>.hasQuery(address: String, count: Int, status: ModbusQuery.Status, vararg fields: Field) {
        val modbusQuery = ModbusQuery(Address.of(address), count)
        modbusQuery.status = status
        modbusQuery.fields.addAll(fields)
        require(this.contains(modbusQuery)) {
            "ModbusQuery not found: $modbusQuery"
        }
    }


    @Test
    fun verifySingleHole() {
        val modbusDevice = createTestModbusDevice()
        val schemaDevice = SchemaDevice("Test device")
        val block = Block(schemaDevice, "Block1", "Block 1")
        val boolean0 = Field(block, "boolean0", expression = "boolean(c:0)")
//        val boolean1 = Field(block, "boolean1", expression = "boolean(c:1)") << This is the hole (no field for address)
//        val boolean2 = Field(block, "boolean2", expression = "boolean(c:2)") << This is the hole (no field for address)
        val boolean3 = Field(block, "boolean3", expression = "boolean(c:3)")
        val boolean4 = Field(block, "boolean4", expression = "boolean(c:4)") // << This is an error field

        schemaDevice.initialize()
        val modbusBlock = schemaDevice.getModbusBlock(COIL)
        schemaDevice.connect(modbusDevice, 100)

        block.needAll()

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        println("-----------------------------------------------------")
        println("------------------ Update 1")
        var modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")

        // 1 error + 1 for each of the 3 needed fields + 1 for the hole
        assertEquals(5, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 5, ModbusQuery.Status.ERROR,   boolean0, boolean3, boolean4)
        modbusQueries.hasQuery("c:0", 1, ModbusQuery.Status.SUCCESS, boolean0)
        modbusQueries.hasQuery("c:3", 1, ModbusQuery.Status.SUCCESS, boolean3)
        modbusQueries.hasQuery("c:4", 1, ModbusQuery.Status.ERROR,   boolean4)
        modbusQueries.hasQuery("c:1", 2, ModbusQuery.Status.SUCCESS) // The query for the hole (so no related fields)

        println("Registers after update: $modbusBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch

        // ----------------------------------------------------
        // Second fetch. Must see 1 query that reads the holes and avoids the error
        println("-----------------------------------------------------")
        println("------------------ Update 2")
        modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")

        // 1 for each needed field
        assertEquals(1, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 4, ModbusQuery.Status.SUCCESS, boolean0, boolean3) // Do not read c:4

        println("Registers after update: $modbusBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")
    }

    @Test
    fun verifyMultipleHoles() {
        val modbusDevice = createTestModbusDevice()
        val schemaDevice = SchemaDevice("Test device")
        val block = Block(schemaDevice, "Block1", "Block 1")
        val boolean0 = Field(block, "boolean0", expression = "boolean(c:0)")
//        val boolean1 = Field(block, "boolean1", expression = "boolean(c:1)") << Hole (no field for address)
//        val boolean2 = Field(block, "boolean2", expression = "boolean(c:2)") << Hole (no field for address)
        val boolean3 = Field(block, "boolean3", expression = "boolean(c:3)")
//        val boolean4 = Field(block, "boolean4", expression = "boolean(c:4)") << Hole (no field for address) and also an error value
//        val boolean5 = Field(block, "boolean5", expression = "boolean(c:5)") << Hole (no field for address)
        val boolean6 = Field(block, "boolean6", expression = "boolean(c:6)") // << This is an error field

        schemaDevice.initialize()
        val modbusBlock = schemaDevice.getModbusBlock(COIL)
        schemaDevice.connect(modbusDevice, 100)

        block.needAll()

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        println("-----------------------------------------------------")
        println("------------------ Update 1")
        var modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")

        // 1 error + 1 for each of the 3 needed fields + 2 for the holes
        assertEquals(6, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 7, ModbusQuery.Status.ERROR,   boolean0, boolean3, boolean6)
        modbusQueries.hasQuery("c:0", 1, ModbusQuery.Status.SUCCESS, boolean0)
        modbusQueries.hasQuery("c:1", 2, ModbusQuery.Status.SUCCESS ) // The query for the hole (so no related fields)
        modbusQueries.hasQuery("c:3", 1, ModbusQuery.Status.SUCCESS, boolean3)
        modbusQueries.hasQuery("c:4", 2, ModbusQuery.Status.ERROR   ) // The query for the hole (so no related fields)
        modbusQueries.hasQuery("c:6", 1, ModbusQuery.Status.ERROR,   boolean6)

        println("Registers after update: $modbusBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch

        // ----------------------------------------------------
        // Second fetch. Must see 1 query that reads the holes and avoids the error
        println("-----------------------------------------------------")
        println("------------------ Update 2")
        modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")

        // 1 for each needed field
        assertEquals(1, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 4, ModbusQuery.Status.SUCCESS, boolean0, boolean3) // Do not read c:4

        println("Registers after update: $modbusBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")
    }


}
