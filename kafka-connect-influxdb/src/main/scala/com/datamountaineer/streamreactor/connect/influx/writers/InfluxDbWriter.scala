/*
 * Copyright 2017 Datamountaineer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datamountaineer.streamreactor.connect.influx.writers

import com.datamountaineer.streamreactor.common.errors.ErrorHandler
import com.datamountaineer.streamreactor.common.sink.DbWriter
import com.datamountaineer.streamreactor.connect.influx.config.InfluxSettings
import com.datamountaineer.streamreactor.connect.influx.{NanoClock, ValidateStringParameterFn}
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.connect.sink.SinkRecord
import org.influxdb.InfluxDBFactory

import scala.util.Try

class InfluxDbWriter(settings: InfluxSettings) extends DbWriter with StrictLogging with ErrorHandler {

  ValidateStringParameterFn(settings.connectionUrl, "settings")
  ValidateStringParameterFn(settings.user, "settings")

  //initialize error tracker
  initialize(settings.maxRetries, settings.errorPolicy)
  private val influxDB = InfluxDBFactory.connect(settings.connectionUrl, settings.user, settings.password)
  private val builder = new InfluxBatchPointsBuilder(settings, new NanoClock())

  override def write(records: Seq[SinkRecord]): Unit = {
    if (records.isEmpty) {
      logger.debug("No records received.")
    } else {
      val _ = handleTry(
        builder
          .build(records)
          .flatMap { batchPoints =>
            logger.debug(s"Writing ${batchPoints.getPoints.size()} points to the database...")
            Try(influxDB.write(batchPoints))
          }.map(_ => logger.debug("Writing complete")))
    }
  }

  override def close(): Unit = {}
}



