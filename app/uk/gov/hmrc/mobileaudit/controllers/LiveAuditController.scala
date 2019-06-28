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

import cats.implicits._
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}
@Singleton()
class LiveAuditController @Inject()(
  override val controllerComponents: ControllerComponents,
  override val authConnector:        AuthConnector,
  auditConnector:                    AuditConnector,
  @Named("auditSource") auditSource: String
)(
  implicit val ec: ExecutionContext
) extends BackendBaseController
    with AuthorisedFunctions {

  def getNinoFromDetailBody(body: Map[String, String]): Option[String] =
    body match {
      case detailBody if detailBody.keySet.contains("nino") => Some(detailBody("nino"))
      case _                                                => None
    }

  def auditOneEvent(journeyId: Option[String]): Action[IncomingAuditEvent] =
    Action.async(controllerComponents.parsers.json[IncomingAuditEvent]) { implicit request =>
      withNinoFromAuth(forwardAuditEvent(_, request.body).map(_ => NoContent), getNinoFromDetailBody(request.body.detail))
    }

  def auditManyEvents(journeyId: Option[String]): Action[IncomingAuditEvents] =
    Action.async(controllerComponents.parsers.json[IncomingAuditEvents]) { implicit request =>
      withNinoFromAuth(
        ninoFromAuth =>
          request.body.events
            .traverse(forwardAuditEvent(ninoFromAuth, _))
            .map(_ => NoContent),
        getNinoFromDetailBody(request.body.events.head.detail) //Assume all events have the same nino in each event
      )
    }

  def forwardAuditEvent(nino: String, incomingEvent: IncomingAuditEvent)(implicit hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendEvent(DataEventBuilder.buildEvent(auditSource, nino, incomingEvent, hc))


  private def withNinoFromAuth(f: String => Future[Result], suppliedNino: Option[String])(implicit hc: HeaderCarrier): Future[Result] =
    suppliedNino match {
      case None => Future.successful(BadRequest("Invalid details payload"))
      case Some(presentNino) =>
        authorised()
          .retrieve(Retrievals.nino) {
            case None => Future.successful(Unauthorized("Authorization failure [Not enrolled for NI]"))
            case Some(nino) if nino.toUpperCase == presentNino.toUpperCase => f(nino)
            case Some(nino) if nino.toUpperCase != presentNino.toUpperCase =>
              Future.successful(Unauthorized("Authorization failure [failed to validate Nino]"))
          }
          .recover {
            case e: NoActiveSession        => Unauthorized(s"Authorisation failure [${e.reason}]")
            case e: AuthorisationException => Forbidden(s"Authorisation failure [${e.reason}]")
          }
    }
}
