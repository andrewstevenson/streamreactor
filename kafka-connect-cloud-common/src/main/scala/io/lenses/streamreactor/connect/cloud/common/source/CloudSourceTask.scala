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
package io.lenses.streamreactor.connect.cloud.common.source

import cats.effect.unsafe.implicits.global
import cats.effect.FiberIO
import cats.effect.IO
import cats.effect.Ref
import cats.implicits.catsSyntaxOptionId
import com.typesafe.scalalogging.LazyLogging
import io.lenses.streamreactor.common.config.base.traits.WithConnectorPrefix
import io.lenses.streamreactor.common.utils.AsciiArtPrinter.printAsciiHeader
import io.lenses.streamreactor.common.utils.JarManifest
import io.lenses.streamreactor.connect.cloud.common.config.traits.CloudSourceConfig
import io.lenses.streamreactor.connect.cloud.common.config.ConnectorTaskId
import io.lenses.streamreactor.connect.cloud.common.config.ConnectorTaskIdCreator
import io.lenses.streamreactor.connect.cloud.common.model.location.CloudLocation
import io.lenses.streamreactor.connect.cloud.common.model.location.CloudLocationValidator
import io.lenses.streamreactor.connect.cloud.common.source.distribution.CloudPartitionSearcher
import io.lenses.streamreactor.connect.cloud.common.source.distribution.PartitionSearcher
import io.lenses.streamreactor.connect.cloud.common.source.reader.PartitionDiscovery
import io.lenses.streamreactor.connect.cloud.common.source.reader.ReaderManager
import io.lenses.streamreactor.connect.cloud.common.source.reader.ReaderManagerState
import io.lenses.streamreactor.connect.cloud.common.source.state.CloudSourceTaskState
import io.lenses.streamreactor.connect.cloud.common.source.state.ReaderManagerBuilder
import io.lenses.streamreactor.connect.cloud.common.storage.DirectoryLister
import io.lenses.streamreactor.connect.cloud.common.storage.FileMetadata
import io.lenses.streamreactor.connect.cloud.common.storage.StorageInterface
import io.lenses.streamreactor.connect.cloud.common.utils.MapUtils
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters._
abstract class CloudSourceTask[MD <: FileMetadata, C <: CloudSourceConfig[MD], CT]
    extends SourceTask
    with LazyLogging
    with WithConnectorPrefix {

  def validator: CloudLocationValidator

  private val contextOffsetFn: CloudLocation => Option[CloudLocation] =
    SourceContextReader.getCurrentOffset(() => context)

  private val manifest = JarManifest(getClass.getProtectionDomain.getCodeSource.getLocation)

  @volatile
  private var s3SourceTaskState: Option[CloudSourceTaskState] = None

  @volatile
  private var cancelledRef: Option[Ref[IO, Boolean]] = None

  private var partitionDiscoveryLoop: Option[FiberIO[Unit]] = None

  implicit var connectorTaskId: ConnectorTaskId = _

  override def version(): String = manifest.version()

  /**
    * Start sets up readers for every configured connection in the properties
    */
  override def start(props: util.Map[String, String]): Unit = {

    printAsciiHeader(manifest, "/aws-s3-source-ascii.txt")

    logger.debug(s"Received call to S3SourceTask.start with ${props.size()} properties")

    val contextProperties: Map[String, String] =
      Option(context).flatMap(c => Option(c.configs()).map(_.asScala.toMap)).getOrElse(Map.empty)
    val mergedProperties: Map[String, String] = MapUtils.mergeProps(contextProperties, props.asScala.toMap)
    (for {
      result <- make(validator, connectorPrefix, mergedProperties, contextOffsetFn)
      fiber  <- result.partitionDiscoveryLoop.start
    } yield {
      s3SourceTaskState      = result.some
      cancelledRef           = result.cancelledRef.some
      partitionDiscoveryLoop = fiber.some
    }).unsafeRunSync()
  }

  override def stop(): Unit = {
    logger.info(s"Stopping S3 source task")
    (s3SourceTaskState, cancelledRef, partitionDiscoveryLoop) match {
      case (Some(state), Some(signal), Some(fiber)) => stopInternal(state, signal, fiber)
      case _                                        => logger.info("There is no state to stop.")
    }
    logger.info(s"Stopped S3 source task")
  }

  override def poll(): util.List[SourceRecord] =
    s3SourceTaskState.fold(Collections.emptyList[SourceRecord]()) { state =>
      state.poll().unsafeRunSync().asJava
    }

  private def stopInternal(state: CloudSourceTaskState, signal: Ref[IO, Boolean], fiber: FiberIO[Unit]): Unit = {
    (for {
      _ <- signal.set(true)
      _ <- state.close()
      // Don't join the fiber if it's already been cancelled. It will take potentially the interval time to complete
      // and this can create issues on Connect. The task will be terminated and the resource cleaned up by the GC.
      //_ <- fiber.join.timeout(1.minute).attempt.void
    } yield ()).unsafeRunSync()
    cancelledRef           = None
    partitionDiscoveryLoop = None
    s3SourceTaskState      = None
  }

  def createClient(config: C): Either[Throwable, CT]

  def make(
    validator:       CloudLocationValidator,
    connectorPrefix: String,
    props:           Map[String, String],
    contextOffsetFn: CloudLocation => Option[CloudLocation],
  ): IO[CloudSourceTaskState] =
    for {
      connectorTaskId <- IO.fromEither(new ConnectorTaskIdCreator(connectorPrefix).fromProps(props))
      config          <- IO.fromEither(convertPropsToConfig(connectorTaskId, props))
      s3Client        <- IO.fromEither(createClient(config))
      storageInterface: StorageInterface[MD] <- IO.delay(createStorageInterface(connectorTaskId, config, s3Client))

      directoryLister    <- IO.delay(createDirectoryLister(connectorTaskId, s3Client))
      partitionSearcher  <- IO.delay(createPartitionSearcher(directoryLister, connectorTaskId, config))
      readerManagerState <- Ref[IO].of(ReaderManagerState(Seq.empty, Seq.empty))
      cancelledRef       <- Ref[IO].of(false)
    } yield {
      val readerManagerCreateFn: (CloudLocation, String) => IO[ReaderManager] = (root, path) => {
        ReaderManagerBuilder(
          root,
          path,
          storageInterface,
          connectorTaskId,
          contextOffsetFn,
          location => config.bucketOptions.find(sb => sb.sourceBucketAndPrefix == location),
        )(validator)
      }
      val partitionDiscoveryLoop = PartitionDiscovery.run(connectorTaskId,
                                                          config.partitionSearcher,
                                                          partitionSearcher.find,
                                                          readerManagerCreateFn,
                                                          readerManagerState,
                                                          cancelledRef,
      )
      CloudSourceTaskState(readerManagerState.get.map(_.readerManagers), cancelledRef, partitionDiscoveryLoop)
    }

  def createStorageInterface(connectorTaskId: ConnectorTaskId, config: C, s3Client: CT): StorageInterface[MD]

  def convertPropsToConfig(connectorTaskId: ConnectorTaskId, props: Map[String, String]): Either[Throwable, C]

  def createDirectoryLister(connectorTaskId: ConnectorTaskId, s3Client: CT): DirectoryLister

  def getFilesLimit(config: C): CloudLocation => Either[Throwable, Int] = {
    cloudLocation =>
      config.bucketOptions.find(e => e.sourceBucketAndPrefix == cloudLocation).map(_.filesLimit).toRight(
        new IllegalStateException("Cannot find bucket in config to retrieve files limit"),
      )
  }

  def createPartitionSearcher(
    directoryLister: DirectoryLister,
    connectorTaskId: ConnectorTaskId,
    config:          C,
  ): PartitionSearcher =
    new CloudPartitionSearcher(
      getFilesLimit(config),
      directoryLister,
      config.bucketOptions.map(_.sourceBucketAndPrefix),
      config.partitionSearcher,
      connectorTaskId,
    )
}
