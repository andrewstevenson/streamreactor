package io.lenses.streamreactor.connect.testcontainers.scalatest.fixtures

import io.debezium.testing.testcontainers.ConnectorConfiguration
import io.lenses.streamreactor.connect.testcontainers.connect.KafkaConnectClient

object connect {

  def withConnector(
    name:            String,
    connectorConfig: ConnectorConfiguration,
    timeoutSeconds:  Long = 10L,
  )(testCode:        => Any,
  )(
    implicit
    kafkaConnectClient: KafkaConnectClient,
  ): Unit = {
    kafkaConnectClient.registerConnector(name, connectorConfig)
    kafkaConnectClient.waitConnectorInRunningState(name, timeoutSeconds)
    testCode
    kafkaConnectClient.deleteConnector(name)
  }
}
