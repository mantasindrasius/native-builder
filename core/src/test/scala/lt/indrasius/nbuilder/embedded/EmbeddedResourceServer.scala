package lt.indrasius.nbuilder.embedded

import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

/**
 * Created by mantas on 14.12.23.
 */
object EmbeddedResourceServer extends EmbeddedServer(EmbeddedConfig.RESOURCE_PORT) {
  def receive: RequestHandler = {
    case HttpRequest(GET, uri, _, _, _) if uri.path.startsWith(Uri.Path("/hello")) =>
      HttpResponse(OK, HttpEntity("world"))
  }

  def apply() = ()
}
