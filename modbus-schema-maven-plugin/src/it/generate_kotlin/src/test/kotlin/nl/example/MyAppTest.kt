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
package nl.example

import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.schema.Field
import nl.example.modbus.MyNewDevice
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class MyAppTest {

    val modbusDevice = MockedModbusDevice
        .builder()
//            .withLogging()
        .withRegisters(
            AddressClass.HOLDING_REGISTER,
            0,
            """
                449A 5225
                4093 4A45 84FC D47C

                100F
                100F 100F
                100F 100F 100F
                100F 100F 100F 100F
                0001

                0102 0304 0506 0708

                CFC7
                3039
                B669 FD2E
                4996 02d2
                EEDD EF0B 8216 7EEB
                1122 10F4 7DE9 8115
                0102 0304
                0001 0203 0405 0607 0809 1011 1213 1415
                4E69 656C 7320 4261 736A 6573
                """.trimIndent()
        ).build()

    private fun verifyAllFieldValues(
        myFloat:           Field,
        myDouble:          Field,
        myBitset1:         Field,
        myBitset2:         Field,
        myBitset3:         Field,
        myBitset4:         Field,
        myEnum:            Field,
        myEUI48:           Field,
        myShort:           Field,
        myUnsignedShort:   Field,
        myInteger:         Field,
        myUnsignedInteger: Field,
        myLong:            Field,
        myUnsignedLong:    Field,
        myIPv4:            Field,
        myIPv6:            Field,
        myName:            Field,
        myNameHex:         Field,
    ) {
        assertEquals(1234.567                                                           , myFloat           .doubleValue!!, 0.001)
        assertEquals(1234.568                                                           , myDouble          .doubleValue!!, 0.001)
        assertEquals(listOf("Zero", "One", "Two", "Bit 3", "Bit 12"),                                                                                                            myBitset1 .stringListValue)
        assertEquals(listOf("Zero", "One", "Two", "Bit 3", "Bit 12", "Sixteen", "Seventeen", "Eighteen", "Bit 19", "Bit 28"),                                                    myBitset2 .stringListValue)
        assertEquals(listOf("0", "1", "2", "Bit 3", "Bit 12", "16", "17", "18", "Bit 19", "Bit 28", "32", "33", "34", "Bit 35", "Bit 44"),                                       myBitset3 .stringListValue)
        assertEquals(listOf("0", "1", "2", "Bit 3", "Bit 12", "16", "17", "18", "Bit 19", "Bit 28", "32", "33", "34", "Bit 35", "Bit 44", "48", "49", "50", "Bit 51", "Bit 60"), myBitset4 .stringListValue)
        assertEquals("Manual"                                                           , myEnum            .stringValue)
        assertEquals("01:02:03:04:05:06"                                                , myEUI48           .stringValue)
        assertEquals(-12345                                                             , myShort           .longValue)
        assertEquals( 12345                                                             , myUnsignedShort   .longValue)
        assertEquals(-1234567890                                                        , myInteger         .longValue)
        assertEquals( 1234567890                                                        , myUnsignedInteger .longValue)
        assertEquals(-1234567890123456789                                               , myLong            .longValue)
        assertEquals( 1234567890123456789                                               , myUnsignedLong    .longValue)
        assertEquals("1.2.3.4"                                                          , myIPv4            .stringValue)
        assertEquals("0001:0203:0405:0607:0809:1011:1213:1415"                          , myIPv6            .stringValue)
        assertEquals("Niels Basjes"                                                     , myName            .stringValue)
        assertEquals("0x4E 0x69 0x65 0x6C 0x73 0x20 0x42 0x61 0x73 0x6A 0x65 0x73"      , myNameHex         .stringValue)
    }

    @Test
    fun checkIfGeneratedCodeWorks1() {
        val myDevice = MyNewDevice().schemaDevice
        myDevice.connect(modbusDevice)
        myDevice.updateAll()

        val mainBlock = myDevice.getBlock("Main")   ?: throw IllegalArgumentException("Cannot find block \"Main\"")

        verifyAllFieldValues(
            mainBlock.getField("MyFloat")           ?: throw IllegalArgumentException("Cannot find field \"MyFloat\""),
            mainBlock.getField("MyDouble")          ?: throw IllegalArgumentException("Cannot find field \"MyDouble\""),
            mainBlock.getField("MyBitset1")         ?: throw IllegalArgumentException("Cannot find field \"MyBitset1\""),
            mainBlock.getField("MyBitset2")         ?: throw IllegalArgumentException("Cannot find field \"MyBitset2\""),
            mainBlock.getField("MyBitset3")         ?: throw IllegalArgumentException("Cannot find field \"MyBitset3\""),
            mainBlock.getField("MyBitset4")         ?: throw IllegalArgumentException("Cannot find field \"MyBitset4\""),
            mainBlock.getField("MyEnum")            ?: throw IllegalArgumentException("Cannot find field \"MyEnum\""),
            mainBlock.getField("MyEUI48")           ?: throw IllegalArgumentException("Cannot find field \"MyEUI48\""),
            mainBlock.getField("MyShort")           ?: throw IllegalArgumentException("Cannot find field \"MyShort\""),
            mainBlock.getField("MyUnsignedShort")   ?: throw IllegalArgumentException("Cannot find field \"MyUnsignedShort\""),
            mainBlock.getField("MyInteger")         ?: throw IllegalArgumentException("Cannot find field \"MyInteger\""),
            mainBlock.getField("MyUnsignedInteger") ?: throw IllegalArgumentException("Cannot find field \"MyUnsignedInteger\""),
            mainBlock.getField("MyLong")            ?: throw IllegalArgumentException("Cannot find field \"MyLong\""),
            mainBlock.getField("MyUnsignedLong")    ?: throw IllegalArgumentException("Cannot find field \"MyUnsignedLong\""),
            mainBlock.getField("MyIPv4")            ?: throw IllegalArgumentException("Cannot find field \"MyIPv4\""),
            mainBlock.getField("MyIPv6")            ?: throw IllegalArgumentException("Cannot find field \"MyIPv6\""),
            mainBlock.getField("MyName")            ?: throw IllegalArgumentException("Cannot find field \"MyName\""),
            mainBlock.getField("MyNameHex")         ?: throw IllegalArgumentException("Cannot find field \"MyNameHex\""),
        )
    }

    @Test
    fun checkIfGeneratedCodeWorks2() {
        val myDevice = MyNewDevice().connect(modbusDevice)
        myDevice.updateAll()

        verifyAllFieldValues(
            myDevice.main.myFloat           .field,
            myDevice.main.myDouble          .field,
            myDevice.main.myBitset1         .field,
            myDevice.main.myBitset2         .field,
            myDevice.main.myBitset3         .field,
            myDevice.main.myBitset4         .field,
            myDevice.main.myEnum            .field,
            myDevice.main.myEUI48           .field,
            myDevice.main.myShort           .field,
            myDevice.main.myUnsignedShort   .field,
            myDevice.main.myInteger         .field,
            myDevice.main.myUnsignedInteger .field,
            myDevice.main.myLong            .field,
            myDevice.main.myUnsignedLong    .field,
            myDevice.main.myIPv4            .field,
            myDevice.main.myIPv6            .field,
            myDevice.main.myName            .field,
            myDevice.main.myNameHex         .field,
        )
    }

}
