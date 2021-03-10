package lecture013.rest.api.sync

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import lecture013.rest.api.guitardb.GuitarRepository
import lecture013.rest.api.guitardb.commands.{ CreateGuitar, FindAllGuitars, FindGuitar }
import lecture013.rest.api.guitardb.events.GuitarCreated
import lecture013.rest.api.json.protocol.GuitarStoreJsonProtocol
import lecture013.rest.api.model.Guitar

import scala.concurrent.duration._

import spray.json._

object GuitarController extends App with GuitarStoreJsonProtocol {

  implicit val actorSystem = ActorSystem ( "lecture008" )
  implicit val actorMaterializer = ActorMaterializer ()
  implicit val defaultTimeout = Timeout ( 2 seconds )

  // TODO Do not use in production
  import actorSystem.dispatcher

  val guitarRepository = actorSystem.actorOf ( Props [ GuitarRepository ], "guitarRepository" )

  val routes =

  // GET /api/v1/guitars
  ( get & path ( "api" / "v1" / "guitars" ) ) {

    var guitarEntity = ( guitarRepository ? FindAllGuitars )
      .mapTo [ List [ Guitar ] ]
      .map ( _.toJson.prettyPrint )
      .map ( toHttpEntity )

    complete ( guitarEntity )
  } ~
  //
  //
  // POST /api/v1/guitars
  ( post & path ( "api" / "v1" / "guitars" ) & extractRequestEntity ) { entity =>

    // Entities are a Source [ByteString]
    val strictEntityFuture = entity.toStrict ( 3 seconds )

    val guitarEntity = strictEntityFuture.flatMap { strictEntity =>

      val guitarJsonString = strictEntity.data.utf8String
      val guitar = guitarJsonString.parseJson.convertTo [ Guitar ]

      ( guitarRepository ? CreateGuitar ( guitar ) )
        .mapTo [ GuitarCreated ]
        .map ( _ => guitar )
        .map ( _.toJson.prettyPrint )
        .map ( toHttpEntity )
    }

    complete ( guitarEntity )
  } ~
  //
  //
  // GET /api/v1/guitars/{guitarId}
  ( get & path ( "api" / "v1" / "guitars" / IntNumber ) ) { guitarId: Int =>

    val guitarEntity = getGuitar ( guitarId )
      .map ( _.toJson.prettyPrint )
      .map ( toHttpEntity )

    complete ( guitarEntity )
  }

  initGuitarDB ( guitarRepository )
  Http ().bindAndHandle ( routes, "localhost", 8080 )

  private def toHttpEntity ( payload: String ) = {

    HttpEntity ( ContentTypes.`application/json`, payload )
  }

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