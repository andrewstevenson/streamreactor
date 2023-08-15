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
package io.lenses.streamreactor.connect.aws.s3.sink.commit

import org.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CountTest extends AnyFlatSpec with Matchers with EitherValues with MockitoSugar {

  private val count = Count(100)

  private def commitContext(currentCount: Long): CommitContext = {
    val cc = mock[CommitContext]
    when(cc.count).thenReturn(currentCount)
    cc
  }

  "count" should "return false when count not reached yet" in {
    count
      .eval(commitContext(99)) should
      be(ConditionCommitResult(false))
    count
      .eval(commitContext(99)) should
      be(ConditionCommitResult(false))
  }

  "count" should "return true when count reached" in {
    count
      .eval(commitContext(100)) should
      be(ConditionCommitResult(true))
    count
      .eval(commitContext(100)) should
      be(ConditionCommitResult(true))
  }

  "count" should "return true when count exceeded" in {
    count
      .eval(commitContext(101)) should
      be(ConditionCommitResult(true))
    count
      .eval(commitContext(101)) should
      be(ConditionCommitResult(true))
  }

}
