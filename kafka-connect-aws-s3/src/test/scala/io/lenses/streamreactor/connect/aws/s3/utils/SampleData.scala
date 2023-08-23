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
package io.lenses.streamreactor.connect.aws.s3.utils

import io.lenses.streamreactor.connect.aws.s3.model.Topic
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalacheck.Gen.Choose.chooseDouble
import java.util
import scala.jdk.CollectionConverters.MapHasAsScala

object SampleData extends Matchers {
  val topic: Topic = Topic("niceTopic")

  val UsersSchema: Schema = SchemaBuilder.struct()
    .field("name", SchemaBuilder.string().required().build())
    .field("title", SchemaBuilder.string().optional().build())
    .field("salary", SchemaBuilder.float64().optional().build())
    .build()

  val Users: List[Struct] = List(
    new Struct(UsersSchema).put("name", "sam").put("title", "mr").put("salary", 100.43),
    new Struct(UsersSchema).put("name", "laura").put("title", "ms").put("salary", 429.06),
    new Struct(UsersSchema).put("name", "tom").put("title", null).put("salary", 395.44),
    new Struct(UsersSchema).put("name", "martin").put("title", "mr").put("salary", 395.44),
    new Struct(UsersSchema).put("name", "jackie").put("title", "mrs").put("salary", 395.44),
    new Struct(UsersSchema).put("name", "adam").put("title", "mr").put("salary", 395.44),
    new Struct(UsersSchema).put("name", "jonny").put("title", "mr").put("salary", 395.44),
    new Struct(UsersSchema).put("name", "jim").put("title", "mr").put("salary", 395.44),
    new Struct(UsersSchema).put("name", "wilson").put("title", "dog").put("salary", 395.44),
    new Struct(UsersSchema).put("name", "milson").put("title", "dog").put("salary", 395.44),
  )

  val AddressSchema = SchemaBuilder.struct()
    .field("street", SchemaBuilder.string().required().build())
    .field("city", SchemaBuilder.string().required().build())
    .field("country", SchemaBuilder.string().required().build())
    .build()

  val UserWithAddressSchema = SchemaBuilder.struct()
    .field("name", SchemaBuilder.string().required().build())
    .field("title", SchemaBuilder.string().optional().build())
    .field("salary", SchemaBuilder.float64().optional().build())
    .field("phone_numbers", SchemaBuilder.array(SchemaBuilder.string().build()).optional().build())
    .field("address", AddressSchema)
    .build()
  val recordsAsJson: List[String] = List(
    """{"name":"sam","title":"mr","salary":100.43}""",
    """{"name":"laura","title":"ms","salary":429.06}""",
    """{"name":"tom","title":null,"salary":395.44}""",
    "",
  )

  val csvHeader: String = """"name","title","salary""""

  val recordsAsCsv: List[String] = List(
    """"sam","mr","100.43"""",
    """"laura","ms","429.06"""",
    """"tom",,"395.44"""",
    "",
  )

  val recordsAsCsvWithHeaders: List[String] = List(csvHeader) ++ recordsAsCsv

  def generateUser: Gen[Struct] =
    for {
      name   <- Gen.alphaStr
      title  <- Gen.alphaStr
      salary <- Gen.choose(0.00, 1000.00)(chooseDouble)

    } yield new Struct(UsersSchema).put("name", name).put("title", title).put("salary", salary)

  def checkRecord(genericRecord: GenericRecord, name: String, title: String, salary: Double): Assertion =
    checkRecord(genericRecord, name, Some(title), salary)

  def checkRecord(genericRecord: GenericRecord, name: String, title: Option[String], salary: Double): Assertion = {

    genericRecord.get("name").toString should be(name)
    Option(genericRecord.get("title")).fold(Option.empty[String])(e => Some(e.toString)) should be(title)
    genericRecord.get("salary") should be(salary)
  }

  def checkRecord(schemaAndValue: SchemaAndValue, name: String, title: Option[String], salary: Double): Assertion = {

    schemaAndValue.value().asInstanceOf[Struct].get("name").toString should be(name)
    schemaAndValue.value().asInstanceOf[Struct].get("salary") should be(salary)
    checkTitle(schemaAndValue, title)
  }

  private def checkTitle(schemaAndValue: SchemaAndValue, title: Option[String]) =
    title match {
      case None =>
        schemaAndValue.value().asInstanceOf[Struct].get("title") should be(null)
      case Some(title) =>
        schemaAndValue.value().asInstanceOf[Struct].get("title").toString should be(title)
    }

  def checkArray(genericRecord: GenericData.Array[Utf8], values: String*): Unit =
    values.zipWithIndex.foreach {
      case (string, index) => genericRecord.get(index).toString should be(string)
    }

  def readFromStringKeyedMap[T](genericRecords: List[GenericRecord], recordsArrayPosition: Int): Any =
    genericRecords(recordsArrayPosition).asInstanceOf[util.HashMap[_, _]].asScala.map {
      case (k, v) => k.asInstanceOf[Utf8].toString -> v
    }

}
