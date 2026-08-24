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

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import nl.basjes.modbus.device.j2mod.ModbusDeviceJ2Mod
import nl.basjes.modbus.device.test.ModbusTestServer
import nl.basjes.modbus.schema.fetcher.ModbusQuery.Status.ERROR
import nl.basjes.modbus.schema.fetcher.ModbusQuery.Status.SUCCESS
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Thread.sleep
import kotlin.test.Test
import kotlin.test.assertTrue

class TestModbusTestServer {

    private val log = LoggerFactory.getLogger(TestModbusTestServer::class.java)

    fun String.openAsStream(): InputStream {
        val fileStream = ClassLoader.getSystemClassLoader().getResourceAsStream(this)
        if (fileStream != null) {
            return fileStream
        }
        return FileInputStream(this)
    }

    fun InputStream.readAsString(): String  = this.bufferedReader().use { it.readText() }

    fun String.readFileNameToString(): String  = this.openAsStream().readAsString()


    @Test
    fun testModbusTestServer() {
        // We load a specific schema device which has the schema, test values and expected results.
        log.info("Schema: Loading schema")
        val schemaDevice =
            "src/test/resources/TestSchemas/ThermiaGenesis101213.yaml".readFileNameToString().toSchemaDevice()

        val modbusHost = "localhost"
        val modbusUnit = 1
        for (testScenario in schemaDevice.tests) {
            log.info("------------------------------------------")
            // We create a test server and load all the raw blocks from the testcase in there
            log.info("Creating Modbus TCP server")
            val testServer = ModbusTestServer(modbusUnit)

            log.info("Loading registers and discretes for test scenario {}.", testScenario.name)
            testServer.loadModbusBlocks(testScenario.modbusBlocks)

            val modbusPort = testServer.port

            log.info("Connecting to Modbus TCP device on:")
            log.info("- Hostname: {}", modbusHost)
            log.info("- TCP Port: {}", modbusPort)
            log.info("- UnitID  : {}", modbusUnit)

            log.info("Modbus: Connecting...")
            val modbusMaster = ModbusTCPMaster(modbusHost, modbusPort)
            modbusMaster.connect()
            val modbusDevice = ModbusDeviceJ2Mod(modbusMaster, modbusUnit)
            log.info("Modbus: Connected")

            schemaDevice.clearModbusBlocks()
            schemaDevice.connect(modbusDevice)
            log.info("Schema: Connected")

            schemaDevice.needAll()

            log.info("Schema: Getting all values via modbus TCP")
            var modbusQueries = schemaDevice.update()
            log.info("Schema: Needed ${modbusQueries.size} modbus queries for the result " +
                "(Error: ${modbusQueries.filter { it.status == ERROR }.size}, " +
                 "Success: ${modbusQueries.filter { it.status == SUCCESS }.size})")

            sleep(1)
            modbusQueries = schemaDevice.update(0)
            log.info("Schema: Needed ${modbusQueries.size} modbus queries for the result " +
                "(Error: ${modbusQueries.filter { it.status == ERROR }.size}, " +
                "Success: ${modbusQueries.filter { it.status == SUCCESS }.size})")

//            modbusQueries.forEach {
//                log.info("ModbusQuery { ${it.start} # ${it.count} } (${it.fields.size} fields): ${it.status}") }

            log.info("Schema: Verify results")
            val testScenarioResults = testScenario.verify(schemaDevice, true)
            assertTrue(testScenarioResults.logResults())
        }
        log.info("------------------------------------------")
    }
}
