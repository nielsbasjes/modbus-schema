/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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
package nl.basjes.modbus.schema.immutable

import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.ReturnType.BOOLEAN
import nl.basjes.modbus.schema.ReturnType.DOUBLE
import nl.basjes.modbus.schema.ReturnType.LONG
import nl.basjes.modbus.schema.ReturnType.STRING
import nl.basjes.modbus.schema.ReturnType.STRINGLIST
import nl.basjes.modbus.schema.ReturnType.UNKNOWN
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.utils.StringTable
import java.util.TreeMap

/**
 * An immutable copy of a SchemaDevice
 *
 * Some systems will need the current value in such a way that it does not change
 * even if the value from the source system has been updated.
 * This allows buffering several values of the same Field in memory and flush them
 * to a backend system in a batch call.
 */
class ImmutableSchemaDevice(
    /**
     * A human-readable description of this schema device.
     */
    val description: String,

    /**
     * @return A list of all blocks
     */
    val blocks: List<ImmutableBlock>,

    /**
     * @return A list of all fields
     */
    val fields: List<ImmutableField>,
) {
    private lateinit var blockMap: Map<String, ImmutableBlock>
    fun getBlock(blockId: String): ImmutableBlock? = blockMap[blockId]

    internal fun init() {
        blockMap = blocks.associateBy { it.id }
        blocks.forEach { it.init() }
    }

    override fun toString(): String {
        val table = StringTable()
        table.withHeaders("Block", "Field", "Type", "Value", "Unit", "Description")
        fields.forEach { field ->
            table.addRow(
                field.block.id,
                field.id,
                field.returnType.value(),
                field.value?.toString() ?: "<<<null>>>",
                field.unit,
                field.description,
                )
        }
        return """
SchemaDevice: $description
$table
        """.trimIndent()
    }
}

operator fun ImmutableSchemaDevice?.get(blockId: String): ImmutableBlock? = this?.getBlock(blockId)

/**
 * An immutable copy of a Block
 */
class ImmutableBlock(
    /**
     * The schema device of which this block is a part
     */
    val schemaDevice: ImmutableSchemaDevice,

    /**
     * The technical id of the block.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces, starting with a letter) is a good choice.
     */
    val id: String,

    /**
     * The human-readable description of the block.
     */
    val description: String? = null,

    /**
     * A shorter variant of the Human-readable description of the field.
     * If no shorter version is available then it will be the same as the 'long' description.
     */
    val shortDescription: String? = description,

    /**
     * @return A list of all fields in this block
     */
    val fields: List<ImmutableField>,
) {
    private lateinit var fieldMap: Map<String, ImmutableField>
    fun getField(fieldId: String): ImmutableField? = fieldMap[fieldId]
    internal fun init() {
        fieldMap = fields.associateBy { it.id }
    }
}

operator fun ImmutableBlock?.get(fieldId: String): ImmutableField? = this?.getField(fieldId)

/**
 * An immutable copy of a Field including the CURRENT values.
 */
abstract class ImmutableField(
    /** The block in which this field is located */
    open val block: ImmutableBlock,

    /** The original MUTABLE Field of which this is an immutable snapshot */
    open val mutableField: Field,

    /**
     * The technical id of the field.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces) is a good choice.
     */
    open val id: String,

    /**
     * Human-readable description of the field.
     */
    open val description: String,

    /**
     * A shorter variant of the Human-readable description of the field.
     * If no shorter version is available then it will be the same as the 'long' description.
     */
    open val shortDescription: String,

    /**
     * If a field NEVER changes then this can be se to true.
     * This allows a library to only read this value the first time
     * and on subsequent updates skip reading this value.
     */
    open val immutable: Boolean,

    /** Human-readable unit of the field (like 'V' for Volt or '%' for percentage).     */
    open val unit: String,

    /** The return type that the programming language must support. */
    open val returnType: ReturnType,

    /** The resulting value, the subtype will have the specific data type **/
    open val value: Any?,

    /**
     * The epoch (in milliseconds since 1970-01-01) timestamp of the oldest mutable register used to build this value
     * Returns null on fully immutable values
     */
    open val valueEpochMs: Long?
)

class ImmutableFieldBoolean(
    override val block: ImmutableBlock,
    override val mutableField: Field,
    override val id: String,
    override val description: String,
    override val shortDescription: String,
    override val immutable: Boolean,
    override val unit: String,
    override val returnType: ReturnType,
    override val value: Boolean?, // Make the value type more specific
    override val valueEpochMs: Long?
) : ImmutableField(
    block, mutableField, id, description, shortDescription, immutable, unit, returnType, value, valueEpochMs,
)

class ImmutableFieldLong(
    override val block: ImmutableBlock,
    override val mutableField: Field,
    override val id: String,
    override val description: String,
    override val shortDescription: String,
    override val immutable: Boolean,
    override val unit: String,
    override val returnType: ReturnType,
    override val value: Long?, // Make the value type more specific
    override val valueEpochMs: Long?
) : ImmutableField(
    block, mutableField, id, description, shortDescription, immutable, unit, returnType, value, valueEpochMs,
)

class ImmutableFieldDouble(
    override val block: ImmutableBlock,
    override val mutableField: Field,
    override val id: String,
    override val description: String,
    override val shortDescription: String,
    override val immutable: Boolean,
    override val unit: String,
    override val returnType: ReturnType,
    override val value: Double?, // Make the value type more specific
    override val valueEpochMs: Long?
) : ImmutableField(
    block, mutableField, id, description, shortDescription, immutable, unit, returnType, value, valueEpochMs,
)

class ImmutableFieldString(
    override val block: ImmutableBlock,
    override val mutableField: Field,
    override val id: String,
    override val description: String,
    override val shortDescription: String,
    override val immutable: Boolean,
    override val unit: String,
    override val returnType: ReturnType,
    override val value: String?, // Make the value type more specific
    override val valueEpochMs: Long?
) : ImmutableField(
    block, mutableField, id, description, shortDescription, immutable, unit, returnType, value, valueEpochMs,
)

class ImmutableFieldStringList(
    override val block: ImmutableBlock,
    override val mutableField: Field,
    override val id: String,
    override val description: String,
    override val shortDescription: String,
    override val immutable: Boolean,
    override val unit: String,
    override val returnType: ReturnType,
    override val value: List<String>?, // Make the value type more specific
    override val valueEpochMs: Long?
) : ImmutableField(
    block, mutableField, id, description, shortDescription, immutable, unit, returnType, value, valueEpochMs,
)

fun SchemaDevice.toImmutable(): ImmutableSchemaDevice {
    val immutableBlocks       = mutableListOf<ImmutableBlock>()
    val immutableFields       = mutableListOf<ImmutableField>()
    val immutableSchemaDevice = ImmutableSchemaDevice(
        description = description,
        blocks = immutableBlocks,
        fields = immutableFields,
    )
    blocks.forEach { block ->
        val immutableBlockFields = mutableListOf<ImmutableField>()
        val immutableBlock = ImmutableBlock(
            schemaDevice     = immutableSchemaDevice,
            id               = block.id,
            description      = block.description,
            shortDescription = block.shortDescription,
            fields           = immutableBlockFields,
        )
        block.fields.forEach { field ->
            if (field.isNeeded()) {
                val immutableField = field.toImmutableField(immutableBlock)
                if (immutableField != null) {
                    immutableBlockFields.add(immutableField)
                }
            }
        }
        if (immutableBlockFields.isNotEmpty()) {
            immutableBlocks.add(immutableBlock)
            immutableFields.addAll(immutableBlockFields)
        }
    }

    // Initialize the lookup structures.
    immutableSchemaDevice.init()
    return immutableSchemaDevice
}

private fun Field.toImmutableField(block: ImmutableBlock): ImmutableField? {
    // Refuse system fields
    if (isSystem) {
        return null
    }

    return when (returnType) {
        UNKNOWN    -> null
        BOOLEAN    ->
            ImmutableFieldBoolean(
                block                 = block,
                mutableField          = this,
                id                    = id,
                description           = description,
                shortDescription      = shortDescription,
                immutable             = isImmutable,
                unit                  = unit,
                returnType            = returnType,
                valueEpochMs          = valueEpochMs,
                value                 = booleanValue,
            )

        LONG    ->
            ImmutableFieldLong(
                block                 = block,
                mutableField          = this,
                id                    = id,
                description           = description,
                shortDescription      = shortDescription,
                immutable             = isImmutable,
                unit                  = unit,
                returnType            = returnType,
                valueEpochMs          = valueEpochMs,
                value                 = longValue,
            )

        DOUBLE  ->
            ImmutableFieldDouble(
                block                 = block,
                mutableField          = this,
                id                    = id,
                description           = description,
                shortDescription      = shortDescription,
                immutable             = isImmutable,
                unit                  = unit,
                returnType            = returnType,
                valueEpochMs          = valueEpochMs,
                value                 = doubleValue,
            )

        STRING  ->
            ImmutableFieldString(
                block                 = block,
                mutableField          = this,
                id                    = id,
                description           = description,
                shortDescription      = shortDescription,
                immutable             = isImmutable,
                unit                  = unit,
                returnType            = returnType,
                valueEpochMs          = valueEpochMs,
                value                 = stringValue,
            )

        STRINGLIST ->
            ImmutableFieldStringList(
                block                 = block,
                mutableField          = this,
                id                    = id,
                description           = description,
                shortDescription      = shortDescription,
                immutable             = isImmutable,
                unit                  = unit,
                returnType            = returnType,
                valueEpochMs          = valueEpochMs,
                value                 = stringListValue,
            )
    }
}
