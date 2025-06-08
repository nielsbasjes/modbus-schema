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
package nl.basjes.modbus.device.testcases.sunspec

import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder

object DeviceSMASunnyBoy36Dated20250518 {
    @JvmStatic
    val device: MockedModbusDevice
        get() =
            // Extracted on 2025-05-18 from my own SMA SunnyBoy 3.6 after having implemented Modbus read error handling
            builder()
//            .withLogging()
                .withRegisters(
                    HOLDING_REGISTER,
                    40000,
                    """
                    # --------------------------------------
                    # SunS header
                    5375 6E53

                    # --------------------------------------
                    # Model 1 [Header @ hr:40002]: Common
                    0001 0042

                    # Model 1 [Data @ hr:40004 - hr:40069]: 66 registers
                    534D 4100 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 5342 332E 362D 3141
                    562D 3431 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    342E 3031 2E31 352E 5200 0000 0000 0000 3330 3035
                    3036 3734 3135 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 FFFF 8000

                    # --------------------------------------
                    # Model 11 [Header @ hr:40070]: Ethernet Link Layer
                    000B 000D

                    # Model 11 [Data @ hr:40072 - hr:40084]: 13 registers
                    0000 0000 0002 0000 0040 ADA9 9576 0000 0000 0000
                    0000 FFFF FFFF

                    # --------------------------------------
                    # Model 12 [Header @ hr:40085]: IPv4
                    000C 0062

                    # Model 12 [Data @ hr:40087 - hr:40184]: 98 registers
                    0000 0000 0000 0000 0001 0000 0005 0001 0000 3139
                    322E 3136 382E 302E 3137 3000 0000 3235 352E 3235
                    352E 3235 352E 3000 0000 3139 322E 3136 382E 302E
                    3100 0000 0000 3139 322E 3136 382E 302E 3100 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 8000

                    # --------------------------------------
                    # Model 101 [Header @ hr:40185]: Inverter (Single Phase)
                    0065 0032

                    # Model 101 [Data @ hr:40187 - hr:40236]: 50 registers
                    0097 0097 FFFF FFFF FFFF FFFF FFFF FFFF 0988 FFFF
                    FFFF FFFF 0170 0001 1387 FFFE 0170 0001 0008 0001
                    FC18 FFFD 002E 5E85 0001 FFFF 8000 FFFF 8000 8000
                    0001 002C 8000 8000 8000 0000 0004 FFFF 0000 0000
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 120 [Header @ hr:40237]: Nameplate
                    0078 001A

                    # Model 120 [Data @ hr:40239 - hr:40264]: 26 registers
                    0004 0170 0001 0170 0001 00B8 8000 8000 00B8 0001
                    00A0 FFFF 0320 8000 8000 0320 FFFD FFFF 0002 FFFF
                    0000 FFFF 0001 FFFF 0001 8000

                    # --------------------------------------
                    # Model 121 [Header @ hr:40265]: Basic Settings
                    0079 001E

                    # Model 121 [Data @ hr:40267 - hr:40296]: 30 registers
                    0170 00E6 0000 FFFF FFFF 0170 8000 8000 8000 8000
                    0014 8000 8000 8000 8000 FFFF FFFF 0341 0032 0001
                    0001 0000 0000 8000 0001 8000 0000 8000 0000 0000

                    # --------------------------------------
                    # Model 122 [Header @ hr:40297]: Measurements_Status
                    007A 002C

                    # Model 122 [Data @ hr:40299 - hr:40342]: 44 registers
                    0005 0000 0001 0000 0000 01CF B132 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 8000 8000 FFFF
                    8000 FFFF FFFF FFFF FFFF 0000 0000 0000 0000 FFFF
                    FFFF FFFF 012C 0004

                    # --------------------------------------
                    # Model 123 [Header @ hr:40343]: Immediate Controls
                    007B 0018

                    # Model 123 [Data @ hr:40345 - hr:40368]: 24 registers
                    FFFF FFFF 0000 0000 FFFF FFFF FFFF 0001 0000 FFFF
                    FFFF FFFF 0000 0000 8000 8000 FFFF FFFF FFFF 0001
                    0000 FFFE FFFC FFFE

                    # --------------------------------------
                    # Model 124 [Header @ hr:40369]: Storage
                    007C 0018

                    # Model 124 [Data @ hr:40371 - hr:40394]: 24 registers
                    FFFF FFFF FFFF 0000 FFFF FFFF FFFF FFFF FFFF FFFF
                    8000 8000 FFFF FFFF FFFF FFFF 0000 8000 8000 8000
                    0000 8000 FFFE 8000

                    # --------------------------------------
                    # Model 126 [Header @ hr:40395]: Static Volt-VAR
                    007E 0040

                    # Model 126 [Data @ hr:40397 - hr:40460]: 64 registers
                    0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000
                    0004 0002 2710 0000 2710 0000 2710 0000 2710 0000
                    2710 0000 2710 0000 2710 0000 2710 0000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 0000 0000 0000 0000 0000 0000 0000 0000
                    000A 04B0 04B0 0000

                    # --------------------------------------
                    # Model 127 [Header @ hr:40461]: Freq-Watt Param
                    007F 000A

                    # Model 127 [Data @ hr:40463 - hr:40472]: 10 registers
                    0028 0014 0014 0000 0001 000A 0000 FFFE 0000 8000

                    # --------------------------------------
                    # Model 128 [Header @ hr:40473]: Dynamic Reactive Current
                    0080 000E

                    # Model 128 [Data @ hr:40475 - hr:40488]: 14 registers
                    FFFF FFFF FFFF 0000 003C 8000 FFFF 0046 0005 0000
                    FFFF FFFE 0000 8000

                    # --------------------------------------
                    # Model 131 [Header @ hr:40489]: Watt-PF
                    0083 0040

                    # Model 131 [Data @ hr:40491 - hr:40554]: 64 registers
                    0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000
                    0004 2710 0000 2710 0000 2710 0000 2710 0000 2710
                    0000 2710 0000 2710 0000 2710 0000 8000 8000 8000
                    8000 8000 8000 8000 8000 8000 8000 8000 8000 8000
                    8000 8000 8000 8000 8000 8000 8000 8000 8000 8000
                    8000 0000 0000 0000 0000 0000 0000 0000 0000 000A
                    04B0 04B0 0000 8000

                    # --------------------------------------
                    # Model 132 [Header @ hr:40555]: Volt-Watt
                    0084 0040

                    # Model 132 [Data @ hr:40557 - hr:40620]: 64 registers
                    0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000
                    FFFF 0001 2710 2710 2710 2710 2710 0000 2710 0000
                    2710 0000 2710 0000 2710 0000 2710 0000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 0000 0000 0000 0000 0000 0000 0000 0000
                    000A 04B0 04B0 0000

                    # --------------------------------------
                    # Model 160 [Header @ hr:40621]: Multiple MPPT Inverter Extension Model
                    00A0 0080

                    # Model 160 [Data @ hr:40623 - hr:40750]: 128 registers
                    FFFF 0000 0001 8000 0000 0000 0006 FFFF 0001 0000
                    0000 0000 0000 0000 0000 0000 0000 0049 012D 00DD
                    0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0002 0000
                    0000 0000 0000 0000 0000 0000 0000 0042 00F2 00A0
                    0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0003 0000
                    0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                    0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0004 0000
                    0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                    0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF 0005 0000
                    0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                    0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF 0006 0000
                    0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                    0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 129 [Header @ hr:40751]: LVRTD
                    0081 003C

                    # Model 129 [Data @ hr:40753 - hr:40812]: 60 registers
                    0001 0001 FFFF FFFF FFFF 0001 0003 FFFD 0000 8000
                    0003 07D0 0050 2710 0014 2710 0014 FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 0000 0000 0000 0000 0000 0000 0000 0000 0000

                    # --------------------------------------
                    # Model 130 [Header @ hr:40813]: HVRTD
                    0082 003C

                    # Model 130 [Data @ hr:40815 - hr:40874]: 60 registers
                    0001 0001 FFFF FFFF FFFF 0001 0003 FFFD 0000 8000
                    0003 07D0 006E 2710 007A 2710 007A FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 0000 0000 0000 0000 0000 0000 0000 0000 0000

                    # --------------------------------------
                    # NO MORE MODELS
                    FFFF 0000
                    """.trimIndent(),
                ).build()
}
