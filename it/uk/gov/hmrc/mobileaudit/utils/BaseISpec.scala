package uk.gov.hmrc.mobileaudit.utils
import org.scalatest.{FreeSpecLike, Matchers}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

trait BaseISpec
    extends FreeSpecLike
    with Matchers
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout {
  override implicit lazy val app: Application = appBuilder.build()

  val auditType      = "audit-type"
  val auditEventUrl  = "/audit-event?journeyId=d9737a82-813b-4b8b-a9f3-0172c96a24e4"
  val auditEventsUrl = "/audit-events?journeyId=d9737a82-813b-4b8b-a9f3-0172c96a24e4"

  def config: Map[String, Any] = Map(
    "appName"                         -> "mobile-audit",
    "auditing.enabled"                -> true,
    "auditing.consumer.baseUri.port"  -> wireMockPort,
    "microservice.services.auth.port" -> wireMockPort
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
