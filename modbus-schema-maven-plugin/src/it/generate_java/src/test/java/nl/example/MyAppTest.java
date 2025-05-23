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
package nl.example;

import nl.basjes.modbus.device.api.AddressClass;
import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.device.memory.MockedModbusDevice;
import nl.basjes.modbus.schema.Block;
import nl.basjes.modbus.schema.Field;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.example.modbus.MyNewDevice;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyAppTest {

    private static final ModbusDevice modbusDevice = FakeModbusDevice.modbusDevice;

    void verifyAllFieldValues(
        Field myFloat,
        Field myDouble,
        Field myBitset1,
        Field myBitset2,
        Field myBitset3,
        Field myBitset4,
        Field myEnum,
        Field myEUI48,
        Field myShort,
        Field myUnsignedShort,
        Field myInteger,
        Field myUnsignedInteger,
        Field myLong,
        Field myUnsignedLong,
        Field myIPv4,
        Field myIPv6,
        Field myName,
        Field myNameHex
    ) {
        assertEquals(1234.567                                                           , myFloat   .getDoubleValue(), 0.001);
        assertEquals(1234.568                                                           , myDouble  .getDoubleValue(), 0.001);
        assertEquals(List.of("Zero", "One", "Two", "Bit 3", "Bit 12"),                                                                                                            myBitset1 .getStringListValue());
        assertEquals(List.of("Zero", "One", "Two", "Bit 3", "Bit 12", "Sixteen", "Seventeen", "Eighteen", "Bit 19", "Bit 28"),                                                    myBitset2 .getStringListValue());
        assertEquals(List.of("0", "1", "2", "Bit 3", "Bit 12", "16", "17", "18", "Bit 19", "Bit 28", "32", "33", "34", "Bit 35", "Bit 44"),                                       myBitset3 .getStringListValue());
        assertEquals(List.of("0", "1", "2", "Bit 3", "Bit 12", "16", "17", "18", "Bit 19", "Bit 28", "32", "33", "34", "Bit 35", "Bit 44", "48", "49", "50", "Bit 51", "Bit 60"), myBitset4 .getStringListValue());
        assertEquals("Manual"                                                           , myEnum            .getStringValue());
        assertEquals("01:02:03:04:05:06"                                                , myEUI48           .getStringValue());
        assertEquals(-12345                                                             , myShort           .getLongValue());
        assertEquals( 12345                                                             , myUnsignedShort   .getLongValue());
        assertEquals(-1234567890                                                        , myInteger         .getLongValue());
        assertEquals( 1234567890                                                        , myUnsignedInteger .getLongValue());
        assertEquals(-1234567890123456789L                                               , myLong            .getLongValue());
        assertEquals( 1234567890123456789L                                               , myUnsignedLong    .getLongValue());
        assertEquals("1.2.3.4"                                                          , myIPv4            .getStringValue());
        assertEquals("0001:0203:0405:0607:0809:1011:1213:1415"                          , myIPv6            .getStringValue());
        assertEquals("Niels Basjes"                                                     , myName            .getStringValue());
        assertEquals("0x4E 0x69 0x65 0x6C 0x73 0x20 0x42 0x61 0x73 0x6A 0x65 0x73"      , myNameHex         .getStringValue());
    }

    @Test
    void checkIfGeneratedCodeWorks1() throws ModbusException {
        SchemaDevice myDevice = new MyNewDevice().schemaDevice;
        myDevice.connect(modbusDevice);
        myDevice.updateAll();

        Block mainBlock = myDevice.getBlock("Main");
        if (mainBlock == null) {
            throw new IllegalArgumentException("Cannot find block \"Main\"");
        }


        Field myFloat           = mainBlock.getField("MyFloat");
        Field myDouble          = mainBlock.getField("MyDouble");
        Field myBitset1         = mainBlock.getField("MyBitset1");
        Field myBitset2         = mainBlock.getField("MyBitset2");
        Field myBitset3         = mainBlock.getField("MyBitset3");
        Field myBitset4         = mainBlock.getField("MyBitset4");
        Field myEnum            = mainBlock.getField("MyEnum");
        Field myEUI48           = mainBlock.getField("MyEUI48");
        Field myShort           = mainBlock.getField("MyShort");
        Field myUnsignedShort   = mainBlock.getField("MyUnsignedShort");
        Field myInteger         = mainBlock.getField("MyInteger");
        Field myUnsignedInteger = mainBlock.getField("MyUnsignedInteger");
        Field myLong            = mainBlock.getField("MyLong");
        Field myUnsignedLong    = mainBlock.getField("MyUnsignedLong");
        Field myIPv4            = mainBlock.getField("MyIPv4");
        Field myIPv6            = mainBlock.getField("MyIPv6");
        Field myName            = mainBlock.getField("MyName");
        Field myNameHex         = mainBlock.getField("MyNameHex");


        if (myFloat           == null) { throw new IllegalArgumentException("Cannot find field \"MyFloat\""); }
        if (myDouble          == null) { throw new IllegalArgumentException("Cannot find field \"MyDouble\""); }
        if (myBitset1         == null) { throw new IllegalArgumentException("Cannot find field \"MyBitset1\""); }
        if (myBitset2         == null) { throw new IllegalArgumentException("Cannot find field \"MyBitset2\""); }
        if (myBitset3         == null) { throw new IllegalArgumentException("Cannot find field \"MyBitset3\""); }
        if (myBitset4         == null) { throw new IllegalArgumentException("Cannot find field \"MyBitset4\""); }
        if (myEnum            == null) { throw new IllegalArgumentException("Cannot find field \"MyEnum\""); }
        if (myEUI48           == null) { throw new IllegalArgumentException("Cannot find field \"MyEUI48\""); }
        if (myShort           == null) { throw new IllegalArgumentException("Cannot find field \"MyShort\""); }
        if (myUnsignedShort   == null) { throw new IllegalArgumentException("Cannot find field \"MyUnsignedShort\""); }
        if (myInteger         == null) { throw new IllegalArgumentException("Cannot find field \"MyInteger\""); }
        if (myUnsignedInteger == null) { throw new IllegalArgumentException("Cannot find field \"MyUnsignedInteger\""); }
        if (myLong            == null) { throw new IllegalArgumentException("Cannot find field \"MyLong\""); }
        if (myUnsignedLong    == null) { throw new IllegalArgumentException("Cannot find field \"MyUnsignedLong\""); }
        if (myIPv4            == null) { throw new IllegalArgumentException("Cannot find field \"MyIPv4\""); }
        if (myIPv6            == null) { throw new IllegalArgumentException("Cannot find field \"MyIPv6\""); }
        if (myName            == null) { throw new IllegalArgumentException("Cannot find field \"MyName\""); }
        if (myNameHex         == null) { throw new IllegalArgumentException("Cannot find field \"MyNameHex\""); }


        verifyAllFieldValues(
            myFloat,
            myDouble,
            myBitset1,
            myBitset2,
            myBitset3,
            myBitset4,
            myEnum,
            myEUI48,
            myShort,
            myUnsignedShort,
            myInteger,
            myUnsignedInteger,
            myLong,
            myUnsignedLong,
            myIPv4,
            myIPv6,
            myName,
            myNameHex
        );
    }

    @Test
    void checkIfGeneratedCodeWorks2() throws ModbusException {
        MyNewDevice myDevice = new MyNewDevice().connect(modbusDevice);
        myDevice.updateAll();

        assertNotNull(myDevice.main.myFloat           .field);
        assertNotNull(myDevice.main.myDouble          .field);
        assertNotNull(myDevice.main.myBitset1         .field);
        assertNotNull(myDevice.main.myBitset2         .field);
        assertNotNull(myDevice.main.myBitset3         .field);
        assertNotNull(myDevice.main.myBitset4         .field);
        assertNotNull(myDevice.main.myEnum            .field);
        assertNotNull(myDevice.main.myEUI48           .field);
        assertNotNull(myDevice.main.myShort           .field);
        assertNotNull(myDevice.main.myUnsignedShort   .field);
        assertNotNull(myDevice.main.myInteger         .field);
        assertNotNull(myDevice.main.myUnsignedInteger .field);
        assertNotNull(myDevice.main.myLong            .field);
        assertNotNull(myDevice.main.myUnsignedLong    .field);
        assertNotNull(myDevice.main.myIPv4            .field);
        assertNotNull(myDevice.main.myIPv6            .field);
        assertNotNull(myDevice.main.myName            .field);
        assertNotNull(myDevice.main.myNameHex         .field);

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
            myDevice.main.myNameHex         .field
        );
    }

}
