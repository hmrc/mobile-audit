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
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditForwardingServiceSpec extends FreeSpecLike with Matchers with MockFactory with ScalaFutures with OptionValues {

  val auditData = IncomingEvent("test", IncomingEventData("audit type", None, Map(), None))

  val auditConnector: AuditConnector = new AuditConnector {
    override def auditingConfig: AuditingConfig = AuditingConfig(None, enabled = true, "source")

    override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] =
      Future.successful(AuditResult.Success)
  }

  val authConnector: AuthConnector =
    mock[AuthConnector]

  "Forwarding a valid audit event" - {
    "should return AuditForwarded" in {
      val service = new AuditForwardingServiceImpl(auditConnector, authConnector)
      val result  = service.forwardAuditEvent("nino", auditData)(HeaderCarrier()).futureValue

      result shouldBe AuditResult.Success
    }
  }

  "when building a DataEvent" - {
    import AuditForwardingService._

    "and no tag for the path is provided" - {
      val incomingEvent = IncomingEvent("event", IncomingEventData("audit-type", None, Map(), None))
      val dataEvent     = buildEvent("nino-value", incomingEvent, HeaderCarrier())
      "then the path tag should be set to the incoming audit type" in {
        dataEvent.tags.get(pathKey) shouldBe Some(incomingEvent.data.auditType)
      }
    }

    "and a tag for the path is provided" - {
      val pathValue     = "path-value"
      val tags          = Map(pathKey -> pathValue)
      val incomingEvent = IncomingEvent("event", IncomingEventData("audit-type", None, Map(), Some(tags)))
      val dataEvent     = buildEvent("nino-value", incomingEvent, HeaderCarrier())

      "then it should be copied to the DataEvent" in {
        dataEvent.tags.get(pathKey) shouldBe Some(pathValue)
      }
    }

    "and no tag for the transactionName is provided" - {
      val incomingEvent = IncomingEvent("event", IncomingEventData("audit-type", None, Map(), None))
      val dataEvent     = buildEvent("nino-value", incomingEvent, HeaderCarrier())
      "then the transactionName should be set to the default" in {
        dataEvent.tags.get(transactionNameKey) shouldBe Some(AuditForwardingService.defaultTransactionName)
      }
    }

    "and a tag for the transactionName is provided" - {
      val transactionNameValue = "transaction-name-value"
      val tags                 = Map(transactionNameKey -> transactionNameValue)
      val incomingEvent        = IncomingEvent("event", IncomingEventData("audit-type", None, Map(), Some(tags)))
      val dataEvent            = buildEvent("nino-value", incomingEvent, HeaderCarrier())

      "then it should be copied to the DataEvent" in {
        dataEvent.tags.get(transactionNameKey) shouldBe Some(transactionNameValue)
      }
    }

    "any nino in the detail section of the incoming event" - {
      val detail            = Map(ninoKey -> "bogus-nino-value")
      val expectedNinoValue = "expected-nino-value"
      val incomingEvent     = IncomingEvent("event", IncomingEventData("audit-type", None, detail, None))
      val dataEvent         = buildEvent(expectedNinoValue, incomingEvent, HeaderCarrier())
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
      val incomingEvent = IncomingEvent("event", IncomingEventData("audit-type", None, Map(), Some(tags)))
      val dataEvent     = buildEvent("nino-value", incomingEvent, HeaderCarrier())

      dataEvent.tags.get(otherKey1) shouldBe None
      dataEvent.tags.get(otherKey2) shouldBe None
    }
  }
}
