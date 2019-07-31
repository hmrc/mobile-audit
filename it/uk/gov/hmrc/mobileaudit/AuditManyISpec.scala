package uk.gov.hmrc.mobileaudit

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.DateTime
import org.scalatest.OptionValues
import play.api.libs.json._
import uk.gov.hmrc.mobileaudit.controllers.{IncomingAuditEvent, IncomingAuditEvents}
import uk.gov.hmrc.mobileaudit.stubs.{AuditStub, AuthStub}
import uk.gov.hmrc.mobileaudit.utils.BaseISpec
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.collection.JavaConverters._

class AuditManyISpec extends BaseISpec with OptionValues {
  implicit val jodaDateReads: Reads[DateTime]  = play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
  implicit val readDataEvent: Reads[DataEvent] = Json.reads

  val authNino = "AA100000Z"
  val maliciousNIno = "OTHERNINO"

  "when multiple events are sent to /audit-events" - {
    "they should all be forwarded to the audit service" in {

      val detail = Map("nino" -> authNino)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList
      val auditSource = app.configuration.underlying.getString("auditSource")

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
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
        dataEvent.auditSource shouldBe auditSource
        dataEvent.detail.get("nino").value shouldBe authNino
      }

      // Cross-check that each of the unique audit-type values from the incoming events are present
      // in the data events send to the audit service
      incomingEvents.indices.foreach { i =>
        val expectedAuditType = s"$auditType-$i"
        dataEvents.find(_.auditType == expectedAuditType) shouldBe a[Some[_]]
      }
    }


    "it should fail if the nino in the audit body does not match that of the bearer token" in {

      val detail = Map("nino" -> maliciousNIno)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
      response.status shouldBe 401
      response.body shouldBe "Authorization failure [failed to validate Nino]"

      verifyAuditEventWasNotForwarded()
    }


    "it should fail if the detail section does not have a nino in the detail body" in {
      val detail = Map("otherKey" -> maliciousNIno)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
      response.status shouldBe 400
      response.body shouldBe "Invalid details payload"
    }

    "it should fail if the journeyId is not supplied as a query parameter" in {
      val detail = Map("nino" -> authNino)
      val auditType = "audit-type"

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/audit-events").post(Json.toJson(IncomingAuditEvents(incomingEvents))))
      response.status shouldBe 400
      response.body shouldBe "{\"statusCode\":400,\"message\":\"bad request\"}"
    }
  }

  private def verifyAuditEventsWereForwarded(count: Int): Unit =
    wireMockServer.verify(
      count,
      postRequestedFor(urlPathEqualTo("/write/audit"))
        .withHeader("content-type", equalTo("application/json")))

  private def verifyAuditEventWasNotForwarded(): Unit =
    wireMockServer.verify(
      0,
      postRequestedFor(urlPathEqualTo("/write/audit"))
        .withHeader("content-type", equalTo("application/json")))
}
