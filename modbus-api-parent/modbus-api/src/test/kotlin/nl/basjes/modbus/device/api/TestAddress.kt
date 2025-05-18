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
package nl.basjes.modbus.device.api

import nl.basjes.modbus.device.api.AddressClass.COIL
import nl.basjes.modbus.device.api.AddressClass.DISCRETE_INPUT
import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.AddressClass.INPUT_REGISTER
import nl.basjes.modbus.device.api.FunctionCode.Companion.forReading
import nl.basjes.modbus.device.api.FunctionCode.Companion.forWritingMultiple
import nl.basjes.modbus.device.api.FunctionCode.Companion.forWritingSingle
import nl.basjes.modbus.device.api.FunctionCode.WRITE_MULTIPLE_COILS
import nl.basjes.modbus.device.api.FunctionCode.WRITE_MULTIPLE_HOLDING_REGISTERS
import nl.basjes.modbus.device.api.FunctionCode.WRITE_SINGLE_COIL
import nl.basjes.modbus.device.api.FunctionCode.WRITE_SINGLE_HOLDING_REGISTER
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.assertThrows
import kotlin.collections.mapNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TestAddress {
    private fun checkAllAreEqual(
        expectedAddress: Address,
        vararg registerTags: String,
    ) {
        checkAllAreEqualInternal(expectedAddress, *registerTags)

        // Now convert the provided address to the Modicon 5 format and do the same again
        val modicon5 = expectedAddress.toModicon5()
        if (modicon5 != null) {
            val modicon5Address1 = modicon5.asAddress()
            assertEquals(expectedAddress, modicon5Address1, "Found different addresses (Modicon 5 string round trip)")
            checkAllAreEqualInternal(modicon5Address1, *registerTags)

            val modicon5Address2 = modicon5.toInt().asAddressAssumingModicon5()
            assertEquals(expectedAddress, modicon5Address2, "Found different addresses (Modicon 5 int round trip)")
            checkAllAreEqualInternal(modicon5Address2, *registerTags)
        }

        // Now convert the provided address to the Modicon 6 format and do the same again
        val modicon6 = expectedAddress.toModicon6()
        val modicon6Address1 = modicon6.asAddress()
        assertEquals(expectedAddress, modicon6Address1, "Found different addresses (Modicon 6 string round trip)")
        checkAllAreEqualInternal(modicon6Address1, *registerTags)

        val modicon6Address2 = modicon6.toInt().asAddressAssumingModicon6()
        assertEquals(expectedAddress, modicon6Address2, "Found different addresses (Modicon 6 int round trip)")
        checkAllAreEqualInternal(modicon6Address2, *registerTags)

        // Now convert the provided address to the Modicon X format and do the same again
        val modiconX = expectedAddress.toModiconX()
        val modiconXAddress = modiconX.asAddress()
        assertEquals(expectedAddress, modiconXAddress, "Found different addresses (Modicon X string round trip)")
        checkAllAreEqualInternal(modicon6Address1, *registerTags)

        // Now convert the provided address to the normal String format and do the same again
        val stringFormat = expectedAddress.toString()
        val stringAddress = stringFormat.asAddress()
        assertEquals(expectedAddress, stringAddress, "Found different addresses (toString round trip)")
        checkAllAreEqualInternal(stringAddress, *registerTags)

        // Now convert the provided address to the normal String format and do the same again
        val cleanFormat = expectedAddress.toCleanFormat()
        val cleanAddress = cleanFormat.asAddress()
        assertEquals(expectedAddress, cleanAddress, "Found different addresses (Clean Format round trip)")
        checkAllAreEqualInternal(cleanAddress, *registerTags)
    }

    private fun checkAllAreEqualInternal(
        expectedAddress: Address,
        vararg registerTags: String,
    ) {
        assertNotNull(expectedAddress)
        for (registerTag in registerTags) {
            val address = Address.of(registerTag)
            assertNotNull(address)
            assertEquals(expectedAddress, address, "Found different addresses")

            val addressString = address.toString()
            val fromString = Address.of(addressString)
            assertEquals(address, fromString, "Round trip via toString is different")
            LOG.info("{}", String.format("Register string %-25s --> %s", registerTag, addressString))
        }
    }

    @Test
    fun checkAddressParsingAndComparing() {
        // A coil at address 4321.
        checkAllAreEqual(
            Address.of(COIL, 4321),
            "coil:4321",
            "c:000000004321",
            // Old style using register number instead of physical address
            "004322",
            "0x000004322",
            "04322",
            "0x4322",
        )

        // A discrete input at address 4321.
        checkAllAreEqual(
            Address.of(DISCRETE_INPUT, 4321),
            "discrete-input:4321",
            "di:000004321",
            // Old style using register number instead of physical address
            "104322",
            "1x000004322",
            "14322",
            "1x4322",
        )

        // An input register at address 1234.
        checkAllAreEqual(
            Address.of(INPUT_REGISTER, 1234),
            "input-register:1234",
            "ir:000000001234",
            // Old style using register number instead of physical address
            "301235",
            "3x000001235",
            "31235",
            "3x1235",
        )

        // A holding register at address 20.
        checkAllAreEqual(
            Address.of(HOLDING_REGISTER, 20),
            "holding-register:20",
            "hr:0000020",
            // Old style using register number instead of physical address
            "400021",
            "4x000000021",
            "40021",
            "4x0021",
        )

        // A holding register at address 5678.
        checkAllAreEqual(
            Address.of(HOLDING_REGISTER, 5678),
            "holding-register:5678",
            "hr:000005678",
            // Old style using register number instead of physical address
            "405679",
            "4x000005679",
            "45679",
            "4x5679",
        )

        // A holding register number 9999 (last possible modicon 5).
        assertEquals("49999", Address.of("409999").toModicon5())
        checkAllAreEqual(
            Address.of(HOLDING_REGISTER, 9998),
            "holding-register:9998",
            "hr:000009998",
            // Old style using register number instead of physical address
            "409999",
            "4x000009999",
            "49999",
            "4x9999",
        )

        // A holding register number 10000 (NOT possible modicon 5).
        assertNull(Address.of("410000").toModicon5())
        checkAllAreEqual(
            Address.of(HOLDING_REGISTER, 9999),
            "holding-register:9999",
            "hr:000009999",
            // Old style using register number instead of physical address
            "410000",
            "4x0000010000",
            "4x10000",
        )

        // A holding register at address 12345 (i.e. > 9999 so NO modicon 5 digit possible!).
        assertNull(Address.of("412346").toModicon5())
        checkAllAreEqual(
            Address.of(HOLDING_REGISTER, 12345),
            "holding-register:12345",
            "hr:0000012345",
            // Old style using register number instead of physical address
            "4x0000012346",
            "412346",
            "4x12346",
        )
    }

    @Test
    fun checkFunctionCodeRetrieval() {
        // Reading
        assertEquals(FunctionCode.READ_COIL,                forReading(COIL))
        assertEquals(FunctionCode.READ_DISCRETE_INPUT,      forReading(DISCRETE_INPUT))
        assertEquals(FunctionCode.READ_INPUT_REGISTERS,     forReading(INPUT_REGISTER))
        assertEquals(FunctionCode.READ_HOLDING_REGISTERS,   forReading(HOLDING_REGISTER))

        // Writing to a readonly address class is not allowed
        assertThrows<IllegalArgumentException> { forWritingSingle(DISCRETE_INPUT) }
        assertThrows<IllegalArgumentException> { forWritingSingle(INPUT_REGISTER) }
        assertThrows<IllegalArgumentException> { forWritingMultiple(DISCRETE_INPUT) }
        assertThrows<IllegalArgumentException> { forWritingMultiple(INPUT_REGISTER) }

        // Write one
        assertEquals(WRITE_SINGLE_COIL,                     forWritingSingle(COIL))
        assertEquals(WRITE_SINGLE_HOLDING_REGISTER,         forWritingSingle(HOLDING_REGISTER))

        // Write many
        assertEquals(WRITE_MULTIPLE_COILS,                  forWritingMultiple(COIL))
        assertEquals(WRITE_MULTIPLE_HOLDING_REGISTERS,      forWritingMultiple(HOLDING_REGISTER))
    }

    @Test
    fun checkAddressClassRetrieval() {
        assertEquals(COIL, AddressClass.of(0))
        assertEquals(COIL, AddressClass.of(" 0"))
        assertEquals(COIL, AddressClass.of("coil"))
        assertEquals(COIL, AddressClass.of("coils"))

        assertEquals(DISCRETE_INPUT, AddressClass.of(1))
        assertEquals(DISCRETE_INPUT, AddressClass.of(" 1 "))
        assertEquals(DISCRETE_INPUT, AddressClass.of("discrete input"))
        assertEquals(DISCRETE_INPUT, AddressClass.of("Discrete Inputs"))
        assertEquals(DISCRETE_INPUT, AddressClass.of("discrete-input"))
        assertEquals(DISCRETE_INPUT, AddressClass.of("discrete-inPuts"))
        assertEquals(DISCRETE_INPUT, AddressClass.of("discrete_input"))
        assertEquals(DISCRETE_INPUT, AddressClass.of("discrete_inputs"))

        assertEquals(INPUT_REGISTER, AddressClass.of(3))
        assertEquals(INPUT_REGISTER, AddressClass.of("3 "))
        assertEquals(INPUT_REGISTER, AddressClass.of("input register"))
        assertEquals(INPUT_REGISTER, AddressClass.of("input registers"))
        assertEquals(INPUT_REGISTER, AddressClass.of("  input-register"))
        assertEquals(INPUT_REGISTER, AddressClass.of("inpUT-registers  "))
        assertEquals(INPUT_REGISTER, AddressClass.of("input_regISter"))
        assertEquals(INPUT_REGISTER, AddressClass.of("input_regisTERs "))

        assertEquals(HOLDING_REGISTER, AddressClass.of(4))
        assertEquals(HOLDING_REGISTER, AddressClass.of(" 4"))
        assertEquals(HOLDING_REGISTER, AddressClass.of("holding register"))
        assertEquals(HOLDING_REGISTER, AddressClass.of("holding registers"))
        assertEquals(HOLDING_REGISTER, AddressClass.of("  holding-REGister"))
        assertEquals(HOLDING_REGISTER, AddressClass.of("holding-registers  "))
        assertEquals(HOLDING_REGISTER, AddressClass.of("HOLDing_register"))
        assertEquals(HOLDING_REGISTER, AddressClass.of("holding_registers"))
    }

    @Test
    fun testDistance() {
        assertEquals(null, Address.of("hr:0123").distance(Address.of("ir:0123")))
        assertEquals(0, Address.of("hr:0123").distance(Address.of("hr:0123")))
        assertEquals(23, Address.of("hr:0100").distance(Address.of("hr:0123")))
        assertEquals(-23, Address.of("hr:0123").distance(Address.of("hr:0100")))

        val list =
            listOf(
                Address.of(" hr:00217"),
                Address.of(" hr:00218"),
                Address.of(" hr:00219"),
                Address.of(" hr:00220"),
                Address.of(" hr:00221"),
                Address.of(" hr:00222"),
                Address.of(" hr:00223"),
                Address.of(" hr:00224"),
                Address.of(" hr:00225"),
                Address.of(" hr:00226"),
                Address.of(" hr:00227"),
                Address.of(" hr:00228"),
                Address.of(" hr:00229"),
                Address.of(" hr:00230"),
                Address.of(" hr:00231"),
                Address.of(" hr:00232"),
                Address.of(" hr:00233"),
                Address.of(" hr:00234"),
                Address.of(" hr:00235"),
                Address.of(" hr:00236"),
                Address.of(" hr:00237"),
                Address.of(" hr:00238"),
            )

        val firstAddress = Address.of("hr:00199")

        assertTrue {
            list.overlaps(firstAddress, 41)
        }
    }

    fun List<Address>.overlaps(
        firstAddress: Address,
        count: Int,
    ): Boolean {
        if (this.isEmpty()) {
            return false
        }
        require(count > 0) { "At least one address is required" }

        return this
            .mapNotNull { firstAddress.distance(it) }
            .any { it in 0..count }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()
    }
}
