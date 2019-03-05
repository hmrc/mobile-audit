/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.mobileaudit.api

import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Millis, Span}
import uk.gov.hmrc.mobileaudit.stubs.ServiceLocatorStub
import uk.gov.hmrc.mobileaudit.utils.BaseISpec

class ServiceLocatorRegistrationISpec extends BaseISpec with Eventually {

  override def config: Map[String, Any] = Map(
    "microservice.services.service-locator.enabled" -> true,
    "microservice.services.service-locator.host"    -> wireMockHost,
    "microservice.services.service-locator.port"    -> wireMockPort
  )

  "microservice" - {
    "should register itself with the api platform automatically at start up" in {
      ServiceLocatorStub.registrationSucceeds()

      ServiceLocatorStub
        .registerShouldNotHaveBeenCalled("mobile-audit", "https://mobile-audit.protected.mdtp")

      eventually(Timeout(Span(1000 * 20, Millis))) {
        ServiceLocatorStub
          .registerShouldHaveBeenCalled("mobile-audit", "https://mobile-audit.protected.mdtp")
      }
    }
  }
}
