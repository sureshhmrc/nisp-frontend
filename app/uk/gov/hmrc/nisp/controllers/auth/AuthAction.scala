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
import play.api.Mode.Mode
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, NoActiveSession, PlayAuthConnector}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.{NispAuthConnector, WSHttp}
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, CitizenDetailsServiceImpl}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedRequest[A](request: Request[A],
                                   nispAuthedUser: NispAuthedUser
                                  ) extends WrappedRequest[A](request)

class AuthActionImpl @Inject()(override val authConnector: NispAuthConnector,
                               cds: CitizenDetailsService)
                              (implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    //Add authorisation parameters
    authorised().
      retrieve(Retrievals.nino and Retrievals.confidenceLevel) {
        case Some(nino) ~ confidenceLevel => {
          //Todo: We can avoid a call to citizen details if we remove the need to log gender
          //Todo: Is ITMP Address more or less likely than Citizen Details to have the data?

          cds.retrievePerson(Nino(nino)).flatMap {
            case Some(cdr) => {
              block(AuthenticatedRequest(request,
                NispAuthedUser(Nino(nino),
                  confidenceLevel,
                  cdr.person.dateOfBirth,
                  UserName(Name(cdr.person.firstName, cdr.person.lastName)),
                  cdr.address,
                  cdr.person.sex)))
            }
            case None => throw new InternalServerException("")
          }

        }
        case _ => throw new RuntimeException("Can't find credentials for user")
      } recover {
      case t: NoActiveSession => Redirect(ApplicationConfig.ggSignInUrl, Map("continue" -> Seq(ApplicationConfig.postSignInRedirectUrl),
        "origin" -> Seq("nisp-frontend"), "accountType" -> Seq("individual")))
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

//object AuthAction extends AuthActionImpl(NispAuthConnector, new CitizenDetailsServiceImpl)

//object AuthConnector extends PlayAuthConnector with ServicesConfig {
//  override val serviceUrl: String = baseUrl("auth")
//
//  override def http: CorePost = WSHttp
//
//  override protected def mode: Mode = Play.current.mode
//
//  override protected def runModeConfiguration: Configuration = Play.current.configuration
//}

class NispAuthConnector @Inject()(val http: WSHttp, configuration: Configuration) extends PlayAuthConnector {

  val host = configuration.getString("microservice.services.auth.host").get
  val port = configuration.getString("microservice.services.auth.port").get

  override val serviceUrl: String = s"http://$host:$port"

}