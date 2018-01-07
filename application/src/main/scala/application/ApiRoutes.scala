import Repositories._
import akka.http.scaladsl.model.Uri.Path.Segment
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import io.circe.Json
import io.circe.syntax._

trait ApiRoutes extends FailFastCirceSupport {
  val route = pathEndOrSingleSlash {
    get {
      complete("hello")
    }
  }
}
