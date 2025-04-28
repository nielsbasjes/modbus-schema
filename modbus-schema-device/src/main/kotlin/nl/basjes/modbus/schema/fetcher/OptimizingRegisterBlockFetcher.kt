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

import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.schema.SchemaDevice

class OptimizingRegisterBlockFetcher(schemaDevice: SchemaDevice, modbusDevice: ModbusDevice) :
        RegisterBlockFetcher(schemaDevice, modbusDevice) {
    /**
     * How many registers may needlessly be read to optimize fetching
     */
    var allowedGapReadSize = 0
        set(value) {
            require(value>=0) { "A negative Gap Read Size is not allowed" }
            field = value
        }

    override fun calculateFetchBatches(maxAge: Long): List<FetchBatch> {
        val baseFetchBatchList = super.calculateFetchBatches(maxAge)
        val fetchBatches: MutableList<FetchBatch> = ArrayList()

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
        var nextBatch = FetchBatch()
        nextBatch.start = nextInput.start
        nextBatch.count = nextInput.count
        fetchBatches.add(nextBatch)

        while (fetchBatchIterator.hasNext()) {
            nextInput = fetchBatchIterator.next()

            if (nextBatch.start!!.addressClass != nextInput.start!!.addressClass) {
                // Different addressClass is ALWAYS start a new batch
                nextBatch = FetchBatch()
                nextBatch.start = nextInput.start
                nextBatch.count = nextInput.count
                fetchBatches.add(nextBatch)
                continue
            }

            val lastOfNextBatch = nextBatch.start!!.increment(nextBatch.count)

            if (nextInput.start!! == lastOfNextBatch) {
                // Clean append without gaps
                if (nextBatch.count + nextInput.count <= modbusDevice.maxRegistersPerModbusRequest) {
                    // Merge
                    nextBatch.count += nextInput.count
                } else {
                    // DO NOT Merge
                    nextBatch = FetchBatch()
                    nextBatch.start = nextInput.start
                    nextBatch.count = nextInput.count
                    fetchBatches.add(nextBatch)
                }
                continue
            }

            // We have a gap between nextBatch and nextInput and we MAY read those also!
            val nextBatchStart = nextBatch.start!!.physicalAddress
            val nextInputStart = nextInput.start!!.physicalAddress
            val gapSize = nextInputStart - (nextBatchStart + nextBatch.count)
            val mergedCount = nextInputStart + nextInput.count - nextBatchStart
            if (gapSize <= allowedGapReadSize &&  // Do NOT jump more than N registers
                mergedCount <= modbusDevice.maxRegistersPerModbusRequest
            ) {
                nextBatch.count = mergedCount
            } else {
                // DO NOT Merge
                nextBatch = FetchBatch()
                nextBatch.start = nextInput.start
                nextBatch.count = nextInput.count
                fetchBatches.add(nextBatch)
            }
        }

        return fetchBatches
    }
}
