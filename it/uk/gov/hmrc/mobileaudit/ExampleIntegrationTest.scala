package uk.gov.hmrc.mobileaudit

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.integration.ServiceSpec

class ExampleIntegrationTest extends WordSpec with Matchers with ServiceSpec {

  def externalServices: Seq[String] = Seq("datastream", "auth")

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  "This integration test" should {
    "start services via smserver" in {

      val wsClient = app.injector.instanceOf[WSClient]

      val response = wsClient.url(resource("/mobile-audit/")).post(obj()).futureValue
      response.status shouldBe 204

    }
  }
}
