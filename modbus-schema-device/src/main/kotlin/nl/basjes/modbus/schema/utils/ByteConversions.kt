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

import nl.basjes.modbus.schema.expression.DOUBLE_BYTES
import nl.basjes.modbus.schema.expression.FLOAT_BYTES
import nl.basjes.modbus.schema.expression.INTEGER_BYTES
import nl.basjes.modbus.schema.expression.LONG_BYTES
import nl.basjes.modbus.schema.expression.SHORT_BYTES
import java.nio.charset.StandardCharsets
import kotlin.math.min

object ByteConversions {
    private fun assertByteArraySize(bytes: ByteArray, expectedSize: Int, targetType: String) {
        require(bytes.size == expectedSize) {
            "A 'byte[]' to '$targetType' must have exactly $expectedSize bytes instead of the provided ${bytes.size} bytes."
        }
    }

    fun bytesToHexString(bytes: ByteArray): String {
        if (bytes.isEmpty()) {
            return ""
        }
        val result: MutableList<String> = ArrayList()
        for (aByte in bytes) {
            result.add(String.format("%02X", aByte))
        }
        return "0x" + result.joinToString(separator = " 0x")
    }

    fun bytesToSeparatedHexString(bytes: ByteArray, separator: String): String {
        if (bytes.isEmpty()) {
            return ""
        }
        val result: MutableList<String> = ArrayList()
        for (aByte in bytes) {
            result.add(String.format("%02X", aByte))
        }
        return result.joinToString(separator = separator)
    }

    fun bytesToSeparatedIntegerString(bytes: ByteArray, separator: String): String {
        if (bytes.isEmpty()) {
            return ""
        }
        val result: MutableList<String> = ArrayList()
        for (aByte in bytes) {
            result.add(String.format("%d", aByte.toInt()))
        }
        return result.joinToString(separator = separator)
    }

    fun bytesToSeparatedTwoByteHexString(bytes: ByteArray, separator: String): String {
        if (bytes.isEmpty()) {
            return ""
        }

        val result: MutableList<String> = ArrayList()
        var i = 0
        while (i < bytes.size) {
            result.add(String.format("%02X%02X", bytes[i], bytes[i + 1]))
            i += 2
        }
        return result.joinToString(separator = separator)
    }

    // From https://stackoverflow.com/a/33678350/114196 + modifications to fit my needs
    fun splitStringBySize(str: String, size: Int): List<String> {
        val split = ArrayList<String>()
        for (i in 0..str.length / size) {
            val substring = str.substring(
                i * size, min(((i + 1) * size).toDouble(), str.length.toDouble())
                    .toInt()
            )
            if (substring.isNotEmpty()) {
                split.add(substring)
            }
        }
        return split
    }

    fun hexStringToBytes(input: String): ByteArray {
        val hexByteValues = input
            .replace("0x", "")
            .replace(" ", "")
            .trim { it <= ' ' }

        val result = ByteArray(hexByteValues.length / 2)
        var nextByteIndex = 0
        for (word in splitStringBySize(hexByteValues, 2)) {
            var toParse = word.trim { it <= ' ' }
            if (toParse.isEmpty()) {
                continue  // Skip completely empty values
            }
            toParse = toParse.lowercase()
            val parsedInt = toParse.toInt(16) // 16 because of Hex ...
            val value = (parsedInt and 0x00FF).toByte()
            result[nextByteIndex++] = value
        }
        return result
    }

    fun hexStringToBytes(input: List<String>): Array<ByteArray> {
        if (input.isEmpty()) {
            return arrayOf()
        }
        val result = mutableListOf<ByteArray>()
        for (index in input.indices) {
            result.add(index, hexStringToBytes(input[index]))
        }
        return result.toTypedArray()
    }

    /**
     * Determine if all provided byte arrays are the allowed size
     * @param arrayOfByteArrays The array of byte arrays
     * @param size The allowed size
     * @return true if all arrays are of an allowed size
     */
    fun allAreOfSize(arrayOfByteArrays: Array<ByteArray>, size: Int): Boolean {
        for (byteArrays in arrayOfByteArrays) {
            if (byteArrays.size != size) {
                return false
            }
        }
        return true
    }

    fun arrayOfByteArraysContains(arrayOfByteArrays: Array<ByteArray>, bytes: ByteArray): Boolean {
        for (byteArray in arrayOfByteArrays) {
            if (byteArray.contentEquals(bytes)) {
                return true
            }
        }
        return false
    }


    fun bytesToString(byteList: List<Byte>): String {
        val bytes = ByteArray(byteList.size)
        var i = 0
        while (i < byteList.size) {
            val nextByte = byteList[i]
            // https://en.wikipedia.org/wiki/UTF-8#Encoding
            // https://stackoverflow.com/a/6907327/114196
            // There is no other Unicode code point that will be encoded in UTF8 with a zero byte anywhere within it.
            if (nextByte.toInt() == 0x00) {
                break
            }
            bytes[i] = nextByte
            i++
        }

        return String(bytes, 0, i, StandardCharsets.UTF_8)
    }

    fun isInRange(b: Byte, first: UInt, last: UInt): Boolean {
        val unsignedByte = b.toUInt() and 0xFFu
        return (unsignedByte in first..last)
    }

    /**
     * @param bytes The bytes to check
     * @param bytesInChar The number of bytes to check: (1,2,3,4)
     * @return Is the provided byte value valid for a UTF-8 byte at the provided index.
     */
    private fun isValidUtf8(bytes: ByteArray, bytesInChar: Int): Boolean {
        // https://www.unicode.org/versions/Unicode13.0.0/ch03.pdf
        // Code Points         1st Byte     2nd Byte    3rd Byte    4th Byte
        // U+0000..U+007F      00..7F
        // U+0080..U+07FF      C2..DF       80..BF
        // U+0800..U+0FFF      E0           A0..BF      80..BF
        // U+1000..U+CFFF      E1..EC       80..BF      80..BF
        // U+D000..U+D7FF      ED           80..9F      80..BF
        // U+E000..U+FFFF      EE..EF       80..BF      80..BF
        // U+10000..U+3FFFF    F0           90..BF      80..BF      80..BF
        // U+40000..U+FFFFF    F1..F3       80..BF      80..BF      80..BF
        // U+100000..U+10FFFF  F4           80..8F      80..BF      80..BF

        return when (bytesInChar) {
            1 -> isInRange(bytes[0], 0x01u, 0x7Fu)
            2 -> isInRange(bytes[0], 0xC2u, 0xDFu) && isInRange(bytes[1], 0x80u, 0xBFu)
            3 -> isInRange(bytes[0], 0xE0u, 0xE0u) && isInRange(bytes[1], 0xA0u, 0xBFu) && isInRange(
                bytes[2],
                0x80u,
                0xBFu,
            ) ||
                (isInRange(bytes[0], 0xE1u, 0xECu) && isInRange(bytes[1], 0x80u, 0xBFu) && isInRange(
                    bytes[2],
                    0x80u,
                    0xBFu
                )) ||
                (isInRange(bytes[0], 0xEDu, 0xEDu) && isInRange(
                    bytes[1],
                    0x80u,
                    0x9Fu
                ) && isInRange(bytes[2], 0x80u, 0xBFu)) ||
                (isInRange(bytes[0], 0xEEu, 0xEFu) && isInRange(
                    bytes[1],
                    0x80u,
                    0xBFu
                ) && isInRange(bytes[2], 0x80u, 0xBFu))

            4 -> isInRange(bytes[0], 0xF0u, 0xF0u) && isInRange(
                bytes[1],
                0x90u,
                0xBFu
            ) && isInRange(bytes[2], 0x80u, 0xBFu) && isInRange(bytes[3], 0x80u, 0xBFu) ||
                (isInRange(bytes[0], 0xF1u, 0xF3u) && isInRange(
                    bytes[1],
                    0x80u,
                    0xBFu
                ) && isInRange(bytes[2], 0x80u, 0xBFu) && isInRange(bytes[3], 0x80u, 0xBFu)) ||
                (isInRange(bytes[0], 0xF4u, 0xF4u) && isInRange(
                    bytes[1],
                    0x80u,
                    0x8Fu
                ) && isInRange(bytes[2], 0x80u, 0xBFu) && isInRange(bytes[3], 0x80u, 0xBFu))

            else -> false
        }
    }

    /**
     * Convert the provided byte array to a String using UTF8
     * The ONLY reason for having custom code here is that the standard Java implementation
     * simply replaces bad characters with "something". I want to terminate on the first bad character.
     * @param bytes An array of bytes to be converted
     * @return An instance of String or null if the input was null.
     */
    fun bytesToString(bytes: ByteArray): String? {
        if (bytes.isEmpty() || bytes[0].toInt() == 0) {
            return ""
        }

        val sb = StringBuilder()

        var invalidString = false
        // NOTE: The normal String methods do not do error handling the way needed here.
        val nextChar = ByteArray(4) // 4 is the max number of bytes used by UTF-8
        var i = 0
        while (i < bytes.size) {
            val bytesInChar =
                if ((bytes[i].toInt() and 0x80.toByte().toInt()) == 0x00.toByte().toInt()) {
                    // Byte 1 = 0xxxxxxx --> 1 byte char
                    1
                }
                else
                if ((bytes[i].toInt() and 0xE0.toByte().toInt()) == 0xC0.toByte().toInt()) {
                    // Byte 1 = 110xxxxx --> 2 byte char
                    2
                }
                else
                if ((bytes[i].toInt() and 0xF0.toByte().toInt()) == 0xE0.toByte().toInt()) {
                    // Byte 1 = 1110xxxx --> 3 byte char
                    3
                }
                else
                if ((bytes[i].toInt() and 0xF8.toByte().toInt()) == 0xF0.toByte().toInt()) {
                    // Byte 1 = 11110xxx --> 4 byte char
                    4
                }
                else
                {
                    // Error Illegal character, stop
                    break
                }

            for (b in 0 until bytesInChar) {
                if (i == bytes.size) {
                    invalidString = true
                    break
                }
                nextChar[b] = bytes[i++]
            }

            if (!isValidUtf8(nextChar, bytesInChar)) {
                invalidString = true
                break
            }

            if (invalidString) {
                break
            }
            sb.append(String(nextChar, 0, bytesInChar, StandardCharsets.UTF_8))
        }
        val string = sb.toString()
        if (invalidString && string.isEmpty()) {
            return null
        }
        return string
    }

    fun stringToBytes(value: String): ByteArray {
        return value.toByteArray(StandardCharsets.UTF_8)
    }

    // ----------------------------------------------
    fun shortToBytes(input: Short): ByteArray {
        var value = input
        val result = ByteArray(nl.basjes.modbus.schema.expression.SHORT_BYTES)
        for (i in nl.basjes.modbus.schema.expression.SHORT_BYTES - 1 downTo 0) {
            result[i] = (value.toInt() and 0xFF).toByte()
            value = (value.toInt() shr Byte.SIZE_BITS).toShort()
        }
        return result
    }

    fun bytesToShort(bytes: ByteArray): Short {
        assertByteArraySize(bytes, SHORT_BYTES, "short")
        var result: Short = 0
        for (i in 0 until nl.basjes.modbus.schema.expression.SHORT_BYTES) {
            result = (result.toInt() shl Byte.SIZE_BITS).toShort()
            result = (result.toInt() or (bytes[i]
                .toShort().toInt() and 0xFF.toShort().toInt()).toShort().toInt()).toShort()
        }
        return result
    }

    // ----------------------------------------------
    fun integerToBytes(input: Int): ByteArray {
        var value = input
        val result = ByteArray(nl.basjes.modbus.schema.expression.INTEGER_BYTES)
        for (i in nl.basjes.modbus.schema.expression.INTEGER_BYTES - 1 downTo 0) {
            result[i] = (value and 0xFF).toByte()
            value = value shr Byte.SIZE_BITS
        }
        return result
    }

    fun bytesToInteger(bytes: ByteArray): Int {
        assertByteArraySize(bytes, INTEGER_BYTES, "int")
        var result = 0
        for (i in 0 until nl.basjes.modbus.schema.expression.INTEGER_BYTES) {
            result = result shl Byte.SIZE_BITS
            result = result or (bytes[i].toInt() and 0xFF)
        }
        return result
    }

    // ----------------------------------------------
    fun longToBytes(input: Long): ByteArray {
        var value = input
        val result = ByteArray(nl.basjes.modbus.schema.expression.LONG_BYTES)
        for (i in nl.basjes.modbus.schema.expression.LONG_BYTES - 1 downTo 0) {
            result[i] = (value and 0xFFL).toByte()
            value = value shr Byte.SIZE_BITS
        }
        return result
    }

    fun bytesToLong(bytes: ByteArray): Long {
        assertByteArraySize(bytes, LONG_BYTES, "long")
        var result: Long = 0
        for (i in 0 until nl.basjes.modbus.schema.expression.LONG_BYTES) {
            result = result shl Byte.SIZE_BITS
            result = result or (bytes[i].toInt() and 0xFF).toLong()
        }
        return result
    }

    // ----------------------------------------------
    fun floatToBytes(value: Float): ByteArray {
        return integerToBytes(value.toBits())
    }

    fun bytesToFloat(bytes: ByteArray): Float {
        assertByteArraySize(bytes, FLOAT_BYTES, "float")
        return Float.fromBits(bytesToInteger(bytes))
    }

    // ----------------------------------------------
    fun doubleToBytes(value: Double): ByteArray {
        return longToBytes(value.toBits())
    }

    fun bytesToDouble(bytes: ByteArray): Double {
        assertByteArraySize(bytes, DOUBLE_BYTES, "double")
        return Double.fromBits(bytesToLong(bytes))
    }

    // ----------------------------------------------
    // function swaps the array's first element with last
    // element, second element with last second element and
    // so on
    fun reverse(bytes: ByteArray) {
        val size = bytes.size
        var temp: Byte
        var index = 0
        while (index < size / 2) {
            temp = bytes[index]
            bytes[index] = bytes[size - index - 1]
            bytes[size - index - 1] = temp
            index++
        }
    }
}
