package lt.indrasius.nbuilder.it

import akka.actor.ActorSystem

/**
 * Created by mantas on 14.12.22.
 */
object EmbeddedConfig {
  val STORAGE_PORT = 9902
  val actorSystem = ActorSystem()
}