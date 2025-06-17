<#--                                                                           -->
<#-- Modbus Schema toolkit                                                     -->
<#-- Copyright (C) 2019-2025 Niels Basjes                                      -->
<#--                                                                           -->
<#-- Licensed under the Apache License, Version 2.0 (the "License");           -->
<#-- you may not use this file except in compliance with the License.          -->
<#-- You may obtain a copy of the License at                                   -->
<#--                                                                           -->
<#-- https://www.apache.org/licenses/LICENSE-2.0                               -->
<#--                                                                           -->
<#-- Unless required by applicable law or agreed to in writing, software       -->
<#-- distributed under the License is distributed on an "AS IS" BASIS,         -->
<#-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  -->
<#-- See the License for the specific language governing permissions and       -->
<#-- limitations under the License.                                            -->
<#--                                                                           -->
//
// Generated using the nl.basjes.modbus:modbus-schema-maven-plugin:${pluginVersion}
// Using the CUSTOM template to generate Java TEST code.
// https://modbus.basjes.nl
//

// ===========================================================
//               !!! THIS IS GENERATED CODE !!!
// -----------------------------------------------------------
//       EVERY TIME THE SOFTWARE IS BUILD THIS FILE IS
//        REGENERATED AND ALL MANUAL CHANGES ARE LOST
// ===========================================================
package ${packageName};

import nl.basjes.modbus.device.api.Address;
import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.device.memory.MockedModbusDevice;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.basjes.modbus.schema.test.TestScenarioResultsList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing the ${asClassName(className)} class for: ${schemaDevice.description}
 */
class Test${asClassName(className)} {

    @Test
    void ensureValidSchema() throws ModbusException {
        SchemaDevice schemaDevice = new ${asClassName(className)}().schemaDevice;
        assertTrue(schemaDevice.initialize(), "Unable to initialize schema device");
        TestScenarioResultsList results = schemaDevice.verifyProvidedTests();
        assertTrue(results.logResults(), "Unable to verify all tests defined in the schema definition" );
    }

<#list schemaDevice.tests as testScenario>
    // ==========================================
    @Test
    // ${testScenario.name!"Unknown Test"} (${testScenario.description!""})
    void verifyProvidedTest_${asClassName(testScenario.name)}() throws ModbusException  {
        MockedModbusDevice modbusDevice = MockedModbusDevice.builder().build();
        ${asClassName(className)} ${asVariableName(className)} = new ${asClassName(className)}().connect(modbusDevice);
<#list testScenario.modbusBlocks as modbusBlock>
        modbusDevice.addModbusValues(Address.of("${modbusBlock.firstAddress}"), "${asString(modbusBlock)}");
</#list>
        ${asVariableName(className)}.updateAll();
<#list testScenario.expectedBlocks as expectedBlock>
        // ----------
        // Block: ${expectedBlock.blockId}
<#list expectedBlock.expected?keys as fieldName>
<#assign field=schemaDevice.getBlock(expectedBlock.blockId).getField(fieldName)>
<#if !field.system>
<#assign fieldReturnType=valueGetter(field.returnType)>
<#if expectedBlock.expected[fieldName]?has_content>
<#if fieldReturnType == "longValue">
        assertEquals(${expectedBlock.expected[fieldName][0]}L<#if field.unit?has_content> /* ${field.unit} */</#if>, ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.getValue());
<#elseif fieldReturnType == "doubleValue">
<#assign tempVariableName>${asVariableName(expectedBlock.blockId)}${asClassName(fieldName)}Value</#assign>
        assertEquals(${expectedBlock.expected[fieldName][0]}<#if field.unit?has_content> /* ${field.unit} */</#if>, ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.getValue(), 0.001);
<#elseif fieldReturnType == "stringListValue">
        assertEquals(List.of(<#list expectedBlock.expected[fieldName] as exp>"${exp}"<#sep >, </#list>), ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.getValue());
<#elseif fieldReturnType == "booleanValue">
        assertEquals(${expectedBlock.expected[fieldName][0]}, ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.getValue());
<#else>
        assertEquals("${expectedBlock.expected[fieldName][0]}", ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.getValue());
</#if>
</#if>
</#if>
</#list>
</#list>
        // ----------
        System.out.println(${asVariableName(className)});
    }
</#list>
}

