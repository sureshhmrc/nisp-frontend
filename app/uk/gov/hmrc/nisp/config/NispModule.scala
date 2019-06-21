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

package uk.gov.hmrc.nisp.config

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.wiring.{NispCachedStaticHtmlPartialRetriever, NispSessionCache, WSHttp}
import uk.gov.hmrc.nisp.controllers.ExclusionController
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

class NispModule extends Module{

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
      bind[CachedStaticHtmlPartialRetriever].to[NispCachedStaticHtmlPartialRetriever],
      bind[ApplicationConfig].toInstance(ApplicationConfig),
      bind[CitizenDetailsService].toInstance(CitizenDetailsService),
      bind[NationalInsuranceService].toInstance(NationalInsuranceService),
//      bind[SessionCache].to(classOf[NispSessionCache]),
      bind[StatePensionService].toInstance(StatePensionService),
      bind[WSHttp].toInstance(WSHttp),
      bind[ExclusionController].toSelf.eagerly()
  )
}
