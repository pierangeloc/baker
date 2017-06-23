package com.ing.baker.http

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class TestStartBakerManagement extends FunSuite with BeforeAndAfterAll {

  val actorSystem = ActorSystem()
  val bakerManagement = new BakerManagement()(actorSystem)

  test("start baker management frontend for test purposes") {
    Await.result(bakerManagement.start(), 5 minutes)

    bakerManagement.stop()
  }
}
