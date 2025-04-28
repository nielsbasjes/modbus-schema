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
package nl.basjes.modbus.schema.expression.numbers

import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
import nl.basjes.modbus.schema.utils.ByteConversions

class IntegerSigned32(
    private val byteArray: RegistersExpression,
    notImplemented: List<String>,
) : IntegerSigned("int32", nl.basjes.modbus.schema.expression.INTEGER_BYTES, byteArray, notImplemented) {

    override fun getValueAsLong(schemaDevice: SchemaDevice): Long? {
        val bytes = byteArray.getByteArray(schemaDevice) ?: return null
        if (isNotImplemented(bytes)) {
            return null // Not implemented
        }
        return ByteConversions.bytesToInteger(bytes).toLong()
    }

}
