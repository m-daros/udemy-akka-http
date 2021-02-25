package lecture007.lowlevel.api.async

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.stream.ActorMaterializer
import lecture007.lowlevel.api.Pages

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object AsyncServerApp extends App {

  implicit val system = ActorSystem ( "lecture007" )
  implicit val materializer = ActorMaterializer ()

  import system.dispatcher

  // Handlers for the various requests
  val asyncRequestHandler: HttpRequest => Future [HttpResponse] = {

    case HttpRequest ( HttpMethods.GET, Uri.Path ( "/hello" ), _, _, _ ) => Future ( Pages.helloPage () )

    case request: HttpRequest => {

      request.discardEntityBytes ()

      Future ( Pages.notFoundPage ( request ) )
    }
  }

  // Start the server and bind the request handler
  val serverBindingFuture = Http ().bindAndHandleAsync ( asyncRequestHandler, "localhost", 8080 )

  serverBindingFuture.onComplete {

    case Success ( binding ) => {

      println ( s"Server binding successful. ${ binding.localAddress }" )
    }

    case Failure ( ex ) => println ( s"Server binding failed: $ex" )
  }
}