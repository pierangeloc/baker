package com.ing.baker.runtime.actor

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.cluster.sharding.ShardRegion._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.util.Timeout
import com.ing.baker.il.sha256HashCode
import com.ing.baker.runtime.actor.ClusterBakerActorProvider._
import com.ing.baker.runtime.actor.process_index.ProcessIndex.ActorMetadata
import com.ing.baker.runtime.actor.process_index.ProcessIndexProtocol._
import com.ing.baker.runtime.actor.process_index._
import com.ing.baker.runtime.actor.recipe_manager.RecipeManager
import com.ing.baker.runtime.actor.serialization.Encryption
import com.ing.baker.runtime.core.interations.InteractionManager
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Future
import scala.concurrent.duration._

object ClusterBakerActorProvider {

  case class GetShardIndex(entityId: String) extends InternalBakerMessage

  /**
    * This function calculates the names of the ActorIndex actors
    * gets the least significant bits of the UUID, and returns the MOD 10
    * So we have at most 10 manager actors created, all the petrinet actors will fall under these 10 actors
    * Note, the nrOfShards used here has to be aligned with the nrOfShards used in the shardIdExtractor
    */
  def entityId(processId: String, nrOfShards: Int): String =
    s"index-${Math.abs(sha256HashCode(processId) % nrOfShards)}"

  // extracts the actor id -> message from the incoming message
  // Entity id is the first character of the UUID
  def entityIdExtractor(nrOfShards: Int): ExtractEntityId = {
    case msg:ProcessIndexMessage => (entityId(msg.processId, nrOfShards), msg)
    case GetShardIndex(entityId) => (entityId, GetIndex)
    case msg => throw new IllegalArgumentException(s"Message not recognized: $msg")
  }

  // extracts the shard id from the incoming message
  def shardIdExtractor(nrOfShards: Int): ExtractShardId = {
    case msg:ProcessIndexMessage => Math.abs(sha256HashCode(msg.processId) % nrOfShards).toString
    case GetShardIndex(entityId) => entityId.split(s"index-").last
    case ShardRegion.StartEntity(entityId) => entityId.split(s"index-").last
    case msg => throw new IllegalArgumentException(s"Message not recognized: $msg")
  }

  val recipeManagerName = "RecipeManager"
}

class ClusterBakerActorProvider(config: Config, configuredEncryption: Encryption) extends BakerActorProvider {

  private val nrOfShards = config.as[Int]("baker.actor.cluster.nr-of-shards")
  private val retentionCheckInterval = config.as[Option[FiniteDuration]]("baker.actor.retention-check-interval").getOrElse(1 minute)
  private val actorIdleTimeout: Option[FiniteDuration] = config.as[Option[FiniteDuration]]("baker.actor.idle-timeout")

  override def createProcessIndexActor(interactionManager: InteractionManager, recipeManager: ActorRef)(implicit actorSystem: ActorSystem): ActorRef = {
    ClusterSharding(actorSystem).start(
      typeName = "ProcessIndexActor",
      entityProps = ProcessIndex.props(retentionCheckInterval, actorIdleTimeout, configuredEncryption, interactionManager, recipeManager),
      settings = ClusterShardingSettings.create(actorSystem),
      extractEntityId = ClusterBakerActorProvider.entityIdExtractor(nrOfShards),
      extractShardId = ClusterBakerActorProvider.shardIdExtractor(nrOfShards)
    )
  }

  override def createRecipeManagerActor()(implicit actorSystem: ActorSystem): ActorRef = {

    val singletonManagerProps = ClusterSingletonManager.props(
      RecipeManager.props(),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(actorSystem))

    actorSystem.actorOf(props = singletonManagerProps, name = recipeManagerName)

    val singletonProxyProps = ClusterSingletonProxy.props(
      singletonManagerPath = s"/user/$recipeManagerName",
      settings = ClusterSingletonProxySettings(actorSystem))

    actorSystem.actorOf(props = singletonProxyProps, name = "RecipeManagerProxy")
  }

  def getIndex(actor: ActorRef)(implicit system: ActorSystem, timeout: FiniteDuration) = {

    import akka.pattern.ask
    import system.dispatcher
    implicit val akkaTimeout: Timeout = timeout

    val futures = (0 to nrOfShards).map { shard => actor.ask(GetShardIndex(s"index-$shard")).mapTo[Index].map(_.entries) }
    val collected: Seq[Set[ActorMetadata]] = Util.collectFuturesWithin(futures, timeout, system.scheduler)

    collected.reduce((a, b) => a ++ b)
  }
}