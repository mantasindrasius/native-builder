package lt.indrasius.nbuilder.it

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.{ProcessFactory, RealProcessFactory, InstallEnvironment}
import lt.indrasius.nbuilder.embedded.{ArtifactStorage, ConfigureMakeProject, E2E}
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import scala.sys.process.Process
import scala.util.Success
import scala.collection.convert.wrapAsJava.setAsJavaSet

/**
 * Created by mantas on 14.12.24.
 */
class InstallEnvironmentIT extends FlatSpec with MustMatchers with E2E {
  val project = ConfigureMakeProject()
    .withSuccessLines("cool-lib")
    .withOutputName("helloworld")

  val artifactUrl = ArtifactStorage.addArtifactFromProject("cool-project", project)

  object SandboxProcessFactory extends ProcessFactory {
    val tempPath = TempDirectory.create(true).getAbsolutePath
    val makePath = Paths.get(tempPath, "make")

    {
      val makeBody =
        """#!/bin/sh
          |
          |value=`cat output.txt`
          |dir=`echo $value | cut -d '.' -f 1`
          |mkdir -p $dir
          |touch "$value"
        """.stripMargin

      Files.write(makePath, makeBody.getBytes())
      Files.setPosixFilePermissions(makePath,
        Set(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
          PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
          PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE))

      println("Make executable created")
    }

    def run(cmd: String, cwd: String): Process =
      RealProcessFactory.run(rewriteCmd(cmd), cwd)

    def rewriteCmd(cmd: String): String =
      if (cmd.startsWith("make"))
        cmd.replace("make", makePath.toString)
      else
        cmd
  }

  "InstallEnvironment" should "install and artifact and return path" in {
    val givenProjectsDir = TempDirectory.create(true).getAbsolutePath
    val givenInstallsDir = TempDirectory.create(true).getAbsolutePath

    val env = new InstallEnvironment(SandboxProcessFactory)(givenProjectsDir, givenInstallsDir)

    env.install(artifactUrl) must be (Success(givenInstallsDir + "/cool-project"))
  }
}
