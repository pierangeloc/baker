package com.ing.baker.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.DefaultJsonProtocol

object BakerUIRoutes extends Directives with SprayJsonSupport with DefaultJsonProtocol {

  def apply(_pathPrefix: Option[String]): Route = {
    val pp = _pathPrefix.getOrElse("/")
    pathPrefix(pp) {
      path("event") {
        post {
          complete("hello")
        }
      } ~ path("schemas") {
        get {
          complete("schemas")
        }
      }
    }
  }

}
