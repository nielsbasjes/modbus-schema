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
package nl.basjes.modbus.schema

import nl.basjes.modbus.schema.exceptions.ModbusSchemaParseException
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestLoader {

    @Test
    fun testGoodSchema() {
        val schema = """
        description: 'A device'
        schemaFeatureLevel: 1
        blocks:
        - id: 'Main'
          description:  'The only block of registers'
          fields:
          - id: 'MyFloat'
            description:  'A Float 32'
            expression: 'ieee754_32(hr:00000 # 2)'
            unit: 'Foo'

          - id: 'MyDouble'
            description:  'A Float 64'
            expression: 'ieee754_64(hr:00002 # 4)'
            unit: 'Bar'

        tests:
        - id: 'Normal values'
          description: 'Normal values'
          input:
          - firstRegisterAddress: 'hr:0'
            registers: >-
              449A 5225
              4093 4A45 84FC D47C
          blocks:
          - id: 'Main'
            expected:
              MyFloat            : [ 1234.567 ]
              MyDouble           : [ 1234.568 ]
                """.trimIndent()

        val device =  schema.toSchemaDevice()
        assertNotNull(device)
        assertTrue(device.initializeAndVerify())
    }

    @Test
    fun testBadSchema() {
        val schema = """
        description: 'A device'
        schemaFeatureLevel: 1
        blocks:
        - id: 'Main'
          description:  'The only block of registers'
          fields:
          - id: 'MyFloat'
            description:  'A Float 32'
            expression: 'ieee754_32(hr:00000 # 3)'
            unit: 'Foo'

          - id: 'MyDouble'
            description:  'A Float 64'
            expression: 'ieee754_64(hr:00002 # 3)'
            unit: 'Bar'

        tests:
        - id: 'Normal values'
          description: 'Normal values'
          input:
          - firstRegisterAddress: 'hr:0'
            registers: >-
              449A 5225
              4093 4A45 84FC D47C
          blocks:
          - id: 'Main'
            expected:
              MyFloat            : [ 111.111 ]
              MyDouble           : [ 111.111 ]
                """.trimIndent()
        try {
            schema.toSchemaDevice()
        }
        catch (ex: ModbusSchemaParseException) {
            println(ex.message)
            return // This is good
        }
        catch (ex: Exception) {
            error("Wrong exception was thrown: ${ex.javaClass.canonicalName} : ${ex.message}")
        }
        error("No exception was thrown")
    }

    @Test
    fun testBadTestResult() {
        val schema = """
        description: 'A device'
        schemaFeatureLevel: 1
        blocks:
        - id: 'Main'
          description:  'The only block of registers'
          fields:
          - id: 'MyFloat'
            description:  'A Float 32'
            expression: 'ieee754_32(hr:00000 # 2)'
            unit: 'Foo'

          - id: 'MyDouble'
            description:  'A Float 64'
            expression: 'ieee754_64(hr:00002 # 4)'
            unit: 'Bar'

        tests:
        - id: 'Normal values'
          description: 'Normal values'
          input:
          - firstRegisterAddress: 'hr:0'
            registers: >-
              449A 5225
              4093 4A45 84FC D47C
          blocks:
          - id: 'Main'
            expected:
              MyFloat            : [ 111.111 ]
              MyDouble           : [ 111.111 ]
                """.trimIndent()

        try {
            schema.toSchemaDevice()
        }
        catch (ex: ModbusSchemaParseException) {
            println(ex.message)
            return // This is good
        }
        catch (ex: Exception) {
            error("Wrong exception was thrown: ${ex.javaClass.canonicalName} : ${ex.message}")
        }
        error("No exception was thrown")
    }

}
