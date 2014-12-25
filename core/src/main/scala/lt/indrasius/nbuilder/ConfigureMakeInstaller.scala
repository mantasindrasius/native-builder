package lt.indrasius.nbuilder

import scala.util.Try

/**
 * Created by mantas on 14.12.22.
 */
class ConfigureMakeInstaller(installDir: String, processFactory: ProcessFactory) {

  def install(projectDir: String): Try[Unit] =
    for { _ <- configure(projectDir)
          _ <- make(projectDir)
          _ <- makeInstall(projectDir) }
      yield ()

  private def configure(dir: String): Try[Unit] =
    processFactory.runProc("./configure --prefix=" + installDir, dir)

  private def make(dir: String): Try[Unit] =
    processFactory.runProc("make", dir)

  private def makeInstall(dir: String): Try[Unit] =
    processFactory.runProc("make install", dir)
}
