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

package nl.basjes.modbus

class Version {
    val gitCommitId              = GIT_COMMIT_ID
    val gitCommitIdDescribeShort = GIT_COMMIT_ID_DESCRIBE_SHORT
    val buildTimeStamp           = BUILD_TIME_STAMP
    val projectVersion           = PROJECT_VERSION
    val copyright                = COPYRIGHT
    val license                  = LICENSE
    val url                      = URL
    val buildKotlinVersion       = BUILD_KOTLIN_VERSION

    companion object {
        private val instance = nl.basjes.modbus.Version()

        const val GIT_COMMIT_ID                = "@git.commit.id@"
        const val GIT_COMMIT_ID_DESCRIBE_SHORT = "@git.commit.id.describe-short@"
        const val BUILD_TIME_STAMP             = "@project.build.outputTimestamp@"
        const val PROJECT_VERSION              = "@project.version@"
        const val COPYRIGHT                    = "@version.copyright@"
        const val LICENSE                      = "@version.license@"
        const val URL                          = "@version.url@"
        const val BUILD_KOTLIN_VERSION         = "@kotlin.version@"
    }
}
