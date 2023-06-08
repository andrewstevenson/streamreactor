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
package io.lenses.streamreactor.connect.aws.s3.sink.conversion

import io.lenses.streamreactor.connect.aws.s3.formats.writer.MapSinkData
import io.lenses.streamreactor.connect.aws.s3.formats.writer.NullSinkData
import io.lenses.streamreactor.connect.aws.s3.formats.writer.StringSinkData
import org.apache.kafka.connect.data.SchemaBuilder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapSinkDataConverterTest extends AnyFlatSpec with Matchers {

  "convert" should "convert null values in maps to NullSinkData" in {
    MapSinkDataConverter(
      Map(
        "key1" -> "val1",
        "key2" -> null,
      ),
      None,
    ) should be(MapSinkData(Map(StringSinkData("key1") -> StringSinkData("val1"),
                                StringSinkData("key2") -> NullSinkData(),
    )))
  }

  "convert" should "convert null values in maps to NullSinkData with schemas" in {
    val schema = SchemaBuilder.map(SchemaBuilder.string().build(), SchemaBuilder.string().optional().build()).build()
    MapSinkDataConverter(
      Map(
        "key1" -> "val1",
        "key2" -> null,
      ),
      Some(schema),
    ) should be(
      MapSinkData(
        Map(StringSinkData("key1") -> StringSinkData("val1"), StringSinkData("key2") -> NullSinkData()),
        Some(schema),
      ),
    )
  }

}
