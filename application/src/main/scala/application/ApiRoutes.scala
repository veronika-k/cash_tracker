import akka.http.scaladsl.model.Uri.Path.Segment
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import akka.http.scaladsl.model.StatusCodes._
import model._
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
              onComplete { //Repositories.getUserId(login:String, password: String)
              }{
                case util.Success(value) => co/mplete("Token enerated")
                case util.Failure(ex)    => complete(NotFound)
              }
            }
          }
      }
    }
  }
}

}
