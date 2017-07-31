import com.typesafe.config.{Config, ConfigFactory}
import stscfg._
import utest._

class FooCfg(c: Config) extends BaseConfig(c) {
  val str    : String      = string
  val optInt : Option[Int] = optional(int)
}

object Test extends TestSuite {
  val tests: framework.Tree[framework.Test] = this {

    * - {
      class Cfg(c: Config) extends BaseConfig(c) {
        val path : String  = string
        val url  : String  = string | "http://example.net"

        object service extends ObjConfig {
          val port : Int = int | 8080
        }

        val foo    : FooCfg         = $[FooCfg]
        val optFoo : Option[FooCfg] = optional($[FooCfg])
        val foos   : List[FooCfg]   = $[FooCfg].list
      }

      val config = ConfigFactory.parseString(
        """
          |path = "/tmp"
          |
          |service = {
          |  port = 9090
          |}
          |
          |foo = { str = "baz", optInt = 3 }
          |optFoo = { str = "bar" }
          |foos = [ { str = "baz0" }, { str = "baz1" } ]
        """.stripMargin)

      val cfg = new Cfg(config)

      assert( cfg.path == "/tmp" )
      assert( cfg.url == "http://example.net" )
      assert( cfg.service.port == 9090 )
      assert( cfg.foo.str == "baz" )
      assert( cfg.foo.optInt.contains(3) )
    }

    * - {
      val config = ConfigFactory.parseString(
        """
          |reqStr = "reqStr"
          |
          |reqFoo = { str = "baz" }
          |optFoo = { str = "bar", optInt = 3 }
          |lstFoo = [ { str = "baz0" }, { str = "baz1" } ]
          |
          |opLLFoo = [ [ { str = "baz0" }, { str = "baz1" } ] ]
          |
          |lstStr = [foo, baz]
          |
          |service = {
          |  someStr = "xyz"
          |  dblList = [3.14, 1.21]
          |}
        """.stripMargin)

      object cfg extends BaseConfig(config) {
        val reqStr : String          = string
        val optStr : Option[String]  = optional(string)

        object service extends ObjConfig {
          val someStr  : String       = string | "hey"
          val someBool : Boolean      = bool | false
          val dblList  : List[Double] = double.list
        }

        val reqFoo  : FooCfg                     = $[FooCfg]
        val optFoo  : Option[FooCfg]             = optional($[FooCfg])
        val lstFoo  : List[FooCfg]               = $[FooCfg].list
        val opLFoo  : Option[List[FooCfg]]       = $[Option[List[FooCfg]]]
        val opLLFoo : Option[List[List[FooCfg]]] = optional($[FooCfg].list.list)
      }

      cfg.reqStr            ==> "reqStr"
      cfg.reqFoo.str        ==> "baz"
      cfg.optStr.isDefined  ==> false

      cfg.optFoo.isDefined  ==> true
      cfg.optFoo.get.str    ==> "bar"
      cfg.optFoo.get.optInt ==> Some(3)

      cfg.lstFoo.length     ==> 2

      cfg.opLLFoo.isDefined         ==> true
      cfg.opLLFoo.get.length        ==> 1
      cfg.opLLFoo.get.head.length   ==> 2
      cfg.opLLFoo.get.head.head.str ==> "baz0"
      cfg.opLLFoo.get.head(1).str   ==> "baz1"

      cfg.service.someStr  ==> "xyz"
      cfg.service.someBool ==> false
      cfg.service.dblList  ==> List(3.14, 1.21)
    }
  }
}
