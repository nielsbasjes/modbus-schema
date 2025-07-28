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
import nl.basjes.modbus.device.api.AddressClass.Type.DISCRETE
import nl.basjes.modbus.device.api.AddressClass.Type.REGISTER
import nl.basjes.modbus.device.api.DiscreteBlock
import nl.basjes.modbus.device.api.ModbusBlock
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.ModbusValue
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.device.exception.ModbusApiException
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.fetcher.ModbusQuery.Status
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.TreeMap
import kotlin.time.TimeSource

/**
 * A RegisterBlockFetcher needs to have a Schema, a target RegisterBlock and a ModbusDevice
 */
open class ModbusBlockFetcher(
    protected val schemaDevice: SchemaDevice,
    protected val modbusDevice: ModbusDevice,
) {

    private fun calculateFetchGroupToAddressesMapping(): Map<String, MutableList<Address>> {
        val fetchGroupToAddresses: MutableMap<String, MutableList<Address>> = TreeMap()

        // We register all fields in the schemaDevice with the right fetch group as dictated in the Field.
        for (block in schemaDevice.blocks) {
            for (field in block.fields) {
                val fieldFetchGroup   = field.fetchGroup
                val fieldImmutable    = field.isImmutable
                val requiredAddresses = field.requiredAddresses
                for (requiredAddress in requiredAddresses) {
                    val modbusValue = schemaDevice
                        .getModbusBlock(requiredAddress.addressClass)
                        .getOrCreateIfAbsent(requiredAddress)

                    modbusValue.immutable = fieldImmutable
                    modbusValue.fetchGroup = fieldFetchGroup
                }

                fetchGroupToAddresses
                    .computeIfAbsent(fieldFetchGroup) { ArrayList() }
                    .addAll(requiredAddresses)
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
     * We force an immediate update of all registers needed for the provided field which have a maximum age of the provided milliseconds.
     * No batching, buffering or any optimization is done.
     * @param field The field that must be updated
     * @return A (possibly empty) list of all modbus queries that have been done (with duration and status)
     */
    fun update(field: Field, maxAge: Long = 0): List<ModbusQuery> {
        synchronized(this) {
            require(field.initialized) { "You cannot fetch the registers for a Field if the field has not yet been initialized. (Field ID=${field.id})" }
            // Normally in the 'need' call the underlying fields referenced in the expression are also 'needed'.
            // Here this is not the case because we are ignoring the 'need'.
            val allFields = listOf(field, *field.requiredFields.toTypedArray()).sorted().distinct().toList()

            return calculateModbusQueries(allFields, maxAge)
                .map { fetch(it) }
                .flatten()
                .toList()
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
            for (modbusQuery in calculateModbusQueries(maxAge)) {
                fetched.addAll(fetch(modbusQuery))
            }
            return fetched
        }
    }

    private fun ModbusDevice.getDiscretes(modbusQuery: ModbusQuery): DiscreteBlock {
        val start = TimeSource.Monotonic.markNow()
        try {
            val discreteBlock = this.getDiscretes(modbusQuery.start, modbusQuery.count)
            modbusQuery.status = Status.SUCCESS
            if (discreteBlock.values.any { it.isReadError() }) {
                modbusQuery.status = Status.ERROR
            }
            return discreteBlock
        } catch (modbusException: ModbusException) {
            modbusQuery.status = Status.ERROR
            throw modbusException
        }
        finally {
            val stop = TimeSource.Monotonic.markNow()
            modbusQuery.duration = stop - start
        }
    }

    private fun ModbusDevice.getRegisters(modbusQuery: ModbusQuery): RegisterBlock {
        val start = TimeSource.Monotonic.markNow()
        try {
            val registerBlock = this.getRegisters(modbusQuery.start, modbusQuery.count)
            modbusQuery.status = Status.SUCCESS
            if (registerBlock.values.any { it.isReadError() }) {
                modbusQuery.status = Status.ERROR
            }
            return registerBlock
        } catch (modbusException: ModbusException) {
            modbusQuery.status = Status.ERROR
            throw modbusException
        }
        finally {
            val stop = TimeSource.Monotonic.markNow()
            modbusQuery.duration = stop - start
        }
    }

    private fun ModbusDevice.executeQuery(modbusQuery: ModbusQuery): ModbusBlock<out ModbusBlock<*,*,*>,out ModbusValue<*,*>,*> =
        when(modbusQuery.type) {
            DISCRETE -> getDiscretes(modbusQuery)
            REGISTER -> getRegisters(modbusQuery)
        }

    private fun ModbusBlock<out ModbusBlock<*,*,*>,out ModbusValue<*,*>,*>.mergeFetched(fetchedModbusBlock: ModbusBlock<*,out ModbusValue<*,*>,*> ) {
        when (this) {
            is DiscreteBlock if fetchedModbusBlock is DiscreteBlock -> merge(fetchedModbusBlock)
            is RegisterBlock if fetchedModbusBlock is RegisterBlock -> merge(fetchedModbusBlock)
            else -> throw ModbusApiException("Type mismatch existingModbusBlock (${javaClass.name}) and fetchedModbusBlock (${fetchedModbusBlock.javaClass.name})")
        }
    }

    internal fun fetch(modbusQuery: ModbusQuery): List<ModbusQuery> {
        val fetchedQueries = mutableListOf<ModbusQuery>()
        try {
            val fetchedModbusBlock = modbusDevice.executeQuery(modbusQuery)
            fetchedQueries.add(modbusQuery)

            when(modbusQuery.status) {
                Status.NOT_FETCHED ->
                    throw ModbusApiException("This should not happen. After fetching a modbus query it is still not fetched??")

                Status.SUCCESS -> {
                    // Store the result
                    schemaDevice
                        .getModbusBlock(fetchedModbusBlock.addressClass)
                        .mergeFetched(fetchedModbusBlock)
                }

                Status.ERROR -> {
                    when (modbusQuery) {
                        is HoleModbusQuery -> {
                            // If this was a 'hole' query we store them as soft errors
                            // This will avoid them until we explicitly ask for a field in them
                            schemaDevice
                                .getModbusBlock(fetchedModbusBlock.addressClass)
                                .mergeFetched(fetchedModbusBlock)
                        }

                        is MergedModbusQuery -> {
                            // If we have a merged fetch then we can retry on the individuals.
                            val retries = retryFetchOfFailedMergedModbusQuery(modbusQuery)
                            if (retries.isEmpty()) {
                                // No retries were done so we simply store the error result

                                // If this was a 'hole' query we store them as soft errors
                                // This will avoid them until we explicitly ask for a field in them
                                fetchedModbusBlock.values.forEach{ it.setHardReadError() }
                                schemaDevice
                                    .getModbusBlock(fetchedModbusBlock.addressClass)
                                    .mergeFetched(fetchedModbusBlock)
                            } else {
                                fetchedQueries.addAll(retries)
                            }
                            return fetchedQueries
                        }

                        else -> {
                            // If we DO NOT have a merged fetch then it is simply an error situation.
//                    println("-----READ ERROR getting $modbusQuery ; Fields are marked as DEAD")
                            fetchedModbusBlock.values.forEach { it.setHardReadError() }
                            schemaDevice
                                .getModbusBlock(fetchedModbusBlock.addressClass)
                                .mergeFetched(fetchedModbusBlock)
                        }
                    }
                }
            }
        } catch (me: ModbusException) {
            LOG.error("Got ModbusException on {} --> {}", modbusQuery, me)
        }
        return fetchedQueries
    }

    internal open fun retryFetchOfFailedMergedModbusQuery(modbusQuery: MergedModbusQuery): List<ModbusQuery> {
        throw ModbusApiException("If you create MergedModbusQuery instances then you must implement this also.")
    }

    /**
     * Determine which sets of registers need to be retrieved again.
     * @param maxAge The maximum age (in milliseconds) of the data for it to need an update.
     * @return The list of address ranges that must be retrieved (Sorted by the start address)
     */
    fun calculateModbusQueries(maxAge: Long): List<ModbusQuery> {
        return calculateModbusQueries(
            schemaDevice.neededFields(),
            maxAge,
        )
    }

    /**
     * Determine which sets of registers need to be retrieved again for the provided fields.
     * @param fields The list of fields that must be updated
     * @param maxAge The maximum age (in milliseconds) of the data for it to need an update.
     * @return The list of address ranges that must be retrieved (Sorted by the start address)
     */
    open fun calculateModbusQueries(
        fields: List<Field>,
        maxAge: Long,
    ): List<ModbusQuery> {
        // First we determine which of the fields need to be updated
        val fieldsThatMustBeUpdated = allFieldsThatMustBeUpdated(fields, maxAge)

        // Get the reverse mapping for all fetch group to the contained addresses
        val fetchGroupToAddresses = calculateFetchGroupToAddressesMapping()

        // For each fetchGroup that needs to be updated we create a single modbus query
        val modbusQueryMap: MutableMap<String, ModbusQuery> = mutableMapOf()
        for (field in fieldsThatMustBeUpdated) {
            var modbusQuery = modbusQueryMap[field.fetchGroup]
            if (modbusQuery != null) {
                modbusQuery.addField(field)
                continue  // Already have this one
            }
            val addresses: List<Address> = fetchGroupToAddresses[field.fetchGroup]!!
            modbusQuery = ModbusQuery(addresses[0], addresses.size)
            modbusQuery.addField(field)
            modbusQueryMap[field.fetchGroup] = modbusQuery
        }

        return modbusQueryMap.values.sorted().toList()
    }

    /**
     * Determine from the list of provided Fields which of these need to be updated given the max age.
     */
    private fun allFieldsThatMustBeUpdated(
        fields: List<Field>,
        maxAge: Long,
    ): List<Field> {
        val now = System.currentTimeMillis()
        return fields
            .flatMap { field ->
                require(field.initialized) { "You cannot fetch the registers for a Field if the field has not yet been initialized. (Field ID=${field.id})" }

                // If at least one of the needed addresses links to a 'too old' value
                // the entire set for the field needs to be retrieved again.
                if (field
                        .requiredAddresses
                        .map { schemaDevice.getModbusBlock(it.addressClass)[it] }
                        .firstOrNull { it.needsToBeUpdated(now, maxAge) }
                    != null
                ) {
                    listOf(field)
                } else {
                    listOf()
                }
            }
            .toList()
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()
    }
}
