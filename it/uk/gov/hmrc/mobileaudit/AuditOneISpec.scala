package uk.gov.hmrc.mobileaudit
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.joda.time.DateTime
import org.scalatest.OptionValues
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.mobileaudit.controllers.IncomingEventData
import uk.gov.hmrc.mobileaudit.stubs.AuthStub
import uk.gov.hmrc.mobileaudit.utils.BaseISpec
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.collection.JavaConversions._

// TODO: This is very raw and needs a lot of re-factoring to clean it up
class AuditOneISpec extends BaseISpec with OptionValues {
  implicit val jodaDateReads: Reads[DateTime]  = play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
  implicit val readDataEvent: Reads[DataEvent] = Json.reads

  "a single event sent to /audit-one" - {
    "with just a name and audit type (the minimum possible data)" - {
      "should " in {
        val incomingEvent = IncomingEventData("audit-type", None, Map(), None)

        AuthStub.userIsLoggedIn("event-nino")
        respondToAuditWithNoBody
        respondToAuditMergedWithNoBody

        val response = await(wsUrl("/mobile-audit/audit-event").post(Json.toJson(incomingEvent)))
        response.status shouldBe 204

        val pattern = postRequestedFor(urlPathEqualTo("/write/audit"))
          .withHeader("content-type", equalTo("application/json"))

        wireMockServer.verify(1, pattern)

        val auditRequest: ServeEvent = asScalaBuffer(getAllServeEvents).find(_.getRequest.getUrl == "/write/audit").value
        Logger.debug(auditRequest.getRequest.getBodyAsString)
        val sentAuditEvent = Json.parse(auditRequest.getRequest.getBodyAsString).validate[DataEvent].get

        sentAuditEvent.auditSource              shouldBe "native-apps"
        sentAuditEvent.auditType                shouldBe incomingEvent.auditType
        sentAuditEvent.detail.get("nino").value shouldBe "event-nino"
      }
    }
  }

  private def respondToAuditMergedWithNoBody =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/write/audit/merged"))
        .willReturn(aResponse()
          .withStatus(204)))

  private def respondToAuditWithNoBody =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/write/audit"))
        .willReturn(aResponse()
          .withStatus(204)))
}
