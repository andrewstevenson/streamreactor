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

package com.datamountaineer.streamreactor.connect.pulsar.sink

import com.datamountaineer.kcql.{Field, Kcql}
import com.datamountaineer.streamreactor.common.converters.{FieldConverter, ToJsonWithProjections}
import com.datamountaineer.streamreactor.common.errors.ErrorHandler
import com.datamountaineer.streamreactor.connect.pulsar.ProducerConfigFactory
import com.datamountaineer.streamreactor.connect.pulsar.config.PulsarSinkSettings
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.connect.sink.SinkRecord
import org.apache.pulsar.client.api._
import org.apache.pulsar.client.impl.auth.AuthenticationTls

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}
import scala.util.Try


object PulsarWriter {
  def apply(name: String, settings: PulsarSinkSettings): PulsarWriter = {
    val clientConf = new ClientConfiguration()

    settings.sslCACertFile.foreach(f => {
      clientConf.setUseTls(true)
      clientConf.setTlsTrustCertsFilePath(f)

      val authParams = settings.sslCertFile.map(f => ("tlsCertFile", f)).toMap ++ settings.sslCertKeyFile.map(f => ("tlsKeyFile", f)).toMap
      clientConf.setAuthentication(classOf[AuthenticationTls].getName, authParams.asJava)
    })

    lazy val client = PulsarClient.create(settings.connection, clientConf)
    new PulsarWriter(client, name, settings)
  }
}

case class PulsarWriter(client: PulsarClient, name: String, settings: PulsarSinkSettings) extends StrictLogging with ErrorHandler {
  //initialize error tracker
  initialize(settings.maxRetries, settings.errorPolicy)

  private val producersMap = scala.collection.mutable.Map.empty[String, Producer]
  val msgFactory = PulsarMessageBuilder(settings)
  val configs = ProducerConfigFactory(name, settings.kcql)


  def write(records: Iterable[SinkRecord]) = {
    val messages = msgFactory.create(records)

    val t = Try{
      messages.foreach{
        case (topic, message) =>
          val producer = producersMap.getOrElseUpdate(topic, client.createProducer(topic, configs(topic)))
          producer.send(message)
      }
    }

    handleTry(t)
  }

  def flush(): Unit = {}

  def close(): Unit = {
    logger.info("Closing client")
    producersMap.foreach({ case (_, producer) => producer.close()})
    client.close()
  }
}

case class PulsarMessageBuilder(settings: PulsarSinkSettings) extends StrictLogging with ErrorHandler {

  private val mappings: Map[String, Set[Kcql]] = settings.kcql.groupBy(k => k.getSource)

  @nowarn
  def create(records: Iterable[SinkRecord]): Iterable[(String, Message)] = {
    records.flatMap{ record =>
      val topic = record.topic()
      //get the kcql statements for this topic
      val kcqls = mappings(topic)
      kcqls.map { k =>
        val pulsarTopic = k.getTarget

        //optimise this via a map
        val fields = k.getFields.asScala.map(FieldConverter.apply)
        val ignoredFields = k.getIgnoredFields.asScala.map(FieldConverter.apply)
        //for all the records in the group transform

        val json = ToJsonWithProjections(
          fields.toSeq,
          ignoredFields.toSeq,
          record.valueSchema(),
          record.value(),
          k.hasRetainStructure
        )

        val recordTime = if (record.timestamp() != null) record.timestamp().longValue() else System.currentTimeMillis()

        val msg = MessageBuilder
                    .create
                    .setContent(json.toString.getBytes)
                    .setEventTime(recordTime)


        if (k.getWithKeys != null && k.getWithKeys().size() > 0) {
          val parentFields = null

          // Get the fields to construct the key for pulsar
          val (partitionBy, schema, value) = if (k.getWithKeys != null && k.getWithKeys().size() > 0) {
            (k.getWithKeys.asScala.map(f => Field.from(f, f, parentFields)),
              if (record.key() != null) record.keySchema() else record.valueSchema(),
              if (record.key() != null) record.key() else record.value()
            )
          }
          else {
            (Seq(Field.from("*", "*", parentFields)),
              if (record.key() != null) record.keySchema() else record.valueSchema(),
              if (record.key() != null) record.key() else record.value())
          }

          val keyFields = partitionBy.map(FieldConverter.apply)

          val jsonKey = ToJsonWithProjections(
            keyFields.toSeq,
            List.empty[Field].map(FieldConverter.apply),
            schema,
            value,
            k.hasRetainStructure
          )
          msg.setKey(jsonKey.toString)
        }

        val built = msg.build()
        (pulsarTopic, built)
      }
    }
  }
}
