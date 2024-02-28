package io.lenses.java.streamreactor.connect.azure.eventhubs.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.lenses.java.streamreactor.connect.azure.eventhubs.source.TopicPartitionOffsetProvider.AzureOffsetMarker;
import io.lenses.java.streamreactor.connect.azure.eventhubs.source.TopicPartitionOffsetProvider.AzureTopicPartitionKey;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TopicPartitionOffsetProviderTest {

  @BeforeEach
  public void resetSingletonInstance() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    Field instance = TopicPartitionOffsetProvider.class.getDeclaredField("instance");
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  void getInstanceWithoutInitializeShouldReturnEmptyOptional() {
    //given

    //when
    Optional<TopicPartitionOffsetProvider> instance = TopicPartitionOffsetProvider.getInstance();

    //then
    assertTrue(instance.isEmpty());
  }

  @Test
  void getInstanceWithInitializeShouldReturnInitializedInstance() {
    //given
    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);

    //when
    TopicPartitionOffsetProvider.initialize(offsetStorageReader);
    Optional<TopicPartitionOffsetProvider> instance = TopicPartitionOffsetProvider.getInstance();

    //then
    assertFalse(instance.isEmpty());
  }

  @Test
  void getOffsetShouldCallOffsetStorageReader() {
    //given
    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);
    TopicPartitionOffsetProvider.initialize(offsetStorageReader);
    String topic = "some_topic";
    Integer partition = 1;

    //when
    Optional<TopicPartitionOffsetProvider> instance = TopicPartitionOffsetProvider.getInstance();
    assertFalse(instance.isEmpty());
    AzureTopicPartitionKey azureTopicPartitionKey = new AzureTopicPartitionKey(topic, partition);
    instance.get().getOffset(azureTopicPartitionKey);

    //then
    verify(offsetStorageReader).offset(azureTopicPartitionKey);
  }

  @Test
  void getOffsetShouldReturnEmptyOptionalIfCommitsNotFound() {
    //given
    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);
    when(offsetStorageReader.offset(any(Map.class))).thenReturn(new HashMap());
    TopicPartitionOffsetProvider.initialize(offsetStorageReader);
    String topic = "some_topic";
    Integer partition = 1;

    //when
    Optional<TopicPartitionOffsetProvider> instance = TopicPartitionOffsetProvider.getInstance();
    assertFalse(instance.isEmpty());
    AzureTopicPartitionKey azureTopicPartitionKey = new AzureTopicPartitionKey(topic, partition);
    Optional<AzureOffsetMarker> offset = instance.get().getOffset(azureTopicPartitionKey);

    //then
    verify(offsetStorageReader).offset(azureTopicPartitionKey);
    assertTrue(offset.isEmpty());
  }

  @Test
  void getOffsetShouldReturnValidAzureOffsetMarkerIfCommitsFound() {
    //given
    long offsetOne = 1L;
    String OFFSET_KEY = "OFFSET";
    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);
    HashMap<String, Long> offsets = new HashMap<>();
    offsets.put(OFFSET_KEY, offsetOne);
    when(offsetStorageReader.offset(any(Map.class))).thenReturn(offsets);
    TopicPartitionOffsetProvider.initialize(offsetStorageReader);
    String topic = "some_topic";
    Integer partition = 1;

    //when
    Optional<TopicPartitionOffsetProvider> instance = TopicPartitionOffsetProvider.getInstance();
    assertFalse(instance.isEmpty());
    AzureTopicPartitionKey azureTopicPartitionKey = new AzureTopicPartitionKey(topic, partition);
    Optional<AzureOffsetMarker> offset = instance.get().getOffset(azureTopicPartitionKey);

    //then
    verify(offsetStorageReader).offset(azureTopicPartitionKey);
    assertTrue(offset.isPresent());
    assertEquals(offsetOne, offset.get().getOffsetValue());
  }

  @Test
  void AzureTopicPartitionKeyShouldReturnTopicAndPartitionValues() {
    //given
    int partition = 10;
    String topic = "topic";

    //when
    final AzureTopicPartitionKey azureTopicPartitionKey = new AzureTopicPartitionKey(topic, partition);

    //then
    assertEquals(partition, azureTopicPartitionKey.getPartition());
    assertEquals(topic, azureTopicPartitionKey.getTopic());
  }
}