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
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditForwardingServiceSpec extends WordSpec with Matchers with MockFactory with ScalaFutures {

  val auditData = IncomingEvent("test", IncomingEventData("audit type", None, Map(), None))

  val auditConnector: AuditConnector = new AuditConnector {
    override def auditingConfig: AuditingConfig = AuditingConfig(None, enabled = true, "source")

    override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] =
      Future.successful(AuditResult.Success)
  }

  val authConnector: AuthConnector =
    mock[AuthConnector]

  (authConnector
    .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
    .expects(*, *, *, *)
    .returning(Future.successful(Some("nino")))

  "Forwarding a valid audit event" should {
    "return AuditForwarded" in {
      val service = new AuditForwardingServiceImpl(auditConnector, authConnector)
      val result  = service.forwardAuditEvent(auditData)(HeaderCarrier()).futureValue

      result                                          shouldBe an[AuditForwarded]
      result.asInstanceOf[AuditForwarded].auditResult shouldBe AuditResult.Success
    }
  }

}
