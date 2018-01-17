//import io.circe.Json
//import model._
//import io.circe.syntax._
//import java.sql.Timestamp
//import akka.http.scaladsl.model.StatusCodes._
//import akka.http.scaladsl.server.Directives._
//import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
//import io.circe.Json
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import model._

trait ApiRoutes extends FailFastCirceSupport {
  val route = {
    pathPrefix(Segment) { str: String =>
      pathEndOrSingleSlash {
        str match {
          case "get_token" =>
            post {
              entity(as[Json]) { json =>
                val login = json.hcursor.downField("login").as[String]
                val password = json.hcursor.downField("password").as[String]
                print(login)
                print(password)
                onComplete {
                  Repositories.generateToken(login.getOrElse("#"), password.getOrElse("#"))
                } {
                  case util.Success(value) => {
                    complete("Token $value generated")
                  }
                  case util.Failure(ex) => complete(NotFound)
                }
              }
            }

        }
      } ~ {
        pathPrefix(Segment) { str2: String =>
          pathPrefix(IntNumber) { id: Int =>
            pathPrefix(Segment) { token: String =>
              (str, str2, token) match {
                case ("wallet", "balance", _) =>
                  get {
                    println(str.concat(str2))
                    onComplete(Repositories.verifyUserQuery(token)) {
                      case util.Success(value) => {
                        println(value)
                        if (value == true)
                          onComplete(Repositories.getWalletBalance(token, id)) {
                            case util.Success(value) => {
                              value match {
                                case Some(balance) => complete(s"Balance $id = $value")
                                case None => complete(NotFound)
                              }
                              case util.Failure(ex)
                              => complete(NotFound)
                            }
                            else complete(BadRequest)
                          }
                        case util.Failure(_)
                        => complete(BadRequest)
                      }
                    }
                  }
              }
            }
          }

        }
      }
    }

  }
