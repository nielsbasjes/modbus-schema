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
package nl.basjes.modbus.schema.fetcher

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.schema.SchemaDevice

class OptimizingRegisterBlockFetcher(
    schemaDevice: SchemaDevice,
    modbusDevice: ModbusDevice,
) : RegisterBlockFetcher(schemaDevice, modbusDevice) {

    /**
     * How many registers may needlessly be read to optimize fetching
     */
    var allowedGapReadSize = 0
        set(value) {
            require(value >= 0) { "A negative Gap Read Size is not allowed" }
            field = value
        }

    override fun calculateModbusQueries(maxAge: Long): List<ModbusQuery> {
        val modbusQueries: MutableList<ModbusQuery> = ArrayList()

        // This is the most fine-grained list of batches.
        val rawFetchBatchList = super.calculateModbusQueries(maxAge)

        // Any raw fetch batch that contains ANY ReadError register is dropped
        // Since these are the smallest possible ones this cannot be repaired.
        val baseFetchBatchList =
            rawFetchBatchList
                .filter {
                    for (field in it.fields) {
                        if (field.isUsingReadErrorRegisters()) {
                            return@filter false
                        }
                    }
                    return@filter true
                }

        val usedAddressClasses =
            baseFetchBatchList
                .flatMap { it.fields }
                .flatMap { it.requiredRegisters }
                .map { it.addressClass }
                .sorted()
                .distinct()

        val readErrorAddresses =
            usedAddressClasses
                .flatMap { schemaDevice.getRegisterBlock(it).values }
                .filter { it.isReadError() }
                .map { it.address }
                .toList()

        val fetchBatchIterator = baseFetchBatchList.iterator()

        if (!fetchBatchIterator.hasNext()) {
            // No input --> no output
            return emptyList()
        }

        // Algorithm concept:
        // Make sure all provided batches have been sorted
        // Copy the next batch from the provided list
        // Then for each following provided batch either merge or not merge.
        var nextInput = fetchBatchIterator.next()
        var nextBatch = MergedModbusQuery(nextInput.start, nextInput.count)
        nextBatch.add(nextInput)
        modbusQueries.add(nextBatch)

        while (fetchBatchIterator.hasNext()) {
            nextInput = fetchBatchIterator.next()

            if (nextBatch.start.addressClass != nextInput.start.addressClass) {
                // Different addressClass is ALWAYS start a new batch
                nextBatch = MergedModbusQuery(nextInput.start, nextInput.count)
                nextBatch.add(nextInput)
                modbusQueries.add(nextBatch)
                continue
            }

            val lastOfNextBatch = nextBatch.start.increment(nextBatch.count)

            if (nextInput.start == lastOfNextBatch) {
                // Clean append without gaps
                if (nextBatch.count + nextInput.count <= modbusDevice.maxRegistersPerModbusRequest) {
                    // Merge
                    nextBatch.count += nextInput.count
                    nextBatch.add(nextInput)
                } else {
                    // DO NOT Merge
                    nextBatch = MergedModbusQuery(nextInput.start, nextInput.count)
                    nextBatch.add(nextInput)
                    modbusQueries.add(nextBatch)
                }
                continue
            }

            // We have a gap between nextBatch and nextInput and we MAY read those also!
            val nextBatchStart = nextBatch.start.physicalAddress
            val nextInputStart = nextInput.start.physicalAddress
            val gapSize = nextInputStart - (nextBatchStart + nextBatch.count)
            val mergedCount = nextInputStart + nextInput.count - nextBatchStart
            if (gapSize <= allowedGapReadSize && // Do NOT jump more than N registers
                mergedCount <= modbusDevice.maxRegistersPerModbusRequest &&
                !readErrorAddresses.overlaps(nextBatch.start, mergedCount) // Do NOT try to read read errors
            ) {
                nextBatch.count = mergedCount
                nextBatch.add(nextInput)
            } else {
                // DO NOT Merge (i.e. start a new one)
                nextBatch = MergedModbusQuery(nextInput.start, nextInput.count)
                nextBatch.add(nextInput)
                modbusQueries.add(nextBatch)
            }
        }

        return modbusQueries
    }

}

fun List<Address>.overlaps(
    firstAddress: Address,
    count: Int,
): Boolean {
    if (this.isEmpty()) {
        return false
    }
    require(count > 0) { "At least one address is required" }

    return this
        .mapNotNull { firstAddress.distance(it) }
        .any { it in 0 until count }
}
