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

import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder

object DeviceFimerPVSDated20240722 {
    @JvmStatic
    val device: ModbusDevice
        get() =
            // Extracted on 2024-07-22 from a Fimer PVS inverter by https://github.com/Nedi11
            // This data was provided here https://github.com/nielsbasjes/energy/pull/308#issuecomment-2242694058
            builder()
//            .withLogging()
                .withRegisters(
                    AddressClass.HOLDING_REGISTER,
                    40000,  // The SunS header address
                    """
                    # SunS header
                    5375 6E53

                    # --------------------------------------
                    # Model 1 [Header @ hr:40002]: Common
                    0001 0042

                    # Model 1 [Data @ hr:40004 - hr:40070]: 66 registers
                    4649 4D45 5200 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 2D33 5135 382D 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 3078 3035 3543 2F30 7830 4235 372F 0000
                    3233 3330 4600 0000 0000 0000 0000 0000 3131 3837
                    3437 2D33 5135 382D 3432 3231 0000 0000 0000 0000
                    0000 0000 0000 0000 0001 FFFF

                    # --------------------------------------
                    # Model 103 [Header @ hr:40070]: Inverter (Three Phase)
                    0067 0032

                    # Model 103 [Data @ hr:40072 - hr:40122]: 50 registers
                    0C81 042B 042A 042C FFFF 1DE8 1DDE 1DEC 1147 1140
                    1141 FFFF 373A 0001 1387 FFFE 373A 0001 0001 0001
                    D8F0 FFFC 02BD 2174 0001 0609 FFFF FFFF 8000 3869
                    0001 0217 0302 8000 8000 FFFF 0004 0006 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000


                    # --------------------------------------
                    # Model 120 [Header @ hr:40122]: Nameplate
                    0078 001A

                    # Model 120 [Data @ hr:40124 - hr:40150]: 26 registers
                    0004 4844 0001 4844 0001 445C 445C BBA4 BBA4 0001
                    0087 0000 FFFF 0001 FFFF 0001 FFFD FFFF 8000 FFFF
                    8000 FFFF 8000 FFFF 8000 FFFF

                    # --------------------------------------
                    # Model 121 [Header @ hr:40150]: Basic Settings
                    0079 001E

                    # Model 121 [Data @ hr:40152 - hr:40182]: 30 registers
                    4844 FFFF 8000 FFFF FFFF 4844 445C 445C BBA4 BBA4
                    014D 8000 8000 8000 8000 FFFF FFFF FFFF FFFF FFFF
                    0001 8000 8000 8000 0001 0001 FFFF 8000 8000 8000


                    # --------------------------------------
                    # Model 122 [Header @ hr:40182]: Measurements_Status
                    007A 002C

                    # Model 122 [Data @ hr:40184 - hr:40228]: 44 registers
                    0001 0000 0001 0000 0000 1B5E 3DA0 0000 0000 1B65
                    36C0 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 8000 8000 FFFF
                    8000 0000 0000 0000 0000 0000 0000 0000 0000 2E30
                    F1B7 0000 01B1 0003

                    # --------------------------------------
                    # Model 123 [Header @ hr:40228]: Immediate Controls
                    007B 0018

                    # Model 123 [Data @ hr:40230 - hr:40254]: 24 registers
                    0000 003C 0001 03E8 0000 003C 0000 0000 2710 0000
                    003C 0000 0000 0000 0000 8000 0000 003C 0000 0000
                    0000 FFFF FFFC FFFF

                    # --------------------------------------
                    # Model 126 [Header @ hr:40254]: Static Volt-VAR
                    007E 00E2

                    # Model 126 [Data @ hr:40256 - hr:40482]: 226 registers
                    0001 0000 0000 FFFF FFFF 0004 000A FFFF FFFF 0000
                    000A 0000 0384 01B4 0398 0000 0438 0000 044C FE4C
                    044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C
                    044C FE4C FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 6D6F 6465 6C20 3100 0000 0000 0000 0000
                    FFFF FFFF FFFF 0000 000A 0000 0384 01B4 0398 0000
                    0438 0000 044C FE4C 044C FE4C 044C FE4C 044C FE4C
                    044C FE4C 044C FE4C 044C FE4C FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 6D6F 6465 6C20 3200
                    0000 0000 0000 0000 FFFF FFFF FFFF 0000 000A 0000
                    0384 01B4 0398 0000 0438 0000 044C FE4C 044C FE4C
                    044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    6D6F 6465 6C20 3300 0000 0000 0000 0000 FFFF FFFF
                    FFFF 0000 000A 0000 0384 01B4 0398 0000 0438 0000
                    044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C
                    044C FE4C 044C FE4C FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 6D6F 6465 6C20 3400 0000 0000
                    0000 0000 FFFF FFFF FFFF 0000

                    # --------------------------------------
                    # Model 127 [Header @ hr:40482]: Freq-Watt Param
                    007F 000A

                    # Model 127 [Data @ hr:40484 - hr:40494]: 10 registers
                    0028 139C 138D 0001 0000 0258 0000 FFFE 0000 FFFF


                    # --------------------------------------
                    # Model 129 [Header @ hr:40494]: LVRTD
                    0081 003C

                    # Model 129 [Data @ hr:40496 - hr:40556]: 60 registers
                    0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFF FFFF
                    0006 0064 0320 001E 012C 0000 0015 0000 0015 0000
                    0015 0000 0015 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000


                    # --------------------------------------
                    # Model 130 [Header @ hr:40556]: HVRTD
                    0082 003C

                    # Model 130 [Data @ hr:40558 - hr:40618]: 60 registers
                    0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFF FFFF
                    0006 000A 047E 0005 04E0 0000 04E0 0000 04E0 0000
                    04E0 0000 04E0 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000


                    # --------------------------------------
                    # Model 132 [Header @ hr:40618]: Volt-Watt
                    0084 00E2

                    # Model 132 [Data @ hr:40620 - hr:40846]: 226 registers
                    0001 0000 0000 FFFF FFFF 0004 000A FFFF FFFF 0000
                    000A 0001 0384 03E8 03BC 03E8 043E 03E8 0480 00C8
                    0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8
                    0480 00C8 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 6D6F 6465 6C20 3100 0000 0000 0000 0000
                    0009 1770 1770 0000 000A 0001 0384 03E8 03BC 03E8
                    043E 03E8 0480 00C8 0480 00C8 0480 00C8 0480 00C8
                    0480 00C8 0480 00C8 0480 00C8 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 6D6F 6465 6C20 3200
                    0000 0000 0000 0000 0009 1770 1770 0000 000A 0001
                    0384 03E8 03BC 03E8 043E 03E8 0480 00C8 0480 00C8
                    0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    6D6F 6465 6C20 3300 0000 0000 0000 0000 0009 1770
                    1770 0000 000A 0001 0384 03E8 03BC 03E8 043E 03E8
                    0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8
                    0480 00C8 0480 00C8 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                    FFFF 8000 FFFF 8000 6D6F 6465 6C20 3400 0000 0000
                    0000 0000 0009 1770 1770 0000

                    # --------------------------------------
                    # Model 135 [Header @ hr:40846]: LFRT
                    0087 003C

                    # Model 135 [Data @ hr:40848 - hr:40908]: 60 registers
                    0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFE FFFF
                    0006 000A 128E 000A 1194 0000 1194 0000 1194 0000
                    1194 0000 1194 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000


                    # --------------------------------------
                    # Model 136 [Header @ hr:40908]: HFRT
                    0088 003C

                    # Model 136 [Data @ hr:40910 - hr:40970]: 60 registers
                    0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFE FFFF
                    0006 000A 141E 000A 1964 0000 1964 0000 1964 0000
                    1964 0000 1964 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000


                    # --------------------------------------
                    # Model 139 [Header @ hr:40970]: LVRTX
                    008B 003C

                    # Model 139 [Data @ hr:40972 - hr:41032]: 60 registers
                    0001 0000 FFFF FFFF FFFF 0001 0001 FFFE FFFF 0001
                    0001 FFFF 01F4 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000

                    # --------------------------------------
                    # Model 140 [Header @ hr:41032]: HVRTX
                    008C 003C

                    # Model 140 [Data @ hr:41034 - hr:41094]: 60 registers
                    0001 0000 FFFF FFFF FFFF 0001 0001 FFFE FFFF 0001
                    0001 FFFF 04E0 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000


                    # --------------------------------------
                    # Model 145 [Header @ hr:41094]: Extended Settings
                    0091 0008

                    # Model 145 [Data @ hr:41096 - hr:41104]: 8 registers
                    EA60 FFFF FFFF FFFF 0064 FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 160 [Header @ hr:41104]: Multiple MPPT Inverter Extension Model
                    00A0 00F8

                    # Model 160 [Data @ hr:41106 - hr:41354]: 248 registers
                    FFFF FFFF 0001 8000 0000 0000 000C FFFF 0001 5056
                    3100 0000 0000 0000 0000 0000 0000 0080 2553 04CA
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0002 5056
                    3200 0000 0000 0000 0000 0000 0000 0080 24AD 04B6
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0003 5056
                    3300 0000 0000 0000 0000 0000 0000 0082 24CD 04C7
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0004 5056
                    3400 0000 0000 0000 0000 0000 0000 0080 24BD 04B3
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0005 5056
                    3500 0000 0000 0000 0000 0000 0000 0080 2473 04B5
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0006 5056
                    3600 0000 0000 0000 0000 0000 0000 007F 248A 04A8
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0007 5056
                    3700 0000 0000 0000 0000 0000 0000 0080 248E 04B6
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0008 5056
                    3800 0000 0000 0000 0000 0000 0000 0080 240F 049D
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0009 5056
                    3900 0000 0000 0000 0000 0000 0000 0081 2465 04B8
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 000A 5056
                    3130 0000 0000 0000 0000 0000 0000 0081 2445 04AA
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 000B 5056
                    3131 0000 0000 0000 0000 0000 0000 0081 243A 04AD
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 000C 5056
                    3132 0000 0000 0000 0000 0000 0000 0081 2463 04AE
                    0000 0000 FFFF FFFF 8000 0004 0000 0000

                    # --------------------------------------
                    # Model Id 65230
                    FECE 0001
                    # Model Id 65230 is 1 register.
                    0000

                    # Model Id 65232
                    FED0 0014
                    # Model Id 65232 is 20 registers.
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0001 0003 0005 FFFF FFFF 0000 0000 0000

                    # NO MORE MODELS
                    FFFF 0000
                    """.trimIndent(),
                ).build()
}
