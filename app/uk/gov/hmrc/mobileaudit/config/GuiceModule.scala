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
import com.google.inject.AbstractModule
import com.google.inject.name.Names.named
import javax.inject.Inject
import play.api.{Configuration, Environment}
import uk.gov.hmrc.mobileaudit.controllers.api.ApiAccess

class GuiceModule @Inject() (environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bindConfigString("auditSource", "auditSource")

    bind(classOf[ApiAccess]).toInstance(ApiAccess("PRIVATE"))
  }

  private def bindConfigString(
    name: String,
    path: String
  ): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getString(path))
}
