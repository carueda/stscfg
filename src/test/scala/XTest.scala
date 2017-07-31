import com.typesafe.config.{Config, ConfigFactory}
import stscfg.x._
import utest._

class MyXCfg(c: Config) extends BaseConfig(c) {

  val reqStr : String          = $[String]
  val optStr : Option[String]  = $[Option[String]]

  object service extends ObjConfig {
    val someStr  : String       = $[String](default = "hey")
    val someBool : Boolean      = $[Boolean](default = false)
    val dblList  : List[Double] = $[List[Double]]
  }

  val reqFoo  : FooCfg                     = $[FooCfg]
  val optFoo  : Option[FooCfg]             = $[Option[FooCfg]]
  val lstFoo  : List[FooCfg]               = $[List[FooCfg]]
  val opLFoo  : Option[List[FooCfg]]       = $[Option[List[FooCfg]]]
  val opLLFoo : Option[List[List[FooCfg]]] = $[Option[List[List[FooCfg]]]]
}

class FooCfg(c: Config) extends BaseConfig(c) {
  val str      : String        = $[String]
  val optInt   : Option[Int]   = $[Option[Int]]
}

object XTest extends TestSuite {
  val tests: framework.Tree[framework.Test] = this {
    * - {
      val cfg = new MyXCfg(ConfigFactory.parseString(
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
        """.stripMargin))

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
