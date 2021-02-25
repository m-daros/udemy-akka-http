package lecture007.lowlevel.api.stream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import lecture007.lowlevel.api.Pages

import scala.util.{ Failure, Success }

object StreamServerApp extends App {

  implicit val system = ActorSystem ( "lecture007" )
  implicit val materializer = ActorMaterializer ()

  import system.dispatcher

  // Handlers for the various requests
  val streamRequestHandler: Flow [HttpRequest, HttpResponse, _] = Flow [HttpRequest].map {

    case HttpRequest ( HttpMethods.GET, Uri.Path ( "/hello" ), _, _, _ ) => Pages.helloPage ()

    case request: HttpRequest => {

      request.discardEntityBytes ()

      Pages.notFoundPage ( request )
    }
  }

  // Start the server and bind the request handler
  val serverBindingFuture = Http ().bindAndHandle ( streamRequestHandler, "localhost", 8080 )

  serverBindingFuture.onComplete {

    case Success ( binding ) => {

      println ( s"Server binding successful. ${ binding.localAddress }" )
    }

    case Failure ( ex ) => println ( s"Server binding failed: $ex" )
  }
}