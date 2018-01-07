
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.io.StdIn

object WebServer extends ApiRoutes {

  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    //    val bindingFuture = Http().bindAndHandle(route, "travels.com", 80)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}
