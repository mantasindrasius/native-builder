package lt.indrasius.nbuilder.live

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.InstallEnvironment
import org.scalatest.matchers.MustMatchers
import org.scalatest.{FlatSpec, Ignore}

/**
 * Created by mantas on 14.12.24.
 */
@Ignore
class BisonIT extends FlatSpec with MustMatchers {
  "ConfigureMakeInstall" should "install the bison library into the given path" in {
    val projectsDir = TempDirectory.create(true).getAbsolutePath
    val installsDir = TempDirectory.create(true).getAbsolutePath

    InstallEnvironment(projectsDir, installsDir)
      .install("http://ftp.gnu.org/gnu/bison/bison-2.7.tar.gz") must be a 'success
  }
}
