package lt.indrasius.nbuilder.it

import java.io.ByteArrayInputStream

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.{ResourceContext, TarGzResourceUnpacker}
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

import scala.util.Success

/**
 * Created by mantas on 14.12.23.
 */
class TarGzResourceUnpackerIT extends FlatSpec with MustMatchers with MockitoSugar {
  def withWellFormedContext(testCode: (ResourceContext, String) => Any) = {
    val context = mock[ResourceContext]
    val tempDir = TempDirectory.create(true)

    val tarBytes = TarGzArchiveBuilder()
      .addFile("test", "hello")
      .addDir("xyz")
      .addFile("xyz/hello", "world")
      .build

    val in = new ByteArrayInputStream(tarBytes)

    when(context.openRead).thenReturn(Success(in))

    testCode(context, tempDir.getAbsolutePath)
  }

  "TarGzResourceUnpacker" should "unpack from a tar.gz file" in withWellFormedContext { (context, targetDir) =>
    new TarGzResourceUnpacker().unpack(context, targetDir) must be a 'success
  }
}
