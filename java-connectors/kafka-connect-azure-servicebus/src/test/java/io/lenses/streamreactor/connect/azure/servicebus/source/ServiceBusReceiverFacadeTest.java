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
package io.lenses.streamreactor.connect.azure.servicebus.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.lenses.kcql.Kcql;
import io.lenses.streamreactor.connect.azure.servicebus.util.ServiceBusKcqlProperties;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.Test;

class ServiceBusReceiverFacadeTest {

  private final BlockingQueue<SourceRecord> mockedQueue = mock(BlockingQueue.class);
  private final String connectionString =
      "Endpoint=sb://TESTENDPOINT.servicebus.windows.net/;"
          + "SharedAccessKeyName=EXAMPLE_NAME;SharedAccessKey=EXAMPLE_KEY";

  @Test
  void checkReceiverInitializationAndReceiverIdForQueue() {
    //given
    String queueType = "QUEUE";
    Map<String, String> propertiesMap = mock(Map.class);
    when(propertiesMap.get(ServiceBusKcqlProperties.SERVICE_BUS_TYPE.getPropertyName())).thenReturn(queueType);

    String inputBusName = "INPUT";
    Kcql kcql = mock(Kcql.class);
    when(kcql.getSource()).thenReturn(inputBusName);
    when(kcql.getProperties()).thenReturn(propertiesMap);

    ServiceBusReceiverFacade testObj = new ServiceBusReceiverFacade(kcql, mockedQueue, connectionString);

    //when
    String receiverId = testObj.getReceiverId();

    //then
    verify(kcql).getProperties();
    verify(kcql).getSource();
    verify(propertiesMap).get(ServiceBusKcqlProperties.SERVICE_BUS_TYPE.getPropertyName());
    assertThat(receiverId).startsWith(ServiceBusReceiverFacade.class.getSimpleName());
  }

  @Test
  void checkReceiverInitializationAndReceiverIdForTopic() {
    //given
    String topicType = "TOPIC";
    String subscriptionName = "SUBSCRIPTION";
    Map<String, String> propertiesMap = mock(Map.class);
    when(propertiesMap.get(ServiceBusKcqlProperties.SERVICE_BUS_TYPE.getPropertyName())).thenReturn(topicType);
    when(propertiesMap.get(ServiceBusKcqlProperties.SUBSCRIPTION_NAME.getPropertyName())).thenReturn(subscriptionName);

    String inputBusName = "INPUT";
    Kcql kcql = mock(Kcql.class);
    when(kcql.getSource()).thenReturn(inputBusName);
    when(kcql.getProperties()).thenReturn(propertiesMap);

    ServiceBusReceiverFacade testObj = new ServiceBusReceiverFacade(kcql, mockedQueue, connectionString);

    //when
    String receiverId = testObj.getReceiverId();

    //then
    verify(kcql).getProperties();
    verify(kcql).getSource();
    verify(propertiesMap).get(ServiceBusKcqlProperties.SERVICE_BUS_TYPE.getPropertyName());
    verify(propertiesMap).get(ServiceBusKcqlProperties.SUBSCRIPTION_NAME.getPropertyName());
    assertThat(receiverId).startsWith(ServiceBusReceiverFacade.class.getSimpleName());
  }

  @Test
  void shouldGetIllegalArgumentExceptionOnCreationWithBadlyFormattedConnectionString() {
    //given
    Kcql kcql = mock(Kcql.class);
    String badFormatConnectionString = "connectionString";

    //when
    assertThrows(IllegalArgumentException.class, () -> new ServiceBusReceiverFacade(kcql, mockedQueue,
        badFormatConnectionString)
    );
  }
}
