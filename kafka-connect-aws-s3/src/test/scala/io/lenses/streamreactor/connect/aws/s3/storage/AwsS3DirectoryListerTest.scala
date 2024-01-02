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
package io.lenses.streamreactor.connect.aws.s3.storage

import cats.effect.unsafe.implicits.global
import cats.implicits._
import io.lenses.streamreactor.connect.aws.s3.model.location.S3LocationValidator
import io.lenses.streamreactor.connect.cloud.common.config.ConnectorTaskId
import io.lenses.streamreactor.connect.cloud.common.model.location.CloudLocation
import io.lenses.streamreactor.connect.cloud.common.model.location.CloudLocationValidator
import io.lenses.streamreactor.connect.cloud.common.storage.DirectoryFindCompletionConfig
import io.lenses.streamreactor.connect.cloud.common.storage.DirectoryFindResults
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.s3.S3Client

import scala.jdk.CollectionConverters.IteratorHasAsScala

class AwsS3DirectoryListerTest extends AnyFlatSpecLike with Matchers {
  private implicit val cloudLocationValidator: CloudLocationValidator = S3LocationValidator

  private val connectorTaskId: ConnectorTaskId = ConnectorTaskId("sinkName", 1, 1)

  "lister" should "list all directories" in {

    val s3Client: S3Client = new MockS3Client(
      S3Page(
        "prefix1/1.txt",
        "prefix1/2.txt",
        "prefix2/3.txt",
        "prefix2/4.txt",
      ),
    )

    check(CloudLocation("bucket", "prefix1/".some), Set.empty, Set.empty, Set("prefix1/"), s3Client)

    check(CloudLocation("bucket", "prefix2/".some), Set.empty, Set.empty, Set("prefix2/"), s3Client)
    check(CloudLocation("bucket", "prefix3/".some), Set.empty, Set.empty, Set.empty, s3Client)
    check(CloudLocation("bucket", None), Set.empty, Set.empty, Set("prefix1/", "prefix2/"), s3Client)
  }

  "lister" should "list multiple pages" in {

    val s3Client: S3Client = new MockS3Client(
      S3Page(
        "prefix1/1.txt",
        "prefix1/2.txt",
        "prefix2/3.txt",
        "prefix2/4.txt",
      ),
      S3Page(
        "prefix3/5.txt",
        "prefix3/6.txt",
        "prefix4/7.txt",
        "prefix4/8.txt",
      ),
    )

    AwsS3DirectoryLister.findDirectories(
      CloudLocation("bucket", none),
      DirectoryFindCompletionConfig(1),
      Set.empty,
      Set.empty,
      s3Client.listObjectsV2Paginator(_).iterator().asScala,
      connectorTaskId,
    ).unsafeRunSync() should be(DirectoryFindResults(
      Set("prefix1/", "prefix2/", "prefix3/", "prefix4/"),
    ))
  }

  "lister" should "exclude directories" in {

    val s3Client: S3Client = new MockS3Client(
      S3Page(
        "prefix1/1.txt",
        "prefix1/2.txt",
        "prefix2/3.txt",
        "prefix2/4.txt",
      ),
      S3Page(
        "prefix3/5.txt",
        "prefix3/6.txt",
        "prefix4/7.txt",
        "prefix4/8.txt",
      ),
    )

    check(
      CloudLocation("bucket", none),
      Set("prefix1/", "prefix4/"),
      Set.empty,
      Set("prefix2/", "prefix3/"),
      s3Client,
      0,
    )
  }

  "lister" should "consider the connector task owning the partition" in {
    val taskId1 = ConnectorTaskId("sinkName", 2, 1)
    val taskId2 = ConnectorTaskId("sinkName", 2, 0)

    val s3Client: S3Client = new MockS3Client(
      S3Page(
        "prefix1/1.txt",
        "prefix1/2.txt",
        "prefix2/3.txt",
        "prefix2/4.txt",
      ),
      S3Page(
        "prefix3/5.txt",
        "prefix3/6.txt",
        "prefix4/7.txt",
        "prefix4/8.txt",
      ),
    )

    check(
      CloudLocation("bucket", none),
      Set.empty,
      Set.empty,
      Set("prefix2/", "prefix4/"),
      s3Client,
      1,
      taskId1,
    )

    check(
      CloudLocation("bucket", none),
      Set.empty,
      Set.empty,
      Set("prefix1/", "prefix3/"),
      s3Client,
      1,
      taskId2,
    )

    check(
      CloudLocation("bucket", none),
      Set("prefix2/", "prefix4/"),
      Set.empty,
      Set.empty,
      s3Client,
      0,
      taskId1,
    )

    check(
      CloudLocation("bucket", none),
      Set("prefix1/", "prefix3/"),
      Set.empty,
      Set.empty,
      s3Client,
      0,
      taskId2,
    )

    check(
      CloudLocation("bucket", none),
      Set("prefix2/"),
      Set.empty,
      Set("prefix4/"),
      s3Client,
      1,
      taskId1,
    )

    check(
      CloudLocation("bucket", none),
      Set("prefix1/"),
      Set.empty,
      Set("prefix3/"),
      s3Client,
      1,
      taskId2,
    )
  }

  private def check(
    location:         CloudLocation,
    exclude:          Set[String],
    wildcardExcludes: Set[String],
    expected:         Set[String],
    s3Client:         S3Client,
    recursiveLevel:   Int             = 1,
    connectorTaskId:  ConnectorTaskId = connectorTaskId,
  ): Unit = {
    val actual = AwsS3DirectoryLister.findDirectories(
      location,
      DirectoryFindCompletionConfig(recursiveLevel),
      exclude,
      wildcardExcludes,
      s3Client.listObjectsV2Paginator(_).iterator().asScala,
      connectorTaskId,
    ).unsafeRunSync()
    actual should be(DirectoryFindResults(expected))
    ()
  }

  "lister" should "exclude indexes directory when configured as wildcard exclude" in {

    val s3Client: S3Client = new MockS3Client(
      S3Page(
        ".indexes/sinkName/myTopic/00005/00000000000000000050",
        ".indexes/sinkName/myTopic/00005/00000000000000000070",
        ".indexes/sinkName/myTopic/00005/00000000000000000100",
        "prefix1/1.txt",
        "prefix1/2.txt",
        "prefix2/3.txt",
        "prefix2/4.txt",
      ),
    )

    check(CloudLocation("bucket", "prefix1/".some), Set.empty, Set(".indexes"), Set("prefix1/"), s3Client)

    check(CloudLocation("bucket", "prefix2/".some), Set.empty, Set(".indexes"), Set("prefix2/"), s3Client)
    check(CloudLocation("bucket", "prefix3/".some), Set.empty, Set(".indexes"), Set.empty, s3Client)
    check(CloudLocation("bucket", None), Set.empty, Set(".indexes"), Set("prefix1/", "prefix2/"), s3Client)
  }

}
