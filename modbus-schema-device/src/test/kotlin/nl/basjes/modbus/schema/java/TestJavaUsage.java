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
package nl.basjes.modbus.schema.java;

import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.schema.Block;
import nl.basjes.modbus.schema.Field;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.basjes.modbus.schema.YamlLoaderKt;
import nl.basjes.modbus.schema.test.TestScenarioResultsList;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TestJavaUsage {

    @Test
    void testLoad1() throws IOException, ModbusException {
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("TestSchemas/SunSpec2025.yaml")) {
            if (schemaStream == null) {
                fail("Unable to read the test file");
                return;
            }
            SchemaDevice schemaDevice = YamlLoaderKt.toSchemaDevice(schemaStream);

            TestScenarioResultsList testScenarioResults = schemaDevice.verifyProvidedTests();
            assertTrue(testScenarioResults.getAllPassed());
        }
    }

    @Test
    void testLoad2() throws IOException, ModbusException {
        SchemaDevice schemaDevice = YamlLoaderKt.toSchemaDevice(new File("src/test/resources/TestSchemas/SunSpec2025.yaml"));
        TestScenarioResultsList testScenarioResults = schemaDevice.verifyProvidedTests();
        assertTrue(testScenarioResults.getAllPassed());
    }

    String schema =
        "# $schema: https://modbus.basjes.nl/v2/ModbusSchema.json\n" +
        "description: 'Demo based on a SunSpec device schema'\n" +
        "schemaFeatureLevel: 2\n" +
        "\n" +
        "blocks:\n" +
        "  - id: 'Block 1'\n" +
        "    description: 'The first block'\n" +
        "    fields:\n" +
        "      - id: 'Name'\n" +
        "        description: 'The name Field'\n" +
        "        #        immutable: true                  # If a field NEVER changes value then set this to true\n" +
        "        #        system: true                     # If a field is not a user level usable value set this to true (for example a scaling factor)\n" +
        "        expression: 'utf8(hr:0 # 12)'\n" +
        "\n" +
        "tests:\n" +
        "  - id: 'Just to demo the test capability'\n" +
        "    input:\n" +
        "      - firstAddress: 'hr:0'\n" +
        "        rawValues: |2-\n" +
        "          # --------------------------------------\n" +
        "          # The name is here\n" +
        "          4e69 656c 7320 4261 736a 6573 0000 0000 0000 0000 \n" +
        "          0000 0000\n" +
        "\n" +
        "    blocks:\n" +
        "      - id:          'Block 1'\n" +
        "        expected:\n" +
        "          'Name':        [ 'Niels Basjes' ]\n";

    @Test
    void testLoad3() throws IOException, ModbusException {
        SchemaDevice schemaDevice = YamlLoaderKt.toSchemaDevice(schema);

        Block block = schemaDevice.getBlock("Block 1");
        if (block == null) {
            return; // Cannot continue
        }
        Field name = block.getField("Name");
        if (name == null) {
            return; // Cannot continue
        }
        name.need();

        schemaDevice.verifyProvidedTests();
        TestScenarioResultsList testScenarioResults = schemaDevice.verifyProvidedTests();
        System.out.println(testScenarioResults);
        assertTrue(testScenarioResults.getAllPassed());
    }

}
