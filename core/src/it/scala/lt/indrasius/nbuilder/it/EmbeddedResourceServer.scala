package lt.indrasius.nbuilder.it

import spray.http._

import spray.http.HttpMethods._
import spray.http.StatusCodes._

/**
 * Created by mantas on 14.12.23.
 */
object EmbeddedResourceServer extends EmbeddedServer(EmbeddedConfig.RESOURCE_PORT) {
  start

  def receive: RequestHandler = {
    case HttpRequest(GET, uri, _, _, _) if uri.path.startsWith(Uri.Path("/hello")) =>
      HttpResponse(OK, HttpEntity("world"))
  }

  def apply() = ()
}

trait EmbeddedResourceServer {
  EmbeddedResourceServer()
}