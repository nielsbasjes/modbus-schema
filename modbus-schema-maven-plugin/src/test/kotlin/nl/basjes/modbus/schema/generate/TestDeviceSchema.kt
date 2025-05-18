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
package nl.basjes.modbus.schema.generate

import nl.basjes.modbus.schema.toSchemaDevice
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.FileInputStream
import kotlin.test.Test
import kotlin.test.assertFalse

class TestDeviceSchema {

    private val log: Logger = LogManager.getLogger()

    @Test
    fun ensureValidTestDeviceSchema() {
        val schemaFilename = "TestDevice.yaml"
        val schemaStream = TestDeviceSchema::class.java.classLoader.getResourceAsStream(schemaFilename) ?: FileInputStream(schemaFilename)
        val schemaDevice = schemaStream.toSchemaDevice()
        require(schemaDevice.initialize()) { "Unable to initialize schema device" }
        val results = schemaDevice.verifyProvidedTests()
        var failed = false
        for (result in results) {
            if (result.allPassed) {
                log.info("[PASS] Schema test \"${result.testName}\"")
            } else {
                log.error("[FAIL] Schema test \"${result.testName}\":")
                log.error("Failed fields:\n${result.toTable(true)}")
                failed = true
            }
        }
        assertFalse(failed, "Unable to verify all tests defined in the schema definition")
    }
}
