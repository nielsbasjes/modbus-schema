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

val yamlConfiguration =
    YamlConfiguration(
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
        val block =
            Block(
                schemaDevice    = schemaDevice,
                id              = schemaBlock.id,
                description     = schemaBlock.description,
            )
        for (schemaField in schemaBlock.fields) {
            Field(
                block           = block,
                id              = schemaField.id,
                description     = schemaField.description,
                expression      = schemaField.expression,
                unit            = schemaField.unit,
                immutable       = schemaField.immutable,
                system          = schemaField.system,
            )
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
            val expectedBlock = ExpectedBlock(testBlock.id)
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
            },
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

fun Field.toSchema(): SchemaField =
    SchemaField(
        id          = id,
        description = description,
        immutable   = isImmutable,
        system      = isSystem,
        expression  = parsedExpression.toString(),
        unit        = unit,
        fetchGroup  = if (fetchGroupIsDefault) "" else fetchGroup,
    )

fun Block.toSchema(): SchemaBlock {
    val schemaFields: MutableList<SchemaField> = mutableListOf()

    for (field in fields) {
        schemaFields.add(field.toSchema())
    }

    var schemaDescription: String? = null
    val blockDescription = description
    if (!blockDescription.isNullOrEmpty()) {
        schemaDescription = blockDescription
    }

    return SchemaBlock(
        id,
        schemaDescription,
        schemaFields,
    )
}

fun TestScenario.toSchema(): SchemaTest {
    val testRegisters: MutableList<SchemaTestRegisters> = mutableListOf()
    val testBlocks: MutableList<SchemaTestBlock> = mutableListOf()

    for (registerBlock in registerBlocks) {
        testRegisters.add(registerBlock.toSchema())
    }

    for (expectedBlock in expectedBlocks) {
        testBlocks.add(expectedBlock.toSchema())
    }

    return SchemaTest(
        name,
        if (description.isNullOrEmpty()) null else description,
        testRegisters,
        testBlocks,
    )
}

fun RegisterBlock.toSchema(): SchemaTestRegisters {
    return SchemaTestRegisters(
        firstAddress?.toCleanFormat() ?: "Empty",
        this.toMultiLineString(),
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

fun SchemaDevice.toYaml() = yaml.encodeToString(serializer(), toSchema()).reformatModbusSchemaYaml()

// ------------------------------------------

@Serializable
data class Schema(
    val description: String,
    val schemaFeatureLevel: Int,
    val maxRegistersPerModbusRequest: Int = MODBUS_MAX_REGISTERS_PER_REQUEST,
    val blocks: List<SchemaBlock>,
    /** A list of all test scenarios for this block */
    val tests: List<SchemaTest> = emptyList(),
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
    /**
     * Human-readable unit of the field (like 'V' for Volt or '%' for percentage).
     * */
    val unit: String = "",
    /**
     * Used to explicitly force the registers of multiple fields to be retrieved in a single Modbus request.
     * */
    val fetchGroup: String = "",
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
    val blocks: List<SchemaTestBlock>,
) {
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
) {
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
    val expected: Map<String, List<String>>,
) {
    override fun toString(): String = yaml.encodeToString(serializer(), this)
}

/**
 * Reformat the ModbusSchema yaml to look better and align the various parts to be better readable
 */
fun String.reformatModbusSchemaYaml(): String {
    return (this + "\n")
        .replace(Regex("(\n +- id:)"), "\n$1         ")
//        .replace(Regex("(\n +description:)"), "$1") // No replace needed
        .replace(Regex("(\n +immutable:)"), "$1  ")
        .replace(Regex("(\n +system:)"), "$1     ")
        .replace(Regex("(\n +expression:)"), "$1 ")
        .replace(Regex("(\n +unit:)"), "$1       ")
        .replace(Regex("(\n +fetchGroup:)"), "$1 ")
        .replace(Regex("(\n *blocks:\n)\n"), "\n$1")
        .replace(Regex("(\n *fields:\n)\n"), "\n$1")
        .replace(Regex("(\n *tests:\n)\n"), "\n$1")
        // Convert the expected values into a list
        // Single value
        .replace(Regex("(\n +'[^']+':)\n +- ('[^']*')\n"), "$1 [ $2 ]\n")
        .replace(Regex("(\n +'[^']+':)\n +- ('[^']*')\n"), "$1 [ $2 ]\n")
        // Secondary values (can be many)
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 5
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 10
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 15
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 20
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 25
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 30
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 35
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 40
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 45
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 50
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 55
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 60
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n")
        .replace(Regex("]\n +- ('[^']*')\n"), ", $1 ]\n") // 65 Worst case: A bitset of 64 bits that all have a value



        // Ok, this is nasty
        .replace(Regex("('[^']':) \\["),     "$1                         [")
        .replace(Regex("('[^']{2}':) \\["),  "$1                        [")
        .replace(Regex("('[^']{3}':) \\["),  "$1                       [")
        .replace(Regex("('[^']{4}':) \\["),  "$1                      [")
        .replace(Regex("('[^']{5}':) \\["),  "$1                     [")
        .replace(Regex("('[^']{6}':) \\["),  "$1                    [")
        .replace(Regex("('[^']{7}':) \\["),  "$1                   [")
        .replace(Regex("('[^']{8}':) \\["),  "$1                  [")
        .replace(Regex("('[^']{9}':) \\["),  "$1                 [")
        .replace(Regex("('[^']{10}':) \\["), "$1                [")
        .replace(Regex("('[^']{11}':) \\["), "$1               [")
        .replace(Regex("('[^']{12}':) \\["), "$1              [")
        .replace(Regex("('[^']{13}':) \\["), "$1             [")
        .replace(Regex("('[^']{14}':) \\["), "$1            [")
        .replace(Regex("('[^']{15}':) \\["), "$1           [")
        .replace(Regex("('[^']{16}':) \\["), "$1          [")
        .replace(Regex("('[^']{17}':) \\["), "$1         [")
        .replace(Regex("('[^']{18}':) \\["), "$1        [")
        .replace(Regex("('[^']{19}':) \\["), "$1       [")
        .replace(Regex("('[^']{20}':) \\["), "$1      [")
        .replace(Regex("('[^']{21}':) \\["), "$1     [")
        .replace(Regex("('[^']{22}':) \\["), "$1    [")
        .replace(Regex("('[^']{23}':) \\["), "$1   [")
        .replace(Regex("('[^']{24}':) \\["), "$1  [")
        .replace(Regex("('[^']{25}':) \\["), "$1 [")
}
