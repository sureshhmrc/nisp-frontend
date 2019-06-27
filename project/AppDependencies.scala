import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.9.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.4.0-play-25",
    "uk.gov.hmrc" %% "play-language" % "3.4.0",
    "uk.gov.hmrc" %% "tax-year" % "0.5.0",
    "uk.gov.hmrc" %% "csp-client" % "3.4.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc" %% "local-template-renderer"  % "2.7.0-play-25",
    "uk.gov.hmrc" %% "play-breadcrumb"  % "1.0.0"
  )

  val test = Seq(
      "org.pegdown" % "pegdown" % "1.6.0",
      "org.jsoup" % "jsoup" % "1.10.2",
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1",
      "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25",
      "org.mockito" % "mockito-core" % "2.6.3"
  ).map(_ % "test")

  
  val all: Seq[ModuleID] = compile ++ test
}

