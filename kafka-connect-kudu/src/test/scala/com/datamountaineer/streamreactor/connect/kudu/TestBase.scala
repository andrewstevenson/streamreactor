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
package com.datamountaineer.streamreactor.connect.kudu

/**
  * Created by andrew@datamountaineer.com on 24/02/16.
  * stream-reactor
  */

import java.nio.ByteBuffer
import java.util
import com.datamountaineer.streamreactor.connect.kudu.config.KuduConfigConstants
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.sink.SinkRecord
import org.apache.kudu.ColumnSchema.ColumnSchemaBuilder
import org.apache.kudu.ColumnSchema
import org.apache.kudu.Type
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala

trait TestBase extends AnyWordSpec with BeforeAndAfter with Matchers {
  val TOPIC                          = "sink_test"
  val TABLE                          = "table1"
  val KUDU_MASTER                    = "127.0.0.1"
  val KCQL_MAP                       = s"INSERT INTO $TABLE SELECT * FROM $TOPIC"
  val KCQL_MAP_AUTOCREATE            = KCQL_MAP + " AUTOCREATE DISTRIBUTEBY name,adult INTO 10 BUCKETS"
  val KCQL_MAP_AUTOCREATE_AUTOEVOLVE = KCQL_MAP + " AUTOCREATE AUTOEVOLVE DISTRIBUTEBY name,adult INTO 10 BUCKETS"
  val schema =
    """
      |{ "type": "record",
      |"name": "Person",
      |"namespace": "com.datamountaineer",
      |"fields": [
      |{      "name": "name",      "type": "string"},
      |{      "name": "adult",     "type": "boolean"},
      |{      "name": "integer8",  "type": "int"},
      |{      "name": "integer16", "type": "int"},
      |{      "name": "integer32", "type": "long"},
      |{      "name": "integer64", "type": "long"},
      |{      "name": "float32",   "type": "float"},
      |{      "name": "float64",   "type": "double"}
      |]}"
    """.stripMargin

  val schemaDefaults =
    """
      |{ "type": "record",
      |"name": "Person",
      |"namespace": "com.datamountaineer",
      |"fields": [
      |{      "name": "name",      "type": "string"},
      |{      "name": "adult",     "type": "boolean"},
      |{      "name": "integer8",  "type": "int"},
      |{      "name": "integer16", "type": "int"},
      |{      "name": "integer32", "type": "long"},
      |{      "name": "integer64", "type": "long"},
      |{      "name": "float32",   "type": "float"},
      |{      "name": "float64",   "type": ["double", "null"], "default" : 10.0}
      |]}"
    """.stripMargin

  protected val PARTITION:        Int                      = 12
  protected val PARTITION2:       Int                      = 13
  protected val TOPIC_PARTITION:  TopicPartition           = new TopicPartition(TOPIC, PARTITION)
  protected val TOPIC_PARTITION2: TopicPartition           = new TopicPartition(TOPIC, PARTITION2)
  protected val ASSIGNMENT:       util.Set[TopicPartition] = new util.HashSet[TopicPartition]
  //Set topic assignments
  ASSIGNMENT.add(TOPIC_PARTITION)
  ASSIGNMENT.add(TOPIC_PARTITION2)

  before {}

  after {}

  def getConfig =
    Map(
      "topics"                         -> TOPIC,
      KuduConfigConstants.KUDU_MASTER  -> KUDU_MASTER,
      KuduConfigConstants.KCQL         -> KCQL_MAP,
      KuduConfigConstants.ERROR_POLICY -> "THROW",
    ).asJava

  def getConfigAutoCreate(url: String) =
    Map(
      KuduConfigConstants.KUDU_MASTER         -> KUDU_MASTER,
      KuduConfigConstants.KCQL                -> KCQL_MAP_AUTOCREATE,
      KuduConfigConstants.ERROR_POLICY        -> "THROW",
      KuduConfigConstants.SCHEMA_REGISTRY_URL -> url,
    ).asJava

  def getConfigAutoCreateAndEvolve(url: String) =
    Map(
      KuduConfigConstants.KUDU_MASTER         -> KUDU_MASTER,
      KuduConfigConstants.KCQL                -> KCQL_MAP_AUTOCREATE_AUTOEVOLVE,
      KuduConfigConstants.ERROR_POLICY        -> "THROW",
      KuduConfigConstants.SCHEMA_REGISTRY_URL -> url,
    ).asJava

  def getConfigAutoCreateRetry(url: String) =
    Map(
      KuduConfigConstants.KUDU_MASTER         -> KUDU_MASTER,
      KuduConfigConstants.KCQL                -> KCQL_MAP_AUTOCREATE,
      KuduConfigConstants.ERROR_POLICY        -> "RETRY",
      KuduConfigConstants.SCHEMA_REGISTRY_URL -> url,
    ).asJava

  def getConfigAutoCreateRetryWithBackgroundFlush(url: String) =
    Map(
      KuduConfigConstants.KUDU_MASTER         -> KUDU_MASTER,
      KuduConfigConstants.KCQL                -> KCQL_MAP_AUTOCREATE,
      KuduConfigConstants.ERROR_POLICY        -> "RETRY",
      KuduConfigConstants.SCHEMA_REGISTRY_URL -> url,
      KuduConfigConstants.WRITE_FLUSH_MODE    -> "BATCH_BACKGROUND",
    ).asJava

  def createSchema2: Schema =
    SchemaBuilder.struct.name("record")
      .version(1)
      .field("id", Schema.STRING_SCHEMA)
      .field("int_field", Schema.INT32_SCHEMA)
      .field("long_field", Schema.INT64_SCHEMA)
      .field("string_field", Schema.STRING_SCHEMA)
      .field("float_field", Schema.FLOAT32_SCHEMA)
      .field("float64_field", Schema.FLOAT64_SCHEMA)
      .field("boolean_field", Schema.BOOLEAN_SCHEMA)
      .field("int64_field", Schema.INT64_SCHEMA)
      .build

  def createKuduSchema2: org.apache.kudu.Schema = {
    val columns = new util.ArrayList[ColumnSchema]
    val idField = new ColumnSchema.ColumnSchemaBuilder("id", Type.STRING).key(true).build
    columns.add(idField)
    val intField = new ColumnSchema.ColumnSchemaBuilder("int_field", Type.INT32).build
    columns.add(intField)
    val longField = new ColumnSchema.ColumnSchemaBuilder("long_field", Type.INT64).build
    columns.add(longField)
    val stringField = new ColumnSchema.ColumnSchemaBuilder("string_field", Type.STRING).build
    columns.add(stringField)
    val floatField = new ColumnSchema.ColumnSchemaBuilder("float_field", Type.FLOAT).build
    columns.add(floatField)
    val float64Field = new ColumnSchema.ColumnSchemaBuilder("float64_field", Type.DOUBLE).build
    columns.add(float64Field)
    val booleanField = new ColumnSchema.ColumnSchemaBuilder("boolean_field", Type.BOOL).build
    columns.add(booleanField)
    val int64Field = new ColumnSchema.ColumnSchemaBuilder("int64_field", Type.INT64).defaultValue(20.toLong).build
    columns.add(int64Field)
    new org.apache.kudu.Schema(columns)
  }

  def createSchema3: Schema =
    SchemaBuilder.struct.name("record")
      .version(1)
      .field("id", Schema.STRING_SCHEMA)
      .field("int_field", Schema.INT32_SCHEMA)
      .field("long_field", Schema.INT64_SCHEMA)
      .field("string_field", Schema.STRING_SCHEMA)
      .field("float_field", Schema.FLOAT32_SCHEMA)
      .field("float64_field", Schema.FLOAT64_SCHEMA)
      .field("boolean_field", Schema.BOOLEAN_SCHEMA)
      .field("int64_field", Schema.INT64_SCHEMA)
      .field("new_field", Schema.STRING_SCHEMA)
      .build

  def createSchema4: Schema =
    SchemaBuilder.struct.name("record")
      .version(1)
      .field("id", Schema.STRING_SCHEMA)
      .field("int_field", Schema.INT32_SCHEMA)
      .field("long_field", Schema.INT64_SCHEMA)
      .field("string_field", Schema.STRING_SCHEMA)
      .field("float_field", Schema.FLOAT32_SCHEMA)
      .field("float64_field", Schema.FLOAT64_SCHEMA)
      .field("boolean_field", Schema.BOOLEAN_SCHEMA)
      .field("byte_field", Schema.BYTES_SCHEMA)
      .field("int64_field", SchemaBuilder.int64().defaultValue(20.toLong).build())
      .build

  def createKuduSchema4: org.apache.kudu.Schema = {
    val columns = new util.ArrayList[ColumnSchema]
    val idField = new ColumnSchema.ColumnSchemaBuilder("id", Type.STRING).key(true).build
    columns.add(idField)
    val intField = new ColumnSchema.ColumnSchemaBuilder("int_field", Type.INT32).build
    columns.add(intField)
    val longField = new ColumnSchema.ColumnSchemaBuilder("long_field", Type.INT64).build
    columns.add(longField)
    val stringField = new ColumnSchema.ColumnSchemaBuilder("string_field", Type.STRING).build
    columns.add(stringField)
    val floatField = new ColumnSchema.ColumnSchemaBuilder("float_field", Type.FLOAT).build
    columns.add(floatField)
    val float64Field = new ColumnSchema.ColumnSchemaBuilder("float64_field", Type.DOUBLE).build
    columns.add(float64Field)
    val booleanField = new ColumnSchema.ColumnSchemaBuilder("boolean_field", Type.BOOL).build
    columns.add(booleanField)
    val byteField = new ColumnSchema.ColumnSchemaBuilder("byte_field", Type.BINARY).build
    columns.add(byteField)
    val int64Field = new ColumnSchema.ColumnSchemaBuilder("int64_field", Type.INT64).defaultValue(20.toLong).build
    columns.add(int64Field)
    new org.apache.kudu.Schema(columns)
  }

  def createSchema5: Schema =
    SchemaBuilder.struct.name("record")
      .version(2)
      .field("id", Schema.STRING_SCHEMA)
      .field("int_field", Schema.INT32_SCHEMA)
      .field("long_field", Schema.INT64_SCHEMA)
      .field("string_field", Schema.STRING_SCHEMA)
      .field("float_field", Schema.FLOAT32_SCHEMA)
      .field("float64_field", Schema.FLOAT64_SCHEMA)
      .field("boolean_field", Schema.BOOLEAN_SCHEMA)
      .field("byte_field", Schema.BYTES_SCHEMA)
      .field("int64_field", SchemaBuilder.int64().defaultValue(20.toLong).build())
      .field("new_field", SchemaBuilder.string().defaultValue("").build())
      .build

  def createKuduSchema5: org.apache.kudu.Schema = {
    val columns = new util.ArrayList[ColumnSchema]
    val idField = new ColumnSchema.ColumnSchemaBuilder("id", Type.STRING).key(true).build
    columns.add(idField)
    val intField = new ColumnSchema.ColumnSchemaBuilder("int_field", Type.INT32).build
    columns.add(intField)
    val longField = new ColumnSchema.ColumnSchemaBuilder("long_field", Type.INT64).build
    columns.add(longField)
    val stringField = new ColumnSchema.ColumnSchemaBuilder("string_field", Type.STRING).build
    columns.add(stringField)
    val floatField = new ColumnSchema.ColumnSchemaBuilder("float_field", Type.FLOAT).build
    columns.add(floatField)
    val float64Field = new ColumnSchema.ColumnSchemaBuilder("float64_field", Type.DOUBLE).build
    columns.add(float64Field)
    val booleanField = new ColumnSchema.ColumnSchemaBuilder("boolean_field", Type.BOOL).build
    columns.add(booleanField)
    val byteField = new ColumnSchema.ColumnSchemaBuilder("byte_field", Type.BINARY).build
    columns.add(byteField)
    val int64Field = new ColumnSchema.ColumnSchemaBuilder("int64_field", Type.INT64).defaultValue(20.toLong).build
    columns.add(int64Field)
    val newField = new ColumnSchema.ColumnSchemaBuilder("new_field", Type.STRING).defaultValue("").build
    columns.add(newField)
    new org.apache.kudu.Schema(columns)
  }

  //build a test record
  def createRecord5(schema: Schema, id: String): Struct =
    new Struct(schema)
      .put("id", id)
      .put("int_field", 12)
      .put("long_field", 12L)
      .put("string_field", "foo")
      .put("float_field", 0.1.toFloat)
      .put("float64_field", 0.199999)
      .put("boolean_field", true)
      .put("byte_field", ByteBuffer.wrap("bytes".getBytes))
      .put("int64_field", 12L)
      .put("new_field", "teststring")

  def createSinkRecord(record: Struct, topic: String, offset: Long) =
    new SinkRecord(topic,
                   1,
                   Schema.STRING_SCHEMA,
                   "key",
                   record.schema(),
                   record,
                   offset,
                   System.currentTimeMillis(),
                   TimestampType.CREATE_TIME,
    )

  //generate some test records
  def getTestRecords: Set[SinkRecord] = {
    val schema = createSchema
    val assignment: mutable.Set[TopicPartition] = getAssignment.asScala

    assignment.flatMap { a =>
      (1 to 1).map { i =>
        val record: Struct = createRecord(schema, a.topic() + "-" + a.partition() + "-" + i)
        new SinkRecord(a.topic(),
                       a.partition(),
                       Schema.STRING_SCHEMA,
                       "key",
                       schema,
                       record,
                       i.toLong,
                       System.currentTimeMillis(),
                       TimestampType.CREATE_TIME,
        )
      }
    }.toSet
  }

  //get the assignment of topic partitions for the sinkTask
  def getAssignment: util.Set[TopicPartition] =
    ASSIGNMENT

  //build a test record schema
  def createSchema: Schema = {
    import org.apache.kafka.connect.data._
    val o = SchemaBuilder.bytes()
      .name(Decimal.LOGICAL_NAME)
      .optional()
      .parameter("connect.decimal.precision", "18")
      .parameter("scale", "4")
      .build()
    SchemaBuilder.struct.name("record")
      .version(1)
      .field("id", Schema.STRING_SCHEMA)
      .field("int_field", Schema.INT32_SCHEMA)
      .field("long_field", Schema.INT64_SCHEMA)
      .field("string_field", Schema.STRING_SCHEMA)
      .field("float_field", Schema.FLOAT32_SCHEMA)
      .field("float64_field", Schema.FLOAT64_SCHEMA)
      .field("boolean_field", Schema.BOOLEAN_SCHEMA)
      .field("byte_field", Schema.BYTES_SCHEMA)
      .field("optional", o)
      .build
  }

  def createKuduSchema: org.apache.kudu.Schema = {
    val columns = new util.ArrayList[ColumnSchema]
    val idField = new ColumnSchemaBuilder("id", Type.STRING).key(true).build()
    columns.add(idField)
    val intField = new ColumnSchemaBuilder("int_field", Type.INT32).build()
    columns.add(intField)
    val longField = new ColumnSchemaBuilder("long_field", Type.INT64).build()
    columns.add(longField)
    val stringField = new ColumnSchemaBuilder("string_field", Type.STRING).build()
    columns.add(stringField)
    val floatField = new ColumnSchemaBuilder("float_field", Type.FLOAT).build()
    columns.add(floatField)
    val float64Field = new ColumnSchemaBuilder("float64_field", Type.DOUBLE).build()
    columns.add(float64Field)
    val booleanField = new ColumnSchemaBuilder("boolean_field", Type.BOOL).build()
    columns.add(booleanField)
    val byteField = new ColumnSchemaBuilder("byte_field", Type.BINARY).build()
    columns.add(byteField)

    new org.apache.kudu.Schema(columns)
  }

  //build a test record
  def createRecord(schema: Schema, id: String): Struct =
    new Struct(schema)
      .put("id", id)
      .put("int_field", 12)
      .put("long_field", 12L)
      .put("string_field", "foo")
      .put("float_field", 0.1.toFloat)
      .put("float64_field", 0.199999)
      .put("boolean_field", true)
      .put("byte_field", ByteBuffer.wrap("bytes".getBytes))
}
