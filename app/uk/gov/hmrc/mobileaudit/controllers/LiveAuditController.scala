/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logger
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobileaudit.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class LiveAuditController @Inject() (
  override val controllerComponents: ControllerComponents,
  override val authConnector:        AuthConnector,
  auditConnector:                    AuditConnector,
  @Named("auditSource") auditSource: String
)(implicit val ec:                   ExecutionContext)
    extends BackendBaseController
    with AuthorisedFunctions {

  val logger: Logger = Logger(this.getClass)

  def getNinoFromDetailBody(body: Map[String, String]): Option[String] =
    body match {
      case detailBody if detailBody.keySet.contains("nino") => Some(detailBody("nino"))
      case _                                                => None
    }

  def getHeadFromDetails(request: Request[IncomingAuditEvents]): Option[String] =
    request.body.events.headOption match {
      case Some(header) => getNinoFromDetailBody(header.detail)
      case _            => None
    }

  def auditOneEvent(journeyId: JourneyId): Action[IncomingAuditEvent] =
    Action.async(controllerComponents.parsers.json[IncomingAuditEvent]) { implicit request =>
      withNinoFromAuth(forwardAuditEvent(_, request.body).map(_ => NoContent),
                       getNinoFromDetailBody(request.body.detail))
    }

  def auditManyEvents(journeyId: JourneyId): Action[IncomingAuditEvents] =
    Action.async(controllerComponents.parsers.json[IncomingAuditEvents]) { implicit request =>
      withNinoFromAuth(
        ninoFromAuth =>
          request.body.events
            .traverse(forwardAuditEvent(ninoFromAuth, _))
            .map(_ => NoContent),
        getHeadFromDetails(request) //Assume all events have the same nino in each event
      )
    }

  def forwardAuditEvent(
    nino:          String,
    incomingEvent: IncomingAuditEvent
  )(implicit hc:   HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendEvent(DataEventBuilder.buildEvent(auditSource, nino, incomingEvent, hc))

  private def withNinoFromAuth(
    f:            String => Future[Result],
    suppliedNino: Option[String]
  )(implicit hc:  HeaderCarrier
  ): Future[Result] =
    suppliedNino match {
      case None => Future.successful(BadRequest("Invalid details payload"))
      case Some(presentNino) =>
        authorised()
          .retrieve(Retrievals.nino) {
            case None =>
              logger.warn("Authorization failure [Not enrolled for NI]")
              Future.successful(Unauthorized("Invalid credentials"))
            case Some(nino) if nino.toUpperCase == presentNino.toUpperCase => f(nino)
            case Some(nino) if nino.toUpperCase != presentNino.toUpperCase =>
              logger.warn("Authorization failure [failed to validate Nino]")
              Future.successful(Unauthorized("Invalid credentials"))
          }
          .recover {
            case e: NoActiveSession =>
              logger.warn(s"Authorisation failure [${e.reason}]")
              Unauthorized(s"Invalid credentials")
            case e: AuthorisationException =>
              logger.warn(s"Authorisation failure [${e.reason}]")
              Forbidden(s"Invalid credentials")
            case e: Exception =>
              logger.warn(e.getMessage)
              InternalServerError("Error occurred creating audit event")
          }
    }
}
