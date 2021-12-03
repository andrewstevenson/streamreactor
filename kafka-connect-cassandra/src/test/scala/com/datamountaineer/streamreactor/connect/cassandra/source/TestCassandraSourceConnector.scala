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

import com.datamountaineer.streamreactor.connect.cassandra.TestConfig
import com.datamountaineer.streamreactor.connect.cassandra.config.CassandraConfigConstants
import org.scalatest.{DoNotDiscover, Suite}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava}

/**
  * Created by andrew@datamountaineer.com on 20/04/16.
  * stream-reactor
  */

@DoNotDiscover
class TestCassandraSourceConnector extends AnyWordSpec with Matchers with TestConfig {
  "Should start a Cassandra Source Connector" in {
    val props =  Map(
      CassandraConfigConstants.PORT -> strPort(),
      CassandraConfigConstants.CONTACT_POINTS -> CONTACT_POINT,
      CassandraConfigConstants.KEY_SPACE -> CASSANDRA_SOURCE_KEYSPACE,
      CassandraConfigConstants.USERNAME -> USERNAME,
      CassandraConfigConstants.PASSWD -> PASSWD,
      CassandraConfigConstants.KCQL -> IMPORT_QUERY_ALL,
      CassandraConfigConstants.ASSIGNED_TABLES -> ASSIGNED_TABLES,
      CassandraConfigConstants.POLL_INTERVAL -> "1000"
    ).asJava

    val connector = new CassandraSourceConnector()
    connector.start(props)
    val taskConfigs = connector.taskConfigs(1)
    taskConfigs.asScala.head.get(CassandraConfigConstants.KCQL) shouldBe IMPORT_QUERY_ALL
    taskConfigs.asScala.head.get(CassandraConfigConstants.CONTACT_POINTS) shouldBe CONTACT_POINT
    taskConfigs.asScala.head.get(CassandraConfigConstants.KEY_SPACE) shouldBe CASSANDRA_SOURCE_KEYSPACE
    taskConfigs.asScala.head.get(CassandraConfigConstants.ASSIGNED_TABLES) shouldBe ASSIGNED_TABLES
    taskConfigs.size() shouldBe 1
    connector.taskClass() shouldBe classOf[CassandraSourceTask]
    connector.stop()
  }

  override def withPort(port: Int): Suite = {
    setPort(port)
    this
  }
}
