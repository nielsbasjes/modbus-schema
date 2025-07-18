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
<#if isExpressionType(expr, "ExpressionGetModbusDiscretes")>  GetModbusDiscretes( <#list expr.requiredAddresses as address>${address.toModiconX()}<#sep>, </#sep></#list>  )<#else>
<#if isExpressionType(expr, "ExpressionBooleanBit")>          Boolean(            <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> )<#else>
<#if isExpressionType(expr, "ExpressionBooleanBitset")>       BooleanFromBitset(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>, ${expr.bitNr?c} )<#else>
<#if isExpressionType(expr, "ExpressionBooleanConstant")>     BooleanConstant(    ${expr.value?c}                                                                          )<#else>
<#if isExpressionType(expr, "ExpressionBooleanField")>        BooleanField(       ${expr.fieldName}                                                                        )<#else>
<#if isExpressionType(expr, "ExpressionRegistersConstant")>   RegistersConstant( |PER REGISTER| <@hexValues hexStrings=expr.asRegisterHexStrings/> |OR PER BYTE| <@hexValues hexStrings=expr.asByteHexStrings/> )<#else>
<#if isExpressionType(expr, "ExpressionGetModbusRegisters")>  GetModbusRegisters( <#list expr.requiredAddresses as address>${address.toModiconX()}<#sep>, </#sep></#list>  )<#else>
<#if isExpressionType(expr, "ExpressionSwapBytes")>           SwapBytes(          <#list expr.requiredAddresses as address>${address.toModiconX()}<#sep>, </#sep></#list>  )<#else>
<#if isExpressionType(expr, "ExpressionSwapEndian")>          SwapEndian(         <#list expr.requiredAddresses as address>${address.toModiconX()}<#sep>, </#sep></#list>  )<#else>
<#if isExpressionType(expr, "ExpressionLongConstant")>        LongConstant(       ${expr.value?c}                                                                          )<#else>
<#if isExpressionType(expr, "ExpressionDoubleConstant")>      DoubleConstant(     ${expr.value?c}                                                                          )<#else>
<#if isExpressionType(expr, "ExpressionNumericalField")>      NumericalField(     ${expr.fieldName}                                                                        )<#else>
<#if isExpressionType(expr, "ExpressionAdd")>                 Add(                <@expression expr=expr.left/>     , <@expression expr=expr.right/>                       )<#else>
<#if isExpressionType(expr, "ExpressionSubtract")>            Subtract(           <@expression expr=expr.left/>     , <@expression expr=expr.right/>                       )<#else>
<#if isExpressionType(expr, "ExpressionMultiply")>            Multiply(           <@expression expr=expr.left/>     , <@expression expr=expr.right/>                       )<#else>
<#if isExpressionType(expr, "ExpressionDivide")>              Divide(             <@expression expr=expr.dividend/> , <@expression expr=expr.divisor/>                     )<#else>
<#if isExpressionType(expr, "ExpressionPower")>               Power(              <@expression expr=expr.base/>     , <@expression expr=expr.exponent/>                    )<#else>
<#if isExpressionType(expr, "ExpressionIEEE754Float32")>      IEEE754Float32(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIEEE754Float64")>      IEEE754Float64(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIntegerSigned16")>     IntegerSigned16(    <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIntegerSigned32")>     IntegerSigned32(    <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIntegerSigned64")>     IntegerSigned64(    <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIntegerUnsigned16")>   IntegerUnsigned16(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIntegerUnsigned32")>   IntegerUnsigned32(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIntegerUnsigned64")>   IntegerUnsigned64(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionBitsetStringList")>    BitsetStringList(   <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> BIT MAPPED TO <#list expr.mappings as bitNr, result>Bit   ${bitNr} -> ${result}<#sep> | </#sep></#list> <@notImplemented expr/>)<#else>
<#if isExpressionType(expr, "ExpressionStringConstant")>      StringConstant(     ${expr.value}                                                                            )<#else>
<#if isExpressionType(expr, "ExpressionEnumString")>          EnumString(         <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> MAPPED TO     <#list expr.mappings as value, result>Value ${value} -> ${result}<#sep> | </#sep></#list> <@notImplemented expr/>)<#else>
<#if isExpressionType(expr, "ExpressionEui48String")>         Eui48String(        <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionHexString")>           HexString(          <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIPv4AddrString")>      IPv4AddrString(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionIPv6AddrString")>      IPv6AddrString(     <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionStringConcat")>        StringConcat(       <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>   <@notImplemented expr/>  )<#else>
<#if isExpressionType(expr, "ExpressionStringField")>         StringField(        ${expr.fieldName}                                                                        )<#else>
<#if isExpressionType(expr, "ExpressionStringFromNumber")>    StringFromNumber(   <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>       )<#else>
<#if isExpressionType(expr, "ExpressionStringFromBoolean")>   StringFromBoolean(  <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list> ,  ${expr.zeroString},  ${expr.oneString}      )<#else>
<#if isExpressionType(expr, "ExpressionUTF8String")>          UTF8String(         <#list expr.subExpressions as expr><@expression expr=expr/><#sep>, </#sep></#list>       )<#else>
@@@ ERROR: MISSING EXPRESSION TYPE IN TEMPLATE @@@
</#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if></#if>
</#if></#if></#if></#if></#if>
</#compress></#macro>
