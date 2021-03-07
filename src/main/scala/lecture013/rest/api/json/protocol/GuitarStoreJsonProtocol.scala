package lecture013.rest.api.json.protocol

import lecture013.rest.api.model.Guitar
import spray.json.DefaultJsonProtocol

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {

  implicit val guitarFormat = jsonFormat3 ( Guitar )
}