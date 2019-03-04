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

package uk.gov.hmrc.mobileaudit.controllers

import akka.stream.Materializer
import cats.implicits._
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.controller.{BackendBaseController, BackendHeaderCarrierProvider}

import scala.concurrent.{ExecutionContext, Future}
@Singleton()
class LiveAuditController @Inject()(
  override val controllerComponents: ControllerComponents,
  override val authConnector:        AuthConnector,
  auditConnector:                    AuditConnector,
  @Named("auditSource") auditSource: String
)(
  implicit val ec: ExecutionContext,
  val mat:         Materializer
) extends BackendBaseController
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions {

  def auditOneEvent(journeyId: Option[String]): Action[IncomingEventData] =
    Action.async(controllerComponents.parsers.json[IncomingEventData]) { implicit request =>
      withNinoFromAuth(forwardAuditEvent(_, request.body).map(_ => NoContent))
    }

  def auditManyEvents(journeyId: Option[String]): Action[List[IncomingEventData]] =
    Action.async(controllerComponents.parsers.json[List[IncomingEventData]]) { implicit request =>
      withNinoFromAuth { ninoFromAuth =>
        request.body
          .traverse(forwardAuditEvent(ninoFromAuth, _))
          .map(_ => NoContent)
      }
    }

  def forwardAuditEvent(nino: String, incomingEvent: IncomingEventData)(implicit hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendEvent(DataEventBuilder.buildEvent(auditSource, nino, incomingEvent, hc))

  private def withNinoFromAuth(f: String => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    authConnector
      .authorise(EmptyPredicate, Retrievals.nino)
      .flatMap {
        case Some(nino) => f(nino)
        case None       => Future.successful(Unauthorized("Authorization failure [user is not enrolled for NI]"))
      }
      .recover {
        case e: NoActiveSession        => Unauthorized(s"Authorisation failure [${e.reason}]")
        case e: AuthorisationException => Forbidden(s"Authorisation failure [${e.reason}]")
      }
}
