package lt.indrasius.nbuilder.live

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.InstallEnvironment
import org.scalatest.matchers.MustMatchers
import org.scalatest.{FlatSpec, Ignore}

/**
 * Created by mantas on 14.12.24.
 */
@Ignore
class PHPIT extends FlatSpec with MustMatchers {
  "ConfigureMakeInstall" should "install the php into the given path" in {
    val projectsDir = TempDirectory.create(true).getAbsolutePath
    val installsDir = TempDirectory.create(true).getAbsolutePath

    InstallEnvironment(projectsDir, installsDir)
      .install("http://lt1.php.net/distributions/php-5.6.4.tar.gz") must be a 'success
  }
}
