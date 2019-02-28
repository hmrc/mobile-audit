package uk.gov.hmrc.mobileaudit.services
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

