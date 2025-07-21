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
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.TimeZone
import java.util.regex.Pattern

class Generator(val log: Log) {

    fun String.openAsStream(): InputStream? {
        log.debug("Trying to open: $this")
        val resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(this)
        if (resourceStream != null) {
            log.debug("- open as resource: success")
            return resourceStream
        }
        log.debug("- open as resource: failed")
        try {
            val fileInputStream = FileInputStream(this)
            log.debug("- open as file: success")
            return fileInputStream
        } catch (_: FileNotFoundException) {
            log.debug("- open as file: not found")
            return null
        }
    }

    fun execute(
        basedir: File,
        outputDirectory: File?,
        modbusSchemaFile: String?,
        templateDirectory: File?,
        packageName: String?,
        className: String?,
        language: String,
        type: String,
    ) {
        require(outputDirectory != null) {
            throw MojoExecutionException("outputDirectory is mandatory")
        }

        require(modbusSchemaFile != null) {
            throw MojoExecutionException("No modbusSchemaFile was specified")
        }

        log.info("Using Modbus Schema file $modbusSchemaFile")
        var schemaStream = modbusSchemaFile.openAsStream()
        if (schemaStream == null) {
            schemaStream = "${basedir.absolutePath}/${modbusSchemaFile}".openAsStream()
        }
        requireNotNull(schemaStream) {
            throw MojoExecutionException("Could not open the specified modbusSchemaFile $modbusSchemaFile")
        }
        val schemaDevice = schemaStream.toSchemaDevice()

        requireNotNull(schemaDevice) {
            throw MojoExecutionException("Could not open the specified modbusSchemaFile does not exist $modbusSchemaFile")
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
                schemaDevice,
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
