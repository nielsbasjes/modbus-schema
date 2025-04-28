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
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant
import java.util.*

/**
 * A RegisterBlockFetcher needs to have a Schema, a target RegisterBlock and a ModbusDevice
 */
open class RegisterBlockFetcher(
    protected val schemaDevice: SchemaDevice,
    protected val modbusDevice: ModbusDevice,
) {
    // Maps the fetchGroupId to a list of addresses
    protected val fetchGroupToAddresses: MutableMap<String, MutableList<Address>> = TreeMap()

    // Essentially a semaphore map. The number indicates how many need this field.
    protected val neededFieldsMap: MutableMap<Field, Int> = TreeMap()

    val neededFields: List<Field>
        get() = neededFieldsMap
                .entries
                .filter { it.value > 0 }
                .map { it.key }

    fun initialize() {
        fetchGroupToAddresses.clear()
        neededFieldsMap.clear()
        lastFetchGroupToAddressesMappingTimestamp = Instant.ofEpochMilli(0)
        ensureValidFetchGroupToAddressesMapping()
    }

    /** If a field was added or removed this should trigger updates and reinitializations in other parts */
    private var lastFetchGroupToAddressesMappingTimestamp = Instant.ofEpochMilli(0) // Very long ago
    private fun ensureValidFetchGroupToAddressesMapping() {
        if (schemaDevice.lastFieldModificationTimestamp.isBefore(lastFetchGroupToAddressesMappingTimestamp)) {
            return // Nothing to update
        }
        // We register all fields in the schemaDevice with the right fetch group as dictated in the Field.
        for (block in schemaDevice.blocks) {
            for (field in block.fields) {
                val fieldFetchGroup = field.fetchGroup
                val fieldImmutable = field.isImmutable
                val requiredRegisters = field.requiredRegisters
                for (requiredRegister in requiredRegisters) {
                    val registerValue = schemaDevice
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
                firstAddress.increment(numberOfAddresses - 1) == lastAddress
            ) { "There are gaps in the addresses for fetch group \"$key\": $fetchGroupAddresses" }
        }

        lastFetchGroupToAddressesMappingTimestamp = Instant.now()
    }

    /**
     * @param field The field that must be kept up-to-date
     */
    fun need(field: Field) {
        field.initialize()
        neededFieldsMap.merge(field, 1) { a: Int?, b: Int? -> Integer.sum(a!!, b!!) }
        field.requiredFields.forEach { this.need(it) }
    }

    /**
     * @param field The field that no longer needs to be kept up-to-date
     */
    fun unNeed(field: Field) {
        neededFieldsMap.merge(field, -1) { a: Int?, b: Int? -> Integer.sum(a!!, b!!) }
        field.requiredFields.forEach { this.unNeed(it) }
    }

    /**
     * We want all fields to be kept up-to-date
     */
    fun needAll() {
        schemaDevice.blocks
            .map { it.fields }
            .flatMap { it.asSequence() }
            .forEach { this.need(it) }
    }

    /**
     * We no longer want all fields to be kept up-to-date
     */
    fun unNeedAll() {
        schemaDevice.blocks
            .map { it.fields }
            .flatMap { it.asSequence() }
            .forEach { this.unNeed(it) }
    }

    /**
     * Make sure all registers mentioned in all known fields are retrieved.
     */
    @Throws(ModbusException::class)
    @JvmOverloads
    fun updateAll(maxAge: Long = 0) {
        needAll()
        update(maxAge)
        unNeedAll()
    }

    /**
     * We force an immediate update of all registers needed for the provided field.
     * No batching, buffering or any optimization is done.
     * @param field The field that must be updated
     */
    @Throws(ModbusException::class)
    fun update(field: Field) {
        ensureValidFetchGroupToAddressesMapping()
        var requiredRegisters: List<Address>? = fetchGroupToAddresses[field.fetchGroup]
        if (requiredRegisters.isNullOrEmpty()) {
            requiredRegisters = field.requiredRegisters
        }
        val deviceRegisters = modbusDevice.getRegisters(requiredRegisters[0], requiredRegisters.size)
        schemaDevice.getRegisterBlock(deviceRegisters.addressClass).merge(deviceRegisters)
    }

    class FetchBatch : Comparable<FetchBatch> {
        var start: Address? = null
        var count: Int = 0

        override fun compareTo(other: FetchBatch): Int {
            val addressCompare = start!!.compareTo(other.start!!)
            if (addressCompare != 0) {
                return addressCompare
            }
            return count.compareTo(other.count)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is FetchBatch) {
                return false
            }
            return count == other.count && start == other.start
        }

        override fun hashCode(): Int {
            return Objects.hash(start, count)
        }

        override fun toString(): String {
            return "FetchBatch{ $start # $count}"
        }
    }

    /**
     * Update all registers related to the needed fields to be updated with a maximum age of the provided milliseconds
     * @param maxAge maximum age of the fields in milliseconds
     */
    fun update(maxAge: Long) {
        ensureValidFetchGroupToAddressesMapping()
        for (fetchBatch in calculateFetchBatches(maxAge)) {
            try {
                val registers = modbusDevice.getRegisters(fetchBatch.start!!, fetchBatch.count)
                schemaDevice.getRegisterBlock(registers.addressClass).merge(registers)
            } catch (me: ModbusException) {
                LOG.error("Got ModbusException on {} --> {}", fetchBatch, me)
            }
        }
    }

    /**
     * Determine which sets of registers need to be retrieved again.
     * @param maxAge The maximum age (in milliseconds) of the data for it to need an update.
     * @return The list of address ranges that must be retrieved (Sorted by the start address)
     */
    open fun calculateFetchBatches(maxAge: Long): List<FetchBatch> {
        val now = System.currentTimeMillis()

        val fieldsThatMustBeUpdated: MutableList<Field> = ArrayList()

        // First we determine which of the fields need to be updated
        for (field in neededFields) {
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

        // For each fetchGroup that needs to be updated we create a single batch
        val fetchBatchesMap: MutableMap<String, FetchBatch> = TreeMap()
        for (field in fieldsThatMustBeUpdated) {
            if (fetchBatchesMap.containsKey(field.fetchGroup)) {
                continue  // Already have this one
            }
            val addresses: List<Address> = fetchGroupToAddresses[field.fetchGroup]!!
            val fetchBatch = FetchBatch()
            fetchBatch.start = addresses[0]
            fetchBatch.count = addresses.size
            fetchBatchesMap[field.fetchGroup] = fetchBatch
        }

        return fetchBatchesMap.values.sorted().toList()
    }

    init {
        initialize()
    }

    companion object {
        const val FORCE_UPDATE_MAX_AGE: Long = -1000000000000L

        private val LOG: Logger = LogManager.getLogger()
    }
}
