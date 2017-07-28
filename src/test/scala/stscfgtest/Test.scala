package stscfgtest

import com.typesafe.config.{Config, ConfigFactory}
import stscfg._
import utest._

class MyConfig(val config: Config) extends BaseConfig {

  val optStr: Option[String]    = ~string
  val strDef: String            = string | "foo"

  val reqInt: Int               = int
  val optInt: Option[Int]       = int.optional
  val intDef: Int               = int | 2
  val intList: List[Int]        = int.list

  val reqBool: Boolean          = bool
  val optBool: Option[Boolean]  = ~bool
  val boolDef: Boolean          = bool | false

  val boolList: List[Boolean]   = bool.list

  val reqDouble: Double         = double
  val optDouble: Option[Double] = double.optional

  object service extends ObjConfig {
    val http:     Http       = Http(obj)

    val services: List[Http] = !obj map Http

    // TODO val optServices: Option[List[Http]] = obj.list.optional map (_ map Http)

    val optHttp:  Option[Http] = ~obj map Http
  }


  case class Http(config: Config) extends BaseConfig {
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
          |intList = [1,2,3]
          |
          |reqBool = true
          |boolList = [ false, true ]
          |
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
      cfg.intList    ==> List(1,2,3)
      cfg.optInt     ==> None
      cfg.intDef     ==> 2

      cfg.reqBool    ==> true
      cfg.optBool    ==> None
      cfg.boolDef    ==> false
      cfg.boolList   ==> List(false, true)

      cfg.reqDouble  ==> 0.75

      cfg.service.http.interface ==> "0.0.0.0"
      cfg.service.http.port ==> 8080
      cfg.service.services.length ==> 2
      cfg.service.services(1).interface ==> "0.0.0.2"

      cfg.service.optHttp ==> None
    }
  }
}
