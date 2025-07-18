/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2025 Niels Basjes
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

enum class LogColor(private val code: String) {
    //Color end string, color reset
    RESET("\u001b[0m"),

    // Regular Colors. Normal color, no bold, background color etc.
    BLACK("\u001b[0;30m"),
    RED("\u001b[0;31m"),
    GREEN("\u001b[0;32m"),
    YELLOW("\u001b[0;33m"),
    BLUE("\u001b[0;34m"),
    MAGENTA("\u001b[0;35m"),
    CYAN("\u001b[0;36m"),
    WHITE("\u001b[0;37m"),

    // Bold
    BLACK_BOLD("\u001b[1;30m"),
    RED_BOLD("\u001b[1;31m"),
    GREEN_BOLD("\u001b[1;32m"),
    YELLOW_BOLD("\u001b[1;33m"),

    BLUE_BOLD("\u001b[1;34m"),
    MAGENTA_BOLD("\u001b[1;35m"),
    CYAN_BOLD("\u001b[1;36m"),
    WHITE_BOLD("\u001b[1;37m"),

    // Underline
    BLACK_UNDERLINED("\u001b[4;30m"),
    RED_UNDERLINED("\u001b[4;31m"),
    GREEN_UNDERLINED("\u001b[4;32m"),
    YELLOW_UNDERLINED("\u001b[4;33m"),
    BLUE_UNDERLINED("\u001b[4;34m"),
    MAGENTA_UNDERLINED("\u001b[4;35m"),
    CYAN_UNDERLINED("\u001b[4;36m"),
    WHITE_UNDERLINED("\u001b[4;37m"),

    // Background
    BLACK_BACKGROUND("\u001b[40m"),
    RED_BACKGROUND("\u001b[41m"),
    GREEN_BACKGROUND("\u001b[42m"),
    YELLOW_BACKGROUND("\u001b[43m"),
    BLUE_BACKGROUND("\u001b[44m"),
    MAGENTA_BACKGROUND("\u001b[45m"),
    CYAN_BACKGROUND("\u001b[46m"),
    WHITE_BACKGROUND("\u001b[47m"),

    // High Intensity
    BLACK_BRIGHT("\u001b[0;90m"),
    RED_BRIGHT("\u001b[0;91m"),
    GREEN_BRIGHT("\u001b[0;92m"),
    YELLOW_BRIGHT("\u001b[0;93m"),
    BLUE_BRIGHT("\u001b[0;94m"),
    MAGENTA_BRIGHT("\u001b[0;95m"),
    CYAN_BRIGHT("\u001b[0;96m"),
    WHITE_BRIGHT("\u001b[0;97m"),

    // Bold High Intensity
    BLACK_BOLD_BRIGHT("\u001b[1;90m"),
    RED_BOLD_BRIGHT("\u001b[1;91m"),
    GREEN_BOLD_BRIGHT("\u001b[1;92m"),
    YELLOW_BOLD_BRIGHT("\u001b[1;93m"),
    BLUE_BOLD_BRIGHT("\u001b[1;94m"),
    MAGENTA_BOLD_BRIGHT("\u001b[1;95m"),
    CYAN_BOLD_BRIGHT("\u001b[1;96m"),
    WHITE_BOLD_BRIGHT("\u001b[1;97m"),

    // High Intensity backgrounds
    BLACK_BACKGROUND_BRIGHT("\u001b[0;100m"),
    RED_BACKGROUND_BRIGHT("\u001b[0;101m"),
    GREEN_BACKGROUND_BRIGHT("\u001b[0;102m"),
    YELLOW_BACKGROUND_BRIGHT("\u001b[0;103m"),
    BLUE_BACKGROUND_BRIGHT("\u001b[0;104m"),
    MAGENTA_BACKGROUND_BRIGHT("\u001b[0;105m"),
    CYAN_BACKGROUND_BRIGHT("\u001b[0;106m"),
    WHITE_BACKGROUND_BRIGHT("\u001b[0;107m");

    override fun toString(): String {
        return code
    }
}
