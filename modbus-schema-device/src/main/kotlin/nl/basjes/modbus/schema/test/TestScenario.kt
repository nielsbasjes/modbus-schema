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
package nl.basjes.modbus.schema.test

import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.utils.StringTable
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

private val LOG: Logger = LogManager.getLogger("TestResults")

class TestScenario(
    /**
     * The name of the test.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces) is a good choice.
     */
    val name: String,
    /**
     * Human-readable description of the test.
     */
    val description: String? = null
) {
    val registerBlocks: MutableList<RegisterBlock> = mutableListOf()
    val expectedBlocks: MutableList<ExpectedBlock> = mutableListOf()

    fun modbusDevice(): MockedModbusDevice {
        val deviceBuilder = MockedModbusDevice.builder()
        registerBlocks.forEach { deviceBuilder.withRegisters(it) }
        return deviceBuilder.build()
    }

    fun addRegisterBlock(registerBlock: RegisterBlock) {
        registerBlocks.add(registerBlock)
    }

    fun addExpectedBlock(block: ExpectedBlock) {
        expectedBlocks.add(block)
    }

    override fun toString(): String {
        return "TestScenario: '$name' => $description)"
    }

}

open class TestScenarioResults(
    val testName: String,
    val schemaDevice: SchemaDevice,
    val testResults: MutableMap<String, MutableMap<String, SchemaDevice.TestResult>>) {

    val allPassed: Boolean
        get() = testResults.isNotEmpty() && testResults.all { it.value.all { r -> r.value.passed } }

    fun toTable(onlyFailed: Boolean = false):String {
        val stringTable = StringTable()
            .withHeaders("Test", "Block", "Field", "Unit", "Expected", "Actual", "Good?")
        for ((blockId, results) in testResults) {
            for ((fieldId, testResult) in results) {
                if (!onlyFailed || !testResult.passed) {
                    stringTable.addRow(
                        testName,
                        blockId,
                        fieldId,
                        schemaDevice.getBlock(blockId)?.getField(fieldId)?.unit ?: "",
                        "\""+testResult.expectedValue+"\"",
                        "\""+testResult.actualValue+"\"",
                        testResult.passed.toString()
                    )
                }
            }
        }
        return stringTable.toString()
    }

}

class TestScenarioResultsList: ArrayList<TestScenarioResults>() {
    /**
     * Logs all the results
     * @return true if all passed, false if one or more failed.
     */
    fun logResults(): Boolean {
        var success = true
        for (result in this) {
            if (result.allPassed) {
                LOG.info("[PASS] Schema test \"${result.testName}\"")
//                LOG.info("\n${result.toTable()}")
            } else {
                LOG.error("[FAIL] Schema test \"${result.testName}\":")
                LOG.error("Failed fields:\n${result.toTable(true)}")
                success = false
            }
        }
        return success
    }

    val allPassed: Boolean
        get() = isEmpty() || none { !it.allPassed }

    val failedTests: List<String>
        get() = filter { !it.allPassed } .map { it.testName }
}

