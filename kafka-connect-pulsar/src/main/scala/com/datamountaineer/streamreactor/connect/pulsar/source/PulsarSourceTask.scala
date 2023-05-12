/*
 * Copyright 2017-2023 Lenses.io Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datamountaineer.streamreactor.connect.pulsar.source

import com.datamountaineer.streamreactor.common.utils.AsciiArtPrinter.printAsciiHeader
import com.datamountaineer.streamreactor.common.utils.JarManifest
import com.datamountaineer.streamreactor.common.utils.ProgressCounter
import com.datamountaineer.streamreactor.connect.converters.source.Converter
import com.datamountaineer.streamreactor.connect.pulsar.config.PulsarConfigConstants
import com.datamountaineer.streamreactor.connect.pulsar.config.PulsarSourceConfig
import com.datamountaineer.streamreactor.connect.pulsar.config.PulsarSourceSettings
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.auth.AuthenticationTls

import java.util
import java.util.UUID
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class PulsarSourceTask extends SourceTask with StrictLogging {
  private val progressCounter = new ProgressCounter
  private var enableProgress: Boolean               = false
  private var pulsarManager:  Option[PulsarManager] = None
  private val manifest = JarManifest(getClass.getProtectionDomain.getCodeSource.getLocation)

  @nowarn
  override def start(props: util.Map[String, String]): Unit = {
    printAsciiHeader(manifest, "/pulsar-source-ascii.txt")

    val conf = if (context.configs().isEmpty) props else context.configs()

    implicit val settings = PulsarSourceSettings(PulsarSourceConfig(conf), props.getOrDefault("tasks.max", "1").toInt)

    val name          = conf.getOrDefault("name", s"kafka-connect-pulsar-source-${UUID.randomUUID().toString}")
    val convertersMap = buildConvertersMap(conf, settings)

    val messageConverter = PulsarMessageConverter(
      convertersMap,
      settings.kcql,
      settings.throwOnConversion,
      settings.pollingTimeout,
      settings.batchSize,
    )

    val clientConf = PulsarClient.builder()

    settings.sslCACertFile.foreach { f =>
      val authParams = settings.sslCertFile.map(f => ("tlsCertFile", f)).toMap ++ settings.sslCertKeyFile.map(f =>
        ("tlsKeyFile", f),
      ).toMap

      clientConf.enableTls(true)
        .tlsTrustCertsFilePath(f)
        .authentication(classOf[AuthenticationTls].getName, authParams.asJava)
    }

    pulsarManager =
      Some(new PulsarManager(clientConf.serviceUrl(settings.connection).build(), name, settings.kcql, messageConverter))
    enableProgress = settings.enableProgress
  }

  def buildConvertersMap(props: util.Map[String, String], settings: PulsarSourceSettings): Map[String, Converter] =
    settings.sourcesToConverters.map {
      case (topic, clazz) =>
        logger.info(s"Creating converter instance for $clazz")
        val converter = Try(Class.forName(clazz).getDeclaredConstructor().newInstance()) match {
          case Success(value) => value.asInstanceOf[Converter]
          case Failure(_) =>
            throw new ConfigException(
              s"Invalid ${PulsarConfigConstants.KCQL_CONFIG} is invalid. $clazz should have an empty ctor!",
            )
        }
        converter.initialize(props.asScala.toMap)
        topic -> converter
    }

  /**
    * Get all the messages accumulated so far.
    */
  override def poll(): util.List[SourceRecord] = {

    val records = pulsarManager.map { manager =>
      val list = new util.ArrayList[SourceRecord]()
      manager.getRecords(list)
      list
    }.orNull

    if (enableProgress) {
      progressCounter.update(records.asScala.toVector)
    }
    records
  }

  /**
    * Shutdown connections
    */
  override def stop(): Unit = {
    logger.info("Stopping Pulsar source.")
    pulsarManager.foreach(_.close())
    progressCounter.empty()
  }

  override def version: String = manifest.version()
}
