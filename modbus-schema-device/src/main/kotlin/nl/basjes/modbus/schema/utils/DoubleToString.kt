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

import java.util.regex.Pattern
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

object DoubleToString {
    private val ZERO_STRIPPER_1: Pattern = Pattern.compile("(\\.([1-9]|[0-9]+[1-9]+?))0+$")
    private val ZERO_STRIPPER_2: Pattern = Pattern.compile("\\.0+$")

    private fun format(
        value: Any,
        maxDigits: Long,
    ): String {
        val doubleValue =
            if (value is Float) {
                value.toDouble()
            } else {
                value as Double
            }
        val log10 = log10(doubleValue)

        var fDigits: Long = 1
        if (log10.isFinite()) {
            fDigits = floor(log10).toLong()
            // Can still be anything like -10, 0 +10
        }
        val digits: Long
        val decimals: Long
        if (fDigits < 1) {
            digits = 1
            decimals = maxDigits - fDigits
        } else {
            digits = min(maxDigits.toDouble(), fDigits.toDouble()).toLong()
            decimals = max((maxDigits - digits).toDouble(), 1.0).toLong()
        }
        val format = String.format("%%%d.%df", digits, decimals)
        var result = String.format(format, value)
        result = ZERO_STRIPPER_1.matcher(result).replaceAll("$1")
        result = ZERO_STRIPPER_2.matcher(result).replaceAll("")

        //        System.out.println(String.format("%s for %f (log10=%d) digits=%d decimals=%d", format, doubleValue, fDigits, digits, decimals));
        return result
    }

    fun of(value: Float): String {
        if (!value.isFinite() || value.isNaN()) {
            return value.toString()
        }
        // Float (IEEE 754 32 bits) has 6-9 significant decimal digits precision.
        // https://en.wikipedia.org/wiki/Single-precision_floating-point_format
        val maxDigits: Long = 6 // One less to drop rounding errors
        return format(value, maxDigits)
    }

    fun of(value: Double): String {
        // Float (IEEE 754 64 bits) has 15 to 17 significant decimal digits precision.
        // https://en.wikipedia.org/wiki/Double-precision_floating-point_format
        val maxDigits: Long = 10
        return of(value, maxDigits)
    }

    fun of(
        value: Double,
        maxDigits: Long,
    ): String {
        if (!value.isFinite() || value.isNaN()) {
            return value.toString()
        }
        return format(value, maxDigits)
    }
}
