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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status._
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, SessionRecordNotFound}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.helpers.{MockAuthConnector, MockCitizenDetailsConnector, MockCitizenDetailsService}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class VerifyAuthActionSpec extends UnitSpec with OneAppPerSuite with ScalaFutures {

  val verifyUrl = "http://localhost:9949/auth-login-stub/verify-sign-in"
  implicit val timeout: Timeout = 5 seconds

  class BrokenAuthConnector(exception: Throwable) extends AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok }
  }

  "GET /signin/verify" should {
    "return 303 and redirect to verify when No Session" in {
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction = new VerifyAuthActionImpl(new BrokenAuthConnector(new SessionRecordNotFound), cds)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(verifyUrl)
    }

    "return error for blank user" in {
      val authAction = new VerifyAuthActionImpl(MockAuthConnector, MockCitizenDetailsService) with MockAuthorisedFunctions {
        override def authorised(): AuthorisedFunction = new MockAuthorisedFunction(EmptyPredicate)

        override def authorised(predicate: Predicate): AuthorisedFunction = new MockAuthorisedFunction(predicate)
      }
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))

      an[InternalServerException] should be thrownBy await(result)
    }
  }

}
