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
import java.io.FileInputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val LOG: Logger = LogManager.getLogger()

class TestSchemaReducedYaml {

    fun String.openAsStream(): InputStream {
        val fileStream = ClassLoader.getSystemClassLoader().getResourceAsStream(this)
        if (fileStream != null) {
            return fileStream
        }
        return FileInputStream(this)
    }

    fun InputStream.readAsString(): String  = this.bufferedReader().use { it.readText() }

    fun String.readFileNameToString(): String  = this.openAsStream().readAsString()

    @Test
    fun `Verify Yaml Testblock reduction`() {
        val schemaDeviceFullYaml = "src/test/resources/TestSchemas/SDM230-full.yaml".readFileNameToString()
        val schemaDeviceReducedYaml = "src/test/resources/TestSchemas/SDM230-reduced.yaml".readFileNameToString()

        val schemaDevice = schemaDeviceFullYaml.toSchemaDevice()

        assertTrue(schemaDevice.initializeAndVerify())

        val schemaYaml = schemaDevice.toYaml()

        assertEquals(schemaDeviceReducedYaml, schemaYaml)

        val results = schemaDeviceReducedYaml.toSchemaDevice().verifyProvidedTests()
        results.forEach { result -> LOG.info("\n{}", result.toTable()) }
        assertTrue(results.allPassed)
    }

}
