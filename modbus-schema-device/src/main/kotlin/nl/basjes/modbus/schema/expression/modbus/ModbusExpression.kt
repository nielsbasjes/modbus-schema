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
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.Expression.Problem
import java.util.TreeSet

/*
 * An Expression for which the result is a byte array of raw register values
 */
sealed class ModbusExpression(
    protected val addresses: List<Address>,
) : Expression {

    override val requiredAddresses: List<Address>
        get() = addresses

    override var isImmutable: Boolean = false

    override val requiredMutableAddresses: List<Address>
        get() = addresses

    override val problems: List<Problem>
        get() {
            if (addresses.isEmpty()) {
                return listOf(Problem("No addresses"))
            }
            // All registers must be a single incremental unique list without any gaps !
            // But they MAY be used out of order (so we sort them first for this check) !!!!
            val sortedSet = TreeSet(addresses)
            if (sortedSet.size != addresses.size) {
                return listOf(Problem("Duplicate addresses: $addresses")) // Apparently duplicates
            }

            var expectedAddress: Address? = null
            for (address in sortedSet) {
                if (expectedAddress == null) {
                    expectedAddress = address
                }
                if (expectedAddress!! != address) {
                    return listOf(Problem("Illegal Address range specified: $addresses")) // Apparently duplicates
                }
                expectedAddress = expectedAddress.increment()
            }

            return listOf()
        }

    private val isSortedList
        get() =
            if (problems.isEmpty()) {
                addresses == ArrayList(TreeSet(addresses))
            } else {
                false
            }

    protected val addressClass: AddressClass
        get() = addresses[0].addressClass

    override fun toString(): String {
        if (isSortedList) {
            if (addresses.size > 1) {
                return addresses[0].toString() + " # " + addresses.size
            }
            return addresses[0].toString()
        }
        return addresses.joinToString(separator = ", ") { it.toString() }
    }
}
