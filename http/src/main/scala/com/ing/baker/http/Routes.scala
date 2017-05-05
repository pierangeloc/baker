package com.ing.baker.http

import akka.http.scaladsl.server.Directives
import com.ing.baker.compiler.CompiledRecipe
import com.ing.baker.core.Baker
import com.ing.baker.schema.SchemaType

trait Routes extends Directives {

//  val baker: Baker

  val resourceRoutes = pathPrefix("resources") { getFromResourceDirectory("") }

  val routes = pathPrefix("baker") {
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
