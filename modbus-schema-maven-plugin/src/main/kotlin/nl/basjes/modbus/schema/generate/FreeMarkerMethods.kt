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
import nl.basjes.modbus.schema.toYaml
import nl.basjes.modbus.schema.utils.CodeGeneration.convertToCodeCompliantName

fun Configuration.registerAdditionalMethods() = run {
    this.setSharedVariable("packageAsPath", PackageAsPath())
    this.setSharedVariable("asClassName", MakeCodeCompliantName(true))
    this.setSharedVariable("asVariableName", MakeCodeCompliantName(false))
    this.setSharedVariable("yamlSchema", SchemaDeviceAsYamlSchema())
    this.setSharedVariable("breakStringBlock", BreakStringBlock())
    this.setSharedVariable("jvmReturnType", ReturnTypeToJVMType())
    this.setSharedVariable("valueGetter", ReturnTypeValueGetter())
    this.setSharedVariable("hexString", RegisterBlockAsHexString())
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

class MakeCodeCompliantName(private val firstUppercase: Boolean) : BaseSingleStringMethod() {
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
        for(line in source.lines()) {
            result.add(line)
            if (++count > 500) {
                count=0
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
        when(input) {
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
        when(input) {
            ReturnType.BOOLEAN -> TODO()
            ReturnType.LONG -> "longValue"
            ReturnType.DOUBLE -> "doubleValue"
            ReturnType.STRING -> "stringValue"
            ReturnType.STRINGLIST -> "stringListValue"
            else -> "**UNKNOWN**"
        }
}



class RegisterBlockAsHexString : TemplateMethodModelEx {
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
        return SimpleScalar(input.toHexString())
    }
}
