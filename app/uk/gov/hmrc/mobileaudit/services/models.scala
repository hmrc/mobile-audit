package uk.gov.hmrc.mobileaudit.services

import java.time.ZonedDateTime

import play.api.libs.json.{Json, OFormat}

case class IncomingEventData(
  auditType:   String,
  generatedAt: Option[ZonedDateTime],
  detail:      Map[String, String],
  tags:        Option[Map[String, String]]
)

object IncomingEventData {
  implicit val formats: OFormat[IncomingEventData] = Json.format
}

case class IncomingEvent(name: String, data: IncomingEventData)

object IncomingEvent {
  implicit val formats: OFormat[IncomingEvent] = Json.format
}
