package uk.gov.hmrc.mobileaudit.stubs
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object AuditStub {
   def respondToAuditMergedWithNoBody(implicit wireMockServer: WireMockServer): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/write/audit/merged"))
        .willReturn(aResponse()
          .withStatus(204)))

   def respondToAuditWithNoBody(implicit wireMockServer: WireMockServer): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/write/audit"))
        .willReturn(aResponse()
          .withStatus(204)))
}
