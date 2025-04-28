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
package nl.basjes.modbus.schema.utils

import nl.basjes.modbus.schema.utils.ByteConversions.bytesToFloat
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToInteger
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToShort
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToString
import nl.basjes.modbus.schema.utils.ByteConversions.stringToBytes
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.Assertions.assertArrayEquals
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
        verifyHexString("0123456789AbCdEf",byteArrayOf(0x01.toByte(),0x23.toByte(),0x45.toByte(),0x67.toByte(),0x89.toByte(),0xAB.toByte(),0xCD.toByte(),0xEF.toByte()))
        verifyHexString("0 12 345 678 9Ab CdE f",byteArrayOf(0x01.toByte(),0x23.toByte(),0x45.toByte(),0x67.toByte(),0x89.toByte(),0xAB.toByte(),0xCD.toByte(),0xEF.toByte()))
        verifyHexString("0x0123 0x4567 0x89 0xAbCd 0xEf",byteArrayOf(0x01.toByte(),0x23.toByte(),0x45.toByte(),0x67.toByte(),0x89.toByte(),0xAB.toByte(),0xCD.toByte(),0xEF.toByte())
       )
    }

    private fun verifyHexString(input: String, bytes: ByteArray) {
        logger.info("\"{}\" --> \"{}\" ", input, ByteConversions.bytesToHexString(ByteConversions.hexStringToBytes(input)))
        assertArrayEquals(
            bytes,
            ByteConversions.hexStringToBytes(input),
            "Mismatch in bytes for input: \"$input\""
        )
        assertArrayEquals(
            bytes,
            ByteConversions.hexStringToBytes(ByteConversions.bytesToHexString(bytes)),
            "byte[]-String-byte[] round trip for $input"
        )
    }


    private fun checkSplit(input: String, size: Int, vararg splits: String) {
        assertEquals(listOf(*splits), ByteConversions.splitStringBySize(input, size))
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
    private fun verifyString(value: String, expectedChars: Int) {
        verifyString(value)
        assertEquals(expectedChars, value.toCharArray().size)
    }

    private fun verifyString(value: String) {
        val bytes = stringToBytes(value)
        val result = bytesToString(bytes)
        logger.info(
            "[STRING] {} --> | {} | --> {}",
            "\"$value\"",
            ByteConversions.bytesToHexString(bytes),
            if (result == null) "null" else "\"" + result + "\""
        )
        assertEquals(value, result)
    }

    @Test
    fun rangeCheck() {
        var t = 0x2A.toByte()
        assertTrue(ByteConversions.isInRange(t, 0x10u, 0x30u))
        assertTrue(ByteConversions.isInRange(t, 0x10u, 0x2Au))
        assertTrue(ByteConversions.isInRange(t, 0x2Au, 0x30u))
        assertFalse(ByteConversions.isInRange(t, 0x10u, 0x29u))
        assertFalse(ByteConversions.isInRange(t, 0x2Bu, 0x30u))
        t = 0xA0.toByte()
        assertTrue(ByteConversions.isInRange(t, 0x10u, 0xF0u))
        assertTrue(ByteConversions.isInRange(t, 0x10u, 0xA0u))
        assertTrue(ByteConversions.isInRange(t, 0xA0u, 0xF0u))
        assertFalse(ByteConversions.isInRange(t, 0x10u, 0x9Fu))
        assertFalse(ByteConversions.isInRange(t, 0xA1u, 0x30u))
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
        val bytes = stringToBytes("|®|你|🖖|")

        assertEquals(13, bytes.size)

        // Make sure we have 1,2,3 and 4 byte chars in this test string
        assertEquals(1, stringToBytes("|").size)
        assertEquals(2, stringToBytes("®").size)
        assertEquals(3, stringToBytes("你").size)
        assertEquals(4, stringToBytes("🖖").size)

        assertEquals("|®|你|🖖|",  bytesToString(bytes.copyOfRange(0, 13)))
        assertEquals("|®|你|🖖",   bytesToString(bytes.copyOfRange(0, 12)))
        assertEquals("|®|你|",    bytesToString(bytes.copyOfRange(0, 11)))
        assertEquals("|®|你|",    bytesToString(bytes.copyOfRange(0, 10)))
        assertEquals("|®|你|",    bytesToString(bytes.copyOfRange(0, 9)))
        assertEquals("|®|你|",    bytesToString(bytes.copyOfRange(0, 8)))
        assertEquals("|®|你",     bytesToString(bytes.copyOfRange(0, 7)))
        assertEquals("|®|",      bytesToString(bytes.copyOfRange(0, 6)))
        assertEquals("|®|",      bytesToString(bytes.copyOfRange(0, 5)))
        assertEquals("|®|",      bytesToString(bytes.copyOfRange(0, 4)))
        assertEquals("|®",       bytesToString(bytes.copyOfRange(0, 3)))
        assertEquals("|",        bytesToString(bytes.copyOfRange(0, 2)))
        assertEquals("|",        bytesToString(bytes.copyOfRange(0, 1)))
        assertEquals("",         bytesToString(bytes.copyOfRange(0, 0)))
    }

    // ------------------------------------------------------------
    private fun verifyShort(value: Short) {
        val bytes = ByteConversions.shortToBytes(value)
        val result = bytesToShort(bytes)
        logger.info("[SHORT] {} --> | {} | --> {}", value, ByteConversions.bytesToHexString(bytes), result)
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
        val bytes = ByteConversions.integerToBytes(value)
        val result = bytesToInteger(bytes)
        logger.info("[INTEGER] {} --> | {} | --> {}", value, ByteConversions.bytesToHexString(bytes), result)
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
        val bytes = ByteConversions.longToBytes(value)
        val result = ByteConversions.bytesToLong(bytes)
        logger.info("[LONG] {} --> | {} | --> {}", value, ByteConversions.bytesToHexString(bytes), result)
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
        val bytes = ByteConversions.floatToBytes(value)
        val result = bytesToFloat(bytes)
        logger.info("[FLOAT] {} --> | {} | --> {}", value, ByteConversions.bytesToHexString(bytes), result)
        assertEquals(value, result)
    }

    @Test
    fun testFloat() {
        verifyFloat(Float.MIN_VALUE)
        verifyFloat(-1234567.890f)
        verifyFloat(-1.2f)
        verifyFloat(0f)
        verifyFloat(1.2f)
        verifyFloat(1234567.890f)
        verifyFloat(Float.MAX_VALUE)
        verifyFloat(Float.NEGATIVE_INFINITY)
        verifyFloat(Float.POSITIVE_INFINITY)
        verifyFloat(Float.NaN)
    }


    // ------------------------------------------------------------
    private fun verifyDouble(value: Double) {
        val bytes = ByteConversions.doubleToBytes(value)
        val result = ByteConversions.bytesToDouble(bytes)
        logger.info("[DOUBLE] {} --> | {} | --> {}", value, ByteConversions.bytesToHexString(bytes), result)
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
        assertThrows<IllegalArgumentException> { bytesToShort(byteArrayOf()) }
        assertThrows<IllegalArgumentException> { bytesToShort(byteArrayOf(0x00)) }
        // 2 bytes is the only valid number
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09))}
        assertThrows<IllegalArgumentException> {bytesToShort(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A))}
    }

    @Test
    fun testInvalidBytesToInteger() {
        assertThrows<IllegalArgumentException> { bytesToInteger(byteArrayOf()) }
        assertThrows<IllegalArgumentException> { bytesToInteger(byteArrayOf(0x00)) }
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02))}//
        // 4 bytes is the only valid number
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09))}
        assertThrows<IllegalArgumentException> {bytesToInteger(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A))}
    }

    @Test
    fun testInvalidBytesToLong() {
        assertThrows<IllegalArgumentException> { ByteConversions.bytesToLong(byteArrayOf()) }
        assertThrows<IllegalArgumentException> { ByteConversions.bytesToLong(byteArrayOf(0x00)) }
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03,0x04))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06))}
        // 8 bytes is the only valid number
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToLong(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A))}
    }

    @Test
    fun testInvalidBytesToFloat() {
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf()) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02)) }
        // 4 bytes is the only valid number
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09)) }
        assertThrows<IllegalArgumentException> { bytesToFloat(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A)) }
    }

    @Test
    fun testInvalidBytesToDouble() {
        assertThrows<IllegalArgumentException> { ByteConversions.bytesToDouble(byteArrayOf()) }
        assertThrows<IllegalArgumentException> { ByteConversions.bytesToDouble(byteArrayOf(0x00)) }
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03,0x04))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06))}
        // 8 bytes is the only valid number
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09))}
        assertThrows<IllegalArgumentException> {ByteConversions.bytesToDouble(byteArrayOf(0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A))}
    }

}
