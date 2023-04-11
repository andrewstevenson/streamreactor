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
package com.datamountaineer.streamreactor.connect.hbase

import io.confluent.connect.avro.AvroData
import org.apache.avro.generic.GenericData
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.connect.data._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class StructFieldsExtractorBytesTest extends AnyWordSpec with Matchers {
  "StructFieldsExtractor" should {
    "return all the fields and their bytes value" in {
      val schema = SchemaBuilder.struct().name("com.example.Person")
        .field("firstName", Schema.STRING_SCHEMA)
        .field("lastName", Schema.STRING_SCHEMA)
        .field("age", Schema.INT32_SCHEMA)
        .field("threshold", Schema.OPTIONAL_FLOAT64_SCHEMA).build()

      val struct = new Struct(schema)
        .put("firstName", "Alex")
        .put("lastName", "Smith")
        .put("age", 30)

      val map = StructFieldsExtractorBytes(includeAllFields = true, Map.empty).get(struct).toMap

      Bytes.toString(map("firstName")) shouldBe "Alex"
      Bytes.toString(map("lastName")) shouldBe "Smith"
      Bytes.toInt(map("age")) shouldBe 30

    }

    "return all the fields and their bytes value when boolean is involved" in {
      val schema = SchemaBuilder.struct().name("com.example.Person")
        .field("firstName", Schema.STRING_SCHEMA)
        .field("lastName", Schema.STRING_SCHEMA)
        .field("age", Schema.INT32_SCHEMA)
        .field("threshold", Schema.OPTIONAL_FLOAT64_SCHEMA)
        .field("isRight", SchemaBuilder.bool().defaultValue(true).build()).build()

      val struct = new Struct(schema)
        .put("firstName", "Alex")
        .put("lastName", "Smith")
        .put("age", 30)
        .put("isRight", true)

      val map = StructFieldsExtractorBytes(includeAllFields = true, Map.empty).get(struct).toMap

      Bytes.toString(map("firstName")) shouldBe "Alex"
      Bytes.toString(map("lastName")) shouldBe "Smith"
      Bytes.toInt(map("age")) shouldBe 30
      Bytes.toBoolean(map("isRight")) shouldBe true
    }

    "handle bytes" in {
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]
      fields.add(new org.apache.avro.Schema.Field("data",
                                                  org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BYTES),
                                                  null,
                                                  null,
      ))
      val schema = org.apache.avro.Schema.createRecord("Data", "", "avro.test", false)
      schema.setFields(fields)

      val record = new GenericData.Record(schema)
      record.put("data", Array[Byte](1, 2, 3, 4, 5))

      val data           = new AvroData(4)
      val valueAndSchema = data.toConnectData(schema, record)
      val map = StructFieldsExtractorBytes(includeAllFields = true, Map.empty).get(
        valueAndSchema.value().asInstanceOf[Struct],
      ).toMap

      map("data") shouldBe Array[Byte](1, 2, 3, 4, 5)
    }

    "return all fields and apply the mapping" in {
      val schema = SchemaBuilder.struct().name("com.example.Person")
        .field("firstName", Schema.STRING_SCHEMA)
        .field("lastName", Schema.STRING_SCHEMA)
        .field("age", Schema.INT32_SCHEMA)
        .field("threshold", Schema.OPTIONAL_FLOAT64_SCHEMA).build()

      val struct = new Struct(schema)
        .put("firstName", "Alex")
        .put("lastName", "Smith")
        .put("age", 30)

      val map =
        StructFieldsExtractorBytes(includeAllFields = true, Map("lastName" -> "Name", "age" -> "a")).get(struct).toMap

      Bytes.toString(map("firstName")) shouldBe "Alex"
      Bytes.toString(map("Name")) shouldBe "Smith"
      Bytes.toInt(map("a")) shouldBe 30

    }

    "return only the specified fields" in {
      val schema = SchemaBuilder.struct().name("com.example.Person")
        .field("firstName", Schema.STRING_SCHEMA)
        .field("lastName", Schema.STRING_SCHEMA)
        .field("age", Schema.INT32_SCHEMA)
        .field("threshold", Schema.OPTIONAL_FLOAT64_SCHEMA).build()

      val struct = new Struct(schema)
        .put("firstName", "Alex")
        .put("lastName", "Smith")
        .put("age", 30)

      val map = StructFieldsExtractorBytes(includeAllFields = false, Map("lastName" -> "Name", "age" -> "age")).get(
        struct,
      ).toMap

      Bytes.toString(map("Name")) shouldBe "Smith"
      Bytes.toInt(map("age")) shouldBe 30

      map.size shouldBe 2
    }
  }

  "format timestamp field in struct as UTC" in {

    val date = java.util.Date.from(
      LocalDateTime
        .of(2012, 6, 28, 5, 59, 1, 500000000)
        .atZone(ZoneId.of("GMT")).toInstant,
    )

    val isoFormatted = "2012-06-28T05:59:01.500Z"

    val schema = SchemaBuilder.struct().name("com.example.Person")
      .field("myTimestamp", Timestamp.SCHEMA)
      .build()

    val map = StructFieldsExtractorBytes(
      includeAllFields = false,
      Map(
        "myTimestamp" -> "myTimestamp",
      ),
    ).get(new Struct(schema)
      .put("myTimestamp", date)).toMap

    map.size shouldBe 1
    Bytes.toString(map("myTimestamp")) shouldBe isoFormatted

  }

  "format time field in struct as UTC" in {

    val timeOnEpoch = LocalTime
      .of(12, 15, 30, 5)
      .atDate(LocalDate.ofEpochDay(0))
      .atZone(ZoneId.of("GMT"))

    val isoFormatted = DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ").withZone(ZoneId.of("UTC")).format(timeOnEpoch)

    val schema = SchemaBuilder.struct().name("com.example.Person")
      .field("myTime", Time.SCHEMA)
      .build()

    val struct = new Struct(schema)
      .put("myTime", java.util.Date.from(timeOnEpoch.toInstant))

    val map = StructFieldsExtractorBytes(
      includeAllFields = false,
      Map(
        "myTime" -> "myTime",
      ),
    ).get(struct).toMap

    map.size shouldBe 1
    Bytes.toString(map("myTime")) shouldBe isoFormatted

  }

}
