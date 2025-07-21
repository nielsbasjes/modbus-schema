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

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File

@Suppress("unused") // Use reflection via @Mojo annotation
@Mojo(name = "generate-test", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
class GenerateTestMojo: AbstractMojo() {
    @Parameter(defaultValue = $$"${project.build.directory}/generated-test-sources/modbus-schema/", required = true)
    private val outputDirectory: File? = null

    @Parameter(property = "language", defaultValue = "kotlin", required = true)
    private val language: String = "kotlin"

    @Parameter(property = "modbusSchemaFile", required = true)
    private val modbusSchemaFile: String? = null

    @Parameter(property = "templateDirectory")
    private val templateDirectory: File? = null

    @Parameter(property = "packageName", defaultValue = "nl.example.modbus", required = true)
    private val packageName: String = "nl.example.modbus"

    @Parameter(property = "className", defaultValue = "ModbusDevice", required = true)
    private val className: String = "ModbusDevice"

    @Parameter(defaultValue = $$"${project}")
    private var project: MavenProject? = null

    @Throws(MojoExecutionException::class)
    override fun execute() {
        Generator(log).execute(
            project!!.basedir,
            outputDirectory,
            modbusSchemaFile,
            templateDirectory,
            packageName,
            className,
            language,
            "test",
        )
        if (project != null && outputDirectory != null) {
            project!!.addTestCompileSourceRoot(outputDirectory.absolutePath + File.separator + language)
            log.info("Marked the directory $outputDirectory as a test sources directory in the maven build.")
        }
    }
}
