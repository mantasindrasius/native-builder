package lt.indrasius.nbuilder

import org.scalatest.{Ignore, FlatSpec}
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar

/**
 * Created by mantas on 14.12.23.
 */
@Ignore
class ConfigureProcessorTest extends FlatSpec with MustMatchers with MockitoSugar {
  "ConfigureProcessor" should "configure the project" in {
    val projectDir = "/hello"
    val processor = new ConfigureProcessor(projectDir)

    processor.process must be a 'success
  }
}
