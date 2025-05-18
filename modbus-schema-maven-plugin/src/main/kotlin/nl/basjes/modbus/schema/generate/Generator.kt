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

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import nl.basjes.modbus.schema.toSchemaDevice
import nl.basjes.modbus.version.PROJECT_VERSION
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.logging.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.TimeZone
import java.util.regex.Pattern

class Generator {

    companion object {

        fun execute(
            log: Log,
            outputDirectory: File?,
            modbusSchemaFile: File?,
            templateDirectory: File?,
            packageName: String?,
            className: String?,
            language: String,
            type: String,
        ) {
            require(outputDirectory != null) {
                throw MojoExecutionException("outputDirectory is mandatory")
            }
            require(modbusSchemaFile != null && modbusSchemaFile.exists() && modbusSchemaFile.isFile) {
                throw MojoExecutionException("modbusSchemaFile must be an existing file")
            }
            require(!packageName.isNullOrBlank()) { throw MojoExecutionException("packageName is mandatory") }
            require(!className.isNullOrBlank()) { throw MojoExecutionException("className is mandatory") }
            require(packageName.matches("[a-zA-Z][a-zA-Z0-9.]+".toRegex())) { throw MojoExecutionException("Invalid packageName was provided.") }
            require(className.matches("[a-zA-Z][a-zA-Z0-9]+".toRegex())) { throw MojoExecutionException("Invalid className was provided.") }

            require(language.matches("[a-zA-Z0-9]+".toRegex())) { throw MojoExecutionException("Invalid programming language name was provided.") }
            require(type == "main" || type == "test") { throw MojoExecutionException("Invalid type was provided.") }

            val languageSpecificOutputDirectoryPath = (outputDirectory.absolutePath + File.separator + language).replace(Regex(Pattern.quote(File.separator) + "+"), File.separator)
            val languageSpecificOutputDirectory =  File(languageSpecificOutputDirectoryPath)

            if (!languageSpecificOutputDirectory.exists()) {
                if (!languageSpecificOutputDirectory.mkdirs()) {
                    throw MojoExecutionException("Cannot create directory $languageSpecificOutputDirectory")
                }
            }

            if (!modbusSchemaFile.exists()) {
                throw MojoExecutionException("The specified Schema file does not exist $modbusSchemaFile")
            }

            val outputFileName =
                buildFullFileName(
                    languageSpecificOutputDirectoryPath,
                    fileName(templateDirectory, language, type, packageName, className),
                )

            try {
                val outputFile = File(outputFileName)
                outputFile.parentFile.mkdirs()
                if (outputFile.exists() && outputFile.isFile) {
                    outputFile.delete()
                }
                require(outputFile.createNewFile()) { throw MojoExecutionException("Unable to create file $outputFile") }
                val fileOutput = FileOutputStream(outputFile)
                val output = OutputStreamWriter(fileOutput)
                generate(
                    modbusSchemaFile.toSchemaDevice(),
                    templateDirectory,
                    language,
                    type,
                    packageName,
                    className,
                    output,
                )
                log.info("Generated ($language ; $type): $outputFileName")
            }
            catch (e: ModbusSchemaParseException) {
                throw MojoExecutionException(e.message)
            }
            catch (e: IOException) {
                throw MojoExecutionException(e)
            }
        }

        fun getTemplateConfiguration(
            templateDirectory: File?,
            language: String,
            type: String,
        ): Configuration {
            val cfg = Configuration(Configuration.VERSION_2_3_34)
            if (templateDirectory == null) {
                cfg.setClassForTemplateLoading(Generator::class.java, "/${language}/${type}")
            } else {
                cfg.setDirectoryForTemplateLoading(File("${templateDirectory.absolutePath}/${type}"))
            }
            cfg.registerAdditionalMethods()
            cfg.defaultEncoding = "UTF-8"
            cfg.templateExceptionHandler= TemplateExceptionHandler.RETHROW_HANDLER
            cfg.logTemplateExceptions = false
            cfg.wrapUncheckedExceptions = true
            cfg.fallbackOnNullLoopVariable = false
            cfg.sqlDateAndTimeTimeZone = TimeZone.getDefault()
            return cfg
        }

        fun fileName(
            templateDirectory: File?,
            language: String,
            type: String,
            packageName: String,
            className: String,
        ): String {
            val templateConfig = getTemplateConfiguration(templateDirectory, language, type)
            val template: Template = templateConfig.getTemplate("filename.ftl")
            val output = ByteArrayOutputStream()
            template.process(mapOf("packageName" to packageName, "className" to className), OutputStreamWriter(output))
            return output.toString().replace("\r\n", "").replace("\n", "")
        }

        fun generate(
            schemaDevice: SchemaDevice?,
            templateDirectory: File?,
            language: String,
            type: String,
            packageName: String,
            className: String,
            output: Writer,
        ) {
            val templateConfig = getTemplateConfiguration(templateDirectory, language, type)
            val template: Template = templateConfig.getTemplate("code.ftl")
            template.process(
                mapOf(
                    "pluginVersion" to PROJECT_VERSION,
                    "packageName"   to packageName,
                    "className"     to className,
                    "schemaDevice" to schemaDevice,
                ),
                output,
            )
        }

        fun buildFullFileName(
            directory: String,
            fileName: String,
        ) = directory.trim { it <= ' ' } + '/' + fileName.trim { it <= ' ' }.replace("/+".toRegex(), "/")
    }
}
