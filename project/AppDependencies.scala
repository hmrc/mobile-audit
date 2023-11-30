import play.core.PlayVersion
import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  private val play28Bootstrap    = "7.19.0"
  private val playHmrcApiVersion = "7.2.0-play-28"
  private val catsCore           = "2.0.0"

  private val pegdownVersion                 = "1.6.0"
  private val wireMockVersion                = "2.21.0"
  private val scalaMockVersion               = "5.1.0"
  private val scalaTestVersion               = "3.2.9"
  private val playJsonJodaVersion            = "2.7.4"
  private val scalaCheckVersion              = "1.17.0"
  private val refinedVersion                 = "0.9.26"
  private val playJsonSchemaValidatorVersion = "0.9.5"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-28" % play28Bootstrap,
    "uk.gov.hmrc"   %% "play-hmrc-api"             % playHmrcApiVersion,
    "org.typelevel" %% "cats-core"                 % catsCore,
    "eu.timepit"    %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-28" % play28Bootstrap     % scope,
    "com.typesafe.play" %% "play-test"              % PlayVersion.current % scope,
    "org.scalatest"     %% "scalatest"              % scalaTestVersion    % scope,
    "org.pegdown"       % "pegdown"                 % pegdownVersion      % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test = testCommon(scope) ++ Seq(
            "org.scalamock"     %% "scalamock"                  % scalaMockVersion               % scope,
            "org.scalacheck"    %% "scalacheck"                 % scalaCheckVersion              % scope,
            "com.typesafe.play" %% "play-json-joda"             % playJsonJodaVersion            % scope,
            "com.eclipsesource" %% "play-json-schema-validator" % playJsonSchemaValidatorVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope = "it"

        override lazy val test = testCommon(scope) ++ Seq(
            "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
          )
      }.test

  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
