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
package nl.basjes.modbus.schema.expression.parser

import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.InputMismatchException
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token

class ModbusParserErrorStrategy : DefaultErrorStrategy() {

    override fun recover(
        recognizer: Parser,
        e: RecognitionException,
    ) {
        var context = recognizer.context
        while (context != null) {
            context.exception = e
            context = context.getParent()
        }

        throw ModbusSchemaParseException("Parsing the expression failed with ${e.message}.")
    }

    @Throws(RecognitionException::class)
    override fun recoverInline(recognizer: Parser): Token {
        val e = InputMismatchException(recognizer)

        var context = recognizer.context
        while (context != null) {
            context.exception = e
            context = context.getParent()
        }

        throw ModbusSchemaParseException("Parsing the expression failed with ${e.message}.")
    }

    override fun sync(recognizer: Parser) {
    }
}
