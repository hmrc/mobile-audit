package uk.gov.hmrc.mobileaudit
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.joda.time.DateTime
import org.scalatest.OptionValues
import play.api.libs.json._
import uk.gov.hmrc.mobileaudit.controllers.IncomingEventData
import uk.gov.hmrc.mobileaudit.stubs.{AuditStub, AuthStub}
import uk.gov.hmrc.mobileaudit.utils.BaseISpec
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.collection.JavaConverters._

/**
  * The unit tests check that various forms of `IncomingEvent` are correctly translated the the
  * equivalent `DataEvent`, so just do some basic sanity checks that the event that is sent matches
  * the event we generated
  */
class AuditOneISpec extends BaseISpec with OptionValues {
  implicit val jodaDateReads: Reads[DateTime]  = play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
  implicit val readDataEvent: Reads[DataEvent] = Json.reads

  "a single event sent to /audit-one" - {
    "should be forwarded to the audit service" in {
      val auditSource   = app.configuration.underlying.getString("auditSource")
      val auditType     = "audit-type"
      val testNino      = "AA100000Z"
      val incomingEvent = IncomingEventData(auditType, None, Map(), None)

      AuthStub.userIsLoggedIn(testNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/mobile-audit/audit-event").post(Json.toJson(incomingEvent)))
      response.status shouldBe 204

      verifyAuditEventWasForwarded()

      val auditRequest: ServeEvent = getAllServeEvents.asScala.find(_.getRequest.getUrl == "/write/audit").value
      val dataEvent = Json.parse(auditRequest.getRequest.getBodyAsString).validate[DataEvent].get

      dataEvent.auditSource              shouldBe auditSource
      dataEvent.auditType                shouldBe incomingEvent.auditType
      dataEvent.detail.get("nino").value shouldBe testNino
    }
  }

  private def verifyAuditEventWasForwarded(): Unit =
    wireMockServer.verify(
      1,
      postRequestedFor(urlPathEqualTo("/write/audit"))
        .withHeader("content-type", equalTo("application/json")))
}
