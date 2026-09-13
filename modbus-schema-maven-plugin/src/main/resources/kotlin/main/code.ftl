<#--                                                                          -->
<#-- Modbus Schema toolkit                                                    -->
<#-- Copyright (C) 2019-2026 Niels Basjes                                     -->
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
import nl.basjes.modbus.schema.FieldBoolean
import nl.basjes.modbus.schema.FieldLong
import nl.basjes.modbus.schema.FieldDouble
import nl.basjes.modbus.schema.FieldString
import nl.basjes.modbus.schema.FieldStringList
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.toSchemaDevice
import nl.basjes.modbus.schema.utils.StringTable

/**
 * ${schemaDevice.description}
 */
open class ${asClassName(className)} : SchemaDevice("${escapeForJava(schemaDevice.description)}", ${schemaDevice.maxRegistersPerModbusRequest}) {

    override fun connectBase(
        modbusDevice: ModbusDevice,
    ): ${asClassName(className)} {
        super.connectBase(modbusDevice)
        return this
    }

    override fun connect(
        modbusDevice: ModbusDevice,
    ): ${asClassName(className)} {
        super.connect(modbusDevice, 100)
        return this
    }

    override fun connect(
        modbusDevice: ModbusDevice,
        /**
         * How many registers may needlessly be read to optimize fetching
         */
        allowedGapReadSize: Int,
    ): ${asClassName(className)} {
        super.connect(modbusDevice, allowedGapReadSize)
        return this
    }

<#list schemaDevice.blocks as block>
    // ==========================================
    /**
     * ${block.description}
     */
    val ${asVariableName(block.id)} = ${asClassName(block.id)}(this);

    class ${asClassName(block.id)}(schemaDevice: SchemaDevice): Block(
        schemaDevice = schemaDevice,
        id = "${block.id}",
<#if block.description??>
        description = "${escapeForJava(block.description)}",
</#if>
<#if block.shortDescription??>
        shortDescription = "${escapeForJava(block.shortDescription)}",
</#if>
    ) {
<#list block.fields as field>

        // ==========================================
        /**
         * ${field.description}
         <#if field.unit?has_content>
         * Unit: ${field.unit}
         </#if>
         */
        <#if field.system>private<#else>public</#if> val ${asVariableName(field.id)} = ${fieldSubClass(field.returnType)}(
            block        = this,
            id           = "${field.id}",
<#if block.description??>
            description  = "${escapeForJava(field.description)}",
</#if>
            expression   = "${escapeForJava(field.parsedExpression.toString())}",
<#if field.unit?has_content>
            unit         = "${field.unit}",
</#if>
            immutable    = ${field.immutable?string('true', 'false')},
            system       = ${field.system?string('true', 'false')},
            fetchGroup   = "${field.fetchGroup}",
        )
</#list>

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
        require(initialize()) { "Unable to initialize schema device" }
    }

}
