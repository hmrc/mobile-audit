/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.mobileaudit.controllers.{DataEventBuilder, IncomingAuditEvent}

class DataEventBuilderSpec extends FreeSpecLike with Matchers with MockFactory with ScalaFutures with OptionValues {
  "when building a DataEvent" - {
    import uk.gov.hmrc.mobileaudit.controllers.DataEventBuilder._

    val pathKey            = "path"
    val transactionNameKey = "transactionName"

    "and no tag for the path is provided" - {
      val incomingEvent = IncomingAuditEvent("audit-type", None, None, None, Map("nino" -> "SOMENINO"))
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())
      "then the path tag should be set to the incoming audit type" in {
        dataEvent.tags.get(pathKey) shouldBe Some(incomingEvent.auditType)
      }
    }

    "and a tag for the path is provided" - {
      val pathValue     = "path-value"
      val incomingEvent = IncomingAuditEvent("audit-type", None, None, Some("path-value"), Map("nino" -> "SOMENINO"))
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())

      "then it should be copied to the DataEvent" in {
        dataEvent.tags.get(pathKey) shouldBe Some(pathValue)
      }
    }

    "and no tag for the transactionName is provided" - {
      val incomingEvent = IncomingAuditEvent("audit-type", None, None, None, Map("nino" -> "SOMENINO"))
      val dataEvent     = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())
      "then the transactionName should be set to the default" in {
        dataEvent.tags.get(transactionNameKey) shouldBe Some(DataEventBuilder.defaultTransactionName)
      }
    }

    "and a tag for the transactionName is provided" - {
      val transactionNameValue = "transaction-name-value"
      val tags                 = Map(transactionNameKey -> transactionNameValue)
      val incomingEvent =
        IncomingAuditEvent("audit-type", None, Some(transactionNameValue), None, Map("nino" -> "SOMENINO"))
      val dataEvent = buildEvent("audit-source", "nino-value", incomingEvent, HeaderCarrier())

      "then it should be copied to the DataEvent" in {
        dataEvent.tags.get(transactionNameKey) shouldBe Some(transactionNameValue)
      }
    }

    "any nino in the detail section of the incoming event" - {
      val detail            = Map(ninoKey -> "bogus-nino-value")
      val expectedNinoValue = "expected-nino-value"
      val incomingEvent     = IncomingAuditEvent("audit-type", None, None, None, detail)
      val dataEvent         = buildEvent("audit-source", expectedNinoValue, incomingEvent, HeaderCarrier())
      "should be replaced with the nino value supplied to the buildEvent function" in {
        dataEvent.detail.get(ninoKey) shouldBe Some(expectedNinoValue)
      }
    }
  }
}
