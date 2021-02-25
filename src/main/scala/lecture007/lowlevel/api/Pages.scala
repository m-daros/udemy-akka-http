package lecture007.lowlevel.api

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes }

object Pages {

  def helloPage () = HttpResponse (

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

  def notFoundPage ( request: HttpRequest ) = {

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