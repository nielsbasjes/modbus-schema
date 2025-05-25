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

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.fetcher.RegisterBlockFetcher.FetchBatch
import nl.basjes.modbus.schema.get
import nl.basjes.modbus.schema.utils.DoubleCompare
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TestScenarioRegisterBlockFetcher {
    class AssertingMockedModbusDevice : MockedModbusDevice() {
        // Only COMPLETE sets of registers are allowed to be retrieved.
        // So if one register of a set is retrieved the ALL registers of that set MUST be retrieved
        private val registerSets: MutableList<RegisterBlock> = ArrayList()

        var fetchErrors = false

        override fun getRegisters(
            firstRegister: Address,
            count: Int,
        ): RegisterBlock {
            val allAddressesToBeRetrieved: MutableList<Address> = ArrayList()
            for (i in 0 until count) {
                allAddressesToBeRetrieved.add(firstRegister.increment(i))
            }

            val allAddressesThatShouldBeRetrieved: MutableSet<Address> = TreeSet()
            for (address in allAddressesToBeRetrieved) {
                for (registerSet in registerSets) {
                    allAddressesThatShouldBeRetrieved.addAll(registerSet.keys)
                }
            }

            for (address in allAddressesThatShouldBeRetrieved) {
                if (!(allAddressesToBeRetrieved.contains(address))) {
                    LOG.error("For {}#{} should retrieve also {}", firstRegister, count, address)
                    fetchErrors = true
                }
            }

            return super.getRegisters(firstRegister, count)
        }
    }

    private val deadAddress = Address.of("hr:139")
    private val deadSize = 2 // registers

    private fun createTestModbusDevice(maxRegistersPerModbusRequest: Int): AssertingMockedModbusDevice {
        val modbusDevice = AssertingMockedModbusDevice()
        modbusDevice.maxRegistersPerModbusRequest = maxRegistersPerModbusRequest
        modbusDevice.logRequests = true
        modbusDevice.addRegisters(
            AddressClass.HOLDING_REGISTER,
            101,
            """
            # abcdefghijklmnopqrstuvwxyz
            6162 6364 6566 6768 696a 6b6c 6d6e 6f70 7172 7374 7576 7778 797a
            # 0123456789
            3031 3233 3435 3637 3839
            # A deliberate gap without data and a read error
            xxxx ----
            # ABCDEFGHIJKLMNOPQRSTUVWXYZ
            4142 4344 4546 4748 494a 4b4c 4d4e 4f50 5152 5354 5556 5758 595a
            # 0123456789
            3031 3233 3435 3637 3839
            # A deliberate gap with bad data (666) at deadAddress
            029A 029A
            3039    # The number 12345
            000A    # The number 10

            2B67    // The number 11111
            """,
        )
        return modbusDevice
    }

    private fun createTestSchemaDevice(
        f1Group: String,
        f2Group: String,
        f3Group: String,
        f4Group: String,
    ): SchemaDevice {
        val schemaDevice = SchemaDevice("First test device")

        val block = Block(schemaDevice, "Block1", "Block 1")

        // Field names are chosen to have a different logical (by required registers) from a sorted by name ordering.
        // A field registers itself with the mentioned Block
        Field(block, "Some", expression = "utf8( hr:101 # 13)", fetchGroup = f1Group)
        Field(block, "Field", expression = "utf8( hr:114 # 5)", fetchGroup = f2Group, immutable = true)
        Field(block, "Dead", expression = "int32( hr:119 # 2)")
        Field(block, "And", expression = "utf8( hr:121 # 13)", fetchGroup = f3Group)
        Field(block, "Another", expression = "utf8( hr:134 # 5)", fetchGroup = f4Group, immutable = true)
        Field(block, "Value1", expression = "int16( hr:141 ) / Scale1")
        Field(block, "Scale1", expression = "int16( hr:142 ) ", immutable = true)
        Field(block, "Value2", expression = "int16( hr:143 ) / Scale2")
        Field(block, "Scale2", expression = "100")

        schemaDevice.initialize()
        return schemaDevice
    }

    private fun assertCloseEnough(
        value1: Double?,
        value2: Double?,
    ) {
        assertNotNull(value1)
        assertNotNull(value2)
        assertTrue(
            DoubleCompare.closeEnough(value1, value2),
            "Values $value1 and $value2  are not close enough together.",
        )
    }

    fun assertCorrectFieldValues(schemaDevice: SchemaDevice) {
        val block = schemaDevice.getBlock("Block1")
        assertNotNull(block)

        assertEquals(
            "abcdefghijklmnopqrstuvwxyz",
            block.getField("Some")?.stringValue,
            "Field `Some`",
        )
        assertEquals("0123456789", block.getField("Field")?.stringValue, "Field `Field`")
        assertEquals(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            block.getField("And")!!.stringValue,
            "Field `And`",
        )
        assertEquals("0123456789", block.getField("Another")?.stringValue, "Field `Another`")

        assertNull(block.getField("Dead")?.longValue, "Field `Dead`")

        assertEquals(10, block.getField("Scale1")?.longValue, "Field `Scale1`")
        assertCloseEnough(1234.5, block.getField("Value1")?.doubleValue)
        assertEquals(100, block.getField("Scale2")?.longValue, "Field `Scale2`")
        assertCloseEnough(111.11, block.getField("Value2")?.doubleValue)
    }

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

        fetcher.update(0)

        assertCorrectFieldValues(schemaDevice)
        assertFalse(modbusDevice.fetchErrors, "There were problems fetching the registers")

        // ----------------------------------------------------
        // Second fetch. All fields have been loaded; only the mutable fields should be fetched
        TimeUnit.MILLISECONDS.sleep(10) // Needed to cleanly separate the first and second fetch

        fetchBatches = fetcher.calculateFetchBatches(0)
        LOG.warn("Fetch Batches (Update): {}", fetchBatches)
        fetcher.update(0)

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
