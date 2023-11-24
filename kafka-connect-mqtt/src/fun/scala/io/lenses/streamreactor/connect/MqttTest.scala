package io.lenses.streamreactor.connect

import _root_.io.lenses.streamreactor.connect.testcontainers.connect._
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import _root_.io.lenses.streamreactor.connect.mqtt.config.MqttConfigConstants._
import _root_.io.confluent.kafka.serializers.KafkaJsonSerializer
import _root_.io.lenses.streamreactor.connect.model.Order
import _root_.io.lenses.streamreactor.connect.testcontainers.SchemaRegistryContainer
import _root_.io.lenses.streamreactor.connect.testcontainers.connect.KafkaConnectClient.createConnector
import _root_.io.lenses.streamreactor.connect.testcontainers.scalatest.StreamReactorContainerPerSuite
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class MqttTest extends AsyncFlatSpec with AsyncIOSpec with StreamReactorContainerPerSuite with Matchers {

  lazy val container: MqttContainer = MqttContainer().withNetwork(network)

  override val schemaRegistryContainer: Option[SchemaRegistryContainer] = None

  override val connectorModule: String = "mqtt"

  override def beforeAll(): Unit = {
    container.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    container.stop()
  }

  behavior of "mqtt connector"
  val topicName = "orders"

  it should "sink records using json" in {

    val resources = for {
      mqttClientAndPayload <- MqttClientResource(
        container.getExtMqttConnectionUrl,
        container.mqttUser,
        container.mqttPassword,
        topicName,
      )
      producer <- createProducer[String, Order](classOf[StringSerializer], classOf[KafkaJsonSerializer[Order]])
      _        <- createConnector(sinkConfig)
    } yield (mqttClientAndPayload, producer)
    resources.use {
      case (fnLatestPayload, producer: KafkaProducer[String, Order]) =>
        IO {
          writeRecordToTopic(producer)
          eventually {
            fnLatestPayload().getOrElse(fail("not yet"))
          }
        }
    }.asserting(_ should be(
      """{"id":1,"product":"OP-DAX-P-20150201-95.7","price":94.2,"qty":100,"created":null}""",
    ))
  }

  private def writeRecordToTopic(producer: KafkaProducer[String, Order]): Unit = {
    val order = Order(1, "OP-DAX-P-20150201-95.7", 94.2, 100)
    producer.send(new ProducerRecord[String, Order]("orders", order)).get
    producer.flush()
  }

  private def sinkConfig: ConnectorConfiguration =
    ConnectorConfiguration(
      "mqtt-sink",
      Map(
        "connector.class"          -> ConfigValue("io.lenses.streamreactor.connect.mqtt.sink.MqttSinkConnector"),
        "tasks.max"                -> ConfigValue(1),
        "topics"                   -> ConfigValue("orders"),
        KCQL_CONFIG                -> ConfigValue(s"INSERT INTO orders SELECT * FROM orders"),
        HOSTS_CONFIG               -> ConfigValue(container.getMqttConnectionUrl),
        QS_CONFIG                  -> ConfigValue(1),
        CLEAN_SESSION_CONFIG       -> ConfigValue(true),
        CLIENT_ID_CONFIG           -> ConfigValue(UUID.randomUUID().toString),
        CONNECTION_TIMEOUT_CONFIG  -> ConfigValue(1000),
        KEEP_ALIVE_INTERVAL_CONFIG -> ConfigValue(1000),
        PASSWORD_CONFIG            -> ConfigValue(container.mqttPassword),
        USER_CONFIG                -> ConfigValue(container.mqttUser),
      ),
    )

}
