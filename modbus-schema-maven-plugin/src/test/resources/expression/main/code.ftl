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
<#import "expression.ftl" as expr>
//
// Generated using the nl.basjes.modbus:modbus-schema-maven-plugin:${pluginVersion}
// Using the CUSTOM template to generate Custom code.
// https://modbus.basjes.nl
//

// ===========================================================
//               !!! THIS IS GENERATED CODE !!!
// -----------------------------------------------------------
//       EVERY TIME THE SOFTWARE IS BUILD THIS FILE IS
//        REGENERATED AND ALL MANUAL CHANGES ARE LOST
// ===========================================================
PACKAGE: ${packageName}
CLASS:   ${asClassName(className)}

<#list schemaDevice.blocks as block>
// ==========================================
BLOCK: ${asClassName(block.id)} -> id = "${block.id}"<#if block.description??> ; description = "${block.description}"</#if>
// FIELDS
<#list block.fields as field>
    // ==========================================
    FIELD: ${asClassName(field.id)} ; id = "${field.id}"; description = "${field.description}"; unit = "${field.unit}"; immutable = ${field.immutable?string('true', 'false')}; system = ${field.system?string('true', 'false')}; fetchGroup = "${field.fetchGroup}" ))
           GETTER     = field.get${asClassName(valueGetter(field.returnType))}();
           ORIGINAL  EXPRESSION = ${field.parsedExpression}
           REWRITTEN EXPRESSION = <@expr.expression expr=field.parsedExpression/>
</#list>
</#list>
