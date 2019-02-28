package uk.gov.hmrc.mobileaudit

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.integration.ServiceSpec

class GuiceWiringISpec extends WordSpec with Matchers with ServiceSpec {

  def externalServices: Seq[String] = Seq("datastream", "auth")

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  "This integration test" should {
    "successfully start the app with guice wiring" in {
      succeed
    }
  }
}
