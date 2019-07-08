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
import play.api.http.Status._
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, SessionRecordNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.helpers.MockCitizenDetailsService
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthActionSpec extends UnitSpec with OneAppPerSuite {


/*  class MockAuthConnector(exception: Throwable) extends AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(Retrie)
  }*/

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
      val cds: CitizenDetailsService = app.injector.instanceOf[CitizenDetailsService]
      val authAction = new AuthActionImpl(new BrokenAuthConnector(new SessionRecordNotFound), cds)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }

    //TODO : do we need this?
//          "redirect to Verify with IV disabled" in {
//            val controller = new MockStatePensionController {
//              override val authenticate: AuthAction = new MockAuthAction()
//              override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
//              override val applicationConfig: ApplicationConfig = new ApplicationConfig {
//                override val assetsPrefix: String = ""
//                override val reportAProblemNonJSUrl: String = ""
//                override val ssoUrl: Option[String] = None
//                override val betaFeedbackUnauthenticatedUrl: String = ""
//                override val contactFrontendPartialBaseUrl: String = ""
//                override val govUkFinishedPageUrl: String = "govukdone"
//                override val showGovUkDonePage: Boolean = false
//                override val analyticsHost: String = ""
//                override val analyticsToken: Option[String] = None
//                override val betaFeedbackUrl: String = ""
//                override val reportAProblemPartialUrl: String = ""
//                override val verifySignIn: String = ""
//                override val verifySignInContinue: Boolean = false
//                override val postSignInRedirectUrl: String = ""
//                override val notAuthorisedRedirectUrl: String = ""
//                override val identityVerification: Boolean = false
//                override val ivUpliftUrl: String = "ivuplift"
//                override val ggSignInUrl: String = "ggsignin"
//                override val pertaxFrontendUrl: String = ""
//                override val contactFormServiceIdentifier: String = ""
//                override val breadcrumbPartialUrl: String = ""
//                override lazy val showFullNI: Boolean = false
//                override val futureProofPersonalMax: Boolean = false
//                override val isWelshEnabled = false
//                override val frontendTemplatePath: String = configuration.getString("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")
//                override val feedbackFrontendUrl: String = "/foo"
//              }
//              override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
//            }
//            redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/verify-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount")
//          }


//TODO: Appropiate for a unit test or acceptance test?
//          "return 200, create an authenticated session" in {
//            val result = MockStatePensionController.show()(authenticatedFakeRequest())
//            contentAsString(result) should include("Sign out")
//          }

    //TODO: How much data do we need to allow people into the service?
          "return error for blank user" ignore {
      val cds: CitizenDetailsService = CitizenDetailsService
      val authAction = new AuthActionImpl(???, MockCitizenDetailsService) with MockAuthorisedFunctions {
          override def authorised(): AuthorisedFunction = new MockAuthorisedFunction(EmptyPredicate)

          override def authorised(predicate: Predicate): AuthorisedFunction = new MockAuthorisedFunction(predicate)
      }
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }


    // TODO: Need to look in to this
    //    "GET /signout" should {
    //      "redirect to the questionnaire page when govuk done page is disabled" in {
    //        val controller = new MockStatePensionController {
    //          override val authenticate: AuthAction = MockAuthAction
    //          override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    //          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
    //            override val assetsPrefix: String = ""
    //            override val reportAProblemNonJSUrl: String = ""
    //            override val ssoUrl: Option[String] = None
    //            override val betaFeedbackUnauthenticatedUrl: String = ""
    //            override val contactFrontendPartialBaseUrl: String = ""
    //            override val govUkFinishedPageUrl: String = "govukdone"
    //            override val showGovUkDonePage: Boolean = false
    //            override val analyticsHost: String = ""
    //            override val analyticsToken: Option[String] = None
    //            override val betaFeedbackUrl: String = ""
    //            override val reportAProblemPartialUrl: String = ""
    //            override val verifySignIn: String = ""
    //            override val verifySignInContinue: Boolean = false
    //            override val postSignInRedirectUrl: String = ""
    //            override val notAuthorisedRedirectUrl: String = ""
    //            override val identityVerification: Boolean = false
    //            override val ivUpliftUrl: String = "ivuplift"
    //            override val ggSignInUrl: String = "ggsignin"
    //            override val pertaxFrontendUrl: String = ""
    //            override val contactFormServiceIdentifier: String = ""
    //            override val breadcrumbPartialUrl: String = ""
    //            override lazy val showFullNI: Boolean = false
    //            override val futureProofPersonalMax: Boolean = false
    //            override val isWelshEnabled = false
    //            override val frontendTemplatePath: String = configuration.getString("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")
    //            override val feedbackFrontendUrl: String = "/foo"
    //          }
    //          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    //        }
    //        val result = controller.signOut(fakeRequest)
    //
    //        redirectLocation(result).get shouldBe "/foo"
    //      }
    //
    //      "redirect to the gov.uk done page when govuk done page is enabled" in {
    //        val controller = new MockStatePensionController {
    //          override val authenticate: AuthAction = MockAuthAction
    //          override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    //          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
    //            override val assetsPrefix: String = ""
    //            override val reportAProblemNonJSUrl: String = ""
    //            override val ssoUrl: Option[String] = None
    //            override val betaFeedbackUnauthenticatedUrl: String = ""
    //            override val contactFrontendPartialBaseUrl: String = ""
    //            override val govUkFinishedPageUrl: String = "govukdone"
    //            override val showGovUkDonePage: Boolean = true
    //            override val analyticsHost: String = ""
    //            override val analyticsToken: Option[String] = None
    //            override val betaFeedbackUrl: String = ""
    //            override val reportAProblemPartialUrl: String = ""
    //            override val verifySignIn: String = ""
    //            override val verifySignInContinue: Boolean = false
    //            override val postSignInRedirectUrl: String = ""
    //            override val notAuthorisedRedirectUrl: String = ""
    //            override val identityVerification: Boolean = false
    //            override val ivUpliftUrl: String = "ivuplift"
    //            override val ggSignInUrl: String = "ggsignin"
    //            override val pertaxFrontendUrl: String = ""
    //            override val contactFormServiceIdentifier: String = ""
    //            override val breadcrumbPartialUrl: String = ""
    //            override lazy val showFullNI: Boolean = false
    //            override val futureProofPersonalMax: Boolean = false
    //            override val isWelshEnabled = false
    //            override val frontendTemplatePath: String = configuration.getString("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")
    //            override val feedbackFrontendUrl: String = "/foo"
    //          }
    //          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    //        }
    //        val result = controller.signOut(fakeRequest)
    //
    //        redirectLocation(result).get shouldBe "/foo"
    //      }
    //    }

    //    "GET /timeout" should {
    //      "return the timeout page" in {
    //        val result = MockStatePensionController.timeout(fakeRequest)
    //        contentType(result) shouldBe Some("text/html")
    //        contentAsString(result).contains("For your security we signed you out because you didn't use the service for 15 minutes or more.")
    //      }

    //    "return redirect for unauthenticated user" in {
    //      val result = MockNIRecordControllerImpl.showGaps(fakeRequest)
    //      redirectLocation(result) shouldBe Some(ggSignInUrl)
    //    }

    //TODO testing blank data for auth retrieval
    //    "return error page for blank response NINO" ignore{
    //      val result = new MockNIRecordControllerImpl(TestAccountBuilder.blankNino)
    //        .showGaps(authenticatedFakeRequest(mockBlankUserId))
    //      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    //    }

    //    "return redirect for unauthenticated user" in {
    //      val result = MockNIRecordControllerImpl.showFull(fakeRequest)
    //      redirectLocation(result) shouldBe Some(ggSignInUrl)
    //    }


    //    "return redirect for unauthenticated user" in {
    //      val result = MockNIRecordControllerImpl.showGapsAndHowToCheckThem(fakeRequest)
    //      redirectLocation(result) shouldBe Some(ggSignInUrl)
    //    }

    //    "return redirect for unauthenticated user" in {
    //      val result = MockNIRecordControllerImpl.showVoluntaryContributions(fakeRequest)
    //      redirectLocation(result) shouldBe Some(ggSignInUrl)
    //    }

  }
}


