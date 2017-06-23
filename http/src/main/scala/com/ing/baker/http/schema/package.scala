package com.ing.baker.http


package object schema {
  def schemaForClass(clazz: Class[_]): JsonSchema = {
    val fields = clazz.getDeclaredFields
      .filterNot(field => field.isSynthetic)
      .map(schemaForField).toList

    JsonSchema(fields)
  }

  def schemaForField(field: java.lang.reflect.Field): Field = {
    schema.Field(typeForClass(field.getType), field.getName, field.getName, false)
  }

  def typeForClass(clazz: Class[_]): String = {

    clazz match {
      case c if c == classOf[String] => "string"
      case c if c == classOf[Int]    => "integer"
      case c if c == classOf[Long]   => "integer"
      case c if c == classOf[Double] => "number"
      case c if c == classOf[Float]  => "number"
      case _ => "?"
    }
  }
}
