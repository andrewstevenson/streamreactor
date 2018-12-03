package com.datamountaineer.streamreactor.connect.rabbitmq.config

import com.datamountaineer.streamreactor.connect.config.base.const.TraitConfigConst._

object RabbitMQConfigConstants {
    val CONNECTOR_PREFIX = "connect.rabbitmq"

    //Default Configuration
    val HOST_CONFIG = s"${CONNECTOR_PREFIX}.${CONNECTION_HOST_SUFFIX}"
    val HOST_DOC = "Contains the RabbitMQ connection end points."
    val HOST_DISPLAY = "RabbitMQ connection endpoints"

    val TOPIC_CONFIG = s"${CONNECTOR_PREFIX}.topic"
    val TOPIC_DOC = "Kafka topic"
    val TOPIC_DISPLAY = "Kafka topic"

    val KCQL_CONFIG = s"${CONNECTOR_PREFIX}.${KCQL_PROP_SUFFIX}"
    val KCQL_DOC = "Contains the Kafka Connect Query Language describing the sourced RabbitMQ source and the target Kafka topics"
    val KCQL_DISPLAY = "KCQL commands"

    //Optional Configuration
    val USER_CONFIG = s"${CONNECTOR_PREFIX}.${USERNAME_SUFFIX}"
    val USER_DOC = "Contains the RabbitMQ connection user name"
    val USER_DISPLAY = "Username"
    val USER_DEFAULT = "guest"

    val PASSWORD_CONFIG = s"${CONNECTOR_PREFIX}.${PASSWORD_SUFFIX}"
    val PASSWORD_DOC = "Contains the RabbitMQ connection password"
    val PASSWORD_DISPLAY = "Password"
    val PASSWORD_DEFAULT = "guest"

    val PORT_CONFIG = s"${CONNECTOR_PREFIX}.${CONNECTION_PORT_SUFFIX}"
    val PORT_DOC = "Contains the RabbitMQ Server port"
    val PORT_DISPLAY = "Port"
    val PORT_DEFAULT = 5672

    val VIRTUAL_HOST_CONFIG = s"${CONNECTOR_PREFIX}.virtual.host"
    val VIRTUAL_HOST_DOC = "Endpoint of the broker"
    val VIRTUAL_HOST_DISPLAY = "Virtual Host"
    val VIRTUAL_HOST_DEFAULT = "/"

    val POLLING_TIMEOUT_CONFIG = s"${CONNECTOR_PREFIX}.polling.timeout"
    val POLLING_TIMEOUT_DOC = "Provides the timeout to poll incoming messages"
    val POLLING_TIMEOUT_DISPLAY = "Polling timeout"
    val POLLING_TIMEOUT_DEFAULT = 1000

    object ConfigGroups {
        val CONNECTION = "Connection"
    }
}
