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
package com.landoop.streamreactor.connect.hive.orc

import com.landoop.streamreactor.connect.hive.OrcSinkConfig
import com.landoop.streamreactor.connect.hive.OrcSourceConfig
import com.landoop.streamreactor.connect.hive.StructUtils
import com.landoop.streamreactor.connect.hive.orc
import com.landoop.streamreactor.connect.hive.kerberos.UgiExecute
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OrcTest extends AnyFlatSpec with Matchers {

  implicit val conf = new Configuration()
  implicit val fs   = FileSystem.getLocal(conf)

  "Orc" should "read and write orc files" in {

    val schema = SchemaBuilder.struct()
      .field("name", SchemaBuilder.string().optional().build())
      .field("age", SchemaBuilder.int32().optional().build())
      .field("salary", SchemaBuilder.float64().optional().build())
      .name("from_orc")
      .build()

    val users = Seq(
      new Struct(schema).put("name", "sammy").put("age", 38).put("salary", 54.67),
      new Struct(schema).put("name", "laura").put("age", 37).put("salary", 91.84),
    )

    val path = new Path("orctest.orc")
    val sink = orc.sink(path, schema, OrcSinkConfig(overwrite = true))
    users.foreach(sink.write)
    sink.close()

    val source = orc.source(path, OrcSourceConfig(), UgiExecute.NoOp)
    val actual = source.iterator.toList
    actual.head.schema shouldBe schema
    actual.map(StructUtils.extractValues) shouldBe
      List[Vector[Any]](Vector("sammy", 38, 54.67), Vector("laura", 37, 91.84))

    fs.delete(path, false)
  }
}
