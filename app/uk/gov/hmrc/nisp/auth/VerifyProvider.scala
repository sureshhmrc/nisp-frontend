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

import java.net.URLEncoder

import javax.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.frontend.auth.Verify

import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys

class VerifyProvider @Inject()(applicationConfig: ApplicationConfig) extends Verify {
  override def redirectToLogin(implicit request: Request[_]): Future[FailureResult] = {
    Future.successful(Redirect(login).withSession(
      SessionKeys.redirect -> applicationConfig.postSignInRedirectUrl,
      SessionKeys.loginOrigin -> "YSP"
    ))
  }

  override def login: String = {

    var url = applicationConfig.verifySignIn

    if (applicationConfig.verifySignInContinue) {
      url += s"?continue=${URLEncoder.encode(applicationConfig.postSignInRedirectUrl, "UTF-8")}"
    }

    url

  }
}

object VerifyProvider extends VerifyProvider(ApplicationConfig)