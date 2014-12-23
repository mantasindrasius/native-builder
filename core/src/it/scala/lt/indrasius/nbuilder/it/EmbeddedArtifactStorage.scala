package lt.indrasius.nbuilder.it

import java.io.{InputStream, FileInputStream, File, ByteArrayOutputStream}
import java.nio.file.attribute.{PosixFilePermissions, PosixFilePermission}
import java.nio.file.{Paths, Files}
import java.util.zip.GZIPOutputStream

import org.kamranzafar.jtar.{TarHeader, TarEntry, TarOutputStream}
import spray.http._

import scala.collection.concurrent.TrieMap
import scala.io.Source

import collection.convert.wrapAsScala.asScalaSet
import scala.util.Try

/**
 * Created by mantas on 14.12.22.
 */
object EmbeddedArtifactStorageServer extends EmbeddedServer(EmbeddedConfig.STORAGE_PORT) {
  val BASE_URL = s"http://localhost:${EmbeddedConfig.STORAGE_PORT}/libs/"
  val basePath = Uri(BASE_URL).path

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

  def receive = {
    case req: HttpRequest =>
      val name = basePath.relativize(req.uri.path).toString()
      val parts = name.split("\\.", 2)
      val basename = parts(0)

      ArtifactStorage.getProject(basename) match {
        case Some(project) =>
          HttpResponse(StatusCodes.OK, produceArchive(project))
        case None =>
          HttpResponse(StatusCodes.NotFound, HttpEntity("Not Found"))
      }
  }

  def artifactUrl(name: String): String = BASE_URL + name + ".tar.gz"

  def produceArchive(project: ConfigureMakeProject): HttpEntity = {
    val bOut = new ByteArrayOutputStream()
    val gzipOut = new GZIPOutputStream(bOut)
    val tarOut = new TarOutputStream(gzipOut)

    //val f = new File("/media/DevZone/Dev/bison-2.7/configure")
    //val fs = new FileInputStream(f)

    def writeEntry(name: String, content: String): Try[Unit] = Try {
      //val entry = new TarEntry(f, "configure")
      val contentBytes = content.getBytes("UTF-8")
      val header = TarHeader.createHeader(name, contentBytes.size, 1, false)
      val perms = Set(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE) //Files.getPosixFilePermissions(Paths.get(f.getAbsolutePath))

      header.mode = permsToInt(perms)

      val entry = new TarEntry(header)

      tarOut.putNextEntry(entry)
      tarOut.write(contentBytes)
    }

    writeEntry("configure", makeConfigureBody(project))

    //tarOut.putNextEntry(new TarEntry(TarHeader.createHeader("configure",)))

    //tarOut.write('O')
    //tarOut.write('K')

    tarOut.close()
    gzipOut.close()

    val outBytes = bOut.toByteArray

    println(s"Respond with ${outBytes.size} bytes")

    HttpEntity(outBytes)
  }

  def makeConfigureBody(project: ConfigureMakeProject): String = {
    val sb = new StringBuilder
    val rn = "\n"

    sb.append("#!/bin/sh" + rn)
    sb.append("echo 'PATH='$PATH" + rn)

    sb.append(
      """
        |prefix=
        |
        |echo 'Arguments: '$1
        |
        |while :; do
        |  case $1 in
        |    --prefix=*)
        |      prefix=${1#*=}
        |      ;;
        |    --)
        |      shift
        |      break
        |      ;;
        |    *)
        |      break
        |  esac
        |
        |  shift
        |done
        |
        |echo 'Prefix='$prefix""".stripMargin + rn)

    project.configureSuccessLines map { line =>
      sb.append(s"echo 'Checking for $line... yes'$rn")
    }

    project.configureFailedLine map { line =>
      sb.append(s"echo '$line'$rn")
      sb.append(s"exit 2$rn")
    }

    project.outputName map { name =>
      sb.append(s"output=$$prefix'/$name'$rn")
      sb.append(s"echo $$output > output.txt$rn")
      sb.append(s"echo 'Output created as '$$output$rn")
      //sb.append(s"cat output$rn")
    }

    sb.toString()
  }

  def permsToInt(perms: Set[PosixFilePermission]): Int = {
    val result = perms map { permissions.get(_) } collect {
      case Some(v) => v
    }

    result.foldLeft(0) { (a: Int, b: Int) => a | b }
  }


  implicit class RichInputStream(in: InputStream) {
    def toBytes = Stream.continually(in.read()).takeWhile(_ != -1).map(_.toByte).toArray
  }
}

case class ConfigureMakeProject(configureSuccessLines: Seq[String] = Nil,
                                configureFailedLine: Option[String] = None,
                                sourcePath: Option[String] = None,
                                source: Option[String] = None,
                                outputName: Option[String] = None) {

  def withSuccessLines(lines: String*) =
    copy(configureSuccessLines = configureSuccessLines ++ lines.toSeq)

  def withSource(path: String, source: String) =
    copy(sourcePath = Some(path), source = Some(source))

  def withOutputName(name: String) =
    copy(outputName = Some(name))
}

object ArtifactStorage {
  val projects = TrieMap[String, ConfigureMakeProject]()

  def addArtifactFromProject(name: String, project: ConfigureMakeProject): String = {
    projects += (name -> project)

    EmbeddedArtifactStorageServer.artifactUrl(name)
  }

  def getProject(name: String): Option[ConfigureMakeProject] =
    projects.get(name)
}