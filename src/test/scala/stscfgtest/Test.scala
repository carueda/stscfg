package stscfgtest

import com.typesafe.config.{Config, ConfigFactory}
import stscfg._
import utest._

class MyDConfig(val config: Config) extends BaseConfig {

  object service extends ObjConfig {
    val url: String      = string
    val poolSize: Int    = int
    val debug: Boolean   = bool
    val factor: Double   = double
    val http: Http       = obj

    val services: List[Http] = objList
  }

  implicit class Http(val config: Config) extends BaseConfig {
    val interface: String = string
    val port:      Int    = int
  }
}

object Test extends TestSuite {
  val tests: framework.Tree[framework.Test] = this {
    * - {
      val cfg = new MyDConfig(ConfigFactory.parseString(
        """
          |service {
          |  url = "http://example.net/rest"
          |  poolSize = 32
          |  debug = true
          |  factor = 0.75
          |
          |  http {
          |    interface = "0.0.0.0"
          |    port = 8080
          |  }
          |  
          |  services = [{
          |    interface = "0.0.0.1"
          |    port = 8081
          |  }, {
          |    interface = "0.0.0.2"
          |    port = 8082
          |  }]
          |}
        """.stripMargin))

      cfg.service.url ==> "http://example.net/rest"
      cfg.service.poolSize ==> 32
      cfg.service.debug ==> true
      cfg.service.factor ==> 0.75
      cfg.service.http.interface ==> "0.0.0.0"
      cfg.service.http.port ==> 8080
      cfg.service.services.length ==> 2
      cfg.service.services(1).interface ==> "0.0.0.2"
    }
  }
}
