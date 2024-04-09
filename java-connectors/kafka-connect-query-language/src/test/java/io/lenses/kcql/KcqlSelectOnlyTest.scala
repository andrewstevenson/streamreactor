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
package io.lenses.kcql
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

import scala.jdk.CollectionConverters.ListHasAsScala

class KcqlSelectOnlyTest extends AnyFunSuite {

  test("parseStartAndSetAField") {
    val topic  = "TOPIC_A"
    val syntax = s"SELECT * FROM $topic"
    val kcql   = Kcql.parse(syntax)
    kcql.getSource should be(topic)
    kcql.getTarget should be(null)
    kcql.getFields should not be empty
  }

  test("parseASelectAllFromTopic") {
    val topic  = "TOPIC_A"
    val syntax = s"SELECT * FROM $topic withformat text"
    val kcql   = Kcql.parse(syntax)
    kcql.getSource should be(topic)
    kcql.getTarget should be(null)
    kcql.getFields should not be empty
    kcql.getFields.get(0).getName should be("*")
    val pks = kcql.getPrimaryKeys.asScala.map(_.toString).toSet

    pks.size should be(0)
    kcql.getFormatType should be(FormatType.TEXT)
  }

  test("parseInsertSelectWithPkNonParticipatingInFieldSelection") {
    val KCQL = "INSERT INTO SENSOR- SELECT temperature, humidity FROM sensorsTopic PK sensorID STOREAS SS"
    val kcql = Kcql.parse(KCQL)
    kcql.getStoredAs should be("SS")
  }

  test("storeASWithAVRO") {
    val KCQL = "INSERT INTO SENSOR- SELECT temperature, humidity FROM sensorsTopic PK sensorID STOREAS AVRO"
    val kcql = Kcql.parse(KCQL)
    kcql.getStoredAs should be("AVRO")
  }

  test("storeASWithParquet") {
    val KCQL = "INSERT INTO SENSOR- SELECT temperature, humidity FROM sensorsTopic PK sensorID STOREAS PARQUET"
    val kcql = Kcql.parse(KCQL)
    kcql.getStoredAs should be("PARQUET")
  }

  test("storeASWithJSON") {
    val KCQL = "INSERT INTO SENSOR- SELECT temperature, humidity FROM sensorsTopic PK sensorID STOREAS JSON"
    val kcql = Kcql.parse(KCQL)
    kcql.getStoredAs should be("JSON")
  }

  test("storeASWithBYTES") {
    val KCQL = "INSERT INTO SENSOR- SELECT temperature, humidity FROM sensorsTopic PK sensorID STOREAS BYTES"
    val kcql = Kcql.parse(KCQL)
    kcql.getStoredAs should be("BYTES")
  }

  test("testSELECTwithPK") {
    val KCQL = "SELECT temperature, humidity FROM sensorsTopic PK sensorID"
    val kcql = Kcql.parse(KCQL)
    kcql.getPrimaryKeys.get(0).getName should be("sensorID")
    kcql.getPrimaryKeys.get(0).getAlias should be("sensorID")
    kcql.getPrimaryKeys.get(0).getParentFields should be(null)
  }

  test("testSELECTwithNestedFieldsInPK") {
    val KCQL = "SELECT temperature, humidity FROM sensorsTopic PK metadata.sensorID, metadata.timestamp.ticks"
    val kcql = Kcql.parse(KCQL)

    kcql.getPrimaryKeys.size should be(2)

    kcql.getPrimaryKeys.get(0).getName should be("sensorID")
    kcql.getPrimaryKeys.get(0).getAlias should be("sensorID")
    kcql.getPrimaryKeys.get(0).getParentFields should not be null
    kcql.getPrimaryKeys.get(0).getParentFields.size should be(1)
    kcql.getPrimaryKeys.get(0).getParentFields.get(0) should be("metadata")

    kcql.getPrimaryKeys.get(1).getName should be("ticks")
    kcql.getPrimaryKeys.get(1).getAlias should be("ticks")
    kcql.getPrimaryKeys.get(1).getParentFields should not be null
    kcql.getPrimaryKeys.get(1).getParentFields.size should be(2)
    kcql.getPrimaryKeys.get(1).getParentFields.get(0) should be("metadata")
    kcql.getPrimaryKeys.get(1).getParentFields.get(1) should be("timestamp")
  }

  test("testSELECTwithNestedFieldsInPK2") {
    var k    = "INSERT INTO index_andrew SELECT id, string_field FROM sink_test"
    var kcql = Kcql.parse(k)
    kcql.getPrimaryKeys.size should be(0)

    k    = "INSERT INTO index_andrew SELECT id, nested.string_field FROM sink_test"
    kcql = Kcql.parse(k)
    kcql.getPrimaryKeys.size should be(0)
    k    = "UPSERT INTO sink_test SELECT id, string_field FROM sink_andrew PK id"
    kcql = Kcql.parse(k)
    kcql.getPrimaryKeys.size should be(1)
  }

  test("testSTOREAS") {
    val KCQL = "SELECT temperature, humidity FROM sensorsTopic PK sensorID STOREAS SS"
    val kcql = Kcql.parse(KCQL)
    kcql.getStoredAs should be("SS")
    kcql.getPrimaryKeys.get(0).getName should be("sensorID")
    kcql.getPrimaryKeys.get(0).getAlias should be("sensorID")
    kcql.getPrimaryKeys.get(0).getParentFields should be(null)
  }

  test("testUnwrapping") {
    val KCQL = "SELECT temperature, humidity FROM sensorsTopic PK sensorID WITHUNWRAP"
    val kcql = Kcql.parse(KCQL)
    kcql.getPrimaryKeys.get(0).getName should be("sensorID")
    kcql.getPrimaryKeys.get(0).getAlias should be("sensorID")
    kcql.getPrimaryKeys.get(0).getParentFields should be(null)
  }

  test("parseASelectWithAliasingFields") {
    val topic  = "TOPIC-A"
    val syntax = s"SELECT f1 as col1, f2 as col2 FROM $topic withformat binary"
    val kcql   = Kcql.parse(syntax)
    kcql.getSource should be(topic)
    kcql.getTarget should be(null)
    val fa  = kcql.getFields.asScala.toList
    val map = fa.map(f => f.getName -> f).toMap
    fa.size should be(2)
    map should contain key "f1"
    map("f1").getAlias should be("col1")
    map should contain key "f2"
    map("f2").getAlias should be("col2")
  }

  test("parseASelectWithAMixOfAliasing") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM `$topic` withformat text"
    val kcql   = Kcql.parse(syntax)
    kcql.getSource should be(topic)
    kcql.getTarget should be(null)
    val fa  = kcql.getFields.asScala.toList
    val map = fa.map(f => f.getName -> f).toMap
    fa.size should be(4)
    map should contain key "f1"
    map("f1").getAlias should be("col1")
    map should contain key "f2"
    map("f2").getAlias should be("col2")
    map should contain key "f3"
    map("f3").getAlias should be("f3")
    map should contain key "f4"
    map("f4").getAlias should be("f4")
    kcql.hasRetainStructure should be(false)
  }

  test("parseASelectWithAMixOfAliasingAndUsingQuotation") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM '$topic' withformat text"
    val kcql   = Kcql.parse(syntax)
    kcql.getSource should be(topic)
    kcql.getTarget should be(null)
    val fa  = kcql.getFields.asScala.toList
    val map = fa.map(f => f.getName -> f).toMap
    fa.size should be(4)
    map should contain key "f1"
    map("f1").getAlias should be("col1")
    map should contain key "f2"
    map("f2").getAlias should be("col2")
    map should contain key "f3"
    map("f3").getAlias should be("f3")
    map should contain key "f4"
    map("f4").getAlias should be("f4")
    kcql.hasRetainStructure should be(false)
  }

  test("parseASelectWithAMixOfAliasingAndRetainStructure") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM `$topic` withstructure withformat text"
    val kcql   = Kcql.parse(syntax)
    kcql.getSource should be(topic)
    kcql.getTarget should be(null)
    val fa  = kcql.getFields.asScala.toList
    val map = fa.map(f => f.getName -> f).toMap
    fa.size should be(4)
    map should contain key "f1"
    map("f1").getAlias should be("col1")
    map should contain key "f2"
    map("f2").getAlias should be("col2")
    map should contain key "f3"
    map("f3").getAlias should be("f3")
    map should contain key "f4"
    map("f4").getAlias should be("f4")
    kcql.hasRetainStructure should be(true)
  }

  test("throwAnExceptionIfTheFormatIsNotCorrect") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM $topic WITHFORMAT ARO SAMPLE 10 EVERY 0"
    assertThrows[IllegalArgumentException] {
      Kcql.parse(syntax)
    }
  }

  test("throwAnExceptionIfLimitNumberIsMissing") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM $topic LIMIT"
    assertThrows[IllegalArgumentException] {
      Kcql.parse(syntax)
    }
  }

  test("throwAnExceptionIfLimitNumberIsZero") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM $topic LIMIT 0"
    assertThrows[IllegalArgumentException] {
      Kcql.parse(syntax)
    }
  }

  test("parseLimit") {
    val topic  = "TOPIC.A"
    val syntax = s"SELECT f1 as col1, f3, f2 as col2,f4 FROM $topic LIMIT 10"
    val k      = Kcql.parse(syntax)
    k.getLimit should be(10)
  }
}
