package io.lenses.streamreactor.connect.azure.eventhubs.source;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.lenses.streamreactor.connect.azure.eventhubs.source.TopicPartitionOffsetProvider.AzureOffsetMarker;
import io.lenses.streamreactor.connect.azure.eventhubs.source.TopicPartitionOffsetProvider.AzureTopicPartitionKey;
import java.util.Collections;
import java.util.Optional;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class AzureConsumerRebalancerListenerTest {

  private final boolean SEEK_TO_EARLIEST = false;
  private final boolean SEEK_TO_LATEST = true;

  @Test
  void onPartitionsAssignedShouldSeekToBeginningIfOffsetProviderProvidesEmptyOffsetAndSeekingToEarliest() {
    //given
    Consumer<String, String> stringKafkaConsumer = mock(Consumer.class);
    TopicPartitionOffsetProvider offsetProvider = mock(
        TopicPartitionOffsetProvider.class);
    AzureConsumerRebalancerListener testObj =
        new AzureConsumerRebalancerListener(offsetProvider, stringKafkaConsumer, SEEK_TO_EARLIEST);
    String topic = "topic1";
    Integer partition = 1;
    TopicPartition topicPartition1 = mock(TopicPartition.class);
    when(topicPartition1.topic()).thenReturn(topic);
    when(topicPartition1.partition()).thenReturn(partition);

    //when
    testObj.onPartitionsAssigned(Collections.singletonList(topicPartition1));

    //then
    verify(topicPartition1, times(1)).topic();
    verify(topicPartition1, times(1)).partition();
    verify(stringKafkaConsumer).seekToBeginning(anyList());
  }

  @Test
  void onPartitionsAssignedShouldSeekToEndIfOffsetProviderProvidesEmptyOffsetAndSeekingToLatest() {
    //given
    Consumer<String, String> stringKafkaConsumer = mock(Consumer.class);
    TopicPartitionOffsetProvider offsetProvider = mock(
        TopicPartitionOffsetProvider.class);
    AzureConsumerRebalancerListener testObj =
        new AzureConsumerRebalancerListener(offsetProvider, stringKafkaConsumer, SEEK_TO_LATEST);
    String topic = "topic1";
    Integer partition = 1;
    TopicPartition topicPartition1 = mock(TopicPartition.class);
    when(topicPartition1.topic()).thenReturn(topic);
    when(topicPartition1.partition()).thenReturn(partition);

    //when
    testObj.onPartitionsAssigned(Collections.singletonList(topicPartition1));

    //then
    verify(topicPartition1, times(1)).topic();
    verify(topicPartition1, times(1)).partition();
    verify(stringKafkaConsumer).seekToEnd(anyList());
  }

  @Test
  void onPartitionsAssignedShouldSeekToSpecificOffsetIfOffsetProviderProvidesIt() {
    //given
    Long specificOffset = 100L;
    Consumer<String, String> stringKafkaConsumer = mock(Consumer.class);
    TopicPartitionOffsetProvider offsetProvider = mock(
        TopicPartitionOffsetProvider.class);
    when(offsetProvider.getOffset(any(AzureTopicPartitionKey.class)))
        .thenReturn(Optional.of(new AzureOffsetMarker(specificOffset)));
    AzureConsumerRebalancerListener testObj =
        new AzureConsumerRebalancerListener(offsetProvider, stringKafkaConsumer, SEEK_TO_EARLIEST);
    String topic = "topic1";
    Integer partition = 1;
    TopicPartition topicPartition1 = mock(TopicPartition.class);
    when(topicPartition1.topic()).thenReturn(topic);
    when(topicPartition1.partition()).thenReturn(partition);

    //when
    testObj.onPartitionsAssigned(Collections.singletonList(topicPartition1));

    //then
    verify(topicPartition1, times(1)).topic();
    verify(topicPartition1, times(1)).partition();
    verify(stringKafkaConsumer).seek(topicPartition1, specificOffset);
  }
}