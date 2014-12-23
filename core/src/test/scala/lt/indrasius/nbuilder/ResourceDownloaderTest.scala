package lt.indrasius.nbuilder

import lt.indrasius.nbuilder.http.{Request, HttpClient}
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import spray.http.{HttpEntity, StatusCodes, HttpResponse, Uri}
import scala.util.Success

/**
 * Created by mantas on 14.12.23.
 */
class ResourceDownloaderTest extends FlatSpec with MustMatchers with MockitoSugar {
  def withDownloaderContext(testCode: (ResourceDownloader, Request, ResourceHandler) => Any) = {
    val httpClient = mock[HttpClient]
    val request = mock[Request]
    val resourceHandler = mock[ResourceHandler]

    when(httpClient.get).thenReturn(request)

    val downloader = new ResourceDownloader(httpClient, resourceHandler)

    testCode(downloader, request, resourceHandler)
  }

  "ResourceDownloader" should "download the resource" in withDownloaderContext { (downloader, request, resourceHandler) =>
    val givenUri = "http://test/lib/some.tar.gz"
    val givenTargetDir = "/target/dir"
    val givenBytes = "xyz".getBytes
    val givenResponse = HttpResponse(StatusCodes.OK, HttpEntity(givenBytes))
    val targetFile = givenTargetDir + "/some.tar.gz"

    val resourceContext = mock[ResourceContext]

    when(request.apply(givenUri)).thenReturn(givenResponse)
    when(resourceHandler.save(targetFile, givenBytes)).thenReturn(Success(resourceContext))

    downloader.download(givenUri, givenTargetDir) must be 'success

    verify(request).apply(givenUri)
    verify(resourceHandler).save(targetFile, givenBytes)
  }

  "ResourceDownloader" should "fail to download the resource" in withDownloaderContext { (downloader, request, resourceHandler) =>
    val givenUri = "http://test/lib/some.tar.gz"
    val givenTargetDir = "/target/dir"
    val givenResponse = HttpResponse(StatusCodes.NotFound)

    when(request.apply(givenUri)).thenReturn(givenResponse)

    downloader.download(givenUri, givenTargetDir) must be a 'failure

    verify(request).apply(givenUri)
    verify(resourceHandler, never()).save(any(), any())
  }
}
