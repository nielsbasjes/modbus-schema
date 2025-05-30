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
// Using the builtin template to generate Kotlin TEST code.
// https://modbus.basjes.nl
//

// ===========================================================
//               !!! THIS IS GENERATED CODE !!!
// -----------------------------------------------------------
//       EVERY TIME THE SOFTWARE IS BUILD THIS FILE IS
//        REGENERATED AND ALL MANUAL CHANGES ARE LOST
// ===========================================================
package ${packageName}

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.memory.MockedModbusDevice
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/**
 * Testing the ${asClassName(className)} class for: ${schemaDevice.description}
 */
internal class Test${asClassName(className)} {

    @Test
    fun ensureValidSchema() {
        val schemaDevice = ${asClassName(className)}().schemaDevice
        val results = schemaDevice.verifyProvidedTests()
        assertTrue(results.logResults(), "Unable to verify all tests defined in the schema definition" )
    }

<#list schemaDevice.tests as testScenario>
    // ==========================================
    @Test
    // ${testScenario.name!"Unknown Test"} (${testScenario.description!""})
    fun verifyProvidedTest_${asClassName(testScenario.name)}() {
        val modbusDevice = MockedModbusDevice.builder().build()
        val ${asVariableName(className)} = ${asClassName(className)}().connect(modbusDevice)
<#list testScenario.registerBlocks as registerBlock>
        modbusDevice.addRegisters(Address.of("${registerBlock.firstAddress}"), """
${indent(hexStringMultiLine(registerBlock),"            ")}
            """.trimIndent());
</#list>
        ${asVariableName(className)}.updateAll()
<#list testScenario.expectedBlocks as expectedBlock>
<#list expectedBlock.expected?keys as fieldName>
<#assign field=schemaDevice.getBlock(expectedBlock.blockId).getField(fieldName)>
<#if !field.system>
<#if (expectedBlock.expected[fieldName][0])??>
<#assign fieldReturnType=valueGetter(field.returnType)>
<#if fieldReturnType == "longValue">
        assertEquals(${expectedBlock.expected[fieldName][0]}<#if field.unit?has_content> /* ${field.unit} */</#if>, ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.value)
<#elseif fieldReturnType == "doubleValue">
<#assign tempVariableName>${asVariableName(expectedBlock.blockId)}${asClassName(fieldName)}Value</#assign>
        assertEquals(${expectedBlock.expected[fieldName][0]}<#if field.unit?has_content> /* ${field.unit} */</#if>, ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.value ?: Double.NaN, 0.001)
<#elseif fieldReturnType == "stringListValue">
        assertEquals(listOf(<#list expectedBlock.expected[fieldName] as exp>"${exp}"<#sep >, </#list>), ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.value)
<#--        assertEquals(listOf(<#list expectedBlock.expected[fieldName] as expectedStringListValue>"${expectedStringListValue}"<#sep>, </#list>), schemaDevice.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.value)-->
<#else>
        assertEquals("${expectedBlock.expected[fieldName][0]}", ${asVariableName(className)}.${asVariableName(expectedBlock.blockId)}.${asVariableName(fieldName)}.value)
</#if>
</#if>
</#if>
</#list>
</#list>
    }
</#list>
}
