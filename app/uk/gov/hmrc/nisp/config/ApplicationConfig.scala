/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nisp.config

import javax.inject.Inject
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.Play._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.config.ServicesConfig


class ApplicationConfig @Inject()(configuration: Configuration) extends ServicesConfig {

  protected def mode: Mode = Play.current.mode
  protected def runModeConfiguration: Configuration = configuration

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = configuration.getString(s"contact-frontend.host").getOrElse("")

  lazy val assetsPrefix: String = loadConfig(s"assets.url") + loadConfig(s"assets.version") + "/"
  lazy val betaFeedbackUrl = s"${Constants.baseUrl}/feedback"
  lazy val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  lazy val analyticsToken: Option[String] = configuration.getString(s"google-analytics.token")
  lazy val analyticsHost: String = configuration.getString(s"google-analytics.host").getOrElse("auto")
  lazy val ssoUrl: Option[String] = configuration.getString(s"portal.ssoUrl")
  lazy val frontendTemplatePath: String = configuration.getString("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")

  val contactFormServiceIdentifier = "NISP"
  lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  val showGovUkDonePage: Boolean = configuration.getBoolean("govuk-done-page.enabled").getOrElse(true)
  val govUkFinishedPageUrl: String = loadConfig("govuk-done-page.url")
  val identityVerification: Boolean = configuration.getBoolean("microservice.services.features.identityVerification").getOrElse(false)

  lazy val verifySignIn: String = configuration.getString("verify-sign-in.url").getOrElse("")
  lazy val verifySignInContinue: Boolean = configuration.getBoolean("verify-sign-in.submit-continue-url").getOrElse(false)
  lazy val postSignInRedirectUrl = configuration.getString("login-callback.url").getOrElse("")
  lazy val notAuthorisedRedirectUrl = configuration.getString("not-authorised-callback.url").getOrElse("")
  val ivUpliftUrl: String = configuration.getString(s"identity-verification-uplift.host").getOrElse("")
  val ggSignInUrl: String = configuration.getString(s"government-gateway-sign-in.host").getOrElse("")

  val showUrBanner:Boolean = configuration.getBoolean("urBannerToggle").getOrElse(false)
  val GaEventAction: String = "home page UR"

  private val pertaxFrontendService: String = baseUrl("pertax-frontend")
  lazy val pertaxFrontendUrl: String = configuration.getString(s"breadcrumb-service.url").getOrElse("")
  lazy val breadcrumbPartialUrl: String = s"$pertaxFrontendService/personal-account/integration/main-content-header"
  lazy val showFullNI: Boolean = configuration.getBoolean("microservice.services.features.fullNIrecord").getOrElse(false)
  lazy val futureProofPersonalMax: Boolean = configuration.getBoolean("microservice.services.features.future-proof.personalMax").getOrElse(false)
  val isWelshEnabled = configuration.getBoolean("microservice.services.features.welsh-translation").getOrElse(false)
  val feedbackFrontendUrl: String = loadConfig("feedback-frontend.url")
}

object ApplicationConfig extends ApplicationConfig(Play.current.configuration)