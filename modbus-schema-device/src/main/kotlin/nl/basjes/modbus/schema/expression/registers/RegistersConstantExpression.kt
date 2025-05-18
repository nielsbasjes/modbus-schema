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
package nl.basjes.modbus.schema.expression.registers

import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.BYTES_PER_REGISTER
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.utils.ByteConversions
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToHexStringList
import nl.basjes.modbus.schema.utils.ByteConversions.bytesToTwoByteHexStringList

/*
 * An constant byte array value (i.e. Raw Register values)
 */
open class RegistersConstantExpression(
    val value: String,
) : RegistersExpression {

    private val theBytes: ByteArray = ByteConversions.hexStringToBytes(value)

    override val returnedRegisters: Int
        get() = theBytes.size / BYTES_PER_REGISTER

    override var isImmutable: Boolean = true

    override val problems: List<Problem> = listOf()

    override fun toString(): String = '"' + ByteConversions.bytesToHexString(theBytes) + '"'

    override fun getByteArray(schemaDevice: SchemaDevice): ByteArray = theBytes

    /**
     * @return The bytes as a list of HEX ascii Strings (1 byte each)
     */
    @Suppress("unused") // Used by code generating templates
    val asByteHexStrings: List<String> = bytesToHexStringList(theBytes)

    /**
     * @return The registers as a list of HEX ascii Strings (2 bytes each)
     */
    @Suppress("unused") // Used by code generating templates
    val asRegisterHexStrings: List<String> = bytesToTwoByteHexStringList(theBytes)
}
