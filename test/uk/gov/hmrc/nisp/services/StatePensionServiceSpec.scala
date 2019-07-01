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

package uk.gov.hmrc.nisp.services

import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationGlobalTrait
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StatePensionServiceSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[ApplicationGlobalTrait].toInstance(MockApplicationGlobal))
    .build()

  val statePensionService = new StatePensionService()

  "yearsToContributeUntilPensionAge" should {
    "mustBe 2 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2016-4-5" in {
      val actual: Int = statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        finalRelevantYearStart = 2017
      )

      val expected: Int = 2

      actual mustBe expected
    }

    "mustBe 3 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2015-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2015, 4, 5),
        finalRelevantYearStart = 2017
      ) mustBe 3
    }

    "mustBe 1 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2017, 4, 5),
        finalRelevantYearStart = 2017
      ) mustBe 1
    }

    "mustBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2018-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2018, 4, 5),
        finalRelevantYearStart = 2017
      ) mustBe 0
    }

    "mustBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-6" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2017, 4, 6),
        finalRelevantYearStart = 2017
      ) mustBe 0
    }
  }

  "StatePensionConnection" should {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

    lazy val configuration = app.configuration

    lazy val statePensionConnector = new StatePensionConnector(MockNispHttp.mockHttp, MockMetricsService.metrics, configuration)
    lazy val statePensionConnection = new StatePensionConnection(
      statePensionConnector = statePensionConnector,
      statePensionService = MockStatePensionService)

    "transform the Dead 403 into a Left(StatePensionExclusion(Dead))" in {
      val actualFuture: Future[Either[StatePensionExclusionFiltered, StatePension]] = statePensionConnection.getSummary(TestAccountBuilder.excludedAll)

      for {
        actual <- actualFuture
      } yield {
        actual mustBe Left(StatePensionExclusionFiltered(Exclusion.Dead))
      }
    }

    "transform the MCI 403 into a Left(StatePensionExclusion(MCI))" in {
      val actualFuture = statePensionConnection.getSummary(TestAccountBuilder.excludedAllButDead)

      val actual = actualFuture.map {
        exclusion: Either[StatePensionExclusionFiltered, StatePension] =>
        exclusion mustBe Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))
      }
    }

    "return the connector response for a regular user" in {
      val actualFuture = statePensionConnection.getSummary(TestAccountBuilder.regularNino)

      val expected = StatePension(
        new LocalDate(2015, 4, 5),
        StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64, new LocalDate(2018, 7, 6), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = false, abroadAutoCredit = false, statePensionAgeUnderConsideration = false
      )

      for {
        actual <- actualFuture
      } yield {
        actual mustBe Right(expected)
      }
    }

    "return the connector response for a RRE user" in {
      val actualFuture = statePensionConnection.getSummary(TestAccountBuilder.excludedMwrre)

      val expected = StatePension(
        new LocalDate(2015, 4, 5),
        StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64, new LocalDate(2018, 7, 6), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = true, abroadAutoCredit = false, statePensionAgeUnderConsideration = false
      )

      for {
        actual <- actualFuture
      } yield {
        actual mustBe Right(expected)
      }
    }

//    "return the connector response for a Abroad user" in {
//      whenReady(statePensionConnection.getSummary(TestAccountBuilder.excludedAbroad)) { statePension =>
//        statePension mustBe Right(StatePension(
//          new LocalDate(2015, 4, 5),
//          StatePensionAmounts(
//            protectedPayment = false,
//            StatePensionAmountRegular(133.41, 580.1, 6961.14),
//            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
//            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
//            StatePensionAmountRegular(0, 0, 0)
//          ),
//          64, new LocalDate(2018, 7, 6), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = false, abroadAutoCredit = true, statePensionAgeUnderConsideration = false
//        ))
//      }
//    }

//    "return the connector response with PostStatePensionAge exclusion for all the exclusions except MCI and Dead" in {
//      whenReady(statePensionConnection.getSummary(TestAccountBuilder.excludedAllButDeadMCI)) { statePension =>
//        statePension mustBe Left(StatePensionExclusionFiltered(
//          Exclusion.PostStatePensionAge,
//          pensionAge = Some(65),
//          pensionDate = Some(new LocalDate(2017, 7, 18)),
//          statePensionAgeUnderConsideration = Some(false)
//        ))
//      }
//    }
//
//    "return the connector response for a user with a true flag for State Pension Age Under Consideration" in {
//      whenReady(statePensionConnection.getSummary(TestAccountBuilder.spaUnderConsiderationNino)) { statePension =>
//        statePension mustBe Right(StatePension(
//          new LocalDate(2015, 4, 5),
//          StatePensionAmounts(
//            protectedPayment = false,
//            StatePensionAmountRegular(133.41, 580.1, 6961.14),
//            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
//            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
//            StatePensionAmountRegular(0, 0, 0)
//          ),
//          64, new LocalDate(2018, 7, 6), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = false, abroadAutoCredit = false, statePensionAgeUnderConsideration = true
//        ))
//      }
//    }
//
//    "return the connector response for a user with no flag for State Pension Age Under Consideration" in {
//      whenReady(statePensionConnection.getSummary(TestAccountBuilder.spaUnderConsiderationNoFlagNino)) { statePension =>
//        statePension mustBe Right(StatePension(
//          new LocalDate(2015, 4, 5),
//          StatePensionAmounts(
//            protectedPayment = false,
//            StatePensionAmountRegular(133.41, 580.1, 6961.14),
//            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
//            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
//            StatePensionAmountRegular(0, 0, 0)
//          ),
//          64, new LocalDate(2018, 7, 6), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = false, abroadAutoCredit = false, statePensionAgeUnderConsideration = false
//        ))
//      }
//    }
//
//    "return the connector response for a user with exclusion with a true flag for State Pension Age Under Consideration" in {
//      whenReady(statePensionConnection.getSummary(TestAccountBuilder.spaUnderConsiderationExclusionIoMNino)) { statePension =>
//        statePension mustBe Left(StatePensionExclusionFiltered(
//          Exclusion.IsleOfMan,
//          pensionAge = Some(65),
//          pensionDate = Some(new LocalDate(2017, 7, 18)),
//          statePensionAgeUnderConsideration = Some(true)
//        ))
//      }
//    }
//
//    "return the connector response for a user with exclusion with no flag for State Pension Age Under Consideration" in {
//      whenReady(statePensionConnection.getSummary(TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino)) { statePension =>
//        statePension mustBe Left(StatePensionExclusionFiltered(
//          Exclusion.IsleOfMan,
//          pensionAge = Some(65),
//          pensionDate = Some(new LocalDate(2017, 7, 18)),
//          statePensionAgeUnderConsideration = None
//        ))
//      }
//    }

  }

}
