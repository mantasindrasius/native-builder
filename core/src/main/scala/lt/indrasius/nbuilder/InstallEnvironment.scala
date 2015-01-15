package lt.indrasius.nbuilder

import java.io.File
import java.nio.file.Paths

import com.twitter.io.TempDirectory
import lt.indrasius.nbuilder.http.HttpClient

import scala.util.Try

/**
 * Created by mantas on 14.12.24.
 */

trait InstallEnvironmentContext {
  protected def downloader: ResourceDownloader
  protected def unpacker: ResourceUnpacker
  protected def processFactory: ProcessFactory

  class InstallEnvironment(projectsDir: String, installsDir: String) {
    def install(url: String): Try[String] =
      for { downloadDir <- createTempDir
            context <- downloader.download(url, downloadDir)
            projectDir <- unpacker.unpack(context, projectsDir)
            installDir = Paths.get(installsDir, new File(projectDir).getName).toString
            installer = new ConfigureMakeInstaller(installDir, processFactory)
            _ <- installer.install(projectDir) }
      yield installDir

    private def createTempDir: Try[String] =
      Try {
        TempDirectory.create(true)
      } map {
        _.getAbsolutePath
      }
  }
}

trait InstallEnvironment extends InstallEnvironmentContext {
  protected def processFactory: ProcessFactory
  protected val downloader = new ResourceDownloader(HttpClient(), new FileResourceHandler)
  protected val unpacker = new TarGzResourceUnpacker

  def apply(projectsDir: String, installsDir: String): InstallEnvironment =
    new InstallEnvironment(projectsDir, installsDir)
}

object InstallEnvironment extends InstallEnvironment {
  protected def processFactory: ProcessFactory = RealProcessFactory
}