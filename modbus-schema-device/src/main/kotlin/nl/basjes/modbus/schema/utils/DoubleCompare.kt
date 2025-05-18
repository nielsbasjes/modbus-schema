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

import kotlin.math.abs
import kotlin.math.log10

/**
 * A dynamic implementation of comparing doubles and automatically take
 * the numerical precision into account.
 */
object DoubleCompare {

    //    private static final Pattern DOUBLE_HEX_PARSE = Pattern.compile("^(-?)0x1\\.([0-9a-fA-F]+)p([-0-9]+)$");
    //
    //    public static boolean closeEnough(double one, double two) {
    //        if (Double.isFinite(one)) {
    //            String hexString1 = Double.toHexString(one);
    //            String hexString2 = Double.toHexString(two);
    //
    //            Matcher matcher1 = DOUBLE_HEX_PARSE.matcher(hexString1);
    //            Matcher matcher2 = DOUBLE_HEX_PARSE.matcher(hexString2);
    //            if (!matcher1.matches() || !matcher2.matches()) {
    //                // Should not happen
    //                return false;
    //            }
    //
    //            String hexSign1  = matcher1.group(1);
    //            String hexSign2  = matcher2.group(1);
    //            if (!hexSign1.equals(hexSign2)) {
    //                return false;
    //            }
    //
    //            String hexExponent1  = matcher1.group(3);
    //            String hexExponent2  = matcher2.group(3);
    //
    //            if (!hexExponent1.equals(hexExponent2)) {
    //                return false;
    //            }
    //
    //            String hexBase1      = matcher1.group(2);
    //            String hexBase2      = matcher2.group(2);
    //
    //            // 4 = 2^-(8*5) = 2^-40 = 9.094947e-13
    //            return (hexBase1.startsWith(hexBase2.substring(0, 5)));
    //        }
    //
    //        // Special case NaN
    //        if (Double.isNaN(one) && Double.isNaN(two)) {
    //            return true;
    //        }
    //        return one == two;
    //    }
    private const val CLOSE_ENOUGH_LOG10_DIFFERENCE = 0.00000001

    @JvmOverloads
    fun closeEnough(
        one: Double,
        two: Double,
        closeEnoughLog10Difference: Double = CLOSE_ENOUGH_LOG10_DIFFERENCE,
    ): Boolean {
        if (one.isFinite() && two.isFinite()) {
            val oneLog10 = log10(one)
            val twoLog10 = log10(two)
            val diff = abs(oneLog10 - twoLog10)
            //            LOG.warn("Close Enough ({}, {}) -> Log10 ~~ {} {} -- Delta = {}, MaxDelta = {} --> {}", one, two, oneLog10, twoLog10, diff, closeEnoughLog10Difference, diff < closeEnoughLog10Difference);
            return (diff < closeEnoughLog10Difference)
        }
        if (one.isNaN() && two.isNaN()) {
            return true
        }
        return one == two
    }
}
