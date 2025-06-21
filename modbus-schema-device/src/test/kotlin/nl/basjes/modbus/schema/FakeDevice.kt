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

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.schema.utils.DoubleCompare
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.TreeSet
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val LOG: Logger = LogManager.getLogger()

class AssertingMockedModbusDevice : MockedModbusDevice() {
    // Only COMPLETE sets of registers are allowed to be retrieved.
    // So if one register of a set is retrieved the ALL registers of that set MUST be retrieved
    private val registerSets: MutableList<RegisterBlock> = ArrayList()

    var failOnFetch = false
    var fetchErrors = false

    override fun getRegisters(
        firstRegister: Address,
        count: Int,
    ): RegisterBlock {
        assertFalse("Retrieving ${firstRegister}#$count when nothing should be fetched") { failOnFetch }

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

val deadAddress = Address.of("hr:139")
val deadSize = 2 // registers

fun createTestModbusDevice(
    maxRegistersPerModbusRequest: Int = 100,
): AssertingMockedModbusDevice {
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

fun createTestSchemaDevice(
    f1Group: String = "FG_f1",
    f2Group: String = "FG_f2",
    f3Group: String = "FG_f3",
    f4Group: String = "FG_f4",
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

fun assertCloseEnough(
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

