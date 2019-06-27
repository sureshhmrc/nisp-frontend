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

package uk.gov.hmrc.nisp.controllers

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.http._
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispFormPartialRetriever
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.helpers.{MockAuthConnector, MockCachedStaticHtmlPartialRetriever, MockCitizenDetailsService, MockIdentityVerificationConnector}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class GARedirectControllerSpec  extends PlaySpec with MockitoSugar with OneAppPerSuite {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[CitizenDetailsService].toInstance(MockCitizenDetailsService))
    .overrides(bind[ApplicationConfig].toInstance(mock[ApplicationConfig]))
    .overrides(bind[IdentityVerificationConnector].toInstance(MockIdentityVerificationConnector))
    .overrides(bind[AuthConnector].toInstance(MockAuthConnector))
    .overrides(bind[CachedStaticHtmlPartialRetriever].toInstance(MockCachedStaticHtmlPartialRetriever))
    .overrides(bind[TemplateRenderer].toInstance(MockTemplateRenderer))
    .build()

  private implicit val fakeRequest = FakeRequest("GET", "/redirect")
  private implicit val lang = Lang("en")
  private implicit val retriever = MockCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  val testGARedirectController: GARedirectController = app.injector.instanceOf[GARedirectController]

  "GET /redirect" should {
    "return 200" in {
      val result = testGARedirectController.show(fakeRequest)
      status(result) mustBe Status.OK
    }

  }
}
