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

package uk.gov.hmrc.mobileaudit.utils
import org.scalatest.{FreeSpecLike, Matchers}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import ch.qos.logback.core.read.ListAppender
import play.api.LoggerLike

import scala.collection.JavaConverters._

trait BaseISpec
    extends FreeSpecLike
    with Matchers
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout
    with LogCapturing {
  override implicit lazy val app: Application = appBuilder.build()

  val auditType      = "audit-type"
  val auditEventUrl  = "/audit-event?journeyId=d9737a82-813b-4b8b-a9f3-0172c96a24e4"
  val auditEventsUrl = "/audit-events?journeyId=d9737a82-813b-4b8b-a9f3-0172c96a24e4"

  def config: Map[String, Any] = Map(
    "appName"                         -> "mobile-audit",
    "auditing.enabled"                -> true,
    "auditing.consumer.baseUri.port"  -> wireMockPort,
    "microservice.services.auth.port" -> wireMockPort,
    "metrics.enabled"                 -> false
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}

trait LogCapturing {

  def withCaptureOfLoggingFrom(logger: LogbackLogger)(body: (=> List[ILoggingEvent]) => Unit) {
    val appender = new ListAppender[ILoggingEvent]()
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.ALL)
    logger.setAdditive(true)
    body(appender.list.asScala.toList)
  }

  def withCaptureOfLoggingFrom(logger: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit) {
    withCaptureOfLoggingFrom(logger.logger.asInstanceOf[LogbackLogger])(body)
  }
}
