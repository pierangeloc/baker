package com.ing.baker.http

import org.scalatest.{BeforeAndAfterAll, FunSuite}

class TestStartBakerManagement extends FunSuite with BeforeAndAfterAll {

  val bakerManagement = new BakerManagement()

  test("start baker management frontend for test purposes") {
    bakerManagement.start()
    bakerManagement.stop()
  }
}
