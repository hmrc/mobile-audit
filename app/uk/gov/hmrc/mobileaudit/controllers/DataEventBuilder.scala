/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent

object DataEventBuilder {
  val ninoKey                = "nino"
  val defaultTransactionName = "explicitAuditEvent"

  def buildEvent(
    auditSource:   String,
    nino:          String,
    incomingEvent: IncomingAuditEvent,
    hc:            HeaderCarrier
  ): DataEvent = {
    val transactionName: String = incomingEvent.transactionName.getOrElse(defaultTransactionName)
    val path:            String = incomingEvent.path.getOrElse(incomingEvent.auditType)
    val detail = incomingEvent.detail

    DataEvent(
      auditSource,
      incomingEvent.auditType,
      // The `toAuditTags` adds a bunch of standard values from the header carrier
      tags = hc.toAuditTags(transactionName, path),
      // At the time of writing, `toAuditDetails` does nothing other than rebuild this list of key/value pairs
      // back into a Map[String, String], but I guess it's possible that sometime in the future it might change
      // to add some standard details, which is why I'm pushing our values through it
      detail = hc.toAuditDetails((detail ++ Map(ninoKey -> nino)).toList: _*)
    )
  }
}
