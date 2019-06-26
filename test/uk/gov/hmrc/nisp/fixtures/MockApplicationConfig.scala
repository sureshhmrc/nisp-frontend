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

package uk.gov.hmrc.nisp.fixtures

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.nisp.config.ApplicationConfig
import org.mockito.Mockito.when

object MockApplicationConfig extends MockitoSugar {

      val appConfig: ApplicationConfig = mock[ApplicationConfig]

      when(appConfig.ggSignInUrl).thenReturn("")
      when(appConfig.verifySignIn).thenReturn("")
      when(appConfig.verifySignInContinue).thenReturn(false)
      when(appConfig.reportAProblemNonJSUrl).thenReturn("")
      when(appConfig.ssoUrl).thenReturn(None)
      when(appConfig.identityVerification).thenReturn(false)
      when(appConfig.betaFeedbackUnauthenticatedUrl).thenReturn("")
      when(appConfig.notAuthorisedRedirectUrl).thenReturn("")
      when(appConfig.contactFrontendPartialBaseUrl).thenReturn("")
      when(appConfig.govUkFinishedPageUrl).thenReturn("")
      when(appConfig.showGovUkDonePage).thenReturn(false)
      when(appConfig.analyticsHost).thenReturn("")
      when(appConfig.betaFeedbackUrl).thenReturn("")
      when(appConfig.analyticsToken).thenReturn(None)
      when(appConfig.reportAProblemPartialUrl).thenReturn("")
      when(appConfig.contactFormServiceIdentifier).thenReturn("NISP")
      when(appConfig.postSignInRedirectUrl).thenReturn("")
      when(appConfig.ivUpliftUrl).thenReturn("")
      when(appConfig.pertaxFrontendUrl).thenReturn("")
      when(appConfig.breadcrumbPartialUrl).thenReturn("")
      when(appConfig.showFullNI).thenReturn(false)
      when(appConfig.futureProofPersonalMax).thenReturn(false)
      when(appConfig.isWelshEnabled ).thenReturn(false)
      when(appConfig.frontendTemplatePath).thenReturn("microservice.services.frontend-template-provider.path")
      when(appConfig.feedbackFrontendUrl).thenReturn("/foo")
    }

  object BreadcrumbApplicationConfig extends ApplicationConfig {
    override val ggSignInUrl: String = ""
    override lazy val verifySignIn: String = ""
    override lazy val verifySignInContinue: Boolean = false
    override lazy val assetsPrefix: String = ""
    override lazy val reportAProblemNonJSUrl: String = ""
    override lazy val ssoUrl: Option[String] = None
    override val identityVerification: Boolean = false
    override lazy val betaFeedbackUnauthenticatedUrl: String = ""
    override lazy val notAuthorisedRedirectUrl: String = ""
    override lazy val contactFrontendPartialBaseUrl: String = ""
    override val govUkFinishedPageUrl: String = ""
    override val showGovUkDonePage: Boolean = true
    override lazy val analyticsHost: String = ""
    override lazy val betaFeedbackUrl: String = ""
    override lazy val analyticsToken: Option[String] = None
    override lazy val reportAProblemPartialUrl: String = ""
    override lazy val postSignInRedirectUrl: String = ""
    override val ivUpliftUrl: String = ""
    override lazy val pertaxFrontendUrl: String = "http://localhost:9232/account"
    override val contactFormServiceIdentifier: String = ""
    override lazy val breadcrumbPartialUrl: String = "http://localhost:9232/account"
    override lazy val showFullNI: Boolean = false
    override lazy val futureProofPersonalMax: Boolean = false
    override val isWelshEnabled = true
    override lazy val frontendTemplatePath: String = "microservice.services.frontend-template-provider.path"
    override val feedbackFrontendUrl: String = "/foo"
  }