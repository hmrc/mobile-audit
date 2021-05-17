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

import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.DateTime
import org.scalatest.OptionValues
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.mobileaudit.controllers.{IncomingAuditEvent, IncomingAuditEvents, LiveAuditController}
import uk.gov.hmrc.mobileaudit.stubs.{AuditStub, AuthStub}
import uk.gov.hmrc.mobileaudit.utils.BaseISpec
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.collection.JavaConverters._

class AuditManyISpec extends BaseISpec with OptionValues {
  implicit val jodaDateReads: Reads[DateTime]  = play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
  implicit val readDataEvent: Reads[DataEvent] = Json.reads

  val authNino      = "AA100000Z"
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

      val dataEvents: List[JsValue] = getAllServeEvents.asScala
        .filter(_.getRequest.getUrl == "/write/audit")
        .map(e => Json.parse(e.getRequest.getBodyAsString.replaceAll("\\\\", "")))
        .toList

      // no more, no less
      dataEvents.length shouldBe incomingEvents.length

      // confirm that all the data events contain the values we expect
      dataEvents.foreach { dataEvent =>
        (dataEvent \ "auditSource").as[String]     shouldBe auditSource
        (dataEvent \ "detail" \ "nino").as[String] shouldBe authNino
      }

      // Cross-check that each of the unique audit-type values from the incoming events are present
      // in the data events send to the audit service
      incomingEvents.indices.foreach { i =>
        val expectedAuditType = s"$auditType-$i"
        dataEvents.toString().contains(expectedAuditType) shouldBe true
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

      withCaptureOfLoggingFrom(Logger(classOf[LiveAuditController])) { logs =>
        val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
        response.status shouldBe 401
        response.body   shouldBe "Invalid credentials"
        assert(
          logs
            .filter(_.getLevel == Level.WARN)
            .head
            .getMessage
            .startsWith("Authorization failure [failed to validate Nino]")
        )
      }
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
      response.body   shouldBe "Invalid details payload"
    }

    "it should fail if the list of events is empty" in {

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl(auditEventsUrl).post(Json.parse("""{"events": []}""")))
      response.status shouldBe 400
      response.body   shouldBe "Invalid details payload"
    }

    "it should fail if the journeyId is not supplied as a query parameter" in {
      val detail    = Map("nino" -> authNino)
      val auditType = "audit-type"

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/audit-events").post(Json.toJson(IncomingAuditEvents(incomingEvents))))
      response.status shouldBe 400
      response.body   shouldBe "{\"statusCode\":400,\"message\":\"bad request\"}"
    }

    "it should return 400 with an invalid journeyId" in {
      val testNino = "AA100000Z"
      val detail   = Map("nino" -> testNino)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsLoggedIn(testNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(
        wsUrl("/audit-events?journeyId=ThisIsAnInvalidJourneyId").post(Json.toJson(IncomingAuditEvents(incomingEvents)))
      )
      response.status shouldBe 400
    }

    "it should fail if the user is not logged in" in {

      val detail = Map("nino" -> authNino)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsNotLoggedIn()
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      withCaptureOfLoggingFrom(Logger(classOf[LiveAuditController])) { logs =>
        val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
        response.status shouldBe 401
        response.body   shouldBe "Invalid credentials"
        assert(
          logs
            .filter(_.getLevel == Level.WARN)
            .head
            .getMessage
            .startsWith("Authorisation failure [Bearer token not supplied]")
        )
      }
      verifyAuditEventWasNotForwarded()
    }

    "it should fail if the user have insufficient confidence level" in {

      val detail = Map("nino" -> authNino)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userIsLoggedInWithInsufficientConfidenceLevel()
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      withCaptureOfLoggingFrom(Logger(classOf[LiveAuditController])) { logs =>
        val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
        response.status shouldBe 403
        response.body   shouldBe "Invalid credentials"

        assert(
          logs
            .filter(_.getLevel == Level.WARN)
            .head
            .getMessage
            .startsWith("Authorisation failure [Insufficient ConfidenceLevel]")
        )
      }
      verifyAuditEventWasNotForwarded()
    }

    "it should fail if there is an unknow error in Auth" in {

      val detail = Map("nino" -> authNino)

      val incomingEvents = (0 to 3).map { i =>
        IncomingAuditEvent(s"$auditType-$i", None, None, None, detail)
      }.toList

      AuthStub.userLogInThrowsUnknownError()
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      withCaptureOfLoggingFrom(Logger(classOf[LiveAuditController])) { logs =>
        val response = await(wsUrl(auditEventsUrl).post(Json.toJson(IncomingAuditEvents(incomingEvents))))
        response.status shouldBe 500
        response.body   shouldBe "Error occurred creating audit event"
        assert(
          logs
            .filter(_.getLevel == Level.WARN)
            .head
            .getMessage
            .startsWith("POST of 'http://localhost:")
        )
        assert(
          logs
            .filter(_.getLevel == Level.WARN)
            .head
            .getMessage
            .endsWith("auth/authorise' returned 500. Response body: ''")
        )
      }

      verifyAuditEventWasNotForwarded()
    }
  }

  private def verifyAuditEventsWereForwarded(count: Int): Unit =
    wireMockServer.verify(count,
                          postRequestedFor(urlPathEqualTo("/write/audit"))
                            .withHeader("content-type", equalTo("application/json")))

  private def verifyAuditEventWasNotForwarded(): Unit =
    wireMockServer.verify(0,
                          postRequestedFor(urlPathEqualTo("/write/audit"))
                            .withHeader("content-type", equalTo("application/json")))
}
