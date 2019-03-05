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

package uk.gov.hmrc.mobileaudit.controllers.api

import controllers.Assets
import javax.inject.{Inject, Singleton}
import play.api.http.HttpErrorHandler
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.mobileaudit.views.txt

case class ApiAccess(`type`: String, whitelistedApplicationIds: Seq[String])

object ApiAccess {
  implicit val writes: OWrites[ApiAccess] = Json.writes[ApiAccess]
}

@Singleton
class DocumentationController @Inject()(
  controllerComponents: ControllerComponents,
  assets:               Assets,
  apiAccess:            ApiAccess,
  errorHandler:         HttpErrorHandler)
    extends uk.gov.hmrc.api.controllers.DocumentationController(controllerComponents, assets, errorHandler) {

  override def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(apiAccess)).as(JSON)
  }

  override def conf(version: String, file: String): Action[AnyContent] =
    assets.at(s"/public/api/conf/$version", file)
}