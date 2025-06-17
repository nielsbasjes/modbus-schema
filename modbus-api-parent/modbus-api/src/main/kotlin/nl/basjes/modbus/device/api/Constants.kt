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

package nl.basjes.modbus.device.api

/** 1900-01-01T00:00:00.000Z */
internal const val EPOCH_1900 = -2208988800000

/** 1888-08-08T08:08:08.888Z */
internal const val EPOCH_1888 = -2568642711112

/** All timestamps before 1900 are considered to be invalid */
internal const val NEVER_VALID_BEFORE  = EPOCH_1900

/** Use this timestamp in case of a read error */
internal const val READERROR_TIMESTAMP = EPOCH_1888
