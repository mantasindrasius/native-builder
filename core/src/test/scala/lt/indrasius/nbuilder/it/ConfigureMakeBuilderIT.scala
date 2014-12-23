package lt.indrasius.nbuilder.it

import java.io.File
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.http.HttpClient
import lt.indrasius.nbuilder.{ConfigureMakeBuilder, ProcessFactory}
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import scala.collection.convert.wrapAsJava.{seqAsJavaList, setAsJavaSet}
import scala.sys.process.Process

/**
 * Created by mantas on 14.12.22.
 */
class ConfigureMakeBuilderIT extends FlatSpec with MustMatchers with E2E {
  object SandboxProcessFactory extends ProcessFactory {
    val sandboxDir = TempDirectory.create(true)

    {
      val makeBody =
        """#!/bin/sh
          |
          |value=`cat output.txt`
          |touch "$value"
        """.stripMargin

      val path = Paths.get(makePath)

      Files.write(path, makeBody.getBytes())
      Files.setPosixFilePermissions(path,
        Set(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE))

      println("Make executable created")
    }

    def run(cmd: String, cwd: String): Process = {
      val pb = new ProcessBuilder(cmd.split("\\s+").toList)
      pb.directory(new File(cwd))

      println(s"Running $cmd in $cwd")

      //Thread.sleep(500)

      Process(pb).run()
    }

    def makePath = Paths.get(sandboxDir.getAbsolutePath, "make").toString
  }

  def withSuccessProject(testCode: (ConfigureMakeBuilder, String) => Any) {
    val project = ConfigureMakeProject()
      .withSuccessLines("cool-lib")
      .withOutputName("helloworld")

    val artifactUrl = ArtifactStorage.addArtifactFromProject("cool-project", project)
    val installDir = TempDirectory.create(true)
    val artifactPath = new File(installDir, "helloworld").getAbsolutePath
    val httpClient = HttpClient()

    testCode(new ConfigureMakeBuilder(httpClient, artifactUrl, installDir.getAbsolutePath, SandboxProcessFactory, SandboxProcessFactory.makePath), artifactPath) // "loan" the fixture to the test
  }

  "ConfigureMakeBuilder" should "build a simple project successfully" in withSuccessProject { (builder, artifactPath) =>
    builder.build must be a 'success

    new File(artifactPath).exists() must be (true)
  }
}
