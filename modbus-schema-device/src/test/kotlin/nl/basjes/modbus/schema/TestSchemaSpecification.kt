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
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.net.URL
import java.util.zip.ZipInputStream

class TestSchemaSpecification {
    companion object {
        private val LOG: Logger = LogManager.getLogger()

        // Based upon https://stackoverflow.com/a/1429275/114196
        @JvmStatic
        fun listFilesInJar(jar: URL): List<String> {
            val result = mutableListOf<String>()
            LOG.warn("Said to open $jar")
            val zip = ZipInputStream(jar.openStream())
            LOG.warn("Have zipfile: $zip")

            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
//                LOG.warn("Found entry: $name")
                result.add(name)
            }
            return result
        }

        @JvmStatic
        fun allReferenceTestYamlFiles(): List<String> {
            val dir = object {}.javaClass.getResource("/SchemaReferenceTest")

            LOG.info("Have $dir")
            require(dir != null) { "Unable to find the \"/SchemaReferenceTest\"" }
            val result = mutableListOf<String>()

            File(dir.file).walkTopDown().forEach {
                if (it.extension == "yaml") {
                    result.add(it.absolutePath)
                }
            }
            if (result.isEmpty()) {
                LOG.info("Trying to load from reading ZIP file")
                val zipfileUrl = URI(dir.toString().split('!', limit = 2)[0].replace("jar:", "")).toURL()
                result.addAll(listFilesInJar(zipfileUrl))
            }
            if (result.isEmpty()) {
                LOG.fatal("Result set of SchemaReferenceTest files is empty for directory: $dir")
            }
            return result
                .filter { name -> name.contains("SchemaReferenceTest/") }
                .filter { name -> name.endsWith(".yaml") }
                .sorted()
        }
    }

    // ------------------------------------------

    @ParameterizedTest(name = "Testing schema rules {0}")
    @MethodSource("nl.basjes.modbus.schema.TestSchemaSpecification#allReferenceTestYamlFiles")
    fun `Verify all schema test cases`(schemaFile: String) {
        val schemaStream = javaClass.classLoader.getResourceAsStream(schemaFile) ?: FileInputStream(schemaFile)
        LOG.warn("Running tests from $schemaFile")
        val schemaDevice = schemaStream.toSchemaDevice()
        require(schemaDevice.initialize()) { "Unable to initialize schema device" }
        val results = schemaDevice.verifyProvidedTests()
        require(results.logResults()) {
            "Unable to verify all tests defined in the schema definition"
        }
    }
}
