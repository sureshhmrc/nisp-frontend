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

import javax.inject.Inject
import org.joda.time.{LocalDate, Period}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Session}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.{ApplicationConfig, ApplicationGlobal}
import uk.gov.hmrc.nisp.controllers.auth.{AuthorisedForNisp, NispUser}
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountAccessEvent, AccountExclusionEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.{Exclusion, MQPScenario, Scenario}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.Constants._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.ExecutionContext.Implicits.global

class StatePensionController @Inject()(val sessionCache: SessionCache,
                                       val customAuditConnector: CustomAuditConnector,
                                       val applicationConfig: ApplicationConfig,
                                       val citizenDetailsService: CitizenDetailsService,
                                       val metricsService: MetricsService,
                                       val statePensionService: StatePensionService,
                                       val statePensionConnection: StatePensionConnection,
                                       val nationalInsuranceService: NationalInsuranceService,
                                       pertaxHelper: PertaxHelper,
                                       val authConnector: AuthConnector
                                      )
                                      (implicit override val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                        val formPartialRetriever: FormPartialRetriever,
                                        val templateRenderer: TemplateRenderer)
                                      extends FrontendController with AuthorisedForNisp
                                        with PartialRetriever {

  def showCope: Action[AnyContent] = AuthorisedByAny.async { implicit user =>
    implicit request =>
      pertaxHelper.isFromPertax.flatMap { isPertax =>

        statePensionConnection.getSummary(user.nino) map {
          case Right(statePension) if statePension.contractedOut => {
            Ok(statepension_cope(
              statePension.amounts.cope.weeklyAmount,
              isPertax,
              applicationConfig
            ))
          }
          case _ => Redirect(routes.StatePensionController.show())
        }
      }
  }

  def show: Action[AnyContent] = AuthorisedByAny.async { implicit user =>
    implicit request =>

      println("%%%%%%%%%%%%%% got here 1!")

      pertaxHelper.isFromPertax.flatMap { isPertax =>

        val statePensionResponseF = statePensionConnection.getSummary(user.nino)
        val nationalInsuranceResponseF = nationalInsuranceService.getSummary(user.nino)

              println("%%%%%%%%%%%%%% got here 2!")

        (for (
          statePensionResponse <- statePensionResponseF;
          nationalInsuranceResponse <- nationalInsuranceResponseF
        ) yield {
          (statePensionResponse, nationalInsuranceResponse) match {
            case (Right(statePension), Left(nationalInsuranceExclusion)) if statePension.reducedRateElection =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                nationalInsuranceExclusion
              ))
                    println("%%%%%%%%%%%%%% got here 3!")

              Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))

            case (Right(statePension), Right(nationalInsuranceRecord)) =>

              sendAuditEvent(statePension, user)

              val yearsToContributeUntilPensionAge = statePensionService.yearsToContributeUntilPensionAge(
                statePension.earningsIncludedUpTo,
                statePension.finalRelevantStartYear
              )
      println("%%%%%%%%%%%%%% got here 4!")

              if (statePension.mqpScenario.fold(false)(_ != MQPScenario.ContinueWorking)) {
                val yearsMissing = Constants.minimumQualifyingYearsNSP - statePension.numberOfQualifyingYears
                Ok(statepension_mqp(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  yearsMissing,
                  user.livesAbroad,
                  user.dateOfBirth.map(calculateAge(_, now().toLocalDate)),
                  isPertax,
                  yearsToContributeUntilPensionAge,
                  applicationConfig
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))
              } else if (statePension.forecastScenario.equals(Scenario.ForecastOnly)) {

                Ok(statepension_forecastonly(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  user.dateOfBirth.map(calculateAge(_, now().toLocalDate)),
                  user.livesAbroad,
                  isPertax,
                  yearsToContributeUntilPensionAge,
                  applicationConfig
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))

              } else if (statePension.abroadAutoCredit) {
                customAuditConnector.sendEvent(AccountExclusionEvent(
                  user.nino.nino,
                  user.name,
                  Exclusion.Abroad
                ))
                Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))

              } else {
                val (currentChart, forecastChart, personalMaximumChart) =
                  calculateChartWidths(
                    statePension.amounts.current,
                    statePension.amounts.forecast,
                    statePension.amounts.maximum
                  )
                Ok(statepension(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  currentChart,
                  forecastChart,
                  personalMaximumChart,
                  isPertax,
                  hidePersonalMaxYears = applicationConfig.futureProofPersonalMax,
                  user.dateOfBirth.map(calculateAge(_, now().toLocalDate)),
                  user.livesAbroad,
                  yearsToContributeUntilPensionAge,
                  applicationConfig
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))
              }

            case (Left(statePensionExclusion), _) =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                statePensionExclusion.exclusion
              ))
              Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))
            case _ => throw new RuntimeException("StatePensionController: SP and NIR are unmatchable. This is probably a logic error.")
          }
        }).recover {
          case ex: Exception => {
            InternalServerError(ApplicationGlobal.internalServerErrorTemplate)
          }
        }
      }
  }

  private def sendAuditEvent(statePension: StatePension, user: NispUser)(implicit hc: HeaderCarrier): Unit = {
    customAuditConnector.sendEvent(AccountAccessEvent(
      user.nino.nino,
      statePension.pensionDate,
      statePension.amounts.current.weeklyAmount,
      statePension.amounts.forecast.weeklyAmount,
      user.dateOfBirth,
      user.name,
      user.sex,
      statePension.contractedOut,
      statePension.forecastScenario,
      statePension.amounts.cope.weeklyAmount,
      user.authProviderOld
    ))
  }

  def calculateChartWidths(current: StatePensionAmount, forecast: StatePensionAmount, personalMaximum: StatePensionAmount): (SPChartModel, SPChartModel, SPChartModel) = {
    // scalastyle:off magic.number
    if (personalMaximum.weeklyAmount > forecast.weeklyAmount) {
      val currentChart = SPChartModel((current.weeklyAmount / personalMaximum.weeklyAmount * 100).toInt.max(Constants.chartWidthMinimum), current)
      val forecastChart = SPChartModel((forecast.weeklyAmount / personalMaximum.weeklyAmount * 100).toInt.max(Constants.chartWidthMinimum), forecast)
      val personalMaxChart = SPChartModel(100, personalMaximum)
      (currentChart, forecastChart, personalMaxChart)
    } else {
      if (forecast.weeklyAmount > current.weeklyAmount) {
        val currentPercentage = (current.weeklyAmount / forecast.weeklyAmount * 100).toInt
        val currentChart = SPChartModel(currentPercentage.max(Constants.chartWidthMinimum), current)
        val forecastChart = SPChartModel(100, forecast)
        (currentChart, forecastChart, forecastChart)
      } else {
        val currentChart = SPChartModel(100, current)
        val forecastChart = SPChartModel((forecast.weeklyAmount / current.weeklyAmount * 100).toInt, forecast)
        (currentChart, forecastChart, forecastChart)
      }
    }
  }

  private def storeUserInfoInSession(user: NispUser, contractedOut: Boolean)(implicit request: Request[AnyContent]): Session = {
    request.session +
      (NAME -> user.name.getOrElse("N/A")) +
      (NINO -> user.nino.nino) +
      (CONTRACTEDOUT -> contractedOut.toString)
  }

  private[controllers] def calculateAge(dateOfBirth: LocalDate, currentDate: LocalDate): Int = {
    new Period(dateOfBirth, currentDate).getYears
  }

  def pta(): Action[AnyContent] = AuthorisedByAny { implicit user =>
    implicit request =>
      pertaxHelper.setFromPertax
      Redirect(routes.StatePensionController.show())
  }

  def signOut: Action[AnyContent] = UnauthorisedAction { implicit request =>
    Redirect(applicationConfig.feedbackFrontendUrl).withNewSession
  }

  def timeout = UnauthorisedAction { implicit request =>
    Ok(sessionTimeout(applicationConfig))
  }
}
