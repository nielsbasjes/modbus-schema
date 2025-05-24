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

//    This data was generated using the DER emulator (part of DERSec LabTest Pro)
//    kindly provided by DER Security Corp ( https://dersec.io )

object EmulatedDER {
    //    |-------+----------+--------+----------------------------------|
    //    | Model | Address  | Length | Label                            |
    //    |-------+----------+--------+----------------------------------|
    //    | 0     | hr:40000 | 0      | Start of the SunSpec Model chain |
    //    | 1     | hr:40002 | 66     | Common                           |
    //    | 701   | hr:40070 | 153    | DER AC Measurement               |
    //    | 702   | hr:40225 | 50     | DER Capacity                     |
    //    | 703   | hr:40277 | 17     | Enter Service                    |
    //    | 704   | hr:40296 | 65     | DER AC Controls                  |
    //    | 705   | hr:40363 | 67     | DER Volt-Var                     |
    //    | 706   | hr:40432 | 40     | DER Volt-Watt                    |
    //    | 707   | hr:40474 | 105    | DER Trip LV                      |
    //    | 708   | hr:40581 | 105    | DER Trip HV                      |
    //    | 709   | hr:40688 | 135    | DER Trip LF                      |
    //    | 710   | hr:40825 | 135    | DER Trip HF                      |
    //    | 711   | hr:40962 | 42     | DER Frequency Droop              |
    //    | 712   | hr:41006 | 60     | DER Watt-Var                     |
    //    | 713   | hr:41068 | 7      | DER Storage Capacity             |
    //    | 714   | hr:41077 | 68     | DER DC Measurement               |
    //    | 64412 | hr:41147 | 43     | Non existent model               |
    //    | 65535 | hr:41192 | 0      | End of the SunSpec Model chain   |
    //    |-------+----------+--------+----------------------------------|
    @JvmStatic
    val device: MockedModbusDevice
        get() =
            // A schema specifically for the SunSpec device made by DERSec model DER Simulator using version 1.2.3 (SN: SN-Three-Phase)
            builder()
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
                    4445 5253 6563 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 4445 5220 5369 6D75
                    6C61 746F 7200 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 3130 206B 5720 4445 5200 0000 0000 0000
                    312E 322E 3300 0000 0000 0000 0000 0000 534E 2D54
                    6872 6565 2D50 6861 7365 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0001 0000

                    # --------------------------------------
                    # Model 701 [Header @ hr:40070]: DER AC Measurement
                    02BD 0099

                    # Model 701 [Data @ hr:40072 - hr:40224]: 153 registers
                    0002 0001 0003 0001 0000 0000 FFFF FFFF 2648 03E8
                    00C8 03D9 019B 12C2 0AD5 0000 EA6A 0000 0000 0000
                    0096 0000 0000 0000 0000 0000 0000 0000 0009 0000
                    0000 0000 0000 FFF6 0226 028A 01F4 0190 01A4 0C80
                    0D05 0050 03D8 0089 12CC 0AD0 0000 0000 0000 0031
                    0000 0000 0000 0000 0000 0000 0000 0002 0000 0000
                    0000 0000 0CE4 0D05 0050 03DA 0088 12C2 0AD5 0000
                    0000 0000 0032 0000 0000 0000 0000 0000 0000 0000
                    0003 0000 0000 0000 0000 0DAC 0D05 0028 03DB 008A
                    12BF 0AD8 0000 0000 0000 0033 0000 0000 0000 0000
                    0000 0000 0000 0004 0000 0000 0000 0000 FFFF FFFF
                    FFFF FFFF FFFF FFFD 0000 FFFD 0000 0000 0000 0000
                    FFFF 4D61 6E75 6661 6374 7572 6572 2063 7573 746F
                    6D20 6572 726F 7220 696E 666F 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000

                    # --------------------------------------
                    # Model 702 [Header @ hr:40225]: DER Capacity
                    02BE 0032

                    # Model 702 [Data @ hr:40227 - hr:40276]: 50 registers
                    2710 2710 0320 2710 0320 2710 1130 1130 2710 2710
                    2710 2710 12C0 1680 0F00 0096 0352 0352 0000 0001
                    0002 0000 37BF 0000 2710 2134 0352 2134 0352 2710
                    1130 1130 2710 2710 2710 2710 12C0 1680 0F00 0096
                    FFFF FFFF 0000 0000 FFFD 0000 0000 FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 703 [Header @ hr:40277]: Enter Service
                    02BF 0011

                    # Model 703 [Data @ hr:40279 - hr:40295]: 17 registers
                    0001 041A 0395 0000 177A 0000 173E 0000 012C 0000
                    0064 0000 003C 0000 0000 FFFF FFFE

                    # --------------------------------------
                    # Model 704 [Header @ hr:40296]: DER AC Controls
                    02C0 0041

                    # Model 704 [Data @ hr:40298 - hr:40362]: 65 registers
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 03E8 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 2710 0000 0000 0064 0064 0000 0000
                    0000 0000 0000 0000 0004 0000 0000 09C4 0000 09C4
                    03E8 0064 0000 0000 0000 0000 0000 0064 0000 01F4
                    0001 FFFD FFFF FFFF FFFF FFFF FFFF 0384 0001 03E8
                    0001 FFFF FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 705 [Header @ hr:40363]: DER Volt-Var
                    02C1 0043

                    # Model 705 [Data @ hr:40365 - hr:40431]: 67 registers
                    0000 0000 0000 0004 0003 0000 0000 0000 0000 0000
                    FFFE FFFE FFFF 0004 0001 0001 2710 2710 0000 01F4
                    0000 0006 0001 23F0 0BB8 25C6 0000 283C 0000 29CC
                    F448 0004 0001 0001 2710 2710 0001 03E8 0000 0002
                    0000 23F0 0BB8 2562 0000 27D8 0000 2968 F060 0004
                    0001 0001 2710 2710 0000 01F4 0000 0004 0000 24B8
                    07D0 2562 0000 2904 0000 2A30 F830

                    # --------------------------------------
                    # Model 706 [Header @ hr:40432]: DER Volt-Watt
                    02C2 0028

                    # Model 706 [Data @ hr:40434 - hr:40473]: 40 registers
                    0000 0000 0000 0002 0003 0000 0000 0000 0000 0000
                    FFFF FFFF FFFF 0002 0000 0000 000F 0001 0424 03E8
                    044C 0000 0002 0000 0000 0014 0000 041A 03E8 0442
                    0000 0002 0000 0000 0014 0000 041A 03E8 0442 0000

                    # --------------------------------------
                    # Model 707 [Header @ hr:40474]: DER Trip LV
                    02C3 0069

                    # Model 707 [Data @ hr:40476 - hr:40580]: 105 registers
                    0001 0000 0000 0005 0002 FFFF FFFE 0001 0005 0000
                    0000 00C8 01F4 0000 00C8 01F4 0000 0834 0370 0000
                    0834 0370 0000 0898 FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    0002 01F4 0000 0000 01F4 0000 00C8 FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF 0000 0005 0000 0000
                    00C8 01F4 0000 00C8 01F4 0000 0834 0370 0000 0834
                    0370 0000 0898 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF 0002
                    01F4 0000 0000 01F4 0000 00C8 FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 708 [Header @ hr:40581]: DER Trip HV
                    02C4 0069

                    # Model 708 [Data @ hr:40583 - hr:40687]: 105 registers
                    0001 0000 0000 0005 0002 FFFF FFFE 0001 0005 04BA
                    0000 0010 04B0 0000 0010 04B0 0000 0514 044C 0000
                    0514 044C 0000 0578 FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    0002 044C 0000 0000 044C 0000 0514 FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF 0000 0005 04BA 0000
                    0010 04B0 0000 0010 04B0 0000 0514 044C 0000 0514
                    044C 0000 0578 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF 0002
                    044C 0000 0000 044C 0000 0514 FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 709 [Header @ hr:40688]: DER Trip LF
                    02C5 0087

                    # Model 709 [Data @ hr:40690 - hr:40824]: 135 registers
                    0001 0000 0000 0005 0002 FFFF FFFE 0001 0005 0000
                    01F4 0000 0010 0000 0235 0000 0010 0000 0235 0000
                    7530 0000 0249 0000 7530 0000 0249 0000 9C40 FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 0000 0005 0000 01F4 0000 0010 0000 0235 0000
                    0010 0000 0235 0000 7530 0000 0249 0000 7530 0000
                    0249 0000 9C40 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 710 [Header @ hr:40825]: DER Trip HF
                    02C6 0087

                    # Model 710 [Data @ hr:40827 - hr:40961]: 135 registers
                    0001 0000 0000 0005 0002 FFFF FFFE 0001 0005 0000
                    0276 0000 0010 0000 026C 0000 0010 0000 026C 0000
                    7530 0000 0264 0000 7530 0000 0264 0000 9C40 FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF 0000 0005 0000 0276 0000 0010 0000 026C 0000
                    0010 0000 026C 0000 7530 0000 0264 0000 7530 0000
                    0264 0000 9C40 FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                    FFFF FFFF FFFF FFFF FFFF

                    # --------------------------------------
                    # Model 711 [Header @ hr:40962]: DER Frequency Droop
                    02C7 002A

                    # Model 711 [Data @ hr:40964 - hr:41005]: 42 registers
                    0000 0000 0000 0003 0000 0000 0000 0000 0000 FFFD
                    FFFD FFFF 0000 001E 0000 001E 0028 0028 0000 003C
                    0019 0001 0000 001F 0000 001F 0029 0029 0000 0002
                    0019 0000 0000 001F 0000 001F 0029 0029 0000 0002
                    0019 0000

                    # --------------------------------------
                    # Model 712 [Header @ hr:41006]: DER Watt-Var
                    02C8 003C

                    # Model 712 [Data @ hr:41008 - hr:41067]: 60 registers
                    0000 0000 0000 0006 0003 0000 0000 0000 0000 0000
                    FFFF FFFF 0006 0003 0001 0001 0000 0000 0000 0000
                    0000 0000 00C8 0000 01F4 0000 03E8 FC18 0006 0003
                    0001 0000 0000 0000 0000 0000 0000 0000 00C9 0000
                    01F5 0000 03E8 FC18 0006 0003 0001 0000 0000 0000
                    0000 0000 0000 0000 00C9 0000 01F5 0000 03E8 FC18

                    # --------------------------------------
                    # Model 713 [Header @ hr:41068]: DER Storage Capacity
                    02C9 0007

                    # Model 713 [Data @ hr:41070 - hr:41076]: 7 registers
                    0000 0000 0000 0000 0000 0000 FFFF

                    # --------------------------------------
                    # Model 714 [Header @ hr:41077]: DER DC Measurement
                    02CA 0044

                    # Model 714 [Data @ hr:41079 - hr:41146]: 68 registers
                    0000 0000 0002 0906 2710 0000 0000 000A 9012 0000
                    0000 0000 0000 FFFE FFFF 0000 0000 FFFF 0000 0001
                    506F 7274 2031 0000 0000 0000 0000 0000 0320 128E
                    157C 0000 0000 0005 4809 0000 0000 0000 0000 00E6
                    0001 FFFF FFFF 0000 0002 506F 7274 2032 0000 0000
                    0000 0000 0000 0320 128E 157C 0000 0000 0005 4809
                    0000 0000 0000 0000 00E6 0001 FFFF FFFF

                    # --------------------------------------
                    # Model 64412 [Header @ hr:41147]: Unknown (vendor specific?) model. No fields available.
                    FB9C 002B

                    # Model 64412 [Data @ hr:41149 - hr:41191]: 43 registers
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000

                    # --------------------------------------
                    # NO MORE MODELS
                    FFFF 0000
                    """.trimIndent(),
                ).build()
}
