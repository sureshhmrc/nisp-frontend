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
import org.joda.time.{DateTimeZone, LocalDate}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispSessionCache
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthorisedForNisp}
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountExclusionEvent, NIRecordEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.{Constants, Formatting}
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import uk.gov.hmrc.time.TaxYear

// TODO:// bind this in modules
// override val sessionCache: SessionCache = NispSessionCache

class NIRecordController @Inject()(customAuditConnector: CustomAuditConnector,
                                    sessionCache: SessionCache,
                                    metricsService: MetricsService,
                                    nationalInsuranceService: NationalInsuranceService,
                                    statePensionService: StatePensionService = StatePensionService,
                                    authenticate: AuthAction) extends NispFrontendController with PertaxHelper {

  val showFullNI: Boolean = ApplicationConfig.showFullNI
  val currentDate: LocalDate = new LocalDate(DateTimeZone.forID("Europe/London"))

  def showFull: Action[AnyContent] = show(gapsOnlyView = false)

  def showGaps: Action[AnyContent] = show(gapsOnlyView = true)

  def pta: Action[AnyContent] = authenticate {
    implicit request =>
      setFromPertax
      Redirect(routes.NIRecordController.showFull())
  }

  private def sendAuditEvent(nino: Nino, niRecord: NationalInsuranceRecord, yearsToContribute: Int)(implicit hc: HeaderCarrier) = {
    customAuditConnector.sendEvent(NIRecordEvent(
      nino.nino,
      yearsToContribute,
      niRecord.qualifyingYears,
      niRecord.numberOfGaps,
      niRecord.numberOfGapsPayable,
      niRecord.numberOfGaps - niRecord.numberOfGapsPayable,
      niRecord.qualifyingYearsPriorTo1975
    ))
  }

  private[controllers] def showPre1975Years(dateOfEntry: Option[LocalDate], dateOfBirth: Option[LocalDate], pre1975Years: Int): Boolean = {

    val dateOfEntryDiff = dateOfEntry.map(Constants.niRecordStartYear - TaxYear.taxYearFor(_).startYear)

    val sixteenthBirthdayTaxYear = dateOfBirth.map(dob => TaxYear.taxYearFor(dob.plusYears(Constants.niRecordMinAge)))
    val sixteenthBirthdayDiff = sixteenthBirthdayTaxYear.map(Constants.niRecordStartYear - _.startYear)

    (sixteenthBirthdayDiff, dateOfEntryDiff) match {
      case (Some(sb), Some(doe)) => sb.min(doe) > 0
      case (Some(sb), _) => sb > 0
      case (_, Some(doe)) => doe > 0
      case _ => pre1975Years > 0
    }
  }

  private[controllers] def generateTableList(tableStart: String, tableEnd: String): Seq[String] = {
    require(tableStart >= tableEnd)
    require(tableStart.take(Constants.yearStringLength).forall(_.isDigit))
    require(tableEnd.take(Constants.yearStringLength).forall(_.isDigit))

    val start = tableStart.take(Constants.yearStringLength).toInt
    val end = tableEnd.take(Constants.yearStringLength).toInt

    (start to end by -1) map Formatting.startYearToTaxYear
  }

  private def show(gapsOnlyView: Boolean): Action[AnyContent] = authenticate.async {
    implicit request =>
      val nino = request.nispAuthedUser.nino

      val nationalInsuranceResponseF = nationalInsuranceService.getSummary(nino)
      val statePensionResponseF = statePensionService.getSummary(nino)
      (for (
        nationalInsuranceRecordResponse <- nationalInsuranceResponseF;
        statePensionResponse <- statePensionResponseF
      ) yield {
        nationalInsuranceRecordResponse match {
          case Right(niRecord) =>
            if (gapsOnlyView && niRecord.numberOfGaps < 1) {
              Redirect(routes.NIRecordController.showFull())
            } else {
              val finalRelevantStartYear = statePensionResponse match {
                case Left(spExclusion) => spExclusion.finalRelevantStartYear
                  .getOrElse(throw new RuntimeException(s"NIRecordController: Can't get pensionDate from StatePensionExclusion $spExclusion"))
                case Right(sp) => sp.finalRelevantStartYear
              }
              val yearsToContribute = statePensionService.yearsToContributeUntilPensionAge(niRecord.earningsIncludedUpTo, finalRelevantStartYear)
              val recordHasEnded = yearsToContribute < 1
              val tableStart: String =
                if (recordHasEnded) Formatting.startYearToTaxYear(finalRelevantStartYear)
                else Formatting.startYearToTaxYear(niRecord.earningsIncludedUpTo.getYear)
              val tableEnd: String = niRecord.taxYears match {
                case Nil => tableStart
                case _ => niRecord.taxYears.last.taxYear
              }

              sendAuditEvent(nino, niRecord, yearsToContribute)

              Ok(nirecordpage(
                tableList = generateTableList(tableStart, tableEnd),
                niRecord = niRecord,
                gapsOnlyView = gapsOnlyView,
                recordHasEnded = recordHasEnded,
                yearsToContribute = yearsToContribute,
                finalRelevantEndYear = finalRelevantStartYear + 1,
                showPre1975Years = showPre1975Years(niRecord.dateOfEntry, request.dateOfBirth, niRecord.qualifyingYearsPriorTo1975),
                authenticationProvider = authenticate.getAuthenticationProvider(request.confidenceLevel),
                showFullNI = showFullNI,
                currentDate = currentDate))
            }
          case Left(exclusion) =>
            customAuditConnector.sendEvent(AccountExclusionEvent(
              nino.nino,
              request.name,
              exclusion
            ))
            Redirect(routes.ExclusionController.showNI())
        }
      }).recover {
        case ex: Exception => onError(ex)
      }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = authenticate.async {
    implicit request =>

      nationalInsuranceService.getSummary(request.nino) map {
        case Right(niRecord) =>
          Ok(nirecordGapsAndHowToCheckThem(niRecord.homeResponsibilitiesProtection))
        case Left(_) =>
          Redirect(routes.ExclusionController.showNI())
      }
  }

  def showVoluntaryContributions: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(nirecordVoluntaryContributions())
  }

}
