/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.mobileaudit.schemas

import com.eclipsesource.schema.SchemaType
import org.scalatest.{FreeSpecLike, Matchers}
import play.api.libs.json.Json
import play.api.libs.json.Json._
import uk.gov.hmrc.mobileaudit.controllers.IncomingAuditEvent
import uk.gov.hmrc.mobileaudit.schemas.Schema._
import com.eclipsesource.schema.drafts.Version7._

class EventJsonSpec extends FreeSpecLike with Matchers with SchemaMatchers {

  private val strictRamlEventSchema =
    banAdditionalProperties(JsonResource.loadResourceJson("/public/api/conf/1.0/schemas/event.json"))
      .as[SchemaType]

  private val strictRamlEventsSchema =
    banAdditionalProperties(JsonResource.loadResourceJson("/public/api/conf/1.0/schemas/events.json"))
      .as[SchemaType]

  "an event converted to json" - {
    "should validate against the event schema" in {
      val event = IncomingAuditEvent("auditType", None, None, None, Map())

      Json.toJson(event) should validateAgainstSchema(strictRamlEventSchema)
    }
  }

  "a list of events converted to json" - {
    "should validate against the events schema" in {
      val event = IncomingAuditEvent("auditType", None, None, None, Map())

      val json = Json.toJson(obj("events" -> List(event, event)))
      json should validateAgainstSchema(strictRamlEventsSchema)
    }
  }
}
