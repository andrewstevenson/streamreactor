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
package io.lenses.streamreactor.connect.gcp.common.config;

import org.apache.kafka.common.config.ConfigDef;

import io.lenses.streamreactor.common.config.base.ConfigSettings;
import io.lenses.streamreactor.common.config.base.RetryConfig;
import io.lenses.streamreactor.common.config.base.model.ConnectorPrefix;
import io.lenses.streamreactor.common.config.source.ConfigSource;
import io.lenses.streamreactor.connect.gcp.common.auth.GCPConnectionConfig;
import io.lenses.streamreactor.connect.gcp.common.auth.HttpTimeoutConfig;
import lombok.Getter;
import lombok.val;

/**
 * Configuration settings for connecting to Google Cloud Platform (GCP) services.
 * This class provides methods for defining and parsing GCP-specific configuration properties.
 */
@Getter
public class GCPSettings implements ConfigSettings<GCPConnectionConfig> {

  public static final String EMPTY_STRING = "";

  private final String gcpProjectIdKey;
  private final String gcpQuotaProjectIdKey;
  private final String hostKey;
  private final String httpErrorRetryIntervalKey;
  private final String httpNbrOfRetriesKey;
  private final String httpSocketTimeoutKey;
  private final String httpConnectionTimeoutKey;

  public static final Long HTTP_ERROR_RETRY_INTERVAL_DEFAULT = 50L;
  public static final Integer HTTP_NUMBER_OF_RETIRES_DEFAULT = 5;
  public static final Long HTTP_SOCKET_TIMEOUT_DEFAULT = 60000L;
  public static final Long HTTP_CONNECTION_TIMEOUT_DEFAULT = 60000L;

  private final AuthModeSettings authModeSettings;

  /**
   * Constructs a new instance of {@code GCPSettings} with the specified connector prefix.
   *
   * @param connectorPrefix the prefix used for configuration keys
   */
  public GCPSettings(ConnectorPrefix connectorPrefix) {
    gcpProjectIdKey = connectorPrefix.prefixKey("gcp.project.id");
    gcpQuotaProjectIdKey = connectorPrefix.prefixKey("gcp.quota.project.id");
    hostKey = connectorPrefix.prefixKey("endpoint");
    httpErrorRetryIntervalKey = connectorPrefix.prefixKey("http.retry.interval");
    httpNbrOfRetriesKey = connectorPrefix.prefixKey("http.max.retries");
    httpSocketTimeoutKey = connectorPrefix.prefixKey("http.socket.timeout");
    httpConnectionTimeoutKey = connectorPrefix.prefixKey("http.connection.timeout");

    authModeSettings = new AuthModeSettings(connectorPrefix);
  }

  /**
   * Configures the provided {@link ConfigDef} with GCP-specific settings.
   *
   * @param configDef the base configuration definition to extend
   * @return the updated {@link ConfigDef} with GCP-specific settings
   */
  @Override
  public ConfigDef withSettings(ConfigDef configDef) {
    val conf =
        configDef
            .define(
                gcpProjectIdKey,
                ConfigDef.Type.STRING,
                EMPTY_STRING,
                ConfigDef.Importance.HIGH,
                "GCP Project ID")
            .define(
                gcpQuotaProjectIdKey,
                ConfigDef.Type.STRING,
                EMPTY_STRING,
                ConfigDef.Importance.HIGH,
                "GCP Quota Project ID")
            .define(hostKey, ConfigDef.Type.STRING, EMPTY_STRING, ConfigDef.Importance.LOW, "GCP Host")
            .define(
                httpNbrOfRetriesKey,
                ConfigDef.Type.INT,
                HTTP_NUMBER_OF_RETIRES_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                "Number of times to retry the http request, in the case of a resolvable error on"
                    + " the server side.",
                "Error",
                2,
                ConfigDef.Width.LONG,
                httpNbrOfRetriesKey)
            .define(
                httpErrorRetryIntervalKey,
                ConfigDef.Type.LONG,
                HTTP_ERROR_RETRY_INTERVAL_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                "If greater than zero, used to determine the delay after which to retry the http"
                    + " request in milliseconds.  Based on an exponential backoff algorithm.",
                "Error",
                3,
                ConfigDef.Width.LONG,
                httpErrorRetryIntervalKey)
            .define(
                httpSocketTimeoutKey,
                ConfigDef.Type.LONG,
                HTTP_SOCKET_TIMEOUT_DEFAULT,
                ConfigDef.Importance.LOW,
                "Socket timeout (ms)")
            .define(
                httpConnectionTimeoutKey,
                ConfigDef.Type.LONG,
                HTTP_CONNECTION_TIMEOUT_DEFAULT,
                ConfigDef.Importance.LOW,
                "Connection timeout (ms)");

    return authModeSettings.withSettings(conf);
  }

  public GCPConnectionConfig parseFromConfig(ConfigSource configSource) {
    val builder =
        GCPConnectionConfig.builder().authMode(authModeSettings.parseFromConfig(configSource));
    configSource.getString(gcpProjectIdKey).ifPresent(builder::projectId);
    configSource.getString(gcpQuotaProjectIdKey).ifPresent(builder::quotaProjectId);
    configSource.getString(hostKey).ifPresent(builder::host);

    val retryConfig =
        new RetryConfig(
            configSource.getInt(httpNbrOfRetriesKey).orElse(HTTP_NUMBER_OF_RETIRES_DEFAULT),
            configSource.getLong(httpErrorRetryIntervalKey).orElse(HTTP_ERROR_RETRY_INTERVAL_DEFAULT));

    val timeoutConfig =
        new HttpTimeoutConfig(
            configSource.getLong(httpSocketTimeoutKey).orElse(null),
            configSource.getLong(httpConnectionTimeoutKey).orElse(null));

    builder.httpRetryConfig(retryConfig);
    builder.timeouts(timeoutConfig);
    return builder.build();
  }
}
