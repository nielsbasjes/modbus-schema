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
package nl.basjes.modbus.schema.utils

object CodeGeneration {
    private val DROP_LEADING_TRAILING: List<Int> =
        listOf(
            '_'.code,
            '-'.code,
            ' '.code,
        )

    /**
     * In many cases the name or identifier of a thing needs to be available in a form that it can be used as a
     * type name or variable name.
     * @param name The name that must be cleaned
     * @param firstUppercase if the first character must be an uppercase or lowercase
     * @return A cleaned version of the provided input.
     */
    @JvmStatic
    fun convertToCodeCompliantName(
        name: String,
        firstUppercase: Boolean,
    ): String {
        val finalName = StringBuilder(name.length)

        var sawSeparator = false
        var firstLetter = true
        var firstWord = true
        var allInFirstWordAreUppercase = true
        for (codepoint in name.chars().toArray()) {
            if (firstLetter) {
                if (
                    !Character.isUnicodeIdentifierStart(codepoint) ||
                    // Although Java does allow currency symbols as identifiers, we choose to drop them.
                    Character.getType(codepoint) == Character.CURRENCY_SYMBOL.toInt() ||
                    DROP_LEADING_TRAILING.contains(codepoint)
                ) {
                    continue
                }
                firstLetter = false
                if (firstUppercase) {
                    finalName.append(Character.toString(Character.toUpperCase(codepoint)))
                } else {
                    finalName.append(Character.toString(Character.toLowerCase(codepoint)))
                }
                continue
            }

            if (Character.isUnicodeIdentifierPart(codepoint) &&
                // Although Java does allow currency symbols as identifiers, we choose to drop them.
                Character.getType(codepoint) != Character.CURRENCY_SYMBOL.toInt()
            ) {
                if (firstWord && allInFirstWordAreUppercase) {
                    allInFirstWordAreUppercase = Character.isUpperCase(codepoint)
                }
            } else {
                sawSeparator = true
                firstWord = false

                if (allInFirstWordAreUppercase && finalName.length <= 3) {
                    if (!firstUppercase) {
                        val oldFinalName = finalName.toString()
                        finalName.clear()
                        finalName.append(oldFinalName.lowercase())
                    }
                }

                continue
            }

            if (sawSeparator) {
                sawSeparator = false
                finalName.append(Character.toString(Character.toUpperCase(codepoint)))
            } else {
                finalName.append(Character.toString(codepoint))
            }
        }

        var result = finalName.toString()

        if (result.isEmpty()) {
            return result
        }

        while (DROP_LEADING_TRAILING.contains(result.codePointAt(result.length - 1))) {
            result = result.substring(0, result.length - 1)
        }

        return result
    }
}
