package lt.indrasius.nbuilder.it

import lt.indrasius.nbuilder.embedded.{EmbeddedResourceServer, EmbeddedConfig}
import lt.indrasius.nbuilder.http.HttpClient
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.MustMatchers
import spray.http.{HttpEntity, StatusCodes}

/**
 * Created by mantas on 14.12.23.
 */
class HttpClientIT extends FlatSpec with MustMatchers with BeforeAndAfterAll {
  override def beforeAll = {
    EmbeddedResourceServer.start must be a 'success
  }

  override def afterAll = {
    EmbeddedResourceServer.stop
  }

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
