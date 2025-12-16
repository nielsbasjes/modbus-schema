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

object DeviceSolarEdgeDated20191001 {
    @JvmStatic
    val device: MockedModbusDevice
        get() =
            // Extracted on 2019-10-01 from a Solar Edge device
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
                    0001 0041 # THIS IS ABNORMAL.
                              # The size should be 66 (0x0042) registers instead of 65 (0x0041).
                              # The effect is the padding at the end becomes the same as the model id of the next model.

                    # Model 1 [Data @ hr:40004 - hr:40068]: 65 registers
                    536F 6C61 7245 6467 6520 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 5345 3330 3030 482D
                    5257 3030 3042 4E4E 3400 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                    3030 3034 2E30 3030 362E 3030 3234 0000 3733 3141
                    3242 3730 0000 0000 0000 0000 0000 0000 0000 0000
                    0000 0000 0000 0000 0001

                    # --------------------------------------
                    # Model 101 [Header @ hr:40069]: Inverter (Single Phase)
                    0065 0032

                    # Model 101 [Data @ hr:40071 - hr:40120]: 50 registers
                    00DC 00DC FFFF FFFF FFFE 0967 FFFF FFFF FFFF FFFF
                    FFFF FFFF 13E4 FFFF C356 FFFD 14C1 FFFF 3B46 FFFE
                    256F FFFE 0000 450F 0000 34A5 FFFC 0EFC FFFF 1432
                    FFFF 8000 0EE6 8000 8000 FFFE 0004 0000 FFFF FFFF
                    FFFF FFFF 0000 0000 FFFF FFFF FFFF FFFF 0000 0000

                    # --------------------------------------
                    # NO MORE MODELS
                    FFFF 0000
                    """.trimIndent(),
                ).build()
}
