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

package uk.gov.hmrc.nisp.connectors

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.connectors.IdentityVerificationSuccessResponse._
import uk.gov.hmrc.nisp.helpers.MockIdentityVerificationConnector
import uk.gov.hmrc.play.test.UnitSpec


class IdentityVerificationConnectorSpec extends UnitSpec with OneAppPerSuite with ScalaFutures {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[IdentityVerificationConnector].to[MockIdentityVerificationConnector])
    .build()

  lazy val identityVerificationConnector = app.injector.instanceOf[IdentityVerificationConnector]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "return success when identityVerification returns success" in {
    identityVerificationConnector.identityVerificationResponse("success-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(Success)
  }

  "return incomplete when identityVerification returns incomplete" in {
    identityVerificationConnector.identityVerificationResponse("incomplete-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(Incomplete)
  }

  "return failed matching when identityVerification returns failed matching" in {
    identityVerificationConnector.identityVerificationResponse("failed-matching-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(FailedMatching)
  }

  "return failed iv when identityVerification returns failed matching" in {
    identityVerificationConnector.identityVerificationResponse("failed-iv-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(FailedIV)
  }

  "return insufficient evidence when identityVerification returns insufficient evidence" in {
    identityVerificationConnector.identityVerificationResponse("insufficient-evidence-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(InsufficientEvidence)
  }

  "return locked out when identityVerification returns locked out" in {
    identityVerificationConnector.identityVerificationResponse("locked-out-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(LockedOut)
  }

  "return user aborted when identityVerification returns user aborted" in {
    identityVerificationConnector.identityVerificationResponse("user-aborted-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(UserAborted)
  }

  "return timeout when identityVerification returns timeout" in {
    identityVerificationConnector.identityVerificationResponse("timeout-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(Timeout)
  }

  "return technical issue when identityVerification returns technical issue" in {
    identityVerificationConnector.identityVerificationResponse("technical-issue-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(TechnicalIssue)
  }

  "return precondition failed when identityVerification returns precondition failed" in {
    identityVerificationConnector.identityVerificationResponse("precondition-failed-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse(PreconditionFailed)
  }

  "return no failure when identityVerification returns non-existant result type" in {
    identityVerificationConnector.identityVerificationResponse("invalid-journey-id").futureValue shouldBe IdentityVerificationSuccessResponse("ABCDEFG")
  }

  "return failed future for invalid json fields" in {
    val result = identityVerificationConnector.identityVerificationResponse("invalid-fields-journey-id")
    ScalaFutures.whenReady(result) { e =>
      e shouldBe a [IdentityVerificationErrorResponse]
    }
  }
}
