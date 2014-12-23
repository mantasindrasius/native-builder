package lt.indrasius.nbuilder

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.http.HttpClient

import scala.util.Try

/**
 * Created by mantas on 14.12.22.
 */
class ConfigureMakeInstaller(url: String, installDir: String, processFactory: ProcessFactory, makeCommandPath: String) {

  val downloader = new ResourceDownloader(HttpClient(), new FileResourceHandler)
  val unpacker = new TarGzResourceUnpacker

  def build: Try[Unit] =
    for { downloadDir <- createTempDir
          context <- downloader.download(url, downloadDir)
          projectDir <- createTempDir
          _ <- unpacker.unpack(context, projectDir)
          _ <- build(projectDir) }
      yield ()

  private def build(projectDir: String): Try[Unit] =
    for { _ <- configure(projectDir)
          _ <- make(projectDir)
          _ <- install(projectDir) }
      yield ()

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
