/*
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
package nl.basjes.modbus.schema.expression.generic

import nl.basjes.modbus.device.utils.BYTES_PER_REGISTER
import nl.basjes.modbus.device.utils.allAreOfSize
import nl.basjes.modbus.device.utils.arrayOfByteArraysContains
import nl.basjes.modbus.device.utils.hexStringToBytes
import nl.basjes.modbus.device.utils.toSeparatedTwoByteHexString
import nl.basjes.modbus.device.utils.toTwoByteHexStringList
import nl.basjes.modbus.schema.expression.Expression

abstract class NotImplemented(
    private val expectedRegisters: Int,
    notImplementedStrings: List<String>,
) : Expression {
    override fun toString(): String {
        if (notImplementedStrings.isEmpty()) {
            return ""
        }
        return " ; " + notImplementedStrings.joinToString(separator = " ; ")
    }

    private val notImplementedBytes: Array<ByteArray> = hexStringToBytes(notImplementedStrings)
    private val notImplementedStrings: MutableList<String> = mutableListOf()
    val notImplemented: MutableList<List<String>> = mutableListOf()

    init {
        for (notImplementedByte in notImplementedBytes) {
            this.notImplementedStrings.add(
                "0x" + notImplementedByte.toSeparatedTwoByteHexString(" 0x"),
            )
            this.notImplemented.add(notImplementedByte.toTwoByteHexStringList())
        }
    }

    override val problems: List<Expression.Problem>
        get() =
            combine(
                "NotImplemented",
                checkFatal(
                    isValidNotImplemented(expectedRegisters * BYTES_PER_REGISTER),
                    "Wrong number of registers: Got ${notImplementedBytes.size}, need ${expectedRegisters * BYTES_PER_REGISTER}",
                ),
            )

    fun isValidNotImplemented(byteCount: Int): Boolean = allAreOfSize(notImplementedBytes, byteCount)

    fun isNotImplemented(bytes: ByteArray): Boolean = arrayOfByteArraysContains(notImplementedBytes, bytes)
}
