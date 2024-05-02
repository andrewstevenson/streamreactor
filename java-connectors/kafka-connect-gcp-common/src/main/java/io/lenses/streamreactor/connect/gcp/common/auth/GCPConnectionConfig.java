/*
 * Copyright 2017-2024 Lenses.io Ltd
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
package io.lenses.streamreactor.connect.gcp.common.auth;

import static io.lenses.streamreactor.connect.gcp.common.config.GCPSettings.*;

import io.lenses.streamreactor.common.config.base.RetryConfig;
import io.lenses.streamreactor.common.config.base.intf.ConnectionConfig;
import io.lenses.streamreactor.connect.gcp.common.auth.mode.AuthMode;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GCPConnectionConfig implements ConnectionConfig {

  @Nullable private String projectId;
  @Nullable private String quotaProjectId;
  @Nonnull private AuthMode authMode;
  @Nullable private String host;

  @Nonnull @Builder.Default
  private RetryConfig httpRetryConfig =
      RetryConfig.builder()
          .retryLimit(HTTP_NUMBER_OF_RETIRES_DEFAULT)
          .retryIntervalMillis(HTTP_ERROR_RETRY_INTERVAL_DEFAULT)
          .build();

  @Nonnull @Builder.Default
  private HttpTimeoutConfig timeouts = HttpTimeoutConfig.builder().build();
}
