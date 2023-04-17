/*
 * Copyright 2023 HM Revenue & Customs
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
import javax.inject.Inject
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.mobileaudit.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.ExecutionContext

class SandboxAuditController @Inject() (val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendBaseController {

  def auditOneEvent(journeyId: JourneyId): Action[IncomingAuditEvent] =
    Action(controllerComponents.parsers.json[IncomingAuditEvent]) { implicit request =>
      NoContent
    }

  def auditManyEvents(journeyId: JourneyId): Action[IncomingAuditEvents] =
    Action(controllerComponents.parsers.json[IncomingAuditEvents]) { implicit request =>
      NoContent
    }
}
