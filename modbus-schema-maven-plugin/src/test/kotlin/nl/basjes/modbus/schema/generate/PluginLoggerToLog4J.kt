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
package nl.basjes.modbus.schema.generate

import org.apache.logging.log4j.LogManager
import org.apache.maven.plugin.logging.Log

class PluginLoggerToLog4J : Log {

    private val logger = LogManager.getLogger(PluginLoggerToLog4J::class.java)
    override fun isDebugEnabled(): Boolean= logger.isDebugEnabled

    override fun debug(msg: CharSequence?) {
        logger.debug(msg.toString())
    }

    override fun debug(msg: CharSequence?, throwable: Throwable?) {
        logger.debug(msg.toString(), throwable)
    }

    override fun debug(throwable: Throwable?) {
        logger.debug("Throwable happened", throwable)
    }

    override fun isInfoEnabled(): Boolean= logger.isInfoEnabled

    override fun info(msg: CharSequence?) {
        logger.info(msg.toString())
    }

    override fun info(msg: CharSequence?, throwable: Throwable?) {
        logger.info(msg.toString(), throwable)
    }

    override fun info(throwable: Throwable?) {
        logger.info("Throwable happened", throwable)
    }

    override fun isWarnEnabled(): Boolean= logger.isWarnEnabled

    override fun warn(msg: CharSequence?) {
        logger.warn(msg.toString())
    }

    override fun warn(msg: CharSequence?, throwable: Throwable?) {
        logger.warn(msg.toString(), throwable)
    }

    override fun warn(throwable: Throwable?) {
        logger.warn("Throwable happened", throwable)
    }

    override fun isErrorEnabled(): Boolean= logger.isErrorEnabled

    override fun error(msg: CharSequence?) {
        logger.error(msg.toString())
    }

    override fun error(msg: CharSequence?, throwable: Throwable?) {
        logger.error(msg.toString(), throwable)
    }

    override fun error(throwable: Throwable?) {
        logger.error("Throwable happened", throwable)
    }

}
