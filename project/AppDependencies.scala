import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-play-26" % "0.41.0",
    "uk.gov.hmrc"   %% "play-hmrc-api"     % "3.6.0-play-26",
    "org.typelevel" %% "cats-core"         % "1.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalamock"     %% "scalamock"                  % "4.1.0"  % "test",
    "org.scalatest"     %% "scalatest"                  % "3.0.5"  % "test",
    "com.typesafe.play" %% "play-test"                  % current  % "test",
    "org.pegdown"       % "pegdown"                     % "1.6.0"  % "test, it",
    "org.scalacheck"    %% "scalacheck"                 % "1.14.0" % "test, it",
    "com.typesafe.play" %% "play-json-joda"             % "2.6.13" % "test, it",
    "com.eclipsesource" %% "play-json-schema-validator" % "0.9.4"  % "test"
  )

  val it: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"                % "3.0.5"         % "it",
    "com.typesafe.play"      %% "play-test"                % current         % "it",
    "org.pegdown"            % "pegdown"                   % "1.6.0"         % "it",
    "uk.gov.hmrc"            %% "service-integration-test" % "0.4.0-play-26" % "it",
    "com.github.tomakehurst" % "wiremock"                  % "2.21.0"        % "it",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.2"         % "it"
  )
}
