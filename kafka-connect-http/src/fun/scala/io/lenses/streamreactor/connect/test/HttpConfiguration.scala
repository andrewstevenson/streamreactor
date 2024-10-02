package io.lenses.streamreactor.connect.test

import _root_.io.lenses.streamreactor.connect.testcontainers.connect._
import com.typesafe.scalalogging.LazyLogging
import io.lenses.streamreactor.connect.http.sink.client.HttpMethod
import io.lenses.streamreactor.connect.http.sink.config.HttpSinkConfigDef

trait HttpConfiguration extends LazyLogging {

  val ERROR_REPORTING_ENABLED_PROP   = "connect.reporting.error.config.enabled";
  val SUCCESS_REPORTING_ENABLED_PROP = "connect.reporting.success.config.enabled";

  def sinkConfig(
    randomTestId:    String,
    endpointUrl:     String,
    httpMethod:      String,
    contentTemplate: String,
    headerTemplates: Seq[(String, String)],
    topicName:       String,
    converters:      Map[String, String],
    batchSize:       Int,
    jsonTidy:        Boolean,
  ): ConnectorConfiguration = {
    val configMap: Map[String, ConfigValue[_]] = converters.view.mapValues(new ConfigValue[String](_)).toMap ++
      Map(
        "connector.class"                        -> ConfigValue("io.lenses.streamreactor.connect.http.sink.HttpSinkConnector"),
        "tasks.max"                              -> ConfigValue(1),
        "topics"                                 -> ConfigValue(topicName),
        HttpSinkConfigDef.HttpMethodProp         -> ConfigValue(HttpMethod.withNameInsensitive(httpMethod).toString),
        HttpSinkConfigDef.HttpEndpointProp       -> ConfigValue(endpointUrl),
        HttpSinkConfigDef.HttpRequestContentProp -> ConfigValue(contentTemplate),
        HttpSinkConfigDef.HttpRequestHeadersProp -> ConfigValue(headerTemplates.mkString(",")),
        HttpSinkConfigDef.AuthenticationTypeProp -> ConfigValue("none"), //NoAuthentication
        HttpSinkConfigDef.BatchCountProp         -> ConfigValue(batchSize),
        HttpSinkConfigDef.JsonTidyProp           -> ConfigValue(jsonTidy),
        ERROR_REPORTING_ENABLED_PROP             -> ConfigValue("false"),
        SUCCESS_REPORTING_ENABLED_PROP           -> ConfigValue("false"),
      )
    debugLogConnectorConfig(configMap)
    ConnectorConfiguration(
      "connector" + randomTestId,
      configMap,
    )
  }

  private def debugLogConnectorConfig(configMap: Map[String, ConfigValue[_]]): Unit = {
    logger.debug("Creating connector with configuration:")
    configMap.foreachEntry {
      case (k, v) => logger.debug(s"    $k => ${v.underlying}")
    }
    logger.debug(s"End connector config.")
  }
}
