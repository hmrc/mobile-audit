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

package uk.gov.hmrc.mobileaudit.config

import org.apache.pekko.actor.ActorSystem
import com.google.inject.Inject
import com.typesafe.config.Config

import javax.inject.Named
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder, getClass}
import uk.gov.hmrc.play.http.ws._

import java.net.URL

trait Hooks extends HttpHooks with HttpAuditing {
  val hooks: Seq[HttpHook] = Seq(AuditingHook)
}
abstract class HttpClientV2Impl @Inject()(http: HttpClientV2,
                                 @Named("appName") val appName: String,
                                 val auditConnector:            AuditConnector,
                                 val actorSystem:      ActorSystem,
                                 config: Configuration)
                                 extends HttpClientV2 with Hooks {
  lazy val configuration: Config = config.underlying


}


