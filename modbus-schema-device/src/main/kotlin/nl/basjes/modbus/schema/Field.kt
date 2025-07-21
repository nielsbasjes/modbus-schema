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

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.MODBUS_MAX_REGISTERS_PER_REQUEST
import nl.basjes.modbus.schema.ReturnType.BOOLEAN
import nl.basjes.modbus.schema.ReturnType.DOUBLE
import nl.basjes.modbus.schema.ReturnType.LONG
import nl.basjes.modbus.schema.ReturnType.STRING
import nl.basjes.modbus.schema.ReturnType.STRINGLIST
import nl.basjes.modbus.schema.ReturnType.UNKNOWN
import nl.basjes.modbus.schema.exceptions.ModbusSchemaMissingFieldException
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.booleans.BooleanExpression
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression
import nl.basjes.modbus.schema.expression.parser.ExpressionParser.Companion.parse
import nl.basjes.modbus.schema.expression.strings.StringExpression
import nl.basjes.modbus.schema.expression.strings.StringListExpression
import nl.basjes.modbus.schema.fetcher.ModbusQuery
import nl.basjes.modbus.schema.utils.requireValidIdentifier
import kotlin.properties.Delegates

class Field(
    /** The block in which this field is located */
    val block: Block,

    /**
     * The technical id of the field.
     * Must be usable as an identifier in 'all' common programming languages.
     * So "CamelCase" (without spaces) is a good choice.
     */
    id: String,

    /**
     * Human-readable description of the field.
     */
    val description: String = "",

    /**
     * A shorter variant of the Human-readable description of the field.
     * If no shorter version is available then it will be the same as the 'long' description.
     */
    val shortDescription: String = description,

    /**
     * If a field NEVER changes then this can be se to true.
     * This allows a library to only read this value the first time
     * and on subsequent updates skip reading this value.
     */
    immutable: Boolean = false,

    /**
     * Some fields are system fields which means they should not be used by the application.
     */
    system: Boolean = false,

    /**
     * The expression that defines how this Field gets its value.
     */
    val expression: String,

    /** Human-readable unit of the field (like 'V' for Volt or '%' for percentage).     */
    val unit: String = "",

    /**
     * An identifier to that can be used to ensure all needed registers are retrieved together.
     * By default, filled with a random unique value or what was dictated by the block
     */
    fetchGroup: String = "",
) : Comparable<Field> {

    val id: String = id.trim()

    /** The return type that the programming language must support. */
    var returnType: ReturnType = UNKNOWN
        private set

    /**
     * An identifier to that can be used to ensure all needed registers are retrieved together.
     * By default, filled with a random unique value or what was dictated by the block
     */
    var fetchGroup = fetchGroup
        get() = field.ifBlank { "<<${block.id} | $id>>" }
        set(value) {
            field = value
            fetchGroupIsDefault = field.isBlank()
        }

    var fetchGroupIsDefault: Boolean = fetchGroup.isBlank() || fetchGroup == "<<${block.id} | $id>>"
        private set

    var initialized = false
        private set

    fun initialize(): Boolean {
        if (!initialized) {
            if (parsedExpression == null && expression.isNotBlank()) {
                try {
                    parsedExpression = parse(expression)
                } catch (e: ModbusSchemaParseException) {
                    throw ModbusSchemaParseException(
                        "Field \"$id\": Unable to parse the expression >>$expression<< --> ${e.message}",
                        e,
                    )
                } catch (npe: NullPointerException) {
                    throw ModbusSchemaParseException(
                        "Field \"$id\": Unable to parse the expression >>$expression<< --> ${npe.message}",
                        npe,
                    )
                }
            }
            val theExpression = parsedExpression
            if (theExpression != null) {
                initialized = theExpression.initialize(this) &&
                    theExpression.problems.isEmpty()
                if (initialized) {
                    if(requiredAddresses.size > MODBUS_MAX_REGISTERS_PER_REQUEST) {
                        throw ModbusSchemaParseException(
                            "In block ${block.id} the field $id requires a block of ${requiredAddresses.size} registers which cannot be retrieved over Modbus."
                        )
                    }

                    // --------
                    // Before we can do checks on needed registers and such we must make sure all dependencies have been initialized.
                    // We allow 5 deep nesting of fields.
                    var allHaveBeenInitialized = true
                    for (retry in 0..5) {
                        allHaveBeenInitialized = true
                        for (requiredFieldName in theExpression.requiredFields) {
                            val requiredField =
                                block.getField(requiredFieldName)
                                    ?: throw ModbusSchemaMissingFieldException(
                                        "In block ${block.id} the field $id needs the field $requiredFieldName which is missing.",
                                    )
                            if (!requiredField.initialize()) {
                                allHaveBeenInitialized = false
                            }
                        }
                        if (allHaveBeenInitialized) {
                            break
                        }
                    }
                    if (!allHaveBeenInitialized) {
                        println("ERROR")
                    }
                    // --------
                    val actualReturnType = theExpression.returnType
                    require(actualReturnType != UNKNOWN) {
                        "The expression dictated an UNKNOWN return type ?!?!?"
                    }
                    if (returnType == UNKNOWN) {
                        returnType = actualReturnType
                    } else {
                        require(returnType == actualReturnType) {
                            "Field $id was tagged it should return $returnType but the provided expression returns a $actualReturnType"
                        }
                    }
                    // --------
                    val requiredRegisters = theExpression.requiredAddresses
                    if (requiredRegisters.isEmpty()) {
                        addressClass = null // This expression does not need ANY registers.
                    } else {
                        addressClass = requiredRegisters[0].addressClass
                        for (requiredRegister in requiredRegisters) {
                            if (addressClass != requiredRegister.addressClass) {
                                throw ModbusSchemaParseException(
                                    "For field ${block.id}::$id the expression $theExpression requires values from multiple AddressClasses. " +
                                        "This is not supported because it would put them in the same fetch group. " +
                                        "Use multiple Fields to achieve this (mark them as system field to hide them).",
                                )
                            }
                        }
                    }
                    // --------
                    if (isImmutable) {
                        // Force all nodes in the actual expression to be marked as part of an immutable expression
                        // This is needed for the optimizer that combines immutable fields into better code.
                        // Key hurdle: This only works if all required Fields have been initialized
                        theExpression.isImmutable = true
                    }
                }
            }
        }
        return initialized
    }

    /**
     * The parsed version of the expression that is actually executed
     */
    var parsedExpression: Expression? = null
        private set

    // The expression uses 0, 1 or more register values that MUST be all from the same addressClass;
    private var addressClass: AddressClass? = null

    /**
     * Some fields are system fields which means they should not be used by the application.
     */
    val isSystem: Boolean = system

    /**
     * If a field NEVER changes then this can be se to true.
     * This allows a library to only read this value the first time
     * and on subsequent updates skip reading this value.
     */
    val isImmutable: Boolean = immutable
        get() {
            if (field) {
                // This was explicitly set to IMMUTABLE.
                // So we read the value once; and then we can assume it never changes again.
                return true
            }

            // Only if all input fields are immutable then this one is also immutable.
            if (parsedExpression == null || !parsedExpression!!.isImmutable) {
                return false
            }
            for (requiredField in requiredFields) {
                if (!requiredField.isImmutable) {
                    return false
                }
            }
            return true
        }

    val value: Any?
        get() =
            when (returnType) {
                UNKNOWN    -> null //TODO("Unknown returnType (Field $id) means we do not know yet")
                BOOLEAN    -> booleanValue
                LONG       -> longValue
                DOUBLE     -> doubleValue
                STRING     -> stringValue
                STRINGLIST -> stringListValue
            }

    val stringValue: String?
        get() {
            if (parsedExpression is StringExpression) {
                return (parsedExpression as StringExpression).getValue(block.schemaDevice)
            }
            return null
        }

    val stringListValue: List<String>?
        get() {
            if (parsedExpression is StringListExpression) {
                return (parsedExpression as StringListExpression).getValueAsStringList(block.schemaDevice)
            }
            return null
        }

    val doubleValue: Double?
        get() {
            if (parsedExpression is NumericalExpression) {
                return (parsedExpression as NumericalExpression).getValueAsDouble(block.schemaDevice)
            }
            return null
        }

    val longValue: Long?
        get() {
            if (parsedExpression is NumericalExpression) {
                return (parsedExpression as NumericalExpression).getValueAsLong(block.schemaDevice)
            }
            return null
        }

    val booleanValue: Boolean?
        get() {
            if (parsedExpression is BooleanExpression) {
                return (parsedExpression as BooleanExpression).getBoolean(block.schemaDevice)
            }
            return null
        }

    /**
     * The epoch (in milliseconds since 1970-01-01) timestamp of the oldest mutable register used to build this value
     * Returns null on fully immutable values
     */
    val valueEpochMs: Long?
        get() {
            val parsedExpression = parsedExpression
            if (parsedExpression == null) {
                return null
            }
            val addressClass = addressClass
            if (addressClass == null) {
                return null
            }
            val registerValues =
                block.schemaDevice.getModbusBlock(addressClass).get(parsedExpression.requiredMutableAddresses)
            val timestamps = registerValues.mapNotNull { it.timestamp }
            if (timestamps.isEmpty()) {
                return null
            }
            return timestamps.min()
        }

    val requiredFieldNames: List<String>
        get() = parsedExpression?.requiredFields ?: emptyList()

    val requiredFields: List<Field>
        get() =
            parsedExpression
                ?.requiredFields
                ?.mapNotNull { block.getField(it) }
                ?.toList() ?: listOf()

    val requiredAddresses: List<Address>
        get() = parsedExpression?.requiredAddresses ?: emptyList()

    fun usedReadErrorAddresses(): List<Address> {
        val addressClass = addressClass ?: return emptyList()
        val registerValues = block.schemaDevice.getModbusBlock(addressClass).get(requiredAddresses)
        return registerValues.filter { it.isReadError() }.map { it.address }.toList()
    }

    fun isUsingReadErrorRegisters() = !usedReadErrorAddresses().isEmpty()

    fun usedHardReadErrorAddresses(): List<Address> {
        val addressClass = addressClass ?: return emptyList()
        val registerValues = block.schemaDevice.getModbusBlock(addressClass).get(requiredAddresses)
        return registerValues.filter { it.hardReadError }.map { it.address }.toList()
    }

    fun isUsingHardReadErrorRegisters() = !usedHardReadErrorAddresses().isEmpty()

    /**
     * Directly update this field.
     * @return A (possibly empty) list of all fetches that have been done (with duration and status)
     */
    fun update(): List<ModbusQuery> {
        return block.schemaDevice.update(this)
    }

    // Essentially a semaphore. The number indicates how many need this field.
    var neededCount = 0

    /**
     * This field must be kept up-to-date
     */
    fun need() {
        neededCount++
        requiredFields.forEach { it.need() }

        // If the registers of this block were read before then there is the possibility that they were part of a read error.
        // Because (perhaps) this read error was NOT related to this field: we reset any read error status of the cached
        // register values if we are not 100% certain that it was caused by this specific field.
        val addressClass = addressClass ?: return
        val registerValues = block.schemaDevice.getModbusBlock(addressClass).get(requiredAddresses)
        registerValues.forEach { it.clearSoftReadError() }
    }

    /**
     * The field no longer needs to be kept up-to-date
     */
    fun unNeed() {
        neededCount--
        requiredFields.forEach { it.unNeed() }
    }

    fun isNeeded() = neededCount > 0

    val testCompareValue: List<String>
        get() =
            when (returnType) {
                UNKNOWN -> {
                    TODO("Unknown returnType (Field $id) means we do not know yet")
                }

                BOOLEAN -> {
                    val value = booleanValue
                    if (value != null) listOf(if(value) "true" else "false") else listOf()
                }

                LONG -> {
                    if (longValue != null) listOf(longValue.toString()) else listOf()
                }

                DOUBLE -> {
                    when {
                        doubleValue == null -> listOf()
                        doubleValue!!.isNaN() -> listOf("NaN")
                        doubleValue == Double.POSITIVE_INFINITY -> listOf("+Infinite")
                        doubleValue == Double.NEGATIVE_INFINITY -> listOf("-Infinite")
                        else -> listOf(String.format("%.3f", doubleValue))
                    }
                }

                STRING -> {
                    if (stringValue == null) listOf() else listOf(stringValue!!)
                }

                STRINGLIST -> {
                    if (stringListValue == null) listOf() else stringListValue!!
                }
            }


    override fun compareTo(other: Field): Int {
        val thisRequiredRegisters = this.requiredAddresses
        val otherRequiredRegisters = other.requiredAddresses
        if (thisRequiredRegisters.isEmpty() && otherRequiredRegisters.isEmpty()) {
            return 0
        }
        if (thisRequiredRegisters.isEmpty()) {
            return 1
        }
        if (otherRequiredRegisters.isEmpty()) {
            return -1
        }
        val thisAddress = requiredAddresses[0]
        val otherAddress = other.requiredAddresses[0]
        return thisAddress.compareTo(otherAddress)
    }

    override fun toString(): String =
        "Field(id='$id', " +
            "isSystem=$isSystem, " +
            "isImmutable=$isImmutable, " +
            "unit=$unit, " +
            "fetchGroup='$fetchGroup', " +
            "returnType=$returnType, " +
            "initialized=$initialized, " +
            "expression='$expression', " +
            "parsedExpression=$parsedExpression, " +
            "addressClass=$addressClass, " +
            "neededCount=$neededCount)"

    init {
        requireValidIdentifier(id, "Field id")
        block.addField(this)
    }

    companion object {
        @JvmStatic
        fun builder(): FieldBuilder = FieldBuilder()
    }

    open class FieldBuilder {
        /**
         * The block to which this block must be linked
         */
        fun block(block: Block) = apply { this.block = block }

        private var block: Block by Delegates.notNull()

        /**
         * The technical id of this field.
         * Must be usable as an identifier in 'all' common programming languages.
         * So "CamelCase" (without spaces) is a good choice.
         */
        fun id(id: String) = apply { this.id = id }

        private var id: String by Delegates.notNull()

        /**
         * A human-readable description of this field.
         */
        fun description(description: String) = apply { this.description = description }

        private var description: String = ""

        /**
         * A shorter variant of the Human-readable description of the field.
         * If no shorter version is available then it will be the same as the 'long' description.
         */
        fun shortDescription(shortDescription: String) = apply { this.shortDescription = shortDescription }

        private var shortDescription: String = ""

        /**
         * If a field NEVER changes then this can be se to true.
         * This allows a library to only read this value the first time
         * and on subsequent updates skip reading this value.
         */
        fun immutable(immutable: Boolean) = apply { this.immutable = immutable }

        private var immutable: Boolean = false

        /**
         * Some fields are system fields which means they should not be used by the application.
         */
        fun system(system: Boolean) = apply { this.system = system }

        private var system: Boolean = false

        /**
         * The expression that defines how this Field gets its value.
         */
        fun expression(expression: String) = apply { this.expression = expression }

        private var expression: String by Delegates.notNull()

        /** Human-readable unit of the field (like 'V' for Volt or '%' for percentage).     */
        fun unit(unit: String?) = apply { this.unit = unit ?: "" }

        private var unit: String = ""

        /**
         * An identifier to that can be used to ensure all needed registers are retrieved together.
         * By default, filled with a random unique value or what was dictated by the block
         */
        fun fetchGroup(fetchGroup: String) = apply { this.fetchGroup = fetchGroup }

        private var fetchGroup: String? = null

        /**
         * Build the Field, throws IllegalArgumentException if something is wrong
         */
        fun build(): Field {
            val fetchGroup = this.fetchGroup
            return if (fetchGroup.isNullOrBlank()) {
                Field(
                    block = block,
                    id = id,
                    description = description,
                    shortDescription = shortDescription,
                    immutable = immutable,
                    system = system,
                    expression = expression,
                    unit = unit,
                )
            } else {
                Field(
                    block = block,
                    id = id,
                    description = description,
                    shortDescription = shortDescription,
                    immutable = immutable,
                    system = system,
                    expression = expression,
                    unit = unit,
                    fetchGroup = fetchGroup,
                )
            }
        }
    }
}
