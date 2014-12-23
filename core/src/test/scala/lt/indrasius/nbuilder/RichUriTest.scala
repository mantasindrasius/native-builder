package lt.indrasius.nbuilder

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers
import spray.http.Uri

/**
 * Created by mantas on 14.12.23.
 */
class RichUriTest extends FlatSpec with MustMatchers {
  import http.RichUri

  /*"RichUri" should "resolve the same url if full url is given" in {
    Uri("http://whatever").resolve("http://expected/") must be (Uri("http://expected/"))
  }

  "RichUri" should "resolve url if only path is given" in {
    Uri("http://whatever").resolve("hello") must be (Uri("http://whatever/hello"))
  }*/
}
