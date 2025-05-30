/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.modbus.schema.generate

import freemarker.ext.util.WrapperTemplateModel
import freemarker.template.Configuration
import freemarker.template.SimpleScalar
import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModelException
import nl.basjes.modbus.device.api.RegisterBlock
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.expression.Expression
import nl.basjes.modbus.schema.expression.numbers.Add
import nl.basjes.modbus.schema.expression.numbers.Divide
import nl.basjes.modbus.schema.expression.numbers.DoubleConstant
import nl.basjes.modbus.schema.expression.numbers.IEEE754Float32
import nl.basjes.modbus.schema.expression.numbers.IEEE754Float64
import nl.basjes.modbus.schema.expression.numbers.IntegerSigned16
import nl.basjes.modbus.schema.expression.numbers.IntegerSigned32
import nl.basjes.modbus.schema.expression.numbers.IntegerSigned64
import nl.basjes.modbus.schema.expression.numbers.IntegerUnsigned16
import nl.basjes.modbus.schema.expression.numbers.IntegerUnsigned32
import nl.basjes.modbus.schema.expression.numbers.IntegerUnsigned64
import nl.basjes.modbus.schema.expression.numbers.LongConstant
import nl.basjes.modbus.schema.expression.numbers.Multiply
import nl.basjes.modbus.schema.expression.numbers.NumericalField
import nl.basjes.modbus.schema.expression.numbers.Power
import nl.basjes.modbus.schema.expression.numbers.Subtract
import nl.basjes.modbus.schema.expression.registers.RegistersConstantExpression
import nl.basjes.modbus.schema.expression.registers.RegistersModbusExpression
import nl.basjes.modbus.schema.expression.registers.SwapBytes
import nl.basjes.modbus.schema.expression.registers.SwapEndian
import nl.basjes.modbus.schema.expression.strings.BitsetStringList
import nl.basjes.modbus.schema.expression.strings.EnumString
import nl.basjes.modbus.schema.expression.strings.Eui48String
import nl.basjes.modbus.schema.expression.strings.HexString
import nl.basjes.modbus.schema.expression.strings.IPv4AddrString
import nl.basjes.modbus.schema.expression.strings.IPv6AddrString
import nl.basjes.modbus.schema.expression.strings.StringConcat
import nl.basjes.modbus.schema.expression.strings.StringConstant
import nl.basjes.modbus.schema.expression.strings.StringField
import nl.basjes.modbus.schema.expression.strings.StringFromNumber
import nl.basjes.modbus.schema.expression.strings.UTF8String
import nl.basjes.modbus.schema.toYaml
import nl.basjes.modbus.schema.utils.CodeGeneration.convertToCodeCompliantName


val expressionMappings =
    mapOf(
        // Registers
        "ExpressionRegistersConstant"          to RegistersConstantExpression::class,
        "ExpressionGetModbus"                  to RegistersModbusExpression::class,
        "ExpressionSwapBytes"                  to SwapBytes::class,
        "ExpressionSwapEndian"                 to SwapEndian::class,
        // Numerical Values
        "ExpressionLongConstant"               to LongConstant::class,
        "ExpressionDoubleConstant"             to DoubleConstant::class,
        "ExpressionNumericalField"             to NumericalField::class,
        "ExpressionIEEE754Float32"             to IEEE754Float32::class,
        "ExpressionIEEE754Float64"             to IEEE754Float64::class,
        "ExpressionIntegerSigned16"            to IntegerSigned16::class,
        "ExpressionIntegerSigned32"            to IntegerSigned32::class,
        "ExpressionIntegerSigned64"            to IntegerSigned64::class,
        "ExpressionIntegerUnsigned16"          to IntegerUnsigned16::class,
        "ExpressionIntegerUnsigned32"          to IntegerUnsigned32::class,
        "ExpressionIntegerUnsigned64"          to IntegerUnsigned64::class,
        // Numerical Operations
        "ExpressionAdd"                        to Add::class,
        "ExpressionSubtract"                   to Subtract::class,
        "ExpressionMultiply"                   to Multiply::class,
        "ExpressionDivide"                     to Divide::class,
        "ExpressionPower"                      to Power::class,
        // String list
        "ExpressionBitsetStringList"           to BitsetStringList::class,
        // String values
        "ExpressionStringConstant"             to StringConstant::class,
        "ExpressionEnumString"                 to EnumString::class,
        "ExpressionEui48String"                to Eui48String::class,
        "ExpressionHexString"                  to HexString::class,
        "ExpressionIPv4AddrString"             to IPv4AddrString::class,
        "ExpressionIPv6AddrString"             to IPv6AddrString::class,
        "ExpressionStringField"                to StringField::class,
        "ExpressionUTF8String"                 to UTF8String::class,
        // String Operations
        "ExpressionStringConcat"               to StringConcat::class,
        "ExpressionStringFromNumber"           to StringFromNumber::class,
    )

fun Configuration.registerAdditionalMethods() =
    run {
        this.setSharedVariable("packageAsPath",         PackageAsPath())
        this.setSharedVariable("asClassName",           MakeCodeCompliantName(true))
        this.setSharedVariable("asVariableName",        MakeCodeCompliantName(false))
        this.setSharedVariable("yamlSchema",            SchemaDeviceAsYamlSchema())
        this.setSharedVariable("breakStringBlock",      BreakStringBlock())
        this.setSharedVariable("jvmReturnType",         ReturnTypeToJVMType())
        this.setSharedVariable("valueGetter",           ReturnTypeValueGetter())
        this.setSharedVariable("hexString",             RegisterBlockAsHexString(false))
        this.setSharedVariable("hexStringMultiLine",    RegisterBlockAsHexString(true))
        this.setSharedVariable("indent",                Indent())
        // Determine if an expression is of a specific expression type
        // Usage:  <#if isExpressionType(expr, "ExpressionRegistersConstant")>...</#if>
        this.setSharedVariable("isExpressionType",      IsExpressionType())
    }

abstract class BaseSingleStringMethod : TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 1) {
            throw TemplateModelException("Need exactly 1 argument")
        }
        val input = arguments[0]
        if (input !is SimpleScalar) {
            throw TemplateModelException("Only works on Strings")
        }
        return SimpleScalar(transform(input.toString()))
    }

    abstract fun transform(input: String): String
}

class MakeCodeCompliantName(
    private val firstUppercase: Boolean,
) : BaseSingleStringMethod() {
    override fun transform(input: String) = convertToCodeCompliantName(input, firstUppercase)
}

class PackageAsPath : BaseSingleStringMethod() {
    override fun transform(input: String) = input.replace('.', '/')
}

class BreakStringBlock : TemplateMethodModelEx {
    /*
     * First argument in the template must be a String
     * Second argument is the String that is inserted as a new line every 500 lines.
     */
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 2) {
            throw TemplateModelException("Need exactly 2 arguments")
        }
        val arg0 = arguments[0]
        if (arg0 !is SimpleScalar) {
            throw TemplateModelException("Bad input: First argument must be a String")
        }
        val arg1 = arguments[1]
        if (arg1 !is SimpleScalar) {
            throw TemplateModelException("Bad input: Second argument must be a String")
        }

        val source = arg0.toString()
        val extraLine = arg1.toString()

        var count = 0
        val result = mutableListOf<String>()
        for (line in source.lines()) {
            result.add(line)
            if (++count > 500) {
                count = 0
                result.add(extraLine)
            }
        }
        return SimpleScalar(result.joinToString("\n"))
    }
}

class SchemaDeviceAsYamlSchema : TemplateMethodModelEx {
    /*
     * First argument in the template must be a Logical Device
     * Second (optional) argument is the String that is used as prefix for EACH line.
     */
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 1 && arguments.size != 2) {
            throw TemplateModelException("Need exactly 1 or 2 arguments")
        }
        val arg0 = arguments[0]
        if (arg0 !is WrapperTemplateModel) {
            throw TemplateModelException("Bad input: First argument must be a SchemaDevice")
        }
        val input = arg0.wrappedObject
        if (input !is SchemaDevice) {
            throw TemplateModelException("Bad input: First argument must be a SchemaDevice.")
        }

        val prefix: String
        if (arguments.size == 2) {
            val arg1 = arguments[1]
            if (arg1 !is SimpleScalar) {
                throw TemplateModelException("Bad input: Second argument must be a String")
            }
            prefix = arg1.toString()
        } else {
            prefix = ""
        }
        return SimpleScalar(input.toYaml().replaceIndent(prefix))
    }
}

abstract class BaseSingleReturnTypeMethod : TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 1) {
            throw TemplateModelException("Need exactly 1 argument")
        }
        val arg0 = arguments[0]
        if (arg0 !is WrapperTemplateModel) {
            throw TemplateModelException("Bad input")
        }
        val input = arg0.wrappedObject
        if (input !is ReturnType) {
            throw TemplateModelException("Only works on ReturnType")
        }
        return SimpleScalar(transform(input))
    }

    abstract fun transform(input: ReturnType): String
}

class ReturnTypeToJVMType : BaseSingleReturnTypeMethod() {
    override fun transform(input: ReturnType) =
        when (input) {
            ReturnType.BOOLEAN -> TODO()
            ReturnType.LONG -> "Long"
            ReturnType.DOUBLE -> "Double"
            ReturnType.STRING -> "String"
            ReturnType.STRINGLIST -> "List<String>"
            else -> "**UNKNOWN**"
        }
}

class ReturnTypeValueGetter : BaseSingleReturnTypeMethod() {
    override fun transform(input: ReturnType) =
        when (input) {
            ReturnType.BOOLEAN -> TODO()
            ReturnType.LONG -> "longValue"
            ReturnType.DOUBLE -> "doubleValue"
            ReturnType.STRING -> "stringValue"
            ReturnType.STRINGLIST -> "stringListValue"
            else -> "**UNKNOWN**"
        }
}

class RegisterBlockAsHexString(
    val multiLine: Boolean
) : TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 1) {
            throw TemplateModelException("Need exactly 1 argument")
        }
        val arg0 = arguments[0]
        if (arg0 !is WrapperTemplateModel) {
            throw TemplateModelException("Bad input")
        }
        val input = arg0.wrappedObject
        if (input !is RegisterBlock) {
            throw TemplateModelException("Only works on RegisterBlock")
        }

        if (multiLine) {
            return SimpleScalar(input.toMultiLineString())
        } else {
            return SimpleScalar(input.toHexString())
        }
    }
}

class IsExpressionType : TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 2) {
            throw TemplateModelException(
                "Wrong arguments for method 'isExpressionType'. Method has two required parameters: expression and the template name of the type of expression",
            )
        }
        // -----
        val arg0 = arguments[0] ?: return false
        if (arg0 !is WrapperTemplateModel) {
            throw TemplateModelException("Bad input: First argument must be a Expression")
        }
        val theExpression = arg0.wrappedObject
        if (theExpression !is Expression) {
            throw TemplateModelException("Bad input: First argument must be an Expression.")
        }
        // -----
        val arg1 = arguments[1] ?: return false
        if (arg1 !is SimpleScalar) {
            throw TemplateModelException("Bad input: Second argument must be a String (the template name of the class")
        }
        val theClassName  = arg1.toString()
        val theClass = expressionMappings[theClassName] ?: return false

        // -----
        return theClass.java.isAssignableFrom(theExpression.javaClass)
    }
}

class Indent: TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>): Any {
        if (arguments.size != 2) {
            throw TemplateModelException("Need exactly 2 arguments")
        }
        val input = arguments[0]
        if (input !is SimpleScalar) {
            throw TemplateModelException("Bad input: First argument must be a String")
        }

        val prefix = arguments[1]
        if (prefix !is SimpleScalar) {
            throw TemplateModelException("Bad input: Second argument must be a String")
        }
        return SimpleScalar(input.toString().replaceIndent(prefix.toString()))
    }
}
