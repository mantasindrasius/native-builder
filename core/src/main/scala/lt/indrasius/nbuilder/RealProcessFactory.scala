package lt.indrasius.nbuilder

import java.io.File

import scala.sys.process.Process
import scala.util.Try

/**
 * Created by mantas on 14.12.24.
 */
object RealProcessFactory extends ProcessFactory {
  def run(cmd: String, cwd: String): Process =
    Process(cmd, new File(cwd)).run()
}
