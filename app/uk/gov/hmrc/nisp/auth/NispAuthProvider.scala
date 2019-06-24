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

package uk.gov.hmrc.nisp.auth

import javax.inject.Inject
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.{AnyAuthenticationProvider, GovernmentGateway, Verify}

import scala.concurrent.Future

class NispAuthProvider @Inject()(governmentGatewayProvider: GovernmentGatewayProvider, verifyProvider: VerifyProvider) extends AnyAuthenticationProvider {
  override def ggwAuthenticationProvider: GovernmentGateway = governmentGatewayProvider
  override def verifyAuthenticationProvider: Verify = verifyProvider
  override def login: String = ??? // Default is GG. Unable to use this based on library, just override redirectToLogin.
  override def redirectToLogin(implicit request: Request[_]): Future[FailureResult] = governmentGatewayProvider.redirectToLogin
  override def handleSessionTimeout(implicit request: Request[_]): Future[FailureResult] = governmentGatewayProvider.handleSessionTimeout
}

object NispAuthProvider extends NispAuthProvider(GovernmentGatewayProvider, VerifyProvider)