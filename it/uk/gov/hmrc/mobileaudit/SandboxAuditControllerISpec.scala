/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.mobileaudit

import play.api.libs.json.Json
import uk.gov.hmrc.mobileaudit.controllers.{IncomingAuditEvent, IncomingAuditEvents}
import uk.gov.hmrc.mobileaudit.utils.BaseISpec

class SandboxAuditControllerISpec extends BaseISpec {

  def externalServices: Seq[String] = Seq()

  val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "This integration test" - {
    "call the sandbox controller for /audit-event" in {
      val incomingEvent = IncomingAuditEvent("audit-type", None, None, None, Map("nino" -> "CS700100A"))

      val response = await(wsUrl(auditEventUrl).addHttpHeaders(mobileHeader).post(Json.toJson(incomingEvent)))
      response.status shouldBe 204
    }

    "call the sandbox controller for /audit-events" in {
      val incomingEvent = IncomingAuditEvents(events =
        List(IncomingAuditEvent("audit-type", None, None, None, Map("nino" -> "CS700100A")))
      )
      val response = await(wsUrl(auditEventsUrl).addHttpHeaders(mobileHeader).post(Json.toJson(incomingEvent)))
      response.status shouldBe 204
    }
  }
}
