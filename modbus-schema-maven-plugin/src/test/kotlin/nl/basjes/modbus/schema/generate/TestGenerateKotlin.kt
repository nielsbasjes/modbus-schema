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

import nl.basjes.modbus.schema.generate.Generator.Companion.fileName
import nl.basjes.modbus.schema.generate.Generator.Companion.generate
import nl.basjes.modbus.schema.toSchemaDevice
import java.io.File
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGenerateKotlin{

    private val log = PluginLoggerToSlf4j()

    @Test
    fun checkOutputFileName() {
        val mainFileName = fileName(null, "kotlin", "main", "nl.klokko.demo", "Foo")
        assertEquals("nl/klokko/demo/Foo.kt", mainFileName)

        val testFileName = fileName(null, "kotlin", "test", "nl.klokko.demo", "Foo")
        assertEquals("nl/klokko/demo/TestFoo.kt", testFileName)
    }

    @Test
    fun generateKotlin() {
        val output = StringWriter()
        generate(
            File("src/test/resources/TestDevice.yaml").toSchemaDevice(),
            null,
            "kotlin", "main",
            "nl.klokko.demo", "Foo",
            output)
        log.info("\n${output}")
    }

    @Test
    fun generateKotlinTest() {
        val output = StringWriter()
        generate(
            File("src/test/resources/TestDevice.yaml").toSchemaDevice(),
            null,
            "kotlin", "test",
            "nl.klokko.demo", "Foo",
            output)
        log.info("\n${output}")
    }
}
