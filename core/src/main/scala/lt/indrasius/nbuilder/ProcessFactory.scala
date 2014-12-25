package lt.indrasius.nbuilder

import scala.util.Try

/**
 * Created by mantas on 14.12.23.
 */
trait ProcessFactory {
  def run(cmd: String, cwd: String): scala.sys.process.Process
  def runProc(cmd: String, dir: String): Try[Unit] = Try {
    run(cmd, dir).exitValue() match {
      case 0 => ()
      case exitCode => throw new IllegalStateException(s"$cmd exited with $exitCode")
    }
  }
}
