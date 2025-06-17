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
package nl.basjes.modbus.device.exception

/**
 * The generic top level exception
 */
open class ModbusException
    @JvmOverloads
    constructor(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

/**
 * Thrown if the API is used in an invalid way (i.e. developer error)
 */
open class ModbusApiException
    @JvmOverloads
    constructor(
        message: String,
        cause: Throwable? = null,
    ) : ModbusException(message, cause)

/**
 * Thrown if an invalid AddressClass was used (i.e. developer error)
 */
open class ModbusIllegalAddressClassException
@JvmOverloads
constructor(
    message: String,
    cause: Throwable? = null,
) : ModbusApiException(message, cause)

