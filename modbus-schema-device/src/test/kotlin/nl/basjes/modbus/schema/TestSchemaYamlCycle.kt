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

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileInputStream
import kotlin.test.assertEquals

private val LOG: Logger = LogManager.getLogger()

class TestSchemaYamlCycle {
    @ParameterizedTest(name = "Using schema rules {0}")
    @MethodSource("nl.basjes.modbus.schema.TestSchemaSpecification#allReferenceTestYamlFiles")
    fun `Verify all schema test cases`(schemaFile: String) {
        // Load the provided Yaml into a SchemaDevice
        val schemaStream = javaClass.classLoader.getResourceAsStream(schemaFile) ?: FileInputStream(schemaFile)
        LOG.warn("Running tests from $schemaFile")
        LOG.info("- Original Yaml")
        val schemaDevice = schemaStream.toSchemaDevice()
        require(schemaDevice.initializeAndVerify()) { "Unable to initialize and verify all tests (original)" }

        LOG.info("- Generated Yaml 1")
        val yaml1 = schemaDevice.toYaml()
        val schemaDevice1 = yaml1.toSchemaDevice()
        require(schemaDevice1.initializeAndVerify()) { "Unable to initialize and verify all tests (generated Yaml 1)" }

        LOG.info("- Generated Yaml 2")
        val yaml2 = schemaDevice1.toYaml()
        val schemaDevice2 = yaml2.toSchemaDevice()
        require(schemaDevice2.initializeAndVerify()) { "Unable to initialize and verify all tests (generated Yaml 2)" }

        assertEquals(yaml1, yaml2, "The generated yaml files must be identical")
    }
}
