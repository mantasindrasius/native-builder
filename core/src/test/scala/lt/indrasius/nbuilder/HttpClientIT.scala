package lt.indrasius.nbuilder

import lt.indrasius.nbuilder.http.HttpClient
import lt.indrasius.nbuilder.it.{EmbeddedConfig, EmbeddedResourceServer}
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers
import spray.http.{HttpEntity, StatusCodes}

/**
 * Created by mantas on 14.12.23.
 */
class HttpClientIT extends FlatSpec with MustMatchers with EmbeddedResourceServer {
  def withHttpClient(testCode: HttpClient => Any): Unit = {
    val baseUrl = s"http://localhost:${EmbeddedConfig.RESOURCE_PORT}"
    val httpClient = HttpClient(baseUrl)

    testCode(httpClient)
  }

  "HttpClient" should "process a 200 response" in withHttpClient { httpClient =>
    httpClient.get("/hello") must have (
      'status (StatusCodes.OK),
      'entity (HttpEntity("world"))
    )
  }

  it should "process a 404 response" in withHttpClient { httpClient =>
    httpClient.get("/not-found-xyz") must have (
      'status (StatusCodes.NotFound)
    )
  }
}
