package lecture007.lowlevel.api.sync

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

object SyncServerApp extends App {

  implicit val system = ActorSystem ( "lecture007" )
  implicit val materializer = ActorMaterializer ()

  import system.dispatcher

  // Handlers for the various requests
  val requestHandler: HttpRequest => HttpResponse = {

    case HttpRequest ( HttpMethods.GET, Uri.Path ( "/hello" ), _, _, _ ) => helloResponse ()

    case request: HttpRequest => notFoundResponse ( request )
  }

  // Start the server and bind the request handler
  val serverBindingFuture = Http ().bindAndHandleSync ( requestHandler, "localhost", 8080 )

  serverBindingFuture.onComplete {

    case Success ( binding ) => {

      println ( s"Server binding successful. ${ binding.localAddress }" )
    }

    case Failure ( ex ) => println ( s"Server binding failed: $ex" )
  }

  def helloResponse () = HttpResponse (

    StatusCodes.OK, // HTTP 200
    entity = HttpEntity (
      ContentTypes.`text/html(UTF-8)`,
      """
        |<html>
        | <body>
        |   Hello from Akka HTTP!
        | </body>
        |</html>
      """.stripMargin
    )
  )

  def notFoundResponse ( request: HttpRequest ) = {

    request.discardEntityBytes ()

    HttpResponse (

      StatusCodes.NotFound, // 404
      entity = HttpEntity (
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          | <body>
          |   OOPS! The resource can't be found.
          | </body>
          |</html>
        """.stripMargin
      )
    )
  }
}