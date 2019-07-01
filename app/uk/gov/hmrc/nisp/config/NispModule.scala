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
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.wiring.{NispAuditConnector, NispAuthConnector, NispCachedStaticHtmlPartialRetriever, NispSessionCache, WSHttp}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class NispModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
      bind[CachedStaticHtmlPartialRetriever].to[NispCachedStaticHtmlPartialRetriever],
      bind[AuditConnector].toInstance(NispAuditConnector),
      bind[FrontendLoggingFilter].to[NispLoggingFilter],
      bind[FormPartialRetriever].toInstance(NispFormPartialRetriever),
      bind[FrontendAuditFilter].to[NispFrontendAuditFilter],
      bind[HttpGet].toInstance(WSHttp),
      bind[WSHttp].toInstance(WSHttp),
      bind[TemplateRenderer].toInstance(LocalTemplateRenderer),
      bind[SessionCache].to(classOf[NispSessionCache]),
      bind[AuthConnector].toInstance(NispAuthConnector)
  )
}
