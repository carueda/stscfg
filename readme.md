# stscfg

This preliminary project captures some ideas to extend the basic mechanism from 
[static-config](https://github.com/Krever/static-config)
to incorporate features from [tscfg](https://github.com/carueda/tscfg).
Possible future directions to explore include combining or bringing in aspects from
[refined](https://github.com/fthomas/refined), 
[bond](https://github.com/fwbrasil/bond) and similar.

## Usage

stscfg has two main classes, `BaseConfig` and `ObjConfig`.
Extend `BaseConfig` to define a base configuration object,
and extend `ObjConfig` to directly define an embedded 
configuration object. 
Methods in these classes, along with supporting elements, 
can be used to specify your configuration schema.

## Examples

The example from [static-config](https://github.com/Krever/static-config) looks like this:

```scala
import com.typesafe.config.{Config, ConfigFactory}
import java.time.Duration
import stscfg._

class Cfg(c: Config) extends BaseConfig(c) {

  object `static-config` extends ObjConfig {
    val intEntry    : Int    = int
    val stringEntry : String = string

    object group extends ObjConfig {
      val listEntry     : List[String] = string.list
      val durationEntry : Duration     = duration
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
    |  }
    |}
    |other-config = 2
  """.stripMargin)

val cfg = new Cfg(config)

cfg.`static-config`.intEntry ==> 1
cfg.`static-config`.stringEntry ==> "String"
cfg.`static-config`.group.listEntry ==> List("val1", "val2")
cfg.`static-config`.group.durationEntry.toHours ==>  6
cfg.`other-config` ==> 2
```

With an object structure definition to be used multiple times,
first define the corresponding BaseConfig and then refer to it
in the main definitions:

```scala
// the common object structure:
class FooCfg(c: Config) extends BaseConfig(c) {
  val str    : String      = string
  val optInt : Option[Int] = optional[Int]
}

// the main configuration schema:
class Cfg(c: Config) extends BaseConfig(c) {

  // a required string
  val path : String = string
  
  // a string with a default value
  val url : String = string | "http://example.net"

  // directly defined embedded object
  object service extends ObjConfig {
    val port : Int = int | 8080
  }

  val foo    : FooCfg         = $[FooCfg]
  val optFoo : Option[FooCfg] = optional[FooCfg]
  val foos   : List[FooCfg]   = $[FooCfg].list
}

```

With the above in place, we can then parse the following configuration:

```
path = "/tmp"

service = {
  port = 9090
}

foo = { 
  str = "baz", 
  optInt = 3 
}

optFoo = { str = "bar" }

foos = [ 
  { str = "baz0" }, 
  { str = "baz1" } 
]
```

```scala
val cfg = new Cfg(ConfigFactory.parseString(...))

assert( cfg.path == "/tmp" )
assert( cfg.url == "http://example.net" )
assert( cfg.service.port == 9090 )
assert( cfg.foo.str == "baz" )
assert( cfg.foo.optInt == Some(3) )
```
