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
package nl.basjes.modbus.schema.expression.strings

import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.generic.NotImplemented
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions
import java.util.BitSet

class BitsetStringList(
    private val registers: RegistersExpression,
    notImplemented: List<String>,
    val mappings: Map<Int, String>,
) : NotImplemented(registers.returnedAddresses, notImplemented),
    StringListExpression {

    override fun toString(): String =
        "bitset(" + registers + super<NotImplemented>.toString() +
            if (mappings.entries.isEmpty()) "" else { " ; " + mappings.entries.joinToString(" ; ") { "${it.key}->'${it.value}'" }} +
            ")"

    override val subExpressions: List<Expression>
        get() = listOf(registers)

    override var isImmutable: Boolean
        get() = registers.isImmutable
        set(value) {
            registers.isImmutable = value
        }

    override val problems: List<Problem>
        get() =
            combine(
                "bitset",
                checkFatal(registers.returnedAddresses > 0, "No registers"),
                checkFatal(
                    registers.returnedAddresses <=
                        nl.basjes.modbus.schema.expression.LONG_BYTES / nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER,
                    "Too many registers",
                ),
                super<StringListExpression>.problems,
                super<NotImplemented>.problems,
            )

    override fun getModbusValues(schemaDevice: SchemaDevice) = registers.getModbusValues(schemaDevice)

    override fun getValueAsStringList(schemaDevice: SchemaDevice): List<String>? {
        val bytes = registers.getByteArray(schemaDevice)
        if (bytes == null || bytes.isEmpty() || isNotImplemented(bytes)) {
            return null // Not implemented
        }
        ByteConversions.reverse(bytes)
        val bitSet = BitSet.valueOf(bytes)

        val result: MutableList<String> = ArrayList()
        for (i in 0 until bitSet.size()) {
            if (bitSet[i]) {
                var value = mappings[i]
                if (value == null) {
                    value = "Bit $i"
                }
                result.add(value)
            }
        }
        return result
    }
}
