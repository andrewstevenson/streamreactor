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
package io.lenses.streamreactor.connect.aws.s3.formats.writer

import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import io.lenses.streamreactor.connect.aws.s3.formats.bytes.BytesOutputRow
import io.lenses.streamreactor.connect.aws.s3.formats.bytes.BytesWriteMode
import io.lenses.streamreactor.connect.aws.s3.sink.SinkError
import io.lenses.streamreactor.connect.aws.s3.stream.S3OutputStream

import scala.util.Try

class BytesFormatWriter(outputStream: S3OutputStream, bytesWriteMode: BytesWriteMode)
    extends S3FormatWriter
    with LazyLogging {

  private val writeKeys   = bytesWriteMode.entryName.contains("Key")
  private val writeValues = bytesWriteMode.entryName.contains("Value")
  private val writeSizes  = bytesWriteMode.entryName.contains("Size")
  private val byteOutputRow = BytesOutputRow(
    None,
    None,
    Array.empty,
    Array.empty,
  )
  override def write(messageDetail: MessageDetail): Either[Throwable, Unit] =
    for {
      key   <- if (writeKeys) convertToBytes(messageDetail.key) else Array.emptyByteArray.asRight
      value <- if (writeValues) convertToBytes(messageDetail.value) else Array.emptyByteArray.asRight
      data = byteOutputRow.copy(
        keySize   = if (writeKeys && writeSizes) Some(key.length.longValue()) else None,
        key       = key,
        valueSize = if (writeValues && writeSizes) Some(value.length.longValue()) else None,
        value     = value,
      ).toByteArray
      _ <- Try {
        outputStream.write(data)
        outputStream.flush()
      }.toEither
    } yield ()

  private def convertToBytes(sinkData: SinkData): Either[Throwable, Array[Byte]] =
    sinkData match {
      case NullSinkData(_)             => Array.emptyByteArray.asRight
      case ByteArraySinkData(array, _) => array.asRight
      case v =>
        new IllegalStateException(
          s"Non-binary content received: ${v.getClass.getName}. Please check your configuration. It is required the converter to be set to [org.apache.kafka.connect.converters.ByteArrayConverter].",
        ).asLeft
    }

  override def rolloverFileOnSchemaChange(): Boolean = false

  override def complete(): Either[SinkError, Unit] =
    for {
      closed <- outputStream.complete()
      _      <- Suppress(outputStream.flush())
      _      <- Suppress(outputStream.close())
    } yield closed

  override def getPointer: Long = outputStream.getPointer

}
