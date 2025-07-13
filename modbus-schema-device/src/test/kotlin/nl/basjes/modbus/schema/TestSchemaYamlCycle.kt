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

import nl.basjes.modbus.schema.utils.VerifyYamlCycle
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.FileInputStream
import kotlin.test.Test

private val LOG: Logger = LogManager.getLogger()

class TestSchemaYamlCycle: VerifyYamlCycle() {
    @ParameterizedTest(name = "Using schema rules {0}")
    @MethodSource("nl.basjes.modbus.schema.TestSchemaSpecification#allReferenceTestYamlFiles")
    fun `Verify all schema test cases`(schemaFile: String) {
        // Load the provided Yaml into a SchemaDevice
        val schemaStream = javaClass.classLoader.getResourceAsStream(schemaFile) ?: FileInputStream(schemaFile)
        LOG.warn("Running tests from $schemaFile")
        val schemaDevice = schemaStream.toSchemaDevice()
        verifySchemaCycle(schemaDevice)
    }

    @Test
    fun `Verify Yaml cycle with SunSpec 2023 schema`() {
        verifySchemaCycle(File("src/test/resources/TestSchemas/SunSpec2023.yaml").readText().toSchemaDevice())
    }

    @Test
    fun `Verify Yaml cycle with SunSpec 2025 schema`() {
        verifySchemaCycle(File("src/test/resources/TestSchemas/SunSpec2025.yaml").readText().toSchemaDevice())
    }

    @Test
    fun `Verify Yaml cycle with SunSpec Emulated Der schema`() {
        verifySchemaCycle(File("src/test/resources/TestSchemas/SunSpecEmulatedDer.yaml").readText().toSchemaDevice())
    }

    @Test
    fun `Verify Yaml cycle with Test Device Schema which includes Discretes`() {
        verifySchemaCycle(File("src/test/resources/TestSchemas/TestDevice.yaml").readText().toSchemaDevice())
    }

}
