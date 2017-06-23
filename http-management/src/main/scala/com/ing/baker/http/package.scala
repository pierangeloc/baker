package com.ing.baker

import java.net.InetAddress

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

package object http {
  final class BakerManagementSettings(val config: Config) {
    val port: Int = config.getOrElse[Int]("baker.management.port", 0)
    val hostname: String = config.getOrElse[String]("baker.management.hostname", InetAddress.getLocalHost.getHostAddress)
    val clusterEnabled: Boolean = config.getOrElse[Boolean]("baker.management.clusterEnabled", false)
  }
}
