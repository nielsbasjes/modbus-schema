<#--                                                                          -->
<#-- Modbus Schema toolkit                                                    -->
<#-- Copyright (C) 2019-2025 Niels Basjes                                     -->
<#--                                                                          -->
<#-- Licensed under the Apache License, Version 2.0 (the "License");          -->
<#-- you may not use this file except in compliance with the License.         -->
<#-- You may obtain a copy of the License at                                  -->
<#--                                                                          -->
<#-- https://www.apache.org/licenses/LICENSE-2.0                              -->
<#--                                                                          -->
<#-- Unless required by applicable law or agreed to in writing, software      -->
<#-- distributed under the License is distributed on an "AS IS" BASIS,        -->
<#-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. -->
<#-- See the License for the specific language governing permissions and      -->
<#-- limitations under the License.                                           -->
<#--                                                                          -->
//
// Generated using the nl.basjes.modbus:modbus-schema-maven-plugin:${pluginVersion}
// Using the builtin template to generate Kotlin MAIN code.
// https://modbus.basjes.nl
//

// ===========================================================
//               !!! THIS IS GENERATED CODE !!!
// -----------------------------------------------------------
//       EVERY TIME THE SOFTWARE IS BUILD THIS FILE IS
//        REGENERATED AND ALL MANUAL CHANGES ARE LOST
// ===========================================================
package ${packageName}

import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.toSchemaDevice
import nl.basjes.modbus.schema.utils.StringTable

/**
 * ${schemaDevice.description}
 */
open class ${asClassName(className)} {

    val schemaDevice = SchemaDevice()

    val tests = schemaDevice.tests

    fun connectBase(modbusDevice: ModbusDevice): ${asClassName(className)} {
        schemaDevice.connectBase(modbusDevice)
        return this
    }

    fun connect(modbusDevice: ModbusDevice): ${asClassName(className)} {
        schemaDevice.connect(modbusDevice)
        return this
    }

    /**
     * Update all registers related to the needed fields to be updated with a maximum age of the provided milliseconds
     * @param maxAge maximum age of the fields in milliseconds
     * @return A list of all modbus queries that have been done (with duration and status)
     */
    @JvmOverloads
    fun update(maxAge: Long = 0) = schemaDevice.update(maxAge)

    /**
     * Update all registers related to the specified field
     * @param field the Field that must be updated
     * @return A list of all modbus queries that have been done (with duration and status)
     */
    fun update(field: Field) = schemaDevice.update(field)

    /**
     * Make sure all registers mentioned in all known fields are retrieved.
     * @return A (possibly empty) list of all modbus queries that have been done (with duration and status)
     */
    @JvmOverloads
    fun updateAll(maxAge: Long = 0) = schemaDevice.updateAll(maxAge)

    /**
     * @param field The field that must be kept up-to-date
     */
    fun need(field: Field) = schemaDevice.need(field)

    /**
     * @param field The field that no longer needs to be kept up-to-date
     */
    fun unNeed(field: Field) = schemaDevice.unNeed(field)

    /**
     * We want all fields to be kept up-to-date
     */
    fun needAll()  = schemaDevice.needAll()

    /**
     * We no longer want all fields to be kept up-to-date
     */
    fun unNeedAll()  = schemaDevice.unNeedAll()

    abstract class DeviceField(val field: Field) {
        /**
         * Retrieve the value of this field using the currently available device data.
         */
        abstract val value: Any?
        /**
         * We want this field to be kept up-to-date
         */
        fun need() = field.need()
        /**
         * We no longer want this field to be kept up-to-date
         */
        fun unNeed() = field.unNeed()
        /**
         * Directly update this field
         * @return A list of all modbus queries that have been done (with duration and status)
         */
        fun update() = field.update();
        /**
         * The unit of the returns value
         */
        val unit =  field.unit
        /**
         * The description of the Field
         */
        val description = field.description
        override fun toString(): String = if (value == null) { "null" } else { value.toString() }
    }

<#list schemaDevice.blocks as block>
    // ==========================================
    /**
     * ${block.description}
     */
    val ${asVariableName(block.id)} = ${asClassName(block.id)}(schemaDevice);

    class ${asClassName(block.id)}(schemaDevice: SchemaDevice) {
        val block: Block;

        /**
         * Directly update all fields in this Block
         * @return A list of all modbus queries that have been done (with duration and status)
         */
        fun update() = block.update()

        /**
         * All fields in this Block must be kept up-to-date
         */
        fun need() = block.needAll()

        /**
         * All fields in this Block no longer need to be kept up-to-date
         */
        fun unNeed() = block.unNeedAll()

<#list block.fields as field>

        // ==========================================
        /**
         * ${field.description}
         <#if field.unit?has_content>
         * Unit: ${field.unit}
         </#if>
         */
        <#if field.system>private<#else>public</#if> val ${asVariableName(field.id)}: ${asClassName(field.id)}
        <#if field.system>private<#else>public</#if> class ${asClassName(field.id)}(block: Block): DeviceField (
            Field.builder()
                 .block(block)
                 .id("${field.id}")
                 .description("${field.description}")
                 .expression("${field.parsedExpression}")
                 .unit("${field.unit}")
                 .immutable(${field.immutable?string('true', 'false')})
                 .system(${field.system?string('true', 'false')})
                 .fetchGroup("${field.fetchGroup}")
                 .build()) {
            override val value get() = field.${asVariableName(valueGetter(field.returnType))}
        }
</#list>

        init {
            this.block = Block.builder()
              .schemaDevice(schemaDevice)
              .id("${block.id}")
<#if block.description??>
              .description("${block.description}")
</#if>
              .build()

<#list block.fields as field>
            this.${asVariableName(field.id)?right_pad(block.maxFieldIdLength)} = ${asClassName(field.id)}(block);
</#list>
        }

        override fun toString(): String {
            val table = StringTable()
            table.withHeaders("Block", "Field", "Value");
            toStringTable(table)
            return table.toString()
        }

        internal fun toStringTable(table: StringTable) {
<#assign nonSystemFields=block.fields?filter(f -> !f.system)>
<#if nonSystemFields?has_content>
            table
<#list nonSystemFields as field>
<#assign fieldId="\""+field.id+"\",">
                .addRow("${block.id}", ${fieldId?right_pad(block.maxFieldIdLength+3)} "" + ${asVariableName(field.id)}.value)
</#list>
<#else>
            // This block has no fields
</#if>
        }
    }
</#list>

    override fun toString(): String {
        val table = StringTable();
        table.withHeaders("Block", "Field", "Value")
<#list schemaDevice.blocks as block>
        ${asVariableName(block.id)?right_pad(schemaDevice.maxBlockIdLength+1)}.toStringTable(table)
</#list>
        return table.toString()
    }

    init {
        require(schemaDevice.initialize()) { "Unable to initialize schema device" }
    }

}
