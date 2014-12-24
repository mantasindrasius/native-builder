package lt.indrasius.nbuilder.live

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.{RealProcessFactory, ConfigureMakeInstaller}
import org.scalatest.{Ignore, FlatSpec}
import org.scalatest.matchers.MustMatchers

/**
 * Created by mantas on 14.12.24.
 */
@Ignore
class BisonIT extends FlatSpec with MustMatchers {
  "ConfigureMakeInstall" should "install the bison library into the given path" in {
    val installDir = TempDirectory.create(true)

    new ConfigureMakeInstaller("http://ftp.gnu.org/gnu/bison/bison-2.7.tar.gz", installDir.getAbsolutePath,
        RealProcessFactory, "make")
      .build must be a 'success
  }
}
