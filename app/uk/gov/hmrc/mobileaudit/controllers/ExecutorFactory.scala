/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.mobileaudit.controllers

import java.time.{LocalDateTime, ZoneOffset}

import play.api.libs.json.Json.parse
import play.api.libs.json._
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}

case class ExecutorResponse(name: String, responseData: Option[JsValue] = None, failure: Option[Boolean] = None, timeout: Option[Boolean] = None)

@deprecated("This is the old code, to be deleted when we're happy that the new code has replicated it correctly", "Feb 2019")
case class AuditEventExecutor(auditConnector: AuditConnector, logger: LoggerLike = Logger) {
  val executorName: String = "ngc-audit-event"

  private case class ValidData(auditType: String, extraDetail: Map[String, String], tags: Map[String, String])

  def execute(data: Option[JsValue], nino: String, journeyId: Option[String])(
    implicit hc:    HeaderCarrier,
    ex:             ExecutionContext
  ): Future[Option[ExecutorResponse]] = {
    val response =
      data
        .flatMap(validate)
        .map { validData =>
          val transactionName: String = validData.tags.getOrElse("transactionName", "explicitAuditEvent")
          val path:            String = validData.tags.getOrElse("path", validData.auditType)

          val defaultEvent = DataEvent(
            "native-apps",
            validData.auditType,
            tags   = hc.toAuditTags(transactionName, path),
            detail = hc.toAuditDetails(validData.extraDetail.toSeq: _*)
          )

          val event: DataEvent =
            readGeneratedAt(data, defaultEvent.eventId)
              .map(generatedAt => defaultEvent.copy(generatedAt = new org.joda.time.DateTime(generatedAt.toInstant(ZoneOffset.UTC).toEpochMilli)))
              .getOrElse(defaultEvent)

          auditConnector.sendEvent(event)
          ExecutorResponse(executorName, failure = Some(false))
        }
        .getOrElse(
          ExecutorResponse(executorName, responseData = Some(parse("""{"error": "Bad Request"}""")), failure = Some(true))
        )

    Future.successful(Some(response))
  }

  private def validate(data: JsValue): Option[ValidData] = {
    val maybeAuditType:            Option[String]              = (data \ "auditType").asOpt[String]
    val maybeNewFormatExtraDetail: Option[Map[String, String]] = (data \ "detail").asOpt[Map[String, String]]
    val maybeOldFormatExtraDetail: Option[Map[String, String]] = (data \ "nino").asOpt[String].map(nino => Map("nino" -> nino))
    val maybeExtraDetail:          Option[Map[String, String]] = maybeNewFormatExtraDetail.orElse(maybeOldFormatExtraDetail)
    val tags:                      Map[String, String]         = (data \ "tags").asOpt[Map[String, String]].getOrElse(Map.empty)

    for {
      auditType   <- maybeAuditType
      extraDetail <- maybeExtraDetail
    } yield ValidData(auditType, extraDetail, tags)
  }

  private def readGeneratedAt(data: Option[JsValue], eventId: String): Option[LocalDateTime] =
    data
      .flatMap(json => (json \ "generatedAt").asOpt[JsValue])
      .flatMap { generatedAtJsValue: JsValue =>
        generatedAtJsValue.validate[LocalDateTime] match {
          case JsSuccess(dateTime, _) => Some(dateTime)
          case _: JsError =>
            logger.warn(s"""Couldn't parse generatedAt timestamp $generatedAtJsValue, defaulting to now for audit event $eventId""")
            None
        }
      }
}
