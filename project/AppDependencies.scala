import sbt._

object AppDependencies {

  private val playBootstrapVersion = "10.8.0"
  private val playHmrcApiVersion   = "9.0.0"
  private val catsCore             = "2.13.0"

  private val flexmarkVersion   = "0.64.8"
  private val scalaMockVersion = "7.5.5"
  private val refinedVersion   = "0.11.4"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "play-hmrc-api-play-30"     % playHmrcApiVersion,
    "org.typelevel" %% "cats-core"                 % catsCore,
    "eu.timepit"    %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % playBootstrapVersion % scope,
    "com.vladsch.flexmark" % "flexmark"                % flexmarkVersion % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock" % scalaMockVersion % scope
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
