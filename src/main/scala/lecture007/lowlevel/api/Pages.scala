package lecture007.lowlevel.api

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCode, StatusCodes }

object Pages {

  def helloPage () = page ( "Hello from Akka HTTP!", StatusCodes.OK )

  def welcomePage () = page ( "Welcome to Akka HTTP!", StatusCodes.OK )

  def aboutPage () = page ( "This is Akka HTTP!", StatusCodes.OK )

  def notFoundPage () = page ( "OOPS! The resource can't be found.", StatusCodes.NotFound )

  private def page ( message: String, statusCode: StatusCode ) = {

    HttpResponse (

      statusCode,
      entity = HttpEntity (
        ContentTypes.`text/html(UTF-8)`,
        s"""
          |<html>
          | <body>
          |   $message
          | </body>
          |</html>
        """.stripMargin
      )
    )
  }
}