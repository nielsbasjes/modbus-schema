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
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertFalse

class TestGenerateExpression: QuickTestDevice() {

    private val log = PluginLoggerToLog4J()

    @Test
    fun generateExpression() {
        val output = StringWriter()
        Generator().generate(
            testDevice().toSchemaDevice(),
            null,
            "expression",
            "main",
            "nl.klokko.demo",
            "Foo",
            output,
        )
        log.info("\n${output.toString().replace(Regex("(@@@[A-Za-z0-9: _-]+@@@)"), "${LogColor.RED}$1${LogColor.RESET}")}")
        assertFalse(output.toString().contains("@@@"), "Something went wrong in the generated code" )
    }
}
