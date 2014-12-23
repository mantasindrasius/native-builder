package lt.indrasius.nbuilder

import java.io._
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}
import java.util.zip.GZIPInputStream

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.http.HttpClient
import org.kamranzafar.jtar.{TarEntry, TarInputStream}
import spray.http.{HttpResponse, StatusCodes}

import scala.annotation.tailrec
import scala.collection.convert.wrapAsJava.setAsJavaSet
import scala.util.{Failure, Success, Try}

/**
 * Created by mantas on 14.12.22.
 */
class ConfigureMakeBuilder(httpClient: HttpClient, url: String, installDir: String, processFactory: ProcessFactory, makeCommandPath: String) {

  val downloader = new ResourceDownloader(httpClient, new FileResourceHandler)
  val unpacker = new TarGzResourceUnpacker
  //val downloadHandler = SavingToTempStreamHandler(httpClient)

  def build: Try[Unit] =
    for { downloadDir <- createTempDir
          context <- downloader.download(url, downloadDir)
          projectDir <- createTempDir
          _ <- unpacker.unpack(context, projectDir)
          _ <- configure(projectDir)
          _ <- make(projectDir)
          _ <- install(projectDir) }
      yield ()

  def createTempDir: Try[String] =
    Try { TempDirectory.create(true) } map { _.getAbsolutePath }

  private def configure(dir: String): Try[Unit] = {
    runProc("./configure --prefix=" + installDir, dir)
    runProc("ls -l", dir)
  }
  private def make(dir: String): Try[Unit] = runProc(makeCommandPath, dir)
  private def install(dir: String): Try[Unit] = runProc(makeCommandPath + " install", dir)

  private def runProc(cmd: String, dir: String): Try[Unit] = Try {
    processFactory.run(cmd, dir).exitValue() match {
      case 0 => ()
      case exitCode => throw new IllegalStateException(s"$cmd exited with $exitCode")
    }
  }
}
