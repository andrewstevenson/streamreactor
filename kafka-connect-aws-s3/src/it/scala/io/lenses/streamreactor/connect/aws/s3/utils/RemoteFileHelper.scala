/*
 * Copyright 2021 Lenses.io
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

import cats.implicits.catsSyntaxOptionId
import com.google.common.io.ByteStreams
import io.lenses.streamreactor.connect.cloud.common.storage.ResultProcessors.processAsKey
import io.lenses.streamreactor.connect.cloud.common.config.ObjectMetadata
import io.lenses.streamreactor.connect.cloud.common.storage.StorageInterface

import java.io.File
import java.io.InputStream
import java.nio.file.Files

class RemoteFileHelper(storageInterface: StorageInterface) {

  def listBucketPath(bucketName: String, prefix: String): List[String] =
    storageInterface.listRecursive(bucketName, prefix.some, processAsKey) match {
      case Left(value)  => throw new RuntimeException(value.exception)
      case Right(value) => value.map(_.files).toList.flatten
    }

  def remoteFileAsBytes(bucketName: String, fileName: String): Array[Byte] =
    streamToByteArray(remoteFileAsStream(bucketName, fileName))

  def localFileAsBytes(localFile: File): Array[Byte] =
    Files.readAllBytes(localFile.toPath)

  def remoteFileAsStream(bucketName: String, fileName: String): InputStream =
    storageInterface.getBlob(bucketName, fileName) match {
      case Left(value)  => throw new RuntimeException(value.exception)
      case Right(value) => value
    }

  def remoteFileAsString(bucketName: String, fileName: String): String =
    streamToString(remoteFileAsStream(bucketName, fileName))

  def streamToString(inputStream: InputStream): String =
    new String(streamToByteArray(inputStream)).replace("\n", "")

  private def streamToByteArray(inputStream: InputStream): Array[Byte] =
    ByteStreams.toByteArray(inputStream)

  def getMetadata(bucket: String, path: String): ObjectMetadata =
    storageInterface.getMetadata(bucket, path) match {
      case Left(value)  => throw new RuntimeException(value.exception)
      case Right(value) => value
    }

}
