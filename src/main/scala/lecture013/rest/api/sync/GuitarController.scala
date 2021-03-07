package lecture013.rest.api.sync

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives.{ entity, extractRequestEntity, _ }
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import spray.json._
import lecture013.rest.api.json.protocol.GuitarStoreJsonProtocol
import lecture013.rest.api.guitardb.GuitarRepository
import lecture013.rest.api.guitardb.commands.{ CreateGuitar, FindAllGuitars, FindGuitar }
import lecture013.rest.api.model.Guitar
import lecture013.rest.api.guitardb.events.GuitarCreated

import scala.concurrent.Future

object GuitarController extends App with GuitarStoreJsonProtocol {

  implicit val actorSystem = ActorSystem ( "lecture008" )
  implicit val actorMaterializer = ActorMaterializer ()
  implicit val defaultTimeout = Timeout ( 2 seconds )

  // TODO Do not use in production
  import actorSystem.dispatcher

  val guitarRepository = actorSystem.actorOf ( Props [ GuitarRepository ], "guitarRepository" )

  val routes = ( get & path ( "api" / "v1" / "guitars" ) ) {

    val guitarsFuture: Future [ List [ Guitar ] ] = ( guitarRepository ? FindAllGuitars ).mapTo [ List [ Guitar ] ]

    val response = guitarsFuture.map { guitars =>

        HttpEntity ( ContentTypes.`application/json`, guitars.toJson.prettyPrint )
    }

    complete ( response )
  } ~
  ( post & path ( "api" / "v1" / "guitars" ) & extractRequestEntity ) { entity =>


    // Entities are a Source [ByteString]
    val strictEntityFuture = entity.toStrict ( 3 seconds )

    val response = strictEntityFuture.flatMap { strictEntity =>

      val guitarJsonString = strictEntity.data.utf8String
      val guitar = guitarJsonString.parseJson.convertTo [ Guitar ]
      val guitarCreatedFuture: Future [ GuitarCreated ] = ( guitarRepository ? CreateGuitar ( guitar ) ).mapTo [ GuitarCreated ]

      guitarCreatedFuture.map {

        _ => HttpEntity ( ContentTypes.`application/json`, "" )
      }
    }

    complete ( response )
  } ~
  ( get & path ( "api" / "v1" / "guitars" / IntNumber ) ) { guitarId: Int =>

    val guitarFuture = getGuitar ( guitarId )
    val guitarEntity = guitarFuture.map { guitarOption =>

      HttpEntity ( ContentTypes.`application/json`, guitarOption.toJson.prettyPrint )
    }

    complete ( guitarEntity )
  }

  initGuitarDB ( guitarRepository )
  Http ().bindAndHandle ( routes, "localhost", 8080 )

  private def getGuitar ( id: Int ) = {

    ( guitarRepository ? FindGuitar ( id ) )
      .mapTo [ Option [ Guitar ] ]
  }

  private def initGuitarDB ( guitarRepository: ActorRef ): Unit = {

    val guitarList = List (

      Guitar ( -1, "Fender", "Stratocaster" ),
      Guitar ( -1, "Gibson", "Les Paul" ),
      Guitar ( -1, "Martin", "LX1" )
    )

    guitarList.foreach { guitar =>

      guitarRepository ! CreateGuitar ( guitar )
    }
  }
}