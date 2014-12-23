package lt.indrasius.nbuilder

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse, Uri}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Created by mantas on 14.12.22.
 */
package object http {
  implicit val actorSystem = ActorSystem()
  import scala.concurrent.ExecutionContext.Implicits.global

  private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  //private val waitFor = Duration(2, TimeUnit.SECONDS)

  class RelativeRequest(baseUrl: String, b: RequestBuilder) {
    val req = new Request(b)

    def apply(path: String) = req(Uri(baseUrl) / path)
    def apply(path: String, withAspects: RequestTransformer) = req(Uri(baseUrl) / path, withAspects)
  }

  class Request(b: RequestBuilder) {
    def apply(url: Uri): HttpResponse =
      pipeline(b(url))

    def apply(url: Uri, withAspects: RequestTransformer): HttpResponse =
      pipeline(b(url) ~> withAspects)

    implicit private def await(f: => Future[HttpResponse]): HttpResponse =
      Await.result(f, Duration.Inf)
  }

  case class HttpClient(baseUrl: String) {
    private val url = Uri(baseUrl)

    def get = new RelativeRequest(baseUrl, Get)
  }

  case object HttpClient {
    def get = new Request(Get)
  }

  implicit def stringToUri(src: String): Uri =
    Uri(src)

  implicit class RichUri(uri: Uri) {
    def /(path: String) =
      if (uri.path.reverse.startsWithSlash)
        uri.withPath(uri.path + path)
      else
        uri.withPath(uri.path / path)
  }

  implicit class RequestComposition(source: RequestTransformer) {
    def and(that: RequestTransformer): RequestTransformer = source ~> that
  }
}

