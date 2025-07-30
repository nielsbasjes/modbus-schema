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
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice

class OptimizingModbusBlockFetcher(
    schemaDevice: SchemaDevice,
    modbusDevice: ModbusDevice,
) : ModbusBlockFetcher(schemaDevice, modbusDevice) {

    /**
     * How many registers may needlessly be read to optimize fetching
     */
    var allowedGapReadSize = 0
        set(value) {
            require(value >= 0) { "A negative Gap Read Size is not allowed" }
            field = value
        }

    /**
     * Reduce the full set of modbus queries to a lower number by combining requests that are close enough together.
     */
    override fun calculateModbusQueries(fields: List<Field>, maxAge: Long): List<ModbusQuery> {
        // This is the most fine-grained list of Modbus Queries.
        val rawModbusQueryList = super.calculateModbusQueries(fields, maxAge)

        // Any raw ModbusQuery that contains ANY ReadError register is dropped
        // Since these are the smallest possible ones these cannot be repaired.
        val baseModbusQueryList =
            rawModbusQueryList
                .filter {
                    for (field in it.fields) {
                        if (field.isUsingReadErrorRegisters()) {
                            return@filter false
                        }
                    }
                    return@filter true
                }
        return mergeQueries(
            baseModbusQueryList,
            modbusDevice.maxRegistersPerModbusRequest,
            modbusDevice.maxDiscretesPerModbusRequest,
        )
    }

    /**
     * The provided MergedModbusQuery failed.
     * Often this is a query of dozens of fields and 1 or 2 failed.
     * This function will take this query apart and retry the parts until it is clear which
     * query caused the error.
     */
    override fun retryFetchOfFailedMergedModbusQuery(modbusQuery: MergedModbusQuery): List<ModbusQuery> {
        if (modbusQuery.modbusQueries.size == 1) {
            // Nothing to retry, it was already a single query.
            return listOf()
        }

        if (modbusQuery.modbusQueries.size < 4 ) {
            // Such a small list we just try all individually and not optimize
            return modbusQuery.modbusQueries.map { fetch(it) }.flatten()
        }

        // ALGORITHM 1: REMERGE BY NUMBER OF REGISTERS
//        return splitMergedModbusRequest(modbusQuery)
//            { it.count >= (modbusQuery.count / 2) }
//            .map { fetch(it) }.flatten()

        // ALGORITHM 2: REMERGE BY NUMBER OF UNDERLYING REQUESTS
        return splitMergedModbusRequest(modbusQuery)
            { it.modbusQueries.size > (modbusQuery.modbusQueries.size / 2) }
            .map { fetch(it) }.flatten()
    }

    /**
     * Split an existing MergedModbusQuery into 2 or more smaller modbus queries.
     * This simply walks over the underlying modbus queries and recombines them
     * up to a limit which is determined by the 'isFull' lambda.
     */
    fun splitMergedModbusRequest(
        modbusQuery: MergedModbusQuery,
        isFull: (query: MergedModbusQuery) -> Boolean,
    ): List<ModbusQuery> {
        val modbusQueryIterator =  modbusQuery.modbusQueries.iterator()
        val result: MutableList<MergedModbusQuery> = mutableListOf()

        var nextInput = modbusQueryIterator.next()
        var nextModbusQuery = MergedModbusQuery(nextInput.start, nextInput.count)
        nextModbusQuery.add(nextInput)
        result.add(nextModbusQuery)

        while (modbusQueryIterator.hasNext()) {
            nextInput = modbusQueryIterator.next()
            if (isFull(nextModbusQuery)) {
                nextModbusQuery = MergedModbusQuery(nextInput.start, nextInput.count)
                nextModbusQuery.add(nextInput)
                result.add(nextModbusQuery)
                continue
            }
            nextModbusQuery.count += nextInput.count
            nextModbusQuery.add(nextInput)
        }

        // Before we return the results we unmerge the ones that are too small because that would increase the number of requests
        return result.flatMap {
            if (it.modbusQueries.size > 2) listOf(it) else it.modbusQueries
        }
    }

    /**
     * Combine the provided set of queries into a smaller set of Merged Queries where possible.
     */
    fun mergeQueries(
        providedModbusQueries: List<ModbusQuery>,
        maxRegistersPerModbusRequest: Int,
        maxDiscretesPerModbusRequest: Int,
    ): List<ModbusQuery>{
        if (providedModbusQueries.size == 1 ) {
            return providedModbusQueries
        }

        val usedAddressClasses =
            providedModbusQueries
                .flatMap { it.fields }
                .flatMap { it.requiredAddresses }
                .map { it.addressClass }
                .distinct()

        val readErrorAddresses =
            usedAddressClasses
                .flatMap { schemaDevice.getModbusBlock(it).values }
                .filter { it.isReadError() }
                .map { it.address }
                .toList()

        val modbusQueryIterator = providedModbusQueries.sorted().iterator()

        if (!modbusQueryIterator.hasNext()) {
            // No input --> no output
            return emptyList()
        }

        val result: MutableList<ModbusQuery> = mutableListOf()

        // Algorithm concept:
        // Make sure all provided queries have been sorted
        // Copy the next query from the provided list
        // Then for each following provided query either merge or not merge.
        var nextInput = modbusQueryIterator.next()
        var nextModbusQuery = MergedModbusQuery(nextInput.start, nextInput.count)
        nextModbusQuery.add(nextInput)
        result.add(nextModbusQuery)

        while (modbusQueryIterator.hasNext()) {
            nextInput = modbusQueryIterator.next()

            if (nextModbusQuery.start.addressClass != nextInput.start.addressClass) {
                // Different addressClass is ALWAYS start a new query
                nextModbusQuery = MergedModbusQuery(nextInput.start, nextInput.count)
                nextModbusQuery.add(nextInput)
                result.add(nextModbusQuery)
                continue
            }

            val firstAfterNextQuery = nextModbusQuery.start.increment(nextModbusQuery.count)

            val maxCountPerRequest =
                when (nextModbusQuery.type) {
                    AddressClass.Type.DISCRETE -> maxDiscretesPerModbusRequest
                    AddressClass.Type.REGISTER -> maxRegistersPerModbusRequest
                }

            val maxGapSize =
                when (nextModbusQuery.type) {
                    AddressClass.Type.DISCRETE -> allowedGapReadSize * 16 // FIXME: Should make this cleaner
                    AddressClass.Type.REGISTER -> allowedGapReadSize
                }

            if (nextInput.start == firstAfterNextQuery) {
                // Clean append without gaps
                if (nextModbusQuery.count + nextInput.count <= maxCountPerRequest) {
                    // Merge
                    nextModbusQuery.count += nextInput.count
                    nextModbusQuery.add(nextInput)
                } else {
                    // DO NOT Merge
                    nextModbusQuery = MergedModbusQuery(nextInput.start, nextInput.count)
                    nextModbusQuery.add(nextInput)
                    result.add(nextModbusQuery)
                }
                continue
            }

            // We have a gap between nextBatch and nextInput and we MAY read those also!
            val nextBatchStart = nextModbusQuery.start.physicalAddress
            val nextInputStart = nextInput.start.physicalAddress
            val gapSize = nextInputStart - (nextBatchStart + nextModbusQuery.count)
            val mergedCount = nextInputStart + nextInput.count - nextBatchStart
            if (gapSize <= maxGapSize && // Do NOT jump more than N registers
                mergedCount <= maxCountPerRequest &&
                !readErrorAddresses.overlaps(nextModbusQuery.start, mergedCount) // Do NOT try to read read errors
            ) {
                // To make recovery in case of a read error a lot easier we are immediately inserting fake
                // underlying queries to describe each 'hole'.
                // This makes it so the recovery algorithm can make assumptions that make it simpler.
                nextModbusQuery.add(HoleModbusQuery(firstAfterNextQuery, gapSize))

                nextModbusQuery.count = mergedCount
                nextModbusQuery.add(nextInput)
            } else {
                // DO NOT Merge (i.e. start a new one)
                nextModbusQuery = MergedModbusQuery(nextInput.start, nextInput.count)
                nextModbusQuery.add(nextInput)
                result.add(nextModbusQuery)
            }
        }

        return result
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
