package uk.gov.hmrc.mobileaudit

import play.api.libs.json.Json
import uk.gov.hmrc.mobileaudit.controllers.IncomingEventData
import uk.gov.hmrc.mobileaudit.utils.BaseISpec

class SandboxAuditControllerISpec extends BaseISpec {

  def externalServices: Seq[String] = Seq()

  val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "This integration test" - {
    "call the sandbox controller for /audit-event" in {
      val incomingEvent = IncomingEventData("audit-type", None, Map(), None)

      val response = await(wsUrl("/mobile-audit/audit-event").addHttpHeaders(mobileHeader).post(Json.toJson(incomingEvent)))
      response.status shouldBe 204
    }

    "call the sandbox controller for /audit-events" in {
      val incomingEvent = IncomingEventData("audit-type", None, Map(), None)

      val response = await(wsUrl("/mobile-audit/audit-events").addHttpHeaders(mobileHeader).post(Json.toJson(List(incomingEvent))))
      response.status shouldBe 204
    }
  }
}
