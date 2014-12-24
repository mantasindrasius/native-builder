package lt.indrasius.nbuilder.embedded

import akka.actor.ActorSystem

/**
 * Created by mantas on 14.12.22.
 */
object EmbeddedConfig {
  val STORAGE_PORT = 9902
  val RESOURCE_PORT = 9903
  val actorSystem = ActorSystem()
}
