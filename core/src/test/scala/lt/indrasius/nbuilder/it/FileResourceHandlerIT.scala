package lt.indrasius.nbuilder.it

import java.io.File
import java.nio.file.{Paths, Files}

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.FileResourceHandler
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

/**
 * Created by mantas on 14.12.23.
 */
class FileResourceHandlerIT extends FlatSpec with MustMatchers {
  "FileResourceHandler" should "save to file" in {
    val temp = TempDirectory.create(true)
    val givenTempFile = new File(temp, "testFile")
    val givenContent = "abc".getBytes

    new FileResourceHandler().save(givenTempFile.getAbsolutePath, givenContent) must be a 'success

    Files.readAllBytes(Paths.get(givenTempFile.getAbsolutePath)) must be (givenContent)
  }

  it should "fail to save into an unexisting dir" in {
    val givenTempFile = new File("/xyz/dir/not-exists/testFile")
    val givenContent = "abc".getBytes

    new FileResourceHandler().save(givenTempFile.getAbsolutePath, givenContent) must be a 'failure

    givenTempFile.exists() must be (false)
  }
}
