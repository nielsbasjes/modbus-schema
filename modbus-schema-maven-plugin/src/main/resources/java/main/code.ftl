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
// Using the builtin template to generate Java MAIN code.
// https://modbus.basjes.nl
//

// ===========================================================
//               !!! THIS IS GENERATED CODE !!!
// -----------------------------------------------------------
//       EVERY TIME THE SOFTWARE IS BUILD THIS FILE IS
//        REGENERATED AND ALL MANUAL CHANGES ARE LOST
// ===========================================================
package ${packageName};

import nl.basjes.modbus.device.api.AddressClass;
import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.schema.Field;
import nl.basjes.modbus.schema.FieldBoolean;
import nl.basjes.modbus.schema.FieldLong;
import nl.basjes.modbus.schema.FieldDouble;
import nl.basjes.modbus.schema.FieldString;
import nl.basjes.modbus.schema.FieldStringList;
import nl.basjes.modbus.schema.Block;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.basjes.modbus.schema.YamlLoaderKt;
import nl.basjes.modbus.schema.fetcher.ModbusQuery;
import nl.basjes.modbus.schema.test.TestScenario;
import nl.basjes.modbus.schema.utils.StringTable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static nl.basjes.modbus.schema.YamlLoaderKt.toSchemaDevice;

/**
* ${schemaDevice.description}
*/
public class ${asClassName(className)} extends SchemaDevice {

    public ${asClassName(className)}() {
        super("${escapeForJava(schemaDevice.description)}", ${schemaDevice.maxRegistersPerModbusRequest});
        initialize();
    }

    @Override
    public ${asClassName(className)} connectBase(ModbusDevice modbusDevice) {
        super.connectBase(modbusDevice);
        return this;
    }

    @Override
    public ${asClassName(className)} connect(ModbusDevice modbusDevice) {
        super.connect(modbusDevice);
        return this;
    }

    @Override
    public ${asClassName(className)} connect(ModbusDevice modbusDevice, int allowedGapReadSize) {
        super.connect(modbusDevice, allowedGapReadSize);
        return this;
    }

<#list schemaDevice.blocks as block>
    // ==========================================
    /**
     * ${block.description}
     */
    public final ${asClassName(block.id)} ${asVariableName(block.id)} = new ${asClassName(block.id)}(this);

    public static class ${asClassName(block.id)} extends Block {
        ${asClassName(block.id)}(SchemaDevice schemaDevice) {
        super(
            schemaDevice,
            "${block.id}",
<#if block.description??>
            "${escapeForJava(block.description)}",
<#else>
            "",
</#if>
<#if block.shortDescription??>
            "${escapeForJava(block.shortDescription)}"
<#else>
            ""
</#if>
            );
        }
<#list block.fields as field>

        // ==========================================
        /**
         * ${field.description}
         <#if field.unit?has_content>
         * Unit: ${field.unit}
         </#if>
         */
        <#if field.system>private<#else>public</#if> final ${fieldSubClass(field.returnType)} ${asVariableName(field.id)} = new ${fieldSubClass(field.returnType)} (
            /* block            */ this,
            /* id               */ "${field.id}",
<#if block.description??>
            /* description      */ "${escapeForJava(field.description)}",
<#else>
            /* description      */ "",
</#if>
<#if block.shortDescription??>
            /* shortDescription */ "${escapeForJava(field.shortDescription)}",
<#else>
            /* shortDescription */ "",
</#if>
            /* immutable        */ ${field.immutable?string('true', 'false')},
            /* system           */ ${field.system?string('true', 'false')},
            /* expression       */ "${escapeForJava(field.parsedExpression.toString())}",
            /* unit             */ "${field.unit}",
            /* fetchGroup       */ "${field.fetchGroup}"
            );
</#list>

        @Override
        public String toString() {
            StringTable table = new StringTable();
            table.withHeaders("Block", "Field", "Value");
            toStringTable(table);
            return table.toString();
        }

        private void toStringTable(StringTable table) {
<#assign nonSystemFields=block.fields?filter(f -> !f.system)>
<#if nonSystemFields?has_content>
            table
<#list nonSystemFields as field>
<#assign fieldId="\""+field.id+"\",">
                .addRow("${block.id}", ${fieldId?right_pad(block.maxFieldIdLength+3)} "" + ${asVariableName(field.id)}.getValue())
</#list>;
<#else>
            // This block has no fields
</#if>
        }
    }
</#list>

    @Override
    public String toString() {
        StringTable table = new StringTable();
        table.withHeaders("Block", "Field", "Value");
<#list schemaDevice.blocks as block>
        ${asVariableName(block.id)?right_pad(schemaDevice.maxBlockIdLength+1)}.toStringTable(table);
</#list>
        return table.toString();
    }

}
