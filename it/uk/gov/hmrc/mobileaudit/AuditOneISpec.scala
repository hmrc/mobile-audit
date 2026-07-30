/*
 * Copyright 2023 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.scalatest.OptionValues
import play.api.Logger
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.mobileaudit.controllers.{IncomingAuditEvent, LiveAuditController}
import uk.gov.hmrc.mobileaudit.stubs.{AuditStub, AuthStub}
import uk.gov.hmrc.mobileaudit.utils.BaseISpec

import java.time.{Clock, Instant, ZoneOffset, ZonedDateTime}
import scala.jdk.CollectionConverters.*

/**
  * The unit tests check that various forms of `IncomingEvent` are correctly translated to the
  * equivalent `DataEvent`, so just do some basic sanity checks that the event that is sent matches
  * the event we generated
  */
class AuditOneISpec extends BaseISpec with OptionValues {

  val clock = Clock.fixed(Instant.parse("2026-07-30T12:00:00.000Z"), ZoneOffset.UTC)
  val authorisationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer 123"

  "when a single event sent to /audit-event" - {

    "it should be forwarded to the audit service" in {
      val auditSource = app.configuration.underlying.getString("auditSource")
      val testNino = "AA100000Z"
      val detail = Map("nino" -> testNino)
      val generatedAt = ZonedDateTime.now(clock)

      val incomingEvent = IncomingAuditEvent(auditType, Some(generatedAt), None, None, detail)

      AuthStub.userIsLoggedIn(testNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response =
        await(wsUrl(auditEventUrl).addHttpHeaders(authorisationJsonHeader) post (Json.toJson(incomingEvent)))
      response.status shouldBe 204

      verifyAuditEventWasForwarded()

      val auditRequest: ServeEvent = getAllServeEvents.asScala.find(_.getRequest.getUrl == "/write/audit").value

      val dataEvent = Json.parse(auditRequest.getRequest.getBodyAsString.replaceAll("\\\\", ""))

      (dataEvent \ "auditSource").as[String] shouldBe auditSource
      (dataEvent \ "auditType").as[String] shouldBe incomingEvent.auditType
      (dataEvent \ "generatedAt").as[ZonedDateTime] shouldBe generatedAt
      (dataEvent \ "detail" \ "nino").as[String] shouldBe testNino
    }

    "it should fail if the nino in the audit body does not match that of the bearer token" in {
      val authNino      = "AA100000Z"
      val maliciousNIno = "OTHERNINO"
      val detail        = Map("nino" -> maliciousNIno)

      val incomingEvent = IncomingAuditEvent(auditType, None, None, None, detail)

      AuthStub.userIsLoggedIn(authNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      withCaptureOfLoggingFrom(Logger(classOf[LiveAuditController])) { logs =>
        val response =
          await(wsUrl(auditEventUrl).addHttpHeaders(authorisationJsonHeader) post (Json.toJson(incomingEvent)))
        response.status shouldBe 401
        response.body.toString   shouldBe "Invalid credentials"
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
      val nino   = "AA100000X"
      val detail = Map("otherKey" -> nino)

      val incomingEvent = IncomingAuditEvent(auditType, None, None, None, detail)

      AuthStub.userIsLoggedIn(nino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl(auditEventUrl).post(Json.toJson(incomingEvent)))
      response.status shouldBe 400
      response.body.toString  shouldBe "Invalid details payload"

      verifyAuditEventWasNotForwarded()
    }

    "it should fail if the journeyId is not supplied as a query parameter" in {
      val nino   = "AA100000X"
      val detail = Map("otherKey" -> nino)

      val incomingEvent = IncomingAuditEvent(s"$auditType-1", None, None, None, detail)

      AuthStub.userIsLoggedIn(nino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/audit-event").post(Json.toJson(incomingEvent)))
      response.status shouldBe 400
      response.body.toString  shouldBe "{\"statusCode\":400,\"message\":\"Missing parameter: journeyId\"}"

      verifyAuditEventWasNotForwarded()
    }

    "it should fail if there is an unknow error in Auth" in {
      val nino   = "AA100000X"
      val detail = Map("nino" -> nino)

      val incomingEvent = IncomingAuditEvent(s"$auditType-1", None, None, None, detail)

      AuthStub.userLogInThrowsUnknownError()
      AuditStub.respondToAuditMergedWithNoBody

      val response =
        await(wsUrl(auditEventUrl).addHttpHeaders(authorisationJsonHeader) post (Json.toJson(incomingEvent)))
      response.status shouldBe 500
      response.body.toString shouldBe "Error occurred creating audit event"

      verifyAuditEventWasNotForwarded()
    }

    "it should return 400 without a journeyId" in {
      val testNino = "AA100000Z"
      val detail   = Map("nino" -> testNino)

      val incomingEvent = IncomingAuditEvent(auditType, None, None, None, detail)

      AuthStub.userIsLoggedIn(testNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/audit-event").post(Json.toJson(incomingEvent)))
      response.status shouldBe 400

      verifyAuditEventWasNotForwarded()
    }

    "it should return 400 with an invalid journeyId" in {
      val testNino = "AA100000Z"
      val detail   = Map("nino" -> testNino)

      val incomingEvent = IncomingAuditEvent(auditType, None, None, None, detail)

      AuthStub.userIsLoggedIn(testNino)
      AuditStub.respondToAuditWithNoBody
      AuditStub.respondToAuditMergedWithNoBody

      val response = await(wsUrl("/audit-event?journeyId=ThisIsAnInvalidJourneyId").post(Json.toJson(incomingEvent)))
      response.status shouldBe 400

      verifyAuditEventWasNotForwarded()
    }
  }

  private def verifyAuditEventWasForwarded(): Unit =
    wireMockServer.verify(1,
                          postRequestedFor(urlPathEqualTo("/write/audit"))
                            .withHeader("content-type", equalTo("application/json"))
                            .withRequestBody(matchingJsonPath(s"$$.[?(@.auditSource == '${app.configuration.underlying.getString("auditSource")}')]"))
    )

  private def verifyAuditEventWasNotForwarded(): Unit =
    wireMockServer.verify(0,
                          postRequestedFor(urlPathEqualTo("/write/audit"))
                          .withHeader("content-type", equalTo("application/json"))
                          .withRequestBody(matchingJsonPath(s"$$.[?(@.auditSource == '${app.configuration.underlying.getString("auditSource")}')]"))
    )

}
