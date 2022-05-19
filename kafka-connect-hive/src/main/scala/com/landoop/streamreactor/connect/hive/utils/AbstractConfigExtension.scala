package com.landoop.streamreactor.connect.hive.utils

import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigException

object AbstractConfigExtension {

  implicit class AbstractConfigExtensions(val config: AbstractConfig) extends AnyVal {
    def getStringOrThrowIfNull(key: String): String =
      Option(config.getString(key))
        .getOrElse {
          throw new ConfigException(s"Missing the configuration for [$key].")
        }

    def getPasswordOrThrowIfNull(key: String): String =
      Option(config.getPassword(key))
        .map(_.value())
        .getOrElse {
          throw new ConfigException(s"Missing the configuration for [$key].")
        }
  }

}
