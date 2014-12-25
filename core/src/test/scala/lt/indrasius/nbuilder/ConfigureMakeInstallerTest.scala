package lt.indrasius.nbuilder

import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar

import scala.util.Success

/**
 * Created by mantas on 14.12.22.
 */
class ConfigureMakeInstallerTest extends FlatSpec with MustMatchers with MockitoSugar {
  def withSuccessProject(testCode: (ConfigureMakeInstaller, ProcessFactory, String) => Any) {
    val installDir = "/var/install"
    val processFactory = mock[ProcessFactory]

    testCode(new ConfigureMakeInstaller(installDir, processFactory), processFactory, installDir)
  }

  "ConfigureMakeBuilder" should "build a simple project successfully" in withSuccessProject { (builder, processFactory, installDir) =>
    val givenProjectDir = "/project/dir"

    when(processFactory.runProc("./configure --prefix=" + installDir, givenProjectDir)).thenReturn(Success())
    when(processFactory.runProc("make", givenProjectDir)).thenReturn(Success())
    when(processFactory.runProc("make install", givenProjectDir)).thenReturn(Success())

    builder.install(givenProjectDir) must be a 'success

    verify(processFactory).runProc("./configure --prefix=" + installDir, givenProjectDir)
    verify(processFactory).runProc("make", givenProjectDir)
    verify(processFactory).runProc("make install", givenProjectDir)
  }
}
