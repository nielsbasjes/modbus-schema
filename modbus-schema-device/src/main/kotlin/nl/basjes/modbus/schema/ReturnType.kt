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
package nl.basjes.modbus.schema

enum class ReturnType(
    val enumName: String,
) {
    /** Used to indicate we do not know the actual return type yet */
    UNKNOWN("Unknown"),
    BOOLEAN("boolean"),
    LONG("long"),
    DOUBLE("double"),
    STRING("string"),
    STRINGLIST("stringList"),
    ;

    override fun toString(): String = this.enumName

    fun value(): String = this.enumName

    companion object {
        private val CONSTANTS: MutableMap<String, ReturnType> = HashMap()

        init {
            for (c in entries) {
                CONSTANTS[c.enumName] = c
            }
        }

        fun fromValue(value: String): ReturnType {
            val constant = CONSTANTS[value]
            requireNotNull(constant) { value }
            return constant
        }
    }
}
