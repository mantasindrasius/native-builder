package lt.indrasius.nbuilder

import java.io.{EOFException, File, FileOutputStream, InputStream}
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}
import java.util.zip.GZIPInputStream

import org.kamranzafar.jtar.{TarEntry, TarInputStream}

import scala.annotation.tailrec
import scala.collection.convert.wrapAsJava.setAsJavaSet
import scala.util.{Failure, Success, Try}

/**
 * Created by mantas on 14.12.23.
 */
trait ResourceUnpacker {
  def unpack(context: ResourceContext, targetDir: String): Try[String]
}

class TarGzResourceUnpacker extends ResourceUnpacker {
  private val permissions = Map(
    256 -> PosixFilePermission.OWNER_READ,
    128 -> PosixFilePermission.OWNER_WRITE,
    64 -> PosixFilePermission.OWNER_EXECUTE,
    32 -> PosixFilePermission.GROUP_READ,
    16 -> PosixFilePermission.GROUP_WRITE,
    8 -> PosixFilePermission.GROUP_EXECUTE,
    4 -> PosixFilePermission.OTHERS_READ,
    2 -> PosixFilePermission.OTHERS_WRITE,
    1 -> PosixFilePermission.OTHERS_EXECUTE
  )

  def unpack(context: ResourceContext, targetDir: String): Try[String] =
    for { unpackDir <- determineUnpackDir(context, targetDir)
          fs <- context.openRead
          _ <- unpackTo(fs, new File(targetDir))
          _ <- checkDirExists(unpackDir) }
      yield unpackDir

  private def determineUnpackDir(context: ResourceContext, targetDir: String): Try[String] =
    context.filename.split("\\.tar\\.gz").toSeq match {
      case Seq(basename) => Success(Paths.get(targetDir, basename).toString)
      case _=> Failure(new IllegalArgumentException("Invalid archive name: " + context.filename))
    }

  private def checkDirExists(dir: String): Try[Unit] =
    if (new File(dir).exists())
      Success()
    else
      Failure(new IllegalArgumentException("Archive does not exist: " + dir))

  private def unpackTo(in: InputStream, targetDir: File): Try[Unit] = {
    val gzipIn = new GZIPInputStream(in)
    val tarIn = new TarInputStream(gzipIn)

    @tailrec def readEntry: Try[Unit] =
      tarIn.next match {
        case Success(null) | Failure(_: EOFException) =>
          println("eof")

          Success()
        case Success(entry) if entry.isDirectory =>
          val file = new File(targetDir, entry.getName)
          file.mkdirs()

          Files.setPosixFilePermissions(Paths.get(file.getAbsolutePath),
            convertToPermissionsSet(entry.getHeader.mode))

          readEntry
        case Success(entry) =>
          println(entry.getName)

          val file = new File(targetDir, entry.getName)
          val filepath = file.getAbsolutePath

          file.getParentFile.mkdirs()

          tarIn.copyToFile(filepath, entry.getSize)

          val perms = convertToPermissionsSet(entry.getHeader.mode)
          Files.setPosixFilePermissions(Paths.get(filepath), perms)

          println(s"Setting $perms")

          readEntry
        case Failure(e) =>
          println(e)

          Failure(e)
      }

    readEntry map { _ => targetDir.getAbsolutePath }
  }

  def convertToPermissionsSet(mode: Int): Set[PosixFilePermission] = {
    val result = Set[PosixFilePermission]()

    permissions filter { kv => (mode & kv._1) > 0 } map { _._2 } toSet
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