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
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.mobileaudit.services._
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.controller.{BackendBaseController, BackendHeaderCarrierProvider}

import scala.concurrent.ExecutionContext
@Singleton()
class LiveAuditController @Inject()(
  override val controllerComponents: ControllerComponents,
  override val authConnector:        AuthConnector,
  auditForwardingService:            AuditForwardingService
)(
  implicit val ec: ExecutionContext,
  val mat:         Materializer
) extends BackendBaseController
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions {

  /**
    * So here's an interesting thing I found trying to unit test this action. If you use `parser.json`, which creates
    * an `Action[JsValue]` and construct a `FakeRequest` in the test with a json body and the right content-type header
    * then calling the `Action` function works just fine. But if, as I am here, you're using the json parser that also
    * validates the json to the right type of object then exactly same test setup will fail with a low-level
    * exception claiming that the byte array for the body is empty. Somewhere deep in the bowels of the akka streams
    * handling the `FakeRequest` is not doing the right thing, I suspect, as the request works fine in the running service.
    *
    * The upshot is that I have extracted the guts of the handling to `AuditService` (which is a good thing anyway) and
    * reduced this action to just calling the service, and there is no unit test for it. The integration test will
    * provide coverage.
    */
  def audit(journeyId: Option[String]): Action[IncomingEvent] =
    Action.async(controllerComponents.parsers.json[IncomingEvent]) { implicit request =>
      auditForwardingService.forwardAuditEvent(request.body).map(decodeOutcome)
    }

  private[controllers] def decodeOutcome(outcome: AuditOutcome): Result =
    outcome match {
      case NotAuthorized(msg)                          => Unauthorized(msg)
      case NotAllowed(msg)                             => Forbidden(msg)
      case AuditForwarded(AuditResult.Disabled)        => InternalServerError("Audit logging is disabled!")
      case AuditForwarded(AuditResult.Failure(msg, _)) => InternalServerError(msg)
      case AuditForwarded(AuditResult.Success)         => NoContent
    }
}
