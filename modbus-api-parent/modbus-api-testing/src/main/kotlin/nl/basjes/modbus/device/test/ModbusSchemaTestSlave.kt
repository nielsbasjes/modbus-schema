/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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
package nl.basjes.modbus.device.test

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.utils.toRegisters
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.time.Clock.System.now


class ModbusSchemaTestSlave(
    unitId: Int = 42,
) {
    val modbusTestSlave = ModbusTestSlave(unitId)

    // Create an executor with a thread pool
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    val periodInSeconds: Double = 10.0 // seconds

    var updates: Int = 0
    val updatesAddress = Address.of(AddressClass.INPUT_REGISTER, 10)
    var sine: Double = 0.0
    val sineAddress = Address.of(AddressClass.INPUT_REGISTER, 20)
    var sinIsPositive: Boolean = false
    val sinIsPositiveAddress = Address.of(AddressClass.DISCRETE_INPUT, 30)
    var periodsIsEven: Boolean = false
    val periodsIsEvenAddress = Address.of(AddressClass.DISCRETE_INPUT, 40)

    fun calculateNewValues() {
        val time = now().toEpochMilliseconds()
        val period = (time/(periodInSeconds*1000.0))
        sine = sin(period*(2*PI))
        updates++
        sinIsPositive = sine >= 0
        periodsIsEven = floor(period).toInt() % 2 == 0
    }

    fun updateModbusRegisters() {
        modbusTestSlave.addRegisters(updates.toRegisters(updatesAddress))
        modbusTestSlave.addRegisters(sine.toRegisters(sineAddress))
//        modbusTestServer.addDiscretes() // TODO: Implement and make yaml schema
//        modbusTestServer.addDiscretes()
    }

    fun start() {
        println("Starting test server")
        scheduler.scheduleAtFixedRate({
            calculateNewValues()
            updateModbusRegisters()
        }, 0, 10, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        scheduler.shutdown()
        println("Waiting for running tasks to finish...")
        val finishedCleanly = scheduler.awaitTermination(10, TimeUnit.SECONDS)

        println("Terminated ${if (finishedCleanly) "cleanly" else "after a timeout"}.")
    }

}
