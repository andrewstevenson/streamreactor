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
package io.lenses.streamreactor.connect.azure.eventhubs.source;

import static io.lenses.streamreactor.connect.azure.eventhubs.mapping.SourceRecordMapper.mapSourceRecordIncludingHeaders;

import io.lenses.streamreactor.connect.azure.eventhubs.source.TopicPartitionOffsetProvider.AzureOffsetMarker;
import io.lenses.streamreactor.connect.azure.eventhubs.source.TopicPartitionOffsetProvider.AzureTopicPartitionKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.connect.source.SourceRecord;

/**
 * Class is a bridge between EventHub KafkaConsumers and AzureEventHubsSourceTask. It verifies the
 * configuration of kafka consumers and instantiates them, then allows AzureEventHubsSourceTask to
 * pull for SourceRecords.
 */
@Slf4j
public class EventHubsKafkaConsumerController {

  private final BlockingQueue<ConsumerRecords<byte[], byte[]>> recordsQueue;
  private KafkaByteBlockingQueuedProducer queuedKafkaProducer;
  private final Map<String, String> inputToOutputTopics;

  /**
   * Constructs EventHubsKafkaConsumerController.
   *
   * @param queuedKafkaProducer producer to the recordsQueue
   * @param recordsQueue        queue that contains EventHub records
   * @param inputToOutputTopics input to output topics
   */
  public EventHubsKafkaConsumerController(KafkaByteBlockingQueuedProducer queuedKafkaProducer,
      BlockingQueue<ConsumerRecords<byte[], byte[]>> recordsQueue,
      Map<String, String> inputToOutputTopics) {
    this.recordsQueue = recordsQueue;
    this.queuedKafkaProducer = queuedKafkaProducer;
    this.inputToOutputTopics = inputToOutputTopics;
  }

  /**
   * This method leverages BlockingQueue mechanism that BlockingQueuedKafkaConsumer puts EventHub
   * records into. It tries to poll the queue then returns list of SourceRecords
   *
   * @param duration how often to poll.
   * @return list of SourceRecords (can be empty if it couldn't poll from queue)
   * @throws InterruptedException if interrupted while polling
   */
  public List<SourceRecord> poll(Duration duration) throws InterruptedException {
    List<SourceRecord> sourceRecords = null;

    queuedKafkaProducer.start();

    ConsumerRecords<byte[], byte[]> consumerRecords = null;
    try {
      consumerRecords = recordsQueue.poll(
          duration.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.info("{} has been interrupted on poll", this.getClass().getSimpleName());
      throw e;
    }

    if (consumerRecords != null && !consumerRecords.isEmpty()) {
      sourceRecords = new ArrayList<>(consumerRecords.count());
      for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {

        String inputTopic = consumerRecord.topic();
        AzureTopicPartitionKey azureTopicPartitionKey = new AzureTopicPartitionKey(
            inputTopic, consumerRecord.partition());
        AzureOffsetMarker offsetMarker = new AzureOffsetMarker(consumerRecord.offset());

        SourceRecord sourceRecord = mapSourceRecordIncludingHeaders(consumerRecord,
            azureTopicPartitionKey,
            offsetMarker, inputToOutputTopics.get(inputTopic),
            queuedKafkaProducer.getKeyValueTypes().getKeyType().getSchema(),
            queuedKafkaProducer.getKeyValueTypes().getValueType().getSchema());

        sourceRecords.add(sourceRecord);

      }
    }
    return sourceRecords != null ? sourceRecords : Collections.emptyList();
  }

  public void close(Duration timeoutDuration) {
    queuedKafkaProducer.stop(timeoutDuration);
  }
}
