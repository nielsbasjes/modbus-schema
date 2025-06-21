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
package nl.basjes.modbus.schema.fetcher

import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.assertCorrectFieldValues
import nl.basjes.modbus.schema.createTestModbusDevice
import nl.basjes.modbus.schema.createTestSchemaDevice
import nl.basjes.modbus.schema.deadAddress
import nl.basjes.modbus.schema.deadSize
import nl.basjes.modbus.schema.fetcher.RegisterBlockFetcher.FetchBatch
import nl.basjes.modbus.schema.get
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestScenarioRegisterBlockFetcher {

    @ParameterizedTest(name = "Default Fetcher {index}: {arguments}")
    @MethodSource("deviceParameters")
    fun verifyDefaultFetcher(
        maxRegistersPerModbusRequest: Int,
        fg1: String,
        fg2: String,
        fg3: String,
        fg4: String,
    ) {
        val modbusDevice = createTestModbusDevice(maxRegistersPerModbusRequest)
        val schemaDevice = createTestSchemaDevice(fg1, fg2, fg3, fg4)
        schemaDevice.needAll()

        val fetcher = RegisterBlockFetcher(schemaDevice, modbusDevice)

        schemaDevice.blocks
            .map(Block::fields)
            .flatten()
            .forEach { field: Field ->
                val parsedExpression = field.parsedExpression
                assertNotNull(parsedExpression)
                assertTrue(
                    parsedExpression.problems.isEmpty(),
                    "Invalid expression for $field",
                )
            }

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch
        var fetchBatches: List<FetchBatch> = fetcher.calculateFetchBatches(0)
        LOG.warn("Fetch Batches (Clean ): {}", fetchBatches)

        fetcher.update()

        assertCorrectFieldValues(schemaDevice)
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // ----------------------------------------------------
        // Second fetch. All fields have been loaded; only the mutable fields should be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch

        fetchBatches = fetcher.calculateFetchBatches(0)
        LOG.warn("Fetch Batches (Update): {}", fetchBatches)
        fetcher.update()

        assertCorrectFieldValues(schemaDevice)
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // Only the requested registers should be fetched
        val deadInput = modbusDevice.getRegisters(deadAddress, deadSize)
        assertEquals(666, deadInput[deadAddress].value)

        val fetchedRegisters = schemaDevice.getRegisterBlock(deadAddress.addressClass)
        assertNull(fetchedRegisters[deadAddress].value)
        assertNull(fetchedRegisters[deadAddress.increment()].value)
    }

    @ParameterizedTest(name = "Optimized Fetcher (GAP:true) {index}: {arguments}")
    @MethodSource("deviceParameters")
    fun verifyOptimizedFetcherReadGaps(
        maxRegister: Int,
        fg1: String,
        fg2: String,
        fg3: String,
        fg4: String,
    ) {
        verifyOptimizedFetcher(10, maxRegister, fg1, fg2, fg3, fg4)
    }

    @ParameterizedTest(name = "Optimized Fetcher (GAP:false) {index}: {arguments}")
    @MethodSource("deviceParameters")
    fun verifyOptimizedFetcherDoNOTReadGaps(
        maxRegistersPerModbusRequest: Int,
        fg1: String,
        fg2: String,
        fg3: String,
        fg4: String,
    ) {
        verifyOptimizedFetcher(0, maxRegistersPerModbusRequest, fg1, fg2, fg3, fg4)
    }

    @Throws(ModbusException::class)
    fun verifyOptimizedFetcher(
        readingGap: Int,
        maxRegistersPerModbusRequest: Int,
        fg1: String,
        fg2: String,
        fg3: String,
        fg4: String,
    ) {
        val modbusDevice = createTestModbusDevice(maxRegistersPerModbusRequest)
        val schemaDevice = createTestSchemaDevice(fg1, fg2, fg3, fg4)

        val fetcher = OptimizingRegisterBlockFetcher(schemaDevice, modbusDevice)
        fetcher.allowedGapReadSize = readingGap
        schemaDevice.registerBlockFetcher = fetcher

        // First verify if partial fetching works
        val value1 = schemaDevice["Block1"]["Value1"]
        assertNotNull(value1, "Unable to find Value1")
        value1.update()
        val value1result = value1.doubleValue
        assertNotNull(value1result, "Unable to find value1")
        assertEquals(1234.5, value1result, 0.0001)

        val value2 = schemaDevice["Block1"]["Value2"]
        assertNotNull(value2, "Unable to find Value2")
        value2.update()
        val value2result = value2.doubleValue
        assertNotNull(value2result, "Unable to find value1")
        assertEquals(111.11, value2result, 0.0001)

        schemaDevice.needAll()

        // ----------------------------------------------------
        // First fetch. Nothing loaded yet: All must be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch
        var fetchBatches: List<FetchBatch> = fetcher.calculateFetchBatches(0)
        LOG.warn("Fetch Batches (Clean ): {}", fetchBatches)

        fetcher.update()

        assertCorrectFieldValues(schemaDevice)
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // ----------------------------------------------------
        // Second fetch. All fields have been loaded; only the mutable fields should be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch

        fetchBatches = fetcher.calculateFetchBatches(0)
        LOG.warn("Fetch Batches (Update): {}", fetchBatches)
        fetcher.update()

        assertCorrectFieldValues(schemaDevice)
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        if (readingGap == 0) {
            // ----------------------------------------------------
            // If NO gaps are allowed the null fields in the source must NOT have been fetched
            // Only the requested registers should be fetched
            val deadInput = modbusDevice.getRegisters(deadAddress, deadSize)
            assertEquals(666, deadInput[deadAddress].value)

            val fetchedRegisters = schemaDevice.getRegisterBlock(deadAddress.addressClass)
            assertNull(fetchedRegisters[deadAddress].value)
            assertNull(fetchedRegisters[deadAddress.increment()].value)
        }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()

        @JvmStatic
        fun deviceParameters(): Stream<Arguments> =
            Stream.of(
                Arguments.of(15, "a", "b", "c", "d"),
                Arguments.of(20, "a", "b", "c", "d"),
                Arguments.of(25, "a", "b", "c", "d"),
                Arguments.of(30, "a", "b", "c", "d"),
                Arguments.of(35, "a", "b", "c", "d"),
                Arguments.of(40, "a", "b", "c", "d"),
                Arguments.of(45, "a", "b", "c", "d"),

                Arguments.of(15, "a", "b", "c", "c"),
                Arguments.of(20, "a", "b", "c", "c"),
                Arguments.of(25, "a", "b", "c", "c"),
                Arguments.of(30, "a", "b", "c", "c"),
                Arguments.of(35, "a", "b", "c", "c"),
                Arguments.of(40, "a", "b", "c", "c"),
                Arguments.of(45, "a", "b", "c", "c"),

                Arguments.of(15, "a", "a", "c", "d"),
                Arguments.of(20, "a", "a", "c", "d"),
                Arguments.of(25, "a", "a", "c", "d"),
                Arguments.of(30, "a", "a", "c", "d"),
                Arguments.of(35, "a", "a", "c", "d"),
                Arguments.of(40, "a", "a", "c", "d"),
                Arguments.of(45, "a", "a", "c", "d"),

                Arguments.of(15, "a", "a", "c", "c"),
                Arguments.of(20, "a", "a", "c", "c"),
                Arguments.of(25, "a", "a", "c", "c"),
                Arguments.of(30, "a", "a", "c", "c"),
                Arguments.of(35, "a", "a", "c", "c"),
                Arguments.of(40, "a", "a", "c", "c"),
                Arguments.of(45, "a", "a", "c", "c"),
            )
    }
}
