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
    val b = TarGzArchiveBuilder()

    b.addEntry("configure", makeConfigureBody(project))

    val bytes = b.build

    println(s"Respond with ${bytes.size} bytes")

    HttpEntity(bytes)
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