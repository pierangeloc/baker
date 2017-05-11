package com.ing.baker.http

import akka.http.scaladsl.server.Directives
import com.fasterxml.jackson.databind.{MapperFeature, ObjectMapper}
import com.fasterxml.jackson.module.jsonSchema.{JsonSchema, JsonSchemaGenerator}
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.util.{ByteString, CompactByteString}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.ing.baker.api.Event


case class OrderPlaced(order: String) extends Event
case class Customer(name: String, address: String, email: String)
//an event can also provide a more complex ingredient as a case class
case class CustomerInfoReceived(customerInfo: Customer) extends Event

trait JacksonSerialization {
  private val defaultObjectMapper: ObjectMapper = new ObjectMapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
  defaultObjectMapper.registerModule(DefaultScalaModule)

  private def toJSON(mapper: ObjectMapper, `object`: Any): String =
    try {
      mapper.writeValueAsString(`object`)
    }
    catch {
      case e: JsonProcessingException => throw new IllegalArgumentException("Cannot marshal to JSON: " + `object`, e)
    }

  implicit val schemaMarshaller: ToEntityMarshaller[JsonSchema] = Marshaller.opaque { schema =>
    val json = toJSON(defaultObjectMapper, schema)
    HttpEntity.Strict(ContentTypes.`application/json`, CompactByteString(json))
  }

  def schemaForClass(clazz: Class[_]): JsonSchema = {
    // configure mapper, if necessary, then create schema generator
    val schemaGen = new JsonSchemaGenerator(defaultObjectMapper)
    val schema = schemaGen.generateSchema(clazz)
    println(defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema))

    schema
  }
}

trait Routes extends Directives with JacksonSerialization {

  val events = Seq(classOf[OrderPlaced], classOf[CustomerInfoReceived])

  val schemas: Map[String, JsonSchema] = events.map(c => c.getSimpleName -> schemaForClass(c)).toMap

  val dashboardRoutes = pathPrefix("dashboard") { getFromResourceDirectory("") }

  val apiRoutes = pathPrefix("api") {
    path("schemas" / Segment) { id =>
      get {
        complete(schemas(id))
      }
    }
  }
}
