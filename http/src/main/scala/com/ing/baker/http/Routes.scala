package com.ing.baker.http

import akka.http.scaladsl.server.Directives
import com.ing.baker.core.Baker

trait Routes extends Directives {

  // val baker: Baker

  val routes = pathPrefix("baker") {
    path("hello") {
      get {
        complete("hello")
      }
    }
  }
}
