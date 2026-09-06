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
import nl.basjes.modbus.device.utils.toDiscretes
import nl.basjes.modbus.device.utils.toRegisters
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.test.assertTrue
import kotlin.time.Clock.System.now

private val LOG: Logger = LogManager.getLogger()

/**
 * This is a testing Modbus Device that has a few dummy values that can be automatically updated with time.
 */
class ModbusSchemaTestSlave(
    unitId: Int = 42,
    val callback: ((updates: Int) -> Unit)? = null,
) {
    val modbusTestSlave = ModbusTestSlave(unitId)

    /**
     * The config needed to connect to this slave.
     */
    val tcpConfig = modbusTestSlave.tcpConfig

    // Create an executor with a thread pool
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    val periodInSeconds: Int = 10 // seconds

    var updates: Int = 0
    val updatesAddress = Address.of(AddressClass.INPUT_REGISTER, 10)

    var period: Double = 0.0
    val periodAddress = Address.of(AddressClass.INPUT_REGISTER, 100)

    var sine: Double = 0.0
    val sineAddress = Address.of(AddressClass.HOLDING_REGISTER, 20)
    var sinIsPositive: Boolean = false
    val sinIsPositiveAddress = Address.of(AddressClass.COIL, 30)
    var periodsIsEven: Boolean = false
    val periodsIsEvenAddress = Address.of(AddressClass.DISCRETE_INPUT, 40)

    fun recalculateValuesAndUpdateModbusRegisters() {
        val time = now().toEpochMilliseconds()
        updates++
        period = (time/(periodInSeconds*1000.0))
        sine = sin(period*(2*PI))
        sinIsPositive = sine >= 0
        periodsIsEven = floor(period).toLong() % 2 == 0L

        modbusTestSlave.addRegisters(updates        .toRegisters(updatesAddress))
        modbusTestSlave.addRegisters(period         .toRegisters(periodAddress))
        modbusTestSlave.addRegisters(sine           .toRegisters(sineAddress))
        modbusTestSlave.addDiscretes(sinIsPositive  .toDiscretes(sinIsPositiveAddress))
        modbusTestSlave.addDiscretes(periodsIsEven  .toDiscretes(periodsIsEvenAddress))
        if (callback != null) { callback(updates) }
//        LOG.info("Update: {}", updates)
    }

    val modbusSchemaYaml = $$"""
    # $schema: https://modbus.basjes.nl/v2/ModbusSchema.json
    description: 'The schema of the test device'
    schemaFeatureLevel: 2

    blocks:
      - id:          'Values'
        description: 'All the values in this fake device'

        fields:
          - id:          'Updates'
            description: 'How many updates of the values have been done'
            expression:  'int32($$updatesAddress#2)'

          - id:          'Periods'
            description: 'The period (2 PI every $$periodInSeconds seconds)'
            expression:  'ieee754_64($$periodAddress#4)'

          - id:          'Sin'
            description: 'The sin(time) with a period of $$periodInSeconds seconds'
            expression:  'ieee754_64($$sineAddress#4)'

          - id:          'SinIsPositive'
            description: 'The Sin value is positive'
            expression:  'boolean($$sinIsPositiveAddress; ''Positive''; ''Negative'')'

          - id:          'PeriodIsEven'
            description: 'Is the period even or odd. Changes every $$periodInSeconds seconds'
            expression:  'boolean($$periodsIsEvenAddress; ''Even''; ''Odd'')'

    tests:
      - id:          'TestAt_2026_09_05_21_45_38'
        description: 'Test generated from device data at 2026-09-05T21:45:38.303Z'
        input:
          - firstAddress: 'c:00030'
            rawValues: 0
          - firstAddress: 'di:00040'
            rawValues: 0
          - firstAddress: 'ir:00010'
            rawValues: |-
              0000 07D5 ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
              41A5 5282 B3A8 F5C3
          - firstAddress: 'hr:00020'
            rawValues: BFEC 0AB4 5F73 33B2

        blocks:
          - id:          'Values'
            expected:
              'Updates':                                            [ '2005' ]
              'Periods':                                            [ '178864473.830' ]
              'Sin':                                                [ '-0.876' ]
              'SinIsPositive':                                      [ 'Positive' ]
              'PeriodIsEven':                                       [ 'Even' ]

    """.trimIndent()

    fun start() {
        println("Starting test server auto updates.")
        // Immediately make sure there is always a value or sometimes tests fail
        recalculateValuesAndUpdateModbusRegisters()

        scheduler.scheduleAtFixedRate({
            recalculateValuesAndUpdateModbusRegisters()
        }, 0, 10, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        scheduler.shutdown()
        println("Waiting for running tasks to finish...")
        val finishedCleanly = scheduler.awaitTermination(10, TimeUnit.SECONDS)

        println("Terminated ${if (finishedCleanly) "cleanly" else "after a timeout"}.")
        assertTrue(finishedCleanly, "Modbus Test Slave did not terminate cleanly." )
    }

}
