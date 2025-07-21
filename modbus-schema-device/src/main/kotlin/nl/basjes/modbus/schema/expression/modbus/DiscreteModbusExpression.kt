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
package nl.basjes.modbus.schema.expression.modbus

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.DiscreteBlock
import nl.basjes.modbus.device.api.DiscreteValue
import nl.basjes.modbus.device.api.ModbusValue
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression.Problem
import nl.basjes.modbus.schema.expression.booleans.BooleanExpression

/*
 * An Expression for which the result is a raw boolean values
 */
class DiscreteModbusExpression(
    val address: Address,
) : BooleanExpression, ModbusExpression(listOf(address)) {

    override fun toString(): String =
        "boolean($address)"

    override val problems: List<Problem>
        get() =
            combine(
                "boolean",
                super<BooleanExpression>.problems,
            )

    override fun getBoolean(schemaDevice: SchemaDevice): Boolean? {
        val discreteBlock = schemaDevice.getModbusBlock(addressClass)
        require(discreteBlock is DiscreteBlock) {
            "This should occur: doing getBoolean() on address $addresses (not discrete)."
        }
        return discreteBlock[address].value
    }

    override fun getModbusValues(schemaDevice: SchemaDevice): List<ModbusValue<*, *>> {
        val discreteValues = ArrayList<DiscreteValue>()
        val discreteBlock = schemaDevice.getModbusBlock(addressClass)
        require(discreteBlock is DiscreteBlock) {
            "This should occur: doing getModbusValues() on address $addresses (not discrete)."
        }
        discreteValues.add(discreteBlock[address])
        return discreteValues
    }

}
