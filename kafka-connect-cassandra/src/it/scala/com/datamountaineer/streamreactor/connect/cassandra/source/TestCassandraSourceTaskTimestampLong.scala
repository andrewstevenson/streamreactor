/*
 * Copyright 2017 Datamountaineer.
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

package com.datamountaineer.streamreactor.connect.cassandra.source

import com.datamountaineer.streamreactor.common.queues.QueueHelpers
import com.datamountaineer.streamreactor.common.schemas.ConverterUtil
import com.datamountaineer.streamreactor.connect.cassandra.ItTestConfig
import com.datamountaineer.streamreactor.connect.cassandra.config.CassandraConfigSource
import com.datamountaineer.streamreactor.connect.cassandra.config.CassandraSettings
import com.datastax.driver.core.Session
import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.connect.source.SourceRecord
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.DoNotDiscover
import org.scalatest.Suite

import java.util.concurrent.LinkedBlockingQueue
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.ListHasAsScala

@DoNotDiscover
@nowarn
class TestCassandraSourceTaskTimestampLong
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ItTestConfig
    with ConverterUtil
    with BeforeAndAfterAll
    with TestCassandraSourceUtil {

  var session: Session = _
  val keyspace = "source"
  var tableName: String = _

  override def beforeAll(): Unit = {
    session   = createKeySpace(keyspace, secure = true)
    tableName = createTimestampTable(session, keyspace)
  }

  override def afterAll(): Unit = {
    session.close()
    session.getCluster.close()
  }

  "CassandraReader should read in incremental mode with timestamp and time slices (long)" in {
    val taskContext = getSourceTaskContextDefault
    val taskConfig  = new CassandraConfigSource(getCassandraConfigDefault)

    // queue for reader to put records in
    val queue   = new LinkedBlockingQueue[SourceRecord](100)
    val setting = CassandraSettings.configureSource(taskConfig).head

    val reader =
      CassandraTableReader(name = "test", session = session, setting = setting, context = taskContext, queue = queue)

    insertIntoTimestampTable(session, keyspace, tableName, "id1", "magic_string", getFormattedDateNow(), 1.toByte)

    // clear out the default of Jan 1, 1900
    // and read the inserted row
    reader.read()

    // sleep and check queue size
    while (queue.size() < 1) {
      Thread.sleep(1000)
    }
    queue.size() shouldBe 1

    // drain the queue
    val sourceRecord = QueueHelpers.drainQueue(queue, 1).asScala.toList.head
    val json: JsonNode = convertValueToJson(sourceRecord)
    json.get("string_field").asText().equals("magic_string") shouldBe true

    // insert another two records
    insertIntoTimestampTable(session, keyspace, tableName, "id2", "magic_string2", getFormattedDateNow(), 1.toByte)
    insertIntoTimestampTable(session, keyspace, tableName, "id3", "magic_string3", getFormattedDateNow(), 1.toByte)

    // sleep for longer than time slice (10 sec)
    Thread.sleep(11000)

    // insert another record
    insertIntoTimestampTable(session, keyspace, tableName, "id4", "magic_string4", getFormattedDateNow(), 1.toByte)

    //read
    reader.read()

    // sleep and check queue size
    // expecting to only get the 2 rows
    // in the time slice
    // the insert with "magic_string4" should fall
    // outside this range
    while (queue.size() < 2) {
      Thread.sleep(1000)
    }
    queue.size() shouldBe 2

    val sourceRecords2 = QueueHelpers.drainQueue(queue, 10).asScala.toList
    sourceRecords2.size shouldBe 2

    // sleep for longer than time slice (10 sec)
    Thread.sleep(11000)

    // read but don't insert any new rows
    reader.read()
    // sleep
    Thread.sleep(1000)
    //read
    reader.read()

    //sleep and check queue size
    while (queue.size() < 1) {
      Thread.sleep(1000)
    }
    //should be the inserted row "magic_string4"
    queue.size() shouldBe 1

  }

  private def getCassandraConfigDefault = {
    val myKcql =
      s"INSERT INTO sink_test SELECT string_field, timestamp_field FROM $tableName PK timestamp_field INCREMENTALMODE=timestamp"
    getCassandraConfig(keyspace, tableName, myKcql, strPort())
  }

  override def withPort(port: Int): Suite = {
    setPort(port)
    this
  }

}
