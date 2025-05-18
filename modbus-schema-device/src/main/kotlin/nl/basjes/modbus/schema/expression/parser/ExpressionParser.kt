/*
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
package nl.basjes.modbus.schema.expression.parser

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
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
import nl.basjes.modbus.schema.expression.numbers.NumericalExpression
import nl.basjes.modbus.schema.expression.numbers.NumericalField
import nl.basjes.modbus.schema.expression.numbers.Power
import nl.basjes.modbus.schema.expression.numbers.Subtract
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsLexer
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.AddSubtractContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.DoubleConstantContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.ExtraBracesContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.ImplicitMultiplyContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadIeee754_32Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadIeee754_64Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadInt16Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadInt32Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadInt64Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadUInt16Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadUInt32Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LoadUInt64Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.LongConstantContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.MultiplyDivideContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.NotImplementedContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.NumberFieldContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.PowerContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.RegisterCountContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.RegisterRangeContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.RegisterSwapBytesContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.RegisterSwapEndianContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.RegisterValuesContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.RegistersContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringConcatContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringConstantContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringEnumContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringEui48Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringFieldContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringHexContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringIPv4AddrContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringIPv6AddrContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringListBitSetContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringNumberContext
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParser.StringUtf8Context
import nl.basjes.modbus.schema.expression.parser.generated.FieldExpressionsParserBaseVisitor
import nl.basjes.modbus.schema.expression.registers.RegistersConstantExpression
import nl.basjes.modbus.schema.expression.registers.RegistersExpression
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
import nl.basjes.modbus.schema.expression.strings.StringExpression
import nl.basjes.modbus.schema.expression.strings.StringField
import nl.basjes.modbus.schema.expression.strings.StringFromNumber
import nl.basjes.modbus.schema.expression.strings.UTF8String
import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import java.util.TreeMap

// Sonar calls this a Monster Class
class ExpressionParser : FieldExpressionsParserBaseVisitor<Expression?>() {

    override fun aggregateResult(
        aggregate: Expression?,
        nextResult: Expression?,
    ): Expression? {
        if (nextResult == null) {
            return aggregate
        }
        return nextResult
    }

    private fun visitRegistersExpression(context: ParserRuleContext): RegistersExpression {
        val expression = super.visit(context)!!
        if (expression is RegistersExpression) {
            return expression
        }
        throw IllegalStateException(
            "The provided Expression MUST be an instance of ByteArrayExpression but was a " + expression.javaClass,
        )
    }

    private fun visitNumericalExpression(context: ParserRuleContext): NumericalExpression {
        val expression = super.visit(context)!!
        if (expression is NumericalExpression) {
            return expression
        }
        throw IllegalStateException(
            "The provided Expression MUST be an instance of NumericalExpression but was a " + expression.javaClass,
        )
    }

    private fun visitStringExpression(context: ParserRuleContext): StringExpression {
        val expression = super.visit(context)!!
        if (expression is StringExpression) {
            return expression
        }
        throw IllegalStateException(
            "The provided Expression MUST be an instance of StringExpression but was a " + expression.javaClass,
        )
    }

    // Constants
    override fun visitLongConstant(ctx: LongConstantContext): Expression {
        if (ctx.MINUS() == null) {
            return LongConstant(ctx.LONG().text.toLong())
        }
        return LongConstant(-1 * ctx.LONG().text.toLong())
    }

    override fun visitDoubleConstant(ctx: DoubleConstantContext): Expression {
        if (ctx.MINUS() == null) {
            return DoubleConstant(ctx.DOUBLE().text.toDouble())
        }
        return DoubleConstant(-1 * ctx.DOUBLE().text.toDouble())
    }

    override fun visitRegisterValues(ctx: RegisterValuesContext): Expression =
        RegistersConstantExpression(ctx.constantHexString().text)

    // Getting the raw register Addresses needed. !!MAINTAINING THE PROVIDED ORDER!!
    override fun visitRegisters(ctx: RegistersContext): Expression =
        RegistersModbusExpression(ctx.singleRegister().map { Address.of(it.text) }.toList())

    override fun visitRegisterCount(ctx: RegisterCountContext): Expression {
        val startRegister = Address.of(ctx.startRegister.text)
        val totalRegisters = ctx.count.text.toInt()

        if (totalRegisters <= 0) {
            throw ModbusSchemaParseException("Invalid register count specified: count=$totalRegisters")
        }

        return RegistersModbusExpression((0 until totalRegisters).map { count -> startRegister.increment(count) }.toList())
    }

    // Getting the raw register Addresses needed. !!MAINTAINING THE PROVIDED ORDER!!
    override fun visitRegisterRange(ctx: RegisterRangeContext): Expression {
        val startRegister = Address.of(ctx.startRegister.text)
        val lastRegister = Address.of(ctx.lastRegister.text)

        val totalRegisters = lastRegister.physicalAddress - startRegister.physicalAddress + 1
        if (totalRegisters <= 0) {
            throw ModbusSchemaParseException("Invalid register range specified: start=$startRegister ; last=$lastRegister")
        }

        return RegistersModbusExpression((0 until totalRegisters).map { count -> startRegister.increment(count) }.toList())
    }

    override fun visitRegisterSwapEndian(ctx: RegisterSwapEndianContext): Expression =
        SwapEndian(visitRegistersExpression(ctx.registers))

    override fun visitRegisterSwapBytes(ctx: RegisterSwapBytesContext): Expression =
        SwapBytes(visitRegistersExpression(ctx.registers))

    override fun visitStringUtf8(ctx: StringUtf8Context): Expression =
        UTF8String(visitRegistersExpression(ctx.registers))

    override fun visitStringHex(ctx: StringHexContext): Expression =
        HexString(visitRegistersExpression(ctx.registers))

    override fun visitStringConstant(ctx: StringConstantContext): Expression =
        StringConstant(ctx.STRING().text)

    override fun visitStringField(ctx: StringFieldContext): Expression =
        StringField(ctx.FIELDNAME().text)

    override fun visitStringNumber(ctx: StringNumberContext): Expression =
        StringFromNumber(visitNumericalExpression(ctx.number()))

    override fun visitStringConcat(ctx: StringConcatContext): Expression {
        val expressions = mutableListOf<StringExpression>()
        for (string in ctx.stringFragments()) {
            expressions.add(visitStringExpression(string))
        }
        return StringConcat(expressions)
    }

    private fun notImplementedToStringList(notImplementedContexts: List<NotImplementedContext>?): List<String> {
        var notImplemented = emptyList<String>()
        if (!notImplementedContexts.isNullOrEmpty()) {
            notImplemented = notImplementedContexts.map { it.text }
        }
        return notImplemented
    }

    override fun visitStringEui48(ctx: StringEui48Context): Expression =
        Eui48String(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitStringIPv4Addr(ctx: StringIPv4AddrContext): Expression =
        IPv4AddrString(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitStringIPv6Addr(ctx: StringIPv6AddrContext): Expression =
        IPv6AddrString(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitStringEnum(ctx: StringEnumContext): Expression {
        val registers = visitRegistersExpression(ctx.registers)
        val enumMappings: MutableMap<Long, String> = TreeMap()
        for (enumMappingContext in ctx.mapping()) {
            enumMappings[enumMappingContext.key.text.toLong()] = enumMappingContext.value.text
        }
        return EnumString(registers, notImplementedToStringList(ctx.notImplemented()), enumMappings)
    }

    override fun visitStringListBitSet(ctx: StringListBitSetContext): Expression {
        val registers = visitRegistersExpression(ctx.registers)
        val bitsetMappings: MutableMap<Int, String> = TreeMap()
        for (bitsetMappingContext in ctx.mapping()) {
            bitsetMappings[bitsetMappingContext.key.text.toInt()] = bitsetMappingContext.value.text
        }
        return BitsetStringList(registers, notImplementedToStringList(ctx.notImplemented()), bitsetMappings)
    }

    override fun visitLoadInt16(ctx: LoadInt16Context): Expression =
        IntegerSigned16(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadInt32(ctx: LoadInt32Context): Expression =
        IntegerSigned32(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadInt64(ctx: LoadInt64Context): Expression =
        IntegerSigned64(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadUInt16(ctx: LoadUInt16Context): Expression =
        IntegerUnsigned16(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadUInt32(ctx: LoadUInt32Context): Expression =
        IntegerUnsigned32(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadUInt64(ctx: LoadUInt64Context): Expression =
        IntegerUnsigned64(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadIeee754_32(ctx: LoadIeee754_32Context): Expression =
        IEEE754Float32(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitLoadIeee754_64(ctx: LoadIeee754_64Context): Expression =
        IEEE754Float64(visitRegistersExpression(ctx.registers), notImplementedToStringList(ctx.notImplemented()))

    override fun visitNumberField(ctx: NumberFieldContext): Expression = NumericalField(ctx.text)

    override fun visitExtraBraces(ctx: ExtraBracesContext): Expression = super.visit(ctx.number())!!

    // ------------------------------------------------------------
    // Operations
    override fun visitPower(ctx: PowerContext): Expression {
        val base = visitNumericalExpression(ctx.base)
        val exponent = visitNumericalExpression(ctx.exponent)
        return Power(base, exponent)
    }

    override fun visitImplicitMultiply(ctx: ImplicitMultiplyContext): Expression {
        val left = visitNumericalExpression(ctx.left)
        val middle = visitNumericalExpression(ctx.middle)
        return Multiply(left, middle)
    }

    override fun visitMultiplyDivide(ctx: MultiplyDivideContext): Expression {
        val left = visitNumericalExpression(ctx.left)
        val right = visitNumericalExpression(ctx.right)
        if (ctx.DIVIDE() != null) {
            return Divide(left, right)
        }
        return Multiply(left, right)
    }

    override fun visitAddSubtract(ctx: AddSubtractContext): Expression {
        val left = visitNumericalExpression(ctx.left)
        val right = visitNumericalExpression(ctx.right)
        if (ctx.ADD() != null) {
            return Add(left, right)
        }
        return Subtract(left, right)
    }

    companion object {
        @JvmStatic
        fun parse(expression: String): Expression {
            val errorListener: ANTLRErrorListener =
                ModbusAntlrErrorListener(expression)

            val input = CharStreams.fromString(expression)
            val lexer = FieldExpressionsLexer(input)

            lexer.removeErrorListeners()
            lexer.addErrorListener(errorListener)

            val tokens = CommonTokenStream(lexer)
            val parser = FieldExpressionsParser(tokens)

            parser.removeErrorListeners()
            parser.addErrorListener(errorListener)
            parser.errorHandler = ModbusParserErrorStrategy()

            val expressionContext: ParserRuleContext = parser.expression()
            return ExpressionParser().visit(expressionContext)!!
        }
    }
}

