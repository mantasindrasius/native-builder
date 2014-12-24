package lt.indrasius.nbuilder

import java.io.File

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.http.HttpClient
import spray.http.Uri

import scala.util.{Success, Failure, Try}

/**
 * Created by mantas on 14.12.22.
 */
class ConfigureMakeInstaller(url: String, installDir: String, processFactory: ProcessFactory, makeCommandPath: String) {

  val downloader = new ResourceDownloader(HttpClient(), new FileResourceHandler)
  val unpacker = new TarGzResourceUnpacker

  def build: Try[Unit] =
    for { downloadDir <- createTempDir
          context <- downloader.download(url, downloadDir)
          explodeDir <- createTempDir
          _ <- unpacker.unpack(context, explodeDir)
          _ <- build(explodeDir) }
      yield ()

  private def build(explodeDir: String): Try[Unit] =
    for { projectDir <- resolveProjectDir(explodeDir)
          _ <- configure(projectDir)
          _ <- make(projectDir)
          _ <- install(projectDir) }
      yield ()

  private def resolveProjectDir(explodeDir: String): Try[String] =
    Option { Uri(url).path.reverse.head.toString } map { _.split("\\.tar", 2)(0) } map { new File(new File(explodeDir), _) } filter { f =>
      println(f.getAbsolutePath)

      f.exists() } map {
      _.getAbsolutePath
    } match {
      case None => Failure(new IllegalStateException(s"Directory not resolved for $url"))
      case Some(projectDir) => Success(projectDir)
    }

  private def configure(dir: String): Try[Unit] =
    runProc("./configure --prefix=" + installDir, dir)

  private def make(dir: String): Try[Unit] =
    runProc(makeCommandPath, dir)

  private def install(dir: String): Try[Unit] =
    runProc(makeCommandPath + " install", dir)

  private def runProc(cmd: String, dir: String): Try[Unit] = Try {
    processFactory.run(cmd, dir).exitValue() match {
      case 0 => ()
      case exitCode => throw new IllegalStateException(s"$cmd exited with $exitCode")
    }
  }

  private def createTempDir: Try[String] =
    Try { TempDirectory.create(true) } map { _.getAbsolutePath }
}
