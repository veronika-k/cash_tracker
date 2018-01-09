import io.circe.Json
import model._
import io.circe.syntax._
import java.sql.Timestamp
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.syntax._

trait ApiRoutes extends FailFastCirceSupport {
  val route = {
    pathPrefix(Segment) { str: String =>
      pathEndOrSingleSlash { str match {
        case "get_token" =>
          post{
            entity(as[Json]) { json =>
              val login = json.hcursor.downField("login").as[String]
              val password = json.hcursor.downField("password").as[String]
              print(login)
              print(password)
              onComplete { Repositories.generateToken(login.getOrElse("#"), password.getOrElse("#"))
              }{
                case util.Success(value) => { print(value); complete("Token $value generated")}
                case util.Failure(ex)    => { print(ex);complete(NotFound)}
              }
            }
          }
      }
    }
  }
}

}
