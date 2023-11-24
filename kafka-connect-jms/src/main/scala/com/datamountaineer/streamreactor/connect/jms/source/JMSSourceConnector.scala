/*
 * Copyright 2017-2023 Lenses.io Ltd
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
package io.lenses.streamreactor.connect.jms.source

import io.lenses.streamreactor.common.utils.JarManifest
import io.lenses.streamreactor.connect.jms.config.JMSConfig
import io.lenses.streamreactor.connect.jms.config.JMSConfigConstants
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector
import org.apache.kafka.connect.util.ConnectorUtils

import java.util
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
  * Created by andrew@datamountaineer.com on 10/03/2017.
  * stream-reactor
  */
class JMSSourceConnector extends SourceConnector with StrictLogging {
  private var configProps: util.Map[String, String] = _
  private val configDef = JMSConfig.config
  private val manifest  = JarManifest(getClass.getProtectionDomain.getCodeSource.getLocation)

  override def taskClass(): Class[_ <: Task] = classOf[JMSSourceTask]

  def kcqlTaskScaling(maxTasks: Int): util.List[util.Map[String, String]] = {
    val raw = configProps.get(JMSConfigConstants.KCQL)
    require(raw != null && raw.nonEmpty, s"No ${JMSConfigConstants.KCQL} provided!")

    //sql1, sql2
    val kcqls  = raw.split(";")
    val groups = ConnectorUtils.groupPartitions(kcqls.toList.asJava, maxTasks).asScala

    //split up the kcql statement based on the number of tasks.
    groups
      .filterNot(_.isEmpty)
      .map { g =>
        val taskConfigs = new java.util.HashMap[String, String]
        taskConfigs.putAll(configProps)
        taskConfigs.put(JMSConfigConstants.KCQL, g.asScala.mkString(";")) //overwrite
        taskConfigs.asScala.toMap.asJava
      }
  }.asJava

  def defaultTaskScaling(maxTasks: Int): util.List[util.Map[String, String]] = {
    val raw = configProps.get(JMSConfigConstants.KCQL)
    require(raw != null && raw.nonEmpty, s"No ${JMSConfigConstants.KCQL} provided!")
    (1 to maxTasks).map { _ =>
      val taskConfigs: util.Map[String, String] = new java.util.HashMap[String, String]
      taskConfigs.putAll(configProps)
      taskConfigs
    }.toList.asJava
  }

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    val config    = new JMSConfig(configProps)
    val scaleType = config.getString(JMSConfigConstants.TASK_PARALLELIZATION_TYPE).toLowerCase()
    if (scaleType == JMSConfigConstants.TASK_PARALLELIZATION_TYPE_DEFAULT) {
      kcqlTaskScaling(maxTasks)
    } else defaultTaskScaling(maxTasks)
  }

  override def config(): ConfigDef = configDef

  override def start(props: util.Map[String, String]): Unit = {
    val config = new JMSConfig(props)
    configProps = config.props
  }

  override def stop(): Unit = {}

  override def version(): String = manifest.version()
}
