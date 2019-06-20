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

package uk.gov.hmrc.nisp.controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import org.joda.time.LocalDate
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, Name, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.utils.{Constants, Country}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

case class NispAuthedUser (nino: Nino,
                           confidenceLevel: ConfidenceLevel,
                           dateOfBirth: Option[LocalDate],
                           name: Option[Name],
                           address: Option[ItmpAddress]) {

  lazy val livesAbroad: Boolean = address.fold(false)( co => co.countryName.exists(Country.isAbroad(_)) )

}

case class AuthenticatedRequest[A](request: Request[A],
                                   nispAuthedUser: NispAuthedUser
                                  ) extends WrappedRequest[A](request)


class AuthActionImpl @Inject()(override val authConnector: AuthConnector)
                              (implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised().
      retrieve(Retrievals.nino and Retrievals.confidenceLevel and Retrievals.dateOfBirth and Retrievals.name and Retrievals.itmpAddress) {
        case Some(nino) ~ confidenceLevel ~ dateOfBirth ~ name ~ itmpAddress => {
          block(AuthenticatedRequest(request, NispAuthedUser(Nino(nino), confidenceLevel, dateOfBirth, name, itmpAddress)))
        }
        case _ => throw new RuntimeException("Can't find credentials for user")
      }
  }

  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String = {
      if (confidenceLevel.level == 500) Constants.verify else Constants.iv
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest] {
  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String
}
