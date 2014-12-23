package lt.indrasius.nbuilder

/**
 * Created by mantas on 14.12.23.
 */
trait ProcessFactory {
  def run(cmd: String, cwd: String): scala.sys.process.Process
}
