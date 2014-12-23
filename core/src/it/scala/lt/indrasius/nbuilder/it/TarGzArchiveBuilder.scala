package lt.indrasius.nbuilder.it

import java.io.ByteArrayOutputStream
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPOutputStream

import org.kamranzafar.jtar.{TarEntry, TarHeader, TarOutputStream}

import scala.util.Try

/**
 * Created by mantas on 14.12.23.
 */
class TarGzArchiveBuilder {
  private val permissions = Map(
    PosixFilePermission.OWNER_READ -> 0400,
    PosixFilePermission.OWNER_WRITE -> 0200,
    PosixFilePermission.OWNER_EXECUTE -> 0100,
    PosixFilePermission.GROUP_READ -> 040,
    PosixFilePermission.GROUP_WRITE -> 020,
    PosixFilePermission.GROUP_EXECUTE -> 010,
    PosixFilePermission.OTHERS_READ -> 04,
    PosixFilePermission.OTHERS_WRITE -> 02,
    PosixFilePermission.OTHERS_EXECUTE -> 01
  )

  val bOut = new ByteArrayOutputStream()
  val gzipOut = new GZIPOutputStream(bOut)
  val tarOut = new TarOutputStream(gzipOut)

  def addEntry(name: String, content: String): TarGzArchiveBuilder = {
    //val entry = new TarEntry(f, "configure")
    val contentBytes = content.getBytes("UTF-8")
    val header = TarHeader.createHeader(name, contentBytes.size, 1, false)
    val perms = Set(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE) //Files.getPosixFilePermissions(Paths.get(f.getAbsolutePath))

    header.mode = permsToInt(perms)

    val entry = new TarEntry(header)

    tarOut.putNextEntry(entry)
    tarOut.write(contentBytes)

    this
  }

  def build = {
    tarOut.close()
    gzipOut.close()

    bOut.toByteArray
  }

  private def permsToInt(perms: Set[PosixFilePermission]): Int = {
    val result = perms map { permissions.get(_) } collect {
      case Some(v) => v
    }

    result.foldLeft(0) { (a: Int, b: Int) => a | b }
  }
}

object TarGzArchiveBuilder {
  def apply() = new TarGzArchiveBuilder
}