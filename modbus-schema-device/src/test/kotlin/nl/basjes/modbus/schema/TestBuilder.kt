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
package nl.basjes.modbus.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestBuilder {
    @Test
    fun testSchemaDeviceBuilder() {
        val schemaDevice = SchemaDevice
            .builder()
            .description("To rule them all")
            .maxRegistersPerModbusRequest(42)
            .build()
        assertEquals("To rule them all", schemaDevice.description)
        assertEquals(42, schemaDevice.maxRegistersPerModbusRequest)

        val block = Block
            .builder()
            .schemaDevice(schemaDevice)
            .id("BlockOne")
            .description("Block One Description")
            .build()
        assertSame(schemaDevice, block.schemaDevice)
        assertEquals("BlockOne", block.id)
        assertEquals("Block One Description", block.description)

        val field = Field
            .builder()
            .block(block)
            .id("FieldOne")
            .description("Field One Description")
            .system(true)
            .immutable(false)
            .expression("42")
            .build()

        assertEquals(block, field.block)
        assertEquals("FieldOne", field.id)
        assertEquals("Field One Description", field.description)
        assertEquals("42", field.expression)
        assertTrue(field.isSystem)
        assertFalse(field.isImmutable)

        // Also check the array index notation in nullable situations.
        val nullDevice: SchemaDevice? = null

        assertEquals(null, nullDevice["BlockOne"]["FieldOne"]?.expression)
        assertEquals(null, schemaDevice["NoSuchBlock"]["FieldOne"]?.expression)
        assertEquals(null, schemaDevice["BlockOne"]["NoSuchField"]?.expression)
        assertEquals("42", schemaDevice["BlockOne"]["FieldOne"]?.expression)
    }
}
