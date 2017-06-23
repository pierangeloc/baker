package com.ing.baker.http

import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagementRoutes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.DefaultJsonProtocol

object BakerManagementRoutes extends Directives with SprayJsonSupport with DefaultJsonProtocol {

  def apply(_pathPrefix: Option[String], cluster: Option[Cluster]): Route = {
    val pp = _pathPrefix.getOrElse("/")
    val helloRoutes = pathPrefix(pp) {
      path("hello") {
        get {
          complete("hello")
        }
      }
    }

    if (cluster.isDefined) {
      ClusterHttpManagementRoutes(cluster.get, pp) ~ helloRoutes
    } else {
      helloRoutes
    }
  }

}
