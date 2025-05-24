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
package nl.basjes.modbus.schema.expression

import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.of
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestScenarioExpressions {

    data class TestField(
        val name: String,
        val expression: String,
    )

    @Throws(ModbusException::class)
    private fun buildVerifier(
        bytes: String,
        vararg fields: TestField,
    ): SchemaDevice {
        val modbusDevice = MockedModbusDevice()
        //        modbusDevice.setLogRequests(true);
        modbusDevice.addRegisters(HOLDING_REGISTER, 0, bytes)

        val schemaDevice = SchemaDevice("Device")
        val block = Block(schemaDevice, "Block")
        for (field in fields) {
            Field(block = block, id = field.name, expression = field.expression)
        }
        schemaDevice.initialize()
        schemaDevice.connectBase(modbusDevice)
        schemaDevice.updateAll()
        return schemaDevice
    }

    private fun getField(
        schemaDevice: SchemaDevice,
        name: String,
    ): Field? = schemaDevice.getBlock("Block")!!.getField(name)

    // TODO: Boolean --> Coils and Direct Inputs
    //    private void verify(String bytes, String expression, Boolean expected) throws ModbusException {
    //        SchemaDevice schemaDevice = buildVerifier(bytes, new TestField("test", expression, BOOLEAN));
    //        assertEquals(expected, getField(schemaDevice, "test")  .getLongValue());
    //    }
    @Throws(ModbusException::class)
    private fun verify(
        expression: String,
        expected: Int,
        parseOutput: String,
    ) {
        val schemaDevice =
            buildVerifier(
                "",
                TestField("test1", expression),
                TestField("test2", "test1"),
            )
        val test1 = getField(schemaDevice, "test1") ?: throw AssertionError("Unable to load Field test1 back")
        val test2 = getField(schemaDevice, "test2") ?: throw AssertionError("Unable to load Field test2 back")

        assertEquals(ReturnType.LONG, test1.returnType)
        assertEquals(ReturnType.LONG, test2.returnType)

        assertEquals(expected.toLong(), test1.longValue)
        assertEquals(expected.toLong(), test2.longValue)
        val parsedExpression = test1.parsedExpression
        assertNotNull(parsedExpression)
        assertEquals(parseOutput, parsedExpression.toString(), "Unexpected parse tree")
        val registers =
            parsedExpression
                .getRegisterValues(schemaDevice)
                .joinToString(separator = ",") { it.toString() }
        assertTrue(registers.isEmpty(), "Registers was not empty $registers")
    }

    @Throws(ModbusException::class)
    private fun verify(
        expression: String,
        expected: Double?,
        parseOutput: String,
    ) {
        val schemaDevice =
            buildVerifier(
                "",
                TestField("test1", expression),
                TestField("test2", "test1"),
            )
        val test1 = getField(schemaDevice, "test1") ?: throw AssertionError("Unable to load Field test1 back")
        val test2 = getField(schemaDevice, "test2") ?: throw AssertionError("Unable to load Field test2 back")

        assertEquals(ReturnType.DOUBLE, test1.returnType)
        assertEquals(ReturnType.DOUBLE, test2.returnType)

        val parsedExpression1 = test1.parsedExpression
        if (expected == null) {
            assertNull(test1.doubleValue)
            assertNull(test2.doubleValue)
        } else {
            assertCloseEnough(expected, test1.doubleValue, parsedExpression1)
            assertCloseEnough(expected, test2.doubleValue, test2.parsedExpression)
        }
        assertNotNull(parsedExpression1)
        assertEquals(parseOutput, parsedExpression1.toString(), "Unexpected parse tree")
        val registers =
            parsedExpression1
                .getRegisterValues(schemaDevice)
                .joinToString(separator = ",") { it.toString() }
        assertTrue(registers.isEmpty(), "Registers was not empty $registers")
    }

    @Throws(ModbusException::class)
    private fun verify(
        bytes: String,
        expression: String,
        expected: Long?,
    ) {
        val schemaDevice =
            buildVerifier(
                bytes,
                TestField("test1", expression),
                TestField("test2", "test1"),
            )
        val test1 = getField(schemaDevice, "test1") ?: throw AssertionError("Unable to load Field test1 back")
        val test2 = getField(schemaDevice, "test2") ?: throw AssertionError("Unable to load Field test2 back")

        assertEquals(ReturnType.LONG, test1.returnType)
        assertEquals(ReturnType.LONG, test2.returnType)

        if (expected == null) {
            assertNull(test1.longValue)
        } else {
            assertEquals(expected, test1.longValue)
            if (expected > -1000000000000L && expected < 1000000000000L) {
                // We do not test outside of these bounds because of overflow problems.
                assertCloseEnough(
                    expected.toDouble(),
                    test2.doubleValue,
                    test2.parsedExpression,
                )
            }
        }

        val parsedExpression1 = test1.parsedExpression
        assertNotNull(parsedExpression1)
        assertEquals(
            bytes,
            parsedExpression1
                .getRegisterValues(schemaDevice)
                .joinToString(separator = " ") { it.hexValue }
                .replace("0x".toRegex(), ""),
        )
    }

    @Throws(ModbusException::class)
    private fun verify(
        bytes: String,
        expression: String,
        expected: Double?,
    ) {
        val schemaDevice =
            buildVerifier(
                bytes,
                TestField("test1", expression),
                TestField("test2", "test1"),
            )
        val test1 = getField(schemaDevice, "test1") ?: throw AssertionError("Unable to load Field test1 back")
        val test2 = getField(schemaDevice, "test2") ?: throw AssertionError("Unable to load Field test2 back")

        assertEquals(ReturnType.DOUBLE, test1.returnType)
        assertEquals(ReturnType.DOUBLE, test2.returnType)

        val parsedExpression1 = test1.parsedExpression
        if (expected == null) {
            assertNull(test1.doubleValue)
            assertNull(test2.doubleValue)
        } else {
            assertCloseEnough(expected, test1.doubleValue, parsedExpression1)
            assertCloseEnough(expected, test2.doubleValue, test2.parsedExpression)
        }
        assertNotNull(parsedExpression1)
        assertEquals(
            bytes,
            parsedExpression1
                .getRegisterValues(schemaDevice)
                .joinToString(separator = " ", transform = RegisterValue::hexValue)
                .replace("0x".toRegex(), ""),
        )
    }

    private fun verify(
        bytes: String,
        expression: String,
        expected: String?,
    ) {
        val schemaDevice = buildVerifier(bytes, TestField("test", expression))
        val testField = getField(schemaDevice, "test")
        requireNotNull(testField)
        require(schemaDevice.initialize()) { "Init failed :\n${schemaDevice.initializationProblems()}" }
        assertEquals(ReturnType.STRING, testField.returnType)
        assertEquals(expected, testField.stringValue)
    }

    private fun verify(
        bytes: String,
        expression: String,
        expected: List<String>?,
    ) {
        val schemaDevice = buildVerifier(bytes, TestField("test", expression))
        val testField = getField(schemaDevice, "test")
        requireNotNull(testField)
        assertTrue(testField.initialize())
        assertEquals(ReturnType.STRINGLIST, testField.returnType)
        assertEquals(expected, testField.stringListValue)
    }

    private fun isVALID(
        expression: String,
        returnType: ReturnType,
    ) {
        isVALID(expression, returnType, null)
    }

    private fun isVALID(
        expression: String,
        returnType: ReturnType,
        expectedExpression: String?,
    ) {
        try {
            val bytes =
                "0123 4567 89AB CDEF 0123 4567 89AB CDEF 0123 4567 89AB CDEF 0123 4567 89AB CDEF 0123 4567 89AB CDEF "
            val schemaDevice = buildVerifier(bytes, TestField("test", expression))
            assertTrue(schemaDevice.initialize(), "Expression $expression failed to initialize")
            if (expectedExpression != null) {
                val field = schemaDevice.getBlock("Block")?.getField("test")
                requireNotNull(field)
                assertEquals(returnType, field.returnType)
                assertEquals(
                    expectedExpression,
                    field.parsedExpression.toString(),
                    "The parsed expression was incorrect.",
                )
            }
        } catch (e: Exception) {
            error("Expression " + expression + " should be valid:" + e.message)
        }
    }

    private fun iNvalid(expression: String) {
        try {
            val bytes =
                "0123 4567 89AB CDEF 0123 4567 89AB CDEF 0123 4567 89AB CDEF 0123 4567 89AB CDEF 0123 4567 89AB CDEF "
            val schemaDevice = buildVerifier(bytes, TestField("test", expression))
            val field = schemaDevice.getBlock("Block")?.getField("test")
            assertFalse(
                schemaDevice.initialize(),
                "The expression $expression MUST trigger an exception but it did not.\n" +
                    "The field was parsed as: ${field?.parsedExpression}",
            )
        } catch (e: ModbusSchemaParseException) {
            // Success !
        }
    }

    private fun verifyToString(
        expression: String,
        expected: String,
    ) {
        val schemaDevice1 = buildVerifier("DEAD", TestField("test", expression))
        val fieldToString = getField(schemaDevice1, "test")!!.parsedExpression.toString()
        assertEquals(expected, fieldToString)
        // Now we parse the result of toString, create a new Field and then check if they are equal
        val schemaDevice2 = buildVerifier("DEAD", TestField("test", fieldToString))
        assertEquals(expected, getField(schemaDevice2, "test")!!.parsedExpression.toString())
    }

    @Test
    fun testTooManyRegisters() {
        iNvalid("hexstring(hr:0#126)")
    }

    @Test
    fun testBitManipulation() {
        // Reverse 16 bits: 0xABCD ( 10101011 11001101 ) into 0xB3D5 ( 10110011 11010101 )
        verify("ABCD", "hexstring(swapendian(hr:0))", "0xB3 0xD5")
        // Reverse 2 bytes: 0xABCD into 0xCDAB
        verify("ABCD", "hexstring(swapbytes(hr:0))", "0xCD 0xAB")

        verifyToString("hexstring(swapendian(hr:0))", "hexstring(swapendian(hr:00000))")
        verifyToString("hexstring(swapbytes(hr:0))", "hexstring(swapbytes(hr:00000))")

        iNvalid("hexstring(swapendian(hr:0#2))")
        iNvalid("hexstring(swapbytes(hr:0#2))")
    }

    @Test
    fun testStringUtf8() {
        // UTF8         BRACEOPEN registers=registerlist BRACECLOSE
        verifyToString("utf8(hr:0#13)", "utf8(hr:00000 # 13)")
        verify(
            "6162 6364 6566 6768 696a 6b6c 6d6e 6f70 7172 7374 7576 7778 797a",
            "utf8(hr:0#13)",
            "abcdefghijklmnopqrstuvwxyz",
        )
        verify("6162 6364 6566 6700 0000 0000 0000 0000 0000 0000 0000 0000 0000", "utf8(hr:0#13)", "abcdefg")
    }

    @Test
    fun testStringHexString() {
        // HEXSTRING    BRACEOPEN registers=registerlist BRACECLOSE
        verifyToString("hexstring(hr:0#13)", "hexstring(hr:00000 # 13)")
        verify(
            "6162   6364   6566   6700  0000   ",
            "hexstring(hr:0#5)",
            "0x61 0x62 0x63 0x64 0x65 0x66 0x67 0x00 0x00 0x00",
        )
    }

    @Test
    fun testStringConstant() {
        verifyToString("'Hello World!'", "'Hello World!'")
        verify(
            "0000",
            "'Hello World!'",
            "Hello World!",
        )
    }

    @Test
    fun testStringConcat() {
        // CONCAT BRACEOPEN string ( COMMA string )* BRACECLOSE
        verifyToString("concat('Hello World!')", "concat('Hello World!')")
        verifyToString("concat('Hello' , ' ' , 'World', '!')", "concat('Hello', ' ', 'World', '!')")
        verify(
            "  0000   ",
            "concat('Hello' , ' ' , 'World', '!')",
            "Hello World!",
        )

        verify(
            //    H e  l l  o    W o  r l  d    ! !
            "4865 6C6C 6F00 576F 726C 6400 2121",
            "concat(utf8(hr:0#3), ' ' , utf8(hr:3#3), '!')",
            "Hello World!",
        )
    }

    @Test
    fun testStringEUI48() {
        // EUI48        BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verifyToString(
            "eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )",
            "eui48(hr:00000 # 4 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
        )
        verifyToString("eui48(hr:0#4  )", "eui48(hr:00000 # 4)")

        isVALID("eui48(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD )", ReturnType.STRING)

        isVALID("eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )", ReturnType.STRING)

        verify(
            "0102 0304 0506 0708",
            "eui48(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD )",
            "01:02:03:04:05:06",
        )
        verify(
            "0102 0304 0506 0708",
            "eui48(hr:0#3)",
            "01:02:03:04:05:06",
        )
        verify(
            "DEAD DEAD DEAD DEAD",
            "eui48(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD)",
            null as String?,
        )

        verify(
            "0102 0304 0506 0708",
            "eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
            "03:04:05:06:07:08",
        )
        verify(
            "0102 0304 0506 0708",
            "eui48(hr:0#4)",
            "03:04:05:06:07:08",
        )
        verify(
            "DEAD DEAD DEAD DEAD",
            "eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
            null as String?,
        )

        // Wrong "not implemented" sizes (it must be the exact same size as the retrieve number of registers)
        iNvalid("eui48(hr:0#3 ; 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#4 ; 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#4 ; 0xDEAD 0xDEAD 0xDEAD ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")

        // Too long
        iNvalid("eui48(hr:0#5 ; 0xDEAD 0xDEAD 0xDEAD ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#5                                                      )")

        // Too short
        iNvalid("eui48(hr:0#2 ; 0xDEAD 0xDEAD 0xDEAD ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("eui48(hr:0#2                                                      )")
        iNvalid("eui48(hr:0#2 ; 0xDEAD 0xDEAD 0xDEAD ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testStringIPv4Addr() {
        // IPv4ADDR        BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verifyToString(
            "ipv4addr(hr:0#2 ; 0xDEAD 0xDEAD)",
            "ipv4addr(hr:00000 # 2 ; 0xDEAD 0xDEAD)",
        )
        verifyToString("ipv4addr(hr:0#2                )", "ipv4addr(hr:00000 # 2)")
        verify("0102 0304", "ipv4addr(hr:0#2 ; 0xDEAD 0xDEAD)", "1.2.3.4")
        verify("0102 0304", "ipv4addr(hr:0#2                )", "1.2.3.4")
        verify("DEAD DEAD", "ipv4addr(hr:0#2 ; 0xDEAD 0xDEAD)", null as String?)
        verify("0000 0000", "ipv4addr(hr:0#2 ; 0xDEAD 0xDEAD ; 0x0000 0x0000)", null as String?)

        // Too long
        iNvalid("ipv4addr(hr:0#3 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)")
        iNvalid("ipv4addr(hr:0#3                              )")
        // Too short
        iNvalid("ipv4addr(hr:0   ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)")
        iNvalid("ipv4addr(hr:0                                )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testStringIPv6Addr() {
        // IPv6ADDR        BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verifyToString(
            "ipv6addr(hr:0#8 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
            "ipv6addr(hr:00000 # 8 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
        )
        verifyToString("ipv6addr(hr:0#8  )", "ipv6addr(hr:00000 # 8)")
        verify(
            "0001 0203 0405 0607 0809 0A0B 0C0D 0E0F",
            "ipv6addr(hr:0#8 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
            "0001:0203:0405:0607:0809:0A0B:0C0D:0E0F",
        )
        verify(
            "0001 0203 0405 0607 0809 0A0B 0C0D 0E0F",
            "ipv6addr(hr:0#8                                                          )",
            "0001:0203:0405:0607:0809:0A0B:0C0D:0E0F",
        )
        verify(
            "DEAD DEAD DEAD DEAD DEAD DEAD DEAD DEAD",
            "ipv6addr(hr:0#8 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD)",
            null as String?,
        )

        // Missing byte
        verify(
            "DEAD null null null null null null null",
            "ipv6addr(hr:0#8                                                          )",
            null as String?,
        )

        // NotImplemented size mismatch
        iNvalid("ipv6addr(hr:0#8 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("ipv6addr(hr:0#8 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD)")

        // Too long
        iNvalid("ipv6addr(hr:0#9 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("ipv6addr(hr:0#9                                                                  )")

        // Too short
        iNvalid("ipv6addr(hr:0#7 ; 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD  )")
        iNvalid("ipv6addr(hr:0#7                                                            )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testStringEnum() {
        // ENUM         BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*  ( SEMICOLON mapping )+ BRACECLOSE
        verifyToString(
            "enum(hr:0     ; 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')",
            "enum(hr:00000 ; 0xDEAD ; 0->'Off' ; 1->'Manual' ; 2->'Automatic')",
        )
        verifyToString(
            "enum(hr:0     ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')",
            "enum(hr:00000 ; 0->'Off' ; 1->'Manual' ; 2->'Automatic')",
        )
        verify("0001", "enum(hr:0 ; 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')", "Manual")
        verify("0001", "enum(hr:0          ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')", "Manual")
        verify(
            "0004",
            "enum(hr:0 ; 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')",
            "No mapping for value 0x00 0x04",
        )
        verify(
            "0004",
            "enum(hr:0          ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')",
            "No mapping for value 0x00 0x04",
        )
        verify("DEAD", "enum(hr:0 ; 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')", null as String?)

        // 2 bytes
        verify(
            "000F 4240",
            "enum(hr:0#2 ; 0xDEAD 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic'; 1000000 -> 'Million')",
            "Million",
        )
        verify(
            "DEAD DEAD",
            "enum(hr:0#2 ; 0xDEAD 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic'; 1000000 -> 'Million')",
            null as String?,
        )

        // NotImplemented size mismatch
        iNvalid("enum(hr:0   ; 0xDEAD 0xDEAD        ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')")
        iNvalid("enum(hr:0#2 ; 0xDEAD               ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')")
        iNvalid("enum(hr:0#2 ; 0xDEAD 0xDEAD 0xDEAD ; 0-> 'Off'; 1->'Manual'; 2-> 'Automatic')")
    }

    @Test
    @Throws(ModbusException::class)
    fun testStringListBitSet() {
        // BITSET          BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*  ( SEMICOLON mapping )+ BRACECLOSE #stringListBitSet
        verifyToString(
            "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')",
            "bitset(hr:00000 ; 0xDEAD ; 0->'Zero' ; 1->'One' ; 2->'Two')",
        )
        verify("DEAD", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", null as List<String>?)
        verify("0000", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf())
        verify("0000", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf())
        verify("0001", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero"))
        verify("0001", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero"))
        verify("0002", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("One"))
        verify("0002", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("One"))
        verify("0003", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero", "One"))
        verify("0003", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero", "One"))
        verify("0004", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Two"))
        verify("0004", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Two"))
        verify("0005", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero", "Two"))
        verify("0005", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero", "Two"))
        verify("0006", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("One", "Two"))
        verify("0006", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("One", "Two"))
        verify("0007", "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero", "One", "Two"))
        verify("0007", "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')", listOf("Zero", "One", "Two"))
        verify(
            "000F",
            "bitset(hr:0 ; 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')",
            listOf("Zero", "One", "Two", "Bit 3"),
        )
        verify(
            "000F",
            "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')",
            listOf("Zero", "One", "Two", "Bit 3"),
        )
        verify(
            "100F",
            "bitset(hr:0          ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')",
            listOf("Zero", "One", "Two", "Bit 3", "Bit 12"),
        )

        // 2 bytes
        verify(
            "100F 100F",
            "bitset(hr:0#2   ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two'; 16 -> 'Sixteen'; 17-> 'Seventeen'; 18-> 'Eighteen' )",
            listOf(
                "Zero",
                "One",
                "Two",
                "Bit 3",
                "Bit 12",
                "Sixteen",
                "Seventeen",
                "Eighteen",
                "Bit 19",
                "Bit 28",
            ),
        )

        // 3 bytes
        verify(
            "100F 100F 100F",
            "bitset(hr:0#3   " +
                "; 0  -> '0' ;  1-> '1' ;  2-> '2'" +
                "; 16 -> '16'; 17-> '17'; 18-> '18' " +
                "; 32 -> '32'; 33-> '33'; 34-> '34' " +
                ")",
            listOf(
                "0",
                "1",
                "2",
                "Bit 3",
                "Bit 12",
                "16",
                "17",
                "18",
                "Bit 19",
                "Bit 28",
                "32",
                "33",
                "34",
                "Bit 35",
                "Bit 44",
            ),
        )

        // 4 bytes
        verify(
            "100F 100F 100F 100F",
            "bitset(hr:0#4   " +
                "; 0  -> '0' ;  1-> '1' ;  2-> '2'" +
                "; 16 -> '16'; 17-> '17'; 18-> '18' " +
                "; 32 -> '32'; 33-> '33'; 34-> '34' " +
                "; 48 -> '48'; 49-> '49'; 50-> '50' " +
                ")",
            listOf(
                "0",
                "1",
                "2",
                "Bit 3",
                "Bit 12",
                "16",
                "17",
                "18",
                "Bit 19",
                "Bit 28",
                "32",
                "33",
                "34",
                "Bit 35",
                "Bit 44",
                "48",
                "49",
                "50",
                "Bit 51",
                "Bit 60",
            ),
        )

        // NotImplemented size mismatch
        iNvalid("bitset(hr:0 ; 0xDEAD 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')")
        iNvalid("bitset(hr:0#4 ; 0xDEAD 0xDEAD ; 0-> 'Zero'; 1-> 'One'; 2-> 'Two')")
    }

    @Test
    @Throws(ModbusException::class)
    fun testDoubleConstantExpressions() {
        verify("  1234.11 ", 1234.11, "1234.11") // MINUS? DOUBLE
        verify(" -1234.11 ", -1234.11, "-1234.11")

        verify("( 1234.11)", 1234.11, "1234.11") // BRACEOPEN double BRACECLOSE
        verify("(-1234.11)", -1234.11, "-1234.11")

        verify(" 5  ^3.3", 202.5820, "5^3.3") // base=long       POWER     exponent=double
        verify(" 5.5^3", 166.375, "5.5^3") // base=double     POWER     exponent=long
        verify("10  ^-3", 0.001, "10^-3") // base=double     POWER     exponent=long
        verify("5.5 ^3.3", 277.4577, "5.5^3.3") // base=double     POWER     exponent=double

        verify("5  *3.3", 16.5, "5*3.3") // left=long       MULTIPLY  right=double
        verify("5.5*3", 16.5, "5.5*3") // left=double     MULTIPLY  right=long
        verify("5.5*3.3", 18.15, "5.5*3.3") // left=double     MULTIPLY  right=double

        // Valid forms of explicit and implicit multiplications
        verify("5 (3.3)", 16.5, "5*3.3")
        verify("5 (1.1)*3", 16.5, "(5*1.1)*3")
        verify("5 (3)*1.1", 16.5, "(5*3)*1.1")
        verify("5.5(3)", 16.5, "5.5*3")
        verify("(5.5)*3", 16.5, "5.5*3")
        verify("(5.5)(3.3)", 18.15, "5.5*3.3")
        verify("(1.1)*5(3.3)", 18.15, "(1.1*5)*3.3")

        verify("5  /3", 1.6666, "5/3") // dividend=long   DIVIDE    divisor=long
        verify("5  /3.3", 1.5151, "5/3.3") // dividend=long   DIVIDE    divisor=double
        verify("5.5/3", 1.8333, "5.5/3") // dividend=double DIVIDE    divisor=long
        verify("5.5/3.3", 1.6666, "5.5/3.3") // dividend=double DIVIDE    divisor=double

        verify("5  +3.3", 8.3, "5+3.3") // left=long       ADD       right=double
        verify("5.5+3", 8.5, "5.5+3") // left=double     ADD       right=long
        verify("5.5+3.3", 8.8, "5.5+3.3") // left=double     ADD       right=double

        verify("5  -3.3", 1.7, "5-3.3") // left=long       MINUS     right=double
        verify("5.5-3", 2.5, "5.5-3") // left=double     MINUS     right=long
        verify("5.5-3.3", 2.2, "5.5-3.3") // left=double     MINUS     right=double
    }

    @Test
    @Throws(ModbusException::class)
    fun testInvalidImplicitMultiplications() {
        // Implicit multiply
        // A left to braces "5(3)" is accepted as a multiplication,
        // A right to braces like "(5)3" is NOT accepted because it is not clear what the calculation
        // should be with negative numbers: "(3)-2" -->  "3-2" OR "3 * -2"
        iNvalid("(5.5)3.3")
        iNvalid("(5.5)3")
        iNvalid("(5)3")

        isVALID("(5.5)-3.3", ReturnType.DOUBLE, "5.5-3.3")
        isVALID("(5.5)-3", ReturnType.DOUBLE, "5.5-3")
        isVALID("(5)-3", ReturnType.LONG, "5-3")
    }

    @Test
    @Throws(ModbusException::class)
    fun testDoubleIEEE754_32() {
        // IEEE754_32      BRACEOPEN registers=registerlist BRACECLOSE
        verify("449A 5225", "ieee754_32(hr:0#2)", 1234.567)
        verify("DEAD DEAD", "ieee754_32(hr:0#2;  0xDEAD 0xDEAD)", null as Double?)

        iNvalid("ieee754_32(hr:0#1 )")
        isVALID("ieee754_32(hr:0#2 )", ReturnType.DOUBLE)
        iNvalid("ieee754_32(hr:0#3 )")

        iNvalid("ieee754_32(hr:0#1;  0xDEAD               )")
        iNvalid("ieee754_32(hr:0#1;  0xDEAD 0xDEAD        )")

        iNvalid("ieee754_32(hr:0#2;  0xDEAD               )")
        isVALID("ieee754_32(hr:0#2;  0xDEAD 0xDEAD        )", ReturnType.DOUBLE)
        iNvalid("ieee754_32(hr:0#2;  0xDEAD 0xDEAD 0xDEAD )")

        iNvalid("ieee754_32(hr:0#3;  0xDEAD               )")
        iNvalid("ieee754_32(hr:0#3;  0xDEAD 0xDEAD        )")
        iNvalid("ieee754_32(hr:0#3;  0xDEAD 0xDEAD 0xDEAD )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testDoubleIEEE754_64() {
        // IEEE754_64      BRACEOPEN registers=registerlist BRACECLOSE
        verify("4093 4A45 84FC D47C", "ieee754_64(hr:0#4)", 1234.56789012)
        verify("DEAD DEAD DEAD DEAD", "ieee754_64(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)", null as Double?)

        iNvalid("ieee754_64(hr:0#1 )")
        iNvalid("ieee754_64(hr:0#2 )")
        iNvalid("ieee754_64(hr:0#3 )")
        isVALID("ieee754_64(hr:0#4 )", ReturnType.DOUBLE)
        iNvalid("ieee754_64(hr:0#5 )")
        iNvalid("ieee754_64(hr:0#6 )")

        iNvalid("ieee754_64(hr:0#1;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("ieee754_64(hr:0#2;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("ieee754_64(hr:0#3;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        isVALID("ieee754_64(hr:0#4;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )", ReturnType.DOUBLE)
        iNvalid("ieee754_64(hr:0#5;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("ieee754_64(hr:0#6;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )")

        iNvalid("ieee754_64(hr:0#1;  0xDEAD                      )")
        iNvalid("ieee754_64(hr:0#2;  0xDEAD 0xDEAD               )")
        iNvalid("ieee754_64(hr:0#3;  0xDEAD 0xDEAD 0xDEAD        )")
        isVALID("ieee754_64(hr:0#4;  0xDEAD 0xDEAD 0xDEAD 0xDEAD )", ReturnType.DOUBLE)
        iNvalid("ieee754_64(hr:0#5;  0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD         )")
        iNvalid("ieee754_64(hr:0#6;  0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD 0xDEAD  )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongConstantExpressions() {
        // MINUS? LONG
        verify("  1234 ", 1234, "1234")
        verify(" -1234 ", -1234, "-1234")

        // BRACEOPEN long BRACECLOSE
        verify("( 1234)", 1234, "1234")
        verify("( -1234 )", -1234, "-1234")

        verify("5*3", 15, "5*3") // left=long MULTIPLY right=long
        verify("5+3", 8, "5+3") // left=long ADD right=long
        verify("5-3", 2, "5-3") // left=long SUBTRACT right=long
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongInt16() {
        // INT16  BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verify("3039", "int16(hr:0)", 12345)
        verify("D431", "int16(hr:0)", -11215)
        verify("CFC7", "int16(hr:0)", -12345)
        verify("DEAD", "int16(hr:0; 0xDEAD)", null as Long?)

        isVALID("int16(hr:0#1 )", ReturnType.LONG)
        iNvalid("int16(hr:0#2 )")
        iNvalid("int16(hr:0#3 )")
        iNvalid("int16(hr:0#4 )")
        isVALID("int16(hr:0#1; 0xDEAD )", ReturnType.LONG)
        iNvalid("int16(hr:0#2; 0xDEAD 0xDEAD)")
        iNvalid("int16(hr:0#3; 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("int16(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongUInt16() {
        // UINT16 BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verify("3039", "uint16(hr:0)", 12345)
        verify("D431", "uint16(hr:0)", 54321)
        verify("CFC7", "uint16(hr:0)", 53191)
        verify("DEAD", "uint16(hr:0; 0xDEAD)", null as Long?)

        isVALID("uint16(hr:0#1 )", ReturnType.LONG)
        iNvalid("uint16(hr:0#2 )")
        iNvalid("uint16(hr:0#3 )")
        iNvalid("uint16(hr:0#4 )")
        isVALID("uint16(hr:0#1; 0xDEAD )", ReturnType.LONG)
        iNvalid("uint16(hr:0#2; 0xDEAD 0xDEAD)")
        iNvalid("uint16(hr:0#3; 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("uint16(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongInt32() {
        // INT32  BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verify("4996 02D2", "int32(hr:0#2)", 1234567890)
        verify("B669 FD2E", "int32(hr:0#2)", -1234567890)
        verify("DEAD DEAD", "int32(hr:0#2; 0xDEAD 0xDEAD)", null as Long?)

        iNvalid("int32(hr:0#1 )")
        isVALID("int32(hr:0#2 )", ReturnType.LONG)
        iNvalid("int32(hr:0#3 )")
        iNvalid("int32(hr:0#4 )")
        iNvalid("int32(hr:0#1; 0xDEAD )")
        isVALID("int32(hr:0#2; 0xDEAD 0xDEAD)", ReturnType.LONG)
        iNvalid("int32(hr:0#3; 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("int32(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongUInt32() {
        // UINT32 BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verify("4996 02D2", "uint32(hr:0#2)", 1234567890)
        verify("B669 FD2E", "uint32(hr:0#2)", 3060399406)
        verify("DEAD DEAD", "uint32(hr:0#2; 0xDEAD 0xDEAD)", null as Long?)

        iNvalid("uint32(hr:0#1 )")
        isVALID("uint32(hr:0#2 )", ReturnType.LONG)
        iNvalid("uint32(hr:0#3 )")
        iNvalid("uint32(hr:0#4 )")
        iNvalid("uint32(hr:0#1; 0xDEAD )")
        isVALID("uint32(hr:0#2; 0xDEAD 0xDEAD)", ReturnType.LONG)
        iNvalid("uint32(hr:0#3; 0xDEAD 0xDEAD 0xDEAD )")
        iNvalid("uint32(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )")
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongInt64() {
        // INT64  BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verify("1122 10F4 7DE9 8115", "int64(hr:0#4)", 1234567890123456789L)
        verify("EEDD EF0B 8216 7EEB", "int64(hr:0#4)", -1234567890123456789L)
        verify("DEAD DEAD DEAD DEAD", "int64(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD)", null as Long?)

        iNvalid("int64(hr:0#1 )")
        iNvalid("int64(hr:0#2 )")
        iNvalid("int64(hr:0#3 )")
        isVALID("int64(hr:0#4 )", ReturnType.LONG)
        iNvalid("int64(hr:0#1; 0xDEAD )")
        iNvalid("int64(hr:0#2; 0xDEAD 0xDEAD)")
        iNvalid("int64(hr:0#3; 0xDEAD 0xDEAD 0xDEAD )")
        isVALID("int64(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )", ReturnType.LONG)
    }

    @Test
    @Throws(ModbusException::class)
    fun testLongUInt64() {
        // UINT64 BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE
        verify("1122 10F4 7DE9 8115", "uint64(hr:0#4)", 1234567890123456789L)
        // FIXME: Known problem; There is no unsigned 64 bit int in Java, so this is really an unsigned 63 bit int.
        //        If you get too big then I made it to simply fail.
        verify("EEDD EF0B 8216 7EEB", "uint64(hr:0#4)", null as Long?)

        iNvalid("uint64(hr:0#1 )")
        iNvalid("uint64(hr:0#2 )")
        iNvalid("uint64(hr:0#3 )")
        isVALID("uint64(hr:0#4 )", ReturnType.LONG)
        iNvalid("uint64(hr:0#1; 0xDEAD )")
        iNvalid("uint64(hr:0#2; 0xDEAD 0xDEAD)")
        iNvalid("uint64(hr:0#3; 0xDEAD 0xDEAD 0xDEAD )")
        isVALID("uint64(hr:0#4; 0xDEAD 0xDEAD 0xDEAD 0xDEAD )", ReturnType.LONG)
    }

    @Test
    @Throws(ModbusException::class)
    fun testExpressionPrecedence() {
        // https://youtu.be/GnE9b__GX1E
        // 12/3*4 = 16
        verify("  12 / 3 *  4 ", 16.0, "(12/3)*4")
        verify("   4 * 3 / 12 ", 1.0, "(4*3)/12")

        // https://youtu.be/2hsW8_Wpffk
        verify("3^3+4*(8-5)/6", 29.0, "(3^3)+((4*(8-5))/6)")

        // https://www.chilimath.com/lessons/introductory-algebra/order-of-operations-practice-problems/
        verify("7-24/8*4+6", 1.0, "(7-((24/8)*4))+6") // 1
        verify("18/3-7+2*5", 9.0, "((18/3)-7)+(2*5)") // 2
        verify("6*4/12+72/8-9", 2.0, "(((6*4)/12)+(72/8))-9") // 3
        verify("(17-6/2)+4*3", 26.0, "(17-(6/2))+(4*3)") // 4
        verify("-2(1*4-2/2)+(6+2-3)", -1.0, "(-2*((1*4)-(2/2)))+((6+2)-3)") // 5
        verify("-1((3-4*7)/5)-2*24/6", -3.0, "(-1*((3-(4*7))/5))-((2*24)/6)") // 6
        verify("(3*5^2/15)-(5-2^2)", 4.0, "((3*(5^2))/15)-(5-(2^2))") // 7
        verify("(1^4*2^2+3^3)-2^5/4", 23.0, "(((1^4)*(2^2))+(3^3))-((2^5)/4)") // 8
        verify("(22/2-2*5)^2+(4-6/6)^2", 10.0, "(((22/2)-(2*5))^2)+((4-(6/6))^2)") // 9

        // https://leverageedu.com/blog/pemdas/
        verify("9 + (12 + 1)^2", 178, "9+((12+1)^2)")
        verify("7 + (-5(-10 - 1))^3", 166382, "7+((-5*(-10-1))^3)")
        verify("12 / 6 * 3 / 2", 3.0, "((12/6)*3)/2")
        verify("8 + (16 * 5^2 - 10)", 398, "8+((16*(5^2))-10)")
        verify("7 * 3 + 10 * (25 / 5)", 71.0, "(7*3)+(10*(25/5))")
    }

    @Test
    fun testFailOnMixingAddressClasses() {
        val schemaDevice = SchemaDevice("Device")
        val block = Block(schemaDevice, "Block")
        Field(block, "test", expression = "int32(ir:1, hr:1)")
        assertFalse(schemaDevice.initialize())
        println(schemaDevice.initializationProblems())
    }

    @Test
    fun testFailOnMixingAddressClassesInSubExpressions() {
        val schemaDevice = SchemaDevice("Device")
        val block = Block(schemaDevice, "Block")
        Field(block, "test", expression = "int16(ir:1)/int16(hr:1)")
        assertThrows<ModbusSchemaParseException> {
            schemaDevice.initialize()
        }
    }

    @Test
    fun verifyComplexExpression() {
        val schemaDevice = SchemaDevice("Device1")
        val block = Block(schemaDevice, "Block0", "Only block")

        val one = Field(block, "One", "One", expression = "ieee754_32(hr:0#2)")
        val two = Field(block, "Two", "Two", expression = "ieee754_64(hr:2#4)")
        val three = Field(block, "Three", "Three", expression = "uint16(hr:6)")
        val four = Field(block, "Four", "Four", expression = "int16(hr:7)")
        val factor = Field(block, "Factor", "Factor", expression = "int16(swapendian(hr:8))")
        val combined = Field(block, "Combined", "Combined", expression = "(One+Two+Three+Four)*10^Factor")

        schemaDevice.initialize()

        val modbusDevice: ModbusDevice = of(HOLDING_REGISTER, 0, "449A 5225 4093 4A45 84FC D47C 3039 CFC7 8000")

        schemaDevice.connect(modbusDevice)
        schemaDevice.needAll()
        schemaDevice.update(0)

        val oneValue = one.doubleValue
        val twoValue = two.doubleValue
        val threeValue = three.longValue
        val fourValue = four.longValue
        val factorValue = factor.longValue
        val combinedValue = combined.doubleValue

        assertNotNull(oneValue)
        assertNotNull(twoValue)
        assertNotNull(threeValue)
        assertNotNull(fourValue)
        assertNotNull(factorValue)
        assertNotNull(combinedValue)

        assertEquals(1234.567, oneValue, 0.001)
        assertEquals(1234.568, twoValue, 0.001)
        assertEquals(12345, threeValue)
        assertEquals(-12345, fourValue)
        assertEquals(1, factorValue)
        assertEquals(24691.349, combinedValue, 0.001)
    }

    companion object {
        fun assertCloseEnough(
            expected: Double?,
            actual: Double?,
            expression: Expression?,
        ) {
            if (actual == null && expected == null) {
                return
            }
            assertNotNull(actual) { "The actual value was NULL and that is bad" }
            assertNotNull(expected) { "The expected value was NULL and the actual value was $actual" }
            assertEquals(
                expected,
                actual,
                0.001,
                "The value $actual was not close enough to the expected $expected: $expression",
            )
        }
    }
}
