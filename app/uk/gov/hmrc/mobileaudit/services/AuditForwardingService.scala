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
import com.google.inject.ImplementedBy
import javax.inject.Inject
import org.joda.time.DateTime
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, NoActiveSession}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.AuditExtensions._

import scala.concurrent.{ExecutionContext, Future}

sealed trait AuditOutcome
case class NotAuthorized(msg:          String) extends AuditOutcome
case class NotAllowed(msg:             String) extends AuditOutcome
case class AuditForwarded(auditResult: AuditResult) extends AuditOutcome

@ImplementedBy(classOf[AuditForwardingServiceImpl])
trait AuditForwardingService {
  def forwardAuditEvent(incomingEvent: IncomingEvent)(implicit hc: HeaderCarrier): Future[AuditOutcome]
}

class AuditForwardingServiceImpl @Inject()(auditConnector: AuditConnector, authConnector: AuthConnector)(
  implicit val ec:                               ExecutionContext
) extends AuditForwardingService {

  override def forwardAuditEvent(incomingEvent: IncomingEvent)(implicit hc: HeaderCarrier): Future[AuditOutcome] = {
    val tags        = incomingEvent.data.tags.getOrElse(Map())
    val generatedAt = incomingEvent.data.generatedAt.map(d => new DateTime(d.toInstant.toEpochMilli)).getOrElse(DateTime.now())

    withNinoFromAuth { ninoFromAuth =>
      val event = DataEvent(
        "native-apps",
        incomingEvent.data.auditType,
        tags        = hc.toAuditTags(tags.getOrElse("transactionName", "explicitAuditEvent")),
        detail      = incomingEvent.data.detail ++ Map("nino" -> ninoFromAuth),
        generatedAt = generatedAt
      )
      auditConnector.sendEvent(event).map(AuditForwarded)
    }
  }
  private def withNinoFromAuth(f: String => Future[AuditOutcome])(implicit hc: HeaderCarrier): Future[AuditOutcome] =
    authConnector
      .authorise(EmptyPredicate, Retrievals.nino)
      .flatMap {
        case Some(nino) => f(nino)
        case None       => Future.successful(NotAuthorized("Authorization failure [user is not enrolled for NI]"))
      }
      .recover {
        case e: NoActiveSession        => NotAuthorized(s"Authorisation failure [${e.reason}]")
        case e: AuthorisationException => NotAllowed(s"Authorisation failure [${e.reason}]")
      }
}

