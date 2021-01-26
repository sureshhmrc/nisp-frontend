/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.utils.Constants

case class AuthDetails(confidenceLevel: ConfidenceLevel,
                       authProvider: Option[String],
                       loginTimes: LoginTimes
                      ){
  val isGG: Boolean = authProvider.contains(Constants.GovernmentGatewayId) //TODO test and check if these are correct
  val isVerify: Boolean = authProvider.contains(Constants.VerifyProviderId)
}
