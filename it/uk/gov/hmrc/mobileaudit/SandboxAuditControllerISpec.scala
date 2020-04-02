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
