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
import nl.basjes.modbus.schema.AssertingMockedModbusDevice
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.toTable
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestReadErrorSkipping {

    private fun createTestModbusDevice(): AssertingMockedModbusDevice {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.logRequests = true
        modbusDevice.addRegisters(
            HOLDING_REGISTER,
            100,
            "002A 002A 002A 002A 002A 002A 002A xxxx 002A xxxx 002A 002A 002A 002A 002A 002A",
        )
        return modbusDevice
    }

    fun assertCorrectFieldValues(
        schemaDevice: SchemaDevice,
        valid: List<Field>,
        softReadError: List<Field>,
        hardReadError: List<Field>,
    ) {
        valid.forEach { field -> assertEquals(42L, field.longValue, "Field `${field.id}` should be 42") }
        val registerBlock = schemaDevice.getRegisterBlock(HOLDING_REGISTER)
        softReadError
            .forEach { field ->
                registerBlock
                    .get(field.requiredRegisters)
                    .forEach {
                        assertTrue("Field `${field.id}` should be a READ ERROR") { it.isReadError() }
                        assertFalse("Field `${field.id}` should be SOFT READ ERROR") { it.hardReadError }
                    }
            }
        hardReadError
            .forEach { field ->
                registerBlock
                    .get(field.requiredRegisters)
                    .forEach {
                        assertTrue("Field `${field.id}` should be a READ ERROR") { it.isReadError() }
                        assertTrue("Field `${field.id}` should be HARD READ ERROR") { it.hardReadError }
                    }
            }
    }

    @Test
    fun verifyFetchingGaps() {
        val modbusDevice = createTestModbusDevice()

        val schemaDevice = SchemaDevice("First test device")

        val block = Block(schemaDevice, "Block1", "Block 1")

        val int100 = Field(block, "int 100", expression = "int16( hr:100 ) ", immutable = true)
        val int101 = Field(block, "int 101", expression = "int16( hr:101 ) ", immutable = true)
        val int102 = Field(block, "int 102", expression = "int16( hr:102 ) ", immutable = true)
        val int103 = Field(block, "int 103", expression = "int16( hr:103 ) ", immutable = true)
        val int104 = Field(block, "int 104", expression = "int16( hr:104 ) ", immutable = true)
        val int105 = Field(block, "int 105", expression = "int16( hr:105 ) ")
        val int106 = Field(block, "int 106", expression = "int16( hr:106 ) ")
        val int107 = Field(block, "int 107", expression = "int16( hr:107 ) ")
        val int108 = Field(block, "int 108", expression = "int16( hr:108 ) ", immutable = true)
        val int109 = Field(block, "int 109", expression = "int16( hr:109 ) ")
        val int110 = Field(block, "int 110", expression = "int16( hr:110 ) ")
        val int111 = Field(block, "int 111", expression = "int16( hr:111 ) ")
        val int112 = Field(block, "int 112", expression = "int16( hr:112 ) ")
        val int113 = Field(block, "int 113", expression = "int16( hr:113 ) ")
        val int114 = Field(block, "int 114", expression = "int16( hr:114 ) ")
        val int115 = Field(block, "int 115", expression = "int16( hr:115 ) ")

        schemaDevice.initialize()
        val registerBlock = schemaDevice.getRegisterBlock(HOLDING_REGISTER)

        schemaDevice.connect(modbusDevice, 100)

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        println("-----------------------------------------------------")
        println("------------------ SET OF REGISTERS 1 ---------------")
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch
        int100.need()
        int102.need()
        int104.need()
        int106.need()
        int108.need()
        int110.need()
        int114.need()

        println("------------------ SET OF REGISTERS 1: Update 1")
        schemaDevice.update()

        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        assertCorrectFieldValues(
            schemaDevice,
            listOf(
                int100,
                int101, // This is read because it is a field in a batch that had an error
                int102,
                int103, // This is read because it is a field in a batch that had an error
                int104,
                int105, // This is read because it is a field in a batch that had an error
                int106,
                int108,
                int110,
                int111, // This is read because it is a field in a batch that had an error
                int112, // This is read because it is a field in a batch that had an error
                int113, // This is read because it is a field in a batch that had an error
                int114,
            ),
            listOf(
                int107, // Not part of the above because of the batch size
                int109,
            ),
            listOf(),
        )

        println("------------------ SET OF REGISTERS 1: Update 2 (Skipping immutables at the start)")
        schemaDevice.update()

        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // ----------------------------------------------------
        // Second fetch. All fields have been loaded; only the mutable fields should be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch
        int101.need()
        int103.need()
        int104.unNeed()
        int111.need()
        int113.need()

        println("------------------ SET OF REGISTERS 2: Update 1")
        schemaDevice.update()
        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        assertCorrectFieldValues(
            schemaDevice,
            listOf(
                int100,
                int101,
                int102,
                int103,
                int104, // No longer needed but still available.
                int105, // This is read because it is a field in a batch that had an error
                int106,
                int108,
                int110,
                int111,
                int112,
                int113,
                int114,
//                int115, // Never read
            ),
            listOf(
                int107,
                int109,
            ),
            listOf(),
        )
        println("------------------ SET OF REGISTERS 2: Update 2")
        schemaDevice.update()
        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // ----------------------------------------------------
        // Second fetch. Almost all fields have been loaded; only the mutable fields should be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch
        int100.need()
        int101.need()
        int102.need()
        int103.need()
        int104.need()
        int105.need()
        int106.need()
        int107.need()
        int108.need()
//        int109.need()
        int110.need()
        int111.need()
        int112.need()
        int113.need()
        int114.need()
        int115.need()

        println("------------------ FULL SET OF REGISTERS: Update 1")
        schemaDevice.update()
        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        assertCorrectFieldValues(
            schemaDevice,
            listOf(
                int100,
                int101,
                int102,
                int103,
                int104,
                int105,
                int106,
                int108,
                int110,
                int111,
                int112,
                int113,
                int114,
                int115,
            ),
            listOf(
                int109,
            ),
            listOf(
                int107, // No longer a soft error because we KNOW this one is the problem
            ),
        )

        println("------------------ FULL SET OF REGISTERS: Update 2")
        schemaDevice.update()
        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // ----------------------------------------------------
        // Third fetch. All fields have been loaded; only the mutable fields should be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch
        int109.need()

        println("------------------ FULL SET OF REGISTERS: Update 1")
        schemaDevice.update()
        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        assertCorrectFieldValues(
            schemaDevice,
            listOf(
                int100,
                int101,
                int102,
                int103,
                int104,
                int105,
                int106,
                int108,
                int110,
                int111,
                int112,
                int113,
                int114,
                int115,
            ),
            listOf(),
            listOf(
                int107, // No longer a soft error because we KNOW this one is the problem
                int109, // No longer a soft error because we KNOW this one is the problem
            ),
        )

        println("------------------ FULL SET OF REGISTERS: Update 2")
        schemaDevice.update()
        println("Registers after update: $registerBlock")
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        println(schemaDevice.toTable(true))
    }
}
