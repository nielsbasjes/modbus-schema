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
package nl.basjes.modbus.schema

import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import nl.basjes.modbus.schema.utils.requireValidIdentifier
import java.util.TreeMap
import kotlin.properties.Delegates

open class Block(
    /**
     * The schema device of which this block is a part
     */
    val schemaDevice: SchemaDevice,
    /**
     * The technical id of the block.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces, starting with a letter) is a good choice.
     */
    id: String,
    /**
     * The human-readable description of the block.
     */
    val description: String? = null,
    /**
     * A shorter variant of the Human-readable description of the field.
     * If no shorter version is available then it will be the same as the 'long' description.
     */
    val shortDescription: String? = description,
) {
    // ------------------------------------------

    /**
     * The technical id of the block.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces, starting with a letter) is a good choice.
     */
    val id: String = id.trim()

    // ------------------------------------------

    /**
     * The set of fields defined in this block
     */
    private val mutableFields: MutableList<Field> = ArrayList()

    val fields: List<Field> = mutableFields

    private val fieldMap: MutableMap<String, Field> = TreeMap()

    fun addField(vararg fields: Field): Block {
        for (field in fields) {
            if (this.fieldMap[field.id] != null) {
                throw ModbusSchemaParseException("Field $field already exists.")
            }
            mutableFields.add(field)
            fieldMap[field.id] = field
        }
        schemaDevice.aFieldWasChanged()
        return this
    }

    fun getField(fieldId: String): Field? = fieldMap[fieldId]

//    operator fun get(fieldId: String): Field? = fieldMap[fieldId]

    /**
     * In some templates it is convenient to have the length of the longest field id.
     */
    val maxFieldIdLength get() = fieldMap.keys.maxOfOrNull { it.length } ?: 0

    /**
     * Verify the basics
     */
    fun initialize(): Boolean {
        // Because sometimes we get the needed fields later it can fail
        for (field in fields) {
            // Just initialize them all.
            field.initialize()
        }
        for (field in fields) {
            if (!field.initialize()) {
                return false
            }
        }
        // Verify the structure (this will throw if invalid)
        findCircularReference()
        return true
    }

    private fun findCircularReference() {
        for (field in fields) {
            val circularReference = findCircularReference(emptyList(), field)
            if (circularReference.isNotEmpty()) {
                throw ModbusSchemaParseException("Found circular reference starting with field \"${field.id}\" : $circularReference")
            }
        }
    }

    private fun findCircularReference(
        usageChainSoFar: List<String>,
        field: Field,
    ): List<String> {
        if (usageChainSoFar.contains(field.id)) {
            return usageChainSoFar
        }
        val usageChain: MutableList<String> = ArrayList(usageChainSoFar)
        usageChain.add(field.id)
        for (requiredFieldName in field.requiredFieldNames) {
            val requiredField =
                fieldMap[requiredFieldName]
                    ?: throw ModbusSchemaParseException("Required field \"$requiredFieldName\" (needed for \"${field.id}\") is missing.")
            val circularReference = findCircularReference(usageChain, requiredField)
            if (circularReference.isNotEmpty()) {
                return circularReference
            }
        }
        return emptyList()
    }

    override fun toString(): String = "Block(id='$id', description=$description, fieldMap=$fieldMap)"

    init {
        requireValidIdentifier(id, "Block id")
        schemaDevice.addBlock(this)
    }

    /**
     * Directly update all fields in this Block
     */
    fun update() = fields.map { it.update() }.flatten()

    /**
     * All fields in this Block must be kept up-to-date
     */
    fun needAll() = fields.forEach { it.need() }

    /**
     * All fields in this Block no longer need to be kept up-to-date
     */
    fun unNeedAll() = fields.forEach { it.unNeed() }

    /**
     * Get the list of needed fields
     */
    fun neededFields() = fields.filter { it.isNeeded() }

    // ------------------------------------------

    companion object {
        @JvmStatic
        fun builder(): BlockBuilder = BlockBuilder()
    }

    open class BlockBuilder {
        /**
         * The schema device to which this block must be linked
         */
        fun schemaDevice(schemaDevice: SchemaDevice) = apply { this.schemaDevice = schemaDevice }

        private var schemaDevice: SchemaDevice by Delegates.notNull()

        /**
         * The technical id of this block.
         * Must be usable as an identifier in 'all' common programming languages.
         * So "CamelCase" (without spaces) is a good choice.
         */
        fun id(id: String) = apply { this.id = id }

        private var id: String by Delegates.notNull()

        /**
         * A human-readable description of this block.
         */
        fun description(description: String) = apply { this.description = description }

        private var description: String? = null

        /**
         * A shorter variant of the Human-readable description of the field.
         * If no shorter version is available then it will be the same as the 'long' description.
         */
        fun shortDescription(shortDescription: String) = apply { this.shortDescription = shortDescription }

        private var shortDescription: String? = null

        /**
         * Build the SchemaDevice, throws IllegalArgumentException if something is wrong
         */
        fun build(): Block {
            val block =
                Block(
                    schemaDevice,
                    id,
                    description         = description,
                    shortDescription    = shortDescription ?: description,
                )
            return block
        }
    }
}

operator fun Block?.get(fieldId: String): Field? = this?.getField(fieldId)
