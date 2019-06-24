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

package uk.gov.hmrc.nisp.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

@Singleton
class ExclusionController @Inject()(val citizenDetailsService: CitizenDetailsService,
                                    val applicationConfig: ApplicationConfig,
                                    statePensionConnection: StatePensionConnection,
                                    nationalInsuranceService: NationalInsuranceService)(implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                     implicit val formPartialRetriever: FormPartialRetriever,
                                     implicit val templateRenderer: TemplateRenderer)
                                    extends NispFrontendController(cachedStaticHtmlPartialRetriever,
                                      formPartialRetriever,
                                      templateRenderer)
                                    with AuthorisedForNisp
                                    with AuthenticationConnectors {

  def showSP: Action[AnyContent] = AuthorisedByAny.async {
    implicit user =>
      implicit request =>

        val statePensionF = statePensionConnection.getSummary(user.nino)
        val nationalInsuranceF = nationalInsuranceService.getSummary(user.nino)

        for (
          statePension <- statePensionF;
          nationalInsurance <- nationalInsuranceF
        ) yield {
          statePension match {
            case Right(sp) if sp.reducedRateElection =>
              Ok(excluded_sp(Exclusion.MarriedWomenReducedRateElection, Some(sp.pensionAge), Some(sp.pensionDate), false, None, applicationConfig))
            case Right(sp) if sp.abroadAutoCredit =>
              Ok(excluded_sp(Exclusion.Abroad, Some(sp.pensionAge), Some(sp.pensionDate), nationalInsurance.isRight, None, applicationConfig))
            case Left(exclusion) =>
              if (exclusion.exclusion == Exclusion.Dead)
                Ok(excluded_dead(Exclusion.Dead, exclusion.pensionAge, applicationConfig))
              else if (exclusion.exclusion == Exclusion.ManualCorrespondenceIndicator)
                Ok(excluded_mci(Exclusion.ManualCorrespondenceIndicator, exclusion.pensionAge, applicationConfig))
              else {
                Ok(excluded_sp(exclusion.exclusion, exclusion.pensionAge, exclusion.pensionDate, nationalInsurance.isRight, exclusion.statePensionAgeUnderConsideration, applicationConfig))
              }
            case _ =>
              Logger.warn("User accessed /exclusion as non-excluded user")
              Redirect(routes.StatePensionController.show())
          }
        }
  }

  def showNI: Action[AnyContent] = AuthorisedByAny.async {
    implicit user =>
      implicit request =>
        nationalInsuranceService.getSummary(user.nino).map {
          case Left(exclusion) =>
            if (exclusion == Exclusion.Dead) {
              Ok(excluded_dead(Exclusion.Dead, None, applicationConfig))
            }
            else if (exclusion == Exclusion.ManualCorrespondenceIndicator) {
              Ok(excluded_mci(Exclusion.ManualCorrespondenceIndicator, None, applicationConfig))
            } else {
              Ok(excluded_ni(exclusion, applicationConfig))
            }
          case _ =>
            Logger.warn("User accessed /exclusion/nirecord as non-excluded user")
            Redirect(routes.NIRecordController.showGaps())
        }
  }
}
