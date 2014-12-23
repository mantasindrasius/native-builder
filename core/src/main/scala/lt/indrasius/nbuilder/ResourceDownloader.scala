package lt.indrasius.nbuilder

import java.nio.file.Paths

import lt.indrasius.nbuilder.http.HttpClient
import spray.http.{Uri, HttpResponse, StatusCodes}

import scala.util.{Failure, Try}

/**
 * Created by mantas on 14.12.23.
 */
class ResourceDownloader(httpClient: HttpClient, resourceHandler: ResourceHandler) {
  def download(uri: String, targetDir: String): Try[ResourceContext] = {
    httpClient.get(uri) match {
      case HttpResponse(StatusCodes.OK, entity, _, _) =>
        val filename = Uri(uri).path.reverse.head.toString

        resourceHandler.save(Paths.get(targetDir, filename).toString, entity.data.toByteArray)
      case HttpResponse(_, entity, _, _) =>
        Failure(DownloadFailed(uri))
    }
  }
}

case class DownloadFailed(uri: String) extends Exception("Download failed: " + uri)