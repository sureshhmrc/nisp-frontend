package uk.gov.hmrc.nisp.helpers


import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.nisp.auth.{NispAuthProvider, NispCompositePageVisibilityPredicate, VerifyProvider}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthorisedForNisp, NispUser}
import uk.gov.hmrc.nisp.exceptions.EmptyPayeException
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, AuthenticationProvider, Principal, TaxRegime}

import scala.concurrent.Future

trait MockAuthorisedForNisp extends AuthorisedForNisp {
  val citizenDetailsService: CitizenDetailsService
  val applicationConfig: ApplicationConfig

  private type PlayRequest = Request[AnyContent] => Result
  private type UserRequest = NispUser => PlayRequest
  private type AsyncPlayRequest = Request[AnyContent] => Future[Result]
  private type AsyncUserRequest = NispUser => AsyncPlayRequest

  implicit private def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  class AuthorisedBy(regime: TaxRegime) {

    def async(action: AsyncUserRequest): Action[AnyContent] = {
      val authContext: AuthContext = ???
      action(NispUser(
        authContext = authContext,
        name = citizen.flatMap(_.person.getNameFormatted),
        authProvider = request.session.get(SessionKeys.authProvider).getOrElse(""),
        sex = citizen.flatMap(_.person.sex),
        dateOfBirth = citizen.map(_.person.dateOfBirth),
        address = citizen.flatMap(_.address)
      ))(request)
    }

    def apply(action: UserRequest): Action[AnyContent] = async(user => request => Future.successful(action(user)(request)))
  }

  object AuthorisedByAny extends AuthorisedBy(NispAnyRegime)

  object AuthorisedByVerify extends AuthorisedBy(NispVerifyRegime)

  def retrievePerson(authContext: AuthContext)(implicit request: Request[AnyContent]): Future[Option[CitizenDetailsResponse]] =
    citizenDetailsService.retrievePerson(retrieveNino(authContext.principal))

  private def retrieveNino(principal: Principal): Nino = {
    principal.accounts.paye match {
      case Some(account) => account.nino
      case None => throw new EmptyPayeException("PAYE Account is empty")
    }
  }

  trait NispRegime extends TaxRegime {
    override def isAuthorised(accounts: Accounts): Boolean = true

    override def authenticationType: AuthenticationProvider = NispAuthProvider
  }

  object NispAnyRegime extends NispRegime

  object NispVerifyRegime extends NispRegime {
    override def authenticationType: AuthenticationProvider = VerifyProvider
  }

  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String = {
    if (confidenceLevel.level == 500) Constants.verify else Constants.iv
  }


}
