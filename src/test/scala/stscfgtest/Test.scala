package stscfgtest

import com.typesafe.config.{Config, ConfigFactory}
import stscfg._
import utest._

class MyConfig(val config: Config) extends BaseConfig {

  val optStr: Option[String]  = ~string
  val strDef: String          = string | "foo"

  val reqInt: Int             = int
  val optInt: Option[Int]     = ~int
  val intDef: Int             = int | 2

  val reqBool: Boolean        = bool
  val optBool: Option[Boolean]= ~bool
  val boolDef: Boolean        = bool | false

  val reqDouble: Double       = double

  object service extends ObjConfig {
    val http:     Http       = obj
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
      val cfg = new MyConfig(ConfigFactory.parseString(
        """
          |optStr = "baz"
          |
          |reqInt = 32
          |
          |reqBool = true
          |reqDouble = 0.75
          |
          |service {
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

      cfg.optStr     ==> Some("baz")
      cfg.strDef     ==> "foo"

      cfg.reqInt     ==> 32
      cfg.optInt     ==> None
      cfg.intDef     ==> 2

      cfg.reqBool    ==> true
      cfg.optBool    ==> None
      cfg.boolDef    ==> false

      cfg.reqDouble  ==> 0.75

      cfg.service.http.interface ==> "0.0.0.0"
      cfg.service.http.port ==> 8080
      cfg.service.services.length ==> 2
      cfg.service.services(1).interface ==> "0.0.0.2"
    }
  }
}
