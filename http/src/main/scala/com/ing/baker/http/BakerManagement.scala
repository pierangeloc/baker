package com.ing.baker.http

import java.util.concurrent.atomic.AtomicReference

import akka.Done
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.server.{Directives, RouteResult}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer

import scala.concurrent.{Future, Promise}

class BakerManagement(
                       pathPrefix: Option[String] = None,
                       https: Option[ConnectionContext] = None,
                       cluster: Option[Cluster] = None
                     ) extends Directives {
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val settings = new BakerManagementSettings(system.settings.config)

  import system.dispatcher

  private val bindingFuture = new AtomicReference[Future[Http.ServerBinding]]()

  def start(): Future[Done] = {
    val serverBindingPromise = Promise[Http.ServerBinding]()
    if (bindingFuture.compareAndSet(null, serverBindingPromise.future)) {

      val routes = RouteResult.route2HandlerFlow(BakerManagementRoutes(pathPrefix, cluster) ~ BakerUIRoutes(pathPrefix))

      val serverFutureBinding = https match {
        case Some(context) ⇒
          Http().bindAndHandle(
            routes,
            settings.hostname,
            settings.port,
            connectionContext = context
          )
        case None ⇒
          Http().bindAndHandle(
            routes,
            settings.hostname,
            settings.port
          )
      }

      serverBindingPromise.completeWith(serverFutureBinding)
      serverBindingPromise.future.map(_ => Done)
    } else {
      Future(Done)
    }
  }

  def stop(): Future[Done] =
    if (bindingFuture.get() == null) {
      Future(Done)
    } else {
      val stopFuture = bindingFuture.get().flatMap(_.unbind()).map(_ => Done)
      bindingFuture.set(null)
      stopFuture
    }
}


//class BakerManagement extends HttpApp with Routes {
//  private val system = ActorSystem()
//  private val bakerManagementConfig = system.settings.config.getConfig("baker.management")
//
//  def start(): Unit = {
//    val hostname: String = bakerManagementConfig.as[String]("http.hostname")
//    val port: Int = bakerManagementConfig.as[Int]("http.port")
//
//    startServer(hostname, port, ServerSettings(system), system)
//  }
//
//  def stop(): Unit = {
//    binding().map(_.unbind())
//  }
//
//  override def route(): Route = routes
//
//  override def waitForShutdownSignal(system: ActorSystem)(implicit ec: ExecutionContext): Future[Done] =
//    Promise().future // never completes
//}
