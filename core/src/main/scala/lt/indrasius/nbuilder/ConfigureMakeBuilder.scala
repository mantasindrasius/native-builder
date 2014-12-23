package lt.indrasius.nbuilder

import java.io.{ByteArrayInputStream, EOFException, File, FileOutputStream}
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
class ConfigureMakeBuilder(url: String, installDir: String, processFactory: ProcessFactory, makeCommandPath: String) {

  private val permissions = Map(
    0400 -> PosixFilePermission.OWNER_READ,
    0200 -> PosixFilePermission.OWNER_WRITE,
    0100 -> PosixFilePermission.OWNER_EXECUTE,
    040 -> PosixFilePermission.GROUP_READ,
    020 -> PosixFilePermission.GROUP_WRITE,
    010 -> PosixFilePermission.GROUP_EXECUTE,
    04 -> PosixFilePermission.OTHERS_READ,
    02 -> PosixFilePermission.OTHERS_WRITE,
    01 -> PosixFilePermission.OTHERS_EXECUTE
  )

  def build: Try[Unit] =
    HttpClient.get(url) match {
      case HttpResponse(StatusCodes.OK, entity, _, _) =>
        buildWith(entity.data.toByteArray)
    }

  private def buildWith(bytes: Array[Byte]): Try[Unit] =
    for { dir <- unpack(bytes)
          _ <- run(dir) }
      yield ()

  private def unpack(bytes: Array[Byte]): Try[String] = {
    val inp = new ByteArrayInputStream(bytes)
    val gzipIn = new GZIPInputStream(inp)
    val tarIn = new TarInputStream(gzipIn)

    val projectDir = TempDirectory.create(true)

    @tailrec def readEntry: Try[Unit] =
      tarIn.next match {
        case Success(null) | Failure(_: EOFException) =>
          println("eof")

          Success()
        case Success(entry) =>
          println(entry.getName)

          val file = new File(projectDir, entry.getName)
          val filepath = file.getAbsolutePath

          tarIn.copyToFile(filepath, entry.getSize)

          val perms = convertToPermissionsSet(entry.getHeader.mode)
          Files.setPosixFilePermissions(Paths.get(filepath), perms)

          println(s"Setting $perms")

          readEntry
        case Failure(e) =>
          println(e)

          Failure(e)
      }

    readEntry map { _ => projectDir.getAbsolutePath }
  }

  def convertToPermissionsSet(mode: Int): Set[PosixFilePermission] = {
    val result = Set[PosixFilePermission]()

    permissions filter { kv => (mode & kv._1) > 0 } map { _._2 } toSet
  }

  private def run(dir: String): Try[Unit] =
    for { _ <- configure(dir)
          _ <- make(dir)
          _ <- install(dir) }
      yield ()

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

  private implicit class RichTarInputStream(in: TarInputStream) {
    private val chunkSize = 32768

    def next: Try[TarEntry] = Try {
      in.getNextEntry
    }

    def copyToFile(filepath: String, length: Long): Try[Unit] = {
      val buffer = new Array[Byte](chunkSize)
      val out = new FileOutputStream(filepath)

      @tailrec def write(bytesLeft: Int): Try[Unit] =
        Try { in.read(buffer, 0, Math.min(buffer.size, bytesLeft)) } match {
          case Failure(e) => Failure(e)
          case Success(-1) => Success()
          case Success(n) =>
            out.write(buffer, 0, n)

            write(bytesLeft - n)
        }

      try write(length.toInt)
      finally out.close()
    }
  }
}
