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
package io.lenses.streamreactor.connect.gcp.pubsub.source.config;

import static io.lenses.streamreactor.common.util.EitherUtils.combineErrors;
import static io.lenses.streamreactor.connect.gcp.pubsub.source.configdef.PubSubKcqlConverter.KCQL_PROP_KEY_BATCH_SIZE;
import static io.lenses.streamreactor.connect.gcp.pubsub.source.configdef.PubSubKcqlConverter.KCQL_PROP_KEY_CACHE_TTL;
import static io.lenses.streamreactor.connect.gcp.pubsub.source.configdef.PubSubKcqlConverter.KCQL_PROP_KEY_QUEUE_MAX;

import java.util.List;

import org.apache.kafka.common.config.ConfigException;

import cyclops.control.Either;
import io.lenses.kcql.Kcql;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SourceConfigSettings holds the configuration for the PubSub connector.
 * It contains the gcpSettings and kcqlSettings.
 */
@Getter
@AllArgsConstructor
public class PubSubSourceConfig {

  private final PubSubConfig gcpSettings;

  private final List<Kcql> kcqlSettings;

  public Either<ConfigException, List<Kcql>> validateKcql() {
    return combineErrors(
        kcqlSettings
            .stream()
            .map(k -> k.validateKcqlProperties(
                KCQL_PROP_KEY_BATCH_SIZE,
                KCQL_PROP_KEY_CACHE_TTL,
                KCQL_PROP_KEY_QUEUE_MAX
            )
            )
    );
  }
}
