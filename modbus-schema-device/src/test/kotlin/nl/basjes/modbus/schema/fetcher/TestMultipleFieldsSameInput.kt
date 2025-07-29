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

import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.exception.ModbusApiException
import nl.basjes.modbus.schema.AssertingMockedModbusDevice
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.toTable
import nl.basjes.modbus.schema.utils.println
import nl.basjes.modbus.schema.utils.toTable
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestMultipleFieldsSameInput {

    // ==========================================================================================

    @Test
    fun verifySingleRegister() {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addRegisters(
            HOLDING_REGISTER,
            0,
            "0001 0002 0003 0004",
        )

        // This is REALLY nasty and will probably never happen in real systems.
        // - Several registers that are used by multiple fields (ok, happens)
        // - In partially overlapping rules (nasty!, I cannot think of a valid reason to do this)

        val schemaDevice = SchemaDevice("Test device")
        val block = Block(schemaDevice, "Block1", "Block 1")
        // First and Last Fields looking at the SAME input
        val int16_0a = Field(block, "int16_0a", expression = "int16(hr:0)")
        val int16_1a = Field(block, "int16_1a", expression = "int16(hr:1)")
        val int16_2a = Field(block, "int16_2a", expression = "int16(hr:2)")
        val int16_3a = Field(block, "int16_3a", expression = "int16(hr:3)")

        // Duplicate set overlapping 100%
        val int16_0b = Field(block, "int16_0b", expression = "int16(hr:0)")
        val int16_1b = Field(block, "int16_1b", expression = "int16(hr:1)")
        val int16_2b = Field(block, "int16_2b", expression = "int16(hr:2)")
        val int16_3b = Field(block, "int16_3b", expression = "int16(hr:3)")

        val valid_int16_0:Long = 1       // 0x0001        --> "int16(hr:0)"
        val valid_int16_1:Long = 2       // 0x0002        --> "int16(hr:1)"
        val valid_int16_2:Long = 3       // 0x0003        --> "int16(hr:2)"
        val valid_int16_3:Long = 4       // 0x0004        --> "int16(hr:3)"

        schemaDevice.initialize()
        val modbusBlock = schemaDevice.getModbusBlock(HOLDING_REGISTER)
        schemaDevice.connect(modbusDevice, 100)

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        println("-----------------------------------------------------")
        println("------------------ Update 1")

        schemaDevice.needAll()
        val modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")
        println(schemaDevice.toTable(onlyUseFullFields = false, includeRawDataAndMappings = true))

        assertEquals(1, modbusQueries.count())

        modbusQueries.hasQuery("hr:0", 4, ModbusQuery.Status.SUCCESS,
            int16_0a, int16_1a, int16_2a, int16_3a, //int32_0a, int32_1a, int32_2a,
            int16_0b, int16_1b, int16_2b, int16_3b, //int32_0b, int32_1b, int32_2b,
        )

        println("Registers after update: $modbusBlock")

        assertEquals(valid_int16_0, int16_0a.longValue, "Incorrect value for int16_0a")
        assertEquals(valid_int16_1, int16_1a.longValue, "Incorrect value for int16_1a")
        assertEquals(valid_int16_2, int16_2a.longValue, "Incorrect value for int16_2a")
        assertEquals(valid_int16_3, int16_3a.longValue, "Incorrect value for int16_3a")
        assertEquals(valid_int16_0, int16_0b.longValue, "Incorrect value for int16_0b")
        assertEquals(valid_int16_1, int16_1b.longValue, "Incorrect value for int16_1b")
        assertEquals(valid_int16_2, int16_2b.longValue, "Incorrect value for int16_2b")
        assertEquals(valid_int16_3, int16_3b.longValue, "Incorrect value for int16_3b")

        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")
    }

    // ==========================================================================================

    @Test
    fun verifyMultiRegister() {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addRegisters(
            HOLDING_REGISTER,
            0,
            "0001 0002 0003 0004",
        )

        // This is REALLY nasty and will probably never happen in real systems.
        // - Several registers that are used by multiple fields (ok, happens)
        // - In partially overlapping rules (nasty!, I cannot think of a valid reason to do this)

        val schemaDevice = SchemaDevice("Test device")
        val block = Block(schemaDevice, "Block1", "Block 1")
        // First and Last Fields looking at the SAME input
        val int32_0a = Field(block, "int32_0a", expression = "int32(hr:0#2)")
        val int32_2a = Field(block, "int32_2a", expression = "int32(hr:2#2)")

        // Duplicate set overlapping 100%
        val int32_0b = Field(block, "int32_0b", expression = "int32(hr:0#2)")
        val int32_2b = Field(block, "int32_2b", expression = "int32(hr:2#2)")

        val valid_int32_0:Long = 65538   // 0x0001 0x0002 --> "int32(hr:0#2)"
        val valid_int32_2:Long = 196612  // 0x0003 0x0004 --> "int32(hr:2#2)"

        schemaDevice.initialize()
        val modbusBlock = schemaDevice.getModbusBlock(HOLDING_REGISTER)
        schemaDevice.connect(modbusDevice, 100)

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        println("-----------------------------------------------------")
        println("------------------ Update 1")

        schemaDevice.needAll()
        val modbusQueries = schemaDevice.update()
        modbusQueries.toTable().println("\n")
        println(schemaDevice.toTable(onlyUseFullFields = false, includeRawDataAndMappings = true))

        assertEquals(1, modbusQueries.count())

        modbusQueries.hasQuery("hr:0", 4, ModbusQuery.Status.SUCCESS,
            int32_0a, int32_2a,
            int32_0b, int32_2b,
        )

        println("Registers after update: $modbusBlock")

        assertEquals(valid_int32_0, int32_0a.longValue, "Incorrect value for int32_0a")
        assertEquals(valid_int32_2, int32_2a.longValue, "Incorrect value for int32_2a")
        assertEquals(valid_int32_0, int32_0b.longValue, "Incorrect value for int32_0b")
        assertEquals(valid_int32_2, int32_2b.longValue, "Incorrect value for int32_2b")

        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")
    }

    // ==========================================================================================

    @Test
    fun verifyMultiRegisterPartialOverlap() {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addRegisters(
            HOLDING_REGISTER,
            0,
            "0001 0002 0003 0004",
        )

        // This is REALLY nasty and will probably never happen in real systems.
        // - Several registers that are used by multiple fields (ok, happens)
        // - In partially overlapping rules (nasty!, I cannot think of a valid reason to do this)

        val schemaDevice = SchemaDevice("Test device")
        val block = Block(schemaDevice, "Block1", "Block 1")
        // First and Last Fields looking at partially overlapping input
        Field(block, "int32_0a", expression = "int32(hr:0#2)")
        Field(block, "int32_1a", expression = "int32(hr:1#2)")
        Field(block, "int32_2a", expression = "int32(hr:2#2)")

        schemaDevice.initialize()
        schemaDevice.connect(modbusDevice, 100)

        schemaDevice.needAll()
        val modbusApiException = assertThrows<ModbusApiException> { schemaDevice.update() }

        assertTrue { modbusApiException.message!!.contains("partially overlapping Modbus values is not allowed") }
    }

}
