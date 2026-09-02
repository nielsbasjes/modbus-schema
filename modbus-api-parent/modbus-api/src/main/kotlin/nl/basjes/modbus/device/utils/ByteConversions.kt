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
import nl.basjes.modbus.device.api.RegisterBlock
import java.nio.charset.StandardCharsets
import kotlin.math.min

const val SHORT_BYTES        = 2
const val SHORT_REGISTERS    = SHORT_BYTES/2
const val INTEGER_BYTES      = 4
const val INTEGER_REGISTERS  = INTEGER_BYTES/2
const val LONG_BYTES         = 8
const val LONG_REGISTERS     = LONG_BYTES/2
const val FLOAT_BYTES        = 4
const val FLOAT_REGISTERS    = FLOAT_BYTES/2
const val DOUBLE_BYTES       = 8
const val DOUBLE_REGISTERS   = DOUBLE_BYTES/2

const val BITS_PER_BYTE      = 8
const val BYTES_PER_REGISTER = 2
const val BITS_PER_REGISTER  = BYTES_PER_REGISTER * BITS_PER_BYTE

const val BITS16_IN_REGISTERS = 1
const val BITS32_IN_REGISTERS = 2
const val BITS48_IN_REGISTERS = 3
const val BITS64_IN_REGISTERS = 4

const val BITS16_IN_BYTES = 2
const val BITS32_IN_BYTES = 4
const val BITS48_IN_BYTES = 6
const val BITS64_IN_BYTES = 8


private fun assertByteArraySize(
    bytes: ByteArray,
    expectedSize: Int,
    targetType: String,
) {
    require(bytes.size == expectedSize) {
        "A 'byte[]' to '$targetType' must have exactly $expectedSize bytes instead of the provided ${bytes.size} bytes."
    }
}

fun ByteArray?.toHexString0x(): String {
    if (this == null || isEmpty()) {
        return ""
    }
    return "0x" + toHexStringList().joinToString(separator = " 0x")
}

fun ByteArray.toSeparatedHexString(
    separator: String,
): String {
    if (isEmpty()) {
        return ""
    }
    return toHexStringList().joinToString(separator = separator)
}

fun ByteArray.toSeparatedIntegerString(
    bytes: ByteArray,
    separator: String,
): String {
    if (isEmpty()) {
        return ""
    }
    val result: MutableList<String> = ArrayList()
    for (aByte in bytes) {
        result.add(String.format("%d", aByte.toInt()))
    }
    return result.joinToString(separator = separator)
}

fun ByteArray.toSeparatedUnsignedIntegerString(
    separator: String,
): String {
    if (isEmpty()) {
        return ""
    }
    val result: MutableList<String> = ArrayList()
    for (aByte in this) {
        result.add(String.format("%d", aByte.toUnsignedLong()))
    }
    return result.joinToString(separator = separator)
}

fun Byte.toUnsignedLong(): Long {
    val longBytes =
        byteArrayOf(
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            this,
        )
    return longBytes.toLong()
}

fun ByteArray.toSeparatedTwoByteHexString(
    separator: String,
): String {
    if (isEmpty()) {
        return ""
    }
    return toTwoByteHexStringList().joinToString(separator = separator)
}

fun ByteArray?.toHexStringList(): List<String> {
    if (this == null || isEmpty()) {
        return emptyList()
    }

    val result: MutableList<String> = ArrayList()
    for (aByte in this) {
        result.add(String.format("%02X", aByte))
    }
    return result
}

fun ByteArray?.toTwoByteHexStringList(): List<String> {
    if (this == null || isEmpty()) {
        return emptyList()
    }

    val result: MutableList<String> = ArrayList()
    var i = 0
    while (i < size) {
        result.add(String.format("%02X%02X", this[i], this[i + 1]))
        i += 2
    }
    return result
}

// From https://stackoverflow.com/a/33678350/114196 + modifications to fit my needs
fun splitStringBySize(
    str: String,
    size: Int,
): List<String> {
    val split = ArrayList<String>()
    for (i in 0..str.length / size) {
        val substring =
            str.substring(
                i * size,
                min(((i + 1) * size).toDouble(), str.length.toDouble())
                    .toInt(),
            )
        if (substring.isNotEmpty()) {
            split.add(substring)
        }
    }
    return split
}

fun hexStringToBytes(input: String): ByteArray {
    val hexByteValues =
        input
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
fun allAreOfSize(
    arrayOfByteArrays: Array<ByteArray>,
    size: Int,
): Boolean {
    for (byteArrays in arrayOfByteArrays) {
        if (byteArrays.size != size) {
            return false
        }
    }
    return true
}

fun arrayOfByteArraysContains(
    arrayOfByteArrays: Array<ByteArray>,
    bytes: ByteArray,
): Boolean {
    for (byteArray in arrayOfByteArrays) {
        if (byteArray.contentEquals(bytes)) {
            return true
        }
    }
    return false
}

fun isInRange(
    b: Byte,
    first: UInt,
    last: UInt,
): Boolean {
    val unsignedByte = b.toUInt() and 0xFFu
    return (unsignedByte in first..last)
}

/**
 * @param bytesInChar The number of bytes to check: (1,2,3,4)
 * @return Is the provided byte value valid for a UTF-8 byte at the provided index.
 */
private fun ByteArray.isValidUtf8(
    bytesInChar: Int,
): Boolean {
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

    @Suppress("ktlint:standard:indent")
    return when (bytesInChar) {
        1 -> {
            isInRange(this[0], 0x01u, 0x7Fu)
        }

        2 -> {
            isInRange(this[0], 0xC2u, 0xDFu) &&
            isInRange(this[1], 0x80u, 0xBFu)
        }

        3 -> {
            (
                isInRange(this[0], 0xE0u, 0xE0u) &&
                isInRange(this[1], 0xA0u, 0xBFu) &&
                isInRange(this[2], 0x80u, 0xBFu)
            ) ||
            (
                isInRange(this[0], 0xE1u, 0xECu) &&
                isInRange(this[1], 0x80u, 0xBFu) &&
                isInRange(this[2], 0x80u, 0xBFu)
            ) ||
            (
                isInRange(this[0], 0xEDu, 0xEDu) &&
                isInRange(this[1], 0x80u, 0x9Fu) &&
                isInRange(this[2], 0x80u, 0xBFu)
            ) ||
            (
                isInRange(this[0], 0xEEu, 0xEFu) &&
                isInRange(this[1], 0x80u, 0xBFu) &&
                isInRange(this[2], 0x80u, 0xBFu)
            )
        }

        4 -> {
            (
                isInRange(this[0], 0xF0u, 0xF0u) &&
                isInRange(this[1], 0x90u, 0xBFu) &&
                isInRange(this[2], 0x80u, 0xBFu) &&
                isInRange(this[3], 0x80u, 0xBFu)
            ) ||
            (
                isInRange(this[0], 0xF1u, 0xF3u) &&
                isInRange(this[1], 0x80u, 0xBFu) &&
                isInRange(this[2], 0x80u, 0xBFu) &&
                isInRange(this[3], 0x80u, 0xBFu)
            ) ||
            (
                isInRange(this[0], 0xF4u, 0xF4u) &&
                isInRange(this[1], 0x80u, 0x8Fu) &&
                isInRange(this[2], 0x80u, 0xBFu) &&
                isInRange(this[3], 0x80u, 0xBFu)
            )
        }

        else -> {
            false
        }
    }
}

/**
 * Convert the provided byte array to a String using UTF8
 * The ONLY reason for having custom code here is that the standard Java implementation
 * simply replaces bad characters with "something". I want to terminate on the first bad character.
 * @return An instance of String or null if the input was null.
 */
fun ByteArray.toUtf8String(): String? {
    if (isEmpty() || this[0].toInt() == 0) {
        return ""
    }

    val sb = StringBuilder()

    var invalidString = false
    // NOTE: The normal String methods do not do error handling the way needed here.
    val nextChar = ByteArray(4) // 4 is the max number of bytes used by UTF-8
    var i = 0
    while (i < size) {
        val bytesInChar =
            when {
                // Byte 1 = 0xxxxxxx --> 1 byte char
                ((this[i].toInt() and 0x80.toByte().toInt()) == 0x00.toByte().toInt()) -> 1

                // Byte 1 = 110xxxxx --> 2 byte char
                ((this[i].toInt() and 0xE0.toByte().toInt()) == 0xC0.toByte().toInt()) -> 2

                // Byte 1 = 1110xxxx --> 3 byte char
                ((this[i].toInt() and 0xF0.toByte().toInt()) == 0xE0.toByte().toInt()) -> 3

                // Byte 1 = 11110xxx --> 4 byte char
                ((this[i].toInt() and 0xF8.toByte().toInt()) == 0xF0.toByte().toInt()) -> 4

                // Error Illegal character, stop
                else -> break
            }

        for (b in 0 until bytesInChar) {
            if (i == size) {
                invalidString = true
                break
            }
            nextChar[b] = this[i++]
        }

        if (!nextChar.isValidUtf8(bytesInChar)) {
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

// ----------------------------------------------
fun Short.toByteArray(): ByteArray {
    var value = this
    val result = ByteArray(SHORT_BYTES)
    for (i in SHORT_BYTES - 1 downTo 0) {
        result[i] = (value.toInt() and 0xFF).toByte()
        value = (value.toInt() shr Byte.SIZE_BITS).toShort()
    }
    return result
}

fun ByteArray.toShort(): Short {
    assertByteArraySize(this, SHORT_BYTES, "short")
    var result: Short = 0
    for (i in 0 until SHORT_BYTES) {
        result = (result.toInt() shl Byte.SIZE_BITS).toShort()
        result = (result.toInt() or (this[i].toShort().toInt() and 0xFF.toShort().toInt()).toShort().toInt()).toShort()
    }
    return result
}

// ----------------------------------------------
fun Int.toByteArray(): ByteArray {
    var value = this
    val result = ByteArray(INTEGER_BYTES)
    for (i in INTEGER_BYTES - 1 downTo 0) {
        result[i] = (value and 0xFF).toByte()
        value = value shr Byte.SIZE_BITS
    }
    return result
}

fun ByteArray.toInteger(): Int {
    assertByteArraySize(this, INTEGER_BYTES, "int")
    var result = 0
    for (i in 0 until INTEGER_BYTES) {
        result = result shl Byte.SIZE_BITS
        result = result or (this[i].toInt() and 0xFF)
    }
    return result
}

// ----------------------------------------------
fun Long.toByteArray(): ByteArray {
    var value = this
    val result = ByteArray(LONG_BYTES)
    for (i in LONG_BYTES - 1 downTo 0) {
        result[i] = (value and 0xFFL).toByte()
        value = value shr Byte.SIZE_BITS
    }
    return result
}

fun ByteArray.toLong(): Long {
    assertByteArraySize(this, LONG_BYTES, "long")
    var result: Long = 0
    for (i in 0 until LONG_BYTES) {
        result = result shl Byte.SIZE_BITS
        result = result or (this[i].toInt() and 0xFF).toLong()
    }
    return result
}

// ----------------------------------------------
fun Float.toByteArray(): ByteArray = toBits().toByteArray()

fun ByteArray.toFloat(): Float {
    assertByteArraySize(this, FLOAT_BYTES, "float")
    return Float.fromBits(toInteger())
}

// ----------------------------------------------
fun Double.toByteArray(): ByteArray = toBits().toByteArray()

fun ByteArray.toDouble(): Double {
    assertByteArraySize(this, DOUBLE_BYTES, "double")
    return Double.fromBits(toLong())
}

// ----------------------------------------------
// function swaps the array's first element with last
// element, second element with last second element and
// so on
fun ByteArray.reverse() {
    var temp: Byte
    var index = 0
    while (index < size / 2) {
        temp = this[index]
        this[index] = this[size - index - 1]
        this[size - index - 1] = temp
        index++
    }
}

fun ByteArray.toRegisters(startingAddress: Address): RegisterBlock {
    val registerBlock = RegisterBlock(startingAddress.addressClass)
    require(size%2 == 0 ) { "Only an even number of bytes" } // FIXME: Can do better
    var address = startingAddress
    (0 until this.size/2).forEach { registerNr ->
        val byteIndex=registerNr*2
        registerBlock[address] = sliceArray(byteIndex..byteIndex+1).toShort()
        address++
    }
    return registerBlock
}

fun Short   .toRegisters(startingAddress: Address): RegisterBlock = toByteArray().toRegisters(startingAddress)
fun Int     .toRegisters(startingAddress: Address): RegisterBlock = toByteArray().toRegisters(startingAddress)
fun Long    .toRegisters(startingAddress: Address): RegisterBlock = toByteArray().toRegisters(startingAddress)
fun Float   .toRegisters(startingAddress: Address): RegisterBlock = toByteArray().toRegisters(startingAddress)
fun Double  .toRegisters(startingAddress: Address): RegisterBlock = toByteArray().toRegisters(startingAddress)

fun RegisterBlock.getShort   (startingAddress: Address): Short  ? = getByteArray(startingAddress, SHORT_REGISTERS   )?.toShort  ()
fun RegisterBlock.getInteger (startingAddress: Address): Int    ? = getByteArray(startingAddress, INTEGER_REGISTERS )?.toInteger()
fun RegisterBlock.getLong    (startingAddress: Address): Long   ? = getByteArray(startingAddress, LONG_REGISTERS    )?.toLong   ()
fun RegisterBlock.getFloat   (startingAddress: Address): Float  ? = getByteArray(startingAddress, FLOAT_REGISTERS   )?.toFloat  ()
fun RegisterBlock.getDouble  (startingAddress: Address): Double ? = getByteArray(startingAddress, DOUBLE_REGISTERS  )?.toDouble ()
