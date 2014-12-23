package lt.indrasius.nbuilder

import java.io.{FileInputStream, File, InputStream, FileOutputStream}

import scala.util.Try

/**
 * Created by mantas on 14.12.23.
 */
trait ResourceHandler {
  def save(filename: String, bytes: Array[Byte]): Try[ResourceContext]
}

trait ResourceContext {
  def openRead: Try[InputStream]
}

case class FileResourceContext(file: File) extends ResourceContext {
  def openRead: Try[InputStream] = Try { new FileInputStream(file) }
}

class FileResourceHandler extends ResourceHandler {
  def save(filename: String, bytes: Array[Byte]) =
    Try { new FileOutputStream(filename) } map { fs =>
      try {
        fs.write(bytes)
        FileResourceContext(new File(filename))
      }
      finally fs.close()
    }
}
