import java.time.Duration

import com.typesafe.config.{Config, ConfigFactory}
import stscfg._
import utest._

class FooCfg(c: Config) extends BaseConfig(c) {
  val str    : String      = string
  val optInt : Option[Int] = optional[Int]
}

object Test extends TestSuite {
  val tests: framework.Tree[framework.Test] = this {

    "static-config" - {
      class Cfg(c: Config) extends BaseConfig(c) {
        object `static-config` extends ObjConfig {
          val intEntry    : Int    = int
          val stringEntry : String = string

          object group extends ObjConfig {
            val listEntry     : List[String] = string.list
            val durationEntry : Duration     = duration
            val optDuration   : Option[Duration] = optional(duration)
          }
        }

        val `other-config` : Int = int
      }

      val config = ConfigFactory.parseString(
        """
          |static-config {
          |  intEntry = 1
          |  stringEntry = "String"
          |  group {
          |    listEntry = ["val1", "val2"]
          |    durationEntry = 6h
          |    optDuration = 3600s
          |  }
          |}
          |other-config = 2
        """.stripMargin)

      val cfg = new Cfg(config)

      cfg.`static-config`.intEntry ==> 1
      cfg.`static-config`.stringEntry ==> "String"
      cfg.`static-config`.group.listEntry ==> List("val1", "val2")
      cfg.`static-config`.group.durationEntry.toHours ==>  6
      cfg.`static-config`.group.optDuration.map(_.toMinutes) ==> Some(60)
      cfg.`other-config` ==> 2
    }

    "service-and-foo" - {
      class Cfg(c: Config) extends BaseConfig(c) {
        val path : String  = string
        val url  : String  = string | "http://example.net"

        object service extends ObjConfig {
          val port : Int = int | 8080
        }

        val foo    : FooCfg         = $[FooCfg]
        val optFoo : Option[FooCfg] = optional[FooCfg]
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

    "service-and-foo2" - {
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
        val optStr : Option[String]  = optional[String]

        object service extends ObjConfig {
          val someStr  : String       = string | "hey"
          val someBool : Boolean      = bool | false
          val dblList  : List[Double] = double.list
        }

        val reqFoo  : FooCfg                     = $[FooCfg]
        val optFoo  : Option[FooCfg]             = optional[FooCfg]
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

    "size-in-bytes" - {
      class Cfg(c: Config) extends BaseConfig(c) {
        val sizeReq : Size             = size
        val sizeOpt : Option[Size]     = optional(size)
        val sizeDfl : Size             = size | Size(1024)
        val sizes   : List[Size]       = size.list
        val sizes2  : List[List[Size]] = size.list.list
      }

      val config = ConfigFactory.parseString(
        """
          |regularLong = 2121
          |
          |sizeReq = 2048K
          |sizeOpt = "1024000"
          |sizes = [ 1000, "64G", "16kB" ]
          |sizes2  = [[ 1000, "64G" ], [ "16kB" ] ]
        """.stripMargin)

      val cfg = new Cfg(config)

      cfg.sizeReq.bytes ==> 2048*1024
      assert(cfg.sizeOpt.contains(Size(1024000)))
      cfg.sizeDfl.bytes ==> 1024
      cfg.sizes.map(_.bytes) ==> List(1000, 64*1024*1024*1024L, 16*1000)
      cfg.sizes2.map(_ map (_.bytes)) ==> List(List(1000, 64*1024*1024*1024L), List(16*1000))
    }
  }
}
