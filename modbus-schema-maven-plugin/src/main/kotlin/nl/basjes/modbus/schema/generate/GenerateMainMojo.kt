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
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "generate-main", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
class GenerateMainMojo: AbstractMojo() {
    @Parameter(defaultValue = "\${project.build.directory}/generated-sources/", required = true)
    private val outputDirectory: File? = null

    @Parameter(property = "language", defaultValue = "kotlin", required = true)
    private val language: String = "kotlin"

    @Parameter(property = "modbusSchemaFile", required = true)
    private val modbusSchemaFile: File? = null

    @Parameter(property = "templateDirectory")
    private val templateDirectory: File? = null

    @Parameter(property = "packageName", defaultValue = "nl.example.modbus", required = true)
    private val packageName: String = "nl.example.modbus"

    @Parameter(property = "className", defaultValue = "ModbusDevice", required = true)
    private val className: String = "ModbusDevice"

    @Parameter(defaultValue = "\${project}")
    private var project: MavenProject? = null

    @Throws(MojoExecutionException::class)
    override fun execute() {
        Generator.execute(
            log,
            outputDirectory,
            modbusSchemaFile,
            templateDirectory,
            packageName,
            className,
            language,
            "main"
        )
        if (project != null && outputDirectory != null) {
            project!!.addCompileSourceRoot(outputDirectory.absolutePath + File.separator + language)
        }
    }
}
