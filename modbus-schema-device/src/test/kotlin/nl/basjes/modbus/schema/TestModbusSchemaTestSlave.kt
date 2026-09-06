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
package nl.basjes.modbus.schema

import nl.basjes.modbus.device.j2mod.toModbusDeviceJ2Mod
import nl.basjes.modbus.device.test.ModbusSchemaTestSlave
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.Thread.sleep
import kotlin.test.Test
import kotlin.test.assertTrue

private val LOG: Logger = LogManager.getLogger()

class TestModbusSchemaTestSlave {
    @Test
    fun doIt() {
        val slave = ModbusSchemaTestSlave()// callback = { updates ->  LOG.warn("UPDATE: $updates") })

        LOG.info("Connecting to slave using ${slave.tcpConfig}")

        val master = slave.tcpConfig.toModbusDeviceJ2Mod()

        require(master.isConnected()) { "Somehow there was no connection with the slave device."}

        val schemaDevice = slave.modbusSchemaYaml.toSchemaDevice()
        schemaDevice.connect(master)
        schemaDevice.needAll()
        slave.start()

        (0..19).forEach { _ ->
            val modbusQueries = schemaDevice.update()
            val updatesField        = schemaDevice["Values"]["Updates"]       ?: throw IllegalStateException("This should not happen")
            val periodsField        = schemaDevice["Values"]["Periods"]       ?: throw IllegalStateException("This should not happen")
            val sinField            = schemaDevice["Values"]["Sin"]           ?: throw IllegalStateException("This should not happen")
            val sinIsPositiveField  = schemaDevice["Values"]["SinIsPositive"] ?: throw IllegalStateException("This should not happen")
            val periodIsEvenField   = schemaDevice["Values"]["PeriodIsEven"]  ?: throw IllegalStateException("This should not happen")

            val updates             = updatesField.longValue
            val periods             = periodsField.doubleValue
            val sin                 = sinField.doubleValue
            val sinIsPositive       = sinIsPositiveField.value
            val periodIsEven        = periodIsEvenField.value

            if (updates == null || periods == null || sin == null || sinIsPositive == null || periodIsEven == null) {
                LOG.info("Modbus Queries: \n${modbusQueries.joinToString("\n")}")
            }

            requireNotNull(updates)       { "The retrieved value for updates was null."}
            requireNotNull(periods)       { "The retrieved value for periods was null."}
            requireNotNull(sin)           { "The retrieved value for sin was null."}
            requireNotNull(sinIsPositive) { "The retrieved value for sinIsPositive was null."}
            requireNotNull(periodIsEven)  { "The retrieved value for periodIsEven was null."}

            fun Double  .format() = "%7.4f".format(this)
            fun Long    .format() = "%6d".format(this)

            LOG.info(
             "Updates = ${          updates       .format()}, " +
             "Periods = ${          periods       .format()}, " +
             "Sin = ${              sin           .format()}, " +
             "SinIsPositive = $sinIsPositive, " +
             "PeriodIsEven = $periodIsEven")
            sleep(200)
        }

        schemaDevice.createTestsUsingCurrentRealData()

        val results = schemaDevice.verifyProvidedTests()
        results.forEach { result -> LOG.info("\n{}", result.toTable()) }
        assertTrue(results.allPassed)

        println(schemaDevice.toYaml())

        slave.stop()
    }
}
