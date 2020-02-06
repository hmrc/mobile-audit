import play.sbt.PlayImport.PlayKeys.playDefaultPort
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "mobile-audit"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.8",
    playDefaultPort := 8252,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test ++ AppDependencies.it,
    dependencyOverrides ++= overrides
  )
  .settings(publishingSettings: _*)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.mobileaudit.domain.types._",
      "uk.gov.hmrc.mobileaudit.domain.types.ModelTypes._"
    )
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo, resolvers += Resolver.bintrayRepo("emueller", "maven"))
  .settings( // based on https://tpolecat.github.io/2017/04/25/scalac-flags.html but cut down for scala 2.11
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      //"-Ywarn-unused-import", - does not work well with fatal-warnings because of play-generated sources
      //"-Xfatal-warnings",
      "-Xlint"
    ),
    coverageMinimum := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;.*BuildInfo.*;.*Routes.*;.*javascript.*;.*Reverse.*"
  )

// Transitive dependencies in scalatest/scalatestplusplay drag in a newer version of jetty that is not
// compatible with wiremock, so we need to pin the jetty stuff to the older version.
// see https://groups.google.com/forum/#!topic/play-framework/HAIM1ukUCnI
val jettyVersion = "9.2.13.v20150730"

val overrides: Seq[ModuleID] = Seq(
  "org.eclipse.jetty"           % "jetty-server"       % jettyVersion,
  "org.eclipse.jetty"           % "jetty-servlet"      % jettyVersion,
  "org.eclipse.jetty"           % "jetty-security"     % jettyVersion,
  "org.eclipse.jetty"           % "jetty-servlets"     % jettyVersion,
  "org.eclipse.jetty"           % "jetty-continuation" % jettyVersion,
  "org.eclipse.jetty"           % "jetty-webapp"       % jettyVersion,
  "org.eclipse.jetty"           % "jetty-xml"          % jettyVersion,
  "org.eclipse.jetty"           % "jetty-client"       % jettyVersion,
  "org.eclipse.jetty"           % "jetty-http"         % jettyVersion,
  "org.eclipse.jetty"           % "jetty-io"           % jettyVersion,
  "org.eclipse.jetty"           % "jetty-util"         % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-api"      % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-common"   % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-client"   % jettyVersion
)
