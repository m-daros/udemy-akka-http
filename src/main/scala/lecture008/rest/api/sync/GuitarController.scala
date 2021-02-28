package lecture008.rest.api.sync

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri }
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import spray.json._
import lecture008.rest.api.json.protocol.GuitarStoreJsonProtocol
import lecture008.rest.api.guitardb.GuitarRepository
import lecture008.rest.api.guitardb.commands.{ CreateGuitar, FindAllGuitars, FindGuitar }
import lecture008.rest.api.guitardb.events.GuitarCreated
import lecture008.rest.api.model.Guitar

object GuitarController extends App with GuitarStoreJsonProtocol {

  implicit val actorSystem = ActorSystem ( "lecture008" )
  implicit val actorMaterializer = ActorMaterializer ()
  implicit val defaultTimeout = Timeout ( 2 seconds )

  // TODO Do not use in production
  import actorSystem.dispatcher

  val guitarRepository = actorSystem.actorOf ( Props [ GuitarRepository ], "guitarRepository" )

  val guitarsURI = "/api/v1/guitars"

  val requestHandler: HttpRequest => Future [ HttpResponse ] = {

    case HttpRequest ( HttpMethods.GET, uri @ Uri.Path ( guitarsURI ), _, _, _ ) => {

      // TODO Define id as path param
      /*
        query parameter handling code
       */
      val query = uri.query () // query object <=> Map[String, String]

      if ( query.isEmpty ) {

        val guitarsFuture: Future [ List [ Guitar ] ] = ( guitarRepository ? FindAllGuitars ).mapTo [ List [ Guitar ] ]

        guitarsFuture.map { guitars =>

          HttpResponse (

            entity = HttpEntity (

              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          )
        }
      }
      else {

        // fetch guitar associated to the guitar id
        // localhost:8080/api/guitar?id=45
        getGuitar ( query )
      }
    }

    case HttpRequest ( HttpMethods.POST, Uri.Path ( guitarsURI ), _, entity, _ ) => {

      // Entities are a Source [ByteString]
      val strictEntityFuture = entity.toStrict ( 3 seconds )

      strictEntityFuture.flatMap { strictEntity =>

        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo [ Guitar ]
        val guitarCreatedFuture: Future[ GuitarCreated ] = ( guitarRepository ? CreateGuitar ( guitar ) ).mapTo [ GuitarCreated ]

        guitarCreatedFuture.map {

          _ => HttpResponse ( StatusCodes.OK )
        }
      }
    }

    case request: HttpRequest => {

      request.discardEntityBytes ()

      Future {

        HttpResponse ( status = StatusCodes.NotFound )
      }
    }
  }

  initGuitarDB ( guitarRepository )
  Http ().bindAndHandleAsync ( requestHandler, "localhost", 8080 )

  def getGuitar ( query: Query ): Future [ HttpResponse ] = {

    val guitarId = query.get ( "id" ).map ( _.toInt ) // Option[Int]

    guitarId match {

      case None => Future ( HttpResponse ( StatusCodes.NotFound ) ) // /api/guitar?id=

      case Some ( id: Int ) =>

        val guitarFuture: Future [ Option[ Guitar ] ] = ( guitarRepository ? FindGuitar ( id ) ).mapTo [ Option [ Guitar ] ]

        guitarFuture.map {

          case None => HttpResponse ( StatusCodes.NotFound ) // /api/guitar?id=9000

          case Some ( guitar ) => {

            HttpResponse (

              entity = HttpEntity (

                ContentTypes.`application/json`,
                guitar.toJson.prettyPrint
              )
            )
          }
        }
    }
  }

  def initGuitarDB ( guitarRepository: ActorRef ): Unit = {

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