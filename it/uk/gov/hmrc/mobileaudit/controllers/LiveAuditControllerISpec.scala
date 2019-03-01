package uk.gov.hmrc.mobileaudit.controllers
import play.api.libs.json.Json
import uk.gov.hmrc.mobileaudit.support.BaseISpec

class LiveAuditControllerISpec extends BaseISpec {

  "when a single event is sent to the audit service it" should {
    val event = IncomingEvent("event-name", IncomingEventData("audit-type", None, Map(), None))

    "respond with 204 NoBody" in {
      val response = await(wsUrl("/mobile-audit/audit-event").addHttpHeaders("content-type" -> "application/json").post(Json.toJson(event)))
      response.status shouldBe 204
    }
  }
}
