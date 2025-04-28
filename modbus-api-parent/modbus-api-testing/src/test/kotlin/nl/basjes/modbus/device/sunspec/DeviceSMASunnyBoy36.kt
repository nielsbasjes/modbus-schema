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
package nl.basjes.modbus.device.sunspec

import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder

object DeviceSMASunnyBoy36 {
    @JvmStatic
    val device: ModbusDevice
        get() =// Extracted on 2023-08-10 from my own SMA SunnyBoy 3.6
            builder() //            .withLogging()
                .withRegisters(
                    HOLDING_REGISTER,
                    40000,
                    /* SunS header */
                    "5375 6E53 " +

                    /* Model 1 [Header] */
                    "0001 0042 " +
                    /* Model 1 [66 registers] */
                    "534D 4100 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 5342 332E 362D 3141 " +
                    "562D 3431 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "342E 3030 2E37 352E 5200 0000 0000 0000 3330 3035 " +
                    "3036 3734 3135 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 FFFF 8000 " +

                    /* Model 11 [Header] */
                    "000B 000D " +
                    /* Model 11 [13 registers] */
                    "0000 0000 0002 0000 0040 ADA9 9576 0000 0000 0000 " +
                    "0000 FFFF FFFF " +

                    /* Model 12 [Header] */
                    "000C 0062 " +
                    /* Model 12 [98 registers] */
                    "0000 0000 0000 0000 0001 0000 0005 0001 0000 3139 " +
                    "322E 3136 382E 302E 3137 3000 0000 3235 352E 3235 " +
                    "352E 3235 352E 3000 0000 3139 322E 3136 382E 302E " +
                    "3100 0000 0000 3139 322E 3136 382E 302E 3100 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 8000 " +

                    /* Model 101 [Header] */
                    "0065 0032 " +
                    /* Model 101 [50 registers] */
                    "002D 002D FFFF FFFF FFFF FFFF FFFF FFFF 0958 FFFF " +
                    "FFFF FFFF 006C 0001 1386 FFFE 006D 0001 000A 0001 " +
                    "FC1C FFFD 0021 1FAD 0001 FFFF 8000 FFFF 8000 8000 " +
                    "0001 002A 8000 8000 8000 0000 0004 FFFF 0000 0000 " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +

                    /* Model 120 [Header] */
                    "0078 001A " +
                    /* Model 120 [26 registers] */
                    "0004 0170 0001 0170 0001 00B8 8000 8000 00B8 0001 " +
                    "00A0 FFFF 0320 8000 8000 0320 FFFD FFFF 0002 FFFF " +
                    "0000 FFFF 0001 FFFF 0001 8000 " +

                    /* Model 121 [Header] */
                    "0079 001E " +
                    /* Model 121 [30 registers] */
                    "0170 00E6 0000 FFFF FFFF 0170 8000 8000 8000 8000 0014 " +
                    "8000 8000 8000 8000 FFFF FFFF 0341 0032 0001 0001 0000 " +
                    "0000 8000 0001 8000 0000 8000 0000 0000 " +

                    /* Model 122 [Header] */
                    "007A 002C " +
                    /* Model 122 [44 registers] */
                    "0005 0000 0001 0000 0000 014B 3CBF 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 8000 8000 FFFF 8000 FFFF FFFF " +
                    "FFFF FFFF 0000 0000 0000 0000 FFFF FFFF FFFF 012C 0004 " +

                    /* Model 123 [Header] */
                    "007B 0018 " +
                    /* Model 123 [24 registers] */
                    "FFFF FFFF 0000 0000 FFFF FFFF FFFF 0001 0000 FFFF FFFF " +
                    "FFFF 0000 0000 8000 8000 FFFF FFFF FFFF 0001 0000 FFFE " +
                    "FFFC FFFE " +

                    /* Model 124 [Header] */
                    "007C 0018 " +
                    /* Model 124 [24 registers] */
                    "FFFF FFFF FFFF 0000 FFFF FFFF FFFF FFFF FFFF FFFF 8000 " +
                    "8000 FFFF FFFF FFFF FFFF 0000 8000 8000 8000 0000 8000 " +
                    "FFFE 8000 " +

                    /* Model 126 [Header] */
                    "007E 0040 " +
                    /* Model 126 [64 registers] */
                    "0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000 0004 " +
                    "0002 2710 0000 2710 0000 2710 0000 2710 0000 2710 0000 " +
                    "2710 0000 2710 0000 2710 0000 FFFF 8000 FFFF 8000 FFFF " +
                    "8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 000A 04B0 04B0 0000 " +

                    /* Model 127 [Header] */
                    "007F 000A " +
                    /* Model 127 [10 registers] */
                    "0028 0014 0014 0000 0001 000A 0000 FFFE 0000 8000 " +

                    /* Model 128 [Header] */
                    "0080 000E " +
                    /* Model 128 [14 registers] */
                    "FFFF FFFF FFFF 0000 003C 8000 FFFF 0046 0005 0000 FFFF FFFE " +
                    "0000 8000 " +

                    /* Model 131 [Header] */
                    "0083 0040 " +
                    /* Model 131 [64 registers] */
                    "0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000 0004 2710 " +
                    "0000 2710 0000 2710 0000 2710 0000 2710 0000 2710 0000 2710 " +
                    "0000 2710 0000 8000 8000 8000 8000 8000 8000 8000 8000 8000 " +
                    "8000 8000 8000 8000 8000 8000 8000 8000 8000 8000 8000 8000 " +
                    "8000 8000 8000 0000 0000 0000 0000 0000 0000 0000 0000 000A " +
                    "04B0 04B0 0000 8000 " +

                    /* Model 132 [Header] */
                    "0084 0040 " +
                    /* Model 132 [64 registers] */
                    "0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000 0002 0001 " +
                    "2710 2710 2710 2710 2710 0000 2710 0000 2710 0000 2710 0000 " +
                    "2710 0000 2710 0000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "000A 04B0 04B0 0000 " +

                    /* Model 160 [Header] */
                    "00A0 0080 " +
                    /* Model 160 [128 registers] */
                    "FFFF 0000 0001 8000 0000 0000 0006 FFFF 0001 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0015 0142 0043 0000 0000 FFFF FFFF 8000 FFFF " +
                    "0000 0000 0002 0000 0000 0000 0000 0000 0000 0000 0000 0015 00E0 " +
                    "0030 0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0003 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 FFFF FFFF FFFF 0000 0000 FFFF FFFF 8000 " +
                    "FFFF 0000 0000 0004 0000 0000 0000 0000 0000 0000 0000 0000 FFFF " +
                    "FFFF FFFF 0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF 0005 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF 0000 0000 FFFF FFFF " +
                    "8000 FFFF FFFF FFFF 0006 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "FFFF FFFF FFFF 0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF " +

                    /* Model 129 [Header] */
                    "0081 003C " +
                    /* Model 129 [60 registers] */
                    "0001 0001 FFFF FFFF FFFF 0001 0003 FFFD 0000 8000 0003 07D0 0050 " +
                    "2710 0014 2710 0014 FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 " +

                    /* Model 130 [Header] */
                    "0082 003C " +
                    /* Model 130 [60 registers] */
                    "0001 0001 FFFF FFFF FFFF 0001 0003 FFFD 0000 8000 0003 07D0 006E " +
                    "2710 007A 2710 007A FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 " +

                    /* NO MORE MODELS */
                    "FFFF 0000"
                )
                .build()
}
