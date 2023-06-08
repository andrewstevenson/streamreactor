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
package io.lenses.streamreactor.connect.aws.s3.source

import io.lenses.streamreactor.connect.aws.s3.model.location.S3Location
import io.lenses.streamreactor.connect.aws.s3.source.ContextConstants.ContainerKey
import io.lenses.streamreactor.connect.aws.s3.source.ContextConstants.LineKey
import io.lenses.streamreactor.connect.aws.s3.source.ContextConstants.PathKey
import io.lenses.streamreactor.connect.aws.s3.source.ContextConstants.PrefixKey
import io.lenses.streamreactor.connect.aws.s3.source.ContextConstants.TimeStampKey

import java.time.Instant

object SourceRecordConverter {

  def fromSourcePartition(root: S3Location): Map[String, String] =
    Map(
      ContainerKey -> root.bucket,
      PrefixKey    -> root.prefixOrDefault(),
    )

  def fromSourceOffset(bucketAndPath: S3Location, offset: Long, lastModified: Instant): Map[String, AnyRef] =
    Map(
      PathKey      -> bucketAndPath.pathOrUnknown,
      LineKey      -> offset.toString,
      TimeStampKey -> lastModified.toEpochMilli.toString,
    )

}
