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

package uk.gov.hmrc.nisp.builders

import javax.inject.Inject
import play.api.{Configuration, Play}
import uk.gov.hmrc.nisp.config.ApplicationConfig

class ApplicationConfigBuilder @Inject()(configuration: Configuration) {
  def apply(assetsPrefix: String = "", betaFeedbackUrl: String = "", betaFeedbackUnauthenticatedUrl: String = "",
            analyticsToken: Option[String] = None, analyticsHost: String = "", ssoUrl: Option[String] = None,
            contactFormServiceIdentifier: String = "", contactFrontendPartialBaseUrl: String = "",
            reportAProblemPartialUrl: String = "", reportAProblemNonJSUrl: String = "", showGovUkDonePage: Boolean = false,
            govUkFinishedPageUrl: String = "", identityVerification: Boolean = false, postSignInRedirectUrl: String = "",
            notAuthorisedRedirectUrl: String = "", verifySignIn: String = "", verifySignInContinue: Boolean = false,
            ivUpliftUrl: String = "ivuplift", ggSignInUrl: String = "ggsignin", twoFactorUrl: String = "twofactor",
            pertaxFrontendUrl: String = "", breadcrumbPartialUrl: String = "", showFullNI: Boolean = false,
            futureProofPersonalMax: Boolean = false,
            isWelshEnabled: Boolean = true,
            frontendTemplatePath: String = "",
            feedbackFrontendUrl: String = "/foo"
           ): ApplicationConfig = new ApplicationConfig(configuration) {
    override lazy val assetsPrefix: String = assetsPrefix
    override lazy val betaFeedbackUrl: String = betaFeedbackUrl
    override lazy val betaFeedbackUnauthenticatedUrl: String = betaFeedbackUnauthenticatedUrl
    override lazy val analyticsToken: Option[String] = analyticsToken
    override lazy val analyticsHost: String = analyticsHost
    override lazy val ssoUrl: Option[String] = ssoUrl
    override val contactFormServiceIdentifier: String = contactFormServiceIdentifier
    override lazy val contactFrontendPartialBaseUrl: String = contactFormServiceIdentifier
    override lazy val reportAProblemPartialUrl: String = reportAProblemPartialUrl
    override lazy val reportAProblemNonJSUrl: String = reportAProblemNonJSUrl
    override val showGovUkDonePage: Boolean = showGovUkDonePage
    override val govUkFinishedPageUrl: String = govUkFinishedPageUrl
    override val identityVerification: Boolean = identityVerification
    override lazy val postSignInRedirectUrl: String = postSignInRedirectUrl
    override lazy val notAuthorisedRedirectUrl: String = notAuthorisedRedirectUrl
    override lazy val verifySignIn: String = verifySignIn
    override lazy val verifySignInContinue: Boolean = verifySignInContinue
    override val ivUpliftUrl: String = ivUpliftUrl
    override val ggSignInUrl: String = ggSignInUrl
    override lazy val pertaxFrontendUrl: String = pertaxFrontendUrl
    override lazy val breadcrumbPartialUrl: String = breadcrumbPartialUrl
    override lazy val showFullNI: Boolean = showFullNI
    override lazy val futureProofPersonalMax: Boolean = futureProofPersonalMax
    override val isWelshEnabled: Boolean = isWelshEnabled
    override lazy val frontendTemplatePath: String = frontendTemplatePath
    override val feedbackFrontendUrl: String = "/foo"
  }
}
