package lecture008.rest.api.guitardb

import akka.actor.{ Actor, ActorLogging }
import lecture008.rest.api.guitardb.commands.{ CreateGuitar, FindAllGuitars, FindGuitar }
import lecture008.rest.api.guitardb.events.GuitarCreated
import lecture008.rest.api.model.Guitar

class GuitarRepository extends Actor with ActorLogging {

  var guitars: Map [ Int, Guitar ] = Map ()
  var currentGuitarId: Int = 0

  override def receive: Receive = {

    case FindAllGuitars => {

      log.info ( "Searching for all guitars" )
      sender () ! guitars.values.toList
    }

    case FindGuitar ( id ) => {

      log.info ( s"Searching guitar by id: $id" )
      sender () ! guitars.get ( id )
    }

    case CreateGuitar ( guitar ) => {

      val entity = guitar.copy ( id = currentGuitarId )
      log.info ( s"Adding guitar $entity with id $currentGuitarId" )
      guitars = guitars + ( currentGuitarId -> entity )
      sender () ! GuitarCreated ( currentGuitarId )
      currentGuitarId += 1
    }
  }
}