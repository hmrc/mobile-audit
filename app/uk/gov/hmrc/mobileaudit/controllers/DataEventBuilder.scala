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
import org.joda.time.DateTime
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent

object DataEventBuilder {
  val transactionNameKey     = "transactionName"
  val ninoKey                = "nino"
  val pathKey                = "path"
  val defaultTransactionName = "explicitAuditEvent"

  def buildEvent(auditSource:String, nino: String, incomingEvent: IncomingEventData, hc: HeaderCarrier): DataEvent = {
    val tags        = incomingEvent.tags.getOrElse(Map())
    val generatedAt = incomingEvent.generatedAt.map(d => new DateTime(d.toInstant.toEpochMilli)).getOrElse(DateTime.now())
    val transactionName: String = tags.getOrElse(transactionNameKey, defaultTransactionName)
    val path:            String = tags.getOrElse(pathKey, incomingEvent.auditType)

    DataEvent(
      auditSource,
      incomingEvent.auditType,
      tags        = hc.toAuditTags(transactionName, path),
      detail      = incomingEvent.detail ++ Map(ninoKey -> nino),
      generatedAt = generatedAt
    )
  }
}
