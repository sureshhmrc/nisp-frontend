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

import com.typesafe.config.Config
import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.nisp.config.wiring.{NispAuditConnector, WSHttp}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}
import uk.gov.hmrc.play.partials.FormPartialRetriever

object ApplicationGlobal extends ApplicationGlobalTrait

object NispFormPartialRetriever extends FormPartialRetriever {
  override def crypto: String => String = ApplicationGlobal.sessionCookieCryptoFilter.encrypt
  override def httpGet: CoreGet = WSHttp
}

case class GlobalErrorParams(frontendTemplatePath: String, analyticsHost: String, analyticsToken: Option[String])

trait ApplicationGlobalTrait extends DefaultFrontendGlobal with PartialRetriever {
  val controllerConfiguration: ControllerConfiguration = new ControllerConfiguration(Play.current.configuration)

  override val auditConnector = NispAuditConnector
  override val loggingFilter = new NispLoggingFilter(controllerConfiguration)
  override val frontendAuditFilter = new NispFrontendAuditFilter(controllerConfiguration)

  implicit val partialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: LocalTemplateRenderer = LocalTemplateRenderer

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(Play.current.configuration.underlying).verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {

    lazy val analyticsToken: Option[String] = configuration.getString(s"google-analytics.token")
    lazy val frontendTemplatePath: String = configuration.getString("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")
    lazy val analyticsHost: String = configuration.getString(s"google-analytics.host").getOrElse("auto")

    val params = GlobalErrorParams(
      frontendTemplatePath,
      analyticsHost,
      analyticsToken)

    uk.gov.hmrc.nisp.views.html.global_error(pageTitle, heading, message, params)
  }

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
}

class ControllerConfiguration @Inject()(configuration: Configuration) extends ControllerConfig {
  lazy val controllerConfigs = configuration.underlying.as[Config]("controllers")
}

class NispLoggingFilter @Inject()(controllerConfiguration: ControllerConfiguration) extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = controllerConfiguration.paramsForController(controllerName).needsLogging
}

class NispFrontendAuditFilter @Inject()(controllerConfiguration: ControllerConfiguration) extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {
  override lazy val maskedFormFields = Seq.empty
  override lazy val applicationPort = None
  override lazy val auditConnector = NispAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean = controllerConfiguration.paramsForController(controllerName).needsAuditing
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override protected def appNameConfiguration: Configuration = Play.current.configuration
}
