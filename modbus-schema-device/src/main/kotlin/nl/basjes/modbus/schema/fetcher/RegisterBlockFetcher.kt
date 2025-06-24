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
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.fetcher.ModbusQuery.ModbusQueryStatus
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.TreeMap
import kotlin.time.TimeSource

/**
 * A RegisterBlockFetcher needs to have a Schema, a target RegisterBlock and a ModbusDevice
 */
open class RegisterBlockFetcher(
    protected val schemaDevice: SchemaDevice,
    protected val modbusDevice: ModbusDevice,
) {

    private fun calculateFetchGroupToAddressesMapping(): Map<String, MutableList<Address>> {
        val fetchGroupToAddresses: MutableMap<String, MutableList<Address>> = TreeMap()

        // We register all fields in the schemaDevice with the right fetch group as dictated in the Field.
        for (block in schemaDevice.blocks) {
            for (field in block.fields) {
                val fieldFetchGroup = field.fetchGroup
                val fieldImmutable = field.isImmutable
                val requiredRegisters = field.requiredRegisters
                for (requiredRegister in requiredRegisters) {
                    val registerValue =
                        schemaDevice
                            .getRegisterBlock(requiredRegister.addressClass)
                            .computeIfAbsent(requiredRegister) { RegisterValue(it) }

                    registerValue.immutable = fieldImmutable
                    registerValue.fetchGroup = fieldFetchGroup
                }

                fetchGroupToAddresses
                    .computeIfAbsent(fieldFetchGroup) { ArrayList() }
                    .addAll(requiredRegisters)
            }
        }

        // Verify the fetchGroups to ensure they do not have gaps !
        for ((key, value) in fetchGroupToAddresses) {
            val fetchGroupAddresses = value.sorted().distinct().toList()
            if (fetchGroupAddresses.isEmpty()) {
                continue  // Also good
            }
            // A valid fetch group ONLY has a sequential list of addresses without any gaps
            val firstAddress = fetchGroupAddresses[0]
            val numberOfAddresses = fetchGroupAddresses.size
            val lastAddress = fetchGroupAddresses[numberOfAddresses - 1]
            check(
                firstAddress.increment(numberOfAddresses - 1) == lastAddress,
            ) { "There are gaps in the addresses for fetch group \"$key\": $fetchGroupAddresses" }
        }
        return fetchGroupToAddresses
    }

    /**
     * We force an immediate update of all registers needed for the provided field.
     * No batching, buffering or any optimization is done.
     * @param field The field that must be updated
     * @return A (possibly empty) list of all modbus queries that have been done (with duration and status)
     */
    fun update(field: Field): List<ModbusQuery> {
        require(field.initialized) { "You cannot fetch the registers for a Field if the field has not yet been initialized. (Field ID=${field.id})" }

        if (field.isUsingHardReadErrorRegisters()) {
            return listOf()// Cannot update
        }

        val fetched = mutableListOf<ModbusQuery>()

        // Make sure all fields needed to build the requested value are also present
        field.requiredFields.forEach { fetched.addAll(it.update()) }

        val fetchGroupToAddresses = calculateFetchGroupToAddressesMapping()

        var requiredRegisters: List<Address>? = fetchGroupToAddresses[field.fetchGroup]
        if (requiredRegisters.isNullOrEmpty()) {
            requiredRegisters = field.requiredRegisters
        }

        if (requiredRegisters.isEmpty()) {
            // Nothing to update
            return listOf()
        }
        val modbusQuery = ModbusQuery(requiredRegisters[0], requiredRegisters.size)
        modbusQuery.fields.add(field)
        val deviceRegisters = modbusDevice.getRegisters(modbusQuery)
        if (deviceRegisters.values.any { it.isReadError() }) {
            // We are only fetching a SINGLE field so all registers must be marked as a HARD read error
            modbusQuery.status = ModbusQueryStatus.ERROR
            deviceRegisters.values.forEach { it.setHardReadError() }
        }

        schemaDevice.getRegisterBlock(deviceRegisters.addressClass).merge(deviceRegisters)
        fetched.add(modbusQuery)
        return fetched
    }

    fun ModbusDevice.getRegisters(modbusQuery: ModbusQuery): RegisterBlock {
        val start = TimeSource.Monotonic.markNow()
        try {
            val registerBlock = this.getRegisters(modbusQuery.start, modbusQuery.count)
            modbusQuery.status = ModbusQueryStatus.SUCCESS
            return registerBlock
        } catch (modbusException: ModbusException) {
            modbusQuery.status = ModbusQueryStatus.ERROR
            throw modbusException
        }
        finally {
            val stop = TimeSource.Monotonic.markNow()
            modbusQuery.duration = stop - start
        }
    }

    /**
     * Update all registers related to the needed fields to be updated with a maximum age of the provided milliseconds
     * @param maxAge maximum age of the fields in milliseconds
     * @return A (possibly empty) list of all fetches that have been done (with duration and status)
     */
    @JvmOverloads
    fun update(maxAge: Long = 0): List<ModbusQuery> {
        synchronized(this) {
            val fetched = mutableListOf<ModbusQuery>()
            for (fetchBatch in calculateModbusQueries(maxAge)) {
                fetched.addAll(fetch(fetchBatch))
            }
            return fetched
        }
    }

    private fun fetch(modbusQuery: ModbusQuery): List<ModbusQuery> {
        val fetched = mutableListOf<ModbusQuery>()
        try {
            val registers = modbusDevice.getRegisters(modbusQuery)
            fetched.add(modbusQuery)
            val registerBlock = schemaDevice.getRegisterBlock(registers.addressClass)
            registerBlock.merge(registers)
            if (registers.values.any { it.isReadError() }) {
                modbusQuery.status = ModbusQueryStatus.ERROR
                if (modbusQuery is MergedModbusQuery) {
                    // If we have a merged fetch then we retry on the individuals.
                    for (fetchPart in modbusQuery.modbusQueries) {
                        fetched.addAll(fetch(fetchPart))
                    }

                    // If only trying to fetch the requested parts we would skip the registers
                    // of any intermediate Fields ... that have just been wiped.

                    // We get the blocks this batch touches.
                    val blocks = modbusQuery.fields.map { it.block }.distinct()

                    // We get ALL fields that have registers in the current batch range
                    // and update them one by one. Regardless if they are needed.
                    // This way we can permanently mark the bad ones and avoid them in all future calls
                    blocks
                        .map { it.fields }
                        .flatten()
                        .filter { !modbusQuery.fields.contains(it) }
                        .filter { it.requiredRegisters.overlaps(modbusQuery.start, modbusQuery.count) }
                        .forEach { fetched.addAll(it.update()) }
                    return fetched
                } else {
//                    println("-----READ ERROR getting $fetchBatch ; Fields are marked as DEAD")
                    registers.values.forEach { it.setHardReadError() }
                    registerBlock.merge(registers)
                }
            }
        } catch (me: ModbusException) {
            LOG.error("Got ModbusException on {} --> {}", modbusQuery, me)
        }
        return listOf(modbusQuery)
    }

    /**
     * Determine which sets of registers need to be retrieved again.
     * @param maxAge The maximum age (in milliseconds) of the data for it to need an update.
     * @return The list of address ranges that must be retrieved (Sorted by the start address)
     */
    open fun calculateModbusQueries(maxAge: Long): List<ModbusQuery> {
        val now = System.currentTimeMillis()

        val fieldsThatMustBeUpdated: MutableList<Field> = ArrayList()

        // First we determine which of the fields need to be updated
        for (field in schemaDevice.neededFields()) {
            require(field.initialized) { "You cannot fetch the registers for a Field if the field has not yet been initialized. (Field ID=${field.id})" }

            val requiredRegisters = field.requiredRegisters

            if (requiredRegisters.isEmpty()) {
                continue
            }
            val registerBlock = schemaDevice.getRegisterBlock(requiredRegisters[0].addressClass)
            // If at least one of the needed addresses links to a 'too old' value
            // the entire set for the field needs to be retrieved again.
            if (requiredRegisters
                    .map { registerBlock[it] }
                    .firstOrNull { it.needsToBeUpdated(now, maxAge) }
                != null
            ) {
                fieldsThatMustBeUpdated.add(field)
            }
        }

        val fetchGroupToAddresses = calculateFetchGroupToAddressesMapping()

        // For each fetchGroup that needs to be updated we create a single batch
        val fetchBatchesMap: MutableMap<String, ModbusQuery> = TreeMap()
        for (field in fieldsThatMustBeUpdated) {
            var fetchBatch = fetchBatchesMap[field.fetchGroup]
            if (fetchBatch != null) {
                fetchBatch.fields.add(field)
                continue  // Already have this one
            }
            val addresses: List<Address> = fetchGroupToAddresses[field.fetchGroup]!!
            fetchBatch = ModbusQuery(addresses[0], addresses.size)
            fetchBatch.fields.add(field)
            fetchBatchesMap[field.fetchGroup] = fetchBatch
        }

        return fetchBatchesMap.values.sorted().toList()
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()
    }
}
