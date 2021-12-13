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

package com.datamountaineer.streamreactor.connect.hazelcast.sink

import com.datamountaineer.streamreactor.connect.hazelcast.config.{HazelCastConnectionConfig, HazelCastSinkConfig, HazelCastSinkConfigConstants}
import com.datamountaineer.streamreactor.connect.hazelcast.{HazelCastConnection, MessageListenerImplJson, TestBase}
import com.hazelcast.config.{Config, GroupConfig}
import com.hazelcast.core.{Hazelcast, ITopic}
import org.apache.kafka.connect.sink.SinkTaskContext
import org.mockito.MockitoSugar

import scala.collection.JavaConverters._

/**
  * Created by andrew@datamountaineer.com on 12/08/16. 
  * stream-reactor
  */
class TestHazelCastSinkTask extends TestBase with MockitoSugar {

  "should start SinkTask and write json" in {
    val configApp1 = new Config()
    configApp1.setProperty( "hazelcast.logging.type", "slf4j" )
    val gc = new  GroupConfig(TESTS_GROUP_NAME, HazelCastSinkConfigConstants.SINK_GROUP_PASSWORD_DEFAULT)
    configApp1.setGroupConfig(gc)
    val instance = Hazelcast.newHazelcastInstance(configApp1)

    val props = getPropsJson
    val context = mock[SinkTaskContext]
    val assignment = getAssignment
    when(context.assignment()).thenReturn(assignment)
    when(context.configs()).thenReturn(props)
    val records = getTestRecords()
    val task = new HazelCastSinkTask
    //initialise the tasks context
    task.initialize(context)
    //start task
    task.start(props)

    //get client and check hazelcast
    val config = new HazelCastSinkConfig(props)
    val conn = HazelCastConnection.buildClient(HazelCastConnectionConfig(config))
    val reliableTopic = conn.getReliableTopic(TABLE).asInstanceOf[ITopic[Object]]
    val listener = new MessageListenerImplJson
    reliableTopic.addMessageListener(listener)

    //write
    task.put(records.asJava)
    //task.stop()

    while (!listener.gotMessage) {
      Thread.sleep(1000)
    }

    val message = listener.message.get
    message.toString shouldBe json
    conn.shutdown()
    task.stop()
    instance.shutdown()
  }
}
