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

  /*class RelativeRequest(baseUrl: String, b: RequestBuilder) {
    val req = new Request(b)

    def apply(path: String) = req(Uri(baseUrl) / path)
    def apply(path: String, withAspects: RequestTransformer) = req(Uri(baseUrl) / path, withAspects)
  }*/

  class Request private[http] (b: RequestBuilder, baseUri: Option[Uri] = None) {
    def apply(url: String): HttpResponse =
      pipeline(b(resolve(url)))

    def apply(url: String, withAspects: RequestTransformer): HttpResponse =
      pipeline(b(resolve(url)) ~> withAspects)

    private def resolve(url: String): Uri =
      baseUri map { Uri(url).resolvedAgainst(_) } getOrElse(Uri(url))

    implicit private def await(f: => Future[HttpResponse]): HttpResponse =
      Await.result(f, Duration.Inf)
  }

  class HttpClient private(baseUrl: Option[String] = None) {
    def get = request(Get)

    def request(b: RequestBuilder): Request = new Request(b, baseUrl map { Uri(_) })
  }

  object HttpClient {
    def apply() = new HttpClient()
    def apply(baseUrl: String) = new HttpClient(Some(baseUrl))
  }

  implicit def stringToUri(src: String): Uri =
    Uri(src)

  implicit class RichUri(uri: Uri) {
    def /(path: String) =
      if (uri.path.reverse.startsWithSlash)
        uri.withPath(uri.path + path)
      else
        uri.withPath(uri.path / path)

    /*def resolve(givenUrl: String): Uri =
      Uri(givenUrl) match {
        case givenUri if uri.isAbsolute => givenUri.resolvedAgainst(uri)
        case givenUri => givenUri.resolvedAgainst(uri)
      }*/
  }

  implicit class RequestComposition(source: RequestTransformer) {
    def and(that: RequestTransformer): RequestTransformer = source ~> that
  }
}

