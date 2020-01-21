/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Application
import uk.gov.hmrc.mobileaudit.utils.BaseISpec

class ApiDefinitionISpec extends BaseISpec {

  override def config: Map[String, Any] = super.config ++ Map(
    "api.access.white-list.applicationIds" -> Seq("00010002-0003-0004-0005-000600070008",
                                                  "00090002-0003-0004-0005-000600070008")
  )

  override implicit lazy val app: Application = appBuilder.build()

  "GET /api/definition" - {

    "should provide definition with configurable whitelist" in {
      val response = await(wsUrl("/api/definition").get())
      response.status shouldBe 200

      response.header("Content-Type") shouldBe Some("application/json")

      val definition = response.json
      (definition \\ "version").map(_.as[String]).head shouldBe "1.0"

      val accessConfigs = definition \ "api" \ "versions" \\ "access"
      accessConfigs.length should be > 0
      accessConfigs.foreach { accessConfig =>
        (accessConfig \ "type").as[String]                           shouldBe "PRIVATE"
        (accessConfig \ "whitelistedApplicationIds").head.as[String] shouldBe "00010002-0003-0004-0005-000600070008"
        (accessConfig \ "whitelistedApplicationIds")(1).as[String]   shouldBe "00090002-0003-0004-0005-000600070008"
      }
    }
  }

  "GET /api/conf/1.0/application.raml" - {
    "should return RAML" in {
      val response = await(wsUrl("/api/conf/1.0/application.raml").get())
      response.status shouldBe 200
    }
  }
}
