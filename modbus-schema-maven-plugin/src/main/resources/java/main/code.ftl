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
import nl.basjes.modbus.schema.Block;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.basjes.modbus.schema.YamlLoaderKt;
import nl.basjes.modbus.schema.test.TestScenario;
import nl.basjes.modbus.schema.utils.StringTable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static nl.basjes.modbus.schema.YamlLoaderKt.toSchemaDevice;

/**
* ${schemaDevice.description}
*/
public class ${asClassName(className)} {

    public ${asClassName(className)}() {
        schemaDevice.initialize();
    }

    public final SchemaDevice schemaDevice = new SchemaDevice();

    public ${asClassName(className)} connectBase(ModbusDevice modbusDevice) {
        schemaDevice.connectBase(modbusDevice);
        return this;
    }

    public ${asClassName(className)} connect(ModbusDevice modbusDevice){
        schemaDevice.connect(modbusDevice);
        return this;
    }

    public ${asClassName(className)} connect(ModbusDevice modbusDevice, int allowedGapReadSize){
        schemaDevice.connect(modbusDevice, allowedGapReadSize);
        return this;
    }

    /**
     * Update all registers related to the needed fields to be updated
     */
    public void update() {
        schemaDevice.update();
    }

    /**
     * Update all registers related to the needed fields to be updated with a maximum age of the provided milliseconds
     * @param maxAge maximum age of the fields in milliseconds
     */
    public void update(Long maxAge) {
        schemaDevice.update(maxAge);
    }

    /**
     * Update all registers related to the specified field
     * @param field the Field that must be updated
     */
    public void update(Field field) {
        schemaDevice.update(field);
    }

    /**
     * Make sure all registers mentioned in all known fields are retrieved.
     */
    public void updateAll() throws ModbusException {
        schemaDevice.updateAll();
    }

    /**
     * @param field The field that must be kept up-to-date
     */
    public void need(Field field) {
        schemaDevice.need(field);
    }

    /**
     * @param field The field that no longer needs to be kept up-to-date
     */
    public void unNeed(Field field) {
        schemaDevice.unNeed(field);
    }

    /**
     * We want all fields to be kept up-to-date
     */
    public void needAll() {
        schemaDevice.needAll();
    }

    /**
     * We no longer want all fields to be kept up-to-date
     */
    public void unNeedAll() {
        schemaDevice.unNeedAll();
    }

    abstract public static class DeviceField {
      public final Field field;
      public DeviceField(Field field) {
        this.field = field;
      }
      /**
       * Retrieve the value of this field using the currently available device data.
       */
      abstract Object getValue();
      /**
       * We want this field to be kept up-to-date
       */
      public void need() {
          field.need();
      }
      /**
       * We no longer want this field to be kept up-to-date
       */
      public void unNeed() {
          field.unNeed();
      }
      /**
       * Directly update this field
       */
      public void update() {
          field.update();
      }
    }

<#list schemaDevice.blocks as block>
    // ==========================================
    /**
     * ${block.description}
     */
    public final ${asClassName(block.id)} ${asVariableName(block.id)} = new ${asClassName(block.id)}(schemaDevice);

    public static class ${asClassName(block.id)} {
        private Block block;
        ${asClassName(block.id)}(SchemaDevice schemaDevice) {
            this.block = Block.builder()
              .schemaDevice(schemaDevice)
              .id("${block.id}")
<#if block.description??>
              .description("${block.description}")
</#if>
              .build();

<#list block.fields as field>
            this.${asVariableName(field.id)?right_pad(block.maxFieldIdLength)} = new ${asClassName(field.id)}(block);
</#list>
        }

        /**
         * Directly update all fields in this Block
         */
        public void update() {
            block.getFields().stream().forEach(Field::update);
        }

        /**
         * All fields in this Block must be kept up-to-date
         */
        public void need() {
            block.getFields().stream().forEach(Field::need);
        }

        /**
         * All fields in this Block no longer need to be kept up-to-date
         */
        public void unNeed() {
            block.getFields().stream().forEach(Field::unNeed);
        }
<#list block.fields as field>

        // ==========================================
        /**
         * ${field.description}
         <#if field.unit?has_content>
         * Unit: ${field.unit}
         </#if>
         */
        <#if field.system>private<#else>public</#if> final ${asClassName(field.id)} ${asVariableName(field.id)};
        <#if field.system>private<#else>public</#if> static class ${asClassName(field.id)} extends DeviceField {
            public ${asClassName(field.id)}(Block block) {
                super(Field.builder()
                           .block(block)
                           .id("${field.id}")
                           .description("${field.description}")
                           .expression("${field.parsedExpression}")
                           .unit("${field.unit}")
                           .immutable(${field.immutable?string('true', 'false')})
                           .system(${field.system?string('true', 'false')})
                           .fetchGroup("${field.fetchGroup}")
                           .build());
            }

            @Override
            public ${jvmReturnType(field.returnType)} getValue() {
                return field.get${asClassName(valueGetter(field.returnType))}();
            }
        }
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
