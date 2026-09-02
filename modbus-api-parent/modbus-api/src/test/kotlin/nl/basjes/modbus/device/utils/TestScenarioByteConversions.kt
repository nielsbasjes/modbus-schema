/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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
package nl.basjes.modbus.device.utils

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TestScenarioByteConversions {
    private val logger: Logger = LogManager.getLogger()

    // ------------------------------------------------------------
    @Test
    fun testHexString() {
        verifyHexString("", byteArrayOf())
        verifyHexString("    ", byteArrayOf())
        verifyHexString("00", byteArrayOf(0x00.toByte()))
        verifyHexString(" 0 0 ", byteArrayOf(0x00.toByte()))

        val theByteArray = byteArrayOf(0x01.toByte(),0x23.toByte(),0x45.toByte(),0x67.toByte(),0x89.toByte(),0xAB.toByte(),0xCD.toByte(),0xEF.toByte())
        verifyHexString("0123456789AbCdEf", theByteArray)
        verifyHexString("0 12 345 678 9Ab CdE f", theByteArray)
        verifyHexString("0x0123 0x4567 0x89 0xAbCd 0xEf", theByteArray)
    }

    private fun verifyHexString(
        input: String,
        bytes: ByteArray,
    ) {
        logger.info("\"{}\" --> \"{}\" ", input, hexStringToBytes(input).toHexString0x())
        assertArrayEquals(
            bytes,
            hexStringToBytes(input),
            "Mismatch in bytes for input: \"$input\"",
        )
        assertArrayEquals(
            bytes,
            hexStringToBytes(bytes.toHexString0x()),
            "byte[]-String-byte[] round trip for $input",
        )
    }

    private fun checkSplit(
        input: String,
        size: Int,
        vararg splits: String,
    ) {
        assertEquals(listOf(*splits), splitStringBySize(input, size))
    }

    @Test
    fun testSplitter() {
        checkSplit("", 1)
        checkSplit("A", 1, "A")
        checkSplit("AB", 1, "A", "B")
        checkSplit("ABC", 1, "A", "B", "C")
        checkSplit("ABCD", 1, "A", "B", "C", "D")
        checkSplit("ABCDE", 1, "A", "B", "C", "D", "E")
        checkSplit("ABCDEF", 1, "A", "B", "C", "D", "E", "F")

        checkSplit("", 2)
        checkSplit("A", 2, "A")
        checkSplit("AB", 2, "AB")
        checkSplit("ABC", 2, "AB", "C")
        checkSplit("ABCD", 2, "AB", "CD")
        checkSplit("ABCDE", 2, "AB", "CD", "E")
        checkSplit("ABCDEF", 2, "AB", "CD", "EF")

        checkSplit("", 3)
        checkSplit("A", 3, "A")
        checkSplit("AB", 3, "AB")
        checkSplit("ABC", 3, "ABC")
        checkSplit("ABCD", 3, "ABC", "D")
        checkSplit("ABCDE", 3, "ABC", "DE")
        checkSplit("ABCDEF", 3, "ABC", "DEF")

        checkSplit("", 4)
        checkSplit("A", 4, "A")
        checkSplit("AB", 4, "AB")
        checkSplit("ABC", 4, "ABC")
        checkSplit("ABCD", 4, "ABCD")
        checkSplit("ABCDE", 4, "ABCD", "E")
        checkSplit("ABCDEF", 4, "ABCD", "EF")
    }

    // ------------------------------------------------------------
    private fun verifyString(
        value: String,
        expectedChars: Int,
    ) {
        verifyString(value)
        assertEquals(expectedChars, value.toCharArray().size)
    }

    private fun verifyString(value: String) {
        val bytes = value.toByteArray()
        val result = bytes.toUtf8String()
        logger.info(
            "[STRING] {} --> | {} | --> {}",
            "\"$value\"",
            bytes.toHexString0x(),
            if (result == null) "null" else "\"" + result + "\"",
        )
        assertEquals(value, result)
    }

    @Test
    fun rangeCheck() {
        var t = 0x2A.toByte()
        assertTrue(isInRange(t, 0x10u, 0x30u))
        assertTrue(isInRange(t, 0x10u, 0x2Au))
        assertTrue(isInRange(t, 0x2Au, 0x30u))
        assertFalse(isInRange(t, 0x10u, 0x29u))
        assertFalse(isInRange(t, 0x2Bu, 0x30u))
        t = 0xA0.toByte()
        assertTrue(isInRange(t, 0x10u, 0xF0u))
        assertTrue(isInRange(t, 0x10u, 0xA0u))
        assertTrue(isInRange(t, 0xA0u, 0xF0u))
        assertFalse(isInRange(t, 0x10u, 0x9Fu))
        assertFalse(isInRange(t, 0xA1u, 0x30u))
    }

    @Test
    fun testString() {
        verifyString("")
        verifyString(" ")
        verifyString("  ")
        // A mix of 1 byte, 2 byte and 3 byte characters
        verifyString("你", 1) // 1 char
        verifyString("🖖", 2) // 2 chars
        verifyString("👹", 2) // 2 chars
        verifyString("|你|🖖|👹|")
    }

    @Test
    fun testChoppedBytesToString() {
        val bytes = "|®|你|🖖|".toByteArray()

        assertEquals(13, bytes.size)

        // Make sure we have 1,2,3 and 4 byte chars in this test string
        assertEquals(1, "|".toByteArray().size)
        assertEquals(2, "®".toByteArray().size)
        assertEquals(3, "你".toByteArray().size)
        assertEquals(4, "🖖".toByteArray().size)

        assertEquals("|®|你|🖖|",  bytes.copyOfRange(0, 13).toUtf8String())
        assertEquals("|®|你|🖖",   bytes.copyOfRange(0, 12).toUtf8String())
        assertEquals("|®|你|",    bytes.copyOfRange(0, 11).toUtf8String())
        assertEquals("|®|你|",    bytes.copyOfRange(0, 10).toUtf8String())
        assertEquals("|®|你|",    bytes.copyOfRange(0, 9).toUtf8String())
        assertEquals("|®|你|",    bytes.copyOfRange(0, 8).toUtf8String())
        assertEquals("|®|你",     bytes.copyOfRange(0, 7).toUtf8String())
        assertEquals("|®|",      bytes.copyOfRange(0, 6).toUtf8String())
        assertEquals("|®|",      bytes.copyOfRange(0, 5).toUtf8String())
        assertEquals("|®|",      bytes.copyOfRange(0, 4).toUtf8String())
        assertEquals("|®",       bytes.copyOfRange(0, 3).toUtf8String())
        assertEquals("|",        bytes.copyOfRange(0, 2).toUtf8String())
        assertEquals("|",        bytes.copyOfRange(0, 1).toUtf8String())
        assertEquals("",         bytes.copyOfRange(0, 0).toUtf8String())
    }

    // ------------------------------------------------------------
    private fun verifyShort(value: Short) {
        val bytes = value.toByteArray()
        val result = bytes.toShort()
        logger.info("[SHORT] {} --> | {} | --> {}", value, bytes.toHexString0x(), result)
        assertEquals(value, result)
    }

    @Test
    fun testShort() {
        verifyShort(Short.MIN_VALUE)
        verifyShort((-12345).toShort())
        verifyShort((-1).toShort())
        verifyShort((-256).toShort())
        verifyShort(0)
        verifyShort((-1).toShort())
        verifyShort(255.toShort())
        verifyShort(12345.toShort())
        verifyShort(Short.MAX_VALUE)
    }

    // ------------------------------------------------------------
    private fun verifyInteger(value: Int) {
        val bytes = value.toByteArray()
        val result = bytes.toInteger()
        logger.info("[INTEGER] {} --> | {} | --> {}", value, bytes.toHexString0x(), result)
        assertEquals(value, result)
    }

    @Test
    fun testInteger() {
        verifyInteger(Int.MIN_VALUE)
        verifyInteger(-1234567890)
        verifyInteger(-256)
        verifyInteger(-1)
        verifyInteger(0)
        verifyInteger(1)
        verifyInteger(255)
        verifyInteger(1234567890)
        verifyInteger(Int.MAX_VALUE)
    }

    // ------------------------------------------------------------
    private fun verifyLong(value: Long) {
        val bytes = value.toByteArray()
        val result = bytes.toLong()
        logger.info("[LONG] {} --> | {} | --> {}", value, bytes.toHexString0x(), result)
        assertEquals(value, result)
    }

    @Test
    fun testLong() {
        verifyLong(Long.MIN_VALUE)
        verifyLong(-1234567890L)
        verifyLong(-256L)
        verifyLong(-1L)
        verifyLong(0L)
        verifyLong(1L)
        verifyLong(255L)
        verifyLong(1234567890L)
        verifyLong(Long.MAX_VALUE)
    }

    // ------------------------------------------------------------
    private fun verifyFloat(value: Float) {
        val bytes = value.toByteArray()
        val result = bytes.toFloat()
        logger.info("[FLOAT] {} --> | {} | --> {}", value, bytes.toHexString0x(), result)
        assertEquals(value, result)
    }

    @Test
    fun testFloat() {
        verifyFloat(Float.MIN_VALUE)
        verifyFloat(-1234567.8f)
        verifyFloat(-1.2f)
        verifyFloat(0f)
        verifyFloat(1.2f)
        verifyFloat(1234567.8f)
        verifyFloat(Float.MAX_VALUE)
        verifyFloat(Float.NEGATIVE_INFINITY)
        verifyFloat(Float.POSITIVE_INFINITY)
        verifyFloat(Float.NaN)
    }

    // ------------------------------------------------------------
    private fun verifyDouble(value: Double) {
        val bytes = value.toByteArray()
        val result = bytes.toDouble()
        logger.info("[DOUBLE] {} --> | {} | --> {}", value, bytes.toHexString0x(), result)
        assertEquals(value, result)
    }

    @Test
    fun testDouble() {
        verifyDouble(Double.MIN_VALUE)
        verifyDouble(-1234567.890)
        verifyDouble(-1.2)
        verifyDouble(0.0)
        verifyDouble(1.2)
        verifyDouble(1234567.890)
        verifyDouble(Double.MAX_VALUE)
        verifyDouble(Double.NEGATIVE_INFINITY)
        verifyDouble(Double.POSITIVE_INFINITY)
        verifyDouble(Double.NaN)
    }

    // ------------------------------------------------------------
    @Test
    fun testInvalidBytesToShort() {
        assertThrows<IllegalArgumentException> { byteArrayOf().toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00).toShort() }
        // 2 bytes is the only valid number
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09).toShort() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A).toShort() }
    }

    @Test
    fun testInvalidBytesToInteger() {
        assertThrows<IllegalArgumentException> { byteArrayOf().toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02).toInteger() }
        // 4 bytes is the only valid number
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09).toInteger() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A).toInteger() }
    }

    @Test
    fun testInvalidBytesToLong() {
        assertThrows<IllegalArgumentException> { byteArrayOf().toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06).toLong() }
        // 8 bytes is the only valid number
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09).toLong() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A).toLong() }
    }

    @Test
    fun testInvalidBytesToFloat() {
        assertThrows<IllegalArgumentException> { byteArrayOf().toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02).toFloat() }
        // 4 bytes is the only valid number
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09).toFloat() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A).toFloat() }
    }

    @Test
    fun testInvalidBytesToDouble() {
        assertThrows<IllegalArgumentException> { byteArrayOf().toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06).toDouble() }
        // 8 bytes is the only valid number
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09).toDouble() }
        assertThrows<IllegalArgumentException> { byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A).toDouble() }
    }

    @Test
    fun testShortToRegisters() {
        val address = Address.of(AddressClass.INPUT_REGISTER, 1)
        listOf<Short>(-1234,-1,0,1,1234).forEach { expected ->
            val registerBlock = expected.toRegisters(address)
            val actualBytes = registerBlock.getByteArray(address, SHORT_REGISTERS)
            assertNotNull(actualBytes)
            val actual = actualBytes.toShort()
            assertEquals(expected, actual, "Converting a \"Short\" to bytes and back failed.")
        }
    }

    @Test
    fun testIntegerToRegisters() {
        val address = Address.of(AddressClass.INPUT_REGISTER, 1)
        listOf<Int>(-123456789,-1,0,1,123456789).forEach { expected ->
            val registerBlock = expected.toRegisters(address)
            val actualBytes = registerBlock.getByteArray(address, INTEGER_REGISTERS)
            assertNotNull(actualBytes)
            val actual = actualBytes.toInteger()
            assertEquals(expected, actual, "Converting a \"Int\" to bytes and back failed.")
        }
    }

    @Test
    fun testLongToRegisters() {
        val address = Address.of(AddressClass.INPUT_REGISTER, 1)
        listOf<Long>(-123456789012,-1,0,1,123456789012).forEach { expected ->
            val registerBlock = expected.toRegisters(address)
            val actualBytes = registerBlock.getByteArray(address, LONG_REGISTERS)
            assertNotNull(actualBytes)
            val actual = actualBytes.toLong()
            assertEquals(expected, actual, "Converting a \"Long\" to bytes and back failed.")
        }
    }

    @Test
    fun testFloatToRegisters() {
        val address = Address.of(AddressClass.INPUT_REGISTER, 1)
        listOf<Float>(-123.456f,-1.0f,0.0f,1.0f,123.456f).forEach { expected ->
            val registerBlock = expected.toRegisters(address)
            val actualBytes = registerBlock.getByteArray(address, FLOAT_REGISTERS)
            assertNotNull(actualBytes)
            val actual = actualBytes.toFloat()
            assertEquals(expected, actual, "Converting a \"Float\" to bytes and back failed.")
        }
    }

    @Test
    fun testDoubleToRegisters() {
        val address = Address.of(AddressClass.INPUT_REGISTER, 1)
        listOf<Double>(-123.456789012,-1.0,0.0,1.0,123.456789012).forEach { expected ->
            val registerBlock = expected.toRegisters(address)
            val actualBytes = registerBlock.getByteArray(address, DOUBLE_REGISTERS)
            assertNotNull(actualBytes)
            val actual = actualBytes.toDouble()
            assertEquals(expected, actual, "Converting a \"Double\" to bytes and back failed.")
        }
    }
}
