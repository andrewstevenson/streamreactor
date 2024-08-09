/*
 * Copyright 2017-2024 Lenses.io Ltd
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
package io.lenses.streamreactor.connect.azure.servicebus.sink;

import static io.lenses.streamreactor.common.util.AsciiArtPrinter.printAsciiHeader;
import static io.lenses.streamreactor.common.util.EitherUtils.unpackOrThrow;

import io.lenses.streamreactor.common.util.JarManifest;
import io.lenses.streamreactor.connect.azure.servicebus.config.AzureServiceBusConfigConstants;
import io.lenses.streamreactor.connect.azure.servicebus.config.AzureServiceBusSinkConfig;
import io.lenses.streamreactor.connect.azure.servicebus.util.KcqlConfigBusMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

/**
 * Sink Connector for Azure Service Bus.
 */
@Slf4j
public class AzureServiceBusSinkConnector extends SinkConnector {

  private final JarManifest jarManifest =
      unpackOrThrow(JarManifest
          .produceFromClass(getClass())
      );
  private Map<String, String> configProperties;

  @Override
  public void start(Map<String, String> props) {
    parseAndValidateConfigs(props);
    configProperties = props;
    printAsciiHeader(jarManifest, "/azure-servicebus-ascii.txt");
  }

  private void parseAndValidateConfigs(Map<String, String> props) {
    KcqlConfigBusMapper.mapKcqlsFromConfig(props.get(AzureServiceBusConfigConstants.KCQL_CONFIG), false)
        .fold(ex -> {
          throw ex;
        }, Function.identity());
  }

  @Override
  public Class<? extends Task> taskClass() {
    return AzureServiceBusSinkTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    log.info("Setting task configurations for {} workers.", maxTasks);
    Map<String, String> immutableProps = Map.copyOf(configProperties);

    return IntStream.range(0, maxTasks)
        .mapToObj(i -> immutableProps)
        .collect(Collectors.toList());
  }

  @Override
  public void stop() {
    // connector-specific implementation not needed
  }

  @Override
  public ConfigDef config() {
    return AzureServiceBusSinkConfig.getConfigDefinition();
  }

  @Override
  public String version() {
    return jarManifest.getVersion();
  }
}
