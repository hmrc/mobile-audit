package uk.gov.hmrc.mobileaudit

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.DateTime
import org.scalatest.OptionValues
import play.api.libs.json._
import uk.gov.hmrc.mobileaudit.controllers.IncomingEventData
import uk.gov.hmrc.mobileaudit.stubs.{AuditStub, AuthStub}
import uk.gov.hmrc.mobileaudit.utils.BaseISpec
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.collection.JavaConverters._

class AuditManyISpec extends BaseISpec with OptionValues {
  implicit val jodaDateReads: Reads[DateTime]  = play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
  implicit val readDataEvent: Reads[DataEvent] = Json.reads

  "when multiple events are sent to /audit-events" - {
    "they should all be forwarded to the audit service" in {
      val auditType = "audit-type"
      val testNino  = "AA100000Z"
      val incomingEvents = (0 to 3).map { i =>
        IncomingEventData(s"$auditType-$i", None, Map(), None)
      }
      val auditSource = app.configuration.underlying.getString("auditSource")

      AuthStub.userIsLoggedIn(testNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/mobile-audit/audit-events").post(Json.toJson(incomingEvents)))
      response.status shouldBe 204

      verifyAuditEventsWereForwarded(incomingEvents.length)

      val dataEvents: List[DataEvent] = getAllServeEvents.asScala
        .filter(_.getRequest.getUrl == "/write/audit")
        .map(e => Json.parse(e.getRequest.getBodyAsString).validate[DataEvent].get)
        .toList

      // no more, no less
      dataEvents.length shouldBe incomingEvents.length

      // confirm that all the data events contain the values we expect
      dataEvents.foreach { dataEvent =>
        dataEvent.auditSource              shouldBe auditSource
        dataEvent.detail.get("nino").value shouldBe testNino
      }

      // Cross-check that each of the unique audit-type values from the incoming events are present
      // in the data events send to the audit service
      incomingEvents.indices.foreach { i =>
        val expectedAuditType = s"$auditType-$i"
        dataEvents.find(_.auditType == expectedAuditType) shouldBe a[Some[_]]
      }
    }
  }

  private def verifyAuditEventsWereForwarded(count: Int): Unit =
    wireMockServer.verify(
      count,
      postRequestedFor(urlPathEqualTo("/write/audit"))
        .withHeader("content-type", equalTo("application/json")))
}
