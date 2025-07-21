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
<#macro notImplemented expr><#compress>
  <#if expr.notImplemented??>
    <#if expr.notImplemented?has_content>
    |  NOT IMPLEMENTED:[ <#list expr.notImplemented as notImplementedHexList>[<@hexValues hexStrings=notImplementedHexList/>]<#sep>, </#sep></#list> ]
    </#if>
  </#if>
</#compress></#macro>

<#macro hexValues hexStrings><#compress>
    [ <#list hexStrings as hexString>0x${hexString}<#sep>, </#sep></#list> ]
</#compress></#macro>

<#macro expression expr><#compress>
<#if !expr?has_content>
NULL EXPRESSION
<#else>
<#switch expressionType(expr)>
<#on "ExpressionGetModbusDiscretes">  GetModbusDiscretes( <#list expr.requiredAddresses as address>${address.toModiconX()}<#sep>, </#sep></#list>  )
<#on "ExpressionBooleanConstant">     BooleanConstant(    ${expr.value?c}                                                                          )
<#on "ExpressionBooleanBitset">       BooleanFromBitset(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>, ${expr.bitNr?c}  <@notImplemented expr/>)
<#on "ExpressionBooleanField">        BooleanField(       ${expr.fieldName}                                                                        )

<#on "ExpressionRegistersConstant">   RegistersConstant( |PER REGISTER| <@hexValues hexStrings=expr.asRegisterHexStrings/> |OR PER BYTE| <@hexValues hexStrings=expr.asByteHexStrings/> )
<#on "ExpressionGetModbusRegisters">  GetModbusRegisters( <#list expr.requiredAddresses as address>${address.toModiconX()}<#sep>, </#sep></#list>  )
<#on "ExpressionSwapBytes">           SwapBytes(          <@expression expr=expr.registers/>  )
<#on "ExpressionSwapEndian">          SwapEndian(         <@expression expr=expr.registers/>  )

<#on "ExpressionLongConstant">        LongConstant(       ${expr.value?c}                                                                          )
<#on "ExpressionDoubleConstant">      DoubleConstant(     ${expr.value?c}                                                                          )
<#on "ExpressionNumericalField">      NumericalField(     ${expr.fieldName}                                                                        )
<#on "ExpressionAdd">                 Add(                <@expression expr=expr.left/>     , <@expression expr=expr.right/>                       )
<#on "ExpressionSubtract">            Subtract(           <@expression expr=expr.left/>     , <@expression expr=expr.right/>                       )
<#on "ExpressionMultiply">            Multiply(           <@expression expr=expr.left/>     , <@expression expr=expr.right/>                       )
<#on "ExpressionDivide">              Divide(             <@expression expr=expr.dividend/> , <@expression expr=expr.divisor/>                     )
<#on "ExpressionPower">               Power(              <@expression expr=expr.base/>     , <@expression expr=expr.exponent/>                    )
<#on "ExpressionIEEE754Float32">      IEEE754Float32(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIEEE754Float64">      IEEE754Float64(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIntegerSigned16">     IntegerSigned16(    <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIntegerSigned32">     IntegerSigned32(    <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIntegerSigned64">     IntegerSigned64(    <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIntegerUnsigned16">   IntegerUnsigned16(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIntegerUnsigned32">   IntegerUnsigned32(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIntegerUnsigned64">   IntegerUnsigned64(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )

<#on "ExpressionBitsetStringList">    BitsetStringList(   <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> BIT MAPPED TO <#list expr.mappings as bitNr, result>Bit   ${bitNr} -> ${result}<#sep> | </#sep></#list> <@notImplemented expr/>)

<#on "ExpressionStringConstant">      StringConstant(     ${expr.value}                                                                            )
<#on "ExpressionEnumString">          EnumString(         <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> MAPPED TO     <#list expr.mappings as value, result>Value ${value} -> ${result}<#sep> | </#sep></#list> <@notImplemented expr/>)
<#on "ExpressionEui48String">         Eui48String(        <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionHexString">           HexString(          <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIPv4AddrString">      IPv4AddrString(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionIPv6AddrString">      IPv6AddrString(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionStringConcat">        StringConcat(       <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )
<#on "ExpressionStringField">         StringField(        ${expr.fieldName}                                                                        )
<#on "ExpressionStringFromNumber">    StringFromNumber(   <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>       )
<#on "ExpressionStringFromBoolean">   StringFromBoolean(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> ,  ${expr.zeroString},  ${expr.oneString}      )
<#on "ExpressionUTF8String">          UTF8String(         <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>       )

<#default>@@@ ERROR: MISSING EXPRESSION TYPE IN TEMPLATE @@@
</#switch>
</#if>
</#compress></#macro>
