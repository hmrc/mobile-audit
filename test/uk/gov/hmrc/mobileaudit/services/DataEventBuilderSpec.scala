/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.mobileaudit.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpecLike, Matchers, OptionValues}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobileaudit.controllers.{DataEventBuilder, IncomingEventData}

class DataEventBuilderSpec extends FreeSpecLike with Matchers with MockFactory with ScalaFutures with OptionValues {
  "when building a DataEvent" - {
    import uk.gov.hmrc.mobileaudit.controllers.DataEventBuilder._

    "and no tag for the path is provided" - {
      val incomingEvent = IncomingEventData("audit-type", None, Map(), None)
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())
      "then the path tag should be set to the incoming audit type" in {
        dataEvent.tags.get(pathKey) shouldBe Some(incomingEvent.auditType)
      }
    }

    "and a tag for the path is provided" - {
      val pathValue     = "path-value"
      val tags          = Map(pathKey -> pathValue)
      val incomingEvent = IncomingEventData("audit-type", None, Map(), Some(tags))
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())

      "then it should be copied to the DataEvent" in {
        dataEvent.tags.get(pathKey) shouldBe Some(pathValue)
      }
    }

    "and no tag for the transactionName is provided" - {
      val incomingEvent = IncomingEventData("audit-type", None, Map(), None)
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())
      "then the transactionName should be set to the default" in {
        dataEvent.tags.get(transactionNameKey) shouldBe Some(DataEventBuilder.defaultTransactionName)
      }
    }

    "and a tag for the transactionName is provided" - {
      val transactionNameValue = "transaction-name-value"
      val tags                 = Map(transactionNameKey -> transactionNameValue)
      val incomingEvent        = IncomingEventData("audit-type", None, Map(), Some(tags))
      val dataEvent            = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())

      "then it should be copied to the DataEvent" in {
        dataEvent.tags.get(transactionNameKey) shouldBe Some(transactionNameValue)
      }
    }

    "any nino in the detail section of the incoming event" - {
      val detail            = Map(ninoKey -> "bogus-nino-value")
      val expectedNinoValue = "expected-nino-value"
      val incomingEvent     = IncomingEventData("audit-type", None, detail, None)
      val dataEvent         = buildEvent("audit-source", expectedNinoValue, incomingEvent, HeaderCarrier())
      "should be replaced with the nino value supplied to the buildEvent function" in {
        dataEvent.detail.get(ninoKey) shouldBe Some(expectedNinoValue)
      }
    }

    "none of the supplied tags other than path and transactionName should be copied" in {
      val otherKey1 = "other-key-1"
      val otherKey2 = "other-key-2"
      val tags = Map(
        transactionNameKey -> "transaction-name-value",
        pathKey            -> "path-value",
        otherKey1          -> "other-value-1",
        otherKey2          -> "other-value-2"
      )
      val incomingEvent = IncomingEventData("audit-type", None, Map(), Some(tags))
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())

      dataEvent.tags.get(otherKey1) shouldBe None
      dataEvent.tags.get(otherKey2) shouldBe None
    }
  }
}
