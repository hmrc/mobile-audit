import sbt._

object AppDependencies {

  private val playBootstrapVersion = "8.5.0"
  private val playHmrcApiVersion   = "8.0.0"
  private val catsCore             = "2.0.0"

  private val pegdownVersion                 = "1.6.0"
  private val wireMockVersion                = "2.21.0"
  private val scalaMockVersion               = "5.2.0"
  private val refinedVersion                  = "0.11.1"
  private val playJsonSchemaValidatorVersion = "0.9.5"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % playBootstrapVersion,
    "uk.gov.hmrc"   %% "play-hmrc-api-play-30"     % playHmrcApiVersion,
    "org.typelevel" %% "cats-core"                 % catsCore,
    "eu.timepit"    %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % playBootstrapVersion % scope,
    "org.pegdown" % "pegdown"                 % pegdownVersion       % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test = testCommon(scope) ++ Seq(
            "org.scalamock"     %% "scalamock"                  % scalaMockVersion               % scope,
            "com.eclipsesource" %% "play-json-schema-validator" % playJsonSchemaValidatorVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope = "it"

        override lazy val test = testCommon(scope) ++ Seq.empty
      }.test

  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
