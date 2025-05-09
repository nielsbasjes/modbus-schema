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

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlMultiLineStringStyle
import com.charleskorn.kaml.YamlSingleLineStringStyle
import kotlinx.serialization.Serializable
import nl.basjes.modbus.device.api.MODBUS_MAX_REGISTERS_PER_REQUEST
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.asAddress
import nl.basjes.modbus.device.api.toRegisterBlock
import nl.basjes.modbus.schema.Schema.Companion.serializer
import nl.basjes.modbus.schema.SchemaDevice.Companion.CURRENT_SCHEMA_FEATURE_LEVEL
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import nl.basjes.modbus.schema.test.ExpectedBlock
import nl.basjes.modbus.schema.test.TestScenario
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream


val yamlConfiguration = YamlConfiguration(
    encodeDefaults = false,
    breakScalarsAt = 100000,
    singleLineStringStyle = SingleLineStringStyle.SingleQuoted,
    multiLineStringStyle = MultiLineStringStyle.Literal,
    sequenceBlockIndent = 2,
    )

val yaml: Yaml = Yaml(configuration = yamlConfiguration)

fun String.toSchemaDevice(): SchemaDevice {
    val parsedSchema = yaml.decodeFromString(serializer(), this)
    val schemaDevice = SchemaDevice(parsedSchema.description, parsedSchema.maxRegistersPerModbusRequest)
    require(schemaDevice.schemaFeatureLevel >= parsedSchema.schemaFeatureLevel) {
        "The provided schema requires schema level ${parsedSchema.schemaFeatureLevel} which the currently " +
            "used runtime does not support (max = ${schemaDevice.schemaFeatureLevel})"
    }

    for (schemaBlock in parsedSchema.blocks) {
        val block = Block(
            schemaDevice = schemaDevice,
            id            = schemaBlock.id,
            description   = schemaBlock.description
        )
        schemaDevice.addBlock(block)
        for (schemaField in schemaBlock.fields) {
            val field = Field(
                block       = block,
                id          = schemaField.id,
                description = schemaField.description,
                expression  = schemaField.expression,
                unit        = schemaField.unit,
                immutable   = schemaField.immutable,
                system = schemaField.system,
            )
            block.addField(field)
        }
    }

    for (schemaTest in parsedSchema.tests) {
        val testScenario = TestScenario(schemaTest.id, schemaTest.description)
        schemaDevice.addTestScenario(testScenario)

        for (registers in schemaTest.input) {
            val address = registers.firstRegisterAddress.asAddress()
            val registerBlock = registers.registers.toRegisterBlock(address)
            testScenario.addRegisterBlock(registerBlock)
        }

        for (testBlock in schemaTest.blocks) {
            val expectedBlock= ExpectedBlock(testBlock.id)
            testBlock.expected.forEach { (field, value) -> expectedBlock.addExpectation(field, value) }
            testScenario.addExpectedBlock(expectedBlock)
        }
    }

    if (!schemaDevice.initialize()) {
        throw ModbusSchemaParseException("The initialize of the Logical Device failed\n" + schemaDevice.initializationProblems())
    }

    val results = schemaDevice.verifyProvidedTests()
    if (!results.allPassed) {
        throw ModbusSchemaParseException(
            results.joinToString(separator = "\n") {
                if (it.allPassed) {
                    "\n[PASS] Schema test \"${it.testName}\""
                } else {
                    "\n[FAIL] Schema test \"${it.testName}\":\nFailed fields:\n${it.toTable(true)}"
                }
            }
        )
    }

    return schemaDevice
}

// ------------------------------------------

fun File.toSchemaDevice(): SchemaDevice {
    FileInputStream(this).use { inputStream ->
        return inputStream.toSchemaDevice()
    }
}

// ------------------------------------------

fun InputStream.toSchemaDevice(): SchemaDevice {
    val content = this.bufferedReader().use(BufferedReader::readText)
    return content.toSchemaDevice()
}

// ------------------------------------------

fun Field.toSchema(): SchemaField = SchemaField(
        id,
        description,
        immutable = isImmutable,
        system = isSystem,
        expression = parsedExpression.toString(),
        unit = unit,
    )

fun Block.toSchema(): SchemaBlock {
    val schemaFields: MutableList<SchemaField> = mutableListOf()

    for (field in fields) {
        schemaFields.add(field.toSchema())
    }

    var schemaDescription:String? = null
    val blockDescription = description
    if (!blockDescription.isNullOrEmpty()) {
        schemaDescription = blockDescription
    }

    return SchemaBlock(
        id,
        schemaDescription,
        schemaFields
    )
}

fun TestScenario.toSchema(): SchemaTest {
    val testRegisters:  MutableList<SchemaTestRegisters>    = mutableListOf()
    val testBlocks:     MutableList<SchemaTestBlock>        = mutableListOf()

    for (registerBlock in registerBlocks) {
        testRegisters.add(registerBlock.toSchema())
    }

    for (expectedBlock in expectedBlocks) {
        testBlocks.add(expectedBlock.toSchema())
    }

    return SchemaTest(
        name ,
        if (description.isNullOrEmpty()) null else description,
        testRegisters,
        testBlocks
    )
}

fun RegisterBlock.toSchema(): SchemaTestRegisters {
    val sb = StringBuilder()
    val valuesList = noGapsValuesList()
    // The number of elements on THIS line
    var lineCount = 0
    for (registerValue in valuesList) {
        var comment = registerValue.comment
        if (!comment.isNullOrEmpty()) {
            comment = comment.replace("{address}", registerValue.address.toCleanFormat())
            sb.append("\n\n# " + comment.replace("\n", "\n# ") + "\n")
            lineCount = 0
        }
        if (lineCount>0) {
            sb.append(" ")
        }
        sb.append(registerValue.hexValue)
        lineCount++
        if (lineCount >= 10) {
            sb.append("\n")
            lineCount = 0
        }
    }

    return SchemaTestRegisters(
        firstAddress.toCleanFormat(),
        sb.toString()
    )
}

fun ExpectedBlock.toSchema(): SchemaTestBlock =
    SchemaTestBlock(
        blockId,
        expected,
    )

fun SchemaDevice.toSchema(): Schema {
    val schemaBlocks: MutableList<SchemaBlock> = mutableListOf()
    val schemaTests: MutableList<SchemaTest> = mutableListOf()

    for (block in blocks) {
        schemaBlocks.add(block.toSchema())
    }

    for (test in tests) {
        schemaTests.add(test.toSchema())
    }

    return Schema(
        description,
        CURRENT_SCHEMA_FEATURE_LEVEL,
        maxRegistersPerModbusRequest,
        schemaBlocks,
        schemaTests,
    )
}

fun SchemaDevice.toYaml() = yaml.encodeToString(serializer(), toSchema())

// ------------------------------------------

@Serializable
data class Schema(
    val description: String,
    val schemaFeatureLevel: Int,
    val maxRegistersPerModbusRequest: Int = MODBUS_MAX_REGISTERS_PER_REQUEST,
    val blocks: List<SchemaBlock>,
    /** A list of all test scenarios for this block */
    val tests: List<SchemaTest>,
) {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}

@Serializable
data class SchemaBlock(
    /**
     * The technical id of the block.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces, starting with a letter) is a good choice.
     */
    val id: String,

    /**
     * The human-readable description of the block.
     */
    val description: String? = null,

    /** A list of all the fields in this block */
    val fields: List<SchemaField>,
) {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}


@Serializable
data class SchemaField(
    /**
     * The technical id of the field.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces) is a good choice.
     */
    val id: String,

    /** Human-readable description of the field. */
    val description: String = "",

    /**
     * If a field NEVER changes then this can be se to true.
     * This allows a library to only read this value the first time
     * and on subsequent updates skip reading this value.
     * An example of these are software version numbers and the scaling factor as used by SunSpec.
     */
    val immutable: Boolean = false,

    /**
     * Some fields are system fields which means they should not be used by the application.
     * An example of these are the scaling factor as used by SunSpec.
     */
    val system: Boolean = false,

    /**
     * The expression that defines how this Field gets its value.
     */
    @YamlSingleLineStringStyle(SingleLineStringStyle.SingleQuoted)
    @YamlMultiLineStringStyle(MultiLineStringStyle.Literal)
    val expression: String,

    /** Human-readable unit of the field (like 'V' for Volt or '%' for percentage).     */
    val unit: String = "",
) {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}


@Serializable
data class SchemaTest(
    /**
     * The technical id of the test scenario.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces) is a good choice.
     */
    val id: String,
    /** Human-readable description of the test scenario. */
    val description: String? = null,
    /** A list of 1 or more register blocks used for the test. */
    val input: List<SchemaTestRegisters>,
    /** Per block which field values are expected. */
    val blocks: List<SchemaTestBlock>
)  {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}

@Serializable
data class SchemaTestRegisters(
    /** The Address of the first provided registers. */
    val firstRegisterAddress: String,
    /** The register values used for this test. */
    @YamlSingleLineStringStyle(SingleLineStringStyle.Plain)
    @YamlMultiLineStringStyle(MultiLineStringStyle.Literal)
    val registers: String,
)  {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}

@Serializable
data class SchemaTestBlock(
    /**
     * The technical id of the block.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces, starting with a letter) is a good choice.
     */
    val id: String,

    /** A map of all field ids and the expected value given the provided registers. */
    val expected: Map<String, List<String>>
)  {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}
