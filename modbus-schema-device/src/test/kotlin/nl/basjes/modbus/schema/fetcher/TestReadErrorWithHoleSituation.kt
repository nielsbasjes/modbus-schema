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
import nl.basjes.modbus.schema.toTable
import nl.basjes.modbus.schema.utils.println
import nl.basjes.modbus.schema.utils.toTable
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        fields.forEach { modbusQuery.addField(it) }
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
//        assertEquals(5, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 5, ModbusQuery.Status.ERROR,   boolean0, boolean3, boolean4)
//        modbusQueries.hasQuery("c:0", 1, ModbusQuery.Status.SUCCESS, boolean0)
//        modbusQueries.hasQuery("c:3", 1, ModbusQuery.Status.SUCCESS, boolean3)
        modbusQueries.hasQuery("c:4", 1, ModbusQuery.Status.ERROR,   boolean4)
//        modbusQueries.hasQuery("c:1", 2, ModbusQuery.Status.SUCCESS) // The query for the hole (so no related fields)

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

    // ==========================================================================================

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
//        assertEquals(6, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 7, ModbusQuery.Status.ERROR,   boolean0, boolean3, boolean6)
//        modbusQueries.hasQuery("c:0", 1, ModbusQuery.Status.SUCCESS, boolean0)
//        modbusQueries.hasQuery("c:1", 2, ModbusQuery.Status.SUCCESS ) // The query for the hole (so no related fields)
//        modbusQueries.hasQuery("c:3", 1, ModbusQuery.Status.SUCCESS, boolean3)
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

    // ==========================================================================================

    @Test
    fun verifySingleHoleWithUnqueriedField() {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addDiscretes(
            COIL,
            0,
            // 0 1 2 3 4
              "1 x 1 x 1",
        )

        val schemaDevice = SchemaDevice("Test device")
        val block = Block(schemaDevice, "Block1", "Block 1")
        val boolean0 = Field(block, "boolean0", expression = "boolean(c:0)")
//        val boolean1 = Field(block, "boolean1", expression = "boolean(c:1)") << This is the hole which is an error
        val boolean2 = Field(block, "boolean2", expression = "boolean(c:2)")
        val boolean3 = Field(block, "boolean3", expression = "boolean(c:3)") // << Field error
        val boolean4 = Field(block, "boolean4", expression = "boolean(c:4)")

        schemaDevice.initialize()
        val modbusBlock = schemaDevice.getModbusBlock(COIL)
        schemaDevice.connect(modbusDevice, 100)

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        println("-----------------------------------------------------")
        println("------------------ Update 1")

        // By only needing the first and last, the 2 in between will be fetched also by the optimizer.
        boolean0.need()
        boolean4.need()

        var modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")
        println(schemaDevice.toTable(onlyUseFullFields = false, includeRawDataAndMappings = true))

        assertEquals(4, modbusQueries.count())

        modbusQueries.hasQuery("c:0", 5, ModbusQuery.Status.ERROR,   boolean0, boolean4)
        modbusQueries.hasQuery("c:0", 1, ModbusQuery.Status.SUCCESS, boolean0)
        // So the hole query was the one with the error.
        // NOTE: The valid FIELD was NOT in error so we should be able to get it after this !!!
        modbusQueries.hasQuery("c:1", 3, ModbusQuery.Status.ERROR)
        modbusQueries.hasQuery("c:4", 1, ModbusQuery.Status.SUCCESS, boolean4)

        println("Registers after update: $modbusBlock")

        assertTrue("Wrong value for boolean 0 (update 1)") { boolean0.booleanValue?:false }
        assertTrue("Wrong value for boolean 4 (update 1)") { boolean4.booleanValue?:false }

        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        boolean0.unNeed()
        boolean4.unNeed()

        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the next fetch

        assertTrue(schemaDevice.neededFields().isEmpty(), "There should not be any needed fields at this point.")
        // ----------------------------------------------------
        // Second fetch.
        println("-----------------------------------------------------")
        println("------------------ Update field 2")

        // Now in the previous update the address of this field was part of a larger query that was an error.
        // We must be able to query this and get the value anyway.
        boolean2.need()
        modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")

        println(schemaDevice.toTable(onlyUseFullFields = false, includeRawDataAndMappings = true))

        // 1 for each needed field
        assertEquals(1, modbusQueries.count())

        modbusQueries.hasQuery("c:2", 1, ModbusQuery.Status.SUCCESS, boolean2)

        println("Registers after update: $modbusBlock")

        assertTrue("Wrong value for boolean 0 (update 2)") { boolean0.booleanValue?:false }
        assertTrue("Wrong value for boolean 2 (update 2)") { boolean2.booleanValue?:false }
        assertTrue("Wrong value for boolean 4 (update 2)") { boolean4.booleanValue?:false }

        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        boolean2.unNeed()
        assertTrue(schemaDevice.neededFields().isEmpty(), "There should not be any needed fields at this point.")

        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the next fetch

        // ----------------------------------------------------
        // Second fetch.
        println("-----------------------------------------------------")
        println("------------------ Update field 3")

        // Now in the previous update the address of this field was part of a larger query that was an error.
        // We must be able to query this and get the value anyway.
        boolean3.need()
        modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")

        println(schemaDevice.toTable(onlyUseFullFields = false, includeRawDataAndMappings = true))

        // 1 for each needed field
        assertEquals(1, modbusQueries.count())

        modbusQueries.hasQuery("c:3", 1, ModbusQuery.Status.ERROR, boolean3)

        println("Registers after update: $modbusBlock")

        assertTrue("Wrong value for boolean 0 (update 3)") { boolean0.booleanValue?:false }
        assertTrue("Wrong value for boolean 2 (update 3)") { boolean2.booleanValue?:false }
        assertNull(boolean3.booleanValue, "Wrong value for boolean 3 (update 3)")
        assertTrue("Wrong value for boolean 4 (update 3)") { boolean4.booleanValue?:false }

        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")
        boolean3.unNeed()
        assertTrue(schemaDevice.neededFields().isEmpty(), "There should not be any needed fields at this point.")
    }

}
