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

import java.net.{URI, URLEncoder}

import javax.inject.Inject
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L200
import uk.gov.hmrc.play.frontend.auth.{CompositePageVisibilityPredicate, PageVisibilityPredicate, UpliftingIdentityConfidencePredicate}

class NispCompositePageVisibilityPredicate @Inject()(applicationConfig: ApplicationConfig) extends CompositePageVisibilityPredicate {
  override def children: Seq[PageVisibilityPredicate] = Seq(
    new UpliftingIdentityConfidencePredicate(L200, ivUpliftURI)
  )

  private val ivUpliftURI: URI =
    new URI(s"${applicationConfig.ivUpliftUrl}?origin=NISP&" +
      s"completionURL=${URLEncoder.encode(applicationConfig.postSignInRedirectUrl, "UTF-8")}&" +
      s"failureURL=${URLEncoder.encode(applicationConfig.notAuthorisedRedirectUrl, "UTF-8")}" +
      s"&confidenceLevel=200")
}

object NispCompositePageVisibilityPredicate extends NispCompositePageVisibilityPredicate(ApplicationConfig)