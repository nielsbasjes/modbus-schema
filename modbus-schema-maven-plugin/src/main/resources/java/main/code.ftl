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
    public static final String schema = List.of("""
${breakStringBlock(yamlSchema(schemaDevice), "\"\"\",\"\"\"")}
""").stream().collect(Collectors.joining());

    public final SchemaDevice schemaDevice = toSchemaDevice(schema);

    public final List<TestScenario> tests = schemaDevice.getTests();

    public ${asClassName(className)} connectBase(ModbusDevice modbusDevice) {
        schemaDevice.connectBase(modbusDevice);
        return this;
    }

    public ${asClassName(className)} connect(ModbusDevice modbusDevice){
        schemaDevice.connect(modbusDevice, 0);
        return this;
    }

    public ${asClassName(className)} connect(ModbusDevice modbusDevice, int allowedGapReadSize){
        schemaDevice.connect(modbusDevice, allowedGapReadSize);
        return this;
    }

    /**
     * Update all registers related to the needed fields to be updated with a maximum age of the provided milliseconds
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

    abstract public static class DeviceBlock {
        private Block block;
        public DeviceBlock(Block block) {
            if (block == null) {
                throw new IllegalArgumentException("The generated code was unable to find the Block \"" + block.getId() + "\"");
            }
            this.block = block;
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
    }

    abstract public static class DeviceField {
        public final Field field;
        public DeviceField(Block block, String fieldId) {
            field = block.getField(fieldId);
            if (field == null) {
                throw new IllegalArgumentException("The generated code was unable to find the Field \"" + fieldId + "\" in the Block \"" + block.getId() + "\"");
            }
        }
        public abstract Object getValue();

        public void need() {
            field.need();
        }

        public void unNeed() {
            field.unNeed();
        }

        @Override
        public String toString() {
            return getValue().toString();
        }
    }

    public static class DeviceFieldLong extends DeviceField {
        public DeviceFieldLong(Block block, String fieldId) {
            super(block, fieldId);
        }
        @Override
        public Long getValue() {
            return super.field.getLongValue();
        }
    }

    public static class DeviceFieldDouble extends DeviceField {
        public DeviceFieldDouble(Block block, String fieldId) {
            super(block, fieldId);
        }
        @Override
        public Double getValue() {
            return super.field.getDoubleValue();
        }
    }

    public static class DeviceFieldString extends DeviceField {
        public DeviceFieldString(Block block, String fieldId) {
            super(block, fieldId);
        }
        @Override
        public String getValue() {
            return super.field.getStringValue();
        }
    }

    public static class DeviceFieldStringList extends DeviceField {
        public DeviceFieldStringList(Block block, String fieldId) {
            super(block, fieldId);
        }
        @Override
        public List<String> getValue() {
            return super.field.getStringListValue();
        }
    }

  <#list schemaDevice.blocks as block>
    // ==========================================
    /**
    * ${block.description}
    */
    public final ${asClassName(block.id)} ${asVariableName(block.id)} = new ${asClassName(block.id)}(schemaDevice.getBlock("${block.id}"));

    public static class ${asClassName(block.id)} extends DeviceBlock {
        public final List<DeviceField> allFields;

        ${asClassName(block.id)}(Block block) {
            super(block);
            <#list block.fields as field>
            <#if !field.system>
            ${asVariableName(field.id)} = new DeviceField${asClassName(field.returnType.enumName)}(block, "${field.id}");
            </#if>
            </#list>

           allFields = Arrays.asList(
           <#list block.fields as field>
             <#if !field.system>
               ${asVariableName(field.id)},
             </#if>
           </#list>
               (DeviceField)null // Needed as terminator in the list because of the templating
           ).stream().filter(deviceField -> deviceField!=null).toList();
      }

    <#list block.fields as field>
      <#if !field.system>
      /**
        * ${field.description}
        <#if field.unit?has_content>
        * Unit: ${field.unit}
        </#if>
        */
      public final DeviceField${asClassName(field.returnType.enumName)} ${asVariableName(field.id)};
      </#if>
    </#list>

    @Override
        public String toString() {
            return new StringTable()
                .withHeaders("Field in Block \"${block.id}\"", "Value")
                <#list block.fields as field>
                <#if !field.system>
                .addRow( "${field.id}", "" + ${asVariableName(field.id)})
                </#if>
                </#list>
                .toString();
        }

    }
</#list>
}
