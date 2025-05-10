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
package nl.basjes.modbus.schema;

import org.junit.jupiter.api.Test;

import static nl.basjes.modbus.device.api.AddressClass.INPUT_REGISTER;
import static org.junit.jupiter.api.Assertions.*;

public class TestBuilderJava {

    @Test
    public void testSchemaDeviceBuilderJava() {
        SchemaDevice schemaDevice = SchemaDevice
            .builder()
            .description("To rule them all")
            .maxRegistersPerModbusRequest(42)
            .build();
        assertEquals("To rule them all", schemaDevice.getDescription());
        assertEquals(42, schemaDevice.getMaxRegistersPerModbusRequest());

        Block block = Block
            .builder()
            .schemaDevice(schemaDevice)
            .id("BlockOne")
            .description("Block One Description")
            .build();
        assertSame(schemaDevice, block.getSchemaDevice());
        assertEquals("BlockOne", block.getId());
        assertEquals("Block One Description", block.getDescription());

        Field field = Field
            .builder()
            .block(block)
            .id("FieldOne")
            .description("Field One Description")
            .system(true)
            .immutable(false)
            .expression("42")
            .build();

        assertEquals(block, field.getBlock());
        assertEquals("FieldOne", field.getId());
        assertEquals("Field One Description", field.getDescription());
        assertEquals("42", field.getExpression());
        assertTrue(field.isSystem());
        assertFalse(field.isImmutable());
    }

}
