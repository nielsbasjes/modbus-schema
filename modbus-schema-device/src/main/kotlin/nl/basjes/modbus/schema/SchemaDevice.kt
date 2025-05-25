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

import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.MODBUS_MAX_REGISTERS_PER_REQUEST
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.fetcher.OptimizingRegisterBlockFetcher
import nl.basjes.modbus.schema.fetcher.RegisterBlockFetcher
import nl.basjes.modbus.schema.test.ExpectedBlock
import nl.basjes.modbus.schema.test.TestScenario
import nl.basjes.modbus.schema.test.TestScenarioResults
import nl.basjes.modbus.schema.test.TestScenarioResultsList
import nl.basjes.modbus.schema.utils.StringTable
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TreeMap

open class SchemaDevice
@JvmOverloads
constructor(
    /**
     * A human-readable description of this schema device.
     */
    var description: String = "",
    /**
     * The maximum number of modbus registers that can be requested PER call.
     * Some devices do not allow the normal max of 125.
     */
    maxRegistersPerModbusRequest: Int = MODBUS_MAX_REGISTERS_PER_REQUEST,
) {
    // The registerBlock from which the values must be retrieved.
    private val registerBlocks: MutableMap<AddressClass, RegisterBlock> = TreeMap()

    /**
     * The maximum number of modbus registers that can be requested PER call.
     * Some devices do not allow the normal max of 125.
     */
    var maxRegistersPerModbusRequest = maxRegistersPerModbusRequest
        set(value) {
            if (value < 1 || value > MODBUS_MAX_REGISTERS_PER_REQUEST) {
                throw ModbusException(
                    "The maxRegistersPerModbusRequest must be between 1 and $MODBUS_MAX_REGISTERS_PER_REQUEST" +
                        " (was set to $value).",
                )
            }
            field = value
        }

    private fun clearRegisterBlocks() {
        registerBlocks.values.forEach { it.clear() }
    }

    fun getRegisterBlock(addressClass: AddressClass): RegisterBlock =
        registerBlocks.computeIfAbsent(addressClass) { addrCls ->
            RegisterBlock(addrCls)
        }

    // ------------------------------------------

    /**
     * The Modbus Schema level of the schema device.
     * A Modbus Schema level is the set of available converter methods, return types, etc.
     * - A schema device uses a set of converter methods (i.e. the version)
     * - A code generator supports a set of converter methods.
     * A schema device with level 5 will work with a code generator that supports level 10
     * A schema device with level 10 will NOT work with a code generator that supports level 5
     */
    val schemaFeatureLevel = CURRENT_SCHEMA_FEATURE_LEVEL

    // ------------------------------------------

    /**
     * The set of fields defined in this schema device.
     */
    private val mutableBlocks: MutableList<Block> = mutableListOf()
    private val mutableBlocksMap: MutableMap<String, Block> = mutableMapOf()

    val blocks: List<Block> = mutableBlocks

    fun addBlock(newBlock: Block) {
        if (!mutableBlocks.contains(newBlock)) {
            mutableBlocks.add(newBlock)
            mutableBlocksMap.putIfAbsent(newBlock.id, newBlock)
        }
    }

    fun getBlock(blockId: String): Block? = mutableBlocksMap[blockId]

    /**
     * In some templates it is convenient to have the length of the longest block id.
     */
    val maxBlockIdLength get() = mutableBlocksMap.keys.maxOfOrNull { it.length } ?: 0

    /** If a field was added or removed this should trigger updates and reinitializations in other parts */
    var lastFieldModificationTimestamp: Instant = Instant.now()

    fun aFieldWasChanged() {
        lastFieldModificationTimestamp = Instant.now()
    }

    /**
     * Verify the basics
     */
    fun initialize(): Boolean {
        var allOk = true
        for (block in blocks) {
            if (!block.initialize()) {
                allOk = false
            }
        }
        return allOk
    }

    /**
     * Verify the basics
     */
    @Throws(ModbusException::class)
    fun initializeAndVerify(): Boolean {
        if (!initialize()) {
            return false
        }

        val results = verifyProvidedTests()
        if (!results.allPassed) {
            LOG.error("Logical device initialization: These Schema tests failed: ${results.failedTests}.")
            results.logResults()
            return false
        }

        return true
    }

    fun initializationProblems(): String {
        val stringTable = StringTable()
        stringTable.withHeaders("Block", "Field", "Problem")
        blocks.forEach { block ->
            run {
                block.fields.forEach { field ->
                    run {
                        val parsedExpression = field.parsedExpression
                        if (parsedExpression == null) {
                            stringTable.addRow(block.id, field.id, "Unable to parse the expression")
                        } else {
                            val problemList = parsedExpression.problems
                            if (problemList.isNotEmpty()) {
                                stringTable.addRow(block.id, field.id, problemList.joinToString(separator = ", "))
                            }
                        }
                    }
                }
            }
        }
        return stringTable.toString()
    }

    // ------------------------------------------

    var modbusDevice: ModbusDevice? = null
    var registerBlockFetcher: RegisterBlockFetcher? = null

    fun connectBase(modbusDevice: ModbusDevice): SchemaDevice {
        clearRegisterBlocks()
        this.modbusDevice = modbusDevice
        modbusDevice.maxRegistersPerModbusRequest = maxRegistersPerModbusRequest
        this.registerBlockFetcher = RegisterBlockFetcher(this, modbusDevice)
        return this
    }

    @JvmOverloads
    fun connect(
        modbusDevice: ModbusDevice,
        /**
         * How many registers may needlessly be read to optimize fetching
         */
        allowedGapReadSize: Int = 10,
    ): SchemaDevice {
        clearRegisterBlocks()
        this.modbusDevice = modbusDevice
        modbusDevice.maxRegistersPerModbusRequest = maxRegistersPerModbusRequest
        this.registerBlockFetcher = OptimizingRegisterBlockFetcher(this, modbusDevice)
        (this.registerBlockFetcher as OptimizingRegisterBlockFetcher).allowedGapReadSize = allowedGapReadSize
        return this
    }

    /**
     * Update all registers related to the needed fields to be updated with a maximum age of the provided milliseconds
     * @param maxAge maximum age of the fields in milliseconds
     */
    @JvmOverloads
    fun update(maxAge: Long = 0) {
        registerBlockFetcher?.update(maxAge)
    }

    /**
     * Update all registers related to the specified field
     * @param field the Field that must be updated
     */
    fun update(field: Field) {
        registerBlockFetcher?.update(field)
    }

    /**
     * Make sure all registers mentioned in all known fields are retrieved.
     */
    @Throws(ModbusException::class)
    @JvmOverloads
    fun updateAll(maxAge: Long = 0) {
        initialize()
        needAll()
        update(maxAge)
        unNeedAll()
    }

    /**
     * @param field The field that must be kept up-to-date
     */
    fun need(field: Field) {
        field.need()
    }

    /**
     * @param field The field that no longer needs to be kept up-to-date
     */
    fun unNeed(field: Field) {
        field.unNeed()
    }

    /**
     * We want all fields to be kept up-to-date
     */
    fun needAll() {
        blocks.forEach { it.needAll() }
    }

    /**
     * We no longer want all fields to be kept up-to-date
     */
    fun unNeedAll() {
        blocks.forEach { it.unNeedAll() }
    }

    /**
     * Get the list of needed fields
     */
    fun neededFields() = blocks.flatMap { it.neededFields() }

    // ------------------------------------------

    /**
     * The set of test scenarios defined for this logicaldevice.
     */
    private val mutableTests: MutableList<TestScenario> = mutableListOf()
    val tests: List<TestScenario> = mutableTests

    fun addTestScenario(testScenario: TestScenario) {
        mutableTests.add(testScenario)
    }

    fun createTestsUsingCurrentRealData() {
        var oldestTimestampOfData = 0L
        for (registerBlock in registerBlocks.values) {
            oldestTimestampOfData =
                oldestTimestampOfData
                    .coerceAtLeast(
                        registerBlock.values
                            .map(RegisterValue::fetchTimestamp)
                            .filter { it > 0 }
                            .minOrNull() ?: 0L,
                    )
        }

        val dateTimeFormatterCompact =
            DateTimeFormatter
                .ofPattern("_uuuu_MM_dd_HH_mm_ss")
                .withZone(ZoneOffset.UTC)

        val dateTimeFormatterISO =
            DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)

        val oldestInstantOfData = Instant.ofEpochMilli(oldestTimestampOfData)

        val name = "TestAt" + dateTimeFormatterCompact.format(oldestInstantOfData)
        val description =
            "Test generated from device data at " +
                dateTimeFormatterISO.format(oldestInstantOfData)

        val test = TestScenario(name, description)
        mutableTests.add(test)

        for (registerBlock in registerBlocks.values) {
            test.addRegisterBlock(registerBlock.clone())
        }

        for (block in blocks) {
            val expectedBlock = ExpectedBlock(block.id)
            test.addExpectedBlock(expectedBlock)
            for (field in block.fields) {
                expectedBlock.addExpectation(field.id, field.testCompareValue)
            }
        }
    }

    data class TestResult(
        val expectedValue: List<String>,
        val actualValue: List<String>,
        @get:JvmName("isPassed")
        val passed: Boolean,
    )

    /**
     * Check if the basic parameters of the defined testcases are valid without actually running the tests.
     */
    @Throws(ModbusException::class)
    fun verifyProvidedTests(): TestScenarioResultsList {
        val allTestResults = TestScenarioResultsList()
        for (test in tests) {
            val testResults: MutableMap<String, MutableMap<String, TestResult>> = mutableMapOf()
            allTestResults.add(TestScenarioResults(test.name, this, testResults))

            // First we set all the TEST registers in the schema device
            clearRegisterBlocks() // Wipe anything old
            // Then we load all provided register values for this test
            for (testRegisterBlock in test.registerBlocks) {
                this.getRegisterBlock(testRegisterBlock.addressClass).merge(testRegisterBlock)
            }

            // Then for each block as defined in the test scenario
            for (expectedBlock in test.expectedBlocks) {
                val blockId = expectedBlock.blockId
                val block = getBlock(blockId)
                requireNotNull(block) {
                    "There are expectations for the blockid \"${expectedBlock.blockId}\" in the test \"${test.name}\" which does not exist."
                }
                testResults[blockId] = mutableMapOf()
                val blockTestResult = testResults[blockId]!!
                for ((fieldId, expectedValue) in expectedBlock.expected) {
                    val field = block.getField(fieldId)
                    requireNotNull(field) {
                        "For block '${block.id}' an expected value was specified for a non existent field '$fieldId'"
                    }

                    val actualValue = field.testCompareValue
                    blockTestResult[fieldId] =
                        TestResult(expectedValue, actualValue, expectedValue == actualValue)
                }
            }
        }
        // Finally ensure no test registers remain in the schema device
        clearRegisterBlocks()

        return allTestResults
    }


    override fun toString(): String = toTable(false)

    fun resolveAllImmutableFields() {
        val immutableFields =
            blocks
                .flatMap { block -> block.fields }
                .filter { it.isImmutable }
                .toList()
        immutableFields.forEach { it.need() }
        update()
        immutableFields.forEach { it.unNeed() }
    }

    companion object {
        @JvmStatic
        fun builder(): SchemaDeviceBuilder = SchemaDeviceBuilder()

        const val CURRENT_SCHEMA_FEATURE_LEVEL = 1

        private val LOG: Logger = LogManager.getLogger()
    }

    open class SchemaDeviceBuilder {
        /**
         * A human-readable description of this schema device.
         */
        fun description(description: String) = apply { this.description = description }

        var description: String = ""
            private set

        /**
         * The maximum number of registers per request is different for some devices
         */
        fun maxRegistersPerModbusRequest(maxRegistersPerModbusRequest: Int) =
            apply { this.maxRegistersPerModbusRequest = maxRegistersPerModbusRequest }

        var maxRegistersPerModbusRequest: Int = MODBUS_MAX_REGISTERS_PER_REQUEST
            private set

        /**
         * Build the SchemaDevice, throws IllegalArgumentException if something is wrong
         */
        fun build(): SchemaDevice = SchemaDevice(description, maxRegistersPerModbusRequest)
    }
}

operator fun SchemaDevice?.get(blockId: String): Block? = this?.getBlock(blockId)
