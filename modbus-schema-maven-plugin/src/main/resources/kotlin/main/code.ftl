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

/**
 * ${schemaDevice.description}
 */
open class ${asClassName(className)} {
    companion object {
        val schema = listOf("""
${breakStringBlock(yamlSchema(schemaDevice, "            "), "\"\"\",\"\"\"")}
            """).joinToString("").trimIndent()
    }

    val schemaDevice = schema.toSchemaDevice()

    init {
        require(schemaDevice.initialize()) { "Unable to initialize schema device" }
    }

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
     */
    fun update(maxAge: Long) = schemaDevice.update(maxAge)

    /**
     * Update all registers related to the specified field
     * @param field the Field that must be updated
     */
    fun update(field: Field) = schemaDevice.update(field)

    /**
     * Make sure all registers mentioned in all known fields are retrieved.
     */
    @Throws(ModbusException::class)
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

    abstract class DeviceField(block: Block, fieldId: String) {
        val field = block.getField(fieldId) ?: throw IllegalArgumentException("The generated code was unable to find the Field \"${r"${fieldId}"}\" in the Block \"${r"${block.id}"}\"")
        abstract val value: Any?
        fun need() = field.need()
        fun unNeed() = field.unNeed()
        override fun toString(): String = if (value == null) { "null" } else { value.toString() }
    }

    class DeviceFieldLong(block: Block, fieldId: String): DeviceField(block, fieldId) {
        override val value: Long?
            get() = super.field.longValue
    }

    class DeviceFieldDouble(block: Block, fieldId: String): DeviceField(block, fieldId) {
        override val value: Double?
            get() = super.field.doubleValue
    }

    class DeviceFieldString(block: Block, fieldId: String): DeviceField(block, fieldId) {
        override val value: String?
            get() = super.field.stringValue
    }

    class DeviceFieldStringList(block: Block, fieldId: String): DeviceField(block, fieldId) {
        override val value: List<String>?
            get() = super.field.stringListValue
    }

<#list schemaDevice.blocks as block>
    // ==========================================
    /**
     * ${block.description}
     */
    val ${asVariableName(block.id)} = ${asClassName(block.id)}(schemaDevice.getBlock("${block.id}") ?: throw IllegalArgumentException("The generated code was unable to find the Block \"${block.id}\""))

    class ${asClassName(block.id)}(private val block: Block) {
        /**
        * Directly update all fields in this Block
        */
        fun update() = block.fields.forEach { it.update() }

        /**
        * All fields in this Block must be kept up-to-date
        */
        fun need() = block.fields.forEach { it.need() }

        /**
        * All fields in this Block no longer need to be kept up-to-date
        */
        fun unNeed() = block.fields.forEach { it.unNeed() }

<#list block.fields as field>
<#if !field.system>
        /**
         * ${field.description}
         <#if field.unit?has_content>
         * Unit: ${field.unit}
         </#if>
         */
        val ${asVariableName(field.id)} = DeviceField${asClassName(field.returnType.enumName)}(block, "${field.id}")
</#if>
</#list>
    }
</#list>
}
