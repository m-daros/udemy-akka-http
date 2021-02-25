package lecture007

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import lecture007.lowlevel.api.Pages
import scala.util.{ Failure, Success }

object Exercise extends App {

  implicit val system = ActorSystem ( "lecture007" )
  implicit val materializer = ActorMaterializer ()

  import system.dispatcher

  // Handlers for the various requests
  val streamRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow [HttpRequest].map {

    case HttpRequest ( HttpMethods.GET, Uri.Path ( "/" ), _, _, _ ) => Pages.welcomePage ()

    case HttpRequest ( HttpMethods.GET, Uri.Path ( "/about" ), _, _, _ ) => Pages.aboutPage ()

    case request: HttpRequest => {

      request.discardEntityBytes ()
      Pages.notFoundPage ()
    }
  }
  // Start the server and bind the request handler
  val serverBindingFuture = Http ().bindAndHandle ( streamRequestHandler, "localhost", 8388 )
  serverBindingFuture.onComplete {

    case Success ( binding ) => {
      println ( s"Server binding successful. ${ binding.localAddress }" )
    }

    case Failure ( ex ) => println ( s"Server binding failed: $ex" )
  }
}
