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

package uk.gov.hmrc.nisp.controllers.auth

import akka.util.Timeout
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, SessionRecordNotFound}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.controllers.LandingController.applicationConfig
import uk.gov.hmrc.nisp.helpers.{MockAuthAction, MockAuthConnector, MockCachedStaticHtmlPartialRetriever, MockCitizenDetailsConnector, MockCitizenDetailsService, MockStatePensionController, MockStatePensionControllerImpl, TestAccountBuilder}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec
import play.api.Play.configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthActionSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].toInstance(MockAuthConnector))
    .overrides(bind[CitizenDetailsService].toInstance(MockCitizenDetailsService))
    .build()

  class BrokenAuthConnector(exception: Throwable) extends AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok }
  }

  val ggSignInUrl = "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  implicit val timeout: Timeout = 5 seconds

  "GET /statepension" should {
    "return 303 when no session" in {
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction = new AuthActionImpl(new BrokenAuthConnector(new SessionRecordNotFound), cds)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }

    //TODO : do we need this?
          "redirect to Verify with IV disabled" in {
            val controller = new MockStatePensionController {

              override val applicationConfig: ApplicationConfig = new ApplicationConfig {
                override val assetsPrefix: String = ""
                override val reportAProblemNonJSUrl: String = ""
                override val ssoUrl: Option[String] = None
                override val betaFeedbackUnauthenticatedUrl: String = ""
                override val contactFrontendPartialBaseUrl: String = ""
                override val govUkFinishedPageUrl: String = "govukdone"
                override val showGovUkDonePage: Boolean = false
                override val analyticsHost: String = ""
                override val analyticsToken: Option[String] = None
                override val betaFeedbackUrl: String = ""
                override val reportAProblemPartialUrl: String = ""
                override val verifySignIn: String = ""
                override val verifySignInContinue: Boolean = false
                override val postSignInRedirectUrl: String = ""
                override val notAuthorisedRedirectUrl: String = ""
                override val identityVerification: Boolean = false
                override val ivUpliftUrl: String = "ivuplift"
                override val ggSignInUrl: String = "ggsignin"
                override val pertaxFrontendUrl: String = ""
                override val contactFormServiceIdentifier: String = ""
                override val breadcrumbPartialUrl: String = ""
                override lazy val showFullNI: Boolean = false
                override val futureProofPersonalMax: Boolean = false
                override val isWelshEnabled = false
                override val frontendTemplatePath: String = "/template/mustache"
                override val feedbackFrontendUrl: String = "/foo"
              }
              override val authenticate: AuthAction = AuthActionSelector.decide(applicationConfig)
              override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
            }
             val result = controller.show()(FakeRequest("", ""))
            redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/verify-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount")
          }

    "return error for blank user" in {
      val authAction = new AuthActionImpl(MockAuthConnector, MockCitizenDetailsService) with MockAuthorisedFunctions {
        override def authorised(): AuthorisedFunction = new MockAuthorisedFunction(EmptyPredicate)

        override def authorised(predicate: Predicate): AuthorisedFunction = new MockAuthorisedFunction(predicate)
      }
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))

      an[InternalServerException] should be thrownBy await(result)
    }

  }
}


