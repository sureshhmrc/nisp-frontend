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

package uk.gov.hmrc.nisp.helpers

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.mock.MockitoSugar.mock
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

object AppConfig {

  val appConfig: ApplicationConfig = mock[ApplicationConfig]

  when(appConfig.ggSignInUrl).thenReturn("ggsignin")
  when(appConfig.verifySignIn).thenReturn("")
  when(appConfig.verifySignInContinue).thenReturn(false)
  when(appConfig.reportAProblemNonJSUrl).thenReturn("")
  when(appConfig.ssoUrl).thenReturn(None)
  when(appConfig.identityVerification).thenReturn(true)
  when(appConfig.betaFeedbackUnauthenticatedUrl).thenReturn("")
  when(appConfig.notAuthorisedRedirectUrl).thenReturn("")
  when(appConfig.contactFrontendPartialBaseUrl).thenReturn("")
  when(appConfig.govUkFinishedPageUrl).thenReturn("govukdone")
  when(appConfig.showGovUkDonePage).thenReturn(true)
  when(appConfig.analyticsHost).thenReturn("")
  when(appConfig.betaFeedbackUrl).thenReturn("")
  when(appConfig.analyticsToken).thenReturn(None)
  when(appConfig.reportAProblemPartialUrl).thenReturn("")
  when(appConfig.contactFormServiceIdentifier).thenReturn("")
  when(appConfig.postSignInRedirectUrl).thenReturn("")
  when(appConfig.ivUpliftUrl).thenReturn("ivuplift")
  when(appConfig.pertaxFrontendUrl).thenReturn("")
  when(appConfig.breadcrumbPartialUrl).thenReturn("")
  when(appConfig.showFullNI).thenReturn(false)
  when(appConfig.futureProofPersonalMax).thenReturn(false)
  when(appConfig.isWelshEnabled).thenReturn(true)
  when(appConfig.frontendTemplatePath).thenReturn("microservice.services.frontend-template-provider.path")
  when(appConfig.feedbackFrontendUrl).thenReturn("/foo")
}

object MockStatePensionController extends StatePensionController (
  MockSessionCache,
  MockCustomAuditConnector,
  AppConfig.appConfig,
  MockCitizenDetailsService,
  MockMetricsService.metrics,
  MockStatePensionService,
  MockStatePensionConnection,
  MockNationalInsuranceServiceViaNationalInsurance,
  MockPertaxHelper,
  MockAuthConnector
)(
  MockCachedStaticHtmlPartialRetriever,
  MockFormPartialRetriever,
  MockTemplateRenderer
) with MockitoSugar {
  override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  override implicit val formPartialRetriever: FormPartialRetriever = MockFormPartialRetriever
  override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  //  override val applicationConfig: ApplicationConfig = new ApplicationConfig {
  //    override val assetsPrefix: String = ""
  //    override val reportAProblemNonJSUrl: String = ""
  //    override val ssoUrl: Option[String] = None
  //    override val betaFeedbackUnauthenticatedUrl: String = ""
  //    override val contactFrontendPartialBaseUrl: String = ""
  //    override val analyticsHost: String = ""
  //    override val analyticsToken: Option[String] = None
  //    override val betaFeedbackUrl: String = ""
  //    override val reportAProblemPartialUrl: String = ""
  //    override val showGovUkDonePage: Boolean = true
  //    override val govUkFinishedPageUrl: String = "govukdone"
  //    override val verifySignIn: String = ""
  //    override val verifySignInContinue: Boolean = false
  //    override val postSignInRedirectUrl: String = ""
  //    override val notAuthorisedRedirectUrl: String = ""
  //    override val identityVerification: Boolean = true
  //    override val ivUpliftUrl: String = "ivuplift"
  //    override val ggSignInUrl: String = "ggsignin"
  //    override val pertaxFrontendUrl: String = ""
  //    override val contactFormServiceIdentifier: String = ""
  //    override val breadcrumbPartialUrl: String = ""
  //    override val showFullNI: Boolean = false
  //    override val futureProofPersonalMax: Boolean = false
  //    override val isWelshEnabled = true
  //    override val frontendTemplatePath: String = "microservice.services.frontend-template-provider.path"
  //    override val feedbackFrontendUrl: String = "/foo"
  //
  //  }
}